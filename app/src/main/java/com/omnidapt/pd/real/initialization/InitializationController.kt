package com.omnidapt.pd.real.initialization

import com.omnidapt.pd.real.RealRepository
import com.omnidapt.pd.real.ble.BleCentralClient
import com.omnidapt.pd.real.network.ApiInitialization
import com.omnidapt.protocol.ScenarioCommand
import com.omnidapt.protocol.SimulatedState
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout

data class InitializationUiState(
    val running: Boolean = false,
    val phase: String = "尚未开始",
    val stateLabel: String? = null,
    val remainingSeconds: Int = 0,
    val collectedSamples: Int = 0,
    val targetSamples: Int = 0,
    val result: ApiInitialization? = null,
    val error: String? = null,
)

class InitializationController(
    private val client: BleCentralClient,
    private val repository: RealRepository,
) {
    private val mutableState = MutableStateFlow(InitializationUiState())
    val state: StateFlow<InitializationUiState> = mutableState

    suspend fun loadLatest(patientId: String) {
        runCatching { repository.initializations(patientId).firstOrNull() }
            .onSuccess { latest ->
                if (!mutableState.value.running) {
                    mutableState.update {
                        it.copy(
                            phase = when (latest?.status) {
                                "review" -> "已有分析结果，等待医生审核"
                                "approved" -> "模型已审核启用"
                                "failed" -> "最近一次初始化失败"
                                null -> "尚未开始"
                                else -> "已有初始化任务：${latest.status}"
                            },
                            result = latest,
                            error = latest?.error,
                        )
                    }
                }
            }
    }

    suspend fun run(
        patientId: String,
        mode: String,
        electrodeConfig: Map<String, Any> = mapOf(
            "leftPositive" to 5,
            "leftNegative" to 6,
            "rightPositive" to 1,
            "rightNegative" to 2,
            "source" to "windows_ble_simulator",
        ),
    ) {
        check(!mutableState.value.running) { "初始化正在进行" }
        check(client.snapshot.value.verifiedSimulator) { "请先连接并验证电脑模拟刺激设备" }
        mutableState.value = mutableState.value.copy(running = true, phase = "准备当前状态", error = null)
        try {
            var run = mutableState.value.result
                ?.takeIf { it.status in setOf("draft", "configuring", "capturing") }
                ?: repository.createInitialization(
                    patientId,
                    mode,
                    electrodeConfig + ("source" to "windows_ble_simulator"),
                )
            val states = listOf(
                SimulatedState.OFF_REST,
                SimulatedState.OFF_MOVE,
                SimulatedState.ON_REST,
                SimulatedState.ON_MOVE,
            )
            val accepted = run.segments.filter { it.accepted }.map { it.state_label }.toSet()
            val simulatedState = states.firstOrNull { it.label !in accepted }
                ?: error("四状态采集已完成，请启动模型分析")
            requestScenario(simulatedState)
            countdown("状态稳定", simulatedState.label, run.settle_seconds)
            val before = client.snapshot.value
            val targetSamples = run.capture_seconds * 256
            val frameCount = (targetSamples + 24) / 25
            val values = ArrayList<Short>(frameCount * 50)
            mutableState.update {
                it.copy(
                    phase = "采集基线",
                    stateLabel = simulatedState.label,
                    remainingSeconds = run.capture_seconds,
                    collectedSamples = 0,
                    targetSamples = targetSamples,
                )
            }
            withTimeout((run.capture_seconds + 20L) * 1_000L) {
                client.lfp
                    .filter { (_, chunk) -> chunk.state == simulatedState }
                    .take(frameCount)
                    .collect { (_, chunk) ->
                        chunk.samples.forEach(values::add)
                        val samplesPerChannel = values.size / 2
                        mutableState.update {
                            it.copy(
                                collectedSamples = minOf(samplesPerChannel, targetSamples),
                                remainingSeconds = (
                                    run.capture_seconds - samplesPerChannel / 256
                                ).coerceAtLeast(0),
                            )
                        }
                    }
            }
            val exact = values.take(targetSamples * 2).toShortArray()
            check(exact.size == targetSamples * 2) { "有效样本不足 ${run.capture_seconds} 秒" }
            val after = client.snapshot.value
            val saturation = exact.count { abs(it.toInt()) >= 32_760 }
            val impedance = after.impedance?.readings
                ?.associate { reading ->
                    "C${reading.leftContact}-C${reading.rightContact}_kohm" to reading.kiloOhms
                }.orEmpty()
            mutableState.update { it.copy(phase = "上传并校验 ${simulatedState.label}") }
            run = repository.uploadInitializationSegment(
                initializationId = run.id,
                patientId = patientId,
                deviceId = run.device_id,
                stateLabel = simulatedState.label,
                interleavedSamples = exact,
                receivedFrames = frameCount,
                packetLossCount = (after.lostFrames - before.lostFrames).toInt().coerceAtLeast(0),
                crcErrorCount = (after.crcErrors - before.crcErrors).toInt().coerceAtLeast(0),
                saturatedSampleCount = saturation,
                impedance = impedance,
            )
            check(run.segments.first { it.state_label == simulatedState.label }.accepted) {
                "${simulatedState.label} 质量检查未通过，可重采当前状态"
            }
            val completed = run.segments.count { it.accepted }
            mutableState.value = InitializationUiState(
                running = false,
                phase = if (completed == 4) {
                    "四状态采集完成，等待启动模型分析"
                } else {
                    "${simulatedState.label} 质量通过，等待医生确认下一状态"
                },
                result = run,
            )
        } catch (error: Throwable) {
            mutableState.update {
                it.copy(
                    running = false,
                    phase = "初始化中止",
                    error = error.message ?: "未知错误",
                )
            }
            throw error
        }
    }

    suspend fun analyze() {
        var run = mutableState.value.result ?: error("尚未创建初始化任务")
        check(run.segments.count { it.accepted } == 4) { "四状态尚未全部通过质量检查" }
        mutableState.update {
            it.copy(running = true, phase = "提交模型分析", error = null)
        }
        try {
            run = repository.analyzeInitialization(run.id)
            var attempts = 0
            while (run.status !in setOf("review", "failed") && attempts < 180) {
                delay(1_000)
                run = repository.initialization(run.id)
                mutableState.update {
                    it.copy(
                        phase = analysisStageLabel(run.analysis_stage),
                        result = run,
                    )
                }
                attempts++
            }
            check(run.status == "review") { run.error ?: "模型分析未完成：${run.status}" }
            mutableState.value = InitializationUiState(
                running = false,
                phase = "等待医生审核",
                result = run,
            )
        } catch (error: Throwable) {
            mutableState.update {
                it.copy(running = false, phase = "模型分析失败", error = error.message)
            }
            throw error
        }
    }

    suspend fun approve() {
        val run = mutableState.value.result ?: error("没有待审核模型")
        check(run.status == "review") { "当前任务不是待审核状态" }
        mutableState.update { it.copy(phase = "正在批准模型", error = null) }
        val approved = repository.approveInitialization(run.id)
        mutableState.update {
            it.copy(phase = "初始化与模型审核完成", result = approved)
        }
    }

    private suspend fun requestScenario(state: SimulatedState) {
        mutableState.update { it.copy(phase = "请求模拟器切换场景", stateLabel = state.label) }
        val sequence = client.setScenario(
            ScenarioCommand(
                state = state,
                medicationEffectPercent = if (state in setOf(SimulatedState.ON_REST, SimulatedState.ON_MOVE)) 100 else 0,
                movementIntensityPercent = if (state in setOf(SimulatedState.OFF_MOVE, SimulatedState.ON_MOVE)) 80 else 0,
                transitionMs = 1_000,
            ),
        ) ?: error("无法发送模拟场景命令")
        val ack = withTimeout(8_000) {
            client.acknowledgements.first { it.acknowledgedSequence == sequence }
        }
        check(ack.success) { "模拟器拒绝场景命令，状态码 ${ack.statusCode}" }
    }

    private suspend fun countdown(phase: String, stateLabel: String, seconds: Int) {
        for (remaining in seconds downTo 1) {
            mutableState.update {
                it.copy(
                    phase = phase,
                    stateLabel = stateLabel,
                    remainingSeconds = remaining,
                    collectedSamples = 0,
                    targetSamples = 0,
                )
            }
            delay(1_000)
        }
    }

    private fun analysisStageLabel(stage: String): String = when (stage) {
        "queued" -> "等待分析Worker"
        "data_validation" -> "校验四状态数据"
        "loading" -> "载入脱敏LFP"
        "psd" -> "计算Welch功率谱"
        "fisher" -> "计算Fisher曲线与个体化频段"
        "features" -> "生成五维特征"
        "gmm" -> "训练快速/稳态GMM"
        "validation" -> "验证模型"
        "packaging" -> "打包版本化模型"
        else -> "服务器模型计算中"
    }
}
