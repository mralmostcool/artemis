# 01-contract-operations.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

Write-Host "1. Fetching all contracts..." -ForegroundColor Cyan
curl.exe -s -X GET "$($vars.baseURL)/contracts" -H "Authorization: Bearer $($vars.adminToken)"
Write-Host "`n"

if (![string]::IsNullOrEmpty($vars.contractId)) {
    Write-Host "2. Fetching details for contract $($vars.contractId)..." -ForegroundColor Cyan
    curl.exe -s -X GET "$($vars.baseURL)/contracts/$($vars.contractId)" -H "Authorization: Bearer $($vars.adminToken)"
    Write-Host "`n"
}
