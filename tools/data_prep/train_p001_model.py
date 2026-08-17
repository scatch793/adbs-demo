from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np

from backend.app.services.algorithm import (
    STATE_LABELS,
    StreamingFeatureExtractor,
    fit_gmm_model,
    infer_state_probabilities,
)


FILE_BY_STATE = {
    "OFF-Rest": "off_rest.npz",
    "OFF-Move": "off_move.npz",
    "ON-Rest": "on_rest.npz",
    "ON-Move": "on_move.npz",
}


def features_for_file(path: Path) -> np.ndarray:
    with np.load(path, allow_pickle=False) as payload:
        samples = np.asarray(payload["samples"], dtype=np.int16)
        sample_rate = int(payload["sample_rate_hz"])
    extractor = StreamingFeatureExtractor(sample_rate_hz=sample_rate)
    rows: list[list[float]] = []
    whole_windows = samples.shape[0] // sample_rate
    for window_index in range(whole_windows):
        start = window_index * sample_rate
        features = extractor.push(samples[start : start + sample_rate])
        if extractor.warmed_up:
            rows.append(features)
    return np.asarray(rows, dtype=float)


def main() -> None:
    parser = argparse.ArgumentParser(description="Train the versioned P001 five-feature GMM model.")
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    feature_blocks: list[np.ndarray] = []
    labels: list[str] = []
    per_state_count: dict[str, int] = {}
    for state in STATE_LABELS:
        features = features_for_file(args.dataset / FILE_BY_STATE[state])
        if features.shape[0] < 20:
            raise RuntimeError(f"{state} does not contain enough warmed-up windows")
        feature_blocks.append(features)
        labels.extend([state] * features.shape[0])
        per_state_count[state] = int(features.shape[0])
    all_features = np.vstack(feature_blocks)
    model = fit_gmm_model(all_features, labels)
    predictions = [
        infer_state_probabilities(row, model)["top_state"]
        for row in all_features
    ]
    accuracy = float(np.mean(np.asarray(predictions) == np.asarray(labels)))
    model["model_id"] = "P001-five-feature-gmm-v1"
    model["patient_code"] = "P001"
    model["training_summary"] = {
        "window_count_by_state": per_state_count,
        "training_accuracy": accuracy,
        "evaluation_note": "Training-set smoke metric only; not a clinical validation result.",
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(model, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(model["training_summary"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
