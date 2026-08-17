package com.omnidapt.pd.real.algorithm

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

val PD_STATE_LABELS = listOf("OFF-Rest", "OFF-Move", "ON-Rest", "ON-Move")

data class FeatureBands(
    val medicationBeta: ClosedFloatingPointRange<Double> = 15.0..19.0,
    val movementBeta: ClosedFloatingPointRange<Double> = 26.0..34.0,
    val movementGamma: ClosedFloatingPointRange<Double> = 81.0..89.0,
)

data class GmmComponent(
    val weight: Double,
    val mean: DoubleArray,
    val covariance: Array<DoubleArray>,
)

data class GmmState(val prior: Double, val components: List<GmmComponent>)

data class GmmModel(
    val id: String? = null,
    val sampleRateHz: Int = 256,
    val smoothingWindows: Int = 30,
    val probabilitySmoothingWindows: Int = 5,
    val bands: FeatureBands = FeatureBands(),
    val scalerMean: DoubleArray,
    val scalerScale: DoubleArray,
    val states: Map<String, GmmState>,
    val minConfidence: Double = 0.45,
    val minLogLikelihood: Double = -30.0,
) {
    companion object {
        fun fromJson(json: String, id: String? = null): GmmModel {
            val root = JsonParser.parseString(json).asJsonObject
            val selected = if (root.has("profiles")) {
                root.getAsJsonObject("profiles").getAsJsonObject("stable")
            } else {
                root
            }
            return fromObject(selected, id)
        }

        fun bundleFromJson(json: String, id: String? = null): GmmModelBundle {
            val root = JsonParser.parseString(json).asJsonObject
            if (!root.has("profiles")) {
                val stable = fromObject(root, id)
                return GmmModelBundle(stable.copy(smoothingWindows = 5, probabilitySmoothingWindows = 3), stable)
            }
            val profiles = root.getAsJsonObject("profiles")
            return GmmModelBundle(
                fast = fromObject(profiles.getAsJsonObject("fast"), id),
                stable = fromObject(profiles.getAsJsonObject("stable"), id),
            )
        }

        private fun fromObject(root: JsonObject, id: String? = null): GmmModel {
            val bands = root.getAsJsonObject("bands")
            val scaler = root.getAsJsonObject("scaler")
            val rejection = root.getAsJsonObject("rejection")
            return GmmModel(
                id = id,
                sampleRateHz = root["sample_rate_hz"].asInt,
                smoothingWindows = root["smoothing_windows"].asInt,
                probabilitySmoothingWindows =
                    root.get("probability_smoothing_windows")?.asInt ?: 5,
                bands = FeatureBands(
                    range(bands, "medication_beta"),
                    range(bands, "movement_beta"),
                    range(bands, "movement_gamma"),
                ),
                scalerMean = scaler["mean"].asJsonArray.map { it.asDouble }.toDoubleArray(),
                scalerScale = scaler["scale"].asJsonArray.map { it.asDouble }.toDoubleArray(),
                states = PD_STATE_LABELS.associateWith { label ->
                    val state = root.getAsJsonObject("states").getAsJsonObject(label)
                    GmmState(
                        prior = state["prior"].asDouble,
                        components = state["components"].asJsonArray.map { element ->
                            val component = element.asJsonObject
                            GmmComponent(
                                weight = component["weight"].asDouble,
                                mean = component["mean"].asJsonArray.map { it.asDouble }.toDoubleArray(),
                                covariance = component["covariance"].asJsonArray.map { row ->
                                    row.asJsonArray.map { it.asDouble }.toDoubleArray()
                                }.toTypedArray(),
                            )
                        },
                    )
                },
                minConfidence = rejection?.get("min_confidence")?.asDouble ?: 0.45,
                minLogLikelihood = rejection?.get("min_log_likelihood")?.asDouble ?: -30.0,
            )
        }

        fun default(): GmmModel {
            val means = listOf(
                doubleArrayOf(0.30, 0.40, 1.25, 0.45, 0.20),
                doubleArrayOf(0.42, 0.48, 1.05, 1.10, 0.72),
                doubleArrayOf(-0.10, 0.18, 0.42, 0.32, 0.18),
                doubleArrayOf(0.08, 0.20, 0.38, 0.95, 1.15),
            )
            val variances = listOf(
                doubleArrayOf(0.30, 0.30, 0.45, 0.35, 0.25),
                doubleArrayOf(0.32, 0.32, 0.42, 0.45, 0.36),
                doubleArrayOf(0.28, 0.24, 0.35, 0.28, 0.22),
                doubleArrayOf(0.30, 0.28, 0.32, 0.42, 0.46),
            )
            return GmmModel(
                scalerMean = DoubleArray(5),
                scalerScale = DoubleArray(5) { 1.0 },
                states = PD_STATE_LABELS.mapIndexed { index, label ->
                    label to GmmState(
                        0.25,
                        listOf(GmmComponent(1.0, means[index], diagonal(variances[index]))),
                    )
                }.toMap(),
            )
        }

        private fun range(objectValue: JsonObject, key: String): ClosedFloatingPointRange<Double> {
            val values = objectValue[key].asJsonArray
            return values[0].asDouble..values[1].asDouble
        }

        private fun diagonal(values: DoubleArray): Array<DoubleArray> =
            Array(values.size) { row -> DoubleArray(values.size) { column -> if (row == column) values[row] else 0.0 } }
    }
}

