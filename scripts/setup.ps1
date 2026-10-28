$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $root '.env'
if (Test-Path -LiteralPath $envFile) {
    Write-Host '.env already exists; preserving credentials.'
    exit 0
}
function New-LocalSecret {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
@"
DB_PASSWORD=$(New-LocalSecret)
ADMIN_PASSWORD=$(New-LocalSecret)
REVIEWER_PASSWORD=$(New-LocalSecret)
AI_ENABLED=false
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4.1-mini
"@ | Set-Content -LiteralPath $envFile -Encoding ascii
Write-Host 'Created .env with random local credentials. Keep this file private.'
