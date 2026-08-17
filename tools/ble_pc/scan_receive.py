"""Windows BLE central smoke test for the Ominidapt research simulator."""

from __future__ import annotations

import argparse
import asyncio
import json
import struct
import time
from dataclasses import dataclass

from bleak import BleakClient, BleakScanner

SERVICE = "8f3a0000-6f4d-4b2b-9a7e-1a0d9c2e1000"
DEVICE_INFO = "8f3a0001-6f4d-4b2b-9a7e-1a0d9c2e1000"
TELEMETRY = "8f3a0002-6f4d-4b2b-9a7e-1a0d9c2e1000"
LFP_STREAM = "8f3a0003-6f4d-4b2b-9a7e-1a0d9c2e1000"
COMMAND = "8f3a0004-6f4d-4b2b-9a7e-1a0d9c2e1000"
ACK = "8f3a0005-6f4d-4b2b-9a7e-1a0d9c2e1000"
IMPEDANCE = "8f3a0006-6f4d-4b2b-9a7e-1a0d9c2e1000"

MESSAGE_STREAM_CONTROL = 0x11
MESSAGE_ACK = 0x20
MESSAGE_LFP = 0x03


def crc16_ccitt(data: bytes) -> int:
    crc = 0xFFFF
    for value in data:
        crc ^= value << 8
        for _ in range(8):
            crc = ((crc << 1) ^ 0x1021) & 0xFFFF if crc & 0x8000 else (crc << 1) & 0xFFFF
    return crc


def encode_frame(message_type: int, sequence: int, payload: bytes) -> bytes:
    header = struct.pack(
        "<2sBBIQH",
        b"OP",
        2,
        message_type,
        sequence & 0xFFFFFFFF,
        int(time.time() * 1000),
        len(payload),
    )
    body = header + payload
    return body + struct.pack("<H", crc16_ccitt(body))


def decode_frame(data: bytes) -> tuple[int, int, int, bytes]:
    if len(data) < 20:
        raise ValueError("frame is shorter than 20 bytes")
    magic, version, message_type, sequence, timestamp_ms, payload_size = struct.unpack_from(
        "<2sBBIQH", data
    )
    if magic != b"OP" or version != 2:
        raise ValueError("invalid magic or protocol version")
    if len(data) != 18 + payload_size + 2:
        raise ValueError("payload length mismatch")
    expected_crc = struct.unpack_from("<H", data, len(data) - 2)[0]
    if crc16_ccitt(data[:-2]) != expected_crc:
        raise ValueError("CRC mismatch")
    return message_type, sequence, timestamp_ms, data[18:-2]


@dataclass
class Counters:
    lfp_frames: int = 0
    samples: int = 0
    sequence_gaps: int = 0
    crc_errors: int = 0
    last_sequence: int | None = None
    ack_received: bool = False


async def run(duration: float, scan_timeout: float) -> None:
    print(f"Scanning for Ominidapt service {SERVICE} ...")
    device = await BleakScanner.find_device_by_filter(
        lambda _device, advertisement: SERVICE.lower()
        in {value.lower() for value in advertisement.service_uuids},
        timeout=scan_timeout,
    )
    if device is None:
        raise RuntimeError("simulator was not found; start BLE peripheral mode on the tablet")

    print(f"Found {device.name or 'unnamed'} [{device.address}]")
    counters = Counters()

    def on_ack(_characteristic, data: bytearray) -> None:
        try:
            message_type, _, _, payload = decode_frame(bytes(data))
            if message_type != MESSAGE_ACK or len(payload) != 6:
                return
            acknowledged_sequence, success, status = struct.unpack("<IBB", payload)
            counters.ack_received = success == 1
            print(
                f"ACK command_sequence={acknowledged_sequence} "
                f"success={bool(success)} status={status}"
            )
        except ValueError as error:
            counters.crc_errors += 1
            print(f"Invalid ACK: {error}")

    def on_lfp(_characteristic, data: bytearray) -> None:
        try:
            message_type, sequence, _, payload = decode_frame(bytes(data))
            if message_type != MESSAGE_LFP:
                return
            sample_rate, channels, sample_count, state = struct.unpack_from("<HBBB", payload)
            expected_size = 5 + channels * sample_count * 2
            if len(payload) != expected_size:
                raise ValueError("LFP sample count mismatch")
            if counters.last_sequence is not None:
                delta = (sequence - counters.last_sequence) & 0xFFFFFFFF
                if delta > 1:
                    counters.sequence_gaps += delta - 1
            counters.last_sequence = sequence
            counters.lfp_frames += 1
            counters.samples += sample_count
            if counters.lfp_frames == 1:
                first_values = struct.unpack_from(
                    f"<{min(6, channels * sample_count)}h", payload, 5
                )
                print(
                    f"First LFP frame: {sample_rate} Hz, {channels} channels, "
                    f"{sample_count} samples/channel, state={state}, values={first_values}"
                )
        except ValueError as error:
            counters.crc_errors += 1
            print(f"Invalid LFP frame: {error}")

    async with BleakClient(device) as client:
        raw_info = await client.read_gatt_char(DEVICE_INFO)
        info = json.loads(bytes(raw_info).decode("utf-8"))
        if info.get("simulated") is not True or info.get("clinicalUse") is not False:
            raise RuntimeError(f"refusing non-simulator device: {info}")
        print(
            f"Connected serial={info['serial']} protocol={info['protocol']} "
            "simulated=true clinicalUse=false"
        )

        raw_telemetry = await client.read_gatt_char(TELEMETRY)
        _, _, _, telemetry = decode_frame(bytes(raw_telemetry))
        battery, flags = struct.unpack_from("<BB", telemetry)
        print(
            f"Telemetry battery={battery}% streaming={bool(flags & 1)} "
            f"alarm={bool(flags & 2)}"
        )

        await client.start_notify(ACK, on_ack)
        await client.start_notify(LFP_STREAM, on_lfp)
        await client.write_gatt_char(
            COMMAND,
            encode_frame(MESSAGE_STREAM_CONTROL, 1, b"\x01"),
            response=True,
        )
        await asyncio.sleep(duration)
        await client.write_gatt_char(
            COMMAND,
            encode_frame(MESSAGE_STREAM_CONTROL, 2, b"\x00"),
            response=True,
        )
        await asyncio.sleep(0.5)

    print(
        "Result: "
        f"ack={counters.ack_received}, frames={counters.lfp_frames}, "
        f"samples/channel={counters.samples}, sequence_gaps={counters.sequence_gaps}, "
        f"crc_errors={counters.crc_errors}"
    )
    if not counters.ack_received or counters.lfp_frames == 0 or counters.crc_errors:
        raise RuntimeError("BLE smoke test failed")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--duration", type=float, default=15.0)
    parser.add_argument("--scan-timeout", type=float, default=30.0)
    args = parser.parse_args()
    asyncio.run(run(args.duration, args.scan_timeout))


if __name__ == "__main__":
    main()
