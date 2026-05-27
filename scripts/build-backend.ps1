param()

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mvnw = Join-Path $root "backend\mvnw.cmd"
$bundledMvn = Join-Path $root "tools\apache-maven-3.9.9\bin\mvn.cmd"
$mvn = Get-Command mvn -ErrorAction SilentlyContinue

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
  & $mavenCommand -DskipTests package
} finally {
  Pop-Location
}
