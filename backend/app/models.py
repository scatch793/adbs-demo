from __future__ import annotations

import enum
import uuid
from datetime import datetime, timezone

from sqlalchemy import (
    Boolean,
    DateTime,
    Enum,
    Float,
    ForeignKey,
    Integer,
    JSON,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .database import Base


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


def new_id() -> str:
    return str(uuid.uuid4())


class UserRole(str, enum.Enum):
    ADMIN = "admin"
    DOCTOR = "doctor"
    PATIENT = "patient"


class WorkflowStatus(str, enum.Enum):
    DRAFT = "draft"
    SUBMITTED = "submitted"
    APPROVED = "approved"
    REJECTED = "rejected"
    DISPATCHED = "dispatched"
    ACKNOWLEDGED = "acknowledged"
    FAILED = "failed"


class InitializationStatus(str, enum.Enum):
    DRAFT = "draft"
    CONFIGURING = "configuring"
    CAPTURING = "capturing"
    ANALYZING = "analyzing"
    REVIEW = "review"
    APPROVED = "approved"
    FAILED = "failed"
    CANCELLED = "cancelled"


class User(Base):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    username: Mapped[str] = mapped_column(String(80), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    role: Mapped[UserRole] = mapped_column(Enum(UserRole), index=True)
    display_name: Mapped[str] = mapped_column(String(120))
    active: Mapped[bool] = mapped_column(Boolean, default=True)
    must_change_password: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    token_hash: Mapped[str] = mapped_column(String(64), unique=True)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class Patient(Base):
    __tablename__ = "patients"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    user_id: Mapped[str | None] = mapped_column(ForeignKey("users.id"), unique=True)
    code: Mapped[str] = mapped_column(String(40), unique=True, index=True)
    name: Mapped[str] = mapped_column(String(120))
    gender: Mapped[str] = mapped_column(String(20), default="未填写")
    age: Mapped[int | None] = mapped_column(Integer)
    implant_date: Mapped[str | None] = mapped_column(String(20))
    summary: Mapped[str] = mapped_column(Text, default="")
    emergency_contact: Mapped[str | None] = mapped_column(String(80))
    emergency_phone: Mapped[str | None] = mapped_column(String(40))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class CareRelation(Base):
    __tablename__ = "care_relations"
    __table_args__ = (UniqueConstraint("doctor_user_id", "patient_id"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    doctor_user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class Device(Base):
    __tablename__ = "devices"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    serial_number: Mapped[str] = mapped_column(String(80), unique=True, index=True)
    name: Mapped[str] = mapped_column(String(120), default="Ominidapt BLE Simulator")
    simulated: Mapped[bool] = mapped_column(Boolean, default=True)
    protocol_version: Mapped[int] = mapped_column(Integer, default=2)
    battery_percent: Mapped[int] = mapped_column(Integer, default=100)
    last_seen_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class DeviceBinding(Base):
    __tablename__ = "device_bindings"
    __table_args__ = (UniqueConstraint("patient_id", "device_id"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    device_id: Mapped[str] = mapped_column(ForeignKey("devices.id"), index=True)
    active: Mapped[bool] = mapped_column(Boolean, default=True)
    bound_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class SymptomEntry(Base):
    __tablename__ = "symptom_entries"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    event_id: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    tremor: Mapped[int] = mapped_column(Integer)
    rigidity: Mapped[int] = mapped_column(Integer)
    speech: Mapped[int] = mapped_column(Integer)
    note: Mapped[str] = mapped_column(Text, default="")
    recorded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class MedicationEvent(Base):
    __tablename__ = "medication_events"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    event_id: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    medication_name: Mapped[str] = mapped_column(String(120), default="左旋多巴")
    status: Mapped[str] = mapped_column(String(30), default="taken")
    scheduled_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    recorded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class LfpSession(Base):
    __tablename__ = "lfp_sessions"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    device_id: Mapped[str] = mapped_column(ForeignKey("devices.id"), index=True)
    purpose: Mapped[str] = mapped_column(String(40), default="monitor")
    state_label: Mapped[str | None] = mapped_column(String(40))
    sample_rate_hz: Mapped[int] = mapped_column(Integer, default=256)
    channels: Mapped[int] = mapped_column(Integer, default=2)
    recording_enabled: Mapped[bool] = mapped_column(Boolean, default=False)
    object_key: Mapped[str | None] = mapped_column(String(255))
    sample_count: Mapped[int] = mapped_column(Integer, default=0)
    packet_loss_count: Mapped[int] = mapped_column(Integer, default=0)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    ended_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class InitializationRun(Base):
    __tablename__ = "initialization_runs"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    device_id: Mapped[str] = mapped_column(ForeignKey("devices.id"), index=True)
    created_by: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    mode: Mapped[str] = mapped_column(String(20), default="demo")
    status: Mapped[InitializationStatus] = mapped_column(
        Enum(InitializationStatus),
        default=InitializationStatus.DRAFT,
        index=True,
    )
    current_state: Mapped[str | None] = mapped_column(String(40))
    settle_seconds: Mapped[int] = mapped_column(Integer, default=5)
    capture_seconds: Mapped[int] = mapped_column(Integer, default=30)
    electrode_config: Mapped[dict] = mapped_column(JSON, default=dict)
    quality_summary: Mapped[dict] = mapped_column(JSON, default=dict)
    frequency_results: Mapped[dict] = mapped_column(JSON, default=dict)
    analysis_stage: Mapped[str] = mapped_column(String(40), default="idle")
    progress_percent: Mapped[int] = mapped_column(Integer, default=0)
    model_version_id: Mapped[str | None] = mapped_column(
        ForeignKey("model_versions.id"),
        index=True,
    )
    error: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=utcnow,
        onupdate=utcnow,
    )
    approved_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class InitializationSegment(Base):
    __tablename__ = "initialization_segments"
    __table_args__ = (UniqueConstraint("initialization_id", "state_label"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    initialization_id: Mapped[str] = mapped_column(
        ForeignKey("initialization_runs.id", ondelete="CASCADE"),
        index=True,
    )
    lfp_session_id: Mapped[str] = mapped_column(
        ForeignKey("lfp_sessions.id"),
        unique=True,
        index=True,
    )
    state_label: Mapped[str] = mapped_column(String(40), index=True)
    order_index: Mapped[int] = mapped_column(Integer)
    sample_count: Mapped[int] = mapped_column(Integer)
    received_frames: Mapped[int] = mapped_column(Integer, default=0)
    packet_loss_count: Mapped[int] = mapped_column(Integer, default=0)
    crc_error_count: Mapped[int] = mapped_column(Integer, default=0)
    saturated_sample_count: Mapped[int] = mapped_column(Integer, default=0)
    impedance: Mapped[dict] = mapped_column(JSON, default=dict)
    quality: Mapped[dict] = mapped_column(JSON, default=dict)
    accepted: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class ModelVersion(Base):
    __tablename__ = "model_versions"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    version: Mapped[int] = mapped_column(Integer)
    status: Mapped[WorkflowStatus] = mapped_column(Enum(WorkflowStatus), default=WorkflowStatus.DRAFT)
    algorithm: Mapped[str] = mapped_column(String(80), default="five_feature_gmm")
    payload: Mapped[dict] = mapped_column(JSON)
    metrics: Mapped[dict] = mapped_column(JSON, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    approved_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class InferenceResult(Base):
    __tablename__ = "inference_results"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    event_id: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    model_version_id: Mapped[str | None] = mapped_column(ForeignKey("model_versions.id"))
    features: Mapped[list] = mapped_column(JSON)
    probabilities: Mapped[dict] = mapped_column(JSON)
    top_state: Mapped[str] = mapped_column(String(40))
    confidence: Mapped[float] = mapped_column(Float)
    rejected: Mapped[bool] = mapped_column(Boolean, default=False)
    recorded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class OptimizationTask(Base):
    __tablename__ = "optimization_tasks"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    created_by: Mapped[str] = mapped_column(ForeignKey("users.id"))
    status: Mapped[WorkflowStatus] = mapped_column(Enum(WorkflowStatus), default=WorkflowStatus.DRAFT)
    settings: Mapped[dict] = mapped_column(JSON, default=dict)
    safety_bounds: Mapped[dict] = mapped_column(JSON, default=dict)
    rounds: Mapped[int] = mapped_column(Integer, default=7)
    current_round: Mapped[int] = mapped_column(Integer, default=1)
    observation_seconds: Mapped[int] = mapped_column(Integer, default=30)
    eligible_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    current_parameters: Mapped[dict] = mapped_column(JSON, default=dict)
    best_parameters: Mapped[dict] = mapped_column(JSON, default=dict)
    excluded_currents: Mapped[list] = mapped_column(JSON, default=list)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class FeedbackResponse(Base):
    __tablename__ = "feedback_responses"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    event_id: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    task_id: Mapped[str] = mapped_column(ForeignKey("optimization_tasks.id"), index=True)
    answers: Mapped[dict] = mapped_column(JSON)
    side_effects: Mapped[dict] = mapped_column(JSON, default=dict)
    score: Mapped[float] = mapped_column(Float)
    blocked: Mapped[bool] = mapped_column(Boolean, default=False)
    parameters: Mapped[dict] = mapped_column(JSON)
    round_index: Mapped[int] = mapped_column(Integer, default=1)
    submitted_by: Mapped[str | None] = mapped_column(ForeignKey("users.id"))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class ParameterProposal(Base):
    __tablename__ = "parameter_proposals"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    task_id: Mapped[str] = mapped_column(ForeignKey("optimization_tasks.id"), index=True)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    status: Mapped[WorkflowStatus] = mapped_column(Enum(WorkflowStatus), default=WorkflowStatus.SUBMITTED)
    parameters: Mapped[dict] = mapped_column(JSON)
    score: Mapped[float] = mapped_column(Float)
    safety_result: Mapped[dict] = mapped_column(JSON)
    round_index: Mapped[int] = mapped_column(Integer, default=1)
    acquisition: Mapped[dict] = mapped_column(JSON, default=dict)
    model_version: Mapped[str] = mapped_column(String(80), default="gp-ei-v1")
    reviewed_by: Mapped[str | None] = mapped_column(ForeignKey("users.id"))
    review_note: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class DeviceCommand(Base):
    __tablename__ = "device_commands"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    proposal_id: Mapped[str] = mapped_column(ForeignKey("parameter_proposals.id"), index=True)
    device_id: Mapped[str] = mapped_column(ForeignKey("devices.id"), index=True)
    sequence: Mapped[int] = mapped_column(Integer)
    status: Mapped[WorkflowStatus] = mapped_column(Enum(WorkflowStatus), default=WorkflowStatus.DISPATCHED)
    payload: Mapped[dict] = mapped_column(JSON)
    ack_payload: Mapped[dict | None] = mapped_column(JSON)
    dispatched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    acknowledged_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class ChatSession(Base):
    __tablename__ = "chat_sessions"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    doctor_user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class ChatMessage(Base):
    __tablename__ = "chat_messages"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    event_id: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    session_id: Mapped[str] = mapped_column(ForeignKey("chat_sessions.id"), index=True)
    sender_user_id: Mapped[str] = mapped_column(ForeignKey("users.id"))
    content: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class ExportJob(Base):
    __tablename__ = "export_jobs"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    patient_id: Mapped[str] = mapped_column(ForeignKey("patients.id"), index=True)
    requested_by: Mapped[str] = mapped_column(ForeignKey("users.id"))
    format: Mapped[str] = mapped_column(String(12))
    status: Mapped[str] = mapped_column(String(30), default="queued")
    object_key: Mapped[str | None] = mapped_column(String(255))
    error: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class AuditLog(Base):
    __tablename__ = "audit_logs"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    actor_user_id: Mapped[str | None] = mapped_column(ForeignKey("users.id"), index=True)
    action: Mapped[str] = mapped_column(String(120), index=True)
    target_type: Mapped[str] = mapped_column(String(80))
    target_id: Mapped[str | None] = mapped_column(String(80))
    before: Mapped[dict | None] = mapped_column(JSON)
    after: Mapped[dict | None] = mapped_column(JSON)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
