package com.omnidapt.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.roundToInt

object OminidaptBleUuids {
    val SERVICE: UUID = UUID.fromString("8f3a0000-6f4d-4b2b-9a7e-1a0d9c2e1000")
    val DEVICE_INFO: UUID = UUID.fromString("8f3a0001-6f4d-4b2b-9a7e-1a0d9c2e1000")
    val TELEMETRY: UUID = UUID.fromString("8f3a0002-6f4d-4b2b-9a7e-1a0d9c2e1000")
    val LFP_STREAM: UUID = UUID.fromString("8f3a0003-6f4d-4b2b-9a7e-1a0d9c2e1000")
    val COMMAND: UUID = UUID.fromString("8f3a0004-6f4d-4b2b-9a7e-1a0d9c2e1000")
    val ACK: UUID = UUID.fromString("8f3a0005-6f4d-4b2b-9a7e-1a0d9c2e1000")
    val IMPEDANCE: UUID = UUID.fromString("8f3a0006-6f4d-4b2b-9a7e-1a0d9c2e1000")
    val CLIENT_CONFIGURATION: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}

enum class MessageType(val code: Int) {
    DEVICE_INFO(0x01),
    TELEMETRY(0x02),
    LFP_DATA(0x03),
    DEVICE_ALARM(0x04),
    QUERY_STATE(0x10),
    STREAM_CONTROL(0x11),
    SET_PARAMETERS(0x12),
    HEARTBEAT(0x13),
    SET_SCENARIO(0x14),
    IMPEDANCE(0x15),
    MEASURE_IMPEDANCE(0x16),
    ACK(0x20);

    companion object {
        fun fromCode(code: Int): MessageType =
            entries.firstOrNull { it.code == code }
                ?: throw ProtocolException("unknown message type: $code")
    }
}

enum class SimulatedState(val code: Int, val label: String) {
    OFF_REST(0, "OFF-Rest"),
    OFF_MOVE(1, "OFF-Move"),
    ON_REST(2, "ON-Rest"),
    ON_MOVE(3, "ON-Move"),
    CONTINUOUS(4, "Continuous-Replay");

    companion object {
        fun fromCode(code: Int): SimulatedState =
            entries.firstOrNull { it.code == code } ?: CONTINUOUS
    }
}

data class BleFrame(
    val type: MessageType,
    val sequence: Long,
    val timestampMs: Long,
    val payload: ByteArray,
)

data class LfpChunk(
    val sampleRateHz: Int,
    val channelCount: Int,
    val sampleCount: Int,
    val state: SimulatedState,
    val samples: ShortArray,
) {
    override fun equals(other: Any?): Boolean =
        other is LfpChunk &&
            sampleRateHz == other.sampleRateHz &&
            channelCount == other.channelCount &&
            sampleCount == other.sampleCount &&
            state == other.state &&
            samples.contentEquals(other.samples)

    override fun hashCode(): Int {
        var result = sampleRateHz
        result = 31 * result + channelCount
        result = 31 * result + sampleCount
        result = 31 * result + state.hashCode()
        return 31 * result + samples.contentHashCode()
    }

    override fun toString(): String =
        "LfpChunk(sampleRateHz=$sampleRateHz, channelCount=$channelCount, " +
            "sampleCount=$sampleCount, state=$state, samples=${samples.contentToString()})"
}

data class StimulationParameters(
    val currentMa: Float,
    val frequencyHz: Int,
    val pulseWidthUs: Int,
    val dutyCycle: Int,
    val leftContact: Int = 6,
    val rightContact: Int = 2,
)

data class DeviceTelemetry(
    val batteryPercent: Int,
    val streaming: Boolean,
    val alarm: Boolean,
    val parameters: StimulationParameters,
    val simulatedState: SimulatedState = SimulatedState.CONTINUOUS,
    val medicationEffectPercent: Int = 0,
    val movementIntensityPercent: Int = 0,
)

