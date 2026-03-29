package service

import (
	"crypto/rand"
	"encoding/base64"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/config"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/domain"
)

type TokenService interface {
	GenerateAccessToken(user *domain.User) (string, error)
	GenerateRefreshToken() (string, time.Time, error)
	ValidateAccessToken(tokenStr string) (uuid.UUID, error)
}

// tokenClaims JWT'nin içine koyacağımız JSON alanları
type tokenClaims struct {
	UserID uuid.UUID `json:"user_id"`
	Email  string    `json:"email"`
	jwt.RegisteredClaims
}

type tokenService struct {
	cfg *config.Config
}

func NewTokenService(cfg *config.Config) TokenService {
	return &tokenService{cfg: cfg}
}

func (s *tokenService) GenerateAccessToken(user *domain.User) (string, error) {
	// Expiry süresini .env'den çek
	duration, err := time.ParseDuration(s.cfg.JWTAccessExpiry)
	if err != nil {
		duration = 15 * time.Minute // Default fallback
	}

	claims := &tokenClaims{
		UserID: user.ID,
		Email:  user.Email,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(duration)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(s.cfg.JWTSecret))
}

func (s *tokenService) GenerateRefreshToken() (string, time.Time, error) {
	// Kriptografik güvenli random 32 bytes üretip base64 formatına çeviriyoruz
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return "", time.Time{}, err
	}
	tokenStr := base64.URLEncoding.EncodeToString(b)

	duration, err := time.ParseDuration(s.cfg.JWTRefreshExpiry)
	if err != nil {
		duration = 7 * 24 * time.Hour
	}

	return tokenStr, time.Now().Add(duration), nil
}

func (s *tokenService) ValidateAccessToken(tokenStr string) (uuid.UUID, error) {
	token, err := jwt.ParseWithClaims(tokenStr, &tokenClaims{}, func(t *jwt.Token) (interface{}, error) {
		return []byte(s.cfg.JWTSecret), nil
	})

	if err != nil || !token.Valid {
		return uuid.Nil, domain.ErrInvalidToken
	}

	if claims, ok := token.Claims.(*tokenClaims); ok {
		return claims.UserID, nil
	}

	return uuid.Nil, domain.ErrInvalidToken
}
