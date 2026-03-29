# 🔐 Auth Service (Courier Microservice)

Canlı kullanıma (**Production-Ready**) hazır, Echo v5 ve Go rutinleri üzerine inşa edilmiş yüksek performanslı Kimlik Doğrulama (Authentication) mikroservisidir.

## 🌟 Öne Çıkan Özellikler & Mimari Best-Practice'ler

Bu servis, kurumsal seviye bir güvenliği ve ayakta kalma kabiliyetini sağlamak amacıyla katı kurallarla yazılmıştır:

- **Clean Architecture (3 Katmanlı):** `Handler`, `Service`, `Repository` yapıları ayrıştırılmış; `Domain` entity'leri merkeze alınarak bağımlılıklar koptarılmıştır.
- **Token Rotasyonu (JWT):** Sıradan JWT kullanımından farklı olarak **Opaque** *Refresh Token* yapısı kurulmuştur. Her yenileme (Refresh) işleminde eski token güvenlik gereği veri tabanından kalıcı olarak yok edilir.
- **Kalkan & Rate Limiting:** Kaba kuvvet (Brute-Force) sözlük saldırılarını engellemek adına IP başına saniyede belirli limite sahip Memory Store Rate-Limiter mevcuttur.
- **Veritabanı Havuzu (Connection Pooling):** Ağır trafik patlamalarında veritabanını felç etmemek için `MaxOpenConns` ve `MaxIdleConns` devreye alınmıştır.
- **Fail-Fast Doğrulama (Validator):** Kritik sistem konfigürasyonları (örn: 16 byte algoritmik `JWT_SECRET`) `.env` üzerinden eksik verilirse, sistem yarım çalışmak yerine kendini doğrudan baştan durdurur.
- **Bcrypt & Structured Logging:** Tüm loglar `log/slog` ile JSON formatında kaydedilir ve şifreler "Cost: 12" hash ile saklanır.

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

## 🚀 Kurulum & Çalıştırma Rehberi

İlk kurulum esnasında `.env.example` dosyasını referans alarak kök dizinde bir `.env` dosyası oluşturun. Özellikle `JWT_SECRET` bölümünün rastgele üretilmiş uzun bir Cryptography anahtarı olmasına özen gösterin.

**1. Veritabanını Hazırlama (Docker Gerekir)**

`db.sh` betiğini kullanarak kendinize izole bir PostgreSQL ortamı kurabilirsiniz. Bu sayede bilgisayarınıza Postgres yüklemenize gerek kalmaz.

```bash
# Docker içindeki Auth-DB imajı indirilir, ayağa kalkar, migration ve indexler yaratılır
sh db.sh reset
```

**2. Projeyi Başlatma**

```bash
go run ./cmd/main.go
# VEYA doğrudan derleyip çalıştırabilirsiniz:
go build -o bin/app ./cmd/main.go && ./bin/app
```

---

## 📐 Endpoints (API V1 Spesifikasyonları)

Varsayılan port HTTP `:8081` üzerinden yayın yapar.

| İsim | Method | Endpoint | İçerik (Payload / Header) | Yetki |
|---|---|---|---|---|
| **Sağlık Testi**| GET | `/health` | Kubernetes / Load Balancer canlılık testi | 🔴 Yok |
| **Kayıt Ol** | POST | `/api/v1/auth/register` | `{"email", "password", "full_name"}` | 🔴 Yok |
| **Giriş Yap** | POST | `/api/v1/auth/login` | `{"email", "password"}` - *(Token çifti döner)* | 🔴 Yok |
| **Token Yenile** | POST | `/api/v1/auth/refresh` | `{"refresh_token"}` - *(Eskisi imha olur)* | 🔴 Yok |
| **Çıkış**| POST | `/api/v1/auth/logout` | `{"refresh_token"}` - *(Kalıcı silinme)* | 🔴 Yok |
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