data class DeviceInfo(
    val serialNumber: String,
    val name: String,
    val firmwareVersion: String,
    val protocolVersion: Int,
    val sampleRateHz: Int,
    val channelCount: Int,
    val contacts: List<Int>,
    val capabilities: List<String>,
    val safetyRuleVersion: String,
    val simulated: Boolean,
    val clinicalUse: Boolean,
)

data class ImpedanceReading(
    val leftContact: Int,
    val rightContact: Int,
    val kiloOhms: Float,
    val qualityCode: Int = 0,
)

data class ImpedanceRequest(val pairs: List<Pair<Int, Int>>)

data class ImpedanceSnapshot(
    val measurementSequence: Long,
    val readings: List<ImpedanceReading>,
)

data class ScenarioCommand(
    val state: SimulatedState,
    val medicationEffectPercent: Int,
    val movementIntensityPercent: Int,
    val transitionMs: Int = 1_000,
)

data class CommandAck(
    val acknowledgedSequence: Long,
    val success: Boolean,
    val statusCode: Int,
)

class ProtocolException(message: String) : IllegalArgumentException(message)

object OminidaptBleProtocol {
    const val VERSION: Int = 3
    const val HEADER_SIZE: Int = 18
    const val CRC_SIZE: Int = 2
    const val LFP_SAMPLES_PER_CHUNK: Int = 25
    const val REQUESTED_MTU: Int = 247
    const val ATT_OVERHEAD: Int = 3
    const val FRAGMENT_HEADER_SIZE: Int = 9
    private const val MAGIC_0: Byte = 0x4F
    private const val MAGIC_1: Byte = 0x50
    private const val FRAGMENT_MAGIC_1: Byte = 0x46

    fun encode(frame: BleFrame): ByteArray {
        require(frame.sequence in 0..0xFFFF_FFFFL) { "sequence must fit uint32" }
        require(frame.payload.size <= 0xFFFF) { "payload is too large" }
        val buffer = ByteBuffer
            .allocate(HEADER_SIZE + frame.payload.size + CRC_SIZE)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(MAGIC_0)
        buffer.put(MAGIC_1)
        buffer.put(VERSION.toByte())
        buffer.put(frame.type.code.toByte())
        buffer.putInt(frame.sequence.toInt())
        buffer.putLong(frame.timestampMs)
        buffer.putShort(frame.payload.size.toShort())
        buffer.put(frame.payload)
        val withoutCrc = buffer.array().copyOf(buffer.position())
        buffer.putShort(crc16Ccitt(withoutCrc).toShort())
        return buffer.array()
    }

    fun decode(bytes: ByteArray): BleFrame {
        if (bytes.size < HEADER_SIZE + CRC_SIZE) throw ProtocolException("frame is too short")
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (buffer.get() != MAGIC_0 || buffer.get() != MAGIC_1) {
            throw ProtocolException("invalid frame magic")
        }
        val version = buffer.get().toInt() and 0xFF
        if (version != VERSION) throw ProtocolException("unsupported protocol version: $version")
        val type = MessageType.fromCode(buffer.get().toInt() and 0xFF)
        val sequence = buffer.int.toLong() and 0xFFFF_FFFFL
        val timestamp = buffer.long
        val payloadLength = buffer.short.toInt() and 0xFFFF
        if (bytes.size != HEADER_SIZE + payloadLength + CRC_SIZE) {
            throw ProtocolException("payload length does not match frame size")
        }
        val payload = ByteArray(payloadLength)
        buffer.get(payload)
        val expectedCrc = buffer.short.toInt() and 0xFFFF
        val actualCrc = crc16Ccitt(bytes.copyOf(bytes.size - CRC_SIZE))
        if (expectedCrc != actualCrc) throw ProtocolException("CRC mismatch")
        return BleFrame(type, sequence, timestamp, payload)
    }

