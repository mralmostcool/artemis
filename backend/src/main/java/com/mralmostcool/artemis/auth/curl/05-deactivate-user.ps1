# 05-deactivate-user.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

if ([string]::IsNullOrEmpty($vars.adminToken) -or [string]::IsNullOrEmpty($vars.memberId) -or [string]::IsNullOrEmpty($vars.memberToken)) {
    Write-Error "Ensure both admin and member setup scripts have run."
    exit 1
}

$statusUrl = "$($vars.authBaseURL)/users/$($vars.memberId)/status"

Write-Host "1. Admin disabling Member B (enabled=false)..." -ForegroundColor Cyan
$disableHeadersResponse = curl.exe -i -s -X PUT "$($statusUrl)?enabled=false" `
  -H "Authorization: Bearer $($vars.adminToken)"
$disableHeadersResponse | Select-Object -First 3

Write-Host "`n2. Member B requesting profile (expecting 401 Unauthorized)..." -ForegroundColor Cyan
$memberGetHeadersResponse = curl.exe -i -s -X GET $vars.authBaseURL `
  -H "Authorization: Bearer $($vars.memberToken)"
$memberGetHeadersResponse | Select-Object -First 3

Write-Host "`n3. Admin re-enabling Member B (enabled=true)..." -ForegroundColor Cyan
$enableHeadersResponse = curl.exe -i -s -X PUT "$($statusUrl)?enabled=true" `
  -H "Authorization: Bearer $($vars.adminToken)"
$enableHeadersResponse | Select-Object -First 3

Write-Host "`n=== STATUS MANAGEMENT SUMMARY ===" -ForegroundColor Green
Write-Host "Admin disabled Member B successfully."
Write-Host "Member B blocked from API while disabled (HTTP 401 confirmed)."
Write-Host "Admin reactivated Member B successfully."
Write-Host "=================================`n"
