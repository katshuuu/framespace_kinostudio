#!/usr/bin/env bash
set -euo pipefail

BACKEND_ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$BACKEND_ROOT/.." && pwd)"
COMPOSE_FILE="$BACKEND_ROOT/docker-compose.yml"
ENV_FILE="$REPO_ROOT/.env"
WAIT_TIMEOUT="${1:-420}"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "docker-compose.yml not found: $COMPOSE_FILE" >&2
  exit 1
fi

if ! grep -q "FRAMESPACE_COMPOSE_V3" "$COMPOSE_FILE"; then
  echo "Wrong/old docker-compose.yml. Expected marker FRAMESPACE_COMPOSE_V3." >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo ".env not found at repo root. Copy .env.example to .env and fill secrets:" >&2
  echo "  cp \"$REPO_ROOT/.env.example\" \"$ENV_FILE\"" >&2
  exit 1
fi

cd "$BACKEND_ROOT"

echo "Stopping stale containers (database volume is preserved)..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down --remove-orphans

echo "Starting FrameSpace backend..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --force-recreate --wait --wait-timeout "$WAIT_TIMEOUT"

APP_PORT="$(grep -E '^APP_PORT=' "$ENV_FILE" | cut -d= -f2- || true)"
APP_PORT="${APP_PORT:-8080}"
POSTGRES_PORT="$(grep -E '^POSTGRES_PORT=' "$ENV_FILE" | cut -d= -f2- || true)"
POSTGRES_PORT="${POSTGRES_PORT:-55432}"
POSTGRES_DB="$(grep -E '^POSTGRES_DB=' "$ENV_FILE" | cut -d= -f2- || true)"
POSTGRES_DB="${POSTGRES_DB:-framespace}"
ADMIN="$(grep -E '^BOOTSTRAP_ADMIN_EMAIL=' "$ENV_FILE" | cut -d= -f2- || true)"

cat <<EOF

============================================================
 FRAMESPACE IS READY
============================================================
 Web UI:           http://localhost:${APP_PORT}/
 Swagger UI:       http://localhost:${APP_PORT}/swagger-ui.html
 REST API:         http://localhost:${APP_PORT}/api
 OpenAPI JSON:     http://localhost:${APP_PORT}/v3/api-docs
 Health:           http://localhost:${APP_PORT}/actuator/health
 PostgreSQL JDBC:  jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}
 Admin login:      ${ADMIN:-admin@example.org}
============================================================

EOF