data class GmmModelBundle(
    val fast: GmmModel,
    val stable: GmmModel,
)

data class StateInference(
    val features: DoubleArray,
    val probabilities: Map<String, Double>,
    val topState: String,
    val confidence: Double,
    val logLikelihood: Double,
    val rejected: Boolean,
)

/** Arithmetic probability smoothing with confidence rejection; this is deliberately not labelled HMM. */
class ProbabilitySmoother(private val windowSize: Int = 5) {
    private val history = ArrayDeque<Map<String, Double>>()

    fun push(inference: StateInference): StateInference {
        history.addLast(inference.probabilities)
        if (history.size > windowSize) history.removeFirst()
        val probabilities = PD_STATE_LABELS.associateWith { label ->
            history.map { it.getValue(label) }.average()
        }
        val top = probabilities.maxBy { it.value }
        return inference.copy(
            probabilities = probabilities,
            topState = top.key,
            confidence = top.value,
            rejected = inference.rejected || top.value < 0.45,
        )
    }
}

class StreamingFeatureExtractor(
    private val sampleRateHz: Int = 256,
    private val smoothingWindows: Int = 30,
    private val bands: FeatureBands = FeatureBands(),
) {
    private val history = Array(5) { ArrayDeque<Double>() }
    val warmedUp: Boolean get() = history[0].size >= smoothingWindows

    fun push(channels: Array<DoubleArray>): DoubleArray {
        return pushRaw(extractRawSpectralFeatures(channels, sampleRateHz, bands))
    }

    fun pushRaw(raw: DoubleArray): DoubleArray {
        require(raw.size == 5)
        raw.forEachIndexed { index, value ->
            history[index].addLast(value)
            if (history[index].size > smoothingWindows) history[index].removeFirst()
        }
        return doubleArrayOf(
            raw[0] - history[0].average(),
            history[1].average(),
            history[2].average(),
            history[3].average(),
            history[4].average(),
        )
    }
}

fun extractRawSpectralFeatures(
    channelsMicrovolts: Array<DoubleArray>,
    sampleRateHz: Int = 256,
    bands: FeatureBands = FeatureBands(),
): DoubleArray {
    require(channelsMicrovolts.isNotEmpty())
    require(channelsMicrovolts.all { it.size == sampleRateHz }) {
        "feature extraction requires exactly one second per channel"
    }
    val nfft = sampleRateHz * 2
    val window = DoubleArray(sampleRateHz) { index ->
        0.5 - 0.5 * cos(2.0 * PI * index / sampleRateHz)
    }
    val windowPower = window.sumOf { it * it }
    val meanPower = DoubleArray(nfft / 2 + 1)
    for (channel in channelsMicrovolts) {
        val channelMean = channel.average()
        for (bin in meanPower.indices) {
            var real = 0.0
            var imaginary = 0.0
            for (sample in channel.indices) {
                val valueVolts = (channel[sample] - channelMean) * 1e-6 * window[sample]
                val angle = -2.0 * PI * bin * sample / nfft
                real += valueVolts * cos(angle)
                imaginary += valueVolts * sin(angle)
            }
            var density = (real * real + imaginary * imaginary) / (sampleRateHz * windowPower)
            if (bin != 0 && bin != nfft / 2) density *= 2.0
            meanPower[bin] += density / channelsMicrovolts.size
        }
    }
    val frequencies = DoubleArray(197) { 2.0 + it * 0.5 }
    val logFrequency = frequencies.map(::log10)
    val logPower = frequencies.map { frequency ->
        val bin = (frequency / 0.5).toInt()
        log10(max(meanPower[bin], Double.MIN_VALUE))
    }
    val fitIndices = frequencies.indices.filter { index ->
        val frequency = frequencies[index]
        frequency < 13.0 || (frequency > 35.0 && frequency < 60.0) || frequency > 90.0
    }
    val xMean = fitIndices.map { logFrequency[it] }.average()
    val yMean = fitIndices.map { logPower[it] }.average()
    val slope = fitIndices.sumOf { (logFrequency[it] - xMean) * (logPower[it] - yMean) } /
        fitIndices.sumOf { (logFrequency[it] - xMean) * (logFrequency[it] - xMean) }
    val offset = yMean - slope * xMean
    val periodic = DoubleArray(frequencies.size) { index ->
        max(logPower[index] - (slope * logFrequency[index] + offset), 0.0)
    }
    fun bandSum(range: ClosedFloatingPointRange<Double>): Double =
        frequencies.indices.sumOf { index ->
            if (frequencies[index] in range) periodic[index] else 0.0
        }
    return doubleArrayOf(
        offset,
        -slope,
        bandSum(bands.medicationBeta),
        bandSum(bands.movementBeta),
        bandSum(bands.movementGamma),
    )
}

