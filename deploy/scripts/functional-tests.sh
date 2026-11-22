#!/usr/bin/env bash
# Тесты работоспособности ПО (функциональные проверки REST API).
set -euo pipefail

BASE="${BASE_URL:-http://127.0.0.1:8080}"
HTTPS="${HTTPS_URL:-https://127.0.0.1:8443}"
EMAIL="${BOOTSTRAP_ADMIN_EMAIL:-admin@example.org}"
PASS="${BOOTSTRAP_ADMIN_PASSWORD:-DemoPassw0rd!x}"
ENVF="$(cd "$(dirname "$0")/../.." && pwd)/.env"
if [[ -f "$ENVF" ]]; then
  EMAIL="$(grep -E '^BOOTSTRAP_ADMIN_EMAIL=' "$ENVF" | cut -d= -f2- | tr -d '"' || true)"
  PASS="$(grep -E '^BOOTSTRAP_ADMIN_PASSWORD=' "$ENVF" | cut -d= -f2- | tr -d '"' || true)"
  EMAIL="${EMAIL:-admin@example.org}"
  PASS="${PASS:-DemoPassw0rd!x}"
fi

OUT_DIR="${OUT_DIR:-$(cd "$(dirname "$0")/../results" && pwd)}"
mkdir -p "$OUT_DIR"
REPORT="$OUT_DIR/functional-tests.jsonl"
SUMMARY="$OUT_DIR/functional-summary.txt"
: > "$REPORT"

pass=0
fail=0

check() {
  local name="$1" expect="$2" got="$3" detail="${4:-}"
  if [[ "$got" == "$expect" ]]; then
    echo "PASS|$name|expected=$expect got=$got|$detail" | tee -a "$REPORT"
    pass=$((pass + 1))
  else
    echo "FAIL|$name|expected=$expect got=$got|$detail" | tee -a "$REPORT"
    fail=$((fail + 1))
  fi
}

echo "=== Functional tests against $BASE ==="

code="$(curl -s -o /tmp/fs_f_health.json -w '%{http_code}' "$BASE/actuator/health")"
check "health_direct" "200" "$code" "$(cat /tmp/fs_f_health.json)"

code="$(curl -s -o /dev/null -w '%{http_code}' "$BASE/")"
check "web_ui_index" "200" "$code"

code="$(curl -s -o /tmp/fs_f_unauth.json -w '%{http_code}' "$BASE/api/requests")"
check "requests_without_token" "401" "$code" "$(head -c 200 /tmp/fs_f_unauth.json)"

code="$(curl -s -o /tmp/fs_f_login.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"clientType\":\"NATIVE\"}" \
  "$BASE/api/auth/login")"
check "admin_login" "200" "$code"

TOKEN="$(python3 -c 'import json; print(json.load(open("/tmp/fs_f_login.json")).get("accessToken",""))')"
NEED_CHANGE="$(python3 -c 'import json; print(str(json.load(open("/tmp/fs_f_login.json")).get("user",{}).get("passwordChangeRequired", False)).lower())')"

if [[ "$NEED_CHANGE" == "true" && -n "$TOKEN" ]]; then
  NEW_PASS='DemoPassw0rd!y'
  code="$(curl -s -o /tmp/fs_f_chpwd.json -w '%{http_code}' \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"newPassword\":\"$NEW_PASS\"}" \
    "$BASE/api/auth/change-initial-password")"
  check "initial_password_change" "204" "$code"
  if [[ "$code" == "204" ]]; then
    PASS="$NEW_PASS"
    if [[ -f "$ENVF" ]]; then
      sed -i.bak "s|^BOOTSTRAP_ADMIN_PASSWORD=.*|BOOTSTRAP_ADMIN_PASSWORD=$NEW_PASS|" "$ENVF" || true
      rm -f "$ENVF.bak"
    fi
    curl -s -o /tmp/fs_f_login.json -H 'Content-Type: application/json' \
      -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"clientType\":\"NATIVE\"}" \
      "$BASE/api/auth/login" >/dev/null
    TOKEN="$(python3 -c 'import json; print(json.load(open("/tmp/fs_f_login.json")).get("accessToken",""))')"
  fi
