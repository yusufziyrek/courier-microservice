package middleware

import (
	"net/http"
	"strings"

	"github.com/labstack/echo/v5"

	"github.com/yusufziyrek/courier-microservice/auth-service/internal/handler"
	"github.com/yusufziyrek/courier-microservice/auth-service/internal/service"
)

// JWTAuth - Access token'ı doğrular ve UserID'yi context'e yerleştirir.
func JWTAuth(tokenSvc service.TokenService) echo.MiddlewareFunc {
	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(c *echo.Context) error {
			authHeader := c.Request().Header.Get("Authorization")
			
			if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
				return c.JSON(http.StatusUnauthorized, handler.ErrorResponse{
					Error: "missing or malformed authorization header",
					Code:  "UNAUTHORIZED",
				})
			}

			// "Bearer " kısmını at
			tokenStr := strings.TrimPrefix(authHeader, "Bearer ")

			// Token validation
			userID, err := tokenSvc.ValidateAccessToken(tokenStr)
			if err != nil {
				return c.JSON(http.StatusUnauthorized, handler.ErrorResponse{
					Error: "expired or invalid token",
					Code:  "INVALID_TOKEN",
				})
			}

			// Sonraki katmanın (Handler'ın) bu bilgiye kolayca ulaşabilmesi için 
			// UserID'yi string olarak context'e atıyoruz.
			c.Set("user_id", userID.String())

			// İstek yetkili, yola devam
			return next(c)
		}
	}
}
