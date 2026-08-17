from __future__ import annotations

from sqlalchemy.orm import Session

from ..models import AuditLog, User


def audit(
    db: Session,
    actor: User | None,
    action: str,
    target_type: str,
    target_id: str | None,
    before: dict | None = None,
    after: dict | None = None,
) -> AuditLog:
    row = AuditLog(
        actor_user_id=actor.id if actor else None,
        action=action,
        target_type=target_type,
        target_id=target_id,
        before=before,
        after=after,
    )
    db.add(row)
    return row
