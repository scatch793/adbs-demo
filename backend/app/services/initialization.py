from __future__ import annotations

import io
from typing import Any

import numpy as np

from ..database import SessionLocal
from ..models import (
    AuditLog,
    InitializationRun,
    InitializationSegment,
    InitializationStatus,
    LfpSession,
    ModelVersion,
    WorkflowStatus,
    utcnow,
)
from .algorithm import STATE_LABELS, train_initialization_model
from .storage import storage


def serialize_initialization(run: InitializationRun, segments: list[InitializationSegment]) -> dict[str, Any]:
    return {
        "id": run.id,
        "patient_id": run.patient_id,
        "device_id": run.device_id,
        "created_by": run.created_by,
        "mode": run.mode,
        "status": run.status.value,
        "current_state": run.current_state,
        "settle_seconds": run.settle_seconds,
        "capture_seconds": run.capture_seconds,
        "electrode_config": run.electrode_config,
        "quality_summary": run.quality_summary,
        "frequency_results": run.frequency_results,
        "analysis_stage": run.analysis_stage,
        "progress_percent": run.progress_percent,
        "model_version_id": run.model_version_id,
        "error": run.error,
        "created_at": run.created_at,
        "updated_at": run.updated_at,
        "approved_at": run.approved_at,
        "segments": [
            {
                "id": row.id,
                "lfp_session_id": row.lfp_session_id,
                "state_label": row.state_label,
                "order_index": row.order_index,
                "sample_count": row.sample_count,
                "received_frames": row.received_frames,
                "packet_loss_count": row.packet_loss_count,
                "crc_error_count": row.crc_error_count,
                "saturated_sample_count": row.saturated_sample_count,
                "impedance": row.impedance,
                "quality": row.quality,
                "accepted": row.accepted,
            }
            for row in sorted(segments, key=lambda item: item.order_index)
        ],
    }


def calculate_segment_quality(
    run: InitializationRun,
    session: LfpSession,
    received_frames: int,
    packet_loss_count: int,
    crc_error_count: int,
    saturated_sample_count: int,
) -> tuple[dict[str, Any], bool]:
    expected_samples = run.capture_seconds * session.sample_rate_hz
    completeness = min(1.0, session.sample_count / max(1, expected_samples))
    total_frames = received_frames + packet_loss_count
    loss_rate = packet_loss_count / max(1, total_frames)
    saturation_rate = saturated_sample_count / max(1, session.sample_count * session.channels)
    reasons = []
    if session.object_key is None:
        reasons.append("waveform_missing")
    if completeness < 0.90:
        reasons.append("completeness_below_90_percent")
    if loss_rate > 0.05:
        reasons.append("packet_loss_above_5_percent")
    if crc_error_count:
        reasons.append("crc_errors_present")
    if saturation_rate > 0.01:
        reasons.append("saturation_above_1_percent")
    quality = {
        "expected_samples": expected_samples,
        "actual_samples": session.sample_count,
        "completeness": completeness,
        "loss_rate": loss_rate,
        "crc_error_count": crc_error_count,
        "saturation_rate": saturation_rate,
        "reasons": reasons,
    }
    return quality, not reasons


def analyze_initialization(run_id: str) -> dict[str, Any]:
    with SessionLocal() as db:
        run = db.get(InitializationRun, run_id)
        if run is None:
            raise ValueError("initialization not found")
        try:
            def report(stage: str, percent: int) -> None:
                run.analysis_stage = stage
                run.progress_percent = percent
                run.updated_at = utcnow()
                db.commit()

            report("data_validation", 8)
            segments = (
                db.query(InitializationSegment)
                .filter(InitializationSegment.initialization_id == run.id)
                .all()
            )
            by_state = {segment.state_label: segment for segment in segments}
            missing = sorted(set(STATE_LABELS) - set(by_state))
            rejected = sorted(
                segment.state_label for segment in segments if not segment.accepted
            )
            if missing or rejected:
                raise ValueError(f"baseline incomplete: missing={missing}, rejected={rejected}")
            samples_by_state = {}
            report("loading", 15)
            for label in STATE_LABELS:
                session = db.get(LfpSession, by_state[label].lfp_session_id)
                if session is None or session.object_key is None:
                    raise ValueError(f"{label} waveform is missing")
                with np.load(io.BytesIO(storage.get_bytes(session.object_key)), allow_pickle=False) as payload:
                    samples = np.asarray(payload["samples"], dtype=np.int16)
                    sample_rate = int(payload["sample_rate_hz"])
                if sample_rate != 256 or samples.ndim != 2 or samples.shape[1] != 2:
                    raise ValueError(f"{label} waveform must be 256 Hz and two-channel")
                samples_by_state[label] = samples
            model, metrics, frequencies = train_initialization_model(
                samples_by_state,
                progress=report,
            )
            latest = (
                db.query(ModelVersion)
                .filter(ModelVersion.patient_id == run.patient_id)
                .order_by(ModelVersion.version.desc())
                .first()
            )
            metrics.update(
                {
                    "source": "four_state_initialization",
                    "initialization_id": run.id,
                    "mode": run.mode,
                    "short_demo_data": run.mode == "demo",
                }
            )
            model["training"] = {
                "initialization_id": run.id,
                "mode": run.mode,
                "short_demo_data": run.mode == "demo",
                "approved": False,
            }
            row = ModelVersion(
                patient_id=run.patient_id,
                version=latest.version + 1 if latest else 1,
                status=WorkflowStatus.DRAFT,
                algorithm="five_feature_gmm",
                payload=model,
                metrics=metrics,
            )
            db.add(row)
            db.flush()
            run.model_version_id = row.id
            run.frequency_results = frequencies
            run.quality_summary = {
                "all_segments_accepted": True,
                "segment_count": 4,
                "metrics": metrics,
            }
            run.status = InitializationStatus.REVIEW
            run.analysis_stage = "review"
            run.progress_percent = 100
            run.error = None
            run.updated_at = utcnow()
            db.add(
                AuditLog(
                    actor_user_id=run.created_by,
                    action="initialization.analyzed",
                    target_type="initialization",
                    target_id=run.id,
                    after={"model_version_id": row.id, "metrics": metrics},
                )
            )
            db.commit()
            return serialize_initialization(run, segments)
        except Exception as error:
            run.status = InitializationStatus.FAILED
            run.analysis_stage = "failed"
            run.error = str(error)[:2000]
            run.updated_at = utcnow()
            db.add(
                AuditLog(
                    actor_user_id=run.created_by,
                    action="initialization.failed",
                    target_type="initialization",
                    target_id=run.id,
                    after={"error": run.error},
                )
            )
            db.commit()
            raise
