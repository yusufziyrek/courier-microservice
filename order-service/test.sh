#!/usr/bin/env bash
set -euo pipefail

AUTH_BASE_URL="${AUTH_BASE_URL:-http://localhost:8081/api/v1/auth}"
ORDER_BASE_URL="${ORDER_BASE_URL:-http://localhost:8082/api/orders}"

EMAIL="ordertest$(date +%s)@example.com"
PASSWORD="secret123"
FULL_NAME="Order Tester"

ORDER_ID=""

print_step() {
  echo ""
  echo "====================================="
  echo "$1"
  echo "====================================="
}

fail() {
  echo "[FAIL] $1"
  exit 1
}

extract_json_value() {
  local key="$1"
  local json="$2"
    echo "$json" \
      | grep -o "\"$key\":\"[^\"]*\"" \
      | head -n 1 \
      | sed -n "s/\"$key\":\"\([^\"]*\)\"/\1/p"
}

assert_status() {
  local actual="$1"
  local expected="$2"
  local label="$3"
  if [[ "$actual" != "$expected" ]]; then
    fail "$label - expected status $expected but got $actual"
  fi
}

print_step "0. HEALTH CHECK"
AUTH_HEALTH=$(curl -s -o /tmp/auth_health.json -w "%{http_code}" http://localhost:8081/health || true)
ORDER_HEALTH=$(curl -s -o /tmp/order_health.json -w "%{http_code}" http://localhost:8082/actuator/health || true)
assert_status "$AUTH_HEALTH" "200" "auth-service health"
assert_status "$ORDER_HEALTH" "200" "order-service health"
echo "auth-service: OK"
echo "order-service: OK"

print_step "1. REGISTER (AUTH-SERVICE)"
REGISTER_BODY="{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"full_name\":\"$FULL_NAME\"}"
REGISTER_STATUS=$(curl -s -o /tmp/register_resp.json -w "%{http_code}" -X POST "$AUTH_BASE_URL/register" -H "Content-Type: application/json" -d "$REGISTER_BODY")
assert_status "$REGISTER_STATUS" "201" "auth register"
cat /tmp/register_resp.json

print_step "2. LOGIN (AUTH-SERVICE)"
LOGIN_BODY="{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
LOGIN_RESP=$(curl -s -X POST "$AUTH_BASE_URL/login" -H "Content-Type: application/json" -d "$LOGIN_BODY")
ACCESS_TOKEN=$(extract_json_value "access_token" "$LOGIN_RESP")
REFRESH_TOKEN=$(extract_json_value "refresh_token" "$LOGIN_RESP")
[[ -n "$ACCESS_TOKEN" ]] || fail "access token is empty"
[[ -n "$REFRESH_TOKEN" ]] || fail "refresh token is empty"
echo "$LOGIN_RESP"

print_step "3. ORDER ENDPOINT AUTH CHECK (NO TOKEN)"
UNAUTH_STATUS=$(curl -s -o /tmp/order_unauth.json -w "%{http_code}" "$ORDER_BASE_URL" || true)
assert_status "$UNAUTH_STATUS" "401" "order unauthorized check"
cat /tmp/order_unauth.json

print_step "4. CREATE ORDER (ORDER-SERVICE)"
CREATE_BODY='{"items":[{"productId":"22222222-2222-2222-2222-222222222222","quantity":2,"unitPrice":15.50}]}'
CREATE_STATUS=$(curl -s -o /tmp/order_create.json -w "%{http_code}" -X POST "$ORDER_BASE_URL" -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" -d "$CREATE_BODY")
assert_status "$CREATE_STATUS" "201" "order create"
CREATE_RESP=$(cat /tmp/order_create.json)
ORDER_ID=$(extract_json_value "id" "$CREATE_RESP")
[[ -n "$ORDER_ID" ]] || fail "order id missing in create response"
echo "$CREATE_RESP"
echo "ORDER_ID: $ORDER_ID"

print_step "5. GET ORDER"
GET_STATUS=$(curl -s -o /tmp/order_get.json -w "%{http_code}" -H "Authorization: Bearer $ACCESS_TOKEN" "$ORDER_BASE_URL/$ORDER_ID")
assert_status "$GET_STATUS" "200" "order get"
cat /tmp/order_get.json

print_step "6. CHANGE STATUS -> CONFIRMED"
PATCH_STATUS=$(curl -s -o /tmp/order_patch.json -w "%{http_code}" -X PATCH "$ORDER_BASE_URL/$ORDER_ID/status" -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" -d '{"status":"CONFIRMED"}')
assert_status "$PATCH_STATUS" "200" "order patch"
cat /tmp/order_patch.json

print_step "7. CANCEL ORDER"
DELETE_STATUS=$(curl -s -o /tmp/order_delete.json -w "%{http_code}" -X DELETE "$ORDER_BASE_URL/$ORDER_ID" -H "Authorization: Bearer $ACCESS_TOKEN")
assert_status "$DELETE_STATUS" "204" "order delete"
echo "cancel status: $DELETE_STATUS"

print_step "8. REFRESH TOKEN (AUTH-SERVICE)"
REFRESH_BODY="{\"refresh_token\":\"$REFRESH_TOKEN\"}"
REFRESH_STATUS=$(curl -s -o /tmp/refresh_resp.json -w "%{http_code}" -X POST "$AUTH_BASE_URL/refresh" -H "Content-Type: application/json" -d "$REFRESH_BODY")
assert_status "$REFRESH_STATUS" "200" "auth refresh"
cat /tmp/refresh_resp.json

echo ""
echo "All order-service smoke checks passed successfully."
