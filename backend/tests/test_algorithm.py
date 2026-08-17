from __future__ import annotations

import numpy as np

from app.services.algorithm import (
    STATE_LABELS,
    StreamingFeatureExtractor,
    check_safety,
    compute_feedback_score,
    default_model,
    infer_state_probabilities,
    recommend_next_parameter,
    train_initialization_model,
)


def test_probabilities_sum_to_one() -> None:
    result = infer_state_probabilities([0.3, 0.4, 1.2, 0.45, 0.2], default_model())
    assert abs(sum(result["probabilities"].values()) - 1.0) < 1e-9
    assert result["top_state"] in STATE_LABELS


def test_streaming_feature_extractor_warms_after_thirty_windows() -> None:
    extractor = StreamingFeatureExtractor()
    rng = np.random.default_rng(42)
    for _ in range(29):
        features = extractor.push(rng.normal(size=(256, 2)))
        assert len(features) == 5
        assert not extractor.warmed_up
    extractor.push(rng.normal(size=(256, 2)))
    assert extractor.warmed_up


def test_feedback_side_effect_blocks_candidate() -> None:
    result = compute_feedback_score(
        {"tremor_relief": 9, "rigidity_relief": 8, "movement_fluency": 8},
        {"paresthesia": 8},
    )
    assert result["blocked"]
    assert result["final_score"] < result["raw_score"]


def test_safety_rejects_large_change() -> None:
    result = check_safety(
        {"current_ma": 2.4, "pulse_width_us": 70, "frequency_hz": 130},
        {"current_ma": 2.0, "pulse_width_us": 70, "frequency_hz": 130},
        {
            "current_min_ma": 1,
            "current_max_ma": 3,
            "pulse_width_min_us": 50,
            "pulse_width_max_us": 90,
            "frequency_min_hz": 120,
            "frequency_max_hz": 150,
            "max_delta_current_ma": 0.2,
        },
    )
    assert not result["allowed"]
    assert "delta_current_too_large" in result["reasons"]


def test_gp_recommendation_stays_inside_bounds() -> None:
    result = recommend_next_parameter(
        [
            {"current_ma": 1.8, "score": 65},
            {"current_ma": 2.0, "score": 80},
            {"current_ma": 2.2, "score": 72},
        ],
        {"current_min_ma": 1.0, "current_max_ma": 3.0},
    )
    assert 1.0 <= result["current_ma"] <= 3.0
    assert result["acquisition"] == "expected_improvement"


def test_four_state_training_extracts_personalized_fisher_bands() -> None:
    rng = np.random.default_rng(2026)
    time = np.arange(30 * 256) / 256.0

    def signal(beta16: float, beta28: float, gamma75: float) -> np.ndarray:
        base = (
            beta16 * np.sin(2 * np.pi * 16 * time)
            + beta28 * np.sin(2 * np.pi * 28 * time)
            + gamma75 * np.sin(2 * np.pi * 75 * time)
        )
        return np.column_stack(
            [
                base + rng.normal(0, 0.15, time.size),
                0.85 * base + rng.normal(0, 0.15, time.size),
            ]
        ).astype(np.float64)

    model, metrics, result = train_initialization_model(
        {
            "OFF-Rest": signal(2.0, 0.3, 0.2),
            "OFF-Move": signal(1.8, 1.8, 1.2),
            "ON-Rest": signal(0.4, 0.3, 0.2),
            "ON-Move": signal(0.4, 1.8, 1.6),
        }
    )

    assert model["bands"] == result["bands"]
    assert result["strategy"] == "exclusive_fisher_intersection"
    assert 13 <= result["beta_split_hz"] < 35
    assert metrics["validation_windows"] == 36
    assert set(model["states"]) == set(STATE_LABELS)
