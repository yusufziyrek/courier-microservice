package config

import (
	"fmt"

	"github.com/go-playground/validator/v10"
	"github.com/spf13/viper"
)

type Config struct {
	ServerPort       int    `mapstructure:"SERVER_PORT" validate:"required"`
	DBHost           string `mapstructure:"DB_HOST" validate:"required"`
	DBPort           int    `mapstructure:"DB_PORT" validate:"required"`
	DBUser           string `mapstructure:"DB_USER" validate:"required"`
	DBPassword       string `mapstructure:"DB_PASSWORD" validate:"required"`
	DBName           string `mapstructure:"DB_NAME" validate:"required"`
	DBSSLMode        string `mapstructure:"DB_SSLMODE" validate:"required"`
	JWTSecret        string `mapstructure:"JWT_SECRET" validate:"required,min=16"` // Secret en az 16 byte olmalı
	JWTAccessExpiry  string `mapstructure:"JWT_ACCESS_EXPIRY" validate:"required"`
	JWTRefreshExpiry string `mapstructure:"JWT_REFRESH_EXPIRY" validate:"required"`
}

func Load() (*Config, error) {
	viper.SetConfigFile(".env")
	viper.AutomaticEnv() 

	_ = viper.ReadInConfig()

	var cfg Config
	if err := viper.Unmarshal(&cfg); err != nil {
		return nil, err
	}

	// Eksik çevre değişkeni kontrolü (Production Fail-Fast kuralı)
	validate := validator.New()
	if err := validate.Struct(&cfg); err != nil {
		return nil, fmt.Errorf("config eksik veya hatali: %w", err)
	}

	return &cfg, nil
}