    fun encodeLfpPayload(chunk: LfpChunk): ByteArray {
        require(chunk.channelCount in 1..8)
        require(chunk.sampleCount in 1..255)
        require(chunk.samples.size == chunk.channelCount * chunk.sampleCount)
        val buffer = ByteBuffer
            .allocate(5 + chunk.samples.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(chunk.sampleRateHz.toShort())
        buffer.put(chunk.channelCount.toByte())
        buffer.put(chunk.sampleCount.toByte())
        buffer.put(chunk.state.code.toByte())
        chunk.samples.forEach(buffer::putShort)
        return buffer.array()
    }

    fun decodeLfpPayload(payload: ByteArray): LfpChunk {
        if (payload.size < 5) throw ProtocolException("LFP payload is too short")
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val sampleRate = buffer.short.toInt() and 0xFFFF
        val channels = buffer.get().toInt() and 0xFF
        val sampleCount = buffer.get().toInt() and 0xFF
        val state = SimulatedState.fromCode(buffer.get().toInt() and 0xFF)
        val expectedSamples = channels * sampleCount
        if (buffer.remaining() != expectedSamples * 2) {
            throw ProtocolException("LFP sample count does not match payload")
        }
        val samples = ShortArray(expectedSamples) { buffer.short }
        return LfpChunk(sampleRate, channels, sampleCount, state, samples)
    }

    fun encodeParameters(parameters: StimulationParameters): ByteArray =
        ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort((parameters.currentMa * 1000f).toInt().coerceIn(0, 65535).toShort())
            putShort(parameters.frequencyHz.toShort())
            putShort(parameters.pulseWidthUs.toShort())
            put(parameters.dutyCycle.toByte())
            put(parameters.leftContact.toByte())
            put(parameters.rightContact.toByte())
        }.array()

    fun decodeParameters(payload: ByteArray): StimulationParameters {
        if (payload.size != 9) throw ProtocolException("parameter payload must be 9 bytes")
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return StimulationParameters(
            currentMa = (buffer.short.toInt() and 0xFFFF) / 1000f,
            frequencyHz = buffer.short.toInt() and 0xFFFF,
            pulseWidthUs = buffer.short.toInt() and 0xFFFF,
            dutyCycle = buffer.get().toInt() and 0xFF,
            leftContact = buffer.get().toInt() and 0xFF,
            rightContact = buffer.get().toInt() and 0xFF,
        )
    }

