package com.omnidapt.pd.data

enum class UserRole {
    Doctor,
    Patient,
    Admin
}

enum class PatientTab {
    Home,
    Report,
    Telehealth,
    Profile
}

enum class DoctorScreen {
    PatientList,
    Export,
    Settings,
    PatientInfo,
    ParameterAdjustment,
    RealtimeMonitor
}

enum class PatientListGroup {
    PendingInitialization,
    Focus,
    Routine
}

enum class PatientSortField {
    Name,
    Number,
    ImplantDate,
    Age
}

enum class InitializationStep {
    ElectrodeConfig,
    BaselineDetection,
    FrequencyExtraction,
    Completed
}

enum class ImpedanceSide {
    Left,
    Right
}

enum class ImpedanceMode {
    Monopolar,
    Bipolar
}

enum class DeviceConnectionState {
    Disconnected,
    Connecting,
    Connected
}

enum class ExportFileType {
    PatientReport,
    BrainSignal,
    ParameterRecord,
    TelehealthNote
}

enum class ExportFormat {
    PDF,
    CSV,
    MAT,
    EDF,
    EML,
    ZIP
}

data class Patient(
    val id: String,
    val name: String,
    val gender: String,
    val age: Int,
    val number: String
)

data class DoctorPatientRecord(
    val id: String,
    val name: String,
    val gender: String,
    val age: Int,
    val number: String,
    val implantDate: String,
    val summary: String,
    val group: PatientListGroup
)

data class DoctorSettings(
    val notificationsEnabled: Boolean = true,
    val autoConnectDevice: Boolean = true,
    val privacyMode: Boolean = true,
    val cloudSyncEnabled: Boolean = false,
    val language: String = "中文",
    val twoFactorEnabled: Boolean = false
)

data class ExportSettings(
    val includeIdentity: Boolean = true,
    val includePatientFeedback: Boolean = true,
    val includeSignalClips: Boolean = true,
    val includeParameterTimeline: Boolean = true
)

data class ExportFileRecord(
    val id: String,
    val patientId: String,
    val patientName: String,
    val type: ExportFileType,
    val format: ExportFormat,
    val fileName: String,
    val size: String,
    val generatedAt: String,
    val exported: Boolean = false
)

data class TelehealthMessage(
    val sender: String,
    val content: String,
    val time: String
)

data class TelehealthSession(
    val id: String,
    val patientId: String,
    val active: Boolean,
    val startedAt: String,
    val messages: List<TelehealthMessage>
)

data class DoctorActionLog(
    val patientId: String,
    val action: String,
    val time: String
)

data class RealtimeMonitorState(
    val paused: Boolean = false,
    val recording: Boolean = false,
    val displayMode: String = "脑电 + 用药 + 运动",
    val recordedSegments: Int = 0,
    val eventMarkers: List<String> = emptyList()
)

data class FrequencyBands(
    val staticBeta: String = "14.1-21.2 Hz",
    val motionBeta: String = "27.5-31.9 Hz",
    val gamma: String = "81.7-84.8 Hz"
)

data class ElectrodeSelection(
    val leftPositive: Int = 5,
    val leftNegative: Int = 6,
    val rightPositive: Int = 1,
    val rightNegative: Int = 2
) {
    fun isValid(): Boolean =
        leftPositive in 5..8 &&
            leftNegative in 5..8 &&
            rightPositive in 1..4 &&
            rightNegative in 1..4 &&
            leftPositive != leftNegative &&
            rightPositive != rightNegative
}

data class ImpedancePoint(
    val contact: String,
    val valueKOhm: Float
)

data class StimulationParameterDraft(
    val condition: String,
    val frequencyHz: Int,
    val amplitudeMv: Float,
    val pulseWidthUs: Int,
    val dutyCycle: Int
) {
    fun isSafe(): Boolean =
        frequencyHz in 90..170 &&
            amplitudeMv in 0.5f..3.5f &&
            pulseWidthUs in 40..90 &&
            dutyCycle in 20..80
}

data class BaselineSamplingState(
    val activeTask: Int = 0,
    val completedTasks: Set<Int> = emptySet(),
    val sampling: Boolean = false,
    val sampleEnded: Boolean = false,
    val elapsedSeconds: Int = 0
)

data class InitializationWorkflowState(
    val step: InitializationStep = InitializationStep.ElectrodeConfig,
    val electrodeSelection: ElectrodeSelection = ElectrodeSelection(),
    val stimulationParameters: List<StimulationParameterDraft> = defaultStimulationParameters(),
    val baseline: BaselineSamplingState = BaselineSamplingState(),
    val frequencyBands: FrequencyBands = FrequencyBands()
)

fun defaultStimulationParameters(): List<StimulationParameterDraft> = listOf(
    StimulationParameterDraft("药物失效-静息", 130, 2.5f, 60, 45),
    StimulationParameterDraft("药物失效-运动", 130, 2.8f, 65, 50),
    StimulationParameterDraft("药物生效-静息", 120, 1.6f, 55, 40),
    StimulationParameterDraft("药物生效-运动", 135, 2.1f, 60, 45)
)

data class ParameterOptimizationSettings(
    val tremorWeight: Float = 0.15f,
    val rigidityWeight: Float = 0.20f,
    val speechWeight: Float = 0.15f,
    val movementWeight: Float = 0.20f,
    val sideEffectWeight: Float = 0.15f,
    val comparisonWeight: Float = 0.15f,
    val currentMin: Float = 1.0f,
    val currentMax: Float = 3.0f,
    val frequencyMin: Int = 120,
    val frequencyMax: Int = 150,
    val pulseWidthMin: Int = 50,
    val pulseWidthMax: Int = 90,
    val dutyCycleMin: Int = 20,
    val dutyCycleMax: Int = 80,
    val optimizationRounds: Int = 7
)

data class SymptomFeedback(
    val tremor: Int,
    val rigidity: Int,
    val speech: Int,
    val note: String = ""
)

data class MedicationRecord(
    val date: String,
    val morningTaken: Boolean,
    val eveningTaken: Boolean
)

data class BrainSignalPoint(
    val second: Float,
    val microVolt: Float,
    val staticProbability: Float,
    val motionProbability: Float
)

data class TherapyParameters(
    val date: String,
    val currentMa: Float,
    val frequencyHz: Int,
    val pulseWidthUs: Int,
    val contact: String,
    val dutyCycle: Int
)

data class OptimizationSuggestion(
    val currentScore: Float,
    val suggestedParameters: TherapyParameters,
    val curve: List<Float>
)

data class AlertEvent(
    val date: String,
    val tremorCount: Int,
    val rigidityCount: Int,
    val dysarthriaCount: Int
)

data class PatientReport(
    val medications: List<MedicationRecord>,
    val alerts: List<AlertEvent>,
    val parameterHistory: List<TherapyParameters>,
    val latestFeedback: SymptomFeedback
)
