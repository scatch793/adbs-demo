from __future__ import annotations

import math
from collections import deque
from dataclasses import asdict, dataclass
from typing import Any, Callable, Iterable, Mapping, Sequence

import numpy as np
from scipy.signal import welch
from scipy.stats import norm
from sklearn.gaussian_process import GaussianProcessRegressor
from sklearn.gaussian_process.kernels import ConstantKernel, Matern, WhiteKernel
from sklearn.mixture import GaussianMixture


STATE_LABELS = ("OFF-Rest", "OFF-Move", "ON-Rest", "ON-Move")
FEATURE_NAMES = (
    "delta_aperiodic_offset",
    "aperiodic_exponent",
    "medication_beta",
    "movement_beta",
    "movement_gamma",
)
FREQUENCIES = np.arange(2.0, 100.0 + 0.5, 0.5)
DEFAULT_BANDS = {
    "medication_beta": [15.0, 19.0],
    "movement_beta": [26.0, 34.0],
    "movement_gamma": [81.0, 89.0],
}
DEFAULT_ANCHORS_MA = {
    "OFF-Rest": 2.0,
    "OFF-Move": 2.2,
    "ON-Rest": 1.6,
    "ON-Move": 1.8,
}


@dataclass(frozen=True)
class SafetyBounds:
    current_min_ma: float = 1.0
    current_max_ma: float = 3.0
    pulse_width_min_us: float = 50.0
    pulse_width_max_us: float = 90.0
    frequency_min_hz: float = 120.0
    frequency_max_hz: float = 150.0
    max_delta_current_ma: float = 0.2


def _band_sum(periodic: np.ndarray, low: float, high: float) -> float:
    mask = (FREQUENCIES >= low) & (FREQUENCIES <= high)
    return float(np.sum(periodic[mask]))


def extract_spectral_components(
    samples: np.ndarray,
    sample_rate_hz: int = 256,
) -> tuple[float, float, np.ndarray]:
    """Return aperiodic offset/exponent and 1/f-removed log power.

    The frequency grid and background fit regions mirror the MATLAB research
    scripts: 2:0.5:100 Hz, excluding beta and movement-gamma bands from the
    aperiodic regression.
    """
    signal = np.asarray(samples, dtype=float)
    if signal.ndim == 1:
        signal = signal[:, None]
    if signal.shape[0] != sample_rate_hz:
        raise ValueError("feature extraction requires exactly one second of samples")
    if signal.shape[1] < 1:
        raise ValueError("at least one channel is required")
    signal = signal * 1e-6
    _, power = welch(
        signal,
        fs=sample_rate_hz,
        window="hann",
        nperseg=sample_rate_hz,
        noverlap=0,
        nfft=sample_rate_hz * 2,
        axis=0,
        scaling="density",
    )
    native_freqs = np.fft.rfftfreq(sample_rate_hz * 2, d=1.0 / sample_rate_hz)
    mean_power = np.mean(power, axis=1)
    sampled_power = np.interp(FREQUENCIES, native_freqs, mean_power)
    log_frequency = np.log10(FREQUENCIES)
    log_power = np.log10(np.maximum(sampled_power, np.finfo(float).tiny))
    fit_mask = (FREQUENCIES < 13) | (
        (FREQUENCIES > 35) & (FREQUENCIES < 60)
    ) | (FREQUENCIES > 90)
    slope, offset = np.polyfit(log_frequency[fit_mask], log_power[fit_mask], 1)
    exponent = -float(slope)
    baseline = slope * log_frequency + offset
    periodic = np.maximum(log_power - baseline, 0.0)
    return float(offset), exponent, periodic


def extract_raw_spectral_features(
    samples: np.ndarray,
    sample_rate_hz: int = 256,
    bands: Mapping[str, Sequence[float]] = DEFAULT_BANDS,
) -> tuple[float, float, float, float, float]:
    offset, exponent, periodic = extract_spectral_components(samples, sample_rate_hz)
    return (
        offset,
        exponent,
        _band_sum(periodic, *bands["medication_beta"]),
        _band_sum(periodic, *bands["movement_beta"]),
        _band_sum(periodic, *bands["movement_gamma"]),
    )


