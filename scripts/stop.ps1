$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding
$ProjectRoot = Split-Path -Parent $PSScriptRoot

$TomcatHome = if ($env:CATALINA_HOME) {
    $env:CATALINA_HOME
} elseif (Test-Path 'D:\Program Files\Tomcat\apache-tomcat-9.0.121') {
    'D:\Program Files\Tomcat\apache-tomcat-9.0.121'
} else {
    Write-Host '未找到 Tomcat 9。请设置环境变量 CATALINA_HOME 指向 Tomcat 9 安装目录后重试。'
    exit 1
}

$RuntimeBase = Join-Path $ProjectRoot 'runtime\tomcat'
if (-not (Test-Path (Join-Path $RuntimeBase 'conf\server.xml'))) {
    Write-Host 'The local project runtime has not been created.'
    exit 0
}
$env:CATALINA_HOME = $TomcatHome
$env:CATALINA_BASE = $RuntimeBase
$env:JAVA_OPTS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Duser.language=en -Duser.country=US -Duser.timezone=Asia/Shanghai'
& (Join-Path $TomcatHome 'bin\catalina.bat') stop
Write-Host 'Tomcat stopped. Port 8080 is now free.'
