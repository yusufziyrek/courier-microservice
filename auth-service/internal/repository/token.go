package repository

import (
	"context"
	"database/sql"
	"errors"

	"github.com/google/uuid"
	"github.com/jmoiron/sqlx"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/domain"
)

// TokenRepository arayüzü
type TokenRepository interface {
	Create(ctx context.Context, token *domain.RefreshToken) error
	GetByToken(ctx context.Context, tokenStr string) (*domain.RefreshToken, error)
	DeleteByToken(ctx context.Context, tokenStr string) error
	DeleteAllUserTokens(ctx context.Context, userID uuid.UUID) error
}

type tokenRepository struct {
	db *sqlx.DB
}

// NewTokenRepository DB bağımlılığını alır
func NewTokenRepository(db *sqlx.DB) TokenRepository {
	return &tokenRepository{db: db}
}

func (r *tokenRepository) Create(ctx context.Context, token *domain.RefreshToken) error {
	query := `
		INSERT INTO refresh_tokens (user_id, token, expires_at) 
		VALUES ($1, $2, $3) 
		RETURNING id, created_at`

	// pgx sürücüsü sayesinde ekleneni hemen okuyabiliyoruz
	err := r.db.QueryRowContext(ctx, query,
		token.UserID,
		token.Token,
		token.ExpiresAt,
	).Scan(&token.ID, &token.CreatedAt)

	return err
}

func (r *tokenRepository) GetByToken(ctx context.Context, tokenStr string) (*domain.RefreshToken, error) {
	query := `SELECT id, user_id, token, expires_at, created_at FROM refresh_tokens WHERE token = $1`

	var token domain.RefreshToken
	err := r.db.GetContext(ctx, &token, query, tokenStr)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, domain.ErrInvalidToken
		}
		return nil, err
	}
	return &token, nil
}

func (r *tokenRepository) DeleteByToken(ctx context.Context, tokenStr string) error {
	query := `DELETE FROM refresh_tokens WHERE token = $1`
	_, err := r.db.ExecContext(ctx, query, tokenStr)
	return err
}

func (r *tokenRepository) DeleteAllUserTokens(ctx context.Context, userID uuid.UUID) error {
	query := `DELETE FROM refresh_tokens WHERE user_id = $1`
	_, err := r.db.ExecContext(ctx, query, userID)
	return err
}
