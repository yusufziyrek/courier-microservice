#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
mkdir -p "$RUN_DIR"

wait_http() {
  local url="$1"
  local label="$2"
  local tries=60

  while (( tries > 0 )); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
    tries=$((tries - 1))
  done

  echo "[up][FAIL] $label not ready: $url"
  exit 1
}

listening_pid_by_port() {
  local port="$1"
  netstat -ano 2>/dev/null | awk -v suffix=":""$port" '$0 ~ /LISTENING/ && $2 ~ suffix"$" { print $5; exit }'
}

refresh_pid_file_from_port() {
  local port="$1"
  local pid_file="$2"
  local label="$3"
  local pid

  pid="$(listening_pid_by_port "$port" || true)"
  if [[ -n "$pid" ]]; then
    echo "$pid" >"$pid_file"
    echo "[up] $label healthy on port $port (pid=$pid)"
  else
    echo "[up] $label healthy on port $port"
  fi
}

start_if_not_running() {
  local health_url="$1"
  local pid_file="$2"
  local cmd="$3"
  local log_file="$4"
  local port="$5"
  local label="$6"

  if curl -fsS "$health_url" >/dev/null 2>&1; then
    refresh_pid_file_from_port "$port" "$pid_file" "$label"
    return 0
  fi

  local port_pid
  port_pid="$(listening_pid_by_port "$port" || true)"
  if [[ -n "$port_pid" ]]; then
    echo "[up] port $port already in use (pid=$port_pid), waiting for health: $health_url"
    return 0
  fi

  if [[ -f "$pid_file" ]]; then
    local old_pid
    old_pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [[ -n "$old_pid" ]] && kill -0 "$old_pid" >/dev/null 2>&1; then
      echo "[up] process exists (pid=$old_pid), waiting for health: $health_url"
      return 0
    fi
  fi

  echo "[up] starting: $cmd"
  nohup bash -lc "$cmd" >"$log_file" 2>&1 &
  echo $! >"$pid_file"
}

ensure_db_container() {
  local container_name="$1"
  local create_cmd="$2"

  if docker ps --format '{{.Names}}' | grep -qx "$container_name"; then
    echo "[up] db already running: $container_name"
    return 0
  fi

  if docker ps -a --format '{{.Names}}' | grep -qx "$container_name"; then
    echo "[up] starting existing db container: $container_name"
    docker start "$container_name" >/dev/null
    return 0
  fi

  echo "[up] creating db container: $container_name"
  bash -lc "$create_cmd"
}

echo "[up] starting databases"
ensure_db_container "auth_postgres" "cd '$ROOT_DIR/auth-service' && ./db.sh create >/dev/null"
ensure_db_container "order_postgres" "cd '$ROOT_DIR/order-service' && ./db.sh start >/dev/null"

# auth-service tablolari her acilista garanti edilir
(cd "$ROOT_DIR/auth-service" && ./db.sh migrate)

echo "[up] ensuring rabbitmq"
if docker ps --format '{{.Names}}' | grep -qx 'order_rabbitmq'; then
  echo "[up] rabbitmq already running"
elif docker ps -a --format '{{.Names}}' | grep -qx 'order_rabbitmq'; then
  docker start order_rabbitmq >/dev/null
else
  docker run -d --name order_rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4.2.2-management >/dev/null
fi

echo "[up] starting services"
start_if_not_running \
  "http://localhost:8081/health" \
  "$RUN_DIR/auth-service.pid" \
  "cd '$ROOT_DIR/auth-service' && exec go run ./cmd/main.go" \
  "$RUN_DIR/auth-service.log" \
  "8081" \
  "auth-service"

start_if_not_running \
  "http://localhost:8082/actuator/health" \
  "$RUN_DIR/order-service.pid" \
  "cd '$ROOT_DIR/order-service' && exec mvn spring-boot:run -q" \
  "$RUN_DIR/order-service.log" \
  "8082" \
  "order-service"

echo "[up] waiting for services to be healthy"
wait_http "http://localhost:8081/health" "auth-service"
wait_http "http://localhost:8082/actuator/health" "order-service"

# Keep pid files aligned with the real listener process IDs.
refresh_pid_file_from_port "8081" "$RUN_DIR/auth-service.pid" "auth-service"
refresh_pid_file_from_port "8082" "$RUN_DIR/order-service.pid" "order-service"

echo "[up] auth-service:  http://localhost:8081/health"
echo "[up] order-service: http://localhost:8082/actuator/health"
echo "[up] rabbitmq ui:   http://localhost:15672"
echo "[up] logs:          $RUN_DIR/auth-service.log"
echo "[up] logs:          $RUN_DIR/order-service.log"
echo "[up] done (both services are up)"
