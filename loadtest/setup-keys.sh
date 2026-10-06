#!/usr/bin/env bash
# Creates N merchants with a TEST API key each and writes loadtest/data/keys.csv
# (one "Basic <base64(keyId:secret)>" line per key, read by razorpay-load.jmx).
# Usage: BASE_URL=http://localhost:18080 ./loadtest/setup-keys.sh [count]
# The keys file holds live secrets. It is git-ignored.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
COUNT="${1:-5}"
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/data/keys.csv"
mkdir -p "$HERE/data"
: > "$OUT"

json() { python3 -c "import json,sys; print(json.load(sys.stdin)$1)"; }

for i in $(seq 1 "$COUNT"); do
  email="loadtest-$(date +%s)-$RANDOM-$i@example.com"

  merchant_id=$(curl -fsS -X POST "$BASE_URL/v1/auth/signup" -H 'Content-Type: application/json' \
    -d "{\"name\":\"Load $i\",\"email\":\"$email\",\"password\":\"password123\",\"businessName\":\"Load $i\",\"businessType\":\"PROPRIETORSHIP\"}" \
    | json "['id']")

  token=$(curl -fsS -X POST "$BASE_URL/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"password123\"}" | json "['accessToken']")

  key=$(curl -fsS -X POST "$BASE_URL/v1/merchants/$merchant_id/api-keys" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d '{"environment":"TEST"}')
  key_id=$(printf '%s' "$key" | json "['keyId']")
  key_secret=$(printf '%s' "$key" | json "['keySecret']")

  printf 'Basic %s\n' "$(printf '%s:%s' "$key_id" "$key_secret" | base64 | tr -d '\n')" >> "$OUT"
done

echo "wrote $COUNT keys to $OUT"
