package com.omnidapt.pd.real.algorithm

import com.omnidapt.pd.real.RealRepository
import com.omnidapt.pd.real.ble.BleCentralClient
import com.omnidapt.pd.real.network.InferenceBody
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EdgeInferenceSnapshot(
    val fastWarmedWindows: Int = 0,
    val stableWarmedWindows: Int = 0,
    val topState: String? = null,
    val confidence: Double? = null,
    val rejected: Boolean = false,
    val probabilities: Map<String, Double> = emptyMap(),
    val fastProbabilities: Map<String, Double> = emptyMap(),
    val stableProbabilities: Map<String, Double> = emptyMap(),
    val fastRejected: Boolean = true,
    val stableRejected: Boolean = true,
    val modelVersionId: String? = null,
    val lastError: String? = null,
)

class EdgeInferenceController(
    private val bleClient: BleCentralClient,
    private val repository: RealRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableSnapshot = MutableStateFlow(EdgeInferenceSnapshot())
    val snapshot: StateFlow<EdgeInferenceSnapshot> = mutableSnapshot
    private var collectionJob: Job? = null

    fun start(patientId: String) {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            val cached = repository.latestApprovedModel(patientId)
            val bundle = cached?.let {
                runCatching { GmmModel.bundleFromJson(it.payloadJson, it.id) }
                    .onFailure { error ->
                        mutableSnapshot.update { current ->
                            current.copy(lastError = "模型解析失败，已使用内置非临床回退模型：${error.message}")
                        }
                    }
                    .getOrNull()
            } ?: GmmModelBundle(
                fast = GmmModel.default().copy(smoothingWindows = 5, probabilitySmoothingWindows = 3),
                stable = GmmModel.default(),
            )
            val stableModel = bundle.stable
            val fastModel = bundle.fast
            val fastExtractor = StreamingFeatureExtractor(
                sampleRateHz = fastModel.sampleRateHz,
                smoothingWindows = fastModel.smoothingWindows,
                bands = fastModel.bands,
            )
            val stableExtractor = StreamingFeatureExtractor(
                sampleRateHz = stableModel.sampleRateHz,
                smoothingWindows = stableModel.smoothingWindows,
                bands = stableModel.bands,
            )
            val fastProbabilitySmoother = ProbabilitySmoother(fastModel.probabilitySmoothingWindows)
            val stableProbabilitySmoother = ProbabilitySmoother(stableModel.probabilitySmoothingWindows)
            var channels = Array(2) { ArrayList<Double>(stableModel.sampleRateHz) }
            var previousTimestampMs: Long? = null
            bleClient.lfp.collect { (frame, chunk) ->
                if (chunk.sampleRateHz != stableModel.sampleRateHz || chunk.channelCount != 2) {
                    mutableSnapshot.update {
                        it.copy(lastError = "拒绝不兼容数据：需要 ${stableModel.sampleRateHz} Hz 双通道")
                    }
                    return@collect
                }
                val elapsed = previousTimestampMs?.let { frame.timestampMs - it }
                if (elapsed != null && elapsed !in 40..250) {
                    channels = Array(2) { ArrayList(stableModel.sampleRateHz) }
                }
                previousTimestampMs = frame.timestampMs
                for (sample in 0 until chunk.sampleCount) {
                    for (channel in 0 until 2) {
                        channels[channel].add(chunk.samples[sample * 2 + channel].toDouble())
                    }
                    if (channels[0].size == stableModel.sampleRateHz) {
                        val samples = Array(2) { channel -> channels[channel].toDoubleArray() }
                        val raw = extractRawSpectralFeatures(
                            samples,
                            stableModel.sampleRateHz,
                            stableModel.bands,
                        )
                        channels = Array(2) { ArrayList(stableModel.sampleRateHz) }
                        val fastFeatures = fastExtractor.pushRaw(raw)
                        val stableFeatures = stableExtractor.pushRaw(raw)
                        val fastInference = fastProbabilitySmoother.push(
                            inferState(fastFeatures, fastModel),
                        )
                        val stableInference = stableProbabilitySmoother.push(
                            inferState(stableFeatures, stableModel),
                        )
                        val fastWarmed = (mutableSnapshot.value.fastWarmedWindows + 1)
                            .coerceAtMost(fastModel.smoothingWindows)
                        val stableWarmed = (mutableSnapshot.value.stableWarmedWindows + 1)
                            .coerceAtMost(stableModel.smoothingWindows)
                        mutableSnapshot.value = EdgeInferenceSnapshot(
                            fastWarmedWindows = fastWarmed,
                            stableWarmedWindows = stableWarmed,
                            topState = stableInference.topState,
                            confidence = stableInference.confidence,
                            rejected = !stableExtractor.warmedUp || stableInference.rejected,
                            probabilities = stableInference.probabilities,
                            fastProbabilities = fastInference.probabilities,
                            stableProbabilities = stableInference.probabilities,
                            fastRejected = !fastExtractor.warmedUp || fastInference.rejected,
                            stableRejected = !stableExtractor.warmedUp || stableInference.rejected,
                            modelVersionId = stableModel.id,
                            lastError = null,
                        )
                        repository.enqueueInference(
                            InferenceBody(
                                event_id = UUID.randomUUID().toString(),
                                patient_id = patientId,
                                model_version_id = stableModel.id,
                                features = stableInference.features.toList(),
                                probabilities = stableInference.probabilities,
                                top_state = stableInference.topState,
                                confidence = stableInference.confidence,
                                rejected = !stableExtractor.warmedUp || stableInference.rejected,
                                recorded_at = Instant.now().toString(),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }
}
