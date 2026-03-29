# Courier Microservices

This monorepo contains two core services:

- [auth-service/](auth-service/): authentication, JWT issuance, refresh token flow (Go + Echo)
- [order-service/](order-service/): order lifecycle management (Spring Boot)

The project follows a script-first local workflow. There is no Docker Compose dependency for daily development.

## Services

| Service | Port | Health Endpoint |
|---|---|---|
| Auth Service | 8081 | `/health` |
| Order Service | 8082 | `/actuator/health` |

## Quick Start

From the repository root:

```bash
sh scripts/up.sh
```

This script:

- ensures `auth_postgres` and `order_postgres` containers are running
- runs auth DB migration
- ensures `order_rabbitmq` is running
- starts both services
- waits for health checks before finishing

## Integration Smoke Test

With services running:

```bash
sh scripts/smoke.sh
```

This validates an end-to-end flow:

- register
- login
- unauthorized order access check
- create/get/update/cancel order
- refresh token

## Stop Everything

```bash
sh scripts/down.sh
```

To also remove containers:

```bash
REMOVE_CONTAINERS=1 sh scripts/down.sh
```

## Postman

Ready-to-import files are available in [postman/courier-microservices.collection.json](postman/courier-microservices.collection.json) and [postman/courier-local.environment.json](postman/courier-local.environment.json).

## Service-Specific Docs

- [auth-service/README.md](auth-service/README.md)
- [order-service/README.md](order-service/README.md)
