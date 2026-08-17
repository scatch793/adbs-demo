from __future__ import annotations

import os
import tempfile
from pathlib import Path

import pytest
from fastapi.testclient import TestClient


TEST_ROOT = Path(tempfile.mkdtemp(prefix="omnidapt-backend-tests-"))
os.environ["OMNIDAPT_DATABASE_URL"] = f"sqlite:///{(TEST_ROOT / 'test.db').as_posix()}"
os.environ["OMNIDAPT_STORAGE_ROOT"] = str(TEST_ROOT / "objects")
os.environ["OMNIDAPT_SECRET_KEY"] = "test-secret-key-that-is-at-least-32-bytes-long"
os.environ["OMNIDAPT_BOOTSTRAP_ADMIN_PASSWORD"] = "Admin-Test-Password-2026"
os.environ["OMNIDAPT_BOOTSTRAP_DOCTOR_PASSWORD"] = "Doctor-Test-Password-2026"
os.environ["OMNIDAPT_BOOTSTRAP_PATIENT_PASSWORD"] = "Patient-Test-Password-2026"

from app.main import app  # noqa: E402


@pytest.fixture(scope="session")
def client() -> TestClient:
    with TestClient(app) as test_client:
        yield test_client


def login_headers(client: TestClient, username: str, password: str) -> dict[str, str]:
    response = client.post("/auth/login", json={"username": username, "password": password})
    assert response.status_code == 200, response.text
    if response.json()["user"]["must_change_password"]:
        changed = client.post(
            "/auth/change-password",
            headers={"Authorization": f"Bearer {response.json()['access_token']}"},
            json={
                "current_password": password,
                "new_password": f"{username.title()}-Changed-Password-2026",
            },
        )
        assert changed.status_code == 200, changed.text
        response = changed
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


@pytest.fixture(scope="session")
def admin_headers(client: TestClient) -> dict[str, str]:
    return login_headers(client, "admin", "Admin-Test-Password-2026")


@pytest.fixture(scope="session")
def doctor_headers(client: TestClient) -> dict[str, str]:
    return login_headers(client, "doctor", "Doctor-Test-Password-2026")


@pytest.fixture(scope="session")
def patient_headers(client: TestClient) -> dict[str, str]:
    return login_headers(client, "patient", "Patient-Test-Password-2026")
