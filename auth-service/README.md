# Auth Service

Auth Service provides user authentication and token management using Go + Echo.

It supports:

- user registration
- login (access token + refresh token)
- refresh token rotation
- logout
- protected profile endpoint (`/me`)

## Recommended Local Workflow

From repository root:

```bash
sh scripts/up.sh
sh scripts/smoke.sh
sh scripts/down.sh
```

## Architecture

The service follows layered clean architecture:

- `handler`: HTTP request/response and validation
- `service`: business logic and token rules
- `repository`: PostgreSQL access
- `middleware`: JWT verification for protected routes

## Project Structure

```text
auth-service/
├── cmd/
│   └── main.go
├── internal/
│   ├── config/
│   ├── domain/
│   ├── handler/
│   ├── middleware/
│   ├── repository/
│   └── service/
├── db.sh
├── test.sh
└── api.http
```

## Prerequisites

- Go 1.22+
- PostgreSQL 15+ (Docker or local)

## Configuration

Create `.env` from `.env.example` and update values if needed.

Common settings include:

- DB connection info
- JWT secret
- access/refresh token TTLs
- server port

## Run Service Only

1. Prepare DB:

```bash
sh db.sh reset
```

2. Run service:

```bash
go run ./cmd/main.go
```

Optional binary run:

```bash
go build -o bin/app ./cmd/main.go && ./bin/app
```

## API Endpoints

Base path: `/api/v1/auth`

| Name | Method | Endpoint | Auth Required |
|---|---|---|---|
| Health | GET | `/health` | No |
| Register | POST | `/api/v1/auth/register` | No |
| Login | POST | `/api/v1/auth/login` | No |
| Refresh | POST | `/api/v1/auth/refresh` | No |
| Logout | POST | `/api/v1/auth/logout` | No |
| Me | GET | `/api/v1/auth/me` | Yes |

### Request/Response Notes

- Token response fields use snake_case: `access_token`, `refresh_token`
- Error contract: `error`, `code`, optional `details`
- Validation errors return `code=VALIDATION_ERROR`

## Testing

Run unit tests:

```bash
go test ./internal/service/... -v
```

Run auth-only HTTP script:

```bash
bash test.sh
```

Run full integration smoke from repo root:

```bash
sh scripts/smoke.sh
```
