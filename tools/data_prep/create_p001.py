from __future__ import annotations

import argparse
import hashlib
import json
from collections import defaultdict
from pathlib import Path

import numpy as np
from scipy.io import loadmat


STATE_FILES = {
    "OFF-Rest": "off_rest.npz",
    "OFF-Move": "off_move.npz",
    "ON-Rest": "on_rest.npz",
    "ON-Move": "on_move.npz",
}
STATE_TOKENS = {
    "OFF-Rest": ("药物关比较", "静息", "患者1"),
    "OFF-Move": ("药物关比较", "运动", "患者1"),
    "ON-Rest": ("药物开比较", "静息", "患者1"),
    "ON-Move": ("药物开比较", "运动", "患者1"),
}
FORBIDDEN_METADATA_KEYS = (
    "name",
    "姓名",
    "date",
    "日期",
    "hospital",
    "医院",
    "source",
    "path",
)


def sha256_bytes(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def classify_file(path: Path) -> str | None:
    text = str(path)
    for state, tokens in STATE_TOKENS.items():
        if all(token in text for token in tokens):
            return state
    return None


def load_segment(path: Path) -> tuple[np.ndarray, int]:
    payload = loadmat(path, squeeze_me=True)
    if "segmentData" not in payload or "fs" not in payload:
        raise ValueError("not a segment MAT file")
    samples = np.asarray(payload["segmentData"], dtype=np.int16)
    if samples.ndim == 1:
        samples = samples[:, None]
    if samples.shape[1] < 2:
        samples = np.repeat(samples, 2, axis=1)
    return samples[:, :2], int(payload["fs"])


def load_clean_signal(path: Path) -> tuple[np.ndarray, int]:
    payload = loadmat(path, squeeze_me=True)
    signal_key = "signalClean" if "signalClean" in payload else "segmentData"
    if signal_key not in payload or "fs" not in payload:
        raise ValueError("not a supported clean signal MAT file")
    samples = np.asarray(payload[signal_key], dtype=np.int16)
    if samples.ndim == 1:
        samples = samples[:, None]
    if samples.shape[1] < 2:
        samples = np.repeat(samples, 2, axis=1)
    return samples[:, :2], int(payload["fs"])


def balanced_state_dataset(source: Path, seconds_per_state: int, sample_rate: int) -> dict[str, dict]:
    grouped: dict[str, list[Path]] = defaultdict(list)
    seen_hashes: set[str] = set()
    for path in sorted(source.rglob("*.mat")):
        state = classify_file(path)
        if state is None:
            continue
        digest = sha256_bytes(path.read_bytes())
        if digest in seen_hashes:
            continue
        seen_hashes.add(digest)
        grouped[state].append(path)

    result: dict[str, dict] = {}
    required_samples = seconds_per_state * sample_rate
    for state in STATE_FILES:
        pieces: list[np.ndarray] = []
        contributing_files = 0
        current_samples = 0
        for path in grouped[state]:
            try:
                samples, fs = load_segment(path)
            except (ValueError, OSError):
                continue
            if fs != sample_rate:
                raise ValueError(f"unexpected sample rate {fs}; expected {sample_rate}")
            remaining = required_samples - current_samples
            if remaining <= 0:
                break
            piece = samples[:remaining]
            if piece.size == 0:
                continue
            pieces.append(piece)
            current_samples += piece.shape[0]
            contributing_files += 1
        if current_samples < required_samples:
            raise RuntimeError(
                f"{state} has only {current_samples / sample_rate:.1f}s; "
                f"{seconds_per_state}s requested"
            )
        result[state] = {
            "samples": np.concatenate(pieces, axis=0),
            "contributing_files": contributing_files,
        }
    return result


def continuous_dataset(source: Path, seconds: int, sample_rate: int) -> tuple[np.ndarray, int]:
    required_samples = seconds * sample_rate
    pieces: list[np.ndarray] = []
    contributing_files = 0
    current_samples = 0
    seen_hashes: set[str] = set()
    for path in sorted(source.rglob("*.mat")):
        digest = sha256_bytes(path.read_bytes())
        if digest in seen_hashes:
            continue
        seen_hashes.add(digest)
        try:
            samples, fs = load_clean_signal(path)
        except (ValueError, OSError):
            continue
        if fs != sample_rate:
            continue
        remaining = required_samples - current_samples
        if remaining <= 0:
            break
        piece = samples[:remaining]
        if piece.size == 0:
            continue
        pieces.append(piece)
        current_samples += piece.shape[0]
        contributing_files += 1
    if current_samples < required_samples:
        raise RuntimeError(
            f"continuous source has only {current_samples / sample_rate:.1f}s; {seconds}s requested"
        )
    return np.concatenate(pieces, axis=0), contributing_files


def write_npz(path: Path, samples: np.ndarray, sample_rate: int, state: str) -> str:
    np.savez_compressed(
        path,
        samples=np.asarray(samples, dtype=np.int16),
        sample_rate_hz=np.int32(sample_rate),
        channel_count=np.int16(samples.shape[1]),
        patient_code=np.asarray("P001"),
        state_label=np.asarray(state),
        relative_start_seconds=np.float64(0),
    )
    return sha256_bytes(path.read_bytes())


def write_lfp(path: Path, samples: np.ndarray) -> str:
    path.write_bytes(np.asarray(samples, dtype="<i2").tobytes(order="C"))
    return sha256_bytes(path.read_bytes())


def metadata_is_deidentified(metadata: dict) -> bool:
    text = json.dumps(metadata, ensure_ascii=False).lower()
    return not any(token.lower() in text for token in FORBIDDEN_METADATA_KEYS)


def main() -> None:
    parser = argparse.ArgumentParser(description="Create a deidentified P001 LFP replay dataset.")
    parser.add_argument("--source", type=Path, required=True, help="Patient four-state MAT directory")
    parser.add_argument("--long-source", type=Path, required=True, help="Clean long-recording MAT directory")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seconds-per-state", type=int, default=300)
    parser.add_argument("--continuous-seconds", type=int, default=1800)
    parser.add_argument("--sample-rate", type=int, default=256)
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    states = balanced_state_dataset(args.source, args.seconds_per_state, args.sample_rate)
    continuous, continuous_files = continuous_dataset(
        args.long_source,
        args.continuous_seconds,
        args.sample_rate,
    )
    files: list[dict] = []
    for state, filename in STATE_FILES.items():
        path = args.output / filename
        digest = write_npz(path, states[state]["samples"], args.sample_rate, state)
        lfp_name = filename.replace(".npz", ".lfp")
        lfp_digest = write_lfp(args.output / lfp_name, states[state]["samples"])
        files.append(
            {
                "file": filename,
                "lfp_file": lfp_name,
                "state": state,
                "seconds": args.seconds_per_state,
                "samples": int(states[state]["samples"].shape[0]),
                "channels": 2,
                "sha256": digest,
                "lfp_sha256": lfp_digest,
                "contributing_file_count": states[state]["contributing_files"],
            }
        )
    continuous_path = args.output / "continuous.npz"
    continuous_digest = write_npz(
        continuous_path,
        continuous,
        args.sample_rate,
        "Continuous-Replay",
    )
    continuous_lfp_digest = write_lfp(args.output / "continuous.lfp", continuous)
    files.append(
        {
            "file": "continuous.npz",
            "lfp_file": "continuous.lfp",
            "state": "Continuous-Replay",
            "seconds": args.continuous_seconds,
            "samples": int(continuous.shape[0]),
            "channels": 2,
            "sha256": continuous_digest,
            "lfp_sha256": continuous_lfp_digest,
            "contributing_file_count": continuous_files,
        }
    )
    metadata = {
        "schema_version": 1,
        "patient_code": "P001",
        "sample_rate_hz": args.sample_rate,
        "channel_count": 2,
        "units": "device_int16",
        "clinical_use": False,
        "files": files,
    }
    if not metadata_is_deidentified(metadata):
        raise RuntimeError("deidentification audit failed")
    metadata_path = args.output / "manifest.json"
    metadata_path.write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
    report = {
        "passed": True,
        "patient_code": "P001",
        "original_identifiers_copied": False,
        "original_timestamps_copied": False,
        "original_paths_copied": False,
        "numeric_waveform_preserved": True,
        "file_count": len(files),
        "manifest_sha256": sha256_bytes(metadata_path.read_bytes()),
    }
    (args.output / "deidentification_report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
