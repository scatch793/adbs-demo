from __future__ import annotations

from collections.abc import Callable

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from .database import get_db
from .models import CareRelation, Patient, User, UserRole
from .security import decode_token


bearer = HTTPBearer(auto_error=False)


def authenticated_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="missing bearer token")
    try:
        payload = decode_token(credentials.credentials, "access")
    except jwt.PyJWTError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid access token") from exc
    user = db.get(User, payload.get("sub"))
    if user is None or not user.active:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="inactive user")
    return user


def current_user(user: User = Depends(authenticated_user)) -> User:
    if user.must_change_password:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="password change required",
        )
    return user


def require_roles(*roles: UserRole) -> Callable:
    def dependency(user: User = Depends(current_user)) -> User:
        if user.role not in roles:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="insufficient role")
        return user

    return dependency


def patient_for_user(db: Session, user: User) -> Patient:
    patient = db.query(Patient).filter(Patient.user_id == user.id).one_or_none()
    if patient is None:
        raise HTTPException(status_code=404, detail="patient profile not found")
    return patient


def ensure_patient_access(db: Session, user: User, patient_id: str) -> Patient:
    patient = db.get(Patient, patient_id)
    if patient is None:
        raise HTTPException(status_code=404, detail="patient not found")
    if user.role == UserRole.ADMIN:
        return patient
    if user.role == UserRole.PATIENT and patient.user_id == user.id:
        return patient
    if user.role == UserRole.DOCTOR:
        relation = (
            db.query(CareRelation)
            .filter(
                CareRelation.doctor_user_id == user.id,
                CareRelation.patient_id == patient_id,
            )
            .one_or_none()
        )
        if relation is not None:
            return patient
    raise HTTPException(status_code=403, detail="patient access denied")
