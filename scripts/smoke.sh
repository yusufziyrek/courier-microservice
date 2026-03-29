#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v curl >/dev/null 2>&1; then
  echo "[smoke][FAIL] curl is required"
  exit 1
fi

if [[ ! -f "$ROOT_DIR/order-service/test.sh" ]]; then
  echo "[smoke][FAIL] missing test script: $ROOT_DIR/order-service/test.sh"
  exit 1
fi

echo "[smoke] running auth + order smoke flow"

auth_code="$(curl -s -o /tmp/auth_smoke_health.json -w "%{http_code}" http://localhost:8081/health || true)"
order_code="$(curl -s -o /tmp/order_smoke_health.json -w "%{http_code}" http://localhost:8082/actuator/health || true)"

if [[ "$auth_code" != "200" || "$order_code" != "200" ]]; then
  echo "[smoke][FAIL] services are not healthy"
  echo "[smoke] auth health status: $auth_code"
  echo "[smoke] order health status: $order_code"
  echo "[smoke] start services first with: sh scripts/up.sh"
  exit 1
fi

bash "$ROOT_DIR/order-service/test.sh"

echo "[smoke] completed successfully"
