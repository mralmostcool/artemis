# 01-vessel-details.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

Write-Host "1. Fetching all shipping companies..." -ForegroundColor Cyan
curl.exe -s -X GET "$($vars.baseURL)/companies" -H "Authorization: Bearer $($vars.adminToken)"
Write-Host "`n"

if (![string]::IsNullOrEmpty($vars.companyId)) {
    Write-Host "2. Fetching vessels for company $($vars.companyId)..." -ForegroundColor Cyan
    curl.exe -s -X GET "$($vars.baseURL)/companies/$($vars.companyId)/vessels" -H "Authorization: Bearer $($vars.adminToken)"
    Write-Host "`n"
}
