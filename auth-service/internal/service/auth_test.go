package service_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/domain"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/service"
	"golang.org/x/crypto/bcrypt"
)

// ========= MANUAL REPOSITORY MOCKS =========
// Golang'de dış bağımlılıkları testlere sokmamak için MOCK nesneler kullanırız.

type mockUserRepo struct {
	users map[string]*domain.User
}

func newMockUserRepo() *mockUserRepo {
	return &mockUserRepo{users: make(map[string]*domain.User)}
}

func (m *mockUserRepo) Create(ctx context.Context, user *domain.User) error {
	if _, exists := m.users[user.Email]; exists {
		return domain.ErrEmailExists
	}
	user.ID = uuid.New()
	m.users[user.Email] = user
	return nil
}

func (m *mockUserRepo) GetByEmail(ctx context.Context, email string) (*domain.User, error) {
	user, exists := m.users[email]
	if !exists {
		return nil, domain.ErrUserNotFound
	}
	return user, nil
}

func (m *mockUserRepo) GetByID(ctx context.Context, id uuid.UUID) (*domain.User, error) {
	for _, u := range m.users {
		if u.ID == id {
			return u, nil
		}
	}
	return nil, domain.ErrUserNotFound
}

// TokenRepo Mock
type mockTokenRepo struct {
	tokens map[string]*domain.RefreshToken
}

func newMockTokenRepo() *mockTokenRepo {
	return &mockTokenRepo{tokens: make(map[string]*domain.RefreshToken)}
}

func (m *mockTokenRepo) Create(ctx context.Context, token *domain.RefreshToken) error {
	token.ID = uuid.New()
	m.tokens[token.Token] = token
	return nil
}

func (m *mockTokenRepo) GetByToken(ctx context.Context, tokenStr string) (*domain.RefreshToken, error) {
	token, exists := m.tokens[tokenStr]
	if !exists {
		return nil, domain.ErrInvalidToken
	}
	return token, nil
}

func (m *mockTokenRepo) DeleteByToken(ctx context.Context, tokenStr string) error {
	delete(m.tokens, tokenStr)
	return nil
}

func (m *mockTokenRepo) DeleteAllUserTokens(ctx context.Context, userID uuid.UUID) error {
	// Simple mock that deletes tokens by userID
	for k, v := range m.tokens {
		if v.UserID == userID {
			delete(m.tokens, k)
		}
	}
	return nil
}

// Token Service Mock
type mockTokenSvc struct{}

func (m *mockTokenSvc) GenerateAccessToken(user *domain.User) (string, error) {
	return "mock-access-token", nil
}

func (m *mockTokenSvc) GenerateRefreshToken() (string, time.Time, error) {
	return "mock-refresh-token", time.Now().Add(time.Hour), nil
}

func (m *mockTokenSvc) ValidateAccessToken(tokenStr string) (uuid.UUID, error) {
	return uuid.Nil, errors.New("not implemented in mock")
}

// ========= TESTLER =========

func TestAuthService_Register(t *testing.T) {
	uRepo := newMockUserRepo()
	authSvc := service.NewAuthService(uRepo, newMockTokenRepo(), &mockTokenSvc{})

	email := "test@example.com"
	password := "123456"

	// 1. Başarılı Kayıt
	user, err := authSvc.Register(context.Background(), email, password, "Test User")
	if err != nil {
		t.Fatalf("beklenmeyen hata: %v", err)
	}
	if user.Email != email {
		t.Errorf("beklenen e-posta: %s, alinan: %s", email, user.Email)
	}

	// Şifre bcrypt ile hashlenmiş mi kontrol et (düz metin saklanmamalı)
	if user.Password == password {
		t.Error("sifre guvensiz duz metin olarak kaydedildi")
	}

	// 2. Çift Kayıt Durumu (Duplicate)
	_, err = authSvc.Register(context.Background(), email, "anotherpass", "Another User")
	if err != domain.ErrEmailExists {
		t.Errorf("beklenen hata ErrEmailExists, alinan: %v", err)
	}
}

