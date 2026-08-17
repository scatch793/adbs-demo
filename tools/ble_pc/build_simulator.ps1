$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$pythonExecutable = Join-Path $scriptRoot ".venv\Scripts\python.exe"

if (-not (Test-Path -LiteralPath $pythonExecutable)) {
    throw "Run setup_simulator.ps1 before packaging the simulator."
}

Push-Location $scriptRoot
try {
    $distExecutable = Join-Path $scriptRoot "dist\Ominidapt-PD-Simulator\Ominidapt-PD-Simulator.exe"
    $running = Get-CimInstance Win32_Process |
        Where-Object { $_.ExecutablePath -eq $distExecutable }
    if ($running) {
        throw "Close Ominidapt-PD-Simulator.exe before rebuilding it."
    }

    & $pythonExecutable -m PyInstaller `
        --noconfirm `
        --clean `
        --windowed `
        --name "Ominidapt-PD-Simulator" `
        --collect-all pyqtgraph `
        simulator_app.py
    if ($LASTEXITCODE -ne 0) {
        throw "PyInstaller failed with exit code $LASTEXITCODE"
    }

    $dataTarget = Join-Path $scriptRoot "dist\Ominidapt-PD-Simulator\data\p001"
    New-Item -ItemType Directory -Force -Path $dataTarget | Out-Null
    @(
        "off_rest.npz",
        "off_move.npz",
        "on_rest.npz",
        "on_move.npz",
        "manifest.json",
        "deidentification_report.json"
    ) | ForEach-Object {
        $source = Join-Path $repositoryRoot "private_data\p001\$_"
        if (-not (Test-Path -LiteralPath $source)) {
            throw "Missing deidentified P001 file: $source"
        }
        Copy-Item -LiteralPath $source -Destination $dataTarget -Force
    }

    Write-Host "Built: $distExecutable"
} finally {
    Pop-Location
}
