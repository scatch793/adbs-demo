package com.omnidapt.pd.real.ble

import com.omnidapt.pd.real.RealRepository
import com.omnidapt.protocol.StimulationParameters
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommandDispatchSnapshot(
    val lastCommandId: String? = null,
    val status: String = "等待医生审核后的模拟参数",
    val successfulAcks: Long = 0,
    val failedAcks: Long = 0,
)

class DeviceCommandDispatcher(
    private val client: BleCentralClient,
    private val repository: RealRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sequenceToCommand = ConcurrentHashMap<Long, String>()
    private val mutableSnapshot = MutableStateFlow(CommandDispatchSnapshot())
    val snapshot: StateFlow<CommandDispatchSnapshot> = mutableSnapshot
    private var job: Job? = null

    fun start(patientId: String) {
        job?.cancel()
        job = scope.launch {
            launch {
                client.acknowledgements.collect { ack ->
                    val commandId = sequenceToCommand.remove(ack.acknowledgedSequence) ?: return@collect
                    runCatching {
                        repository.acknowledgeCommand(
                            commandId = commandId,
                            sequence = ack.acknowledgedSequence,
                            success = ack.success,
                            statusCode = if (ack.success) "ok" else "simulator_rejected_${ack.statusCode}",
                        )
                    }
                    mutableSnapshot.update {
                        it.copy(
                            lastCommandId = commandId,
                            status = if (ack.success) "模拟器已确认参数" else "模拟器拒绝参数（${ack.statusCode}）",
                            successfulAcks = it.successfulAcks + if (ack.success) 1 else 0,
                            failedAcks = it.failedAcks + if (ack.success) 0 else 1,
                        )
                    }
                }
            }
            while (true) {
                if (client.snapshot.value.verifiedSimulator && sequenceToCommand.isEmpty()) {
                    runCatching { repository.pendingCommands(patientId) }
                        .getOrNull()
                        ?.firstOrNull()
                        ?.let { command ->
                            val parameters = command.payload.toSafeParameters()
                            if (parameters == null) {
                                repository.acknowledgeCommand(
                                    command.id,
                                    command.sequence,
                                    false,
                                    "client_safety_rejected",
                                    "参数超出端侧科研模拟安全边界",
                                )
                                mutableSnapshot.update {
                                    it.copy(lastCommandId = command.id, status = "端侧安全规则已拒绝越界参数", failedAcks = it.failedAcks + 1)
                                }
                            } else {
                                client.setParameters(parameters, command.sequence)?.let { bleSequence ->
                                    sequenceToCommand[bleSequence] = command.id
                                    mutableSnapshot.update {
                                        it.copy(lastCommandId = command.id, status = "参数已写入模拟器，等待 ACK")
                                    }
                                    launch {
                                        delay(8_000)
                                        if (sequenceToCommand.remove(bleSequence) != null) {
                                            repository.acknowledgeCommand(
                                                command.id,
                                                bleSequence,
                                                false,
                                                "ack_timeout",
                                                "8 秒内未收到模拟器 ACK",
                                            )
                                            mutableSnapshot.update {
                                                it.copy(status = "模拟器 ACK 超时，未记录为成功", failedAcks = it.failedAcks + 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
                delay(2_000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        sequenceToCommand.clear()
    }
}

private fun Map<String, Double>.toSafeParameters(): StimulationParameters? {
    val current = this["current_ma"] ?: return null
    val frequency = this["frequency_hz"] ?: return null
    val pulseWidth = this["pulse_width_us"] ?: return null
    val duty = this["duty_cycle"] ?: 45.0
    val safe = current in 1.0..3.0 &&
        frequency in 120.0..150.0 &&
        pulseWidth in 50.0..90.0 &&
        duty in 20.0..80.0
    if (!safe) return null
    return StimulationParameters(
        currentMa = current.toFloat(),
        frequencyHz = frequency.toInt(),
        pulseWidthUs = pulseWidth.toInt(),
        dutyCycle = duty.toInt(),
        leftContact = (this["left_contact"] ?: 6.0).toInt(),
        rightContact = (this["right_contact"] ?: 2.0).toInt(),
    )
}
