$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$TomcatHome = if ($env:CATALINA_HOME) { $env:CATALINA_HOME } else { 'E:\Tomcat\apache-tomcat-9.0.115' }
$BuildDir = Join-Path $ProjectRoot 'build'
$ClassesDir = Join-Path $BuildDir 'WEB-INF\classes'
$WarStage = Join-Path $BuildDir 'war'

if (-not (Test-Path (Join-Path $TomcatHome 'lib\servlet-api.jar'))) {
    throw "Tomcat not found: $TomcatHome"
}

if (Test-Path $BuildDir) { Remove-Item -LiteralPath $BuildDir -Recurse -Force }
New-Item -ItemType Directory -Force $ClassesDir, $WarStage | Out-Null

$sources = Get-ChildItem (Join-Path $ProjectRoot 'src') -Filter '*.java' -Recurse | ForEach-Object FullName
$classpath = @(
    (Join-Path $TomcatHome 'lib\servlet-api.jar'),
    (Join-Path $ProjectRoot 'web\WEB-INF\lib\h2-2.2.224.jar'),
    (Join-Path $ProjectRoot 'web\WEB-INF\lib\mysql-connector-java-8.0.28.jar')
) -join ';'

& javac -encoding UTF-8 -source 8 -target 8 -cp $classpath -d $ClassesDir $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }

Copy-Item -Path (Join-Path $ProjectRoot 'web\*') -Destination $WarStage -Recurse -Force
$WarClasses = Join-Path $WarStage 'WEB-INF\classes'
New-Item -ItemType Directory -Force $WarClasses | Out-Null
Copy-Item -Path (Join-Path $ClassesDir '*') -Destination $WarClasses -Recurse -Force
Copy-Item -Path (Join-Path $ProjectRoot 'db\schema-h2.sql') -Destination (Join-Path $WarClasses 'schema-h2.sql') -Force
Copy-Item -Path (Join-Path $ProjectRoot 'db\seed-h2.sql') -Destination (Join-Path $WarClasses 'seed-h2.sql') -Force

$WarPath = Join-Path $BuildDir 'after-sales.war'
Push-Location $WarStage
try { & jar cf $WarPath * } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw 'WAR packaging failed.' }
Write-Host "Built: $WarPath"
