from __future__ import annotations

import io
from contextlib import asynccontextmanager
from datetime import datetime, timedelta, timezone
from typing import Any

import jwt
import numpy as np
from fastapi import (
    Depends,
    FastAPI,
    File,
    HTTPException,
    Query,
    UploadFile,
    WebSocket,
    WebSocketDisconnect,
)
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from .config import get_settings
from .database import Base, SessionLocal, engine, get_db
from .deps import (
    authenticated_user,
    current_user,
    ensure_patient_access,
    patient_for_user,
    require_roles,
)
from .models import (
    CareRelation,
    ChatMessage,
    ChatSession,
    Device,
    DeviceBinding,
    DeviceCommand,
    ExportJob,
    FeedbackResponse,
    InferenceResult,
    InitializationRun,
    InitializationSegment,
    InitializationStatus,
    LfpSession,
    MedicationEvent,
    ModelVersion,
    OptimizationTask,
    ParameterProposal,
    Patient,
    RefreshToken,
    SymptomEntry,
    User,
    UserRole,
    WorkflowStatus,
    utcnow,
)
from .schemas import (
    AdminUserCreate,
    CareBindingRequest,
    ChangePasswordRequest,
    ChatMessageCreate,
    ChatSessionCreate,
    DeviceAck,
    DeviceBindingRequest,
    DeviceCreate,
    ExportCreate,
    FeedbackCreate,
    FeedbackScoreRequest,
    InferenceCreate,
    InitializationApprove,
    InitializationCreate,
    InitializationSegmentCreate,
    InitializationTransition,
    LfpSessionComplete,
    LfpSessionCreate,
    LoginRequest,
    MedicationCreate,
    ModelImportRequest,
    OptimizationTaskCreate,
    PasswordResetRequest,
    PatientCreate,
    PatientUpdate,
    PatientView,
    ProposalReview,
    RefreshRequest,
    SafetyCheckRequest,
    StateInferRequest,
    SymptomCreate,
    TokenResponse,
    UserView,
)
from .security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    token_hash,
    verify_password,
)
from .services.algorithm import (
    check_safety,
    compute_feedback_score,
    default_model,
    infer_state_probabilities,
    recommend_next_parameter,
)
from .services.audit import audit
from .services.bootstrap import bootstrap_demo_data
from .services.exporter import build_export
from .services.storage import storage
from .services.initialization import (
    analyze_initialization,
    calculate_segment_quality,
    serialize_initialization,
)
from .services.websocket import hub


settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        bootstrap_demo_data(db)
    yield


