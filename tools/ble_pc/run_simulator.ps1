$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$python = Join-Path $root ".venv\Scripts\python.exe"
if (-not (Test-Path -LiteralPath $python)) {
    throw "Simulator environment is missing. Run setup_simulator.ps1 first."
}
& $python (Join-Path $root "simulator_app.py") @args
