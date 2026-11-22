#!/usr/bin/env bash
# Деплой FrameSpace для КР №3 СПО: backend + PostgreSQL + nginx(TLS) + health-monitor.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BACKEND_DIR="$REPO_ROOT/backend"
ENV_FILE="$REPO_ROOT/.env"
COMPOSE_BASE="$BACKEND_DIR/docker-compose.yml"
COMPOSE_SPO="$REPO_ROOT/deploy/docker-compose.spo.yml"
WAIT_TIMEOUT="${1:-420}"

cd "$REPO_ROOT"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Создаю .env из .env.example..."
  cp "$REPO_ROOT/.env.example" "$ENV_FILE"
fi

# Для HTTPS-прокси добавляем origin, если его ещё нет.
if ! grep -q 'https://localhost:8443' "$ENV_FILE"; then
  if grep -q '^CORS_ALLOWED_ORIGINS=' "$ENV_FILE"; then
    # shellcheck disable=SC2016
    sed -i.bak 's|^CORS_ALLOWED_ORIGINS=.*|CORS_ALLOWED_ORIGINS=http://localhost:8080,https://localhost:8443|' "$ENV_FILE"
    rm -f "$ENV_FILE.bak"
  fi
fi

"$REPO_ROOT/deploy/scripts/generate-ssl.sh"

echo "Останавливаю предыдущий стек (том БД сохраняется)..."
docker compose --env-file "$ENV_FILE" \
  -f "$COMPOSE_BASE" -f "$COMPOSE_SPO" \
  down --remove-orphans >/dev/null 2>&1 || true

echo "Запускаю стек КР №3 СПО..."
docker compose --env-file "$ENV_FILE" \
  -f "$COMPOSE_BASE" -f "$COMPOSE_SPO" \
  up -d --force-recreate --wait --wait-timeout "$WAIT_TIMEOUT"

APP_PORT="$(grep -E '^APP_PORT=' "$ENV_FILE" | cut -d= -f2- || true)"
APP_PORT="${APP_PORT:-8080}"
ADMIN="$(grep -E '^BOOTSTRAP_ADMIN_EMAIL=' "$ENV_FILE" | cut -d= -f2- || true)"

cat <<EOF

============================================================
 FRAMESPACE SPO DEPLOY READY
============================================================
 Direct HTTP UI:   http://localhost:${APP_PORT}/
 HTTPS via nginx:  https://localhost:8443/
 HTTP→HTTPS:       http://localhost:8088/  (redirect)
 Health (direct):  http://localhost:${APP_PORT}/actuator/health
 Health (TLS):     https://localhost:8443/actuator/health
 Admin login:      ${ADMIN:-admin@example.org}
============================================================
 Сертификат самоподписанный — в браузере подтвердите исключение.
============================================================

EOF
