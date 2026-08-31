# 03-get-profile.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

if ([string]::IsNullOrEmpty($vars.adminToken) -or [string]::IsNullOrEmpty($vars.memberToken)) {
    Write-Error "Ensure both admin and member setup scripts have run."
    exit 1
}

Write-Host "1. Fetching Admin Profile..." -ForegroundColor Cyan
$adminProfile = curl.exe -s -X GET $vars.authBaseURL `
  -H "Authorization: Bearer $($vars.adminToken)"
Write-Host "Response: $adminProfile`n"

Write-Host "2. Fetching Member Profile..." -ForegroundColor Cyan
$memberProfile = curl.exe -s -X GET $vars.authBaseURL `
  -H "Authorization: Bearer $($vars.memberToken)"
Write-Host "Response: $memberProfile`n"

Write-Host "=== GET PROFILES SUMMARY ===" -ForegroundColor Green
Write-Host "Successfully fetched profiles for both users."
Write-Host "============================`n"
