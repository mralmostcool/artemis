# 02-setup-member.ps1
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$varsPath = Join-Path $scriptPath "vars.json"
$vars = Get-Content $varsPath | ConvertFrom-Json

if ([string]::IsNullOrEmpty($vars.orgId)) {
    Write-Error "Please run '01-setup-admin.ps1' first to create an organization."
    exit 1
}

if ([string]::IsNullOrEmpty($vars.memberEmail)) {
    $randomId = Get-Random -Minimum 1000 -Maximum 9999
    $vars.memberEmail = "harambe-member-$randomId@example.com"
}

Write-Host "1. Signing up Member User on Supabase: $($vars.memberEmail)..." -ForegroundColor Cyan
$signupBody = @{
    email = $vars.memberEmail
    password = $vars.memberPassword
} | ConvertTo-Json

$signupResponse = curl.exe -s -X POST "$($vars.supabaseUrl)/signup" `
  -H "apikey: $($vars.anonKey)" `
  -H "Content-Type: application/json" `
  -d $signupBody | ConvertFrom-Json

if (-not $signupResponse.access_token) {
    Write-Error "Failed to sign up member user. Response: $signupResponse"
    exit 1
}

$vars.memberToken = $signupResponse.access_token
$vars.memberId = $signupResponse.user.id

Write-Host "2. Registering member profile on Artemis..." -ForegroundColor Cyan
$profileBody = @{
    organizationId = $vars.orgId
    role = "EMPLOYEE"
    displayName = "Simple Harambe Helper"
    phoneNumber = "987-654-3210"
} | ConvertTo-Json

$registerResponse = curl.exe -s -X POST $vars.authBaseURL `
  -H "Authorization: Bearer $($vars.memberToken)" `
  -H "Content-Type: application/json" `
  -d $profileBody | ConvertFrom-Json

if (-not $registerResponse.id) {
    Write-Error "Failed to register member profile on Artemis. Response: $registerResponse"
    exit 1
}

$vars | ConvertTo-Json | Out-File $varsPath -Encoding utf8

Write-Host "`n=== SETUP MEMBER SUMMARY ===" -ForegroundColor Green
Write-Host "Member Email: $($vars.memberEmail)"
Write-Host "Member User ID: $($vars.memberId)"
Write-Host "Belongs to Org ID: $($vars.orgId)"
Write-Host "Role: EMPLOYEE"
Write-Host "============================`n"
