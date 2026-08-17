from __future__ import annotations

import csv
import io
import json
import tempfile
import zipfile
from datetime import datetime, timezone
from email.message import EmailMessage
from pathlib import Path
from typing import Any

import numpy as np
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from scipy.io import savemat
from sqlalchemy.orm import Session

from ..models import (
    InferenceResult,
    LfpSession,
    MedicationEvent,
    ParameterProposal,
    Patient,
    SymptomEntry,
)
from .storage import storage


MIME_TYPES = {
    "pdf": "application/pdf",
    "csv": "text/csv",
    "mat": "application/x-matlab-data",
    "edf": "application/edf",
    "eml": "message/rfc822",
    "zip": "application/zip",
}


def _snapshot(db: Session, patient: Patient) -> dict[str, Any]:
    symptoms = (
        db.query(SymptomEntry)
        .filter(SymptomEntry.patient_id == patient.id)
        .order_by(SymptomEntry.recorded_at.desc())
        .all()
    )
    medications = (
        db.query(MedicationEvent)
        .filter(MedicationEvent.patient_id == patient.id)
        .order_by(MedicationEvent.recorded_at.desc())
        .all()
    )
    inferences = (
        db.query(InferenceResult)
        .filter(InferenceResult.patient_id == patient.id)
        .order_by(InferenceResult.recorded_at.desc())
        .limit(1000)
        .all()
    )
    proposals = (
        db.query(ParameterProposal)
        .filter(ParameterProposal.patient_id == patient.id)
        .order_by(ParameterProposal.created_at.desc())
        .all()
    )
    return {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "patient": {
            "code": patient.code,
            "name": patient.name,
            "gender": patient.gender,
            "age": patient.age,
            "implant_date": patient.implant_date,
            "summary": patient.summary,
        },
        "symptoms": [
            {
                "event_id": row.event_id,
                "tremor": row.tremor,
                "rigidity": row.rigidity,
                "speech": row.speech,
                "note": row.note,
                "recorded_at": row.recorded_at.isoformat(),
            }
            for row in symptoms
        ],
        "medications": [
            {
                "event_id": row.event_id,
                "medication_name": row.medication_name,
                "status": row.status,
                "recorded_at": row.recorded_at.isoformat(),
            }
            for row in medications
        ],
        "inferences": [
            {
                "event_id": row.event_id,
                "features": row.features,
                "probabilities": row.probabilities,
                "top_state": row.top_state,
                "confidence": row.confidence,
                "rejected": row.rejected,
                "recorded_at": row.recorded_at.isoformat(),
            }
            for row in inferences
        ],
        "parameter_proposals": [
            {
                "id": row.id,
                "status": row.status.value,
                "parameters": row.parameters,
                "score": row.score,
                "safety_result": row.safety_result,
                "created_at": row.created_at.isoformat(),
            }
            for row in proposals
        ],
    }


def _csv_bytes(snapshot: dict[str, Any]) -> bytes:
    stream = io.StringIO(newline="")
    writer = csv.writer(stream)
    writer.writerow(
        [
            "record_type",
            "timestamp",
            "event_id",
            "field_1",
            "field_2",
            "field_3",
            "detail",
        ]
    )
    for row in snapshot["symptoms"]:
        writer.writerow(
            [
                "symptom",
                row["recorded_at"],
                row["event_id"],
                row["tremor"],
                row["rigidity"],
                row["speech"],
                row["note"],
            ]
        )
    for row in snapshot["medications"]:
        writer.writerow(
            [
                "medication",
                row["recorded_at"],
                row["event_id"],
                row["medication_name"],
                row["status"],
                "",
                "",
            ]
        )
    for row in snapshot["inferences"]:
        writer.writerow(
            [
                "inference",
                row["recorded_at"],
                row["event_id"],
                row["top_state"],
                row["confidence"],
                row["rejected"],
                json.dumps(row["probabilities"], ensure_ascii=False),
            ]
        )
    return ("\ufeff" + stream.getvalue()).encode("utf-8")


