package domain

import (
	"errors"
	"time"

	"github.com/google/uuid"
)

// Hata Kodları - Katmanlar arası ortak kullanacağımız hatalar
var (
	ErrUserNotFound       = errors.New("kayit bulunamadi")
	ErrInvalidCredentials = errors.New("gecersiz e-posta veya sifre")
	ErrEmailExists        = errors.New("bu e-posta adresi zaten kullanimda")
	ErrInvalidToken       = errors.New("gecersiz veya suresi dolmus token")
)

// User sistemdeki ana kullanıcı modelimiz
type User struct {
	ID        uuid.UUID `json:"id" db:"id"`
	Email     string    `json:"email" db:"email"`
	Password  string    `json:"-" db:"password"` // json:"-" şifrenin dışarıya açılmasını önler
	FullName  string    `json:"full_name" db:"full_name"`
	CreatedAt time.Time `json:"created_at" db:"created_at"`
	UpdatedAt time.Time `json:"updated_at" db:"updated_at"`
}

// RefreshToken uzun süreli oturum kimlikleri
type RefreshToken struct {
	ID        uuid.UUID `json:"id" db:"id"`
	UserID    uuid.UUID `json:"user_id" db:"user_id"`
	Token     string    `json:"-" db:"token"` // Güvenlik gereği dışarı JSON olarak çıkmaz
	ExpiresAt time.Time `json:"expires_at" db:"expires_at"`
	CreatedAt time.Time `json:"created_at" db:"created_at"`
}
