# Courier Microservices

Bu monorepo, birbirinden bağımsız çalışan iki temel servisten oluşur:

- [auth-service/](auth-service/): Go + Echo ile kimlik doğrulama ve JWT üretimi
- [order-service/](order-service/): Spring Boot ile sipariş yönetimi

Sistem script-first yaklaşımla sade tutuldu. Docker Compose yok; tüm temel operasyonlar scriptlerle yönetilir.

## Servisler

| Servis | Port | Sağlık Endpointi |
|---|---|---|
| Auth Service | 8081 | `/health` |
| Order Service | 8082 | `/actuator/health` |

## Hızlı Başlangıç

Repo kök dizininden:

```bash
sh scripts/up.sh
```

Bu komut şu işlemleri yapar:

- `auth_postgres` ve `order_postgres` containerlarını hazırlar/başlatır
- auth-service migration çalıştırır
- `order_rabbitmq` containerını başlatır
- auth-service ve order-service'i ayağa kaldırır
- health endpointleri ile servisleri doğrular

## Smoke Test

Sistem ayaktayken tek komutla auth + order akışını test etmek için:

```bash
sh scripts/smoke.sh
```

Bu komut [order-service/test.sh](order-service/test.sh) üzerinden şu akışı doğrular:

- register
- login
- order create/get/update/cancel
- refresh token

## Sistemi Durdurma

```bash
sh scripts/down.sh
```

Containerları da silmek istersen:

```bash
REMOVE_CONTAINERS=1 sh scripts/down.sh
```

## Servisleri Ayrı Çalıştırma

Sadece auth-service detayları için [auth-service/README.md](auth-service/README.md),
sadece order-service detayları için [order-service/README.md](order-service/README.md) dosyasına bak.
