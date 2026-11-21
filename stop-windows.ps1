<#
.SYNOPSIS
  Stop the kanban stack (Windows primary stopper).
.DESCRIPTION
  AD-15 stopper. Runs `docker compose down`. Pass -Purge to also remove the
  named volume kanban-data (prompts for confirmation — destructive).
.EXAMPLE
  .\stop-windows.ps1
  .\stop-windows.ps1 -Purge
#>
[CmdletBinding()]
param(
  [switch]$Purge
)
$ErrorActionPreference = "Stop"

function Write-Info($msg) { Write-Host "[kanban] $msg" -ForegroundColor Cyan }
function Write-Warn($msg) { Write-Host "[kanban] $msg" -ForegroundColor Yellow }

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$compose  = Join-Path $repoRoot "docker-compose.yml"

if ($Purge) {
  Write-Warn "-Purge requested. This will DELETE the kanban-data volume and all board data."
  $answer = Read-Host "Type 'yes' to confirm"
  if ($answer -ne "yes") {
    Write-Info "Cancelled. No data destroyed. Doing a regular stop instead."
    docker compose -f $compose down
    exit $LASTEXITCODE
  }
  docker compose -f $compose down -v
  Write-Info "Containers stopped and kanban-data volume removed."
  exit $LASTEXITCODE
}

docker compose -f $compose down
Write-Info "Containers stopped. Board data survives in the 'kanban-data' volume."
Write-Info "Restart anytime with  .\start-windows.ps1."
