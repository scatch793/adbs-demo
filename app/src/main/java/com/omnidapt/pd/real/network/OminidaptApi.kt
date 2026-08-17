package com.omnidapt.pd.real.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call

interface OminidaptApi {
    @GET("health")
    suspend fun health(): Map<String, Any>

    @POST("auth/login")
    suspend fun login(@Body body: LoginBody): TokenResponse

    @POST("auth/refresh")
    fun refresh(@Body body: RefreshBody): Call<TokenResponse>

    @GET("auth/me")
    suspend fun me(): ApiUser

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordBody): TokenResponse

    @GET("admin/users")
    suspend fun users(): List<ApiUser>

    @POST("admin/users")
    suspend fun createUser(@Body body: AdminUserBody): ApiUser

    @POST("admin/users/{userId}/reset-password")
    suspend fun resetPassword(
        @Path("userId") userId: String,
        @Body body: PasswordResetBody,
    ): ApiUser

    @POST("admin/care-relations")
    suspend fun bindCare(@Body body: CareBindingBody): Map<String, String>

    @POST("admin/devices")
    suspend fun createDevice(@Body body: DeviceCreateBody): ApiDevice

    @POST("devices/bindings")
    suspend fun bindDevice(@Body body: DeviceBindingBody): Map<String, Any>

    @GET("patients")
    suspend fun patients(): List<ApiPatient>

    @GET("patients/{patientId}/care-team")
    suspend fun careTeam(@Path("patientId") patientId: String): List<ApiUser>

    @POST("symptoms")
    suspend fun createSymptom(@Body body: SymptomBody): IdempotentResponse

    @GET("symptoms")
    suspend fun symptoms(@Query("patient_id") patientId: String? = null): List<Map<String, Any>>

    @POST("medications")
    suspend fun createMedication(@Body body: MedicationBody): IdempotentResponse

    @GET("medications")
    suspend fun medications(@Query("patient_id") patientId: String? = null): List<Map<String, Any>>

    @POST("inferences")
    suspend fun createInference(@Body body: InferenceBody): IdempotentResponse

    @POST("lfp-sessions")
    suspend fun createLfpSession(@Body body: LfpSessionBody): LfpSessionResponse

    @Multipart
    @PUT("lfp-sessions/{sessionId}/waveform")
    suspend fun uploadLfpWaveform(
        @Path("sessionId") sessionId: String,
        @Part waveform: MultipartBody.Part,
    ): Map<String, Any>

    @POST("lfp-sessions/{sessionId}/complete")
    suspend fun completeLfpSession(
        @Path("sessionId") sessionId: String,
        @Body body: LfpCompleteBody,
    ): Map<String, Any>

    @POST("initializations")
    suspend fun createInitialization(@Body body: InitializationBody): ApiInitialization

    @GET("initializations/{initializationId}")
    suspend fun initialization(
        @Path("initializationId") initializationId: String,
    ): ApiInitialization

    @GET("patients/{patientId}/initialization")
    suspend fun initializations(
        @Path("patientId") patientId: String,
    ): List<ApiInitialization>

    @POST("initializations/{initializationId}/segments")
    suspend fun attachInitializationSegment(
        @Path("initializationId") initializationId: String,
        @Body body: InitializationSegmentBody,
    ): ApiInitialization

    @POST("initializations/{initializationId}/analyze")
    suspend fun analyzeInitialization(
        @Path("initializationId") initializationId: String,
    ): ApiInitialization

    @POST("initializations/{initializationId}/approve")
    suspend fun approveInitialization(
        @Path("initializationId") initializationId: String,
        @Body body: InitializationApproveBody,
    ): ApiInitialization

    @GET("devices")
    suspend fun devices(@Query("patient_id") patientId: String? = null): List<ApiDevice>

    @GET("models")
    suspend fun models(@Query("patient_id") patientId: String): List<ApiModel>

    @GET("devices/commands/pending")
    suspend fun pendingCommands(@Query("patient_id") patientId: String? = null): List<PendingCommand>

    @POST("devices/commands/{commandId}/ack")
    suspend fun acknowledge(
        @Path("commandId") commandId: String,
        @Body body: DeviceAckBody,
    ): Map<String, Any>

    @POST("exports")
    suspend fun createExport(@Body body: ExportBody): ExportResponse

    @POST("chat-sessions")
    suspend fun createChatSession(@Body body: ChatSessionBody): ApiChatSession

    @GET("chat-sessions/{sessionId}/messages")
    suspend fun chatMessages(@Path("sessionId") sessionId: String): List<ApiChatMessage>

    @POST("chat-sessions/{sessionId}/messages")
    suspend fun sendChatMessage(
        @Path("sessionId") sessionId: String,
        @Body body: ChatMessageBody,
    ): ApiChatMessage

    @GET("parameter-proposals")
    suspend fun proposals(@Query("patient_id") patientId: String): List<ApiProposal>

    @POST("optimization-tasks")
    suspend fun createOptimizationTask(@Body body: OptimizationTaskBody): ApiOptimizationTask

    @GET("optimization-tasks")
    suspend fun optimizationTasks(
        @Query("patient_id") patientId: String? = null,
    ): List<ApiOptimizationTask>

    @GET("optimization-tasks/{taskId}")
    suspend fun optimizationTask(@Path("taskId") taskId: String): ApiOptimizationTask

    @POST("optimization-tasks/{taskId}/feedback")
    suspend fun submitOptimizationFeedback(
        @Path("taskId") taskId: String,
        @Body body: OptimizationFeedbackBody,
    ): OptimizationFeedbackResponse

    @POST("approvals/{proposalId}")
    suspend fun reviewProposal(
        @Path("proposalId") proposalId: String,
        @Body body: ProposalReviewBody,
    ): Map<String, Any>

    @Streaming
    @GET("exports/{jobId}/download")
    suspend fun downloadExport(@Path("jobId") jobId: String): ResponseBody
}
