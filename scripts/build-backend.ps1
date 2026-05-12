param()

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mvnw = Join-Path $root "backend\mvnw.cmd"
$mvn = Get-Command mvn -ErrorAction SilentlyContinue

Push-Location (Join-Path $root "backend")
try {
  if (Test-Path $mvnw) {
    & $mvnw -DskipTests package
  } elseif ($mvn) {
    & $mvn.Source -DskipTests package
  } else {
    throw "Neither backend\\mvnw.cmd nor a global mvn command is available."
  }
} finally {
  Pop-Location
}
