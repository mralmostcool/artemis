# 02-indos-master.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

Write-Host "1. Querying seafarers list..." -ForegroundColor Cyan
curl.exe -s -X GET "$($vars.baseURL)/seafarers" -H "Authorization: Bearer $($vars.adminToken)"
Write-Host "`n"

Write-Host "2. Querying specific INDoS 66ZZ888..." -ForegroundColor Cyan
curl.exe -s -X GET "$($vars.baseURL)/seafarers?indos=66ZZ888" -H "Authorization: Bearer $($vars.adminToken)"
Write-Host "`n"
