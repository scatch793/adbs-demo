"""Visual Ominidapt simulated implanted-device peripheral for Windows."""

from __future__ import annotations

import argparse
import asyncio
import json
import sys
import time
from collections import deque
from pathlib import Path

import numpy as np
import pyqtgraph as pg
from PySide6 import QtCore, QtWidgets
from qasync import QEventLoop, asyncSlot

from protocol import (
    ACK_MESSAGE,
    HEARTBEAT,
    IMPEDANCE_MESSAGE,
    LFP_MESSAGE,
    MEASURE_IMPEDANCE,
    QUERY_STATE,
    SET_PARAMETERS,
    SET_SCENARIO,
    STATE_CONTINUOUS,
    STREAM_CONTROL,
    TELEMETRY_MESSAGE,
    StimulationParameters,
    decode_frame,
    decode_impedance_request,
    decode_parameters,
    decode_scenario,
    encode_ack,
    encode_frame,
    encode_impedance,
    encode_lfp,
    encode_telemetry,
)
from signal_engine import P001SignalEngine, STATE_LABELS
from winrt_gatt import WindowsGattPeripheral


def find_data_dir(explicit: str | None) -> Path:
    candidates = []
    if explicit:
        candidates.append(Path(explicit))
    if getattr(sys, "frozen", False):
        candidates.append(Path(sys.executable).parent / "data" / "p001")
    candidates.extend(
        [
            Path(__file__).resolve().parents[2] / "private_data" / "p001",
            Path.cwd() / "private_data" / "p001",
        ]
    )
    for candidate in candidates:
        if (candidate / "off_rest.npz").exists():
            return candidate
    raise FileNotFoundError("找不到脱敏P001数据；请使用 --data-dir 指定 private_data/p001")


