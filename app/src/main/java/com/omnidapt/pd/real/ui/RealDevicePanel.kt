package com.omnidapt.pd.real.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnidapt.pd.real.ble.BleCentralClient
import com.omnidapt.pd.real.ble.BleLinkState
import com.omnidapt.pd.real.ble.DeviceCommandDispatcher
import com.omnidapt.pd.real.algorithm.EdgeInferenceController
import com.omnidapt.pd.real.RealRepository
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun RealDevicePanel(
    client: BleCentralClient,
    inference: EdgeInferenceController? = null,
    dispatcher: DeviceCommandDispatcher? = null,
    repository: RealRepository? = null,
    patientId: String? = null,
) {
    val scope = rememberCoroutineScope()
    var recording by remember { mutableStateOf(false) }
    var recordingStatus by remember { mutableStateOf<String?>(null) }
    val snapshot by client.snapshot.collectAsState()
    val recentSamples by client.recentSamples.collectAsState()
    val inferenceSnapshot = inference?.snapshot?.collectAsState()?.value
    val dispatchSnapshot = dispatcher?.snapshot?.collectAsState()?.value
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.all { it }) client.connect()
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("科研模拟设备", style = MaterialTheme.typography.titleMedium)
            Text(
                when (snapshot.linkState) {
                    BleLinkState.IDLE -> "未连接"
                    BleLinkState.SCANNING -> "正在扫描固定模拟服务 UUID"
                    BleLinkState.CONNECTING -> "正在建立 BLE 连接"
                    BleLinkState.VERIFYING -> "正在验证 simulated=true"
                    BleLinkState.CONNECTED -> "已验证：${snapshot.deviceName}"
                    BleLinkState.RECONNECTING -> "连接中断，正在重连"
                },
                color = if (snapshot.verifiedSimulator) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("电量 ${snapshot.batteryPercent?.let { "$it%" } ?: "—"}")
                Text("MTU ${snapshot.negotiatedMtu}")
                Text("接收 ${snapshot.receivedFrames}")
                Text("丢包 ${snapshot.lostFrames}")
                Text("CRC ${snapshot.crcErrors}")
            }
            snapshot.deviceInfo?.let { info ->
                Text(
                    "${info.serialNumber} · 固件 ${info.firmwareVersion} · " +
                        "协议v${info.protocolVersion} · ${info.sampleRateHz}Hz/${info.channelCount}通道",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "能力：${info.capabilities.joinToString()} · 安全规则 ${info.safetyRuleVersion}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            snapshot.parameters?.let { parameters ->
                Text(
                    "刺激参数：${"%.2f".format(parameters.currentMa)}mA · " +
                        "${parameters.frequencyHz}Hz · ${parameters.pulseWidthUs}μs · " +
                        "${parameters.dutyCycle}% · C${parameters.leftContact}/C${parameters.rightContact}",
                )
            }
            snapshot.impedance?.let { impedance ->
                Text(
                    "阻抗：" + impedance.readings.joinToString("；") {
                        "C${it.leftContact}-C${it.rightContact} ${"%.2f".format(it.kiloOhms)}kΩ"
                    },
                )
            }
            if (recentSamples.size >= 4) {
                LfpWaveformAndSpectrum(recentSamples)
            }
            inferenceSnapshot?.topState?.let { state ->
                Text(
                    "端侧状态：$state · 置信度 ${
                        "%.1f".format((inferenceSnapshot.confidence ?: 0.0) * 100)
                    }%${if (inferenceSnapshot.rejected) " · 拒识/预热" else ""}",
                )
                Text(
                    "快速 ${inferenceSnapshot.fastWarmedWindows}/5 · 稳态 ${inferenceSnapshot.stableWarmedWindows}/30 · 模型 ${
                        inferenceSnapshot.modelVersionId ?: "内置回退"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            dispatchSnapshot?.let {
                Text(
                    "参数闭环：${it.status}（成功 ${it.successfulAcks} / 失败 ${it.failedAcks}）",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            snapshot.lastError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (client.hasPermissions()) client.connect()
                        else permissionLauncher.launch(permissions)
                    },
                    enabled = snapshot.linkState == BleLinkState.IDLE,
                ) { Text("连接模拟器") }
                OutlinedButton(
                    onClick = client::disconnect,
                    enabled = snapshot.linkState != BleLinkState.IDLE,
                ) { Text("断开") }
                OutlinedButton(
                    onClick = {
                        val real = repository ?: return@OutlinedButton
                        val targetPatient = patientId ?: return@OutlinedButton
                        recording = true
                        recordingStatus = "正在录制 10 秒手动 LFP…"
                        val lossBefore = snapshot.lostFrames
                        scope.launch {
                            runCatching {
                                val samples = ArrayList<Short>(256 * 2 * 10 + 64)
                                withTimeout(15_000) {
                                    client.lfp.take(103).collect { (_, chunk) ->
                                        chunk.samples.forEach(samples::add)
                                    }
                                }
                                val exact = samples.take(256 * 2 * 10).toShortArray()
                                check(exact.size == 256 * 2 * 10) { "有效样本不足 10 秒" }
                                real.uploadManualLfp(
                                    targetPatient,
                                    exact,
                                    (client.snapshot.value.lostFrames - lossBefore).toInt().coerceAtLeast(0),
                                ).getOrThrow()
                            }.fold(
                                onSuccess = { recordingStatus = "手动 LFP 已压缩上传，导出可使用该波形。" },
                                onFailure = { recordingStatus = "录制失败：${it.message}" },
                            )
                            recording = false
                        }
                    },
                    enabled = snapshot.verifiedSimulator && !recording && repository != null && patientId != null,
                ) { Text(if (recording) "录制中" else "录制10秒") }
            }
            recordingStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(
                "安全限制：本客户端不包含真实植入设备 UUID；未通过模拟标志验证时，通知和参数写入均被禁止。",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LfpWaveformAndSpectrum(interleaved: ShortArray) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val values = remember(interleaved) {
        val points = minOf(interleaved.size / 2, 512)
        val start = interleaved.size / 2 - points
        Array(2) { channel ->
            FloatArray(points) { index ->
                interleaved[(start + index) * 2 + channel].toFloat()
            }
        }
    }
    Text("实时双通道 LFP（最近2秒）", style = MaterialTheme.typography.titleSmall)
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        drawLine(grid, Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
        val peak = max(
            1f,
            values.maxOf { channel -> channel.maxOf { kotlin.math.abs(it) } },
        )
        values.forEachIndexed { channel, samples ->
            val center = if (channel == 0) size.height * 0.25f else size.height * 0.75f
            val color = if (channel == 0) primary else secondary
            for (index in 1 until samples.size) {
                val x0 = (index - 1f) / (samples.size - 1f) * size.width
                val x1 = index.toFloat() / (samples.size - 1f) * size.width
                drawLine(
                    color,
                    Offset(x0, center - samples[index - 1] / peak * size.height * 0.2f),
                    Offset(x1, center - samples[index] / peak * size.height * 0.2f),
                    strokeWidth = 1.5f,
                )
            }
        }
    }
    val spectrum = remember(interleaved) { simpleSpectrum(values[0]) }
    Text("LFP 功率谱（2–100Hz）", style = MaterialTheme.typography.titleSmall)
    Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        if (spectrum.isEmpty()) return@Canvas
        val maxValue = spectrum.maxOf { it.second }.coerceAtLeast(1e-6f)
        for (index in 1 until spectrum.size) {
            val previous = spectrum[index - 1]
            val current = spectrum[index]
            drawLine(
                primary,
                Offset((previous.first - 2f) / 98f * size.width, size.height - previous.second / maxValue * size.height),
                Offset((current.first - 2f) / 98f * size.width, size.height - current.second / maxValue * size.height),
                strokeWidth = 2f,
            )
        }
    }
}

private fun simpleSpectrum(input: FloatArray): List<Pair<Float, Float>> {
    if (input.size < 256) return emptyList()
    val signal = input.takeLast(256)
    val mean = signal.average().toFloat()
    return (2..100 step 2).map { frequency ->
        var real = 0.0
        var imaginary = 0.0
        signal.forEachIndexed { index, value ->
            val window = 0.5 - 0.5 * cos(2.0 * PI * index / 255.0)
            val angle = 2.0 * PI * frequency * index / 256.0
            real += (value - mean) * window * cos(angle)
            imaginary -= (value - mean) * window * sin(angle)
        }
        frequency.toFloat() to ln(1.0 + sqrt(real * real + imaginary * imaginary)).toFloat()
    }
}
