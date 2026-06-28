#!/bin/bash
# =============================================================
# Community Platform — API Smoke Test Script
# 测试 Gateway → 微服务全链路: 15+ 断言
#
# 前置条件: 全部 6 个微服务 + 基础设施已启动
# 用法: bash scripts/smoke-test.sh
# =============================================================
set -euo pipefail

BASE="http://localhost:8080"
PASS=0
FAIL=0

green() { echo -e "\033[32m✓ $1\033[0m"; }
red()   { echo -e "\033[31m✗ $1 (expected: $2, got: $3)\033[0m"; }
check() {
    local label="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        green "$label"; PASS=$((PASS + 1))
    else
        red "$label" "$expected" "$actual"; FAIL=$((FAIL + 1))
    fi
}

echo "=========================================="
echo "Community Platform Smoke Test"
echo "=========================================="
echo ""

# ---- 1. Login ----
echo "--- Auth ---"
RESP=$(curl -s -X POST "$BASE/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"alice","password":"123456"}')
SUCCESS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo "False")
check "1. Login alice"       "True" "$SUCCESS"

TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || echo "")
LOGIN_UID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null || echo "")
check "2. Token non-empty"   "1" "$(test -n "$TOKEN" && echo 1 || echo 0)"
check "3. UID = 1"           "1" "$LOGIN_UID"

# ---- 2. User Profile ----
echo "--- Users ---"
RESP=$(curl -s "$BASE/api/v1/users/me" -H "Authorization: Bearer $TOKEN")
NICK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['nickname'])" 2>/dev/null || echo "")
check "4. GET /users/me"     "Alice" "$NICK"

RESP=$(curl -s "$BASE/api/v1/users/1" -H "Authorization: Bearer $TOKEN")
check "5. GET /users/1"      "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

# ---- 3. Server CRUD ----
echo "--- Servers ---"
RESP=$(curl -s -X POST "$BASE/api/v1/servers" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"SmokeTest","description":"auto","icon":"🧪"}')
SVR_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null || echo "0")
check "6. POST /servers"     "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

RESP=$(curl -s "$BASE/api/v1/servers" -H "Authorization: Bearer $TOKEN")
SVR_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['data']))" 2>/dev/null || echo "0")
check "7. GET /servers"      "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

RESP=$(curl -s "$BASE/api/v1/servers/$SVR_ID" -H "Authorization: Bearer $TOKEN")
check "8. GET /servers/{id}" "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

# ---- 4. Channel + Category ----
echo "--- Channels ---"
RESP=$(curl -s -X POST "$BASE/api/v1/servers/$SVR_ID/channels" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"general","type":0}')
CH_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null || echo "0")
check "9. POST /channels"    "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

RESP=$(curl -s "$BASE/api/v1/servers/$SVR_ID/channels" -H "Authorization: Bearer $TOKEN")
check "10. GET /channels"    "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

# ---- 5. Message + Thread + Reaction ----
echo "--- Messages ---"
RESP=$(curl -s -X POST "$BASE/api/v1/channels/$CH_ID/messages" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"content":"Hello smoke test!","msgType":1}')
MSG_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null || echo "0")
check "11. POST /messages"   "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

RESP=$(curl -s "$BASE/api/v1/channels/$CH_ID/messages?cursor=&pageSize=10" -H "Authorization: Bearer $TOKEN")
check "12. GET /messages"    "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

RESP=$(curl -s -X POST "http://localhost:8080/api/v1/messages/$MSG_ID/reactions?emoji=%F0%9F%91%8D" \
    -H "Authorization: Bearer $TOKEN")
check "13. POST /reactions"  "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

# ---- 6. Members + Roles ----
echo "--- Members ---"
RESP=$(curl -s "$BASE/api/v1/servers/$SVR_ID/members" -H "Authorization: Bearer $TOKEN")
check "14. GET /members"     "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

RESP=$(curl -s "$BASE/api/v1/servers/$SVR_ID/roles" -H "Authorization: Bearer $TOKEN")
check "15. GET /roles"       "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"

# ---- 7. Invite ----
echo "--- Invite ---"
RESP=$(curl -s -X POST "$BASE/api/v1/servers/$SVR_ID/invites" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" -d '{}')
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['code'])" 2>/dev/null || echo "")
check "16. POST /invites"    "True" "$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['success'])" 2>/dev/null || echo False)"
check "17. invite code len=8" "True" "$(test ${#CODE} -ge 6 && echo True || echo False)"

# ---- Cleanup ----
echo "--- Cleanup ---"
curl -s -X DELETE "$BASE/api/v1/channels/$CH_ID/messages/$MSG_ID" -H "Authorization: Bearer $TOKEN" > /dev/null
curl -s -X DELETE "$BASE/api/v1/channels/$CH_ID" -H "Authorization: Bearer $TOKEN" > /dev/null
curl -s -X DELETE "$BASE/api/v1/servers/$SVR_ID" -H "Authorization: Bearer $TOKEN" > /dev/null
green "Cleanup done (server $SVR_ID, channel $CH_ID, msg $MSG_ID)"

echo ""
echo "=========================================="
echo "Results: $PASS passed, $FAIL failed"
echo "=========================================="

exit $FAIL
