from __future__ import annotations

from celery import Celery

from .config import get_settings
from .services.initialization import analyze_initialization


settings = get_settings()
celery_app = Celery(
    "omnidapt",
    broker=settings.redis_url or "memory://",
    backend=settings.redis_url or "cache+memory://",
)
celery_app.conf.update(task_serializer="json", result_serializer="json", accept_content=["json"])


@celery_app.task(name="omnidapt.analyze_initialization")
def analyze_initialization_task(run_id: str) -> dict:
    result = analyze_initialization(run_id)
    return {
        "initialization_id": run_id,
        "status": result["status"],
        "model_version_id": result["model_version_id"],
    }
