# 02-enrollments-checkout.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

if (![string]::IsNullOrEmpty($vars.courseId)) {
    Write-Host "1. Submitting checkout request..." -ForegroundColor Cyan
    curl.exe -s -X POST "$($vars.baseURL)/institutes/courses/$($vars.courseId)/checkout" `
      -H "Authorization: Bearer $($vars.adminToken)" `
      -H "Content-Type: application/json"
    Write-Host "`n"
}
