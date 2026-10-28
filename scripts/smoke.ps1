$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$root = Split-Path $PSScriptRoot -Parent
$settings = @{}
Get-Content (Join-Path $root '.env') | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $settings[$matches[1]] = $matches[2] }
}
$base = 'http://localhost:8080'
function Invoke-Api($session, $method, $path, $body = $null) {
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($method), "$base$path")
    $csrf = $session.Cookies.GetCookies([Uri]$base)['XSRF-TOKEN']
    if ($null -ne $csrf) { $request.Headers.Add('X-XSRF-TOKEN', $csrf.Value) }
    if ($null -ne $body) { $request.Content = [System.Net.Http.StringContent]::new($body, [Text.Encoding]::UTF8, 'application/json') }
    $response = $null
    try {
        $response = $session.Http.SendAsync($request).GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) { throw "HTTP $([int]$response.StatusCode): $method $path" }
        return ($response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json)
    } finally {
        if ($null -ne $response) { $response.Dispose() }
        $request.Dispose()
    }
}
function New-ApiClient($username, $password) {
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $client = [System.Net.Http.HttpClient]::new($handler)
    $session = [pscustomobject]@{ Http = $client; Cookies = $handler.CookieContainer }
    $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${username}:${password}"))
    $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Basic', $encoded)
    Invoke-Api $session 'GET' '/api/v1/csrf' | Out-Null
    return $session
}
$admin = New-ApiClient 'admin' $settings.ADMIN_PASSWORD
$reviewer = New-ApiClient 'reviewer' $settings.REVIEWER_PASSWORD
try {
    $product = Invoke-Api $admin 'POST' '/api/v1/products' '{"name":"Portfolio headphones","description":"Demo product for local API exploration"}'
    $path = "/api/v1/products/$($product.id)"
    $review = Invoke-Api $reviewer 'POST' "$path/reviews" '{"rating":5,"title":"Great audio","content":"Comfortable fit and clear sound."}'
    $summary = Invoke-Api $admin 'GET' "$path/summary"
    if ($summary.totalReviews -ne 1 -or $summary.averageRating -ne 5) { throw 'Unexpected summary' }
    $cached = Invoke-Api $admin 'GET' "$path/summary"
    if ($cached.totalReviews -ne 1) { throw 'Unexpected cached summary' }
    $analysis = Invoke-Api $admin 'POST' "$path/analysis" '{}'
    Write-Host "Smoke passed. Product: $($product.id); summary: $($summary.averageRating)/5; AI status: $($analysis.status)"
    Write-Host 'The demo product and review remain available in Swagger.'
} finally {
    $admin.Http.Dispose()
    $reviewer.Http.Dispose()
}
