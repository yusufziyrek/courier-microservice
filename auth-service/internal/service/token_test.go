package service_test

import (
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/config"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/domain"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/service"
)

func TestTokenService_GenerateAndValidateAccessToken(t *testing.T) {
	cfg := &config.Config{
		JWTSecret:        "test_secret_key",
		JWTAccessExpiry:  "15m",
		JWTRefreshExpiry: "7d",
	}

	tokenSvc := service.NewTokenService(cfg)
	user := &domain.User{
		ID:    uuid.New(),
		Email: "test@example.com",
	}

	// Üretim aşaması
	tokenStr, err := tokenSvc.GenerateAccessToken(user)
	if err != nil {
		t.Fatalf("beklenmeyen hata: %v", err)
	}
	if tokenStr == "" {
		t.Fatal("jwt token string boş dondu")
	}

	// Doğrulama aşaması
	userID, err := tokenSvc.ValidateAccessToken(tokenStr)
	if err != nil {
		t.Fatalf("token dogrulama hatası uretti: %v", err)
	}

	if userID != user.ID {
		t.Errorf("beklenen id: %v, alinan %v", user.ID, userID)
	}
}

func TestTokenService_GenerateRefreshToken(t *testing.T) {
	cfg := &config.Config{
		JWTRefreshExpiry: "7d",
	}

	tokenSvc := service.NewTokenService(cfg)
	tokenStr, expiresAt, err := tokenSvc.GenerateRefreshToken()
	
	if err != nil {
		t.Fatalf("beklenmeyen hata: %v", err)
	}
	if tokenStr == "" {
		t.Fatal("refresh token string bos dondu")
	}

	// Token'in geçerlilik süresi mantıklı mı (gelecekte mi)?
	if expiresAt.Before(time.Now()) {
		t.Error("refresh token suresi gecmise tarihlenmis")
	}
}

func TestTokenService_ValidateAccessToken_Invalid(t *testing.T) {
	cfg := &config.Config{
		JWTSecret: "test_secret_key",
	}
	tokenSvc := service.NewTokenService(cfg)

	// Sahte (uydurma) token atıyoruz ki hack saldırısını simüle edelim
	_, err := tokenSvc.ValidateAccessToken("invalid.fake.token")
	
	if err == nil {
		t.Fatal("gecersiz token hata atmadan kabul gordu!")
	}
	
	if err != domain.ErrInvalidToken {
		t.Errorf("beklenen hata ErrInvalidToken, alinan: %v", err)
	}
}
