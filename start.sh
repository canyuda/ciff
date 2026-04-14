#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_DIR="$ROOT_DIR/.run"
BACKEND_PID_FILE="$PID_DIR/backend.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend.pid"
BACKEND_PORT=8080
FRONTEND_PORT=3000
HEALTH_URL="http://localhost:$BACKEND_PORT/api/v1/health"
HEALTH_TIMEOUT=60

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[START]${NC} $*"; }
warn() { echo -e "${YELLOW}[START]${NC} $*"; }
die()  { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

mkdir -p "$PID_DIR"

# --- Check if already running ---
is_alive() {
    local pid_file=$1
    [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null
}

if is_alive "$BACKEND_PID_FILE"; then
    die "Backend already running (PID $(cat "$BACKEND_PID_FILE"))"
fi
if is_alive "$FRONTEND_PID_FILE"; then
    die "Frontend already running (PID $(cat "$FRONTEND_PID_FILE"))"
fi

# --- Check MySQL ---
log "Checking MySQL..."
if ! mysqladmin ping -h "${MYSQL_HOST:-localhost}" -P "${MYSQL_PORT:-3306}" -u "${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-123456}" --silent 2>/dev/null; then
    die "MySQL is not available at ${MYSQL_HOST:-localhost}:${MYSQL_PORT:-3306}"
fi
log "MySQL OK"

# --- Check Redis ---
log "Checking Redis..."
if ! redis-cli -h "${REDIS_HOST:-localhost}" -p "${REDIS_PORT:-6379}" ping > /dev/null 2>&1; then
    die "Redis is not available at ${REDIS_HOST:-localhost}:${REDIS_PORT:-6379}"
fi
log "Redis OK"

# --- Build backend ---
log "Building backend..."
cd "$ROOT_DIR"
if ! mvn package -pl ciff-app -am -DskipTests -q; then
    die "Backend build failed"
fi
log "Backend build OK"

# --- Start backend ---
log "Starting backend on port $BACKEND_PORT..."
JAR_FILE="$ROOT_DIR/ciff-app/target/ciff-app-1.0.0-SNAPSHOT.jar"
[ -f "$JAR_FILE" ] || die "JAR not found: $JAR_FILE"

java -jar "$JAR_FILE" > "$PID_DIR/backend.log" 2>&1 &
echo $! > "$BACKEND_PID_FILE"
log "Backend started (PID $(cat "$BACKEND_PID_FILE"))"

# --- Wait for health check ---
log "Waiting for backend health check (timeout ${HEALTH_TIMEOUT}s)..."
elapsed=0
while [ $elapsed -lt $HEALTH_TIMEOUT ]; do
    if curl -sf "$HEALTH_URL" > /dev/null 2>&1; then
        log "Backend health check passed"
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done

if [ $elapsed -ge $HEALTH_TIMEOUT ]; then
    stop_backend
    die "Backend health check timeout after ${HEALTH_TIMEOUT}s. Check $PID_DIR/backend.log"
fi

# --- Start frontend ---
log "Starting frontend on port $FRONTEND_PORT..."
cd "$ROOT_DIR/ciff-web"
npm run dev > "$PID_DIR/frontend.log" 2>&1 &
echo $! > "$FRONTEND_PID_FILE"
log "Frontend started (PID $(cat "$FRONTEND_PID_FILE"))"

echo ""
log "All services started:"
log "  Backend  -> http://localhost:$BACKEND_PORT"
log "  Frontend -> http://localhost:$FRONTEND_PORT"
log "  Logs     -> $PID_DIR/"
echo ""
log "Run ./stop.sh to stop all services."