func TestAuthService_Login(t *testing.T) {
	uRepo := newMockUserRepo()
	authSvc := service.NewAuthService(uRepo, newMockTokenRepo(), &mockTokenSvc{})
	ctx := context.Background()

	email := "login_test@example.com"
	password := "mypassword"

	// Sisteme en baştan bcrypt'li bir kullanıcı enjekte edelim
	hashedPwd, _ := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	uRepo.Create(ctx, &domain.User{Email: email, Password: string(hashedPwd), FullName: "Test"})

	// 1. Hatalı Parola
	_, _, err := authSvc.Login(ctx, email, "wrongpass")
	if err != domain.ErrInvalidCredentials {
		t.Errorf("beklenen hata ErrInvalidCredentials, alinan: %v", err)
	}

	// 2. Olmayan E-Posta
	_, _, err = authSvc.Login(ctx, "nonexistent@example.com", "mypassword")
	if err != domain.ErrInvalidCredentials {
		t.Errorf("beklenen hata ErrInvalidCredentials, alinan: %v", err)
	}

	// 3. Doğru Giriş
	acc, ref, err := authSvc.Login(ctx, email, password)
	if err != nil {
		t.Fatalf("beklenmeyen hata: %v", err)
	}
	if acc != "mock-access-token" || ref != "mock-refresh-token" {
		t.Errorf("hatali token uretimi")
	}
}

func TestAuthService_Refresh(t *testing.T) {
	uRepo := newMockUserRepo()
	tRepo := newMockTokenRepo()
	authSvc := service.NewAuthService(uRepo, tRepo, &mockTokenSvc{})
	ctx := context.Background()

	userID := uuid.New()
	uRepo.users["test@test.com"] = &domain.User{ID: userID, Email: "test@test.com"}

	validTokenStr := "valid-refresh-token"
	tRepo.Create(ctx, &domain.RefreshToken{
		UserID:    userID,
		Token:     validTokenStr,
		ExpiresAt: time.Now().Add(1 * time.Hour), // Süresi dolmamış
	})

	// 1. Success Request (Rotasyon) Testi
	acc, ref, err := authSvc.Refresh(ctx, validTokenStr)
	if err != nil {
		t.Fatalf("refresh sirasinda hata: %v", err)
	}
	if acc == "" || ref == "" {
		t.Error("yeni tokenlar uretilemedi")
	}

	// Token Rotasyonunun asıl olayı: Eski token veri tabanından SİLİNMELİ.
	if _, exists := tRepo.tokens[validTokenStr]; exists {
		t.Error("guvenlik acigi: eski token sistemden silinmedi (rotasyon basarisiz)")
	}

	// 2. Süresi geçmiş token reddedilmeli
	expiredTokenStr := "expired-token"
	tRepo.Create(ctx, &domain.RefreshToken{
		UserID:    userID,
		Token:     expiredTokenStr,
		ExpiresAt: time.Now().Add(-1 * time.Hour), // Gecmise ayarla
	})

	_, _, err = authSvc.Refresh(ctx, expiredTokenStr)
	if err != domain.ErrInvalidToken {
		t.Errorf("beklenen ErrInvalidToken, alinan: %v", err)
	}
}

func TestAuthService_Logout(t *testing.T) {
	tRepo := newMockTokenRepo()
	authSvc := service.NewAuthService(newMockUserRepo(), tRepo, &mockTokenSvc{})
	ctx := context.Background()

	tokenStr := "logout-token"
	tRepo.Create(ctx, &domain.RefreshToken{Token: tokenStr})

	err := authSvc.Logout(ctx, tokenStr)
	if err != nil {
		t.Fatalf("beklenmeyen hata: %v", err)
	}

	if _, exists := tRepo.tokens[tokenStr]; exists {
		t.Error("logout sonrası veritabanından veri kalmış")
	}
}
