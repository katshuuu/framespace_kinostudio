#!/usr/bin/env bash
# Нагрузочное тестирование (ApacheBench + параллельный curl).
set -euo pipefail

BASE="${BASE_URL:-http://127.0.0.1:8080}"
OUT_DIR="${OUT_DIR:-$(cd "$(dirname "$0")/../results" && pwd)}"
mkdir -p "$OUT_DIR"

EMAIL="${BOOTSTRAP_ADMIN_EMAIL:-admin@example.org}"
PASS="${BOOTSTRAP_ADMIN_PASSWORD:-DemoPassw0rd!x}"
# подхватить актуальный пароль из .env после functional-tests
ENVF="$(cd "$(dirname "$0")/../.." && pwd)/.env"
if [[ -f "$ENVF" ]]; then
  EMAIL="$(grep -E '^BOOTSTRAP_ADMIN_EMAIL=' "$ENVF" | cut -d= -f2- | tr -d '"' || true)"
  PASS="$(grep -E '^BOOTSTRAP_ADMIN_PASSWORD=' "$ENVF" | cut -d= -f2- | tr -d '"' || true)"
  EMAIL="${EMAIL:-admin@example.org}"
  PASS="${PASS:-DemoPassw0rd!x}"
fi

echo "=== Load tests against $BASE ==="

# Login for authenticated scenario
curl -s -o /tmp/fs_l_login.json -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"clientType\":\"NATIVE\"}" \
  "$BASE/api/auth/login" >/dev/null || true
TOKEN="$(python3 - <<'PY'
import json
try:
  print(json.load(open('/tmp/fs_l_login.json')).get('accessToken',''))
except Exception:
  print('')
PY
)"

# 1. Health: 500 requests, concurrency 25
ab -n 500 -c 25 -q "$BASE/actuator/health" > "$OUT_DIR/ab-health.txt" 2>&1 || true

# 2. Static UI: 200 requests, concurrency 20
ab -n 200 -c 20 -q "$BASE/" > "$OUT_DIR/ab-ui.txt" 2>&1 || true

# 3. Authenticated API (if token available)
if [[ -n "$TOKEN" ]]; then
  # ApacheBench header file
  printf 'Authorization: Bearer %s\n' "$TOKEN" > /tmp/fs_ab_auth.h
  ab -n 200 -c 20 -q -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/clients?size=10" > "$OUT_DIR/ab-clients.txt" 2>&1 || true
  ab -n 200 -c 20 -q -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/requests?size=10" > "$OUT_DIR/ab-requests.txt" 2>&1 || true
else
  echo "SKIP authenticated ab: no token" | tee "$OUT_DIR/ab-clients.txt"
fi

# 4. Burst login attempts с несуществующим email — не блокируем реального админа.
printf '{"email":"attacker@example.invalid","password":"WrongPassword!1","clientType":"NATIVE"}' \
  > /tmp/fs_login_body.json
ab -n 20 -c 4 -q -p /tmp/fs_login_body.json -T 'application/json' \
  "$BASE/api/auth/login" > "$OUT_DIR/ab-login-fail.txt" 2>&1 || true

# Summarize key metrics
python3 - <<PY
import re, pathlib, json
out = pathlib.Path("$OUT_DIR")
summary = {"timestamp": __import__("datetime").datetime.utcnow().isoformat()+"Z", "scenarios": []}

def parse(path, name):
    text = path.read_text(errors="ignore") if path.exists() else ""
    def grab(pat):
        m = re.search(pat, text)
        return m.group(1).strip() if m else None
    sc = {
        "name": name,
        "file": path.name,
        "complete_requests": grab(r"Complete requests:\\s+(\\d+)"),
        "failed_requests": grab(r"Failed requests:\\s+(\\d+)"),
        "requests_per_sec": grab(r"Requests per second:\\s+([0-9.]+)"),
        "time_per_request_mean_ms": grab(r"Time per request:\\s+([0-9.]+)\\s+\\[ms\\]\\s+\\(mean\\)"),
        "transfer_rate_kb_s": grab(r"Transfer rate:\\s+([0-9.]+)"),
        "non2xx": grab(r"Non-2xx responses:\\s+(\\d+)"),
    }
    summary["scenarios"].append(sc)

parse(out/"ab-health.txt", "health_500x25")
parse(out/"ab-ui.txt", "ui_200x20")
parse(out/"ab-clients.txt", "clients_auth_200x20")
parse(out/"ab-requests.txt", "requests_auth_200x20")
parse(out/"ab-login-fail.txt", "login_fail_20x4")
(out/"load-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2))
print(json.dumps(summary, ensure_ascii=False, indent=2))
PY

echo "Load test artifacts saved to $OUT_DIR"
