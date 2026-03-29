package main

import (
	"context"
	"fmt"
	"log/slog"
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
	// 0. Slog (Structured Logger) Ayarları
	// Docker vb. container ortamlarında en çok tercih edilen JSON formattır.
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	// 1. Config Yükle
	cfg, err := config.Load()
	if err != nil {
		slog.Error("failed to load config", "error", err)
		os.Exit(1)
	}

	// 2. Veritabanına Bağlan (pgx sürücüsü)
	dsn := fmt.Sprintf("postgres://%s:%s@%s:%d/%s?sslmode=%s",
		cfg.DBUser, cfg.DBPassword, cfg.DBHost, cfg.DBPort, cfg.DBName, cfg.DBSSLMode)

	db, err := sqlx.Connect("pgx", dsn)
	if err != nil {
		slog.Error("database connection failed", "error", err)
		os.Exit(1)
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		slog.Error("database unreachable. is container running?", "error", err)
		os.Exit(1)
	}

	slog.Info("connected to database successfully", "db", cfg.DBName)

	// 3. Katmanları Birleştir (Dependency Injection / Wiring)
	userRepo := repository.NewUserRepository(db)
	tokenRepo := repository.NewTokenRepository(db)

	tokenSvc := service.NewTokenService(cfg)
	authSvc := service.NewAuthService(userRepo, tokenRepo, tokenSvc)

	authHandler := handler.NewAuthHandler(authSvc)

	// 4. Echo Router Başlat ve Ayarla (v5)
	e := echo.New()
	
	// Echo'nun tüm default logları artık yukarıda hazırladığımız slog sistemini kullanacak
	e.Logger = logger 
	
	e.Use(middleware.Recover())
	e.Use(middleware.CORS("*"))

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