app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    description="Research demonstration only. This server must not control implanted devices.",
    lifespan=lifespan,
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=settings.cors_origin_list != ["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


def _tokens(db: Session, user: User) -> TokenResponse:
    access = create_access_token(user.id, user.role.value)
    refresh, expires_at = create_refresh_token(user.id)
    db.add(
        RefreshToken(
            user_id=user.id,
            token_hash=token_hash(refresh),
            expires_at=expires_at,
        )
    )
    db.commit()
    return TokenResponse(
        access_token=access,
        refresh_token=refresh,
        expires_in_seconds=settings.access_token_minutes * 60,
        user=UserView.model_validate(user),
    )


def _owned_patient_id(db: Session, user: User, requested: str | None) -> str:
    if requested:
        return ensure_patient_access(db, user, requested).id
    if user.role == UserRole.PATIENT:
        return patient_for_user(db, user).id
    raise HTTPException(status_code=422, detail="patient_id is required")


def _serialize_model(row: ModelVersion) -> dict[str, Any]:
    return {
        "id": row.id,
        "patient_id": row.patient_id,
        "version": row.version,
        "status": row.status.value,
        "algorithm": row.algorithm,
        "payload": row.payload,
        "metrics": row.metrics,
        "created_at": row.created_at,
        "approved_at": row.approved_at,
    }


async def _websocket_user(websocket: WebSocket, db: Session) -> User:
    token = websocket.query_params.get("token")
    if not token:
        await websocket.close(code=4401, reason="missing token")
        raise WebSocketDisconnect(4401)
    try:
        payload = decode_token(token, "access")
    except jwt.PyJWTError:
        await websocket.close(code=4401, reason="invalid token")
        raise WebSocketDisconnect(4401)
    user = db.get(User, payload.get("sub"))
    if user is None or not user.active or user.must_change_password:
        await websocket.close(code=4401, reason="inactive user")
        raise WebSocketDisconnect(4401)
    return user


@app.get("/health")
def health(db: Session = Depends(get_db)) -> dict[str, Any]:
    db.query(User).limit(1).all()
    return {
        "status": "ok",
        "environment": settings.environment,
        "storage": "minio" if storage.client else "local",
        "clinical_use": False,
    }


@app.post("/auth/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    user = db.query(User).filter(User.username == payload.username).one_or_none()
    if user is None or not user.active or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=401, detail="invalid username or password")
    audit(db, user, "auth.login", "user", user.id)
    return _tokens(db, user)


@app.post("/auth/refresh", response_model=TokenResponse)
def refresh(payload: RefreshRequest, db: Session = Depends(get_db)) -> TokenResponse:
    try:
        decoded = decode_token(payload.refresh_token, "refresh")
    except jwt.PyJWTError as exc:
        raise HTTPException(status_code=401, detail="invalid refresh token") from exc
    row = (
        db.query(RefreshToken)
        .filter(RefreshToken.token_hash == token_hash(payload.refresh_token))
        .one_or_none()
    )
    now = datetime.now(timezone.utc)
    if row is None or row.revoked_at is not None or row.expires_at.replace(tzinfo=timezone.utc) <= now:
        raise HTTPException(status_code=401, detail="refresh token revoked or expired")
    row.revoked_at = now
    user = db.get(User, decoded["sub"])
    if user is None or not user.active:
        raise HTTPException(status_code=401, detail="inactive user")
    return _tokens(db, user)


@app.get("/auth/me", response_model=UserView)
def me(user: User = Depends(current_user)) -> User:
    return user


@app.post("/auth/change-password", response_model=TokenResponse)
def change_password(
    payload: ChangePasswordRequest,
    user: User = Depends(authenticated_user),
    db: Session = Depends(get_db),
) -> TokenResponse:
    if not verify_password(payload.current_password, user.password_hash):
        raise HTTPException(status_code=400, detail="current password is incorrect")
    user.password_hash = hash_password(payload.new_password)
    user.must_change_password = False
    db.query(RefreshToken).filter(
        RefreshToken.user_id == user.id,
        RefreshToken.revoked_at.is_(None),
    ).update({RefreshToken.revoked_at: utcnow()})
    audit(db, user, "auth.change_password", "user", user.id)
    db.commit()
    return _tokens(db, user)


@app.get("/admin/users", response_model=list[UserView])
def list_users(
    _: User = Depends(require_roles(UserRole.ADMIN)),
    db: Session = Depends(get_db),
) -> list[User]:
    return db.query(User).order_by(User.created_at.desc()).all()


@app.post("/admin/users", response_model=UserView, status_code=201)
def create_user(
    payload: AdminUserCreate,
    admin: User = Depends(require_roles(UserRole.ADMIN)),
    db: Session = Depends(get_db),
) -> User:
    if db.query(User).filter(User.username == payload.username).first():
        raise HTTPException(status_code=409, detail="username already exists")
    user = User(
        username=payload.username,
        password_hash=hash_password(payload.temporary_password),
        role=payload.role,
        display_name=payload.display_name,
        must_change_password=True,
    )
    db.add(user)
    db.flush()
    if payload.role == UserRole.PATIENT:
        if not payload.patient_code:
            raise HTTPException(status_code=422, detail="patient_code is required for patient users")
        if db.query(Patient).filter(Patient.code == payload.patient_code).first():
            raise HTTPException(status_code=409, detail="patient code already exists")
        db.add(Patient(user_id=user.id, code=payload.patient_code, name=payload.display_name))
    audit(db, admin, "admin.create_user", "user", user.id, after={"role": user.role.value})
    db.commit()
    db.refresh(user)
    return user


@app.post("/admin/users/{user_id}/reset-password", response_model=UserView)
def reset_password(
    user_id: str,
    payload: PasswordResetRequest,
    admin: User = Depends(require_roles(UserRole.ADMIN)),
    db: Session = Depends(get_db),
) -> User:
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(status_code=404, detail="user not found")
    target.password_hash = hash_password(payload.temporary_password)
    target.must_change_password = True
    db.query(RefreshToken).filter(
        RefreshToken.user_id == target.id,
        RefreshToken.revoked_at.is_(None),
    ).update({RefreshToken.revoked_at: utcnow()})
    audit(db, admin, "admin.reset_password", "user", target.id)
    db.commit()
    return target


@app.post("/admin/care-relations", status_code=201)
def bind_care_relation(
    payload: CareBindingRequest,
    admin: User = Depends(require_roles(UserRole.ADMIN)),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    doctor = db.get(User, payload.doctor_user_id)
    patient = db.get(Patient, payload.patient_id)
    if doctor is None or doctor.role != UserRole.DOCTOR or patient is None:
        raise HTTPException(status_code=422, detail="invalid doctor or patient")
    existing = (
        db.query(CareRelation)
        .filter(
            CareRelation.doctor_user_id == doctor.id,
            CareRelation.patient_id == patient.id,
        )
        .one_or_none()
    )
    if existing is None:
        existing = CareRelation(doctor_user_id=doctor.id, patient_id=patient.id)
        db.add(existing)
    audit(db, admin, "admin.bind_care_relation", "patient", patient.id)
    db.commit()
    return {"id": existing.id}


@app.post("/admin/devices", status_code=201)
def create_device(
    payload: DeviceCreate,
    admin: User = Depends(require_roles(UserRole.ADMIN)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    if db.query(Device).filter(Device.serial_number == payload.serial_number).first():
        raise HTTPException(status_code=409, detail="device serial already exists")
    device = Device(**payload.model_dump())
    db.add(device)
    db.flush()
    audit(db, admin, "admin.create_simulated_device", "device", device.id)
    db.commit()
    return {
        "id": device.id,
        "serial_number": device.serial_number,
        "name": device.name,
        "simulated": device.simulated,
    }


@app.post("/devices/bindings", status_code=201)
def bind_device(
    payload: DeviceBindingRequest,
    admin: User = Depends(require_roles(UserRole.ADMIN)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    patient = db.get(Patient, payload.patient_id)
    device = db.get(Device, payload.device_id)
    if patient is None or device is None:
        raise HTTPException(status_code=404, detail="patient or device not found")
    if not device.simulated:
        raise HTTPException(status_code=422, detail="real implanted devices are disabled")
    binding = (
        db.query(DeviceBinding)
        .filter(
            DeviceBinding.patient_id == patient.id,
            DeviceBinding.device_id == device.id,
        )
        .one_or_none()
    )
    if binding is None:
        binding = DeviceBinding(patient_id=patient.id, device_id=device.id, active=True)
        db.add(binding)
    else:
        binding.active = True
    audit(db, admin, "admin.bind_simulated_device", "device", device.id, after={"patient_id": patient.id})
    db.commit()
    return {"id": binding.id, "patient_id": patient.id, "device_id": device.id, "active": True}


@app.get("/devices")
def list_devices(
    patient_id: str | None = None,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    resolved = _owned_patient_id(db, user, patient_id)
    rows = (
        db.query(Device, DeviceBinding)
        .join(DeviceBinding, DeviceBinding.device_id == Device.id)
        .filter(
            DeviceBinding.patient_id == resolved,
            DeviceBinding.active.is_(True),
        )
        .all()
    )
    return [
        {
            "id": device.id,
            "serial_number": device.serial_number,
            "name": device.name,
            "simulated": device.simulated,
            "protocol_version": device.protocol_version,
            "battery_percent": device.battery_percent,
            "last_seen_at": device.last_seen_at,
            "binding_id": binding.id,
        }
        for device, binding in rows
    ]


@app.get("/patients", response_model=list[PatientView])
def list_patients(user: User = Depends(current_user), db: Session = Depends(get_db)) -> list[Patient]:
    query = db.query(Patient)
    if user.role == UserRole.PATIENT:
        query = query.filter(Patient.user_id == user.id)
    elif user.role == UserRole.DOCTOR:
        query = query.join(CareRelation).filter(CareRelation.doctor_user_id == user.id)
    return query.order_by(Patient.code).all()


@app.post("/patients", response_model=PatientView, status_code=201)
def create_patient(
    payload: PatientCreate,
    user: User = Depends(require_roles(UserRole.ADMIN, UserRole.DOCTOR)),
    db: Session = Depends(get_db),
) -> Patient:
    if db.query(Patient).filter(Patient.code == payload.code).first():
        raise HTTPException(status_code=409, detail="patient code already exists")
    patient = Patient(**payload.model_dump())
    db.add(patient)
    db.flush()
    if user.role == UserRole.DOCTOR:
        db.add(CareRelation(doctor_user_id=user.id, patient_id=patient.id))
    audit(db, user, "patient.create", "patient", patient.id, after={"code": patient.code})
    db.commit()
    db.refresh(patient)
    return patient


@app.get("/patients/{patient_id}", response_model=PatientView)
def get_patient(
    patient_id: str,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> Patient:
    return ensure_patient_access(db, user, patient_id)


@app.get("/patients/{patient_id}/care-team", response_model=list[UserView])
def get_care_team(
    patient_id: str,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[User]:
    ensure_patient_access(db, user, patient_id)
    doctor_ids = [
        relation.doctor_user_id
        for relation in db.query(CareRelation).filter(CareRelation.patient_id == patient_id).all()
    ]
    if not doctor_ids:
        return []
    return db.query(User).filter(User.id.in_(doctor_ids), User.active.is_(True)).all()


@app.patch("/patients/{patient_id}", response_model=PatientView)
def update_patient(
    patient_id: str,
    payload: PatientUpdate,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> Patient:
    patient = ensure_patient_access(db, user, patient_id)
    if user.role == UserRole.PATIENT:
        allowed = {"emergency_contact", "emergency_phone"}
        changed = set(payload.model_dump(exclude_unset=True))
        if not changed.issubset(allowed):
            raise HTTPException(status_code=403, detail="patients may only edit emergency contact")
    before = PatientView.model_validate(patient).model_dump(mode="json")
    for key, value in payload.model_dump(exclude_unset=True).items():
        setattr(patient, key, value)
    audit(
        db,
        user,
        "patient.update",
        "patient",
        patient.id,
        before=before,
        after=PatientView.model_validate(patient).model_dump(mode="json"),
    )
    db.commit()
    return patient


@app.post("/symptoms", status_code=201)
def create_symptom(
    payload: SymptomCreate,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    patient_id = _owned_patient_id(db, user, payload.patient_id)
    existing = db.query(SymptomEntry).filter(SymptomEntry.event_id == payload.event_id).one_or_none()
    if existing:
        return {"id": existing.id, "deduplicated": True}
    row = SymptomEntry(
        patient_id=patient_id,
        event_id=payload.event_id,
        tremor=payload.tremor,
        rigidity=payload.rigidity,
        speech=payload.speech,
        note=payload.note,
        recorded_at=payload.recorded_at or utcnow(),
    )
    db.add(row)
    audit(db, user, "symptom.create", "symptom", row.id, after={"patient_id": patient_id})
    db.commit()
    return {"id": row.id, "deduplicated": False}


@app.get("/symptoms")
def list_symptoms(
    patient_id: str | None = Query(default=None),
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    resolved = _owned_patient_id(db, user, patient_id)
    rows = (
        db.query(SymptomEntry)
        .filter(SymptomEntry.patient_id == resolved)
        .order_by(SymptomEntry.recorded_at.desc())
        .all()
    )
    return [
        {
            "id": row.id,
            "event_id": row.event_id,
            "tremor": row.tremor,
            "rigidity": row.rigidity,
            "speech": row.speech,
            "note": row.note,
            "recorded_at": row.recorded_at,
        }
        for row in rows
    ]


@app.post("/medications", status_code=201)
def create_medication(
    payload: MedicationCreate,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    patient_id = _owned_patient_id(db, user, payload.patient_id)
    existing = db.query(MedicationEvent).filter(MedicationEvent.event_id == payload.event_id).one_or_none()
    if existing:
        return {"id": existing.id, "deduplicated": True}
    row = MedicationEvent(
        patient_id=patient_id,
        event_id=payload.event_id,
        medication_name=payload.medication_name,
        status=payload.status,
        scheduled_at=payload.scheduled_at,
        recorded_at=payload.recorded_at or utcnow(),
    )
    db.add(row)
    audit(db, user, "medication.create", "medication", row.id, after={"patient_id": patient_id})
    db.commit()
    return {"id": row.id, "deduplicated": False}


@app.get("/medications")
def list_medications(
    patient_id: str | None = Query(default=None),
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    resolved = _owned_patient_id(db, user, patient_id)
    rows = (
        db.query(MedicationEvent)
        .filter(MedicationEvent.patient_id == resolved)
        .order_by(MedicationEvent.recorded_at.desc())
        .all()
    )
    return [
        {
            "id": row.id,
            "event_id": row.event_id,
            "medication_name": row.medication_name,
            "status": row.status,
            "scheduled_at": row.scheduled_at,
            "recorded_at": row.recorded_at,
        }
        for row in rows
    ]


@app.post("/lfp-sessions", status_code=201)
def create_lfp_session(
    payload: LfpSessionCreate,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    ensure_patient_access(db, user, payload.patient_id)
    device = db.get(Device, payload.device_id)
    if device is None or not device.simulated:
        raise HTTPException(status_code=422, detail="only simulated devices are supported")
    binding = (
        db.query(DeviceBinding)
        .filter(
            DeviceBinding.patient_id == payload.patient_id,
            DeviceBinding.device_id == payload.device_id,
            DeviceBinding.active.is_(True),
        )
        .one_or_none()
    )
    if binding is None:
        raise HTTPException(status_code=403, detail="device is not bound to patient")
    row = LfpSession(**payload.model_dump())
    db.add(row)
    audit(db, user, "lfp.start", "lfp_session", row.id, after={"purpose": row.purpose})
    db.commit()
    return {"id": row.id, "started_at": row.started_at, "recording_enabled": row.recording_enabled}


@app.put("/lfp-sessions/{session_id}/waveform")
async def upload_lfp_waveform(
    session_id: str,
    waveform: UploadFile = File(...),
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    session = db.get(LfpSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="LFP session not found")
    ensure_patient_access(db, user, session.patient_id)
    content = await waveform.read()
    try:
        with np.load(io.BytesIO(content), allow_pickle=False) as payload:
            samples = np.asarray(payload["samples"])
            sample_rate = int(payload["sample_rate_hz"])
    except Exception as exc:
        raise HTTPException(status_code=422, detail="waveform must be a valid NPZ file") from exc
    if samples.ndim != 2 or samples.shape[1] != session.channels or sample_rate != session.sample_rate_hz:
        raise HTTPException(status_code=422, detail="waveform shape or sample rate does not match session")
    key = f"lfp/{session.patient_id}/{session.id}.npz"
    storage.put_bytes(key, content, "application/octet-stream")
    session.object_key = key
    session.sample_count = int(samples.shape[0])
    audit(db, user, "lfp.upload", "lfp_session", session.id, after={"sample_count": session.sample_count})
    db.commit()
    return {"object_key": key, "sample_count": session.sample_count}


@app.post("/lfp-sessions/{session_id}/complete")
def complete_lfp_session(
    session_id: str,
    payload: LfpSessionComplete,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    session = db.get(LfpSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="LFP session not found")
    ensure_patient_access(db, user, session.patient_id)
    session.sample_count = max(session.sample_count, payload.sample_count)
    session.packet_loss_count = payload.packet_loss_count
    session.ended_at = utcnow()
    audit(db, user, "lfp.complete", "lfp_session", session.id)
    db.commit()
    return {
        "id": session.id,
        "sample_count": session.sample_count,
        "packet_loss_count": session.packet_loss_count,
        "ended_at": session.ended_at,
    }


@app.post("/initializations", status_code=201)
def create_initialization(
    payload: InitializationCreate,
    user: User = Depends(require_roles(UserRole.ADMIN, UserRole.DOCTOR)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    ensure_patient_access(db, user, payload.patient_id)
    device = db.get(Device, payload.device_id)
    if device is None or not device.simulated:
        raise HTTPException(status_code=422, detail="initialization requires a simulated device")
    binding = (
        db.query(DeviceBinding)
        .filter(
            DeviceBinding.patient_id == payload.patient_id,
            DeviceBinding.device_id == payload.device_id,
            DeviceBinding.active.is_(True),
        )
        .one_or_none()
    )
    if binding is None:
        raise HTTPException(status_code=403, detail="device is not bound to patient")
    demo = payload.mode == "demo"
    row = InitializationRun(
        patient_id=payload.patient_id,
        device_id=payload.device_id,
        created_by=user.id,
        mode=payload.mode,
        status=InitializationStatus.CONFIGURING,
        settle_seconds=5 if demo else 15,
        capture_seconds=30 if demo else 180,
        electrode_config=payload.electrode_config,
    )
    db.add(row)
    db.flush()
    audit(
        db,
        user,
        "initialization.create",
        "initialization",
        row.id,
        after={"mode": row.mode, "device_id": row.device_id},
    )
    db.commit()
    return serialize_initialization(row, [])


@app.get("/initializations/{initialization_id}")
def get_initialization(
    initialization_id: str,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    row = db.get(InitializationRun, initialization_id)
    if row is None:
        raise HTTPException(status_code=404, detail="initialization not found")
    ensure_patient_access(db, user, row.patient_id)
    segments = (
        db.query(InitializationSegment)
        .filter(InitializationSegment.initialization_id == row.id)
        .all()
    )
    return serialize_initialization(row, segments)


@app.get("/patients/{patient_id}/initialization")
def patient_initializations(
    patient_id: str,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    ensure_patient_access(db, user, patient_id)
    rows = (
        db.query(InitializationRun)
        .filter(InitializationRun.patient_id == patient_id)
        .order_by(InitializationRun.created_at.desc())
        .all()
    )
    return [
        serialize_initialization(
            row,
            db.query(InitializationSegment)
            .filter(InitializationSegment.initialization_id == row.id)
            .all(),
        )
        for row in rows
    ]


@app.post("/initializations/{initialization_id}/transition")
def transition_initialization(
    initialization_id: str,
    payload: InitializationTransition,
    user: User = Depends(require_roles(UserRole.ADMIN, UserRole.DOCTOR)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    row = db.get(InitializationRun, initialization_id)
    if row is None:
        raise HTTPException(status_code=404, detail="initialization not found")
    ensure_patient_access(db, user, row.patient_id)
    if payload.action == "configure":
        row.status = InitializationStatus.CONFIGURING
    elif payload.action == "capture":
        row.status = InitializationStatus.CAPTURING
    else:
        row.status = InitializationStatus.CANCELLED
    row.updated_at = utcnow()
    audit(
        db,
        user,
        f"initialization.{payload.action}",
        "initialization",
        row.id,
        after={"status": row.status.value},
    )
    db.commit()
    segments = (
        db.query(InitializationSegment)
        .filter(InitializationSegment.initialization_id == row.id)
        .all()
    )
    return serialize_initialization(row, segments)


@app.post("/initializations/{initialization_id}/segments", status_code=201)
def attach_initialization_segment(
    initialization_id: str,
    payload: InitializationSegmentCreate,
    user: User = Depends(require_roles(UserRole.ADMIN, UserRole.DOCTOR)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    run = db.get(InitializationRun, initialization_id)
    if run is None:
        raise HTTPException(status_code=404, detail="initialization not found")
    ensure_patient_access(db, user, run.patient_id)
    if run.status in {
        InitializationStatus.ANALYZING,
        InitializationStatus.REVIEW,
        InitializationStatus.APPROVED,
        InitializationStatus.CANCELLED,
    }:
        raise HTTPException(status_code=409, detail="initialization no longer accepts segments")
    session = db.get(LfpSession, payload.lfp_session_id)
    if (
        session is None
        or session.patient_id != run.patient_id
        or session.device_id != run.device_id
        or session.purpose != "baseline"
        or session.state_label != payload.state_label
        or session.ended_at is None
    ):
        raise HTTPException(status_code=422, detail="baseline LFP session does not match initialization")
    quality, accepted = calculate_segment_quality(
        run,
        session,
        payload.received_frames,
        payload.packet_loss_count,
        payload.crc_error_count,
        payload.saturated_sample_count,
    )
    order = ("OFF-Rest", "OFF-Move", "ON-Rest", "ON-Move").index(payload.state_label)
    row = (
        db.query(InitializationSegment)
        .filter(
            InitializationSegment.initialization_id == run.id,
            InitializationSegment.state_label == payload.state_label,
        )
        .one_or_none()
    )
    if row is None:
        row = InitializationSegment(
            initialization_id=run.id,
            state_label=payload.state_label,
            order_index=order,
            lfp_session_id=session.id,
            sample_count=session.sample_count,
        )
        db.add(row)
    else:
        row.lfp_session_id = session.id
        row.sample_count = session.sample_count
    row.received_frames = payload.received_frames
    row.packet_loss_count = payload.packet_loss_count
    row.crc_error_count = payload.crc_error_count
    row.saturated_sample_count = payload.saturated_sample_count
    row.impedance = payload.impedance
    row.quality = quality
    row.accepted = accepted
    run.status = InitializationStatus.CAPTURING
    run.current_state = payload.state_label
    run.updated_at = utcnow()
    audit(
        db,
        user,
        "initialization.segment",
        "initialization",
        run.id,
        after={"state": payload.state_label, "accepted": accepted, "quality": quality},
    )
    db.commit()
    return serialize_initialization(
        run,
        db.query(InitializationSegment)
        .filter(InitializationSegment.initialization_id == run.id)
        .all(),
    )


@app.post("/initializations/{initialization_id}/analyze", status_code=202)
def start_initialization_analysis(
    initialization_id: str,
    user: User = Depends(require_roles(UserRole.ADMIN, UserRole.DOCTOR)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    row = db.get(InitializationRun, initialization_id)
    if row is None:
        raise HTTPException(status_code=404, detail="initialization not found")
    ensure_patient_access(db, user, row.patient_id)
    segments = (
        db.query(InitializationSegment)
        .filter(InitializationSegment.initialization_id == row.id)
        .all()
    )
    states = {segment.state_label for segment in segments if segment.accepted}
    required = {"OFF-Rest", "OFF-Move", "ON-Rest", "ON-Move"}
    if states != required:
        raise HTTPException(
            status_code=409,
            detail={"accepted_states": sorted(states), "missing": sorted(required - states)},
        )
    row.status = InitializationStatus.ANALYZING
    row.current_state = None
    row.error = None
    row.analysis_stage = "queued"
    row.progress_percent = 2
    row.updated_at = utcnow()
    audit(db, user, "initialization.analyze", "initialization", row.id)
    db.commit()
    if settings.redis_url:
        from .worker import analyze_initialization_task

        analyze_initialization_task.delay(row.id)
        return serialize_initialization(row, segments)
    try:
        return analyze_initialization(row.id)
    except Exception as error:
        raise HTTPException(status_code=422, detail=str(error)) from error


@app.post("/initializations/{initialization_id}/approve")
def approve_initialization(
    initialization_id: str,
    payload: InitializationApprove,
    user: User = Depends(require_roles(UserRole.ADMIN, UserRole.DOCTOR)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    row = db.get(InitializationRun, initialization_id)
    if row is None:
        raise HTTPException(status_code=404, detail="initialization not found")
    ensure_patient_access(db, user, row.patient_id)
    if row.status != InitializationStatus.REVIEW or row.model_version_id is None:
        raise HTTPException(status_code=409, detail="initialization is not ready for approval")
    model = db.get(ModelVersion, row.model_version_id)
    if model is None:
        raise HTTPException(status_code=409, detail="candidate model is missing")
    model.status = WorkflowStatus.APPROVED
    model.approved_at = utcnow()
    training = dict(model.payload.get("training", {}))
    training["approved"] = True
    model.payload = {**model.payload, "training": training}
    row.status = InitializationStatus.APPROVED
    row.analysis_stage = "approved"
    row.progress_percent = 100
    row.approved_at = utcnow()
    row.updated_at = utcnow()
    audit(
        db,
        user,
        "initialization.approve",
        "initialization",
        row.id,
        after={"model_version_id": model.id, "note": payload.note},
    )
    db.commit()
    segments = (
        db.query(InitializationSegment)
        .filter(InitializationSegment.initialization_id == row.id)
        .all()
    )
    return serialize_initialization(row, segments)


@app.post("/inferences", status_code=201)
async def create_inference(
    payload: InferenceCreate,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    ensure_patient_access(db, user, payload.patient_id)
    existing = db.query(InferenceResult).filter(InferenceResult.event_id == payload.event_id).one_or_none()
    if existing:
        return {"id": existing.id, "deduplicated": True}
    row = InferenceResult(
        **payload.model_dump(exclude={"recorded_at"}),
        recorded_at=payload.recorded_at or utcnow(),
    )
    db.add(row)
    db.commit()
    await hub.broadcast(
        f"monitor:{payload.patient_id}",
        {
            "type": "inference",
            "patient_id": payload.patient_id,
            "features": payload.features,
            "probabilities": payload.probabilities,
            "top_state": payload.top_state,
            "confidence": payload.confidence,
            "rejected": payload.rejected,
            "recorded_at": row.recorded_at.isoformat(),
        },
    )
    return {"id": row.id, "deduplicated": False}


@app.get("/models")
def list_models(
    patient_id: str,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    ensure_patient_access(db, user, patient_id)
    rows = (
        db.query(ModelVersion)
        .filter(ModelVersion.patient_id == patient_id)
        .order_by(ModelVersion.version.desc())
        .all()
    )
    return [_serialize_model(row) for row in rows]


@app.post("/models/default", status_code=201)
def create_default_model(
    patient_id: str,
    user: User = Depends(require_roles(UserRole.ADMIN, UserRole.DOCTOR)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    ensure_patient_access(db, user, patient_id)
    latest = (
        db.query(ModelVersion)
        .filter(ModelVersion.patient_id == patient_id)
        .order_by(ModelVersion.version.desc())
        .first()
    )
    row = ModelVersion(
        patient_id=patient_id,
        version=(latest.version + 1 if latest else 1),
        status=WorkflowStatus.APPROVED,
        payload=default_model(),
        metrics={"source": "default research demonstration model"},
        approved_at=utcnow(),
    )
    db.add(row)
    audit(db, user, "model.create_default", "model", row.id, after={"version": row.version})
    db.commit()
    return _serialize_model(row)


@app.post("/models", status_code=201)
def import_model(
    payload: ModelImportRequest,
    user: User = Depends(require_roles(UserRole.ADMIN, UserRole.DOCTOR)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    ensure_patient_access(db, user, payload.patient_id)
    required = {"schema_version", "algorithm", "feature_names", "scaler", "states", "rejection"}
    missing = sorted(required - payload.payload.keys())
    if missing or payload.payload.get("algorithm") != "five_feature_gmm":
        raise HTTPException(status_code=422, detail={"invalid_model": missing or ["algorithm"]})
    latest = (
        db.query(ModelVersion)
        .filter(ModelVersion.patient_id == payload.patient_id)
        .order_by(ModelVersion.version.desc())
        .first()
    )
    approved = payload.approve
    row = ModelVersion(
        patient_id=payload.patient_id,
        version=(latest.version + 1 if latest else 1),
        status=WorkflowStatus.APPROVED if approved else WorkflowStatus.DRAFT,
        algorithm="five_feature_gmm",
        payload=payload.payload,
        metrics=payload.metrics,
        approved_at=utcnow() if approved else None,
    )
    db.add(row)
    audit(
        db,
        user,
        "model.import",
        "model",
        row.id,
        after={"version": row.version, "approved": approved},
    )
    db.commit()
    return _serialize_model(row)


@app.post("/algorithm/infer-state")
def algorithm_infer(
    payload: StateInferRequest,
    _: User = Depends(current_user),
) -> dict[str, Any]:
    return infer_state_probabilities(payload.features, payload.model)


@app.post("/algorithm/score-feedback")
def algorithm_feedback_score(
    payload: FeedbackScoreRequest,
    _: User = Depends(current_user),
) -> dict[str, Any]:
    return compute_feedback_score(payload.answers, payload.side_effects)


@app.post("/algorithm/check-safety")
def algorithm_safety(
    payload: SafetyCheckRequest,
    _: User = Depends(current_user),
) -> dict[str, Any]:
    return check_safety(payload.candidate, payload.previous, payload.bounds)


def _optimization_history(db: Session, task: OptimizationTask) -> tuple[list[FeedbackResponse], list[dict[str, float]]]:
    rows = (
        db.query(FeedbackResponse)
        .filter(
            FeedbackResponse.task_id == task.id,
            FeedbackResponse.blocked.is_(False),
        )
        .order_by(FeedbackResponse.round_index.asc(), FeedbackResponse.created_at.asc())
        .all()
    )
    history = [
        {"current_ma": float(row.parameters["current_ma"]), "score": float(row.score)}
        for row in rows
        if "current_ma" in row.parameters
    ]
    return rows, history


def _proposal_view(row: ParameterProposal) -> dict[str, Any]:
    return {
        "id": row.id,
        "task_id": row.task_id,
        "patient_id": row.patient_id,
        "status": row.status.value,
        "round_index": row.round_index,
        "parameters": row.parameters,
        "score": row.score,
        "safety_result": row.safety_result,
        "acquisition": row.acquisition,
        "model_version": row.model_version,
        "review_note": row.review_note,
        "created_at": row.created_at,
    }


def _optimization_view(db: Session, task: OptimizationTask) -> dict[str, Any]:
    feedback_rows, history = _optimization_history(db, task)
    recommendation = recommend_next_parameter(
        history,
        task.safety_bounds,
        task.excluded_currents or [],
    )
    proposals = (
        db.query(ParameterProposal)
        .filter(ParameterProposal.task_id == task.id)
        .order_by(ParameterProposal.created_at.asc())
        .all()
    )
    now = utcnow()
    eligible_at = task.eligible_at
    if eligible_at is not None and eligible_at.tzinfo is None:
        eligible_at = eligible_at.replace(tzinfo=timezone.utc)
    return {
        "id": task.id,
        "patient_id": task.patient_id,
        "status": task.status.value,
        "settings": task.settings,
        "safety_bounds": task.safety_bounds,
        "rounds": task.rounds,
        "current_round": task.current_round,
        "observation_seconds": task.observation_seconds,
        "eligible_at": eligible_at,
        "questionnaire_unlocked": eligible_at is not None and now >= eligible_at,
        "current_parameters": task.current_parameters,
        "best_parameters": task.best_parameters,
        "excluded_currents": task.excluded_currents,
        "completed_at": task.completed_at,
        "created_at": task.created_at,
        "feedback": [
            {
                "id": row.id,
                "round_index": row.round_index,
                "answers": row.answers,
                "side_effects": row.side_effects,
                "score": row.score,
                "blocked": row.blocked,
                "parameters": row.parameters,
                "created_at": row.created_at,
            }
            for row in feedback_rows
        ],
        "proposals": [_proposal_view(row) for row in proposals],
        "chart": {
            **recommendation.get("curve", {}),
            "observations": [
                {
                    "round_index": row.round_index,
                    "current_ma": float(row.parameters.get("current_ma", 0)),
                    "score": row.score,
                }
                for row in feedback_rows
            ],
            "next_current_ma": recommendation.get("current_ma"),
        },
    }


def _create_next_proposal(
    db: Session,
    task: OptimizationTask,
    feedback_score: float,
    feedback_blocked: bool = False,
) -> tuple[ParameterProposal, dict[str, Any]]:
    _, history = _optimization_history(db, task)
    recommendation = recommend_next_parameter(
        history,
        task.safety_bounds,
        task.excluded_currents or [],
    )
    candidate = {
        **task.current_parameters,
        "current_ma": recommendation["current_ma"],
    }
    safety = check_safety(candidate, task.current_parameters, task.safety_bounds)
    blocked = feedback_blocked or not safety["allowed"]
    acquisition = {
        key: value
        for key, value in recommendation.items()
        if key not in {"curve", "current_ma", "expected_score"}
    }
    proposal = ParameterProposal(
        task_id=task.id,
        patient_id=task.patient_id,
        round_index=task.current_round + 1,
        status=WorkflowStatus.REJECTED if blocked else WorkflowStatus.SUBMITTED,
        parameters=candidate,
        score=float(recommendation.get("expected_score") or feedback_score),
        safety_result={**safety, "feedback_blocked": feedback_blocked},
        acquisition=acquisition,
    )
    db.add(proposal)
    db.flush()
    return proposal, recommendation


@app.post("/optimization-tasks", status_code=201)
def create_optimization_task(
    payload: OptimizationTaskCreate,
    doctor: User = Depends(require_roles(UserRole.DOCTOR, UserRole.ADMIN)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    ensure_patient_access(db, doctor, payload.patient_id)
    safety = check_safety(payload.current_parameters, None, payload.safety_bounds)
    if not safety["allowed"]:
        raise HTTPException(status_code=422, detail=safety)
    now = utcnow()
    values = payload.model_dump()
    row = OptimizationTask(
        created_by=doctor.id,
        status=WorkflowStatus.ACKNOWLEDGED,
        current_round=1,
        eligible_at=now + timedelta(seconds=payload.observation_seconds),
        best_parameters=payload.current_parameters,
        **values,
    )
    db.add(row)
    audit(db, doctor, "optimization.create", "optimization_task", row.id)
    db.commit()
    return _optimization_view(db, row)


@app.get("/optimization-tasks")
def list_optimization_tasks(
    patient_id: str | None = None,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    resolved = _owned_patient_id(db, user, patient_id)
    rows = (
        db.query(OptimizationTask)
        .filter(OptimizationTask.patient_id == resolved)
        .order_by(OptimizationTask.created_at.desc())
        .all()
    )
    return [_optimization_view(db, row) for row in rows]


@app.get("/optimization-tasks/{task_id}")
def get_optimization_task(
    task_id: str,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    task = db.get(OptimizationTask, task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="optimization task not found")
    ensure_patient_access(db, user, task.patient_id)
    return _optimization_view(db, task)


@app.post("/optimization-tasks/{task_id}/feedback", status_code=201)
def submit_feedback(
    task_id: str,
    payload: FeedbackCreate,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    task = db.get(OptimizationTask, task_id)
    if task is None or payload.task_id != task_id:
        raise HTTPException(status_code=404, detail="optimization task not found")
    ensure_patient_access(db, user, task.patient_id)
    if user.role != UserRole.PATIENT:
        raise HTTPException(status_code=403, detail="questionnaire must be submitted by the patient")
    existing = db.query(FeedbackResponse).filter(FeedbackResponse.event_id == payload.event_id).one_or_none()
    if existing:
        return {
            "feedback_id": existing.id,
            "task": _optimization_view(db, task),
            "deduplicated": True,
        }
    eligible_at = task.eligible_at
    if eligible_at is None:
        raise HTTPException(status_code=409, detail="task is not observing an acknowledged parameter")
    if eligible_at.tzinfo is None:
        eligible_at = eligible_at.replace(tzinfo=timezone.utc)
    if utcnow() < eligible_at:
        raise HTTPException(
            status_code=409,
            detail={"code": "observation_incomplete", "eligible_at": eligible_at.isoformat()},
        )
    if task.status not in {WorkflowStatus.ACKNOWLEDGED, WorkflowStatus.DRAFT}:
        raise HTTPException(status_code=409, detail="task is not accepting feedback")
    if any(
        abs(float(payload.parameters.get(key, -9999)) - float(task.current_parameters.get(key, -9998))) > 1e-6
        for key in ("current_ma", "pulse_width_us", "frequency_hz", "duty_cycle")
    ):
        raise HTTPException(status_code=409, detail="feedback parameters do not match acknowledged parameters")
    score = compute_feedback_score(payload.answers, payload.side_effects)
    feedback = FeedbackResponse(
        event_id=payload.event_id,
        task_id=task.id,
        answers=payload.answers,
        side_effects=payload.side_effects,
        score=score["final_score"],
        blocked=score["blocked"],
        parameters=payload.parameters,
        round_index=task.current_round,
        submitted_by=user.id,
    )
    db.add(feedback)
    db.flush()
    if not score["blocked"] and (
        not task.best_parameters or score["final_score"] >= max(
            [row.score for row in _optimization_history(db, task)[0]],
            default=score["final_score"],
        )
    ):
        task.best_parameters = dict(payload.parameters)
    proposal = None
    recommendation = None
    if task.current_round >= task.rounds or score["blocked"]:
        task.status = WorkflowStatus.APPROVED if not score["blocked"] else WorkflowStatus.REJECTED
        task.completed_at = utcnow()
        task.eligible_at = None
    else:
        proposal, recommendation = _create_next_proposal(
            db,
            task,
            score["final_score"],
            score["blocked"],
        )
        task.status = proposal.status
    audit(
        db,
        user,
        "optimization.feedback",
        "optimization_task",
        task.id,
        after={"blocked": score["blocked"], "proposal_id": proposal.id if proposal else None},
    )
    db.commit()
    return {
        "feedback_id": feedback.id,
        "proposal_id": proposal.id if proposal else None,
        "score": score,
        "recommendation": recommendation,
        "status": task.status.value,
        "task": _optimization_view(db, task),
        "deduplicated": False,
    }


@app.get("/parameter-proposals")
def list_proposals(
    patient_id: str | None = None,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    resolved = _owned_patient_id(db, user, patient_id)
    rows = (
        db.query(ParameterProposal)
        .filter(ParameterProposal.patient_id == resolved)
        .order_by(ParameterProposal.created_at.desc())
        .all()
    )
    return [_proposal_view(row) for row in rows]


@app.post("/approvals/{proposal_id}")
async def review_proposal(
    proposal_id: str,
    payload: ProposalReview,
    doctor: User = Depends(require_roles(UserRole.DOCTOR, UserRole.ADMIN)),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    proposal = db.get(ParameterProposal, proposal_id)
    if proposal is None:
        raise HTTPException(status_code=404, detail="proposal not found")
    ensure_patient_access(db, doctor, proposal.patient_id)
    if proposal.status != WorkflowStatus.SUBMITTED:
        raise HTTPException(status_code=409, detail="proposal is not awaiting review")
    proposal.reviewed_by = doctor.id
    proposal.reviewed_at = utcnow()
    proposal.review_note = payload.note
    task = db.get(OptimizationTask, proposal.task_id)
    if task is None:
        raise HTTPException(status_code=409, detail="optimization task is missing")
    if payload.action == "reject":
        proposal.status = WorkflowStatus.REJECTED
        task.excluded_currents = [
            *(task.excluded_currents or []),
            float(proposal.parameters["current_ma"]),
        ]
        feedback_rows, _ = _optimization_history(db, task)
        replacement, _ = _create_next_proposal(
            db,
            task,
            feedback_rows[-1].score if feedback_rows else proposal.score,
        )
        task.status = replacement.status
        audit(db, doctor, "proposal.reject", "parameter_proposal", proposal.id)
        db.commit()
        return {
            "proposal_id": proposal.id,
            "status": proposal.status.value,
            "replacement_proposal_id": replacement.id,
        }
    if not proposal.safety_result.get("allowed") or proposal.safety_result.get("feedback_blocked"):
        raise HTTPException(status_code=422, detail="unsafe or side-effect-blocked proposal cannot be approved")
    binding = (
        db.query(DeviceBinding)
        .join(Device, Device.id == DeviceBinding.device_id)
        .filter(
            DeviceBinding.patient_id == proposal.patient_id,
            DeviceBinding.active.is_(True),
            Device.simulated.is_(True),
        )
        .first()
    )
    if binding is None:
        raise HTTPException(status_code=409, detail="no active simulated device binding")
    sequence = db.query(DeviceCommand).filter(DeviceCommand.device_id == binding.device_id).count() + 1
    command = DeviceCommand(
        proposal_id=proposal.id,
        device_id=binding.device_id,
        sequence=sequence,
        status=WorkflowStatus.DISPATCHED,
        payload=proposal.parameters,
    )
    proposal.status = WorkflowStatus.DISPATCHED
    task.status = WorkflowStatus.DISPATCHED
    task.eligible_at = None
    db.add(command)
    audit(
        db,
        doctor,
        "proposal.approve_and_dispatch",
        "device_command",
        command.id,
        after={"sequence": sequence, "simulated": True},
    )
    db.commit()
    await hub.broadcast(
        f"device:{proposal.patient_id}",
        {
            "type": "parameter_command",
            "command_id": command.id,
            "sequence": sequence,
            "parameters": proposal.parameters,
            "simulated_only": True,
        },
    )
    return {
        "proposal_id": proposal.id,
        "status": proposal.status.value,
        "command_id": command.id,
        "sequence": sequence,
    }


@app.get("/devices/commands/pending")
def pending_commands(
    patient_id: str | None = None,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    resolved = _owned_patient_id(db, user, patient_id)
    bindings = db.query(DeviceBinding).filter(
        DeviceBinding.patient_id == resolved,
        DeviceBinding.active.is_(True),
    ).all()
    device_ids = [binding.device_id for binding in bindings]
    rows = (
        db.query(DeviceCommand)
        .filter(
            DeviceCommand.device_id.in_(device_ids),
            DeviceCommand.status == WorkflowStatus.DISPATCHED,
        )
        .order_by(DeviceCommand.dispatched_at)
        .all()
        if device_ids
        else []
    )
    return [
        {
            "id": row.id,
            "device_id": row.device_id,
            "sequence": row.sequence,
            "payload": row.payload,
            "status": row.status.value,
            "dispatched_at": row.dispatched_at,
        }
        for row in rows
    ]


@app.post("/devices/commands/{command_id}/ack")
async def acknowledge_command(
    command_id: str,
    payload: DeviceAck,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    command = db.get(DeviceCommand, command_id)
    if command is None or payload.command_id != command_id or payload.sequence != command.sequence:
        raise HTTPException(status_code=404, detail="device command not found")
    proposal = db.get(ParameterProposal, command.proposal_id)
    ensure_patient_access(db, user, proposal.patient_id)
    device = db.get(Device, command.device_id)
    if device is None or not device.simulated:
        raise HTTPException(status_code=403, detail="real device acknowledgements are disabled")
    command.status = WorkflowStatus.ACKNOWLEDGED if payload.success else WorkflowStatus.FAILED
    command.ack_payload = payload.model_dump()
    command.acknowledged_at = utcnow()
    proposal.status = command.status
    task = db.get(OptimizationTask, proposal.task_id)
    if task is not None:
        if payload.success:
            task.status = WorkflowStatus.ACKNOWLEDGED
            task.current_round = proposal.round_index
            task.current_parameters = dict(command.payload)
            task.eligible_at = utcnow() + timedelta(seconds=task.observation_seconds)
        else:
            task.status = WorkflowStatus.FAILED
            task.eligible_at = None
    audit(
        db,
        user,
        "device.command_ack",
        "device_command",
        command.id,
        after={"success": payload.success, "status_code": payload.status_code},
    )
    db.commit()
    await hub.broadcast(
        f"monitor:{proposal.patient_id}",
        {
            "type": "command_ack",
            "command_id": command.id,
            "sequence": command.sequence,
            "success": payload.success,
            "status_code": payload.status_code,
        },
    )
    return {"command_id": command.id, "status": command.status.value}


@app.post("/chat-sessions", status_code=201)
def create_chat_session(
    payload: ChatSessionCreate,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    ensure_patient_access(db, user, payload.patient_id)
    doctor = db.get(User, payload.doctor_user_id)
    if doctor is None or doctor.role != UserRole.DOCTOR:
        raise HTTPException(status_code=422, detail="invalid doctor")
    relation = (
        db.query(CareRelation)
        .filter(
            CareRelation.doctor_user_id == doctor.id,
            CareRelation.patient_id == payload.patient_id,
        )
        .one_or_none()
    )
    if relation is None:
        raise HTTPException(status_code=403, detail="doctor is not assigned to patient")
    existing = (
        db.query(ChatSession)
        .filter(
            ChatSession.patient_id == payload.patient_id,
            ChatSession.doctor_user_id == doctor.id,
            ChatSession.active.is_(True),
        )
        .one_or_none()
    )
    row = existing or ChatSession(patient_id=payload.patient_id, doctor_user_id=doctor.id)
    if existing is None:
        db.add(row)
        audit(db, user, "chat.create_session", "chat_session", row.id)
        db.commit()
    return {"id": row.id, "patient_id": row.patient_id, "doctor_user_id": row.doctor_user_id}


@app.get("/chat-sessions/{session_id}/messages")
def chat_messages(
    session_id: str,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    session = db.get(ChatSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="chat session not found")
    ensure_patient_access(db, user, session.patient_id)
    rows = (
        db.query(ChatMessage)
        .filter(ChatMessage.session_id == session.id)
        .order_by(ChatMessage.created_at)
        .all()
    )
    return [
        {
            "id": row.id,
            "event_id": row.event_id,
            "sender_user_id": row.sender_user_id,
            "content": row.content,
            "created_at": row.created_at,
        }
        for row in rows
    ]


@app.post("/chat-sessions/{session_id}/messages", status_code=201)
async def post_chat_message(
    session_id: str,
    payload: ChatMessageCreate,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    session = db.get(ChatSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="chat session not found")
    ensure_patient_access(db, user, session.patient_id)
    existing = db.query(ChatMessage).filter(ChatMessage.event_id == payload.event_id).one_or_none()
    if existing:
        return {"id": existing.id, "deduplicated": True}
    row = ChatMessage(
        event_id=payload.event_id,
        session_id=session.id,
        sender_user_id=user.id,
        content=payload.content,
    )
    db.add(row)
    db.commit()
    message = {
        "id": row.id,
        "event_id": row.event_id,
        "sender_user_id": row.sender_user_id,
        "content": row.content,
        "created_at": row.created_at.isoformat(),
    }
    await hub.broadcast(f"chat:{session.id}", {"type": "chat_message", **message})
    return {**message, "deduplicated": False}


@app.post("/exports", status_code=201)
def create_export(
    payload: ExportCreate,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    patient = ensure_patient_access(db, user, payload.patient_id)
    job = ExportJob(
        patient_id=patient.id,
        requested_by=user.id,
        format=payload.format,
        status="running",
    )
    db.add(job)
    db.flush()
    try:
        content, filename, content_type = build_export(db, patient, payload.format)
        key = f"exports/{patient.code}/{job.id}/{filename}"
        storage.put_bytes(key, content, content_type)
        job.object_key = key
        job.status = "completed"
        job.completed_at = utcnow()
    except ValueError as exc:
        job.status = "failed"
        job.error = str(exc)
    audit(db, user, "export.create", "export_job", job.id, after={"format": payload.format, "status": job.status})
    db.commit()
    return {
        "id": job.id,
        "status": job.status,
        "format": job.format,
        "object_key": job.object_key,
        "error": job.error,
    }


@app.get("/exports")
def list_exports(
    patient_id: str | None = None,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[dict[str, Any]]:
    resolved = _owned_patient_id(db, user, patient_id)
    rows = (
        db.query(ExportJob)
        .filter(ExportJob.patient_id == resolved)
        .order_by(ExportJob.created_at.desc())
        .all()
    )
    return [
        {
            "id": row.id,
            "format": row.format,
            "status": row.status,
            "object_key": row.object_key,
            "error": row.error,
            "created_at": row.created_at,
            "completed_at": row.completed_at,
        }
        for row in rows
    ]


@app.get("/exports/{job_id}/download")
def download_export(
    job_id: str,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> Response:
    job = db.get(ExportJob, job_id)
    if job is None:
        raise HTTPException(status_code=404, detail="export job not found")
    ensure_patient_access(db, user, job.patient_id)
    if job.status != "completed" or not job.object_key:
        raise HTTPException(status_code=409, detail=job.error or "export is not complete")
    content = storage.get_bytes(job.object_key)
    filename = job.object_key.rsplit("/", 1)[-1]
    return Response(
        content=content,
        media_type={
            "pdf": "application/pdf",
            "csv": "text/csv",
            "mat": "application/x-matlab-data",
            "edf": "application/edf",
            "eml": "message/rfc822",
            "zip": "application/zip",
        }[job.format],
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )


@app.websocket("/ws/patients/{patient_id}/monitor")
async def monitor_socket(websocket: WebSocket, patient_id: str) -> None:
    with SessionLocal() as db:
        try:
            user = await _websocket_user(websocket, db)
            ensure_patient_access(db, user, patient_id)
            channel = f"monitor:{patient_id}"
            await hub.connect(channel, websocket)
            while True:
                message = await websocket.receive_json()
                if message.get("type") == "ping":
                    await websocket.send_json({"type": "pong"})
        except WebSocketDisconnect:
            pass
        finally:
            hub.disconnect(f"monitor:{patient_id}", websocket)


@app.websocket("/ws/device-stream")
async def device_stream_socket(websocket: WebSocket, patient_id: str = Query(...)) -> None:
    with SessionLocal() as db:
        try:
            user = await _websocket_user(websocket, db)
            ensure_patient_access(db, user, patient_id)
            channel = f"device:{patient_id}"
            await hub.connect(channel, websocket)
            while True:
                message = await websocket.receive_json()
                if message.get("type") in {"telemetry", "lfp_summary", "connection"}:
                    await hub.broadcast(f"monitor:{patient_id}", message)
                elif message.get("type") == "ping":
                    await websocket.send_json({"type": "pong"})
        except WebSocketDisconnect:
            pass
        finally:
            hub.disconnect(f"device:{patient_id}", websocket)


@app.websocket("/ws/chat/{session_id}")
async def chat_socket(websocket: WebSocket, session_id: str) -> None:
    with SessionLocal() as db:
        try:
            user = await _websocket_user(websocket, db)
            session = db.get(ChatSession, session_id)
            if session is None:
                await websocket.close(code=4404, reason="session not found")
                return
            ensure_patient_access(db, user, session.patient_id)
            channel = f"chat:{session_id}"
            await hub.connect(channel, websocket)
            while True:
                message = await websocket.receive_json()
                if message.get("type") == "ping":
                    await websocket.send_json({"type": "pong"})
        except WebSocketDisconnect:
            pass
        finally:
            hub.disconnect(f"chat:{session_id}", websocket)
