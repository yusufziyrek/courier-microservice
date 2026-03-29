# Auth Service

Bu servis Go + Echo ile kimlik doğrulama, JWT üretimi ve refresh token yönetimi sağlar.

Workspace'te önerilen kullanım, tüm sistemi kökten scriptlerle yönetmektir:

```bash
sh scripts/up.sh
sh scripts/smoke.sh
sh scripts/down.sh
```

## Öne Çıkan Özellikler

Sistem, ölçeklenebilir ve bakımı kolay bir yapı sunmak amacıyla aşağıdaki yaklaşımlarla geliştirilmiştir:

- Clean architecture (`Handler`, `Service`, `Repository`)
- JWT access token + opaque refresh token
- IP bazlı rate limiting
- SQL connection pooling
- Fail-fast config doğrulaması
- Structured logging (`log/slog`)

---

## Proje Dizin Yapısı

```text
auth-service/
├── cmd/
│   └── main.go              # Sistemin giriş noktası, Dependency Injection ve Echo ayarları
├── internal/
│   ├── config/              # Viper & Validator ile .env sistem yakalayıcısı
│   ├── domain/              # Çekirdek iş modelleri (User, Token) ve Hata kodları
│   ├── handler/             # HTTP endpoint yöneticileri (Context parse, Payload Binding)
│   ├── middleware/          # JWT yetkilendirme yakalayıcısı
│   ├── repository/          # Postgresql SQLx veritabanı iletişimi (Create, Lookup)
│   └── service/             # Tüm iş mantığı, şifreleme ve token operasyonlarının beyni
├── db.sh                    # Bağımsız Docker Postgres DB yönetim scripti
├── test.sh                  # Terminal uçtan uca otomasyon test betiği
└── api.http                 # VS Code için manuel REST Client testleri
```

---

## Ön Koşullar

Servisin çalışması için:
- Go 1.22+
- PostgreSQL 15+ (Docker veya lokal)

---

## Lokal Çalıştırma

İlk kurulumda `.env.example` baz alınarak `.env` oluştur.

1. Veritabanını Hazırlama

Docker varsa `db.sh` ile izole Postgres ortamı kurulur. Lokal Postgres kullanıyorsan `.env` bağlantı bilgilerini güncelle.

```bash
# Docker ortamını hazırlamak için (opsiyonel):
sh db.sh reset
```

2. Servisi Başlatma

```bash
go run ./cmd/main.go
# VEYA derleyip çalıştırabilirsiniz:
go build -o bin/app ./cmd/main.go && ./bin/app
```

---

## Endpoints

| İsim | Method | Endpoint | İçerik (Payload / Header) | Yetki |
|---|---|---|---|---|
| Sağlık Testi | GET | `/health` | Servis ve DB bağlantı durumunu döner | Yok |
| Kayıt Ol | POST | `/api/v1/auth/register` | `{"email", "password", "full_name"}` | Yok |
| Giriş Yap | POST | `/api/v1/auth/login` | `{"email", "password"}` | Yok |
| Token Yenile | POST | `/api/v1/auth/refresh` | `{"refresh_token"}` | Yok |
| Çıkış | POST | `/api/v1/auth/logout` | `{"refresh_token"}` | Yok |
| Profil (Me) | GET | `/api/v1/auth/me` | `Authorization: Bearer <Access_Token>` | Gerekli |

---

## Test

Unit testler mock repository ile koşar.

**Birim Testleri Çalıştırmak İçin:**
```bash
go test ./internal/service/... -v
```

Çalışan bir sistemde auth test scripti için:
```bash
bash test.sh
```

Tüm sistem için tek komut smoke testi:

```bash
sh ../scripts/smoke.sh
```
