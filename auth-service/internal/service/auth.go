package service

import (
	"context"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"

	"github.com/yusufziyrek/courier-microservice/auth-service/internal/domain"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/repository"
)

type AuthService interface {
	Register(ctx context.Context, email, password, fullName string) (*domain.User, error)
	Login(ctx context.Context, email, password string) (string, string, error)
	Refresh(ctx context.Context, refreshTokenStr string) (string, string, error)
	Logout(ctx context.Context, refreshTokenStr string) error
	GetMe(ctx context.Context, userID uuid.UUID) (*domain.User, error)
}

type authService struct {
	userRepo  repository.UserRepository
	tokenRepo repository.TokenRepository
	tokenSvc  TokenService
}

func NewAuthService(uRepo repository.UserRepository, tRepo repository.TokenRepository, tSvc TokenService) AuthService {
	return &authService{
		userRepo:  uRepo,
		tokenRepo: tRepo,
		tokenSvc:  tSvc,
	}
}

func (s *authService) Register(ctx context.Context, email, password, fullName string) (*domain.User, error) {
	// Şifreyi güvenli bir şekilde hash'le (cost: 12 ideal)
	hashed, err := bcrypt.GenerateFromPassword([]byte(password), 12)
	if err != nil {
		return nil, err
	}

	user := &domain.User{
		Email:    email,
		Password: string(hashed),
		FullName: fullName,
	}

	err = s.userRepo.Create(ctx, user)
	if err != nil {
		return nil, err
	}

	return user, nil
}

func (s *authService) Login(ctx context.Context, email, password string) (string, string, error) {
	user, err := s.userRepo.GetByEmail(ctx, email)
	if err != nil {
		return "", "", domain.ErrInvalidCredentials
	}

	// Hash'lenmiş şifre ile girilen düz metin şifreyi kıyasla
	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password)); err != nil {
		return "", "", domain.ErrInvalidCredentials
	}

	return s.generateTokens(ctx, user)
}

func (s *authService) Refresh(ctx context.Context, refreshTokenStr string) (string, string, error) {
	// Eski token DB'de var mı?
	oldToken, err := s.tokenRepo.GetByToken(ctx, refreshTokenStr)
	if err != nil {
		return "", "", err 
	}

	// Süresi geçmiş mi? Geçmişse hemen sil ki veritabanında yer kaplamasın
	if time.Now().After(oldToken.ExpiresAt) {
		_ = s.tokenRepo.DeleteByToken(ctx, refreshTokenStr)
		return "", "", domain.ErrInvalidToken
	}

	user, err := s.userRepo.GetByID(ctx, oldToken.UserID)
	if err != nil {
		return "", "", err
	}

	// Token Rotasyonu: Güvenlik için eski token'ı sil... 
	_ = s.tokenRepo.DeleteByToken(ctx, refreshTokenStr)

	// ... ve tamamen yeni bir access + refresh ikilisi yarat.
	return s.generateTokens(ctx, user)
}

func (s *authService) Logout(ctx context.Context, refreshTokenStr string) error {
	// Tek yapmak gereken onu DB'den silmek. Böylece artık Refresh edilemez.
	return s.tokenRepo.DeleteByToken(ctx, refreshTokenStr)
}

func (s *authService) GetMe(ctx context.Context, userID uuid.UUID) (*domain.User, error) {
	return s.userRepo.GetByID(ctx, userID)
}

// generateTokens içeride kullanılan gizli bir fonksiyondur, yeni token ikilisi basar.
func (s *authService) generateTokens(ctx context.Context, user *domain.User) (string, string, error) {
	accessToken, err := s.tokenSvc.GenerateAccessToken(user)
	if err != nil {
		return "", "", err
	}

	refreshTokenStr, expiresAt, err := s.tokenSvc.GenerateRefreshToken()
	if err != nil {
		return "", "", err
	}

	err = s.tokenRepo.Create(ctx, &domain.RefreshToken{
		UserID:    user.ID,
		Token:     refreshTokenStr,
		ExpiresAt: expiresAt,
	})
	if err != nil {
		return "", "", err
	}

	return accessToken, refreshTokenStr, nil
}
