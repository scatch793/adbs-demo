from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator

from .models import UserRole, WorkflowStatus


class OrmModel(BaseModel):
    model_config = ConfigDict(from_attributes=True)


class LoginRequest(BaseModel):
    username: str = Field(min_length=2, max_length=80)
    password: str = Field(min_length=8, max_length=128)


class RefreshRequest(BaseModel):
    refresh_token: str


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str = Field(min_length=12, max_length=128)


class UserView(OrmModel):
    id: str
    username: str
    role: UserRole
    display_name: str
    active: bool
    must_change_password: bool


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in_seconds: int
    user: UserView


class AdminUserCreate(BaseModel):
    username: str = Field(min_length=2, max_length=80)
    temporary_password: str = Field(min_length=12, max_length=128)
    role: UserRole
    display_name: str = Field(min_length=1, max_length=120)
    patient_code: str | None = Field(default=None, max_length=40)


class PasswordResetRequest(BaseModel):
    temporary_password: str = Field(min_length=12, max_length=128)


class PatientCreate(BaseModel):
    code: str = Field(min_length=2, max_length=40)
    name: str = Field(min_length=1, max_length=120)
    gender: str = "未填写"
    age: int | None = Field(default=None, ge=0, le=120)
    implant_date: str | None = None
    summary: str = ""
    emergency_contact: str | None = None
    emergency_phone: str | None = None
    user_id: str | None = None


class PatientUpdate(BaseModel):
    name: str | None = None
    gender: str | None = None
    age: int | None = Field(default=None, ge=0, le=120)
    implant_date: str | None = None
    summary: str | None = None
    emergency_contact: str | None = None
    emergency_phone: str | None = None


class PatientView(OrmModel):
    id: str
    user_id: str | None
    code: str
    name: str
    gender: str
    age: int | None
    implant_date: str | None
    summary: str
    emergency_contact: str | None
    emergency_phone: str | None


class CareBindingRequest(BaseModel):
    doctor_user_id: str
    patient_id: str


class DeviceCreate(BaseModel):
    serial_number: str = Field(min_length=3, max_length=80)
    name: str = "Ominidapt BLE Simulator"
    simulated: Literal[True] = True


class DeviceBindingRequest(BaseModel):
    patient_id: str
    device_id: str


class SymptomCreate(BaseModel):
    event_id: str = Field(min_length=8, max_length=64)
    patient_id: str | None = None
    tremor: int = Field(ge=0, le=3)
    rigidity: int = Field(ge=0, le=3)
    speech: int = Field(ge=0, le=3)
    note: str = Field(default="", max_length=2000)
    recorded_at: datetime | None = None


class MedicationCreate(BaseModel):
    event_id: str = Field(min_length=8, max_length=64)
    patient_id: str | None = None
    medication_name: str = "左旋多巴"
    status: Literal["taken", "snoozed", "missed"] = "taken"
    scheduled_at: datetime | None = None
    recorded_at: datetime | None = None


class LfpSessionCreate(BaseModel):
    patient_id: str
    device_id: str
    purpose: Literal["monitor", "baseline", "manual_recording", "alert_window"] = "monitor"
    state_label: str | None = None
    sample_rate_hz: int = Field(default=256, ge=128, le=2048)
    channels: int = Field(default=2, ge=1, le=8)
    recording_enabled: bool = False


class LfpSessionComplete(BaseModel):
    sample_count: int = Field(ge=0)
    packet_loss_count: int = Field(ge=0)


class InitializationCreate(BaseModel):
    patient_id: str
    device_id: str
    mode: Literal["demo", "research"] = "demo"
    electrode_config: dict[str, Any] = Field(default_factory=dict)


class InitializationSegmentCreate(BaseModel):
    lfp_session_id: str
    state_label: Literal["OFF-Rest", "OFF-Move", "ON-Rest", "ON-Move"]
    received_frames: int = Field(ge=0)
    packet_loss_count: int = Field(ge=0)
    crc_error_count: int = Field(ge=0)
    saturated_sample_count: int = Field(ge=0)
    impedance: dict[str, float] = Field(default_factory=dict)


class InitializationTransition(BaseModel):
    action: Literal["configure", "capture", "cancel"]


class InitializationApprove(BaseModel):
    note: str = Field(default="", max_length=2000)


class InferenceCreate(BaseModel):
    event_id: str = Field(min_length=8, max_length=64)
    patient_id: str
    model_version_id: str | None = None
    features: list[float] = Field(min_length=5, max_length=5)
    probabilities: dict[str, float]
    top_state: str
    confidence: float = Field(ge=0, le=1)
    rejected: bool = False
    recorded_at: datetime | None = None

    @field_validator("probabilities")
    @classmethod
    def validate_probabilities(cls, value: dict[str, float]) -> dict[str, float]:
        if any(probability < 0 or probability > 1 for probability in value.values()):
            raise ValueError("probabilities must be between 0 and 1")
        if value and abs(sum(value.values()) - 1.0) > 1e-3:
            raise ValueError("probabilities must sum to one")
        return value


class OptimizationTaskCreate(BaseModel):
    patient_id: str
    settings: dict[str, Any] = Field(default_factory=dict)
    safety_bounds: dict[str, float] = Field(
        default_factory=lambda: {
            "current_min_ma": 1.0,
            "current_max_ma": 3.0,
            "pulse_width_min_us": 50.0,
            "pulse_width_max_us": 90.0,
            "frequency_min_hz": 120.0,
            "frequency_max_hz": 150.0,
            "max_delta_current_ma": 0.2,
        }
    )
    rounds: int = Field(default=7, ge=3, le=12)
    observation_seconds: int = Field(default=30, ge=5, le=600)
    current_parameters: dict[str, float] = Field(
        default_factory=lambda: {
            "current_ma": 1.5,
            "pulse_width_us": 60.0,
            "frequency_hz": 130.0,
            "duty_cycle": 50.0,
            "left_contact": 6.0,
            "right_contact": 2.0,
        }
    )


class FeedbackCreate(BaseModel):
    event_id: str = Field(min_length=8, max_length=64)
    task_id: str
    answers: dict[str, float]
    side_effects: dict[str, float] = Field(default_factory=dict)
    parameters: dict[str, float]


class ProposalReview(BaseModel):
    action: Literal["approve", "reject"]
    note: str = Field(default="", max_length=2000)


class DeviceAck(BaseModel):
    command_id: str
    sequence: int
    success: bool
    status_code: str = "ok"
    detail: str = ""


class ChatSessionCreate(BaseModel):
    patient_id: str
    doctor_user_id: str


class ChatMessageCreate(BaseModel):
    event_id: str = Field(min_length=8, max_length=64)
    content: str = Field(min_length=1, max_length=4000)


class ExportCreate(BaseModel):
    patient_id: str
    format: Literal["pdf", "csv", "mat", "edf", "eml", "zip"]


class StateInferRequest(BaseModel):
    features: list[float] = Field(min_length=5, max_length=5)
    model: dict[str, Any] | None = None


class ModelImportRequest(BaseModel):
    patient_id: str
    payload: dict[str, Any]
    metrics: dict[str, Any] = Field(default_factory=dict)
    approve: bool = True


class FeedbackScoreRequest(BaseModel):
    answers: dict[str, float]
    side_effects: dict[str, float] = Field(default_factory=dict)


class SafetyCheckRequest(BaseModel):
    candidate: dict[str, float]
    previous: dict[str, float] | None = None
    bounds: dict[str, float]