class SimulatorWindow(QtWidgets.QMainWindow):
    def __init__(self, data_dir: Path):
        super().__init__()
        self.setWindowTitle("Ominidapt PD 模拟植入刺激设备（仅科研演示）")
        self.resize(1350, 850)
        self.engine = P001SignalEngine(data_dir)
        self.sequence = 1
        self.streaming = False
        self.battery = 92
        self.alarm = False
        self.drop_percent = 0
        self.crc_error_percent = 0
        self.ack_mode = "正常"
        self.history = deque(maxlen=2560)
        self.last_snapshot = None
        self.gatt = WindowsGattPeripheral(
            device_info={
                "serialNumber": "SIM-PC-P001",
                "name": "OP-PC",
                "firmwareVersion": "3",
                "protocolVersion": 3,
                "sampleRateHz": 256,
                "channelCount": 2,
                "contacts": list(range(1, 9)),
                # Compact capability tokens keep the structured safety record
                # within one MTU-247 read. L=LFP, I=impedance, P=parameters.
                "capabilities": ["LFP", "I", "P"],
                "safetyRuleVersion": "1",
                "simulated": True,
                "clinicalUse": False,
            },
            telemetry_provider=self._telemetry_frame,
            impedance_provider=self._impedance_frame,
            command_handler=self._on_command,
            log=self.log,
        )
        self._build_ui()
        self.timer = QtCore.QTimer(self)
        self.timer.setInterval(98)
        self.timer.timeout.connect(self._tick)
        self.chart_timer = QtCore.QTimer(self)
        self.chart_timer.setInterval(250)
        self.chart_timer.timeout.connect(self._update_charts)

    def _build_ui(self) -> None:
        root = QtWidgets.QWidget()
        self.setCentralWidget(root)
        layout = QtWidgets.QHBoxLayout(root)
        controls = QtWidgets.QScrollArea()
        controls.setWidgetResizable(True)
        panel = QtWidgets.QWidget()
        form = QtWidgets.QVBoxLayout(panel)
        controls.setWidget(panel)
        controls.setFixedWidth(430)
        layout.addWidget(controls)

        warning = QtWidgets.QLabel("仅限脱敏科研演示 · 不连接或控制真实植入设备")
        warning.setStyleSheet("color:#b71c1c;font-weight:700;padding:8px;background:#ffebee")
        warning.setWordWrap(True)
        form.addWidget(warning)

        self.broadcast_button = QtWidgets.QPushButton("开始BLE广播")
        self.broadcast_button.clicked.connect(self.toggle_broadcast)
        form.addWidget(self.broadcast_button)
        self.connection_label = QtWidgets.QLabel("广播未启动 · 客户端 0")
        form.addWidget(self.connection_label)

        scenario_group = QtWidgets.QGroupBox("患者状态")
        scenario = QtWidgets.QVBoxLayout(scenario_group)
        self.state_combo = QtWidgets.QComboBox()
        for code in range(5):
            self.state_combo.addItem(STATE_LABELS[code], code)
        self.state_combo.currentIndexChanged.connect(self._state_changed)
        scenario.addWidget(self.state_combo)
        self.medication = self._slider(scenario, "药物效应", self._medication_changed)
        self.movement = self._slider(scenario, "运动强度", self._movement_changed)
        presets = QtWidgets.QHBoxLayout()
        for label, value in [("静息", 0), ("手指", 30), ("起坐", 60), ("步行", 85)]:
            button = QtWidgets.QPushButton(label)
            button.clicked.connect(lambda _checked=False, x=value: self.movement.setValue(x))
            presets.addWidget(button)
        scenario.addLayout(presets)
        dose = QtWidgets.QPushButton("模拟服药")
        dose.clicked.connect(self._take_dose)
        scenario.addWidget(dose)
        self.closed_loop = QtWidgets.QCheckBox("启用仅模拟的闭环响应")
        self.closed_loop.setChecked(True)
        self.closed_loop.toggled.connect(lambda checked: setattr(self.engine, "closed_loop_enabled", checked))
        scenario.addWidget(self.closed_loop)
        self.ground_truth = QtWidgets.QLabel()
        scenario.addWidget(self.ground_truth)
        form.addWidget(scenario_group)

        device_group = QtWidgets.QGroupBox("模拟设备")
        device_form = QtWidgets.QFormLayout(device_group)
        self.battery_spin = QtWidgets.QSpinBox()
        self.battery_spin.setRange(0, 100)
        self.battery_spin.setValue(self.battery)
        self.battery_spin.valueChanged.connect(lambda x: setattr(self, "battery", x))
        device_form.addRow("电量 %", self.battery_spin)
        self.alarm_check = QtWidgets.QCheckBox("设备告警")
        self.alarm_check.toggled.connect(lambda x: setattr(self, "alarm", x))
        device_form.addRow(self.alarm_check)
        self.parameter_label = QtWidgets.QLabel()
        self._update_parameter_label()
        device_form.addRow("刺激参数", self.parameter_label)
        self.impedance_label = QtWidgets.QLabel("等待平板选择电极对并发起测量")
        self.impedance_label.setWordWrap(True)
        device_form.addRow("最近阻抗", self.impedance_label)
        form.addWidget(device_group)

        impedance_group = QtWidgets.QGroupBox("电极与阻抗")
        impedance_layout = QtWidgets.QVBoxLayout(impedance_group)
        impedance_layout.addWidget(QtWidgets.QLabel("逐触点界面阻抗（kΩ）"))
        grid = QtWidgets.QGridLayout()
        self.contact_impedance = {}
        self.contact_fault = {}
        defaults = [1.05, 1.20, 1.12, 1.35, 1.08, 1.27, 1.16, 1.31]
        for index, value in enumerate(defaults, start=1):
            label = QtWidgets.QLabel(f"C{index}")
            spin = QtWidgets.QDoubleSpinBox()
            spin.setRange(0.05, 20.0)
            spin.setDecimals(2)
            spin.setSingleStep(0.05)
            spin.setValue(value)
            fault = QtWidgets.QComboBox()
            fault.addItems(["正常", "接触不良", "开路", "短路"])
            grid.addWidget(label, index - 1, 0)
            grid.addWidget(spin, index - 1, 1)
            grid.addWidget(fault, index - 1, 2)
            self.contact_impedance[index] = spin
            self.contact_fault[index] = fault
        impedance_layout.addLayout(grid)
        measure_form = QtWidgets.QFormLayout()
        self.impedance_noise = QtWidgets.QDoubleSpinBox()
        self.impedance_noise.setRange(0.0, 1.0)
        self.impedance_noise.setDecimals(2)
        self.impedance_noise.setValue(0.03)
        measure_form.addRow("测量噪声 σ(kΩ)", self.impedance_noise)
        self.impedance_drift = QtWidgets.QDoubleSpinBox()
        self.impedance_drift.setRange(0.0, 1.0)
        self.impedance_drift.setDecimals(2)
        self.impedance_drift.setValue(0.04)
        measure_form.addRow("慢漂移幅度(kΩ)", self.impedance_drift)
        self.impedance_delay = QtWidgets.QSpinBox()
        self.impedance_delay.setRange(100, 5000)
        self.impedance_delay.setValue(650)
        measure_form.addRow("测量延时(ms)", self.impedance_delay)
        impedance_layout.addLayout(measure_form)
        form.addWidget(impedance_group)

        fault_group = QtWidgets.QGroupBox("故障注入")
        fault_form = QtWidgets.QFormLayout(fault_group)
        self.drop_spin = QtWidgets.QSpinBox()
        self.drop_spin.setRange(0, 100)
        self.drop_spin.valueChanged.connect(lambda x: setattr(self, "drop_percent", x))
        fault_form.addRow("丢包 %", self.drop_spin)
        self.crc_spin = QtWidgets.QSpinBox()
        self.crc_spin.setRange(0, 100)
        self.crc_spin.valueChanged.connect(lambda x: setattr(self, "crc_error_percent", x))
        fault_form.addRow("CRC错误 %", self.crc_spin)
        self.ack_combo = QtWidgets.QComboBox()
        self.ack_combo.addItems(["正常", "拒绝", "丢弃"])
        self.ack_combo.currentTextChanged.connect(lambda x: setattr(self, "ack_mode", x))
        fault_form.addRow("ACK", self.ack_combo)
        disconnect = QtWidgets.QPushButton("停止广播/模拟断连")
        disconnect.clicked.connect(self._force_disconnect)
        fault_form.addRow(disconnect)
        form.addWidget(fault_group)

        self.stats = QtWidgets.QLabel("序号 0 · 通知 0 · 命令 0")
        form.addWidget(self.stats)
        self.log_view = QtWidgets.QPlainTextEdit()
        self.log_view.setReadOnly(True)
        self.log_view.setMaximumBlockCount(300)
        form.addWidget(self.log_view, 1)

        charts = QtWidgets.QVBoxLayout()
        layout.addLayout(charts, 1)
        self.lfp_plot = pg.PlotWidget(title="双通道 LFP（最近10秒）")
        self.lfp_plot.addLegend()
        self.lfp_plot.showGrid(x=True, y=True, alpha=0.2)
        self.ch1 = self.lfp_plot.plot(pen="#1565c0", name="CH1")
        self.ch2 = self.lfp_plot.plot(pen="#ef6c00", name="CH2")
        charts.addWidget(self.lfp_plot, 2)
        self.psd_plot = pg.PlotWidget(title="功率谱密度")
        self.psd_plot.setXRange(2, 100)
        self.psd_plot.setLogMode(False, True)
        self.psd_curve = self.psd_plot.plot(pen="#6a1b9a")
        charts.addWidget(self.psd_plot, 1)
        self.effect_plot = pg.PlotWidget(title="药物 / 运动 / 模拟刺激响应")
        self.effect_plot.setYRange(0, 100)
        self.effect_med = self.effect_plot.plot(pen="#2e7d32")
        self.effect_move = self.effect_plot.plot(pen="#c62828")
        self.effect_stim = self.effect_plot.plot(pen="#00838f")
        self.effect_history = deque(maxlen=300)
        charts.addWidget(self.effect_plot, 1)
        self.notifications = 0
        self.commands = 0
        form.addStretch()

    @staticmethod
    def _slider(layout, label, callback):
        row = QtWidgets.QHBoxLayout()
        row.addWidget(QtWidgets.QLabel(label))
        slider = QtWidgets.QSlider(QtCore.Qt.Orientation.Horizontal)
        slider.setRange(0, 100)
        slider.valueChanged.connect(callback)
        row.addWidget(slider)
        value = QtWidgets.QLabel("0%")
        slider.valueChanged.connect(lambda x: value.setText(f"{x}%"))
        row.addWidget(value)
        layout.addLayout(row)
        return slider

    @asyncSlot()
    async def toggle_broadcast(self) -> None:
        try:
            if self.gatt.provider is None:
                await self.gatt.initialize()
            if self.gatt.advertising:
                self.gatt.stop()
                self.broadcast_button.setText("开始BLE广播")
                self.timer.stop()
                self.chart_timer.stop()
            else:
                self.gatt.start()
                self.broadcast_button.setText("停止BLE广播")
                self.timer.start()
                self.chart_timer.start()
        except Exception as error:
            self.log(f"启动失败：{error}")
            QtWidgets.QMessageBox.critical(self, "BLE启动失败", str(error))

    def _state_changed(self) -> None:
        state = int(self.state_combo.currentData())
        self.engine.set_scenario(state)
        self.medication.blockSignals(True)
        self.movement.blockSignals(True)
        self.medication.setValue(round(self.engine.medication_percent))
        self.movement.setValue(round(self.engine.movement_percent))
        self.medication.blockSignals(False)
        self.movement.blockSignals(False)
        self.log(f"场景切换：{STATE_LABELS[state]}")

    def _medication_changed(self, value: int) -> None:
        self.engine.medication_percent = value
        self.engine.manual_state = None
        self.state_combo.blockSignals(True)
        self.state_combo.setCurrentIndex(STATE_CONTINUOUS)
        self.state_combo.blockSignals(False)

    def _movement_changed(self, value: int) -> None:
        self.engine.movement_percent = value
        self.engine.manual_state = None
        self.state_combo.blockSignals(True)
        self.state_combo.setCurrentIndex(STATE_CONTINUOUS)
        self.state_combo.blockSignals(False)

    def _take_dose(self) -> None:
        self.log("开始模拟服药起效")
        self._dose_target = 100
        if not hasattr(self, "_dose_timer"):
            self._dose_timer = QtCore.QTimer(self)
            self._dose_timer.setInterval(500)
            self._dose_timer.timeout.connect(self._dose_step)
        self._dose_timer.start()

    def _dose_step(self) -> None:
        value = min(100, self.medication.value() + 2)
        self.medication.setValue(value)
        if value >= 100:
            self._dose_timer.stop()

    def _force_disconnect(self) -> None:
        self.gatt.stop()
        self.broadcast_button.setText("开始BLE广播")
        self.log("已主动停止广播；平板应进入重连状态")

    def _on_command(self, raw: bytes) -> None:
        try:
            message_type, sequence, _timestamp, payload = decode_frame(raw)
            self.commands += 1
            success, status = True, 0
            if message_type == STREAM_CONTROL:
                self.streaming = bool(payload and payload[0])
                self.log(f"数据流 {'开启' if self.streaming else '关闭'}，命令#{sequence}")
            elif message_type == SET_PARAMETERS:
                parameters = decode_parameters(payload)
                success = (
                    1.0 <= parameters.current_ma <= 3.0
                    and 120 <= parameters.frequency_hz <= 150
                    and 50 <= parameters.pulse_width_us <= 90
                    and 20 <= parameters.duty_cycle <= 80
                )
                status = 0 if success else 2
                if success:
                    self.engine.set_parameters(parameters)
                    self._update_parameter_label()
                self.log(f"参数命令#{sequence}：{'接受' if success else '越界拒绝'} {parameters}")
            elif message_type == SET_SCENARIO:
                state, medication, movement, _transition = decode_scenario(payload)
                self.engine.set_scenario(state, medication, movement)
                self.state_combo.setCurrentIndex(state)
                self.log(f"平板请求场景：{STATE_LABELS.get(state, state)}")
            elif message_type == MEASURE_IMPEDANCE:
                pairs = decode_impedance_request(payload)
                self.log(
                    f"阻抗测量#{sequence}：" +
                    " / ".join(f"C{first}-C{second}" for first, second in pairs)
                )
                asyncio.create_task(self._send_impedance(sequence, pairs))
            elif message_type in (QUERY_STATE, HEARTBEAT):
                pass
            else:
                success, status = False, 3
            if self.ack_mode == "拒绝":
                success, status = False, 4
            if self.ack_mode != "丢弃":
                asyncio.create_task(self._send_ack(sequence, success, status))
            else:
                self.log(f"故障注入：丢弃命令#{sequence} ACK")
        except Exception as error:
            self.log(f"无效命令：{error}")

    async def _send_ack(self, sequence: int, success: bool, status: int) -> None:
        frame_sequence = self._next_sequence()
        frame = encode_frame(ACK_MESSAGE, frame_sequence, encode_ack(sequence, success, status))
        self.notifications += await self.gatt.notify("ack", frame, frame_sequence)

    async def _send_impedance(self, measurement_sequence: int, pairs: list[tuple[int, int]]) -> None:
        await asyncio.sleep(self.impedance_delay.value() / 1000.0)
        readings = [self._calculate_pair_impedance(first, second) for first, second in pairs]
        frame_sequence = self._next_sequence()
        frame = encode_frame(
            IMPEDANCE_MESSAGE,
            frame_sequence,
            encode_impedance(measurement_sequence, readings),
        )
        self.notifications += await self.gatt.notify("impedance", frame, frame_sequence)
        self.impedance_label.setText(
            " / ".join(
                f"C{first}-C{second} {value:.2f} kΩ ({self._quality_label(quality)})"
                for first, second, value, quality in readings
            )
        )

    def _calculate_pair_impedance(self, first: int, second: int) -> tuple[int, int, float, int]:
        faults = {self.contact_fault[first].currentText(), self.contact_fault[second].currentText()}
        if "开路" in faults:
            return first, second, 20.0, 2
        if "短路" in faults:
            return first, second, 0.08, 3
        base = self.contact_impedance[first].value() + self.contact_impedance[second].value() + 0.15
        drift = self.impedance_drift.value() * np.sin(time.monotonic() / 8.0 + first + second)
        noise = float(np.random.normal(0.0, self.impedance_noise.value()))
        value = max(0.01, base + drift + noise)
        quality = 1 if "接触不良" in faults or value > 5.0 else 0
        return first, second, value, quality

    @staticmethod
    def _quality_label(quality: int) -> str:
        return {0: "良好", 1: "需复核", 2: "开路", 3: "短路"}.get(quality, "未知")

    def _telemetry_frame(self) -> bytes:
        snapshot = self.last_snapshot
        state = snapshot.state if snapshot is not None else (
            self.engine.manual_state if self.engine.manual_state is not None else STATE_CONTINUOUS
        )
        medication = snapshot.medication_percent if snapshot is not None else self.engine.medication_percent
        movement = snapshot.movement_percent if snapshot is not None else self.engine.movement_percent
        sequence = self._next_sequence()
        return encode_frame(
            TELEMETRY_MESSAGE,
            sequence,
            encode_telemetry(
                self.battery,
                self.streaming,
                self.alarm,
                self.engine.parameters,
                state,
                medication,
                movement,
            ),
        )

    def _impedance_frame(self) -> bytes:
        sequence = self._next_sequence()
        payload = encode_impedance(
            0,
            [
                self._calculate_pair_impedance(6, 5),
                self._calculate_pair_impedance(2, 1),
            ],
        )
        return encode_frame(IMPEDANCE_MESSAGE, sequence, payload)

    def _next_sequence(self) -> int:
        value = self.sequence
        self.sequence = (self.sequence + 1) & 0xFFFFFFFF
        return value

    @asyncSlot()
    async def _tick(self) -> None:
        samples, snapshot = self.engine.next_chunk()
        self.last_snapshot = snapshot
        self.history.extend(samples.tolist())
        self.effect_history.append(
            (snapshot.medication_percent, snapshot.movement_percent, snapshot.simulated_response_percent)
        )
        self.ground_truth.setText(
            f"真值：{STATE_LABELS[snapshot.state]} · 模拟症状 {snapshot.symptom_percent:.0f}%"
        )
        self.connection_label.setText(
            f"{'正在广播' if self.gatt.advertising else '广播未启动'} · "
            f"客户端 {self.gatt.subscriber_count} · "
            f"{'数据流开启' if self.streaming else '等待平板开启数据流'}"
        )
        if not self.streaming or self.gatt.subscriber_count == 0:
            return
        if np.random.random() * 100 < self.drop_percent:
            self.sequence = (self.sequence + 1) & 0xFFFFFFFF
            return
        sequence = self._next_sequence()
        frame = bytearray(
            encode_frame(LFP_MESSAGE, sequence, encode_lfp(samples, 256, snapshot.state))
        )
        if self.crc_error_percent and np.random.random() * 100 < self.crc_error_percent:
            frame[-1] ^= 0x5A
        self.notifications += await self.gatt.notify("lfp", bytes(frame), sequence)
        if sequence % 10 == 0:
            telemetry_sequence = self._next_sequence()
            telemetry = encode_frame(
                TELEMETRY_MESSAGE,
                telemetry_sequence,
                encode_telemetry(
                    self.battery,
                    self.streaming,
                    self.alarm,
                    self.engine.parameters,
                    snapshot.state,
                    snapshot.medication_percent,
                    snapshot.movement_percent,
                ),
            )
            self.notifications += await self.gatt.notify(
                "telemetry", telemetry, telemetry_sequence
            )
        self.stats.setText(
            f"序号 {sequence} · 通知 {self.notifications} · 命令 {self.commands}"
        )

    def _update_charts(self) -> None:
        if not self.history:
            return
        data = np.asarray(self.history, dtype=float)
        x = np.arange(len(data)) / 256.0
        self.ch1.setData(x, data[:, 0])
        self.ch2.setData(x, data[:, 1])
        if len(data) >= 256:
            window = data[-512:, 0] - np.mean(data[-512:, 0])
            spectrum = np.abs(np.fft.rfft(window * np.hanning(len(window)))) ** 2
            frequencies = np.fft.rfftfreq(len(window), 1 / 256.0)
            mask = (frequencies >= 2) & (frequencies <= 100)
            self.psd_curve.setData(frequencies[mask], np.maximum(spectrum[mask], 1e-6))
        effects = np.asarray(self.effect_history)
        if len(effects):
            t = np.arange(len(effects)) / 4.0
            self.effect_med.setData(t, effects[:, 0])
            self.effect_move.setData(t, effects[:, 1])
            self.effect_stim.setData(t, effects[:, 2])

    def _update_parameter_label(self) -> None:
        p = self.engine.parameters
        self.parameter_label.setText(
            f"{p.current_ma:.2f} mA / {p.frequency_hz} Hz / "
            f"{p.pulse_width_us} μs / {p.duty_cycle}% / C{p.left_contact}-C{p.right_contact}"
        )

    def log(self, message: str) -> None:
        stamp = time.strftime("%H:%M:%S")
        self.log_view.appendPlainText(f"[{stamp}] {message}")

    def closeEvent(self, event) -> None:
        self.gatt.stop()
        super().closeEvent(event)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir")
    args = parser.parse_args()
    app = QtWidgets.QApplication(sys.argv)
    app.setStyle("Fusion")
    loop = QEventLoop(app)
    asyncio.set_event_loop(loop)
    window = SimulatorWindow(find_data_dir(args.data_dir))
    window.show()
    QtCore.QTimer.singleShot(350, window.broadcast_button.click)
    with loop:
        loop.run_forever()


if __name__ == "__main__":
    main()
