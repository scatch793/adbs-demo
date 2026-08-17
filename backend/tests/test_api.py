from __future__ import annotations

import io
import uuid
from datetime import datetime, timezone

import numpy as np
from fastapi.testclient import TestClient
from app.database import SessionLocal
from app.models import OptimizationTask
from app.services.algorithm import default_model


def unlock_optimization_observation(task_id: str) -> None:
    with SessionLocal() as db:
        task = db.get(OptimizationTask, task_id)
        assert task is not None
        task.eligible_at = datetime.now(timezone.utc)
        db.commit()


def test_health_and_role_bound_patient_visibility(
    client: TestClient,
    doctor_headers: dict[str, str],
    patient_headers: dict[str, str],
) -> None:
    assert client.get("/health").json()["clinical_use"] is False
    doctor_patients = client.get("/patients", headers=doctor_headers)
    patient_patients = client.get("/patients", headers=patient_headers)
    assert doctor_patients.status_code == 200
    assert patient_patients.status_code == 200
    assert doctor_patients.json()[0]["code"] == "P001"
    assert patient_patients.json()[0]["code"] == "P001"


def test_admin_created_account_must_change_password_before_data_access(
    client: TestClient,
    admin_headers: dict[str, str],
) -> None:
    suffix = uuid.uuid4().hex[:8]
    username = f"forced-{suffix}"
    temporary = "Temporary-Password-2026"
    created = client.post(
        "/admin/users",
        headers=admin_headers,
        json={
            "username": username,
            "temporary_password": temporary,
            "role": "patient",
            "display_name": f"Research {suffix}",
            "patient_code": f"T-{suffix}",
        },
    )
    assert created.status_code == 201
    login = client.post("/auth/login", json={"username": username, "password": temporary})
    token = login.json()["access_token"]
    assert client.get("/patients", headers={"Authorization": f"Bearer {token}"}).status_code == 403
    changed = client.post(
        "/auth/change-password",
        headers={"Authorization": f"Bearer {token}"},
        json={"current_password": temporary, "new_password": "Changed-Password-2026"},
    )
    assert changed.status_code == 200
    fresh = changed.json()["access_token"]
    assert client.get("/patients", headers={"Authorization": f"Bearer {fresh}"}).status_code == 200


def test_versioned_model_import(
    client: TestClient,
    doctor_headers: dict[str, str],
    patient_headers: dict[str, str],
) -> None:
    patient_id = client.get("/patients", headers=patient_headers).json()[0]["id"]
    response = client.post(
        "/models",
        headers=doctor_headers,
        json={
            "patient_id": patient_id,
            "payload": default_model(),
            "metrics": {"clinical_validation": False},
            "approve": True,
        },
    )
    assert response.status_code == 201, response.text
    assert response.json()["status"] == "approved"


def test_symptom_and_medication_events_are_idempotent(
    client: TestClient,
    patient_headers: dict[str, str],
) -> None:
    symptom_event = str(uuid.uuid4())
    payload = {
        "event_id": symptom_event,
        "tremor": 2,
        "rigidity": 1,
        "speech": 0,
        "note": "offline queued event",
    }
    first = client.post("/symptoms", json=payload, headers=patient_headers)
    second = client.post("/symptoms", json=payload, headers=patient_headers)
    assert first.status_code == 201
    assert second.status_code == 201
    assert not first.json()["deduplicated"]
    assert second.json()["deduplicated"]

    medication_event = str(uuid.uuid4())
    medication = {
        "event_id": medication_event,
        "medication_name": "左旋多巴",
        "status": "taken",
    }
    assert client.post("/medications", json=medication, headers=patient_headers).status_code == 201
    assert client.post("/medications", json=medication, headers=patient_headers).json()["deduplicated"]


def test_bound_patient_can_create_persistent_chat(
    client: TestClient,
    patient_headers: dict[str, str],
) -> None:
    patient_id = client.get("/patients", headers=patient_headers).json()[0]["id"]
    care_team = client.get(
        f"/patients/{patient_id}/care-team",
        headers=patient_headers,
    )
    assert care_team.status_code == 200
    doctor = care_team.json()[0]
    session = client.post(
        "/chat-sessions",
        headers=patient_headers,
        json={"patient_id": patient_id, "doctor_user_id": doctor["id"]},
    )
    assert session.status_code == 201
    event_id = str(uuid.uuid4())
    sent = client.post(
        f"/chat-sessions/{session.json()['id']}/messages",
        headers=patient_headers,
        json={"event_id": event_id, "content": "offline-safe chat"},
    )
    assert sent.status_code == 201
    messages = client.get(
        f"/chat-sessions/{session.json()['id']}/messages",
        headers=patient_headers,
    ).json()
    assert any(row["event_id"] == event_id for row in messages)


