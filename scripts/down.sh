#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
REMOVE_CONTAINERS="${REMOVE_CONTAINERS:-0}"

listening_pid_by_port() {
  local port="$1"
  netstat -ano 2>/dev/null | awk -v suffix=":""$port" '$0 ~ /LISTENING/ && $2 ~ suffix"$" { print $5; exit }'
}

kill_pid_file() {
  local pid_file="$1"
  local label="$2"

  if [[ -f "$pid_file" ]]; then
    echo "[down] removing pid file for $label"
    rm -f "$pid_file"
  else
    echo "[down] no pid file for $label"
  fi
}

terminate_pid() {
  local pid="$1"

  if command -v taskkill >/dev/null 2>&1; then
    taskkill //PID "$pid" //T >/dev/null 2>&1 || true
    sleep 1
    taskkill //PID "$pid" //T //F >/dev/null 2>&1 || true
  else
    kill "$pid" >/dev/null 2>&1 || true

    local tries=10
    while (( tries > 0 )) && kill -0 "$pid" >/dev/null 2>&1; do
      sleep 1
      tries=$((tries - 1))
    done

    if kill -0 "$pid" >/dev/null 2>&1; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
  fi
}

kill_port_listener_if_running() {
  local port="$1"
  local label="$2"
  local pid

  pid="$(listening_pid_by_port "$port" || true)"
  if [[ -z "$pid" ]]; then
    echo "[down] no listener on port $port for $label"
    return 0
  fi

  terminate_pid "$pid"
  echo "[down] stopped $label by port $port (pid=$pid)"
}

stop_container_if_running() {
  local name="$1"
  if docker ps --format '{{.Names}}' | grep -qx "$name"; then
    docker stop "$name" >/dev/null
    echo "[down] stopped container: $name"
  else
    echo "[down] container not running: $name"
  fi

  if [[ "$REMOVE_CONTAINERS" == "1" ]] && docker ps -a --format '{{.Names}}' | grep -qx "$name"; then
    docker rm "$name" >/dev/null
    echo "[down] removed container: $name"
  fi
}

echo "[down] stopping services"
kill_pid_file "$RUN_DIR/auth-service.pid" "auth-service"
kill_pid_file "$RUN_DIR/order-service.pid" "order-service"
kill_port_listener_if_running "8081" "auth-service"
kill_port_listener_if_running "8082" "order-service"

echo "[down] stopping containers"
stop_container_if_running "order_rabbitmq"
stop_container_if_running "order_postgres"
stop_container_if_running "auth_postgres"

if [[ "$REMOVE_CONTAINERS" == "1" ]]; then
  echo "[down] containers were removed (REMOVE_CONTAINERS=1)"
fi

echo "[down] done"
