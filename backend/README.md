# Ominidapt PD backend

This service is a research demonstration backend. It rejects non-simulated
devices and must not be used for clinical treatment or implanted-device
control.

## Local Python mode

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -e ".[test]"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --reload
```

SQLite and local object storage are used by default under `backend/data`.
Open `http://127.0.0.1:8000/docs` for the API description.

Default development accounts are read from `OMNIDAPT_BOOTSTRAP_*`
environment variables. The checked-in defaults are intentionally marked for
immediate password change and are not suitable for shared deployments.

## Docker mode

Copy `deploy/.env.example` to `deploy/.env`, replace every password and secret,
then run:

```powershell
docker compose --env-file .env -f docker-compose.yml up --build
```

The API is exposed on port 8000 and the MinIO console on port 9001.
