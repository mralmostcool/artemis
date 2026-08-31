# 01-setup-admin.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

if ([string]::IsNullOrEmpty($vars.adminEmail)) {
    $randomId = Get-Random -Minimum 1000 -Maximum 9999
    $vars.adminEmail = "harambe-admin-$randomId@example.com"
}

Write-Host "1. Signing up Admin User on Supabase: $($vars.adminEmail)..." -ForegroundColor Cyan
$signupBody = @{
    email = $vars.adminEmail
    password = $vars.adminPassword
} | ConvertTo-Json

$signupResponse = curl.exe -s -X POST "$($vars.supabaseUrl)/signup" `
  -H "apikey: $($vars.anonKey)" `
  -H "Content-Type: application/json" `
  -d $signupBody | ConvertFrom-Json

if (-not $signupResponse.access_token) {
    Write-Error "Failed to sign up admin user. Response: $signupResponse"
    exit 1
}

$vars.adminToken = $signupResponse.access_token
$vars.adminId = $signupResponse.user.id

Write-Host "2. Registering profile on Artemis..." -ForegroundColor Cyan
$profileBody = @{
    organizationName = "Harambe Sanctuary"
    role = "ADMIN"
    displayName = "Master Harambe Admin"
    phoneNumber = "123-456-7890"
} | ConvertTo-Json

$registerResponse = curl.exe -s -X POST $vars.authBaseURL `
  -H "Authorization: Bearer $($vars.adminToken)" `
  -H "Content-Type: application/json" `
  -d $profileBody | ConvertFrom-Json

if (-not $registerResponse.organizationId) {
    Write-Error "Failed to register admin profile on Artemis. Response: $registerResponse"
    exit 1
}

$vars.orgId = $registerResponse.organizationId
$vars | ConvertTo-Json | Out-File $varsPath -Encoding utf8

Write-Host "`n=== SETUP ADMIN SUMMARY ===" -ForegroundColor Green
Write-Host "Admin Email: $($vars.adminEmail)"
Write-Host "Admin User ID: $($vars.adminId)"
Write-Host "Organization ID: $($vars.orgId)"
Write-Host "Organization Name: Harambe Sanctuary"
Write-Host "============================`n"
