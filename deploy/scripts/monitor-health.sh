#!/usr/bin/env bash
# Разовый / периодический опрос health для мониторинга работоспособности.
set -euo pipefail

BASE_HTTP="${BASE_HTTP:-http://127.0.0.1:8080}"
BASE_HTTPS="${BASE_HTTPS:-https://127.0.0.1:8443}"
OUT_DIR="${OUT_DIR:-$(cd "$(dirname "$0")/../results" && pwd)}"
mkdir -p "$OUT_DIR"

TS="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
HTTP_CODE="$(curl -s -o /tmp/fs_health_http.json -w '%{http_code}' "$BASE_HTTP/actuator/health" || echo 000)"
HTTPS_CODE="$(curl -sk -o /tmp/fs_health_https.json -w '%{http_code}' "$BASE_HTTPS/actuator/health" || echo 000)"

{
  echo "{"
  echo "  \"timestamp\": \"$TS\","
  echo "  \"http\": { \"url\": \"$BASE_HTTP/actuator/health\", \"status\": $HTTP_CODE, \"body\": $(cat /tmp/fs_health_http.json 2>/dev/null || echo 'null') },"
  echo "  \"https\": { \"url\": \"$BASE_HTTPS/actuator/health\", \"status\": $HTTPS_CODE, \"body\": $(cat /tmp/fs_health_https.json 2>/dev/null || echo 'null') }"
  echo "}"
} | tee "$OUT_DIR/health-snapshot.json"

echo "HTTP=$HTTP_CODE HTTPS=$HTTPS_CODE" | tee "$OUT_DIR/health-snapshot.txt"