def test_unsafe_proposal_cannot_be_approved(
    client: TestClient,
    doctor_headers: dict[str, str],
    patient_headers: dict[str, str],
) -> None:
    patient_id = client.get("/patients", headers=patient_headers).json()[0]["id"]
    task = client.post(
        "/optimization-tasks",
        headers=doctor_headers,
        json={
            "patient_id": patient_id,
            "rounds": 7,
            "current_parameters": {
                "current_ma": 2.0,
                "pulse_width_us": 70,
                "frequency_hz": 130,
                "duty_cycle": 45,
            },
        },
    )
    assert task.status_code == 201
    unlock_optimization_observation(task.json()["id"])
    feedback = client.post(
        f"/optimization-tasks/{task.json()['id']}/feedback",
        headers=patient_headers,
        json={
            "event_id": str(uuid.uuid4()),
            "task_id": task.json()["id"],
            "answers": {
                "tremor_relief": 8,
                "rigidity_relief": 8,
                "speech_fluency": 7,
                "movement_fluency": 8,
                "task_ease": 7,
                "parameter_preference": 7,
            },
            "side_effects": {"paresthesia": 8},
            "parameters": {
                "current_ma": 2.0,
                "pulse_width_us": 70,
                "frequency_hz": 130,
                "duty_cycle": 45,
            },
        },
    )
    assert feedback.status_code == 201
    assert feedback.json()["status"] == "rejected"
    assert feedback.json()["proposal_id"] is None


def test_safe_proposal_dispatch_and_ack(
    client: TestClient,
    doctor_headers: dict[str, str],
    patient_headers: dict[str, str],
) -> None:
    patient_id = client.get("/patients", headers=patient_headers).json()[0]["id"]
    task = client.post(
        "/optimization-tasks",
        headers=doctor_headers,
        json={
            "patient_id": patient_id,
            "rounds": 7,
            "current_parameters": {
                "current_ma": 2.0,
                "pulse_width_us": 70,
                "frequency_hz": 130,
                "duty_cycle": 45,
            },
        },
    ).json()
    unlock_optimization_observation(task["id"])
    feedback = client.post(
        f"/optimization-tasks/{task['id']}/feedback",
        headers=patient_headers,
        json={
            "event_id": str(uuid.uuid4()),
            "task_id": task["id"],
            "answers": {
                "tremor_relief": 8,
                "rigidity_relief": 8,
                "speech_fluency": 7,
                "movement_fluency": 8,
                "task_ease": 7,
                "parameter_preference": 7,
            },
            "side_effects": {},
            "parameters": {
                "current_ma": 2.0,
                "pulse_width_us": 70,
                "frequency_hz": 130,
                "duty_cycle": 45,
            },
        },
    )
    assert feedback.status_code == 201, feedback.text
    assert feedback.json()["status"] == "submitted"
    dispatched = client.post(
        f"/approvals/{feedback.json()['proposal_id']}",
        headers=doctor_headers,
        json={"action": "approve", "note": "research simulator only"},
    )
    assert dispatched.status_code == 200, dispatched.text
    pending = client.get("/devices/commands/pending", headers=patient_headers).json()
    command = next(row for row in pending if row["id"] == dispatched.json()["command_id"])
    ack = client.post(
        f"/devices/commands/{command['id']}/ack",
        headers=patient_headers,
        json={
            "command_id": command["id"],
            "sequence": command["sequence"],
            "success": True,
            "status_code": "ok",
        },
    )
    assert ack.status_code == 200
    assert ack.json()["status"] == "acknowledged"