def _register_cjk_font() -> str:
    candidates = (
        Path("C:/Windows/Fonts/msyh.ttc"),
        Path("C:/Windows/Fonts/simhei.ttf"),
        Path("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
    )
    for path in candidates:
        if path.exists():
            try:
                pdfmetrics.registerFont(TTFont("OminidaptCJK", str(path), subfontIndex=0))
                return "OminidaptCJK"
            except Exception:
                continue
    return "Helvetica"


def _pdf_bytes(snapshot: dict[str, Any]) -> bytes:
    buffer = io.BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=A4)
    font = _register_cjk_font()
    width, height = A4
    y = height - 52
    pdf.setFont(font, 18)
    pdf.drawString(48, y, "Ominidapt PD 科研演示数据报告")
    y -= 32
    pdf.setFont(font, 10)
    patient = snapshot["patient"]
    lines = [
        f"患者编号：{patient['code']}",
        f"生成时间：{snapshot['generated_at']}",
        f"症状记录：{len(snapshot['symptoms'])} 条",
        f"用药记录：{len(snapshot['medications'])} 条",
        f"状态推理：{len(snapshot['inferences'])} 条",
        f"参数提案：{len(snapshot['parameter_proposals'])} 条",
        "用途：仅限脱敏科研演示，不用于临床治疗决策。",
    ]
    for line in lines:
        pdf.drawString(48, y, line)
        y -= 18
    y -= 8
    pdf.setFont(font, 13)
    pdf.drawString(48, y, "最近症状记录")
    y -= 22
    pdf.setFont(font, 9)
    for row in snapshot["symptoms"][:20]:
        text = (
            f"{row['recorded_at'][:19]}  震颤 {row['tremor']}  "
            f"僵硬 {row['rigidity']}  言语 {row['speech']}  {row['note'][:40]}"
        )
        pdf.drawString(48, y, text)
        y -= 15
        if y < 60:
            pdf.showPage()
            pdf.setFont(font, 9)
            y = height - 52
    pdf.save()
    return buffer.getvalue()


def _latest_signal(db: Session, patient_id: str) -> tuple[np.ndarray, int] | None:
    session = (
        db.query(LfpSession)
        .filter(LfpSession.patient_id == patient_id, LfpSession.object_key.is_not(None))
        .order_by(LfpSession.started_at.desc())
        .first()
    )
    if session is None or session.object_key is None:
        return None
    content = storage.get_bytes(session.object_key)
    with np.load(io.BytesIO(content), allow_pickle=False) as payload:
        samples = np.asarray(payload["samples"], dtype=np.int16)
        sample_rate = int(payload["sample_rate_hz"])
    if samples.ndim == 1:
        samples = samples[:, None]
    return samples, sample_rate


def _mat_bytes(snapshot: dict[str, Any], signal: tuple[np.ndarray, int] | None) -> bytes:
    buffer = io.BytesIO()
    payload: dict[str, Any] = {
        "patient_code": snapshot["patient"]["code"],
        "generated_at": snapshot["generated_at"],
        "symptom_json": json.dumps(snapshot["symptoms"], ensure_ascii=False),
        "medication_json": json.dumps(snapshot["medications"], ensure_ascii=False),
        "inference_json": json.dumps(snapshot["inferences"], ensure_ascii=False),
    }
    if signal is not None:
        payload["lfp_samples_int16"] = signal[0]
        payload["sample_rate_hz"] = signal[1]
    savemat(buffer, payload, do_compression=True)
    return buffer.getvalue()


