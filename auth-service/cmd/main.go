package main

import (
	"context"
	"fmt"
	"log"
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
	// 1. Config Yükle
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("Config hatasi: %v", err)
	}

	// 2. Veritabanına Bağlan (pgx sürücüsü kullanıyoruz, daha performanslıdır)
	dsn := fmt.Sprintf("postgres://%s:%s@%s:%d/%s?sslmode=%s",
		cfg.DBUser, cfg.DBPassword, cfg.DBHost, cfg.DBPort, cfg.DBName, cfg.DBSSLMode)

	db, err := sqlx.Connect("pgx", dsn)
	if err != nil {
		log.Fatalf("DB baglanti hatasi: %v", err)
	}
	defer db.Close() // Uygulama kapanırken DB bağlantılarını da düzgünce kapat

	if err := db.Ping(); err != nil {
		log.Fatalf("DB'ye erisilemiyor (Container calisiyor mu? psql sifreleri dogru mu?): %v", err)
	}

	// 3. Katmanları Birleştir (Dependency Injection / Wiring)
	userRepo := repository.NewUserRepository(db)
	tokenRepo := repository.NewTokenRepository(db)

	tokenSvc := service.NewTokenService(cfg)
	authSvc := service.NewAuthService(userRepo, tokenRepo, tokenSvc)

	authHandler := handler.NewAuthHandler(authSvc)

	// 4. Echo Router Başlat ve Ayarla (v5)
	e := echo.New()

	// Çökmeleri önlemek ve origin'lere izin vermek için temel korumalar
	e.Use(middleware.Recover())
	e.Use(middleware.CORS())

	// 5. Rota Bağlantıları (Routing)
	v1 := e.Group("/api/v1")
	authGroup := v1.Group("/auth")

	authGroup.POST("/register", authHandler.Register)
	authGroup.POST("/login", authHandler.Login)
	authGroup.POST("/refresh", authHandler.Refresh)
	authGroup.POST("/logout", authHandler.Logout)

	// JWTAuth middleware'imizi /me endpoint'ine özel enjekte ediyoruz
	authGroup.GET("/me", authHandler.GetMe, authmw.JWTAuth(tokenSvc))

	// 6. Graceful Shutdown
	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	sc := echo.StartConfig{
		Address:         fmt.Sprintf(":%d", cfg.ServerPort),
		GracefulTimeout: 10 * time.Second,
	}

	fmt.Printf("--Auth Service hazir! Dinlenen Port: %d\n", cfg.ServerPort)
	if err := sc.Start(ctx, e); err != nil {
		log.Fatalf("Sunucu kapatildi: %v", err)
	}
}