    fun encodeTelemetry(telemetry: DeviceTelemetry): ByteArray =
        ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(telemetry.batteryPercent.coerceIn(0, 100).toByte())
            var flags = 0
            if (telemetry.streaming) flags = flags or 0x01
            if (telemetry.alarm) flags = flags or 0x02
            put(flags.toByte())
            put(encodeParameters(telemetry.parameters))
            put(telemetry.simulatedState.code.toByte())
            put(telemetry.medicationEffectPercent.coerceIn(0, 100).toByte())
            put(telemetry.movementIntensityPercent.coerceIn(0, 100).toByte())
        }.array()

    fun decodeTelemetry(payload: ByteArray): DeviceTelemetry {
        if (payload.size !in setOf(12, 14)) {
            throw ProtocolException("telemetry payload must be 12 or 14 bytes")
        }
        val battery = payload[0].toInt() and 0xFF
        val flags = payload[1].toInt() and 0xFF
        return DeviceTelemetry(
            batteryPercent = battery,
            streaming = flags and 0x01 != 0,
            alarm = flags and 0x02 != 0,
            parameters = decodeParameters(payload.copyOfRange(2, 11)),
            simulatedState = if (payload.size >= 14) {
                SimulatedState.fromCode(payload[11].toInt() and 0xFF)
            } else {
                SimulatedState.CONTINUOUS
            },
            medicationEffectPercent = if (payload.size >= 14) payload[12].toInt() and 0xFF else 0,
            movementIntensityPercent = if (payload.size >= 14) payload[13].toInt() and 0xFF else 0,
        )
    }

    fun encodeImpedance(snapshot: ImpedanceSnapshot): ByteArray {
        require(snapshot.readings.size <= 255)
        require(snapshot.measurementSequence in 0..0xFFFF_FFFFL)
        val buffer = ByteBuffer
            .allocate(5 + snapshot.readings.size * 5)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(snapshot.measurementSequence.toInt())
        buffer.put(snapshot.readings.size.toByte())
        snapshot.readings.forEach { reading ->
            require(reading.leftContact in 0..255 && reading.rightContact in 0..255)
            buffer.put(reading.leftContact.toByte())
            buffer.put(reading.rightContact.toByte())
            buffer.putShort((reading.kiloOhms * 100f).roundToInt().coerceIn(0, 65535).toShort())
            buffer.put(reading.qualityCode.coerceIn(0, 255).toByte())
        }
        return buffer.array()
    }

    fun decodeImpedance(payload: ByteArray): ImpedanceSnapshot {
        if (payload.size < 5) throw ProtocolException("impedance payload is too short")
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val measurementSequence = buffer.int.toLong() and 0xFFFF_FFFFL
        val count = buffer.get().toInt() and 0xFF
        if (buffer.remaining() != count * 5) {
            throw ProtocolException("impedance reading count does not match payload")
        }
        return ImpedanceSnapshot(
            measurementSequence = measurementSequence,
            readings = List(count) {
                ImpedanceReading(
                    leftContact = buffer.get().toInt() and 0xFF,
                    rightContact = buffer.get().toInt() and 0xFF,
                    kiloOhms = (buffer.short.toInt() and 0xFFFF) / 100f,
                    qualityCode = buffer.get().toInt() and 0xFF,
                )
            },
        )
    }

    fun encodeImpedanceRequest(request: ImpedanceRequest): ByteArray {
        require(request.pairs.isNotEmpty() && request.pairs.size <= 8)
        return ByteBuffer.allocate(1 + request.pairs.size * 2).apply {
            put(request.pairs.size.toByte())
            request.pairs.forEach { (first, second) ->
                require(first in 1..8 && second in 1..8 && first != second)
                put(first.toByte())
                put(second.toByte())
            }
        }.array()
    }

    fun decodeImpedanceRequest(payload: ByteArray): ImpedanceRequest {
        if (payload.isEmpty()) throw ProtocolException("impedance request is too short")
        val buffer = ByteBuffer.wrap(payload)
        val count = buffer.get().toInt() and 0xFF
        if (count !in 1..8 || buffer.remaining() != count * 2) {
            throw ProtocolException("impedance request pair count does not match payload")
        }
        return ImpedanceRequest(
            List(count) {
                val first = buffer.get().toInt() and 0xFF
                val second = buffer.get().toInt() and 0xFF
                if (first !in 1..8 || second !in 1..8 || first == second) {
                    throw ProtocolException("invalid impedance contact pair")
                }
                first to second
            },
        )
    }

    fun encodeScenario(command: ScenarioCommand): ByteArray =
        ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(command.state.code.toByte())
            put(command.medicationEffectPercent.coerceIn(0, 100).toByte())
            put(command.movementIntensityPercent.coerceIn(0, 100).toByte())
            putShort(command.transitionMs.coerceIn(0, 65_535).toShort())
        }.array()

    fun decodeScenario(payload: ByteArray): ScenarioCommand {
        if (payload.size != 5) throw ProtocolException("scenario payload must be 5 bytes")
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return ScenarioCommand(
            state = SimulatedState.fromCode(buffer.get().toInt() and 0xFF),
            medicationEffectPercent = buffer.get().toInt() and 0xFF,
            movementIntensityPercent = buffer.get().toInt() and 0xFF,
            transitionMs = buffer.short.toInt() and 0xFFFF,
        )
    }

    /**
     * Splits one logical OP frame for GATT notification transports whose
     * negotiated payload is smaller than the frame. Unfragmented frames retain
     * the original OP prefix. Fragmented packets use OF + version + frame id +
     * index/count; CRC is validated after the logical frame is reassembled.
     */
    fun fragment(frameBytes: ByteArray, maxNotificationBytes: Int, frameId: Long): List<ByteArray> {
        require(maxNotificationBytes > FRAGMENT_HEADER_SIZE)
        if (frameBytes.size <= maxNotificationBytes) return listOf(frameBytes)
        val chunkSize = maxNotificationBytes - FRAGMENT_HEADER_SIZE
        val count = (frameBytes.size + chunkSize - 1) / chunkSize
        require(count <= 255) { "logical frame needs too many fragments" }
        return List(count) { index ->
            val start = index * chunkSize
            val end = minOf(frameBytes.size, start + chunkSize)
            ByteBuffer.allocate(FRAGMENT_HEADER_SIZE + end - start)
                .order(ByteOrder.LITTLE_ENDIAN)
                .apply {
                    put(MAGIC_0)
                    put(FRAGMENT_MAGIC_1)
                    put(VERSION.toByte())
                    putInt(frameId.toInt())
                    put(index.toByte())
                    put(count.toByte())
                    put(frameBytes, start, end - start)
                }.array()
        }
    }

    fun isFragment(bytes: ByteArray): Boolean =
        bytes.size >= FRAGMENT_HEADER_SIZE &&
            bytes[0] == MAGIC_0 &&
            bytes[1] == FRAGMENT_MAGIC_1

    fun encodeAck(ack: CommandAck): ByteArray =
        ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(ack.acknowledgedSequence.toInt())
            put((if (ack.success) 1 else 0).toByte())
            put(ack.statusCode.toByte())
        }.array()

    fun decodeAck(payload: ByteArray): CommandAck {
        if (payload.size != 6) throw ProtocolException("ACK payload must be 6 bytes")
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return CommandAck(
            acknowledgedSequence = buffer.int.toLong() and 0xFFFF_FFFFL,
            success = buffer.get().toInt() != 0,
            statusCode = buffer.get().toInt() and 0xFF,
        )
    }

    fun crc16Ccitt(bytes: ByteArray): Int {
        var crc = 0xFFFF
        for (byte in bytes) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }
        return crc
    }
}

