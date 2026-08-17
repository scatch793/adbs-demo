from pathlib import Path

import numpy as np

from protocol import (
    LFP_MESSAGE,
    STATE_ON_MOVE,
    StimulationParameters,
    decode_frame,
    decode_impedance_request,
    encode_frame,
    encode_impedance,
    encode_impedance_request,
    encode_lfp,
    encode_telemetry,
    fragment_frame,
)
from signal_engine import P001SignalEngine


def test_protocol_frame_and_fragment_round_trip():
    samples = np.arange(50, dtype=np.int16).reshape(25, 2)
    frame = encode_frame(LFP_MESSAGE, 44, encode_lfp(samples, 256, STATE_ON_MOVE), 1234)
    packets = fragment_frame(frame, 20, 44)
    assert len(packets) > 1
    chunks = [packet[9:] for packet in packets]
    decoded = decode_frame(b"".join(chunks))
    assert decoded[0] == LFP_MESSAGE
    assert decoded[1] == 44


def test_p001_engine_blends_and_closed_loop_response():
    data_dir = Path(__file__).resolve().parents[2] / "private_data" / "p001"
    engine = P001SignalEngine(data_dir)
    engine.set_scenario(STATE_ON_MOVE, 100, 80)
    engine.set_parameters(StimulationParameters(3.0, 130, 90, 80, 6, 2))
    chunks = [engine.next_chunk() for _ in range(100)]
    assert all(chunk.shape == (25, 2) for chunk, _snapshot in chunks)
    assert chunks[-1][1].simulated_response_percent > 50


def test_telemetry_includes_ground_truth_controls():
    payload = encode_telemetry(
        92,
        True,
        False,
        StimulationParameters(),
        STATE_ON_MOVE,
        75,
        60,
    )
    assert len(payload) == 14
    assert payload[-3:] == bytes((STATE_ON_MOVE, 75, 60))


def test_v3_impedance_request_and_measurement_id_are_stable():
    pairs = [(6, 5), (2, 1)]
    assert decode_impedance_request(encode_impedance_request(pairs)) == pairs
    payload = encode_impedance(0x01020304, [(6, 5, 2.35, 0), (2, 1, 4.5, 1)])
    assert payload[:5] == b"\x04\x03\x02\x01\x02"
