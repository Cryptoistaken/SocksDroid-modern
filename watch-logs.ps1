<#
.SYNOPSIS
  Streams Logcat from the KiloProxy app on an Android emulator/device
  connected via ADB. Filters to the app PID so only relevant debug
  output is shown.

.DESCRIPTION
  Clears the existing logcat buffer, resolves the PID of the
  net.typeblog.socks process, and streams new log entries in real time
  with timestamps.

  The device/emulator must already be connected and ADB must be in PATH.

  For the AVD emulator (port 5557), we use localhost:5557 by default.
  Adjust the -Device parameter if your device is on a different
  ADB endpoint.

.EXAMPLE
  .\watch-logs.ps1
  .\watch-logs.ps1 -Device emulator-5554
  .\watch-logs.ps1 -Device <device-serial>
#>

param(
    [string]$Device = "localhost:5557"
)

$appPackage = "net.typeblog.socks"

# Resolve the PID
Write-Host "[KiloProxy] Connecting to ADB device: $Device" -ForegroundColor Cyan

$pidResult = adb -s $Device shell pidof -s $appPackage 2>$null

if (-not $pidResult) {
    Write-Warning "[KiloProxy] App '$appPackage' is not running. Starting log monitoring anyway (no PID filter)..."
    Write-Host "[KiloProxy] Run the app on the device first, then re-run this script for PID-filtered output." -ForegroundColor Yellow
    Write-Host "[KiloProxy] Streaming all logcat (unfiltered):" -ForegroundColor Cyan
    adb -s $Device logcat -c
    adb -s $Device logcat -v time | Select-String -Pattern "KiloProxy"
    return
}

$pid = $pidResult.Trim()
Write-Host "[KiloProxy] Found PID $pid for $appPackage" -ForegroundColor Green
Write-Host "[KiloProxy] Clearing logcat buffer..." -ForegroundColor Cyan
adb -s $Device logcat -c
Write-Host "[KiloProxy] Streaming logs (Ctrl+C to stop)..." -ForegroundColor Cyan
Write-Host ""

adb -s $Device logcat --pid=$pid -v time
