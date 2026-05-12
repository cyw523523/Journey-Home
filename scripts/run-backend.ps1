param(
  [string]$DbHost = "localhost",
  [int]$DbPort = 3306,
  [string]$DbName = "guitu",
  [string]$DbUser = "root",
  [string]$DbPassword = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mvnw = Join-Path $root "backend\mvnw.cmd"
$mvn = Get-Command mvn -ErrorAction SilentlyContinue

if (-not $DbPassword) {
  $DbPassword = $env:DB_PASSWORD
}

if (-not $DbPassword) {
  throw "Database password is required. Pass -DbPassword or set DB_PASSWORD first."
}

$env:DB_HOST = $DbHost
$env:DB_PORT = "$DbPort"
$env:DB_NAME = $DbName
$env:DB_USERNAME = $DbUser
$env:DB_PASSWORD = $DbPassword

Push-Location (Join-Path $root "backend")
try {
  if (Test-Path $mvnw) {
    & $mvnw spring-boot:run
  } elseif ($mvn) {
    & $mvn.Source spring-boot:run
  } else {
    throw "Neither backend\\mvnw.cmd nor a global mvn command is available."
  }
} finally {
  Pop-Location
}
