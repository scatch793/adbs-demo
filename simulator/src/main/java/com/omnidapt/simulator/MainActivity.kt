package com.omnidapt.simulator

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnidapt.protocol.SimulatedState

class MainActivity : ComponentActivity() {
    private lateinit var controller: BlePeripheralController
    private lateinit var replaySource: ReplaySource

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            controller.startPeripheral()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        replaySource = ReplaySource(assets)
        controller = BlePeripheralController(this, replaySource)
        setContent {
            MaterialTheme {
                SimulatorScreen(controller, replaySource, ::ensurePermissionsAndStart)
            }
        }
    }

    override fun onDestroy() {
        controller.stopPeripheral()
        super.onDestroy()
    }

    private fun ensurePermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !controller.hasPermissions()) {
            requestPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ),
            )
        } else {
            controller.startPeripheral()
        }
    }
}

@Composable
private fun SimulatorScreen(
    controller: BlePeripheralController,
    replaySource: ReplaySource,
    onStartPeripheral: () -> Unit,
) {
    val snapshot by controller.snapshot.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Ominidapt BLE 设备模拟器",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "仅用于脱敏科研演示；不会连接或控制植入式设备。",
            color = Color(0xFFB3261E),
        )
        StatusCard(snapshot, replaySource)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = if (snapshot.advertising) controller::stopPeripheral else onStartPeripheral,
            ) {
                Text(if (snapshot.advertising) "停止 BLE 外设" else "启动 BLE 外设")
            }
            OutlinedButton(
                onClick = if (snapshot.streaming) controller::stopStreaming else controller::startStreaming,
                enabled = snapshot.advertising,
            ) {
                Text(if (snapshot.streaming) "停止 LFP" else "开始 LFP")
            }
            OutlinedButton(
                onClick = controller::disconnectAll,
                enabled = snapshot.connectedDevices > 0,
            ) {
                Text("主动断连")
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7FB))) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("回放状态", fontWeight = FontWeight.Bold)
                SimulatedState.entries.forEach { state ->
                    FilterChip(
                        selected = snapshot.state == state,
                        onClick = { controller.setState(state) },
                        label = {
                            Text(
                                "${state.label}${if (replaySource.usesRealReplay(state)) " · P001" else " · 合成回退"}",
                            )
                        },
                    )
                }
                Text("回放速度 ${"%.1f".format(snapshot.speed)}×")
                Slider(
                    value = snapshot.speed,
                    onValueChange = controller::setSpeed,
                    valueRange = 0.5f..2f,
                    steps = 2,
                )
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E8))) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("故障注入", fontWeight = FontWeight.Bold)
                Text("丢包率 ${snapshot.faults.packetLossPercent}%")
                Slider(
                    value = snapshot.faults.packetLossPercent.toFloat(),
                    onValueChange = {
                        controller.setFaults(
                            snapshot.faults.copy(packetLossPercent = it.toInt()),
                        )
                    },
                    valueRange = 0f..30f,
                )
                Text("附加延迟 ${snapshot.faults.latencyMs} ms")
                Slider(
                    value = snapshot.faults.latencyMs.toFloat(),
                    onValueChange = {
                        controller.setFaults(snapshot.faults.copy(latencyMs = it.toInt()))
                    },
                    valueRange = 0f..1000f,
                )
                FaultSwitch("下一帧 CRC 错误", snapshot.faults.corruptNextFrame) {
                    controller.setFaults(snapshot.faults.copy(corruptNextFrame = it))
                }
                FaultSwitch("拒绝参数命令", snapshot.faults.rejectCommands) {
                    controller.setFaults(snapshot.faults.copy(rejectCommands = it))
                }
                FaultSwitch("丢弃参数 ACK（超时测试）", snapshot.faults.dropAcks) {
                    controller.setFaults(snapshot.faults.copy(dropAcks = it))
                }
                FaultSwitch("低电量 8%", snapshot.faults.lowBattery) {
                    controller.setFaults(snapshot.faults.copy(lowBattery = it))
                }
                FaultSwitch("设备告警", snapshot.faults.alarm) {
                    controller.setFaults(snapshot.faults.copy(alarm = it))
                }
            }
        }
    }
}

@Composable
private fun StatusCard(snapshot: SimulatorSnapshot, replaySource: ReplaySource) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("设备：SIM-P001-001", fontWeight = FontWeight.Bold)
            Text("BLE 广播：${if (snapshot.advertising) "运行中" else "已停止"}")
            Text("连接设备：${snapshot.connectedDevices}")
            Text("LFP：${if (snapshot.streaming) "发送中" else "未发送"} · 序号 ${snapshot.sequence}")
            Text("已发送 ${snapshot.sentFrames} 帧 · 主动丢弃 ${snapshot.droppedFrames} 帧")
            Text(
                "当前参数：${snapshot.parameters.currentMa} mA / " +
                    "${snapshot.parameters.frequencyHz} Hz / " +
                    "${snapshot.parameters.pulseWidthUs} μs",
            )
            Text("最近命令：${snapshot.lastCommand}")
            Text(
                "数据源：${if (replaySource.usesRealReplay(snapshot.state)) "脱敏 P001" else "合成回退"}",
            )
            snapshot.lastError?.let { Text(it, color = Color(0xFFB3261E)) }
        }
    }
}

@Composable
private fun FaultSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