fun inferState(features: DoubleArray, model: GmmModel): StateInference {
    require(features.size == 5)
    val normalized = DoubleArray(5) { index ->
        (features[index] - model.scalerMean[index]) / max(model.scalerScale[index], 1e-9)
    }
    val scores = PD_STATE_LABELS.map { label ->
        val state = requireNotNull(model.states[label])
        val componentScores = state.components.map { component ->
            ln(max(component.weight, 1e-12)) + logGaussian(normalized, component)
        }
        ln(max(state.prior, 1e-12)) + logSumExp(componentScores)
    }
    val logLikelihood = logSumExp(scores)
    val probabilities = scores.map { exp(it - logLikelihood) }
    val best = probabilities.indices.maxBy { probabilities[it] }
    val confidence = probabilities[best]
    return StateInference(
        features = features,
        probabilities = PD_STATE_LABELS.zip(probabilities).toMap(),
        topState = PD_STATE_LABELS[best],
        confidence = confidence,
        logLikelihood = logLikelihood,
        rejected = confidence < model.minConfidence || logLikelihood < model.minLogLikelihood,
    )
}

private fun logGaussian(x: DoubleArray, component: GmmComponent): Double {
    val size = x.size
    val matrix = Array(size) { row ->
        DoubleArray(size) { column ->
            component.covariance[row][column] + if (row == column) 1e-9 else 0.0
        }
    }
    val lower = cholesky(matrix) ?: Array(size) { row -> DoubleArray(size) { column -> if (row == column) 1.0 else 0.0 } }
    val delta = DoubleArray(size) { x[it] - component.mean[it] }
    val solved = forwardSolve(lower, delta)
    val quadratic = solved.sumOf { it * it }
    val logDeterminant = 2.0 * lower.indices.sumOf { ln(max(lower[it][it], 1e-300)) }
    return -0.5 * (size * ln(2.0 * PI) + logDeterminant + quadratic)
}

private fun cholesky(matrix: Array<DoubleArray>): Array<DoubleArray>? {
    val size = matrix.size
    val lower = Array(size) { DoubleArray(size) }
    for (row in 0 until size) {
        for (column in 0..row) {
            val sum = (0 until column).sumOf { lower[row][it] * lower[column][it] }
            if (row == column) {
                val value = matrix[row][row] - sum
                if (value <= 0.0) return null
                lower[row][column] = sqrt(value)
            } else {
                lower[row][column] = (matrix[row][column] - sum) / lower[column][column]
            }
        }
    }
    return lower
}

private fun forwardSolve(lower: Array<DoubleArray>, value: DoubleArray): DoubleArray {
    val result = DoubleArray(value.size)
    for (row in value.indices) {
        result[row] = (value[row] - (0 until row).sumOf { lower[row][it] * result[it] }) / lower[row][row]
    }
    return result
}

private fun logSumExp(values: List<Double>): Double {
    val maximum = values.max()
    return maximum + ln(values.sumOf { exp(it - maximum) })
}