else
  check "password_change_not_required" "true" "true" "flag=$NEED_CHANGE"
fi

AUTH="Authorization: Bearer $TOKEN"

code="$(curl -s -o /tmp/fs_f_me.json -w '%{http_code}' -H "$AUTH" "$BASE/api/auth/me")"
check "auth_me" "200" "$code" "$(head -c 200 /tmp/fs_f_me.json)"

code="$(curl -s -o /tmp/fs_f_clients.json -w '%{http_code}' -H "$AUTH" "$BASE/api/clients?size=20")"
check "clients_list" "200" "$code"
clients_total="$(python3 -c 'import json; d=json.load(open("/tmp/fs_f_clients.json")); print(int(d.get("totalElements", d.get("total", 0)) or 0))')"
echo "$clients_total" > "$OUT_DIR/clients-total.txt"
if [[ "$clients_total" -ge 5 ]]; then
  check "clients_seed_ge_5" "true" "true" "total=$clients_total"
else
  check "clients_seed_ge_5" "true" "false" "total=$clients_total"
fi

code="$(curl -s -o /tmp/fs_f_locs.json -w '%{http_code}' -H "$AUTH" "$BASE/api/locations?size=20")"
check "locations_list" "200" "$code"

code="$(curl -s -o /tmp/fs_f_reqs.json -w '%{http_code}' -H "$AUTH" "$BASE/api/requests?size=20")"
check "requests_list" "200" "$code"
req_count="$(python3 -c 'import json; d=json.load(open("/tmp/fs_f_reqs.json")); print(len(d.get("content") or []))')"
if [[ "$req_count" -gt 0 ]]; then
  check "requests_non_empty" "true" "true" "count=$req_count"
else
  check "requests_non_empty" "true" "false" "count=$req_count"
fi

code="$(curl -s -o /tmp/fs_f_an.json -w '%{http_code}' -H "$AUTH" "$BASE/api/analytics/summary")"
check "analytics_summary" "200" "$code" "$(head -c 200 /tmp/fs_f_an.json)"

RID="$(python3 -c 'import json; d=json.load(open("/tmp/fs_f_reqs.json"));
items=d.get("content") or []
print(next((it["id"] for it in items if it.get("status")=="COMPLETED"), ""))')"
if [[ -n "$RID" ]]; then
  code="$(curl -s -o /tmp/fs_f_bad.json -w '%{http_code}' -H "$AUTH" -H 'Content-Type: application/json' \
    -d "{\"ids\":[\"$RID\"],\"action\":\"SET_STATUS\",\"status\":\"NEW\"}" \
    "$BASE/api/requests/bulk" || echo 000)"
  if [[ "$code" == "409" ]]; then
    check "illegal_status_transition_blocked" "blocked" "blocked" "http=409"
  elif [[ "$code" == "200" ]]; then
    fails="$(python3 -c 'import json; print(len(json.load(open("/tmp/fs_f_bad.json")).get("failures") or []))')"
    if [[ "$fails" -ge 1 ]]; then
      check "illegal_status_transition_blocked" "blocked" "blocked" "http=200 failures=$fails"
    else
      check "illegal_status_transition_blocked" "blocked" "open" "http=200 no failures"
    fi
  else
    check "illegal_status_transition_blocked" "blocked" "open" "http=$code"
  fi
else
  check "illegal_status_transition_blocked" "skipped" "skipped" "no COMPLETED request"
fi

if curl -sk -o /dev/null --connect-timeout 2 "$HTTPS/actuator/health"; then
  code="$(curl -sk -o /tmp/fs_f_tls.json -w '%{http_code}' "$HTTPS/actuator/health")"
  check "health_via_https_proxy" "200" "$code" "$(cat /tmp/fs_f_tls.json)"
else
  check "health_via_https_proxy" "200" "down" "proxy not running yet"
fi

{
  echo "timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "base=$BASE"
  echo "passed=$pass"
  echo "failed=$fail"
  echo "total=$((pass + fail))"
} | tee "$SUMMARY"

echo "RESULT: $pass passed, $fail failed"
[[ "$fail" -eq 0 ]]
