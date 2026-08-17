from __future__ import annotations

from sqlalchemy.orm import Session

from ..config import get_settings
from ..models import CareRelation, Device, DeviceBinding, Patient, User, UserRole
from ..security import hash_password


def _ensure_user(
    db: Session,
    username: str,
    password: str,
    role: UserRole,
    display_name: str,
) -> User:
    user = db.query(User).filter(User.username == username).one_or_none()
    if user is None:
        user = User(
            username=username,
            password_hash=hash_password(password),
            role=role,
            display_name=display_name,
            active=True,
            must_change_password=True,
        )
        db.add(user)
        db.flush()
    return user


def bootstrap_demo_data(db: Session) -> None:
    settings = get_settings()
    _ensure_user(
        db,
        settings.bootstrap_admin_username,
        settings.bootstrap_admin_password,
        UserRole.ADMIN,
        "系统管理员",
    )
    doctor = _ensure_user(
        db,
        settings.bootstrap_doctor_username,
        settings.bootstrap_doctor_password,
        UserRole.DOCTOR,
        "演示医生",
    )
    patient_user = _ensure_user(
        db,
        settings.bootstrap_patient_username,
        settings.bootstrap_patient_password,
        UserRole.PATIENT,
        "P001",
    )
    patient = db.query(Patient).filter(Patient.code == "P001").one_or_none()
    if patient is None:
        patient = Patient(
            user_id=patient_user.id,
            code="P001",
            name="P001",
            gender="脱敏",
            age=None,
            summary="脱敏科研演示患者",
            emergency_contact="演示联系人",
            # Deliberately empty: the research seed must never dial a real person.
            emergency_phone=None,
        )
        db.add(patient)
        db.flush()
    if (
        db.query(CareRelation)
        .filter(
            CareRelation.doctor_user_id == doctor.id,
            CareRelation.patient_id == patient.id,
        )
        .one_or_none()
        is None
    ):
        db.add(CareRelation(doctor_user_id=doctor.id, patient_id=patient.id))
    device = db.query(Device).filter(Device.serial_number == "SIM-P001-001").one_or_none()
    if device is None:
        device = Device(serial_number="SIM-P001-001", simulated=True, protocol_version=2)
        db.add(device)
        db.flush()
    else:
        device.protocol_version = 2
    if (
        db.query(DeviceBinding)
        .filter(DeviceBinding.patient_id == patient.id, DeviceBinding.device_id == device.id)
        .one_or_none()
        is None
    ):
        db.add(DeviceBinding(patient_id=patient.id, device_id=device.id, active=True))
    db.commit()
