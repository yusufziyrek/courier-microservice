# Order Service

Order service strict clean architecture prensipleriyle geliştirildi:

- `domain`: business kuralları, entity ve portlar
- `application`: use-case katmanı
- `infrastructure`: JPA ve RabbitMQ adapterları
- `presentation`: HTTP endpointleri ve JWT filtresi

## En Kolay Çalıştırma (Önerilen)

Bu servis auth-service ile birlikte çalışır. Repo kökünden şu akış önerilir:

```bash
sh scripts/up.sh
sh scripts/smoke.sh
```

Bu akışta order-service otomatik olarak ayağa kalkar.

## Sadece Order Service Çalıştırma

Order servisini bağımsız denemek için:

1. Veritabanı containerını hazırla:

```bash
cp .env.example .env
sh db.sh reset
```

2. RabbitMQ başlat:

```bash
docker rm -f order_rabbitmq 2>/dev/null || true
docker run -d --name order_rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4.2.2-management
```

3. Servisi başlat:

```bash
mvn spring-boot:run
```

## API

Base URL: `http://localhost:8082/api/v1/orders`

- `POST /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `PATCH /api/v1/orders/{id}/status`
- `DELETE /api/v1/orders/{id}`

Tüm endpointler JWT gerektirir:

`Authorization: Bearer <jwt>`

JWT içinde `user_id` claim bulunmalıdır.

## Test

Unit/integration test:

```bash
mvn test
```

Auth ile birlikte e2e benzeri smoke test:

```bash
sh test.sh
```
