# 01-ranks-crud.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

if ([string]::IsNullOrEmpty($vars.adminToken)) {
    Write-Host "Set adminToken in vars.json or environment before running." -ForegroundColor Yellow
}

Write-Host "1. Fetching all ranks..." -ForegroundColor Cyan
curl.exe -s -X GET "$($vars.baseURL)/seafarers/ranks" -H "Authorization: Bearer $($vars.adminToken)"
Write-Host "`n"

Write-Host "2. Creating a new rank..." -ForegroundColor Cyan
$newRank = curl.exe -s -X POST "$($vars.baseURL)/seafarers/ranks" `
  -H "Authorization: Bearer $($vars.adminToken)" `
  -H "Content-Type: application/json" `
  -d '{"name": "Master Mariner", "level": 10}'
Write-Host "Response: $newRank`n"
