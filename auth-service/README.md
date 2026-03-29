# 🔐 Auth Service (Courier Microservice)

Bu servis, **Production-grade (Üretim Seviyesi) Mimari Desenler** kullanılarak, Echo v5 (RC) ve Go rutinleri üzerine inşa edilmiş yüksek performanslı bir Kimlik Doğrulama mikroservisidir.

## 🌟 Öne Çıkan Özellikler & Mimari Yaklaşımlar

Sistem, ölçeklenebilir ve bakımı kolay bir yapı sunmak amacıyla aşağıdaki yaklaşımlarla geliştirilmiştir:

- **Clean Architecture (3 Katmanlı):** `Handler`, `Service`, `Repository` katmanları ile tam izolasyon.
- **Token Rotasyonu (JWT):** Opaque *Refresh Token* yapısı ile güvenli oturum yönetimi.
- **Rate Limiting:** IP tabanlı istek sınırlama ile temel Brute-Force koruması.
- **Connection Pooling:** Veritabanı kaynaklarının verimli kullanımı.
- **Fail-Fast Configuration:** Validator entegrasyonu ile eksik yapılandırmada anında durma.
- **Structured Logging:** `log/slog` ile JSON formatında sistem kayıtları.

---

## 🗂️ Proje Dizin Yapısı

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

## 📋 Önkoşullar & Bağımlılıklar

Servisin çalışabilmesi için aşağıdaki bileşenlerin hazır olması gerekmektedir:
- **Go 1.22+**
- **PostgreSQL 15+** (Docker üzerinden veya yerel kurulum)

---

## 🚀 Kurulum & Çalıştırma Rehberi

İlk kurulum esnasında `.env.example` dosyasını referans alarak kök dizinde bir `.env` dosyası oluşturun. 

**1. Veritabanını Hazırlama**

Eğer sisteminizde Docker yüklü ise, `db.sh` betiği ile izole bir PostgreSQL ortamını saniyeler içinde kurabilirsiniz. Eğer yerel bir Postgres kullanıyorsanız, `.env` dosyasındaki bağlantı bilgilerini buna göre güncelleyin.

```bash
# Docker ortamını hazırlamak için (opsiyonel):
sh db.sh reset
```

**2. Projeyi Başlatma**

```bash
go run ./cmd/main.go
# VEYA derleyip çalıştırabilirsiniz:
go build -o bin/app ./cmd/main.go && ./bin/app
```

---

## 📐 Endpoints (API V1 Spesifikasyonları)

| İsim | Method | Endpoint | İçerik (Payload / Header) | Yetki |
|---|---|---|---|---|
| **Sağlık Testi**| GET | `/health` | Servis ve DB bağlantı durumunu döner | 🔴 Yok |
| **Kayıt Ol** | POST | `/api/v1/auth/register` | `{"email", "password", "full_name"}` | 🔴 Yok |
| **Giriş Yap** | POST | `/api/v1/auth/login` | `{"email", "password"}` | 🔴 Yok |
| **Token Yenile** | POST | `/api/v1/auth/refresh` | `{"refresh_token"}` | 🔴 Yok |
| **Çıkış**| POST | `/api/v1/auth/logout` | `{"refresh_token"}` | 🔴 Yok |
| **Profil(Ben)**| GET | `/api/v1/auth/me` | `Authorization: Bearer <Access_Token>` | 🟢 Gerekli |

---

## 🧪 Test Ortamı

Gerçek hayattaki güvenlik senaryoları `Mock Repository` üzerinden sahte objelerle **Unit Test** edilmiştir.

**Birim Testleri Çalıştırmak İçin:**
```bash
go test ./internal/service/... -v
```

Çalışan bir sistem üzerinde **Uçtan Uca (E2E) Test** başlatmak için:
```bash
# Register, Login, Me, Refresh döngüsünün tam testi:
bash test.sh
```
