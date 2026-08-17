package com.omnidapt.pd.real.network

data class LoginBody(val username: String, val password: String)
data class RefreshBody(val refresh_token: String)
data class ChangePasswordBody(val current_password: String, val new_password: String)
data class AdminUserBody(
    val username: String,
    val temporary_password: String,
    val role: String,
    val display_name: String,
    val patient_code: String? = null,
)
data class PasswordResetBody(val temporary_password: String)
data class CareBindingBody(val doctor_user_id: String, val patient_id: String)
data class DeviceCreateBody(
    val serial_number: String,
    val name: String = "Ominidapt BLE Simulator",
    val simulated: Boolean = true,
)
data class DeviceBindingBody(val patient_id: String, val device_id: String)
data class ChatSessionBody(val patient_id: String, val doctor_user_id: String)
data class ApiChatSession(val id: String, val patient_id: String, val doctor_user_id: String)
data class ChatMessageBody(val event_id: String, val content: String)
data class ApiChatMessage(
    val id: String,
    val event_id: String,
    val sender_user_id: String,
    val content: String,
    val created_at: String,
)
data class LfpSessionBody(
    val patient_id: String,
    val device_id: String,
    val purpose: String = "manual_recording",
    val state_label: String? = null,
    val sample_rate_hz: Int = 256,
    val channels: Int = 2,
    val recording_enabled: Boolean = true,
)
data class LfpSessionResponse(val id: String, val recording_enabled: Boolean)
data class LfpCompleteBody(val sample_count: Int, val packet_loss_count: Int)
data class InitializationBody(
    val patient_id: String,
    val device_id: String,
    val mode: String,
    val electrode_config: Map<String, Any> = emptyMap(),
)
data class InitializationSegmentBody(
    val lfp_session_id: String,
    val state_label: String,
    val received_frames: Int,
    val packet_loss_count: Int,
    val crc_error_count: Int,
    val saturated_sample_count: Int,
    val impedance: Map<String, Float> = emptyMap(),
)
data class InitializationApproveBody(val note: String = "")
data class ApiInitializationSegment(
    val id: String,
    val lfp_session_id: String,
    val state_label: String,
    val order_index: Int,
    val sample_count: Int,
    val received_frames: Int,
    val packet_loss_count: Int,
    val crc_error_count: Int,
    val saturated_sample_count: Int,
    val impedance: Map<String, Any>,
    val quality: Map<String, Any>,
    val accepted: Boolean,
)
data class ApiInitialization(
    val id: String,
    val patient_id: String,
    val device_id: String,
    val mode: String,
    val status: String,
    val current_state: String?,
    val settle_seconds: Int,
    val capture_seconds: Int,
    val electrode_config: Map<String, Any>,
    val quality_summary: Map<String, Any>,
    val frequency_results: Map<String, Any>,
    val analysis_stage: String = "idle",
    val progress_percent: Int = 0,
    val model_version_id: String?,
    val error: String?,
    val segments: List<ApiInitializationSegment>,
)

data class ApiUser(
    val id: String,
    val username: String,
    val role: String,
    val display_name: String,
    val active: Boolean,
    val must_change_password: Boolean,
)

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in_seconds: Int,
    val user: ApiUser,
)

data class ApiPatient(
    val id: String,
    val user_id: String?,
    val code: String,
    val name: String,
    val gender: String,
    val age: Int?,
    val implant_date: String?,
    val summary: String,
    val emergency_contact: String?,
    val emergency_phone: String?,
)

data class SymptomBody(
    val event_id: String,
    val patient_id: String?,
    val tremor: Int,
    val rigidity: Int,
    val speech: Int,
    val note: String = "",
    val recorded_at: String? = null,
)

data class MedicationBody(
    val event_id: String,
    val patient_id: String?,
    val medication_name: String = "左旋多巴",
    val status: String,
    val recorded_at: String? = null,
)

data class InferenceBody(
    val event_id: String,
    val patient_id: String,
    val model_version_id: String?,
    val features: List<Double>,
    val probabilities: Map<String, Double>,
    val top_state: String,
    val confidence: Double,
    val rejected: Boolean,
    val recorded_at: String? = null,
)

data class ApiDevice(
    val id: String,
    val serial_number: String,
    val name: String,
    val simulated: Boolean,
    val protocol_version: Int,
    val battery_percent: Int,
)

data class ApiModel(
    val id: String,
    val patient_id: String,
    val version: Int,
    val status: String,
    val payload: Map<String, Any>,
)

data class PendingCommand(
    val id: String,
    val device_id: String,
    val sequence: Long,
    val payload: Map<String, Double>,
    val status: String,
)

data class DeviceAckBody(
    val command_id: String,
    val sequence: Long,
    val success: Boolean,
    val status_code: String,
    val detail: String = "",
)

data class ExportBody(val patient_id: String, val format: String)
data class ProposalReviewBody(val action: String, val note: String = "")
data class ApiProposal(
    val id: String,
    val task_id: String,
    val patient_id: String,
    val status: String,
    val parameters: Map<String, Double>,
    val score: Double?,
    val safety_result: Map<String, Any>,
    val model_version: String?,
    val review_note: String?,
    val round_index: Int = 1,
    val acquisition: Map<String, Any> = emptyMap(),
)

data class OptimizationTaskBody(
    val patient_id: String,
    val settings: Map<String, Any>,
    val safety_bounds: Map<String, Double>,
    val rounds: Int,
    val observation_seconds: Int = 30,
    val current_parameters: Map<String, Double>,
)

data class OptimizationFeedbackBody(
    val event_id: String,
    val task_id: String,
    val answers: Map<String, Double>,
    val side_effects: Map<String, Double>,
    val parameters: Map<String, Double>,
)

data class ApiOptimizationObservation(
    val round_index: Int,
    val current_ma: Double,
    val score: Double,
)

data class ApiOptimizationChart(
    val grid_current_ma: List<Double> = emptyList(),
    val mean: List<Double> = emptyList(),
    val std: List<Double> = emptyList(),
    val expected_improvement: List<Double> = emptyList(),
    val best_score: Double? = null,
    val next_current_ma: Double? = null,
    val observations: List<ApiOptimizationObservation> = emptyList(),
)

data class ApiOptimizationFeedback(
    val id: String,
    val round_index: Int,
    val answers: Map<String, Double>,
    val side_effects: Map<String, Double>,
    val score: Double,
    val blocked: Boolean,
    val parameters: Map<String, Double>,
)

data class ApiOptimizationTask(
    val id: String,
    val patient_id: String,
    val status: String,
    val settings: Map<String, Any>,
    val safety_bounds: Map<String, Double>,
    val rounds: Int,
    val current_round: Int,
    val observation_seconds: Int,
    val eligible_at: String?,
    val questionnaire_unlocked: Boolean,
    val current_parameters: Map<String, Double>,
    val best_parameters: Map<String, Double>,
    val excluded_currents: List<Double>,
    val feedback: List<ApiOptimizationFeedback>,
    val proposals: List<ApiProposal>,
    val chart: ApiOptimizationChart,
)

data class OptimizationFeedbackResponse(
    val feedback_id: String,
    val proposal_id: String?,
    val status: String?,
    val deduplicated: Boolean,
    val task: ApiOptimizationTask,
)

data class ExportResponse(
    val id: String,
    val status: String,
    val format: String,
    val object_key: String?,
    val error: String?,
)

data class IdempotentResponse(val id: String, val deduplicated: Boolean)
