package main

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	_ "github.com/jackc/pgx/v5/stdlib"
	"github.com/jmoiron/sqlx"
	"github.com/labstack/echo/v5"
	"github.com/labstack/echo/v5/middleware"

	"github.com/yusufziyrek/courier-microservice/auth-service/internal/config"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/handler"
	authmw "github.com/yusufziyrek/courier-microservice/auth-service/internal/middleware"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/repository"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/service"
)

func main() {
	// 0. Slog (Structured Logger)
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	// 1. Config Yükle (Eklenti: Validation)
	cfg, err := config.Load()
	if err != nil {
		slog.Error("failed to load config", "error", err)
		os.Exit(1)
	}

	// 2. Veritabanına Bağlan
	dsn := fmt.Sprintf("postgres://%s:%s@%s:%d/%s?sslmode=%s",
		cfg.DBUser, cfg.DBPassword, cfg.DBHost, cfg.DBPort, cfg.DBName, cfg.DBSSLMode)

	db, err := sqlx.Connect("pgx", dsn)
	if err != nil {
		slog.Error("database connection failed", "error", err)
		os.Exit(1)
	}
	defer db.Close()

	// Eklenti: Connection Pooling (Veritabanı yorulmasını ve connection leak'leri önler)
	db.SetMaxOpenConns(25)
	db.SetMaxIdleConns(10)
	db.SetConnMaxLifetime(5 * time.Minute)

	if err := db.Ping(); err != nil {
		slog.Error("database unreachable. is container running?", "error", err)
		os.Exit(1)
	}

	slog.Info("connected to database successfully", "db", cfg.DBName)

	// 3. Katmanları Birleştir
	userRepo := repository.NewUserRepository(db)
	tokenRepo := repository.NewTokenRepository(db)

	tokenSvc := service.NewTokenService(cfg)
	authSvc := service.NewAuthService(userRepo, tokenRepo, tokenSvc)

	authHandler := handler.NewAuthHandler(authSvc)

	// 4. Echo Router Başlat ve Ayarla
	e := echo.New()
	e.Logger = logger
	
	e.Use(middleware.Recover())
	e.Use(middleware.CORS("*"))
	
	// Eklenti: Rate Limiter (Saniyede Max 20 IP isteği - DDoS & Brute Force koruması)
	e.Use(middleware.RateLimiter(middleware.NewRateLimiterMemoryStore(20.0)))

	// Eklenti: Health-Check (Container, Node veya Load Balancer için Canlılık Testi)
	e.GET("/health", func(c *echo.Context) error {
		return c.JSON(http.StatusOK, map[string]string{
			"status": "UP",
			"database": "connected",
		})
	})

	// 5. Rota Bağlantıları
	v1 := e.Group("/api/v1")
	authGroup := v1.Group("/auth")
	
	authGroup.POST("/register", authHandler.Register)
	authGroup.POST("/login", authHandler.Login)
	authGroup.POST("/refresh", authHandler.Refresh)
	authGroup.POST("/logout", authHandler.Logout)
	authGroup.GET("/me", authHandler.GetMe, authmw.JWTAuth(tokenSvc))

	// 6. Graceful Shutdown
	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	sc := echo.StartConfig{
		Address:         fmt.Sprintf(":%d", cfg.ServerPort),
		GracefulTimeout: 10 * time.Second,
	}

	slog.Info("Auth Service is ready!", "port", cfg.ServerPort)
	if err := sc.Start(ctx, e); err != nil {
		slog.Error("server shut down", "error", err)
		os.Exit(1)
	}
}
