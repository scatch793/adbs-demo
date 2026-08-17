# Ominidapt Windows BLE simulator

`simulator_app.py` turns the Windows PC into the BLE peripheral used by the
Ominidapt PD tablet app. It replays only the locally deidentified P001 data and
exposes visible medication, movement, stimulation, impedance and fault controls.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe simulator_app.py
```

The original `scan_receive.py` remains a central-side diagnostic helper for the
Android peripheral simulator. The production research demonstration described
here uses the Windows peripheral and the Ominidapt PD tablet as BLE central.

Build a local, double-clickable Windows bundle:

```powershell
.\build_simulator.ps1
```

The bundle contains only the six explicitly copied deidentified P001 artifacts.
It must remain on the research PC and is not a public data release.
