package handler

import (
	"errors"
	"net/http"

	"github.com/go-playground/validator/v10"
	"github.com/google/uuid"
	"github.com/labstack/echo/v5"

	"github.com/yusufziyrek/courier-microservice/auth-service/internal/domain"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/service"
)

type AuthHandler struct {
	authSvc   service.AuthService
	validator *validator.Validate
}

func NewAuthHandler(authSvc service.AuthService) *AuthHandler {
	return &AuthHandler{
		authSvc:   authSvc,
		validator: validator.New(),
	}
}

// mapError servis hatalarını belirli HTTP yanıtlarına eşler
func (h *AuthHandler) mapError(c *echo.Context, err error) error {
	switch {
	case errors.Is(err, domain.ErrInvalidCredentials):
		return c.JSON(http.StatusUnauthorized, ErrorResponse{Error: err.Error(), Code: "INVALID_CREDENTIALS"})
	case errors.Is(err, domain.ErrEmailExists):
		return c.JSON(http.StatusConflict, ErrorResponse{Error: err.Error(), Code: "EMAIL_EXISTS"})
	case errors.Is(err, domain.ErrInvalidToken):
		return c.JSON(http.StatusUnauthorized, ErrorResponse{Error: err.Error(), Code: "INVALID_TOKEN"})
	case errors.Is(err, domain.ErrUserNotFound):
		return c.JSON(http.StatusNotFound, ErrorResponse{Error: err.Error(), Code: "USER_NOT_FOUND"})
	default:
		return c.JSON(http.StatusInternalServerError, ErrorResponse{Error: "Unexpected server error", Code: "INTERNAL_ERROR"})
	}
}

func (h *AuthHandler) Register(c *echo.Context) error {
	var req RegisterRequest
	
	// v5 API: JSON Body parse
	if err := echo.BindBody(c, &req); err != nil {
		return c.JSON(http.StatusBadRequest, ErrorResponse{Error: "Invalid JSON format", Code: "BAD_REQUEST"})
	}

	// Validation
	if err := h.validator.Struct(req); err != nil {
		return c.JSON(http.StatusBadRequest, ErrorResponse{Error: err.Error(), Code: "VALIDATION_ERROR"})
	}

	user, err := h.authSvc.Register(c.Request().Context(), req.Email, req.Password, req.FullName)
	if err != nil {
		return h.mapError(c, err)
	}

	return c.JSON(http.StatusCreated, user)
}

func (h *AuthHandler) Login(c *echo.Context) error {
	var req LoginRequest
	if err := echo.BindBody(c, &req); err != nil {
		return c.JSON(http.StatusBadRequest, ErrorResponse{Error: "Invalid JSON format", Code: "BAD_REQUEST"})
	}

	if err := h.validator.Struct(req); err != nil {
		return c.JSON(http.StatusBadRequest, ErrorResponse{Error: err.Error(), Code: "VALIDATION_ERROR"})
	}

	acc, ref, err := h.authSvc.Login(c.Request().Context(), req.Email, req.Password)
	if err != nil {
		return h.mapError(c, err)
	}

	return c.JSON(http.StatusOK, TokenResponse{
		AccessToken:  acc,
		RefreshToken: ref,
	})
}

func (h *AuthHandler) Refresh(c *echo.Context) error {
	var req RefreshRequest
	if err := echo.BindBody(c, &req); err != nil {
		return c.JSON(http.StatusBadRequest, ErrorResponse{Error: "Invalid JSON format", Code: "BAD_REQUEST"})
	}

	if err := h.validator.Struct(req); err != nil {
		return c.JSON(http.StatusBadRequest, ErrorResponse{Error: err.Error(), Code: "VALIDATION_ERROR"})
	}

	acc, ref, err := h.authSvc.Refresh(c.Request().Context(), req.RefreshToken)
	if err != nil {
		return h.mapError(c, err)
	}

	return c.JSON(http.StatusOK, TokenResponse{
		AccessToken:  acc,
		RefreshToken: ref,
	})
}

func (h *AuthHandler) Logout(c *echo.Context) error {
	var req RefreshRequest
	if err := echo.BindBody(c, &req); err != nil {
		return c.JSON(http.StatusBadRequest, ErrorResponse{Error: "Invalid JSON format", Code: "BAD_REQUEST"})
	}

	// Siledik mi silmedik mi dışarı sızdırmaya gerek yok, sildik diyelim :)
	_ = h.authSvc.Logout(c.Request().Context(), req.RefreshToken)

	return c.JSON(http.StatusOK, map[string]string{"message": "Logged out successfully"})
}

func (h *AuthHandler) GetMe(c *echo.Context) error {
	// Middleware userID'yi "user_id" anahtarıyla context'e koyacak.
	// Echo v5 Generic helper'ları ile tip güvenli alıyoruz.
	userIDStr, err := echo.ContextGet[string](c, "user_id")
	if err != nil || userIDStr == "" {
		return c.JSON(http.StatusUnauthorized, ErrorResponse{Error: "Unauthorized (Token missing)", Code: "UNAUTHORIZED"})
	}

	userID, err := uuid.Parse(userIDStr)
	if err != nil {
		return c.JSON(http.StatusUnauthorized, ErrorResponse{Error: "Unauthorized (Invalid token)", Code: "UNAUTHORIZED"})
	}

	user, err := h.authSvc.GetMe(c.Request().Context(), userID)
	if err != nil {
		return h.mapError(c, err)
	}

	return c.JSON(http.StatusOK, user)
}
