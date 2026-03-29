# Auth Service 🔒

Canlı kullanıma (Production-Ready) hazır, Echo v5 ve Go rutinleri üzerine inşa edilmiş yüksek performanslı Kimlik Doğrulama servisidir.

## 🌟 Neler İçerir?
- **3 Katmanlı Mimari:** Domain, Repository ve Service izolasyonu. (Clean Architecture standardı)
- **Token Rotasyonu:** Güvenlik sıkılaştırılmış JWT yapısı (Access + Refresh tokenlar). Geri dönük, eski Refresh token denemeleri otomatik tespit edilir.
- **PostgreSQL & Docker:** Otomotize edilmiş container yönetimi (Sqlx, Pgx driver).
- **Graceful Shutdown:** Echo v5 üzerinden kapanırken açık istekleri başarıyla sonlandırma.
- **Güvenli Saklama:** Bcrypt (cost:12) ile şifre hash'leme ve çevre değişkenleri (`.env`).
- **Structured Logging:** Go `log/slog` kütüphanesi ile modern JSON çıktıları.

## 🚀 Çalıştırma Rehberi

İlk kurulum esnasında `.env.example` dosyasını kopyalayarak `.env` adında bir dosya oluşturun ve içini kendi güvenli değerlerinizle (özellikle uzun bir Base64 `JWT_SECRET` belirleyerek) doldurun.

**1. Veritabanını Hazırlama**
```bash
# Docker içindeki Auth-DB ayağa kalkar, migration ve indexler yaratılır
sh db.sh reset
```

**2. Projeyi Başlatma**

*(Terminalde servis kök dizininde olduğunuzdan emin olun)*
```bash
go run ./cmd/main.go
```

## 🧪 Test Ortamı
Sistemde kusursuz işleyen Go unit-testleri dışında, tüm uç noktalar (endpoints) VS Code üzerinden `api.http` dosyasıyla manuel test edilebileceği gibi, yanda bulunan `.sh` dosyası üzerinden uçtan uca hızlıca otomatik de test edilebilir:
```bash
# Unit testler
go test ./internal/service/... -v

# E2E testleri
bash test.sh
```

## 📐 Endpoints (API V1)

Varsayılan port HTTP `:8081` üzerinden yayın yapar.

| Method | Endpoint | Description | Auth Gerekli |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Yeni Kullanıcı Kaydı. (Bcrypt Encryption) | Hayır |
| POST | `/api/v1/auth/login` | Access ve Refresh Token çifti döndürür | Hayır |
| POST | `/api/v1/auth/refresh` | Eski Refresh Token'ı imha edip yenisini üretir | Hayır |
| POST | `/api/v1/auth/logout` | Token veritabanından kalıcı olarak silinir | Hayır |
| GET | `/api/v1/auth/me` | JWT kontrolünden geçerek profili döner | Evet (Bearer) |
