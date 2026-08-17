$ErrorActionPreference = "Stop"
$repo = Resolve-Path (Join-Path $PSScriptRoot "..")
$testTemp = Join-Path $repo "build\tmp"
New-Item -ItemType Directory -Force -Path $testTemp | Out-Null
$env:TEMP = $testTemp
$env:TMP = $testTemp
$env:GRADLE_USER_HOME = Join-Path $repo ".gradle-user-home"

Push-Location (Join-Path $repo "backend")
try {
    & ".\.venv\Scripts\python.exe" -m pytest tests -q
} finally {
    Pop-Location
}

Push-Location (Join-Path $repo "tools\ble_pc")
try {
    & ".\.venv\Scripts\python.exe" -m pytest test_protocol_engine.py -q
} finally {
    Pop-Location
}

Push-Location $repo
try {
    & ".\gradlew.bat" :protocol:testDebugUnitTest :app:testDebugUnitTest --no-daemon --max-workers=1
} finally {
    Pop-Location
}
