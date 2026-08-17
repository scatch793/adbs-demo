"""Windows BLE peripheral implemented with WinRT GattServiceProvider."""

from __future__ import annotations

import asyncio
import json
import uuid
from collections.abc import Callable

from winrt.windows.devices.bluetooth.genericattributeprofile import (
    GattCharacteristicProperties,
    GattCommunicationStatus,
    GattLocalCharacteristicParameters,
    GattProtectionLevel,
    GattServiceProvider,
    GattServiceProviderAdvertisingParameters,
)
from winrt.windows.storage.streams import Buffer

from protocol import (
    ACK,
    COMMAND,
    DEVICE_INFO,
    IMPEDANCE,
    LFP_STREAM,
    SERVICE,
    TELEMETRY,
    fragment_frame,
)


def _buffer(data: bytes) -> Buffer:
    result = Buffer(len(data))
    result.length = len(data)
    memoryview(result)[:] = data
    return result


def _bytes(buffer: Buffer) -> bytes:
    return bytes(memoryview(buffer))


class WindowsGattPeripheral:
    def __init__(
        self,
        device_info: dict,
        telemetry_provider: Callable[[], bytes],
        impedance_provider: Callable[[], bytes],
        command_handler: Callable[[bytes], None],
        log: Callable[[str], None],
    ):
        self.device_info = device_info
        self.telemetry_provider = telemetry_provider
        self.impedance_provider = impedance_provider
        self.command_handler = command_handler
        self.log = log
        self.provider = None
        self.characteristics = {}
        self.advertising = False
        self.loop: asyncio.AbstractEventLoop | None = None

    async def initialize(self) -> None:
        # WinRT GATT callbacks may arrive on a system thread. Keep the qasync
        # loop so callback work can be marshalled back to the UI/asyncio thread.
        self.loop = asyncio.get_running_loop()
        result = await GattServiceProvider.create_async(uuid.UUID(SERVICE))
        if result.service_provider is None:
            raise RuntimeError(f"cannot create GATT service: {result.error}")
        self.provider = result.service_provider
        await self._create_read("device_info", DEVICE_INFO, lambda: json.dumps(
            self.device_info, ensure_ascii=False, separators=(",", ":")
        ).encode("utf-8"))
        await self._create_notify_read("telemetry", TELEMETRY, self.telemetry_provider)
        await self._create_notify("lfp", LFP_STREAM)
        await self._create_write("command", COMMAND)
        await self._create_notify("ack", ACK)
        await self._create_notify_read("impedance", IMPEDANCE, self.impedance_provider)
        self.provider.add_advertisement_status_changed(
            lambda _sender, args: self._emit_log(f"BLE广播状态：{args.status}")
        )

    def start(self) -> None:
        if self.provider is None:
            raise RuntimeError("GATT service is not initialized")
        parameters = GattServiceProviderAdvertisingParameters()
        parameters.is_connectable = True
        parameters.is_discoverable = True
        self.provider.start_advertising_with_parameters(parameters)
        self.advertising = True
        self._emit_log("已开始广播 Ominidapt 模拟刺激设备")

    def stop(self) -> None:
        if self.provider is not None:
            self.provider.stop_advertising()
        self.advertising = False
        self._emit_log("已停止BLE广播")

    @property
    def subscriber_count(self) -> int:
        characteristic = self.characteristics.get("lfp")
        return len(characteristic.subscribed_clients) if characteristic is not None else 0

    async def notify(self, name: str, frame: bytes, frame_id: int) -> int:
        characteristic = self.characteristics[name]
        clients = list(characteristic.subscribed_clients)
        sent = 0
        for client in clients:
            max_size = max(20, int(client.max_notification_size))
            for packet in fragment_frame(frame, max_size, frame_id):
                result = await characteristic.notify_value_for_subscribed_client_async(
                    _buffer(packet), client
                )
                if result.status == GattCommunicationStatus.SUCCESS:
                    sent += 1
        return sent

    async def _create_read(self, name: str, value: str, provider: Callable[[], bytes]) -> None:
        parameters = GattLocalCharacteristicParameters()
        parameters.characteristic_properties = GattCharacteristicProperties.READ
        parameters.read_protection_level = GattProtectionLevel.PLAIN
        result = await self.provider.service.create_characteristic_async(uuid.UUID(value), parameters)
        self._check_characteristic(result, name)
        characteristic = result.characteristic
        characteristic.add_read_requested(
            lambda _sender, args: self._queue_read(args, provider)
        )
        self.characteristics[name] = characteristic

    async def _create_notify_read(self, name: str, value: str, provider: Callable[[], bytes]) -> None:
        parameters = GattLocalCharacteristicParameters()
        parameters.characteristic_properties = (
            GattCharacteristicProperties.READ | GattCharacteristicProperties.NOTIFY
        )
        parameters.read_protection_level = GattProtectionLevel.PLAIN
        result = await self.provider.service.create_characteristic_async(uuid.UUID(value), parameters)
        self._check_characteristic(result, name)
        characteristic = result.characteristic
        characteristic.add_read_requested(
            lambda _sender, args: self._queue_read(args, provider)
        )
        characteristic.add_subscribed_clients_changed(
            lambda sender, _args: self._emit_log(
                f"{name}订阅客户端：{len(sender.subscribed_clients)}"
            )
        )
        self.characteristics[name] = characteristic

    async def _create_notify(self, name: str, value: str) -> None:
        parameters = GattLocalCharacteristicParameters()
        parameters.characteristic_properties = GattCharacteristicProperties.NOTIFY
        result = await self.provider.service.create_characteristic_async(uuid.UUID(value), parameters)
        self._check_characteristic(result, name)
        characteristic = result.characteristic
        characteristic.add_subscribed_clients_changed(
            lambda sender, _args: self._emit_log(
                f"{name}订阅客户端：{len(sender.subscribed_clients)}"
            )
        )
        self.characteristics[name] = characteristic

    async def _create_write(self, name: str, value: str) -> None:
        parameters = GattLocalCharacteristicParameters()
        parameters.characteristic_properties = (
            GattCharacteristicProperties.WRITE | GattCharacteristicProperties.WRITE_WITHOUT_RESPONSE
        )
        parameters.write_protection_level = GattProtectionLevel.PLAIN
        result = await self.provider.service.create_characteristic_async(uuid.UUID(value), parameters)
        self._check_characteristic(result, name)
        characteristic = result.characteristic
        characteristic.add_write_requested(
            lambda _sender, args: self._queue_write(args)
        )
        self.characteristics[name] = characteristic

    def _queue_read(self, args, provider: Callable[[], bytes]) -> None:
        deferral = args.get_deferral()
        self._schedule(lambda: self._serve_read(args, provider, deferral), deferral)

    def _queue_write(self, args) -> None:
        deferral = args.get_deferral()
        self._schedule(lambda: self._serve_write(args, deferral), deferral)

    def _schedule(self, coroutine_factory: Callable, deferral) -> None:
        loop = self.loop
        if loop is None or loop.is_closed():
            deferral.complete()
            return

        def start() -> None:
            loop.create_task(coroutine_factory())

        loop.call_soon_threadsafe(start)

    def _emit_log(self, message: str) -> None:
        loop = self.loop
        if loop is None or loop.is_closed():
            self.log(message)
            return
        try:
            on_loop = asyncio.get_running_loop() is loop
        except RuntimeError:
            on_loop = False
        if on_loop:
            self.log(message)
        else:
            loop.call_soon_threadsafe(self.log, message)

    async def _serve_read(self, args, provider: Callable[[], bytes], deferral) -> None:
        try:
            request = await args.get_request_async()
            if request is not None:
                payload = provider()
                request.respond_with_value(_buffer(payload))
                self._emit_log(f"GATT读取 {len(payload)}字节")
        except Exception as error:
            self._emit_log(f"GATT读取失败：{error}")
        finally:
            deferral.complete()

    async def _serve_write(self, args, deferral) -> None:
        try:
            request = await args.get_request_async()
            if request is not None:
                self.command_handler(_bytes(request.value))
                request.respond()
        except Exception as error:
            self._emit_log(f"命令解析失败：{error}")
        finally:
            deferral.complete()

    @staticmethod
    def _check_characteristic(result, name: str) -> None:
        if result.characteristic is None:
            raise RuntimeError(f"cannot create {name} characteristic: {result.error}")
