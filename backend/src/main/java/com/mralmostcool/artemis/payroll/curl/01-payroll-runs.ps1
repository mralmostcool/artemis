# 01-payroll-runs.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

Write-Host "1. Initiating monthly payroll run..." -ForegroundColor Cyan
curl.exe -s -X POST "$($vars.baseURL)/runs?startDate=2026-08-01&endDate=2026-08-31&targetCurrency=INR" `
  -H "Authorization: Bearer $($vars.adminToken)"
Write-Host "`n"

if (![string]::IsNullOrEmpty($vars.paySlipId)) {
    Write-Host "2. Fetching details for pay slip $($vars.paySlipId)..." -ForegroundColor Cyan
    curl.exe -s -X GET "$($vars.baseURL)/slips/$($vars.paySlipId)" -H "Authorization: Bearer $($vars.adminToken)"
    Write-Host "`n"
}
