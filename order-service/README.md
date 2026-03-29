# Order Service

Order Service manages order lifecycle operations using Spring Boot and clean architecture.

## Architecture

- `domain`: entities, business rules, domain ports
- `application`: use cases and DTOs
- `infrastructure`: JPA persistence and RabbitMQ publishers
- `presentation`: REST API, request validation, JWT user filter

## Recommended Local Workflow

From repository root:

```bash
sh scripts/up.sh
sh scripts/smoke.sh
```

## Run Order Service Only

1. Prepare environment file:

```bash
cp .env.example .env
```

2. Prepare order DB:

```bash
sh db.sh reset
```

3. Start RabbitMQ:

```bash
docker rm -f order_rabbitmq 2>/dev/null || true
docker run -d --name order_rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4.2.2-management
```

4. Run service:

```bash
mvn spring-boot:run
```

## API

Base URL: `http://localhost:8082/api/v1/orders`

| Name | Method | Endpoint |
|---|---|---|
| Create Order | POST | `/api/v1/orders` |
| Get Order | GET | `/api/v1/orders/{id}` |
| Change Status | PATCH | `/api/v1/orders/{id}/status` |
| Cancel Order | DELETE | `/api/v1/orders/{id}` |

All endpoints require:

`Authorization: Bearer <jwt>`

JWT must include `user_id` claim.

### Response Contract

Order response fields are standardized to snake_case:

- `id`
- `user_id`
- `status`
- `total_amount`
- `created_at`
- `updated_at`
- `items[].product_id`
- `items[].unit_price`

Error contract:

- `error`
- `code`
- optional `details`

## Messaging

Order service publishes events to RabbitMQ for major lifecycle actions:

- order placed
- order confirmed/delivered transitions
- order cancelled

RabbitMQ management UI (default): `http://localhost:15672`

## Testing

Run unit/integration tests:

```bash
mvn test
```

Run service-level script test:

```bash
sh test.sh
```

Run full cross-service smoke from repo root:

```bash
sh scripts/smoke.sh
```

## Troubleshooting

- `401` on order endpoints without token is expected.
- If startup fails with port conflicts, run:

```bash
sh scripts/down.sh
sh scripts/up.sh
```

- If DB or RabbitMQ state is inconsistent, restart containers with `db.sh reset` and recreate RabbitMQ container.
