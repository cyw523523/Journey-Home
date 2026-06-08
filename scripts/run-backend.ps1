param(
  [string]$DbHost = "localhost",
  [int]$DbPort = 3306,
  [string]$DbName = "guitu",
  [string]$DbUser = "root",
  [string]$DbPassword = "",
  [ValidateSet("default", "h2")]
  [string]$Profile = "default"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mvnw = Join-Path $root "backend\mvnw.cmd"
$bundledMvn = Join-Path $root "tools\apache-maven-3.9.9\bin\mvn.cmd"
$mvn = Get-Command mvn -ErrorAction SilentlyContinue

if ($Profile -ne "h2") {
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
}

function Get-MavenCommand {
  if (Test-Path $bundledMvn) {
    return $bundledMvn
  }

  if ($mvn) {
    return $mvn.Source
  }

  if (Test-Path $mvnw) {
    return $mvnw
  }

  throw "No Maven command is available. Expected tools\\apache-maven-3.9.9\\bin\\mvn.cmd, a global mvn command, or backend\\mvnw.cmd."
}

$mavenCommand = Get-MavenCommand

Push-Location (Join-Path $root "backend")
try {
  if ($Profile -eq "h2") {
    & $mavenCommand spring-boot:run "-Dspring-boot.run.profiles=h2"
  } else {
    & $mavenCommand spring-boot:run
  }
} finally {
  Pop-Location
}
