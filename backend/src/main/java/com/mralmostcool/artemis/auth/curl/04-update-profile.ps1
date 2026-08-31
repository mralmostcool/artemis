# 04-update-profile.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

if ([string]::IsNullOrEmpty($vars.adminToken)) {
    Write-Error "Admin token is missing. Run 01 first."
    exit 1
}

$newDisplayName = "Harambe Sanctuary Overlord"
$newPhoneNumber = "777-888-9999"

Write-Host "1. Updating Admin profile details..." -ForegroundColor Cyan
$updateBody = @{
    displayName = $newDisplayName
    phoneNumber = $newPhoneNumber
} | ConvertTo-Json

$null = curl.exe -s -X PUT $vars.authBaseURL `
  -H "Authorization: Bearer $($vars.adminToken)" `
  -H "Content-Type: application/json" `
  -d $updateBody | ConvertFrom-Json

Write-Host "2. Fetching profile to verify updates..." -ForegroundColor Cyan
$profileResponse = curl.exe -s -X GET $vars.authBaseURL `
  -H "Authorization: Bearer $($vars.adminToken)" | ConvertFrom-Json

Write-Host "`n=== UPDATE PROFILE SUMMARY ===" -ForegroundColor Green
Write-Host "Old Name: Master Harambe Admin"
Write-Host "New Name: $($profileResponse.displayName)"
Write-Host "Old Phone: 123-456-7890"
Write-Host "New Phone: $($profileResponse.phoneNumber)"
Write-Host "==============================`n"
