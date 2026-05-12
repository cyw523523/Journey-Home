param(
  [string]$MysqlBin = "mysql",
  [string]$HostName = "localhost",
  [int]$Port = 3306,
  [string]$RootUser = "root",
  [Parameter(Mandatory = $true)]
  [string]$RootPassword,
  [string]$Database = "guitu"
)

$ErrorActionPreference = "Stop"

$sql = @"
CREATE DATABASE IF NOT EXISTS $Database DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
"@

Write-Host "Creating database '$Database' on ${HostName}:$Port ..."
$sql | & $MysqlBin "-h$HostName" "-P$Port" "-u$RootUser" "-p$RootPassword"
Write-Host "Database '$Database' is ready."
