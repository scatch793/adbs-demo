package com.omnidapt.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BleProtocolTest {
    @Test
    fun frameRoundTripPreservesFields() {
        val frame = BleFrame(
            type = MessageType.HEARTBEAT,
            sequence = 0x10203040,
            timestampMs = 1_700_000_000_123,
            payload = byteArrayOf(1, 2, 3, 4),
        )

        val decoded = OminidaptBleProtocol.decode(OminidaptBleProtocol.encode(frame))

        assertEquals(frame.type, decoded.type)
        assertEquals(frame.sequence, decoded.sequence)
        assertEquals(frame.timestampMs, decoded.timestampMs)
        assertArrayEquals(frame.payload, decoded.payload)
    }

    @Test
    fun corruptedFrameIsRejected() {
        val bytes = OminidaptBleProtocol.encode(
            BleFrame(MessageType.HEARTBEAT, 7, 1000, byteArrayOf(1, 2)),
        )
        bytes[8] = (bytes[8].toInt() xor 0x7F).toByte()

        assertThrows(ProtocolException::class.java) {
            OminidaptBleProtocol.decode(bytes)
        }
    }

    @Test
    fun lfpPayloadRoundTripPreservesInterleavedSamples() {
        val samples = ShortArray(50) { it.toShort() }
        val chunk = LfpChunk(256, 2, 25, SimulatedState.OFF_MOVE, samples)

        val decoded = OminidaptBleProtocol.decodeLfpPayload(
            OminidaptBleProtocol.encodeLfpPayload(chunk),
        )

        assertEquals(chunk.copy(samples = shortArrayOf()), decoded.copy(samples = shortArrayOf()))
        assertArrayEquals(samples, decoded.samples)
    }

    @Test
    fun parameterPayloadUsesFixedPointCurrent() {
        val parameters = StimulationParameters(2.125f, 130, 70, 45, 6, 2)

        val decoded = OminidaptBleProtocol.decodeParameters(
            OminidaptBleProtocol.encodeParameters(parameters),
        )

        assertEquals(parameters, decoded)
    }

    @Test
    fun telemetryCarriesSimulatorGroundTruth() {
        val telemetry = DeviceTelemetry(
            batteryPercent = 86,
            streaming = true,
            alarm = false,
            parameters = StimulationParameters(1.75f, 130, 60, 50, 6, 2),
            simulatedState = SimulatedState.ON_MOVE,
            medicationEffectPercent = 72,
            movementIntensityPercent = 64,
        )

        assertEquals(
            telemetry,
            OminidaptBleProtocol.decodeTelemetry(OminidaptBleProtocol.encodeTelemetry(telemetry)),
        )
    }

    @Test
    fun impedanceAndScenarioPayloadsRoundTrip() {
        val request = ImpedanceRequest(listOf(6 to 5, 2 to 1))
        val impedance = ImpedanceSnapshot(
            measurementSequence = 42,
            readings = listOf(
                ImpedanceReading(6, 2, 2.35f, 0),
                ImpedanceReading(7, 3, 4.5f, 1),
            ),
        )
        val scenario = ScenarioCommand(SimulatedState.ON_MOVE, 85, 70, 1_500)

        assertEquals(
            request,
            OminidaptBleProtocol.decodeImpedanceRequest(
                OminidaptBleProtocol.encodeImpedanceRequest(request),
            ),
        )
        assertEquals(
            impedance,
            OminidaptBleProtocol.decodeImpedance(OminidaptBleProtocol.encodeImpedance(impedance)),
        )
        assertEquals(
            scenario,
            OminidaptBleProtocol.decodeScenario(OminidaptBleProtocol.encodeScenario(scenario)),
        )
    }

    @Test
    fun fragmentedFrameCanArriveOutOfOrderAndReassemble() {
        val frame = OminidaptBleProtocol.encode(
            BleFrame(
                MessageType.LFP_DATA,
                42,
                1_700_000_000_000,
                OminidaptBleProtocol.encodeLfpPayload(
                    LfpChunk(
                        256,
                        2,
                        25,
                        SimulatedState.ON_MOVE,
                        ShortArray(50) { (it * 13).toShort() },
                    ),
                ),
            ),
        )
        val packets = OminidaptBleProtocol.fragment(frame, 20, 42)
        val reassembler = BleFrameReassembler()
        var result: ByteArray? = null

        packets.reversed().forEach { packet ->
            reassembler.offer(packet)?.let { result = it }
        }

        assertArrayEquals(frame, result)
        assertEquals(42L, OminidaptBleProtocol.decode(requireNotNull(result)).sequence)
    }

    @Test
    fun thirtyMinuteVirtualReplayKeepsSequenceAndCrcValid() {
        val frameCount = 30 * 60 * 10
        var previous = 0L
        repeat(frameCount) { index ->
            val sequence = index.toLong() + 1
            val samples = ShortArray(50) { sample -> (sample + index).toShort() }
            val encoded = OminidaptBleProtocol.encode(
                BleFrame(
                    MessageType.LFP_DATA,
                    sequence,
                    1_700_000_000_000 + index * 100L,
                    OminidaptBleProtocol.encodeLfpPayload(
                        LfpChunk(256, 2, 25, SimulatedState.CONTINUOUS, samples),
                    ),
                ),
            )
            val decoded = OminidaptBleProtocol.decode(encoded)
            assertEquals(previous + 1, decoded.sequence)
            previous = decoded.sequence
        }
        assertEquals(frameCount.toLong(), previous)
    }
}
