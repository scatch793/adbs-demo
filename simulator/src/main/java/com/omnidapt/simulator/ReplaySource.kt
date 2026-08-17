package com.omnidapt.simulator

import android.content.res.AssetManager
import com.omnidapt.protocol.SimulatedState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class ReplaySource(private val assets: AssetManager) {
    private val dataByState = mutableMapOf<SimulatedState, ShortArray>()
    private val cursorByState = mutableMapOf<SimulatedState, Int>()

    fun next(state: SimulatedState, sampleCount: Int, channels: Int = 2): ShortArray {
        val source = dataByState.getOrPut(state) { loadOrSynthesize(state) }
        var cursor = cursorByState[state] ?: 0
        val result = ShortArray(sampleCount * channels)
        for (index in result.indices) {
            result[index] = source[cursor]
            cursor = (cursor + 1) % source.size
        }
        cursorByState[state] = cursor
        return result
    }

    fun usesRealReplay(state: SimulatedState): Boolean {
        val file = assetName(state)
        return runCatching {
            assets.open(file).close()
            true
        }.getOrDefault(false)
    }

    private fun loadOrSynthesize(state: SimulatedState): ShortArray {
        val bytes = runCatching { assets.open(assetName(state)).use { it.readBytes() } }.getOrNull()
        if (bytes != null && bytes.size >= 4 && bytes.size % 2 == 0) {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return ShortArray(bytes.size / 2) { buffer.short }
        }
        val seconds = 60
        val sampleRate = 256
        val channels = 2
        val result = ShortArray(seconds * sampleRate * channels)
        val betaAmplitude = when (state) {
            SimulatedState.OFF_REST -> 1300.0
            SimulatedState.OFF_MOVE -> 950.0
            SimulatedState.ON_REST -> 500.0
            SimulatedState.ON_MOVE -> 420.0
            SimulatedState.CONTINUOUS -> 800.0
        }
        val gammaAmplitude = if (state == SimulatedState.OFF_MOVE || state == SimulatedState.ON_MOVE) 700.0 else 180.0
        for (sample in 0 until seconds * sampleRate) {
            val time = sample.toDouble() / sampleRate
            for (channel in 0 until channels) {
                val beta = betaAmplitude * sin(2 * PI * (18 + channel * 2) * time)
                val gamma = gammaAmplitude * sin(2 * PI * (82 + channel) * time)
                val noise = Random(sample * 17 + channel).nextDouble(-180.0, 180.0)
                result[sample * channels + channel] = (beta + gamma + noise)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }
        return result
    }

    private fun assetName(state: SimulatedState): String = when (state) {
        SimulatedState.OFF_REST -> "off_rest.lfp"
        SimulatedState.OFF_MOVE -> "off_move.lfp"
        SimulatedState.ON_REST -> "on_rest.lfp"
        SimulatedState.ON_MOVE -> "on_move.lfp"
        SimulatedState.CONTINUOUS -> "continuous.lfp"
    }
}
