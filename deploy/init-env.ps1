param(
    [string]$OutputPath = (Join-Path $PSScriptRoot ".env")
)

$ErrorActionPreference = "Stop"

if (Test-Path -LiteralPath $OutputPath) {
    Write-Host "Environment file already exists: $OutputPath"
    exit 0
}

function New-Secret([int]$ByteCount = 24) {
    $bytes = [byte[]]::new($ByteCount)
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return ([BitConverter]::ToString($bytes) -replace "-", "").ToLowerInvariant()
}

$databasePassword = New-Secret 18
$secretKey = New-Secret 32
$minioPassword = New-Secret 18
$adminPassword = "Admin-" + (New-Secret 8) + "!"
$doctorPassword = "Doctor-" + (New-Secret 8) + "!"
$patientPassword = "Patient-" + (New-Secret 8) + "!"

$lines = @(
    "OMNIDAPT_ENVIRONMENT=development"
    "POSTGRES_PASSWORD=$databasePassword"
    "OMNIDAPT_DATABASE_URL=postgresql+psycopg://omnidapt:$databasePassword@postgres:5432/omnidapt"
    "OMNIDAPT_SECRET_KEY=$secretKey"
    "OMNIDAPT_REDIS_URL=redis://redis:6379/0"
    "OMNIDAPT_MINIO_ENDPOINT=minio:9000"
    "OMNIDAPT_MINIO_ACCESS_KEY=omnidapt"
    "OMNIDAPT_MINIO_SECRET_KEY=$minioPassword"
    "OMNIDAPT_MINIO_SECURE=false"
    "OMNIDAPT_MINIO_BUCKET=omnidapt"
    "OMNIDAPT_BOOTSTRAP_ADMIN_USERNAME=admin"
    "OMNIDAPT_BOOTSTRAP_ADMIN_PASSWORD=$adminPassword"
    "OMNIDAPT_BOOTSTRAP_DOCTOR_USERNAME=doctor"
    "OMNIDAPT_BOOTSTRAP_DOCTOR_PASSWORD=$doctorPassword"
    "OMNIDAPT_BOOTSTRAP_PATIENT_USERNAME=patient"
    "OMNIDAPT_BOOTSTRAP_PATIENT_PASSWORD=$patientPassword"
    "OMNIDAPT_CORS_ORIGINS=*"
)

[IO.File]::WriteAllLines($OutputPath, $lines, [Text.UTF8Encoding]::new($false))
Write-Host "Created $OutputPath with generated local-development secrets."
Write-Host "Bootstrap credentials are stored only in that ignored file."
