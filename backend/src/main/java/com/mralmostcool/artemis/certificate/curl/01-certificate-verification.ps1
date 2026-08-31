# 01-certificate-verification.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

Write-Host "1. Fetching all initiated certificates..." -ForegroundColor Cyan
curl.exe -s -X GET "$($vars.baseURL)?status=INITIATED" -H "Authorization: Bearer $($vars.adminToken)"
Write-Host "`n"

if (![string]::IsNullOrEmpty($vars.qrHash)) {
    Write-Host "2. Verifying certificate via QR hash scan..." -ForegroundColor Cyan
    curl.exe -s -X GET "$($vars.baseURL)/verify/$($vars.qrHash)"
    Write-Host "`n"
}
