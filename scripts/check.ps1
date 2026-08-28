$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1')
$required = @('system_user','engineer_schedule','appointment','repair_order','part_request','part_inventory','inventory_flow','acceptance','review','notification','operation_log')
$schema = Get-Content -Raw -Encoding UTF8 (Join-Path $ProjectRoot 'db\schema-h2.sql')
foreach ($table in $required) {
    if ($schema -notmatch "(?i)CREATE\s+TABLE\s+$table") { throw "Missing table: $table" }
}
Write-Host 'Build and schema checks passed.'
