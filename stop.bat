@echo off
REM Thin wrapper around stop-windows.ps1 — neutralizes PowerShell ExecutionPolicy
REM for users who double-click this file. All logic lives in the .ps1.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-windows.ps1" %*