class BleFrameReassembler(
    private val timeoutMs: Long = 2_000,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {
    private data class Pending(
        val createdAtMs: Long,
        val count: Int,
        val chunks: Array<ByteArray?>,
    )

    private val pending = linkedMapOf<Long, Pending>()

    @Synchronized
    fun offer(packet: ByteArray): ByteArray? {
        evictExpired()
        if (!OminidaptBleProtocol.isFragment(packet)) return packet
        val header = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        header.position(2)
        val version = header.get().toInt() and 0xFF
        if (version != OminidaptBleProtocol.VERSION) {
            throw ProtocolException("unsupported fragment version: $version")
        }
        val frameId = header.int.toLong() and 0xFFFF_FFFFL
        val index = header.get().toInt() and 0xFF
        val count = header.get().toInt() and 0xFF
        if (count == 0 || index >= count) throw ProtocolException("invalid fragment index/count")
        val body = packet.copyOfRange(OminidaptBleProtocol.FRAGMENT_HEADER_SIZE, packet.size)
        val item = pending.getOrPut(frameId) {
            Pending(clockMs(), count, arrayOfNulls(count))
        }
        if (item.count != count) {
            pending.remove(frameId)
            throw ProtocolException("fragment count changed")
        }
        item.chunks[index] = body
        if (item.chunks.any { it == null }) return null
        pending.remove(frameId)
        val size = item.chunks.sumOf { requireNotNull(it).size }
        return ByteArray(size).also { result ->
            var offset = 0
            item.chunks.forEach { chunk ->
                requireNotNull(chunk).copyInto(result, offset)
                offset += chunk.size
            }
        }
    }

    @Synchronized
    fun clear() = pending.clear()

    private fun evictExpired() {
        val cutoff = clockMs() - timeoutMs
        pending.entries.removeAll { it.value.createdAtMs < cutoff }
    }
}
