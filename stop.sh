#!/usr/bin/env bash
# Stop the kanban stack (Mac/Linux). Mirror of stop-windows.ps1 — AD-15.
# Pass --purge to also destroy the kanban-data volume (prompts for confirmation).
set -euo pipefail

C="\033[0;36m"; Y="\033[0;33m"; N="\033[0m"
info() { printf "${C}[kanban]${N} %s\n" "$*"; }
warn() { printf "${Y}[kanban]${N} %s\n" "$*"; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE="${SCRIPT_DIR}/docker-compose.yml"

if [ "${1:-}" = "--purge" ]; then
  warn "--purge requested. This will DELETE the kanban-data volume and all board data."
  read -r -p "Type 'yes' to confirm: " answer
  if [ "${answer}" != "yes" ]; then
    info "Cancelled. No data destroyed. Doing a regular stop instead."
    docker compose -f "${COMPOSE}" down
    exit 0
  fi
  docker compose -f "${COMPOSE}" down -v
  info "Containers stopped and kanban-data volume removed."
  exit 0
fi

docker compose -f "${COMPOSE}" down
info "Containers stopped. Board data survives in the 'kanban-data' volume."
info "Restart anytime with  ./start.sh."
