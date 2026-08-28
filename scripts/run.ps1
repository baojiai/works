$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$TomcatHome = if ($env:CATALINA_HOME) { $env:CATALINA_HOME } else { 'E:\Tomcat\apache-tomcat-9.0.115' }
$ExistingListener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($ExistingListener) {
    Write-Host 'Port 8080 is already in use. If this project is already running, open http://localhost:8080/after-sales/ ; otherwise free the port and retry.'
    exit 0
}
$War = Join-Path $ProjectRoot 'build\after-sales.war'
if (-not (Test-Path $War)) { & (Join-Path $PSScriptRoot 'build.ps1') }
$RuntimeBase = Join-Path $ProjectRoot 'runtime\tomcat'
New-Item -ItemType Directory -Force $RuntimeBase, (Join-Path $RuntimeBase 'webapps'), (Join-Path $RuntimeBase 'logs'), (Join-Path $RuntimeBase 'temp'), (Join-Path $RuntimeBase 'work'), (Join-Path $RuntimeBase 'data') | Out-Null
if (-not (Test-Path (Join-Path $RuntimeBase 'conf\server.xml'))) {
    Copy-Item -Path (Join-Path $TomcatHome 'conf') -Destination $RuntimeBase -Recurse -Force
}
$Target = Join-Path $RuntimeBase 'webapps\after-sales.war'
$ExpandedApp = Join-Path $RuntimeBase 'webapps\after-sales'
if (Test-Path $ExpandedApp) { Remove-Item -LiteralPath $ExpandedApp -Recurse -Force }
$CompiledJsp = Join-Path $RuntimeBase 'work\Catalina\localhost\after-sales'
if (Test-Path $CompiledJsp) { Remove-Item -LiteralPath $CompiledJsp -Recurse -Force }
Copy-Item -LiteralPath $War -Destination $Target -Force
Write-Host 'Starting Tomcat. Press Ctrl+C to stop.'
$env:CATALINA_HOME = $TomcatHome
$env:CATALINA_BASE = $RuntimeBase
$env:JAVA_OPTS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Duser.language=en -Duser.country=US -Duser.timezone=Asia/Shanghai'
& (Join-Path $TomcatHome 'bin\catalina.bat') run
