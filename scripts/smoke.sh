#!/usr/bin/env bash
# End-to-end smoke test against a running stack (docker compose up).
# Usage: BASE_URL=http://localhost:8080 ./scripts/smoke.sh
# Needs: curl, python3. Exits non-zero on the first failed check.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
RUN_ID="$(date +%s)-$RANDOM"
EMAIL="smoke-${RUN_ID}@example.com"
PASSWORD="password123"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

json() { python3 -c "import json,sys; print(json.load(sys.stdin)$1)"; }

# call METHOD PATH EXPECTED_STATUS OUTFILE [curl args...]
call() {
  local method="$1" path="$2" expected="$3" out="$4"; shift 4
  local status
  status=$(curl -s -o "$out" -w '%{http_code}' -X "$method" "$BASE_URL$path" "$@")
  if [ "$status" != "$expected" ]; then
    echo "FAIL $method $path: expected $expected got $status"; cat "$out"; echo; exit 1
  fi
  echo "ok   $method $path -> $status"
}

echo "== signup + login"
call POST /v1/auth/signup 201 "$WORK/signup.json" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Smoke\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"businessName\":\"Smoke Co\",\"businessType\":\"PROPRIETORSHIP\"}"
MERCHANT_ID=$(json "['id']" < "$WORK/signup.json")

call POST /v1/auth/login 401 "$WORK/badlogin.json" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"wrong-password\"}"

call POST /v1/auth/login 200 "$WORK/login.json" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
TOKEN=$(json "['accessToken']" < "$WORK/login.json")

echo "== API key"
call POST "/v1/merchants/$MERCHANT_ID/api-keys" 201 "$WORK/key.json" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"environment":"TEST"}'
KEY_ID=$(json "['keyId']" < "$WORK/key.json")
KEY_SECRET=$(json "['keySecret']" < "$WORK/key.json")
AUTH="Basic $(printf '%s:%s' "$KEY_ID" "$KEY_SECRET" | base64 | tr -d '\n')"

echo "== vault"
call POST /v1/vault/tokenize 201 "$WORK/token.json" -H "Authorization: $AUTH" -H 'Content-Type: application/json' \
  -d "{\"pan\":\"4111111111111111\",\"cvv\":\"123\",\"expiryMonth\":12,\"expiryYear\":2030,\"customerId\":\"$(python3 -c 'import uuid;print(uuid.uuid4())')\",\"cardHolderName\":\"Smoke\"}"
TOKEN_CARD=$(json "['token']" < "$WORK/token.json")

echo "== order + card payment"
call POST /v1/orders 201 "$WORK/order.json" -H "Authorization: $AUTH" -H 'Content-Type: application/json' \
  -d "{\"amount\":{\"amountUnits\":1000,\"currency\":\"INR\"},\"receipt\":\"smoke-$RUN_ID\"}"
ORDER_ID=$(json "['id']" < "$WORK/order.json")

call POST /v1/payments 201 "$WORK/payment.json" -H "Authorization: $AUTH" -H 'Content-Type: application/json' \
  -d "{\"orderId\":\"$ORDER_ID\",\"method\":\"CARD\",\"methodDetails\":{\"token\":\"$TOKEN_CARD\"}}"

echo "== gateway auth edge"
call POST /v1/orders 401 "$WORK/noauth.json" -H 'Content-Type: application/json' -d '{}'

echo "PASS: smoke flow ok for merchant $MERCHANT_ID"
