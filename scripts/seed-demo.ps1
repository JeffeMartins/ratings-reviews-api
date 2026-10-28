$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$settings = @{}
Get-Content (Join-Path $root '.env') | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $settings[$matches[1]] = $matches[2] }
}
$base = 'http://localhost:8080'
$work = Join-Path $root ('.tools/seed-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work -Force | Out-Null
function Invoke-DemoCurl($method, $path, $body = $null, $username = $null) {
    $config = @()
    $curlArgs = @('--silent', '--show-error', '--request', $method, '--output', (Join-Path $work 'response.json'), '--write-out', '%{http_code}')
    if ($username) {
        $password = if ($username -eq 'admin') { $settings.ADMIN_PASSWORD } else { $settings.REVIEWER_PASSWORD }
        $credential = "${username}:${password}".Replace('\', '\\').Replace('"', '\"')
        $config += 'user = "' + $credential + '"'
        $jar = Join-Path $work "$username.cookies"
        $curlArgs += @('--cookie', $jar, '--cookie-jar', $jar)
        if (Test-Path -LiteralPath $jar) {
            $tokenLine = Get-Content $jar | Where-Object { $_ -match "`tXSRF-TOKEN`t" } | Select-Object -Last 1
            if ($tokenLine) { $config += 'header = "X-XSRF-TOKEN: ' + ($tokenLine -split "`t")[-1] + '"' }
        }
    }
    if ($null -ne $body) {
        $bodyFile = Join-Path $work 'request.json'
        [IO.File]::WriteAllText($bodyFile, ($body | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
        $curlArgs += @('--header', 'Content-Type: application/json', '--data-binary', "@$bodyFile")
    }
    $status = ($config -join "`n") | & curl.exe --config - @curlArgs "$base$path"
    if ($LASTEXITCODE -ne 0) { throw "curl failed: $method $path" }
    $data = Get-Content (Join-Path $work 'response.json') -Raw | ConvertFrom-Json
    return [pscustomobject]@{ Status = [int]$status; Data = $data }
}
function Assert-Success($result) {
    if ($result.Status -lt 200 -or $result.Status -ge 300) { throw "HTTP $($result.Status): $($result.Data | ConvertTo-Json -Compress)" }
    return $result.Data
}
try {
    foreach ($username in @('admin', 'reviewer')) { Assert-Success (Invoke-DemoCurl 'GET' '/api/v1/csrf' $null $username) | Out-Null }
    $existing = Invoke-DemoCurl 'GET' '/api/v1/products/by-sku/FONE-BT-001'
    if ($existing.Status -eq 404) {
        $product = Assert-Success (Invoke-DemoCurl 'POST' '/api/v1/products' @{
            sku = 'FONE-BT-001'; name = 'Fone Bluetooth Aurora'; description = 'Produto ficticio para testar reviews, comentarios e notas.'
        } 'admin')
    } else { $product = (Assert-Success $existing).product }
    $path = "/api/v1/products/$($product.id)"
    $existingReviews = (Assert-Success (Invoke-DemoCurl 'GET' "$path/reviews?size=100")).content
    if ($existingReviews.author -notcontains 'reviewer') {
        Assert-Success (Invoke-DemoCurl 'POST' "$path/reviews" @{
            rating = 5; title = 'Excelente som e conforto'; content = 'Uso durante o trabalho e nas caminhadas. O som e limpo, a bateria dura bastante e o encaixe e confortavel.'
        } 'reviewer') | Out-Null
    }
    if ($existingReviews.author -notcontains 'admin') {
        Assert-Success (Invoke-DemoCurl 'POST' "$path/reviews" @{
            rating = 4; title = 'Bom custo-beneficio, microfone pode melhorar'; content = 'Boa qualidade de audio e conexao estavel. Em chamadas na rua, o microfone capta um pouco de ruido.'
        } 'admin') | Out-Null
    }
    $overview = Assert-Success (Invoke-DemoCurl 'GET' '/api/v1/products/by-sku/FONE-BT-001')
    $analysis = Assert-Success (Invoke-DemoCurl 'POST' '/api/v1/products/by-sku/FONE-BT-001/analysis' @{} 'admin')
    if ($overview.summary.totalReviews -ne 2 -or $overview.summary.averageRating -ne 4.5) { throw 'Demo data differs from expected 2 reviews and average 4.5.' }
    [pscustomobject]@{ sku = $product.sku; productId = $product.id; totalReviews = $overview.summary.totalReviews; averageRating = $overview.summary.averageRating; analysisStatus = $analysis.status } | ConvertTo-Json
} finally {
    # Remove only the explicit temporary files created by this invocation.
    foreach ($file in @('response.json', 'request.json', 'admin.cookies', 'reviewer.cookies')) {
        $path = Join-Path $work $file
        if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Force }
    }
    Remove-Item -LiteralPath $work -Force
}