def _fisher_score(group_a: np.ndarray, group_b: np.ndarray) -> np.ndarray:
    if group_a.shape[0] < 6 or group_b.shape[0] < 6:
        return np.zeros(group_a.shape[1], dtype=float)
    numerator = (np.mean(group_a, axis=0) - np.mean(group_b, axis=0)) ** 2
    denominator = np.var(group_a, axis=0) + np.var(group_b, axis=0) + 1e-6
    result = numerator / denominator
    result[~np.isfinite(result)] = 0.0
    return result


def _half_height_band(scores: np.ndarray, frequencies: np.ndarray) -> list[float]:
    if scores.size == 0 or not np.any(np.isfinite(scores)) or float(np.nanmax(scores)) <= 0:
        return [float(frequencies[0]), float(frequencies[-1])]
    peak = int(np.nanargmax(scores))
    threshold = float(scores[peak]) * 0.5
    left = peak
    while left > 0 and scores[left - 1] >= threshold:
        left -= 1
    right = peak
    while right < scores.size - 1 and scores[right + 1] >= threshold:
        right += 1
    return [float(frequencies[left]), float(frequencies[right])]


def select_personalized_bands(
    periodic_by_state: Mapping[str, np.ndarray],
) -> tuple[dict[str, list[float]], dict[str, Any]]:
    """Select mutually exclusive medication/movement beta and movement gamma."""
    beta_mask = (FREQUENCIES >= 13) & (FREQUENCIES <= 35)
    gamma_mask = (FREQUENCIES >= 60) & (FREQUENCIES <= 90)
    beta_frequencies = FREQUENCIES[beta_mask]
    gamma_frequencies = FREQUENCIES[gamma_mask]
    med = _fisher_score(
        periodic_by_state["OFF-Rest"][:, beta_mask],
        periodic_by_state["ON-Rest"][:, beta_mask],
    )
    move = _fisher_score(
        periodic_by_state["ON-Rest"][:, beta_mask],
        periodic_by_state["ON-Move"][:, beta_mask],
    )
    gamma = _fisher_score(
        periodic_by_state["ON-Rest"][:, gamma_mask],
        periodic_by_state["ON-Move"][:, gamma_mask],
    )
    med_norm = med / np.max(med) if np.max(med) > 0 else np.zeros_like(med)
    move_norm = move / np.max(move) if np.max(move) > 0 else np.zeros_like(move)
    if np.max(med_norm) == 0 or np.max(move_norm) == 0:
        split_hz = 20.0
    else:
        med_peak = int(np.argmax(med_norm))
        move_peak = int(np.argmax(move_norm))
        low, high = sorted((med_peak, move_peak))
        difference = med_norm - move_norm
        split_index = None
        for index in range(low, high):
            if difference[index] == 0 or difference[index] * difference[index + 1] <= 0:
                split_index = index
                break
        if split_index is None:
            split_index = low + int(np.argmin(np.abs(difference[low : high + 1])))
            split_index = min(max(split_index, 0), beta_frequencies.size - 2)
        split_hz = float(np.mean(beta_frequencies[split_index : split_index + 2]))
    split_hz = float(np.clip(split_hz, beta_frequencies[0], beta_frequencies[-2]))
    bands = {
        "medication_beta": [float(beta_frequencies[0]), split_hz],
        "movement_beta": [
            float(beta_frequencies[beta_frequencies > split_hz][0]),
            float(beta_frequencies[-1]),
        ],
        "movement_gamma": _half_height_band(gamma, gamma_frequencies),
    }
    diagnostics = {
        "strategy": "exclusive_fisher_intersection",
        "beta_split_hz": split_hz,
        "frequencies_beta_hz": beta_frequencies.tolist(),
        "fisher_medication_beta": med.tolist(),
        "fisher_movement_beta": move.tolist(),
        "frequencies_gamma_hz": gamma_frequencies.tolist(),
        "fisher_movement_gamma": gamma.tolist(),
        "comparisons": {
            "medication": ["OFF-Rest", "ON-Rest"],
            "movement": ["ON-Rest", "ON-Move"],
        },
    }
    return bands, diagnostics


