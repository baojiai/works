$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding

if (-not $env:DEEPSEEK_API_KEY) {
    $secret = Read-Host '请输入 DeepSeek API Key（不会显示，也不会写入文件）' -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
    try {
        $env:DEEPSEEK_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

if (-not $env:DEEPSEEK_MODEL) {
    $env:DEEPSEEK_MODEL = 'deepseek-v4-flash'
}

# run.ps1 负责：构建检查（缺 WAR 时自动调用 build.ps1）+ 部署 + 启动 Tomcat。
& (Join-Path $PSScriptRoot 'stop.ps1')
& (Join-Path $PSScriptRoot 'run.ps1')
