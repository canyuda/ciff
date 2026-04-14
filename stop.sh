#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_DIR="$ROOT_DIR/.run"
GRACEFUL_TIMEOUT=10

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[STOP]${NC} $*"; }
warn() { echo -e "${YELLOW}[STOP]${NC} $*"; }

stop_service() {
    local name=$1
    local pid_file="$PID_DIR/${name}.pid"

    if [ ! -f "$pid_file" ]; then
        warn "$name: PID file not found, skipping"
        return
    fi

    local pid
    pid=$(cat "$pid_file")

    if ! kill -0 "$pid" 2>/dev/null; then
        warn "$name: process $pid already stopped"
        rm -f "$pid_file"
        return
    fi

    log "$name: sending SIGTERM to $pid"
    kill -TERM "$pid" 2>/dev/null || true

    local waited=0
    while [ $waited -lt $GRACEFUL_TIMEOUT ]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            log "$name: stopped gracefully"
            rm -f "$pid_file"
            return
        fi
        sleep 1
        waited=$((waited + 1))
    done

    log "$name: timeout, sending SIGKILL to $pid"
    kill -KILL "$pid" 2>/dev/null || true
    rm -f "$pid_file"
    log "$name: killed"
}

stop_service backend
stop_service frontend

log "All services stopped."
