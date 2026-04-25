#!/bin/bash
# Ciff Deployment Health Check Script

set -e

BASE_URL="${BASE_URL:-http://localhost}"
TIMEOUT=30

echo "========================================"
echo "Ciff Deployment Health Check"
echo "Base URL: $BASE_URL"
echo "========================================"
echo

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_pass() { echo -e "${GREEN}PASS${NC} - $1"; }
check_fail() { echo -e "${RED}FAIL${NC} - $1"; }
check_warn() { echo -e "${YELLOW}WARN${NC} - $1"; }

# 1. Check Nginx is reachable
echo "[1/6] Checking Nginx..."
if curl -sf --max-time "$TIMEOUT" "$BASE_URL" > /dev/null 2>&1; then
    check_pass "Nginx is responding"
else
    check_fail "Nginx is not responding at $BASE_URL"
    exit 1
fi

# 2. Check backend health endpoint
echo "[2/6] Checking backend health..."
HEALTH=$(curl -sf --max-time "$TIMEOUT" "$BASE_URL/api/actuator/health" 2>/dev/null || echo "")
if echo "$HEALTH" | grep -q '"status":"UP"'; then
    check_pass "Backend health check passed"
else
    check_fail "Backend health check failed"
    echo "  Response: $HEALTH"
fi

# 3. Check API login endpoint exists (should get 400/401/405, not 404/502)
echo "[3/6] Checking API login endpoint..."
LOGIN_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" "$BASE_URL/api/auth/login" 2>/dev/null || echo "000")
if [ "$LOGIN_STATUS" = "405" ] || [ "$LOGIN_STATUS" = "400" ] || [ "$LOGIN_STATUS" = "401" ]; then
    check_pass "Login endpoint is reachable (HTTP $LOGIN_STATUS)"
else
    check_warn "Login endpoint returned HTTP $LOGIN_STATUS (expected 400/401/405)"
fi

# 4. Check static assets
echo "[4/6] Checking static assets..."
INDEX_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" "$BASE_URL/index.html" 2>/dev/null || echo "000")
if [ "$INDEX_STATUS" = "200" ]; then
    check_pass "Frontend index.html is served"
else
    check_fail "Frontend index.html returned HTTP $INDEX_STATUS"
fi

# 5. Check SSE endpoint exists
echo "[5/6] Checking SSE endpoint..."
# Just check the endpoint responds (will get 401 since no auth, but that's expected)
SSE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$BASE_URL/api/v1/chat/completions/stream" 2>/dev/null || echo "000")
if [ "$SSE_STATUS" = "401" ] || [ "$SSE_STATUS" = "403" ]; then
    check_pass "SSE endpoint is reachable (HTTP $SSE_STATUS)"
elif [ "$SSE_STATUS" = "404" ]; then
    check_warn "SSE endpoint returned 404 - may not be deployed yet"
else
    check_warn "SSE endpoint returned HTTP $SSE_STATUS"
fi

# 6. Check Docker containers
echo "[6/6] Checking Docker containers..."
if command -v docker >/dev/null 2>&1; then
    if docker ps --format '{{.Names}}' | grep -qE 'ciff-app|ciff-nginx'; then
        check_pass "Ciff containers are running"
        echo
        echo "Running containers:"
        docker ps --format '  {{.Names}} ({{.Status}})' | grep -E 'ciff-app|ciff-nginx' || true
    else
        check_fail "No ciff containers found running"
    fi
else
    check_warn "Docker not available, skipping container check"
fi

echo
echo "========================================"
echo "Health check completed"
echo "========================================"