def train_initialization_model(
    samples_by_state: Mapping[str, np.ndarray],
    progress: Callable[[str, int], None] | None = None,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    """Train fast and stable five-feature models from four baseline segments."""
    report = progress or (lambda _stage, _percent: None)
    unknown = sorted(set(samples_by_state) - set(STATE_LABELS))
    missing = sorted(set(STATE_LABELS) - set(samples_by_state))
    if unknown or missing:
        raise ValueError(f"invalid initialization states, missing={missing}, unknown={unknown}")
    report("psd", 25)
    raw: dict[str, list[tuple[float, float, np.ndarray]]] = {}
    for label in STATE_LABELS:
        signal = np.asarray(samples_by_state[label])
        if signal.ndim != 2 or signal.shape[1] < 1:
            raise ValueError(f"{label} waveform must have shape (N, channels)")
        window_count = signal.shape[0] // 256
        if window_count < 24:
            raise ValueError(f"{label} has fewer than 24 complete one-second windows")
        raw[label] = [
            extract_spectral_components(signal[index * 256 : (index + 1) * 256])
            for index in range(window_count)
        ]
    train_periodic = {
        label: np.stack(
            [item[2] for item in values[: max(18, int(len(values) * 0.7))]]
        )
        for label, values in raw.items()
    }
    report("fisher", 42)
    bands, diagnostics = select_personalized_bands(train_periodic)
    split_indices: dict[str, int] = {}
    for label in STATE_LABELS:
        windows = samples_by_state[label].shape[0] // 256
        split_indices[label] = max(18, int(windows * 0.7))

    def build_features(smoothing_windows: int) -> tuple[np.ndarray, np.ndarray]:
        feature_rows: list[list[float]] = []
        feature_labels: list[str] = []
        for label in STATE_LABELS:
            extractor = StreamingFeatureExtractor(
                bands=bands,
                smoothing_windows=smoothing_windows,
            )
            values = samples_by_state[label]
            windows = values.shape[0] // 256
            feature_rows.extend(
                extractor.push(values[index * 256 : (index + 1) * 256])
                for index in range(windows)
            )
            feature_labels.extend([label] * windows)
        return np.asarray(feature_rows), np.asarray(feature_labels)

    report("features", 58)
    stable_features, all_labels = build_features(30)
    fast_features, fast_labels = build_features(5)
    train_mask = np.zeros(all_labels.size, dtype=bool)
    test_mask = np.zeros(all_labels.size, dtype=bool)
    cursor = 0
    for label in STATE_LABELS:
        count = len(raw[label])
        split = split_indices[label]
        train_mask[cursor : cursor + split] = True
        test_mask[cursor + split : cursor + count] = True
        cursor += count
    def train_profile(
        name: str,
        features: np.ndarray,
        labels: np.ndarray,
        smoothing_windows: int,
        probability_smoothing_windows: int,
    ) -> tuple[dict[str, Any], dict[str, Any]]:
        validation_model = fit_gmm_model(
            features[train_mask],
            labels[train_mask],
            bands,
            smoothing_windows=smoothing_windows,
            probability_smoothing_windows=probability_smoothing_windows,
        )
        predictions = [
            infer_state_probabilities(row, validation_model)["top_state"]
            for row in features[test_mask]
        ]
        truth = labels[test_mask]
        confusion = {
            actual: {
                predicted: int(
                    np.count_nonzero(
                        (truth == actual) & (np.asarray(predictions) == predicted)
                    )
                )
                for predicted in STATE_LABELS
            }
            for actual in STATE_LABELS
        }
        accuracy = float(np.mean(np.asarray(predictions) == truth)) if truth.size else 0.0
        f1_values = []
        for label in STATE_LABELS:
            tp = confusion[label][label]
            fp = sum(confusion[actual][label] for actual in STATE_LABELS) - tp
            fn = sum(confusion[label].values()) - tp
            f1_values.append(
                0.0 if 2 * tp + fp + fn == 0 else 2 * tp / (2 * tp + fp + fn)
            )
        final_model = fit_gmm_model(
            features,
            labels,
            bands,
            smoothing_windows=smoothing_windows,
            probability_smoothing_windows=probability_smoothing_windows,
        )
        return final_model, {
            "name": name,
            "accuracy": accuracy,
            "macro_f1": float(np.mean(f1_values)),
            "confusion": confusion,
        }

    report("gmm", 72)
    fast_model, fast_metrics = train_profile("fast", fast_features, fast_labels, 5, 3)
    stable_model, stable_metrics = train_profile(
        "stable",
        stable_features,
        all_labels,
        30,
        5,
    )
    report("validation", 86)
    stable_mean = np.asarray(stable_model["scaler"]["mean"], dtype=float)
    stable_scale = np.maximum(
        np.asarray(stable_model["scaler"]["scale"], dtype=float),
        1e-9,
    )
    normalized_stable = (stable_features - stable_mean) / stable_scale
    feature_points = [
        {
            "state": str(label),
            "window_index": int(index),
            "medication_beta": float(row[2]),
            "movement_beta": float(row[3]),
            "movement_gamma": float(row[4]),
        }
        for index, (row, label) in enumerate(zip(normalized_stable, all_labels))
    ]
    model_bundle = {
        "schema_version": 2,
        "algorithm": "five_feature_dual_gmm",
        "states": list(STATE_LABELS),
        "sample_rate_hz": 256,
        "window_seconds": 1,
        "feature_names": list(FEATURE_NAMES),
        "bands": bands,
        "profiles": {
            "fast": fast_model,
            "stable": stable_model,
        },
        "stimulation_anchors_ma": DEFAULT_ANCHORS_MA,
    }
    metrics = {
        "accuracy": stable_metrics["accuracy"],
        "macro_f1": stable_metrics["macro_f1"],
        "confusion": stable_metrics["confusion"],
        "profiles": {"fast": fast_metrics, "stable": stable_metrics},
        "train_windows": int(np.count_nonzero(train_mask)),
        "validation_windows": int(np.count_nonzero(test_mask)),
        "windows_per_state": {label: len(raw[label]) for label in STATE_LABELS},
        "split": "chronological_70_30",
    }
    report("packaging", 95)
    return model_bundle, metrics, {
        "bands": bands,
        "frequency_axis_hz": FREQUENCIES.tolist(),
        "feature_points": feature_points,
        **diagnostics,
    }


class StreamingFeatureExtractor:
    """Stateful one-second extractor matching the MATLAB 30-window smoothing."""

    def __init__(
        self,
        sample_rate_hz: int = 256,
        smoothing_windows: int = 30,
        bands: Mapping[str, Sequence[float]] = DEFAULT_BANDS,
    ) -> None:
        self.sample_rate_hz = sample_rate_hz
        self.smoothing_windows = smoothing_windows
        self.bands = dict(bands)
        self.offsets: deque[float] = deque(maxlen=smoothing_windows)
        self.exponents: deque[float] = deque(maxlen=smoothing_windows)
        self.med_beta: deque[float] = deque(maxlen=smoothing_windows)
        self.move_beta: deque[float] = deque(maxlen=smoothing_windows)
        self.move_gamma: deque[float] = deque(maxlen=smoothing_windows)

    @property
    def warmed_up(self) -> bool:
        return len(self.offsets) >= self.smoothing_windows

    def push(self, samples: np.ndarray) -> list[float]:
        offset, exponent, med_beta, move_beta, move_gamma = extract_raw_spectral_features(
            samples,
            self.sample_rate_hz,
            self.bands,
        )
        self.offsets.append(offset)
        self.exponents.append(exponent)
        self.med_beta.append(med_beta)
        self.move_beta.append(move_beta)
        self.move_gamma.append(move_gamma)
        delta_offset = offset - float(np.mean(self.offsets))
        return [
            delta_offset,
            float(np.mean(self.exponents)),
            float(np.mean(self.med_beta)),
            float(np.mean(self.move_beta)),
            float(np.mean(self.move_gamma)),
        ]


def default_model() -> dict[str, Any]:
    means = {
        "OFF-Rest": [0.30, 0.40, 1.25, 0.45, 0.20],
        "OFF-Move": [0.42, 0.48, 1.05, 1.10, 0.72],
        "ON-Rest": [-0.10, 0.18, 0.42, 0.32, 0.18],
        "ON-Move": [0.08, 0.20, 0.38, 0.95, 1.15],
    }
    variances = {
        "OFF-Rest": [0.30, 0.30, 0.45, 0.35, 0.25],
        "OFF-Move": [0.32, 0.32, 0.42, 0.45, 0.36],
        "ON-Rest": [0.28, 0.24, 0.35, 0.28, 0.22],
        "ON-Move": [0.30, 0.28, 0.32, 0.42, 0.46],
    }
    return {
        "schema_version": 1,
        "algorithm": "five_feature_gmm",
        "sample_rate_hz": 256,
        "window_seconds": 1,
        "smoothing_windows": 30,
        "feature_names": list(FEATURE_NAMES),
        "bands": DEFAULT_BANDS,
        "scaler": {"mean": [0.0] * 5, "scale": [1.0] * 5},
        "states": {
            label: {
                "prior": 0.25,
                "components": [
                    {
                        "weight": 1.0,
                        "mean": means[label],
                        "covariance": np.diag(variances[label]).tolist(),
                    }
                ],
            }
            for label in STATE_LABELS
        },
        "rejection": {"min_confidence": 0.45, "min_log_likelihood": -30.0},
        "stimulation_anchors_ma": DEFAULT_ANCHORS_MA,
    }


def _log_gaussian(x: np.ndarray, mean: np.ndarray, covariance: np.ndarray) -> float:
    covariance = np.asarray(covariance, dtype=float)
    covariance = covariance + np.eye(x.size) * 1e-9
    sign, log_det = np.linalg.slogdet(covariance)
    if sign <= 0:
        covariance = np.eye(x.size)
        log_det = 0.0
    delta = x - mean
    quadratic = float(delta @ np.linalg.solve(covariance, delta))
    return -0.5 * (x.size * math.log(2 * math.pi) + log_det + quadratic)


def infer_state_probabilities(features: Sequence[float], model: dict[str, Any] | None = None) -> dict[str, Any]:
    model = model or default_model()
    x = np.asarray(features, dtype=float)
    if x.shape != (5,):
        raise ValueError("features must contain exactly five values")
    scaler = model["scaler"]
    x = (x - np.asarray(scaler["mean"], dtype=float)) / np.maximum(
        np.asarray(scaler["scale"], dtype=float),
        1e-9,
    )
    state_scores: list[float] = []
    for label in STATE_LABELS:
        state = model["states"][label]
        component_scores = []
        for component in state["components"]:
            component_scores.append(
                math.log(max(float(component["weight"]), 1e-12))
                + _log_gaussian(
                    x,
                    np.asarray(component["mean"], dtype=float),
                    np.asarray(component["covariance"], dtype=float),
                )
            )
        max_component = max(component_scores)
        mixture_log = max_component + math.log(
            sum(math.exp(value - max_component) for value in component_scores)
        )
        state_scores.append(math.log(max(float(state["prior"]), 1e-12)) + mixture_log)
    max_score = max(state_scores)
    likelihood_sum = sum(math.exp(value - max_score) for value in state_scores)
    log_likelihood = max_score + math.log(likelihood_sum)
    probabilities = np.asarray(
        [math.exp(value - log_likelihood) for value in state_scores],
        dtype=float,
    )
    top_index = int(np.argmax(probabilities))
    confidence = float(probabilities[top_index])
    rejection = model.get("rejection", {})
    rejected = confidence < float(rejection.get("min_confidence", 0.45)) or log_likelihood < float(
        rejection.get("min_log_likelihood", -30.0)
    )
    return {
        "probabilities": {
            label: float(probability) for label, probability in zip(STATE_LABELS, probabilities)
        },
        "top_state": STATE_LABELS[top_index],
        "confidence": confidence,
        "log_likelihood": float(log_likelihood),
        "rejected": rejected,
    }


def fit_gmm_model(
    features: np.ndarray,
    labels: Sequence[str],
    bands: dict[str, list[float]] | None = None,
    smoothing_windows: int = 30,
    probability_smoothing_windows: int = 5,
) -> dict:
    x = np.asarray(features, dtype=float)
    y = np.asarray(labels)
    if x.ndim != 2 or x.shape[1] != 5:
        raise ValueError("training features must have shape (n, 5)")
    missing = [state for state in STATE_LABELS if np.count_nonzero(y == state) < 2]
    if missing:
        raise ValueError(f"insufficient training samples for states: {missing}")
    mean = np.mean(x, axis=0)
    scale = np.std(x, axis=0)
    scale[scale < 1e-9] = 1.0
    normalized = (x - mean) / scale
    states: dict[str, Any] = {}
    state_log_likelihoods: list[float] = []
    for label in STATE_LABELS:
        state_x = normalized[y == label]
        components = min(2, max(1, state_x.shape[0] // 10))
        gmm = GaussianMixture(
            n_components=components,
            covariance_type="full",
            reg_covar=1e-3,
            max_iter=500,
            random_state=42,
        ).fit(state_x)
        state_log_likelihoods.extend(gmm.score_samples(state_x).tolist())
        states[label] = {
            "prior": float(state_x.shape[0] / normalized.shape[0]),
            "components": [
                {
                    "weight": float(gmm.weights_[index]),
                    "mean": gmm.means_[index].tolist(),
                    "covariance": gmm.covariances_[index].tolist(),
                }
                for index in range(components)
            ],
        }
    return {
        "schema_version": 1,
        "algorithm": "five_feature_gmm",
        "sample_rate_hz": 256,
        "window_seconds": 1,
        "smoothing_windows": smoothing_windows,
        "probability_smoothing_windows": probability_smoothing_windows,
        "feature_names": list(FEATURE_NAMES),
        "bands": bands or DEFAULT_BANDS,
        "scaler": {"mean": mean.tolist(), "scale": scale.tolist()},
        "states": states,
        "rejection": {
            "min_confidence": 0.45,
            "min_log_likelihood": float(np.percentile(state_log_likelihoods, 1)),
        },
        "stimulation_anchors_ma": DEFAULT_ANCHORS_MA,
    }


def compute_feedback_score(
    answers: Mapping[str, float],
    side_effects: Mapping[str, float] | None = None,
) -> dict[str, Any]:
    weights = {
        "tremor_relief": 0.25,
        "rigidity_relief": 0.20,
        "speech_fluency": 0.15,
        "movement_fluency": 0.20,
        "task_ease": 0.10,
        "parameter_preference": 0.10,
    }
    raw = sum(np.clip(float(answers.get(key, 0)), 0, 10) * weight for key, weight in weights.items()) * 10
    side_effects = side_effects or {}
    severities = [float(np.clip(value, 0, 10)) for value in side_effects.values()]
    penalty = min(45.0, sum(severities) * 5.0)
    blocked = max([0.0, *severities]) >= 7.0
    return {
        "raw_score": float(raw),
        "side_effect_penalty": float(penalty),
        "final_score": float(max(0.0, raw - penalty)),
        "blocked": blocked,
    }


def check_safety(
    candidate: Mapping[str, float],
    previous: Mapping[str, float] | None,
    bounds: Mapping[str, float],
) -> dict[str, Any]:
    reasons: list[str] = []
    checks = (
        ("current_ma", "current_min_ma", "current_max_ma"),
        ("pulse_width_us", "pulse_width_min_us", "pulse_width_max_us"),
        ("frequency_hz", "frequency_min_hz", "frequency_max_hz"),
    )
    for value_key, low_key, high_key in checks:
        if value_key not in candidate or not (float(bounds[low_key]) <= float(candidate[value_key]) <= float(bounds[high_key])):
            reasons.append(f"{value_key}_out_of_range")
    if previous is not None and "current_ma" in candidate and "current_ma" in previous:
        if abs(float(candidate["current_ma"]) - float(previous["current_ma"])) > float(
            bounds.get("max_delta_current_ma", 0.2)
        ):
            reasons.append("delta_current_too_large")
    return {"allowed": not reasons, "reasons": reasons, "candidate": dict(candidate), "bounds": dict(bounds)}


def probability_weighted_current(
    probabilities: Mapping[str, float],
    anchors: Mapping[str, float] = DEFAULT_ANCHORS_MA,
) -> float:
    total = sum(max(0.0, float(probabilities.get(state, 0.0))) for state in STATE_LABELS)
    if total <= 0:
        raise ValueError("at least one positive probability is required")
    return sum(
        max(0.0, float(probabilities.get(state, 0.0))) * float(anchors[state])
        for state in STATE_LABELS
    ) / total


def recommend_next_parameter(
    history: Iterable[Mapping[str, float]],
    bounds: Mapping[str, float],
    excluded_currents: Iterable[float] = (),
) -> dict[str, Any]:
    observations = list(history)
    low = float(bounds["current_min_ma"])
    high = float(bounds["current_max_ma"])
    grid = np.linspace(low, high, 201)
    excluded = [float(value) for value in excluded_currents]
    allowed = np.ones(grid.shape, dtype=bool)
    for value in excluded:
        allowed &= np.abs(grid - value) > 0.05
    if observations:
        previous = float(observations[-1]["current_ma"])
        allowed &= np.abs(grid - previous) <= float(bounds.get("max_delta_current_ma", 0.2)) + 1e-9
    if not np.any(allowed):
        allowed[:] = True
    if len(observations) < 2:
        target = float(observations[-1]["current_ma"]) if observations else (low + high) / 2
        index = int(np.argmin(np.where(allowed, np.abs(grid - target), np.inf)))
        current = float(grid[index])
        return {
            "current_ma": current,
            "expected_score": None,
            "uncertainty": None,
            "expected_improvement": None,
            "acquisition": "midpoint_bootstrap",
            "curve": {
                "grid_current_ma": grid.tolist(),
                "mean": [],
                "std": [],
                "expected_improvement": [],
                "best_score": max([row["score"] for row in observations], default=None),
            },
        }
    x = np.asarray([[float(row["current_ma"])] for row in observations])
    y = np.asarray([float(row["score"]) for row in observations])
    kernel = ConstantKernel(1.0, (0.1, 10.0)) * Matern(
        length_scale=0.45,
        length_scale_bounds=(0.08, 1.4),
        nu=2.5,
    ) + WhiteKernel(noise_level=1.0, noise_level_bounds=(0.01, 25.0))
    gp = GaussianProcessRegressor(
        kernel=kernel,
        normalize_y=True,
        n_restarts_optimizer=2,
        random_state=42,
    ).fit(x, y)
    mean, std = gp.predict(grid[:, None], return_std=True)
    best = float(np.max(y))
    improvement = mean - best - 0.05
    z = np.divide(improvement, std, out=np.zeros_like(improvement), where=std > 1e-9)
    expected_improvement = improvement * norm.cdf(z) + std * norm.pdf(z)
    expected_improvement[std <= 1e-9] = 0
    masked_ei = np.where(allowed, expected_improvement, -np.inf)
    index = int(np.argmax(masked_ei))
    return {
        "current_ma": float(grid[index]),
        "expected_score": float(mean[index]),
        "uncertainty": float(std[index]),
        "expected_improvement": float(expected_improvement[index]),
        "acquisition": "expected_improvement",
        "curve": {
            "grid_current_ma": grid.tolist(),
            "mean": mean.tolist(),
            "std": std.tolist(),
            "expected_improvement": expected_improvement.tolist(),
            "best_score": best,
        },
    }
