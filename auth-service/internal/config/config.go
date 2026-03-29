package config

import (
	"github.com/spf13/viper"
)

// Config uygulamanın tüm ayarlarını tutar
type Config struct {
	ServerPort       int    `mapstructure:"SERVER_PORT"`
	DBHost           string `mapstructure:"DB_HOST"`
	DBPort           int    `mapstructure:"DB_PORT"`
	DBUser           string `mapstructure:"DB_USER"`
	DBPassword       string `mapstructure:"DB_PASSWORD"`
	DBName           string `mapstructure:"DB_NAME"`
	DBSSLMode        string `mapstructure:"DB_SSLMODE"`
	JWTSecret        string `mapstructure:"JWT_SECRET"`
	JWTAccessExpiry  string `mapstructure:"JWT_ACCESS_EXPIRY"`
	JWTRefreshExpiry string `mapstructure:"JWT_REFRESH_EXPIRY"`
}

// Load yapılandırmayı .env dosyasından veya ortam değikenlerinden okur
func Load() (*Config, error) {
	viper.SetConfigFile(".env")
	viper.AutomaticEnv() // Ortam değişkenlerini doğrudan bağla

	// .env okumazsa çok sorun değil, belki deployment ortamında (Docker vs) doğrudan env variable verilmiştir
	_ = viper.ReadInConfig()

	var cfg Config
	if err := viper.Unmarshal(&cfg); err != nil {
		return nil, err
	}

	return &cfg, nil
}
