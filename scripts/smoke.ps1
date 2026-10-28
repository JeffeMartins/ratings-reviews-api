$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$settings = @{}
Get-Content (Join-Path $root '.env') | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $settings[$matches[1]] = $matches[2] }
}
$base = 'http://localhost:8080'
function BasicHeader($username, $password) {
    @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${username}:${password}")) }
}
$admin = BasicHeader 'admin' $settings.ADMIN_PASSWORD
$reviewer = BasicHeader 'reviewer' $settings.REVIEWER_PASSWORD
$product = Invoke-RestMethod "$base/api/v1/products" -Method Post -Headers $admin -ContentType 'application/json' -Body '{"name":"Portfolio headphones","description":"Demo product for local API exploration"}'
$path = "$base/api/v1/products/$($product.id)"
$review = Invoke-RestMethod "$path/reviews" -Method Post -Headers $reviewer -ContentType 'application/json' -Body '{"rating":5,"title":"Great audio","content":"Comfortable fit and clear sound."}'
$summary = Invoke-RestMethod "$path/summary"
if ($summary.totalReviews -ne 1 -or $summary.averageRating -ne 5) { throw 'Unexpected summary' }
$cached = Invoke-RestMethod "$path/summary"
if ($cached.totalReviews -ne 1) { throw 'Unexpected cached summary' }
$analysis = Invoke-RestMethod "$path/analysis" -Method Post -Headers $admin
Write-Host "Smoke passed. Product: $($product.id); summary: $($summary.averageRating)/5; AI status: $($analysis.status)"
Write-Host 'The demo product and review remain available in Swagger.'
