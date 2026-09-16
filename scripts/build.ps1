$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding
$ProjectRoot = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host '未检测到 Maven，请安装/配置 Maven 后重试。'
    exit 1
}

Push-Location $ProjectRoot
try {
    & mvn clean package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
} finally {
    Pop-Location
}

$War = Join-Path $ProjectRoot 'target\after-sales.war'
if (-not (Test-Path $War)) { throw "WAR not found: $War" }
Write-Host "Built: $War"