def _edf_bytes(patient_code: str, signal: tuple[np.ndarray, int] | None) -> bytes:
    if signal is None:
        raise ValueError("no recorded LFP session is available for EDF export")
    samples, sample_rate = signal
    channel_count = samples.shape[1]
    record_count = int(np.ceil(samples.shape[0] / sample_rate))
    padded = np.zeros((record_count * sample_rate, channel_count), dtype="<i2")
    padded[: samples.shape[0], :] = np.clip(samples, -32768, 32767).astype("<i2")

    def field(value: object, width: int) -> bytes:
        encoded = str(value).encode("ascii", errors="replace")[:width]
        return encoded.ljust(width, b" ")

    now = datetime.now()
    header_bytes = 256 + channel_count * 256
    header = b"".join(
        [
            field("0", 8),
            field(patient_code, 80),
            field("Ominidapt research simulation only", 80),
            field(now.strftime("%d.%m.%y"), 8),
            field(now.strftime("%H.%M.%S"), 8),
            field(header_bytes, 8),
            field("", 44),
            field(record_count, 8),
            field(1, 8),
            field(channel_count, 4),
        ]
    )
    per_signal_fields = (
        ([f"LFP-{index + 1}" for index in range(channel_count)], 16),
        (["BLE simulator"] * channel_count, 80),
        (["uV"] * channel_count, 8),
        ([int(np.min(padded[:, index])) for index in range(channel_count)], 8),
        ([int(np.max(padded[:, index])) or 1 for index in range(channel_count)], 8),
        ([-32768] * channel_count, 8),
        ([32767] * channel_count, 8),
        (["none"] * channel_count, 80),
        ([sample_rate] * channel_count, 8),
        ([""] * channel_count, 32),
    )
    for values, width in per_signal_fields:
        header += b"".join(field(value, width) for value in values)
    output = io.BytesIO()
    output.write(header)
    for record_index in range(record_count):
        start = record_index * sample_rate
        end = start + sample_rate
        for channel_index in range(channel_count):
            output.write(padded[start:end, channel_index].tobytes(order="C"))
    return output.getvalue()


def build_export(db: Session, patient: Patient, export_format: str) -> tuple[bytes, str, str]:
    snapshot = _snapshot(db, patient)
    signal = _latest_signal(db, patient.id)
    base_name = f"{patient.code}_omnidapt_report"
    if export_format == "csv":
        return _csv_bytes(snapshot), f"{base_name}.csv", MIME_TYPES["csv"]
    if export_format == "pdf":
        return _pdf_bytes(snapshot), f"{base_name}.pdf", MIME_TYPES["pdf"]
    if export_format == "mat":
        return _mat_bytes(snapshot, signal), f"{base_name}.mat", MIME_TYPES["mat"]
    if export_format == "edf":
        return _edf_bytes(patient.code, signal), f"{base_name}.edf", MIME_TYPES["edf"]
    if export_format == "eml":
        message = EmailMessage()
        message["Subject"] = f"Ominidapt PD {patient.code} 科研演示报告"
        message["From"] = "omnidapt@localhost"
        message["To"] = "research@localhost"
        message.set_content("附件为脱敏科研演示报告，不用于临床诊疗。")
        pdf_content = _pdf_bytes(snapshot)
        message.add_attachment(
            pdf_content,
            maintype="application",
            subtype="pdf",
            filename=f"{base_name}.pdf",
        )
        return message.as_bytes(), f"{base_name}.eml", MIME_TYPES["eml"]
    if export_format == "zip":
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            archive.writestr(f"{base_name}.csv", _csv_bytes(snapshot))
            archive.writestr(f"{base_name}.pdf", _pdf_bytes(snapshot))
            archive.writestr(f"{base_name}.mat", _mat_bytes(snapshot, signal))
            if signal is not None:
                archive.writestr(f"{base_name}.edf", _edf_bytes(patient.code, signal))
            archive.writestr(
                "README.txt",
                "Ominidapt PD 脱敏科研演示导出；不用于临床治疗决策。\n",
            )
        return buffer.getvalue(), f"{base_name}.zip", MIME_TYPES["zip"]
    raise ValueError(f"unsupported export format: {export_format}")
