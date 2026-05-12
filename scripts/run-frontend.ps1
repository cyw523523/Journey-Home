param(
  [int]$Port = 5173
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Push-Location (Join-Path $root "frontend")
try {
  npm run dev -- --port $Port
} finally {
  Pop-Location
}
