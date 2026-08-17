"""Binary protocol shared by the Windows simulator and Android application."""

from __future__ import annotations

import struct
import time
from dataclasses import dataclass

VERSION = 3
SERVICE = "8f3a0000-6f4d-4b2b-9a7e-1a0d9c2e1000"
DEVICE_INFO = "8f3a0001-6f4d-4b2b-9a7e-1a0d9c2e1000"
TELEMETRY = "8f3a0002-6f4d-4b2b-9a7e-1a0d9c2e1000"
LFP_STREAM = "8f3a0003-6f4d-4b2b-9a7e-1a0d9c2e1000"
COMMAND = "8f3a0004-6f4d-4b2b-9a7e-1a0d9c2e1000"
ACK = "8f3a0005-6f4d-4b2b-9a7e-1a0d9c2e1000"
IMPEDANCE = "8f3a0006-6f4d-4b2b-9a7e-1a0d9c2e1000"

DEVICE_INFO_MESSAGE = 0x01
TELEMETRY_MESSAGE = 0x02
LFP_MESSAGE = 0x03
QUERY_STATE = 0x10
STREAM_CONTROL = 0x11
SET_PARAMETERS = 0x12
HEARTBEAT = 0x13
SET_SCENARIO = 0x14
IMPEDANCE_MESSAGE = 0x15
MEASURE_IMPEDANCE = 0x16
ACK_MESSAGE = 0x20

STATE_OFF_REST = 0
STATE_OFF_MOVE = 1
STATE_ON_REST = 2
STATE_ON_MOVE = 3
STATE_CONTINUOUS = 4

FRAGMENT_HEADER_SIZE = 9


def crc16_ccitt(data: bytes) -> int:
    crc = 0xFFFF
    for value in data:
        crc ^= value << 8
        for _ in range(8):
            crc = ((crc << 1) ^ 0x1021) & 0xFFFF if crc & 0x8000 else (crc << 1) & 0xFFFF
    return crc


def encode_frame(message_type: int, sequence: int, payload: bytes, timestamp_ms: int | None = None) -> bytes:
    body = struct.pack(
        "<2sBBIQH",
        b"OP",
        VERSION,
        message_type,
        sequence & 0xFFFFFFFF,
        timestamp_ms if timestamp_ms is not None else int(time.time() * 1000),
        len(payload),
    ) + payload
    return body + struct.pack("<H", crc16_ccitt(body))


def decode_frame(data: bytes) -> tuple[int, int, int, bytes]:
    if len(data) < 20:
        raise ValueError("frame is shorter than 20 bytes")
    magic, version, message_type, sequence, timestamp_ms, payload_size = struct.unpack_from(
        "<2sBBIQH", data
    )
    if magic != b"OP":
        raise ValueError("invalid frame magic")
    if version != VERSION:
        raise ValueError(f"unsupported protocol version {version}")
    if len(data) != 18 + payload_size + 2:
        raise ValueError("payload length mismatch")
    expected_crc = struct.unpack_from("<H", data, len(data) - 2)[0]
    if crc16_ccitt(data[:-2]) != expected_crc:
        raise ValueError("CRC mismatch")
    return message_type, sequence, timestamp_ms, data[18:-2]


def fragment_frame(frame: bytes, max_notification_size: int, frame_id: int) -> list[bytes]:
    if len(frame) <= max_notification_size:
        return [frame]
    chunk_size = max_notification_size - FRAGMENT_HEADER_SIZE
    if chunk_size <= 0:
        raise ValueError("notification size is too small for fragment header")
    count = (len(frame) + chunk_size - 1) // chunk_size
    if count > 255:
        raise ValueError("logical frame needs more than 255 fragments")
    return [
        struct.pack("<2sBIBB", b"OF", VERSION, frame_id & 0xFFFFFFFF, index, count)
        + frame[index * chunk_size : (index + 1) * chunk_size]
        for index in range(count)
    ]


def encode_lfp(samples, sample_rate: int, state: int) -> bytes:
    """Encode an (N, 2) int16 numpy-compatible array."""
    rows, channels = samples.shape
    return struct.pack("<HBBB", sample_rate, channels, rows, state) + samples.astype("<i2").tobytes()


@dataclass(slots=True)
class StimulationParameters:
    current_ma: float = 1.5
    frequency_hz: int = 130
    pulse_width_us: int = 60
    duty_cycle: int = 50
    left_contact: int = 6
    right_contact: int = 2


def encode_parameters(value: StimulationParameters) -> bytes:
    return struct.pack(
        "<HHHBBB",
        max(0, min(65535, round(value.current_ma * 1000))),
        value.frequency_hz,
        value.pulse_width_us,
        value.duty_cycle,
        value.left_contact,
        value.right_contact,
    )


def decode_parameters(payload: bytes) -> StimulationParameters:
    if len(payload) != 9:
        raise ValueError("parameter payload must be 9 bytes")
    current, frequency, width, duty, left, right = struct.unpack("<HHHBBB", payload)
    return StimulationParameters(current / 1000.0, frequency, width, duty, left, right)


def encode_telemetry(
    battery: int,
    streaming: bool,
    alarm: bool,
    parameters: StimulationParameters,
    state: int = STATE_CONTINUOUS,
    medication_percent: int = 0,
    movement_percent: int = 0,
) -> bytes:
    flags = int(streaming) | (int(alarm) << 1)
    return (
        struct.pack("<BB", max(0, min(100, battery)), flags)
        + encode_parameters(parameters)
        + struct.pack(
            "<BBB",
            state & 0xFF,
            max(0, min(100, round(medication_percent))),
            max(0, min(100, round(movement_percent))),
        )
    )


def encode_ack(command_sequence: int, success: bool, status_code: int) -> bytes:
    return struct.pack("<IBB", command_sequence & 0xFFFFFFFF, int(success), status_code & 0xFF)


def decode_scenario(payload: bytes) -> tuple[int, int, int, int]:
    if len(payload) != 5:
        raise ValueError("scenario payload must be 5 bytes")
    return struct.unpack("<BBB H", payload)


def encode_impedance(
    measurement_sequence: int,
    readings: list[tuple[int, int, float, int]],
) -> bytes:
    if len(readings) > 255:
        raise ValueError("too many impedance readings")
    payload = bytearray(struct.pack("<IB", measurement_sequence & 0xFFFFFFFF, len(readings)))
    for left, right, kilo_ohms, quality in readings:
        payload += struct.pack(
            "<BBHB",
            left,
            right,
            max(0, min(65535, round(kilo_ohms * 100))),
            quality,
        )
    return bytes(payload)


def encode_impedance_request(pairs: list[tuple[int, int]]) -> bytes:
    if not 1 <= len(pairs) <= 8:
        raise ValueError("impedance request needs 1-8 pairs")
    payload = bytearray([len(pairs)])
    for first, second in pairs:
        if first not in range(1, 9) or second not in range(1, 9) or first == second:
            raise ValueError("invalid impedance contact pair")
        payload += struct.pack("<BB", first, second)
    return bytes(payload)


def decode_impedance_request(payload: bytes) -> list[tuple[int, int]]:
    if not payload:
        raise ValueError("impedance request is empty")
    count = payload[0]
    if count not in range(1, 9) or len(payload) != 1 + count * 2:
        raise ValueError("impedance request pair count mismatch")
    pairs = [struct.unpack_from("<BB", payload, 1 + index * 2) for index in range(count)]
    if any(first not in range(1, 9) or second not in range(1, 9) or first == second for first, second in pairs):
        raise ValueError("invalid impedance contact pair")
    return pairs
