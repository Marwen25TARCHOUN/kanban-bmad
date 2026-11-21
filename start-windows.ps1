<#
.SYNOPSIS
  Start the kanban stack via Docker Compose (Windows primary launcher).
.DESCRIPTION
  AD-15 containerized entry point. Checks Docker Desktop is running, warns if
  .env is missing (per AD-6 / FR-25), then `docker compose up -d --build` and
  prints the URLs. Idempotent — safe to re-run.
.EXAMPLE
  .\start-windows.ps1
#>
[CmdletBinding()]
param()
$ErrorActionPreference = "Stop"

function Write-Info($msg) { Write-Host "[kanban] $msg" -ForegroundColor Cyan }
function Write-Warn($msg) { Write-Host "[kanban] $msg" -ForegroundColor Yellow }
function Write-Err ($msg) { Write-Host "[kanban] $msg" -ForegroundColor Red }

# --- 1) Docker daemon reachable? ---
try {
  docker info 2>$null | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "docker info exit $LASTEXITCODE" }
} catch {
  Write-Err "Docker Desktop is not running (or 'docker' is not on PATH)."
  Write-Err "Start Docker Desktop and re-run this script."
  Write-Err "Install: https://www.docker.com/products/docker-desktop/"
  exit 1
}

# --- 2) .env sanity check (AD-6: AI is optional) ---
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile  = Join-Path $repoRoot ".env"
if (-not (Test-Path $envFile)) {
  Write-Warn ".env not found. The app will start with the AI feature disabled."
  Write-Warn "Copy .env.example to .env and set ANTHROPIC_API_KEY to enable AI breakdown."
} else {
  $hasKey = Select-String -Path $envFile -Pattern "^ANTHROPIC_API_KEY=.+" -Quiet
  if (-not $hasKey) {
    Write-Warn ".env present but ANTHROPIC_API_KEY is empty. App will start with AI disabled."
  }
}

# --- 3) Compose up ---
Write-Info "Building and starting containers (first run takes a minute; cached runs ~10 s)..."
$compose = Join-Path $repoRoot "docker-compose.yml"
docker compose -f $compose up -d --build
if ($LASTEXITCODE -ne 0) {
  Write-Err "docker compose up failed. See errors above."
  exit $LASTEXITCODE
}

# --- 4) Print URLs + helpful follow-ups ---
Write-Host ""
Write-Info "Containers started."
Write-Info "Frontend:       http://localhost:4200"
Write-Info "Backend health: http://localhost:8080/actuator/health"
Write-Host ""
Write-Info "Stop with:  .\stop-windows.ps1  (or  stop.bat)"
Write-Info "Data lives in the 'kanban-data' Docker volume and survives 'stop'."
