#!/usr/bin/env bash
# Start the kanban stack (Mac/Linux). Mirror of start-windows.ps1 — AD-15.
set -euo pipefail

C="\033[0;36m"; Y="\033[0;33m"; R="\033[0;31m"; N="\033[0m"
info() { printf "${C}[kanban]${N} %s\n" "$*"; }
warn() { printf "${Y}[kanban]${N} %s\n" "$*"; }
err () { printf "${R}[kanban]${N} %s\n" "$*"; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE="${SCRIPT_DIR}/docker-compose.yml"

# 1) Docker daemon reachable?
if ! docker info >/dev/null 2>&1; then
  err "Docker is not running (or 'docker' is not on PATH)."
  err "Start Docker Desktop (or your Docker daemon) and re-run."
  err "Install: https://www.docker.com/products/docker-desktop/"
  exit 1
fi

# 2) .env sanity check (AD-6: AI is optional)
if [ ! -f "${SCRIPT_DIR}/.env" ]; then
  warn ".env not found. The app will start with the AI feature disabled."
  warn "Copy .env.example to .env and set ANTHROPIC_API_KEY to enable AI breakdown."
elif ! grep -qE '^ANTHROPIC_API_KEY=.+' "${SCRIPT_DIR}/.env"; then
  warn ".env present but ANTHROPIC_API_KEY is empty. App will start with AI disabled."
fi

# 3) Compose up
info "Building and starting containers (first run takes a minute; cached runs ~10 s)..."
docker compose -f "${COMPOSE}" up -d --build

# 4) URLs
echo
info "Containers started."
info "Frontend:       http://localhost:4200"
info "Backend health: http://localhost:8080/actuator/health"
echo
info "Stop with:  ./stop.sh"
info "Data lives in the 'kanban-data' Docker volume and survives 'stop'."
