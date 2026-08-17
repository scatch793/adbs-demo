"""P001 replay and explicitly non-clinical simulator response model."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np

from protocol import (
    STATE_CONTINUOUS,
    STATE_OFF_MOVE,
    STATE_OFF_REST,
    STATE_ON_MOVE,
    STATE_ON_REST,
    StimulationParameters,
)

STATE_FILES = {
    STATE_OFF_REST: "off_rest.npz",
    STATE_OFF_MOVE: "off_move.npz",
    STATE_ON_REST: "on_rest.npz",
    STATE_ON_MOVE: "on_move.npz",
}
STATE_LABELS = {
    STATE_OFF_REST: "OFF-Rest",
    STATE_OFF_MOVE: "OFF-Move",
    STATE_ON_REST: "ON-Rest",
    STATE_ON_MOVE: "ON-Move",
    STATE_CONTINUOUS: "Continuous",
}


@dataclass(slots=True)
class EngineSnapshot:
    medication_percent: float
    movement_percent: float
    simulated_response_percent: float
    symptom_percent: float
    state: int


class P001SignalEngine:
    sample_rate = 256
    channels = 2

    def __init__(self, data_dir: Path, seed: int = 1001):
        self.data_dir = Path(data_dir)
        self.rng = np.random.default_rng(seed)
        self.sources: dict[int, np.ndarray] = {}
        for state, file_name in STATE_FILES.items():
            path = self.data_dir / file_name
            with np.load(path, allow_pickle=False) as item:
                samples = np.asarray(item["samples"], dtype=np.int16)
                if samples.ndim != 2 or samples.shape[1] != 2:
                    raise ValueError(f"{path} must contain an (N, 2) int16 samples array")
                if int(item["sample_rate_hz"]) != self.sample_rate:
                    raise ValueError(f"{path} sample rate is not 256 Hz")
                self.sources[state] = samples
        self.cursors = {state: 0 for state in STATE_FILES}
        self.medication_percent = 0.0
        self.movement_percent = 0.0
        self.manual_state: int | None = STATE_OFF_REST
        self.closed_loop_enabled = True
        self.response = 0.0
        self.parameters = StimulationParameters()
        self.noise_percent = 1.0

    def set_scenario(
        self,
        state: int,
        medication_percent: float | None = None,
        movement_percent: float | None = None,
    ) -> None:
        self.manual_state = None if state == STATE_CONTINUOUS else state
        if medication_percent is None:
            medication_percent = 100.0 if state in (STATE_ON_REST, STATE_ON_MOVE) else 0.0
        if movement_percent is None:
            movement_percent = 80.0 if state in (STATE_OFF_MOVE, STATE_ON_MOVE) else 0.0
        self.medication_percent = float(np.clip(medication_percent, 0, 100))
        self.movement_percent = float(np.clip(movement_percent, 0, 100))

    def set_parameters(self, parameters: StimulationParameters) -> None:
        self.parameters = parameters

    def next_chunk(self, sample_count: int = 25) -> tuple[np.ndarray, EngineSnapshot]:
        med = self.medication_percent / 100.0
        move = self.movement_percent / 100.0
        weights = {
            STATE_OFF_REST: (1.0 - med) * (1.0 - move),
            STATE_OFF_MOVE: (1.0 - med) * move,
            STATE_ON_REST: med * (1.0 - move),
            STATE_ON_MOVE: med * move,
        }
        mixed = np.zeros((sample_count, 2), dtype=np.float64)
        for state, weight in weights.items():
            if weight <= 0:
                continue
            mixed += weight * self._take(state, sample_count).astype(np.float64)

        target_response = self._response_target() if self.closed_loop_enabled else 0.0
        self.response += (target_response - self.response) * 0.025
        # The response is intentionally a simple display model, not a claim
        # about neural or therapeutic effects.
        mixed *= 1.0 - 0.18 * self.response
        phase_start = sum(self.cursors.values()) / self.sample_rate
        t = phase_start + np.arange(sample_count) / self.sample_rate
        mixed -= (
            np.sin(2 * np.pi * 20.0 * t)[:, None]
            * (65.0 * self.response)
            * np.array([[1.0, 0.85]])
        )
        sigma = np.std(mixed, axis=0, keepdims=True) * (self.noise_percent / 100.0)
        mixed += self.rng.normal(0.0, np.maximum(sigma, 0.5), mixed.shape)
        output = np.clip(np.rint(mixed), -32768, 32767).astype(np.int16)

        state = self.manual_state
        if state is None:
            state = max(weights, key=weights.get)
        symptom = np.clip(self.movement_percent * (1.0 - 0.55 * self.response), 0, 100)
        return output, EngineSnapshot(
            self.medication_percent,
            self.movement_percent,
            self.response * 100.0,
            float(symptom),
            state,
        )

    def _take(self, state: int, count: int) -> np.ndarray:
        source = self.sources[state]
        cursor = self.cursors[state]
        if cursor + count <= len(source):
            result = source[cursor : cursor + count]
        else:
            first = source[cursor:]
            result = np.concatenate([first, source[: count - len(first)]])
        self.cursors[state] = (cursor + count) % len(source)
        return result

    def _response_target(self) -> float:
        p = self.parameters
        amplitude = np.clip((p.current_ma - 1.0) / 2.0, 0.0, 1.0)
        frequency = np.exp(-((p.frequency_hz - 130.0) / 22.0) ** 2)
        width = np.clip((p.pulse_width_us - 40.0) / 60.0, 0.0, 1.0)
        duty = np.clip(p.duty_cycle / 80.0, 0.0, 1.0)
        return float(np.clip(amplitude * frequency * (0.65 + 0.35 * width) * duty, 0.0, 1.0))