def test_waveform_upload_and_all_export_formats(
    client: TestClient,
    doctor_headers: dict[str, str],
    patient_headers: dict[str, str],
) -> None:
    patient_id = client.get("/patients", headers=patient_headers).json()[0]["id"]
    device = client.get("/devices", headers=patient_headers).json()[0]
    session = client.post(
        "/lfp-sessions",
        headers=patient_headers,
        json={
            "patient_id": patient_id,
            "device_id": device["id"],
            "purpose": "manual_recording",
            "sample_rate_hz": 256,
            "channels": 2,
            "recording_enabled": True,
        },
    )
    assert session.status_code == 201, session.text
    rng = np.random.default_rng(7)
    samples = rng.integers(-200, 200, size=(512, 2), dtype=np.int16)
    buffer = io.BytesIO()
    np.savez_compressed(buffer, samples=samples, sample_rate_hz=np.int32(256))
    upload = client.put(
        f"/lfp-sessions/{session.json()['id']}/waveform",
        headers=patient_headers,
        files={"waveform": ("recording.npz", buffer.getvalue(), "application/octet-stream")},
    )
    assert upload.status_code == 200, upload.text

    for export_format in ("pdf", "csv", "mat", "edf", "eml", "zip"):
        job = client.post(
            "/exports",
            headers=doctor_headers,
            json={"patient_id": patient_id, "format": export_format},
        )
        assert job.status_code == 201, job.text
        assert job.json()["status"] == "completed", job.text
        download = client.get(f"/exports/{job.json()['id']}/download", headers=doctor_headers)
        assert download.status_code == 200
        assert len(download.content) > 100


def test_doctor_can_complete_four_state_initialization_and_approve_model(
    client: TestClient,
    doctor_headers: dict[str, str],
) -> None:
    patient = client.get("/patients", headers=doctor_headers).json()[0]
    device = client.get(
        "/devices",
        headers=doctor_headers,
        params={"patient_id": patient["id"]},
    ).json()[0]
    created = client.post(
        "/initializations",
        headers=doctor_headers,
        json={
            "patient_id": patient["id"],
            "device_id": device["id"],
            "mode": "demo",
            "electrode_config": {"left": 6, "right": 2},
        },
    )
    assert created.status_code == 201, created.text
    initialization_id = created.json()["id"]
    rng = np.random.default_rng(77)
    time = np.arange(30 * 256) / 256.0
    definitions = {
        "OFF-Rest": (2.0, 0.3, 0.2),
        "OFF-Move": (1.8, 1.7, 1.0),
        "ON-Rest": (0.4, 0.3, 0.2),
        "ON-Move": (0.4, 1.8, 1.6),
    }
    for state, (beta16, beta28, gamma75) in definitions.items():
        base = (
            beta16 * np.sin(2 * np.pi * 16 * time)
            + beta28 * np.sin(2 * np.pi * 28 * time)
            + gamma75 * np.sin(2 * np.pi * 75 * time)
        )
        samples = np.column_stack(
            [
                base * 1200 + rng.normal(0, 120, time.size),
                base * 1000 + rng.normal(0, 120, time.size),
            ]
        ).astype(np.int16)
        session = client.post(
            "/lfp-sessions",
            headers=doctor_headers,
            json={
                "patient_id": patient["id"],
                "device_id": device["id"],
                "purpose": "baseline",
                "state_label": state,
                "sample_rate_hz": 256,
                "channels": 2,
                "recording_enabled": True,
            },
        )
        assert session.status_code == 201, session.text
        waveform = io.BytesIO()
        np.savez_compressed(
            waveform,
            samples=samples,
            sample_rate_hz=np.int32(256),
        )
        uploaded = client.put(
            f"/lfp-sessions/{session.json()['id']}/waveform",
            headers=doctor_headers,
            files={
                "waveform": (
                    f"{state}.npz",
                    waveform.getvalue(),
                    "application/octet-stream",
                )
            },
        )
        assert uploaded.status_code == 200, uploaded.text
        completed = client.post(
            f"/lfp-sessions/{session.json()['id']}/complete",
            headers=doctor_headers,
            json={"sample_count": samples.shape[0], "packet_loss_count": 0},
        )
        assert completed.status_code == 200, completed.text
        attached = client.post(
            f"/initializations/{initialization_id}/segments",
            headers=doctor_headers,
            json={
                "lfp_session_id": session.json()["id"],
                "state_label": state,
                "received_frames": 300,
                "packet_loss_count": 0,
                "crc_error_count": 0,
                "saturated_sample_count": 0,
                "impedance": {"left_kohm": 2.35, "right_kohm": 2.62},
            },
        )
        assert attached.status_code == 201, attached.text
        assert attached.json()["segments"][-1]["accepted"]

    analyzed = client.post(
        f"/initializations/{initialization_id}/analyze",
        headers=doctor_headers,
    )
    assert analyzed.status_code == 202, analyzed.text
    assert analyzed.json()["status"] == "review"
    assert analyzed.json()["frequency_results"]["strategy"] == "exclusive_fisher_intersection"
    assert analyzed.json()["model_version_id"]
    approved = client.post(
        f"/initializations/{initialization_id}/approve",
        headers=doctor_headers,
        json={"note": "deidentified research simulator validation"},
    )
    assert approved.status_code == 200, approved.text
    assert approved.json()["status"] == "approved"
