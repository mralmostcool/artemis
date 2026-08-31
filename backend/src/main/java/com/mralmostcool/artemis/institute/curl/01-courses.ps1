# 01-courses.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

Write-Host "1. Fetching all institutes..." -ForegroundColor Cyan
curl.exe -s -X GET "$($vars.baseURL)/institutes" -H "Authorization: Bearer $($vars.adminToken)"
Write-Host "`n"

if (![string]::IsNullOrEmpty($vars.instituteId)) {
    Write-Host "2. Fetching courses for MTI $($vars.instituteId)..." -ForegroundColor Cyan
    curl.exe -s -X GET "$($vars.baseURL)/institutes/$($vars.instituteId)/courses" -H "Authorization: Bearer $($vars.adminToken)"
    Write-Host "`n"
}
