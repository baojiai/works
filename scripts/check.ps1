$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1')
$required = @('system_user','customer_profile','engineer_profile','device_type','fault_type','service_area','engineer_skill','engineer_service_area','standard_time_slot','engineer_schedule','repair_request','appointment','appointment_change','repair_order','repair_record','order_status_log','part','part_inventory','part_request','part_request_item','inventory_flow','acceptance','rework','review','notification','engineer_application','engineer_application_skill','operation_log','system_config')
$schema = Get-Content -Raw -Encoding UTF8 (Join-Path $ProjectRoot 'database\after_sales.sql')
foreach ($table in $required) {
    if ($schema -notmatch "(?i)CREATE\s+TABLE\s+$table\s*\(") { throw "Missing table: $table" }
}
if (($schema | Select-String -Pattern '(?im)^CREATE\s+TABLE\s+' -AllMatches).Matches.Count -ne 29) {
    throw 'Expected exactly 29 tables in database/after_sales.sql.'
}
Write-Host 'Build and MySQL schema checks passed.'
