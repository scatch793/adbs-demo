package com.omnidapt.pd.real

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.omnidapt.pd.real.local.CachedPatientEntity
import com.omnidapt.pd.real.local.CachedModelEntity
import com.omnidapt.pd.real.local.OminidaptDatabase
import com.omnidapt.pd.real.local.PendingEventEntity
import com.omnidapt.pd.real.network.ApiFactory
import com.omnidapt.pd.real.network.ApiPatient
import com.omnidapt.pd.real.network.ApiModel
import com.omnidapt.pd.real.network.ApiUser
import com.omnidapt.pd.real.network.AdminUserBody
import com.omnidapt.pd.real.network.ChangePasswordBody
import com.omnidapt.pd.real.network.InferenceBody
import com.omnidapt.pd.real.network.LoginBody
import com.omnidapt.pd.real.network.MedicationBody
import com.omnidapt.pd.real.network.PendingCommand
import com.omnidapt.pd.real.network.DeviceAckBody
import com.omnidapt.pd.real.network.ApiProposal
import com.omnidapt.pd.real.network.ExportBody
import com.omnidapt.pd.real.network.ProposalReviewBody
import com.omnidapt.pd.real.network.CareBindingBody
import com.omnidapt.pd.real.network.DeviceBindingBody
import com.omnidapt.pd.real.network.DeviceCreateBody
import com.omnidapt.pd.real.network.ApiDevice
import com.omnidapt.pd.real.network.ApiChatMessage
import com.omnidapt.pd.real.network.ChatMessageBody
import com.omnidapt.pd.real.network.ChatSessionBody
import java.io.File
import com.omnidapt.pd.real.network.LfpCompleteBody
import com.omnidapt.pd.real.network.LfpSessionBody
import com.omnidapt.pd.real.network.ApiInitialization
import com.omnidapt.pd.real.network.InitializationApproveBody
import com.omnidapt.pd.real.network.InitializationBody
import com.omnidapt.pd.real.network.InitializationSegmentBody
import com.omnidapt.pd.real.network.ApiOptimizationTask
import com.omnidapt.pd.real.network.OptimizationFeedbackBody
import com.omnidapt.pd.real.network.OptimizationFeedbackResponse
import com.omnidapt.pd.real.network.OptimizationTaskBody
import com.omnidapt.pd.real.recording.NpzWriter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.omnidapt.pd.real.network.SymptomBody
import com.omnidapt.pd.real.security.AuthSession
import com.omnidapt.pd.real.security.SecureSessionStore
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = OminidaptDatabase.get(appContext)
    private val sessionStore = SecureSessionStore(appContext)
    private val apiFactory = ApiFactory(sessionStore)
    private val gson = Gson()
    private val webSocketClient = OkHttpClient.Builder().build()

    fun currentSession(): AuthSession? = sessionStore.load()

    fun serverUrl(): String = sessionStore.serverUrl

    suspend fun testServer(url: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            apiFactory.create(authenticated = false, baseUrl = url).health()
        }
        sessionStore.serverUrl = url
    }

    suspend fun login(serverUrl: String, username: String, password: String): Result<AuthSession> =
        runCatching {
            sessionStore.serverUrl = serverUrl
            val response = withContext(Dispatchers.IO) {
                apiFactory.create(authenticated = false).login(LoginBody(username, password))
            }
            val session = AuthSession(
                accessToken = response.access_token,
                refreshToken = response.refresh_token,
                userId = response.user.id,
                username = response.user.username,
                displayName = response.user.display_name,
                role = response.user.role,
                mustChangePassword = response.user.must_change_password,
            )
            sessionStore.save(session)
            if (!session.mustChangePassword) {
                refreshPatients()
                scheduleSync()
            }
            session
        }

    fun logout() {
        sessionStore.clear()
    }

    suspend fun changePassword(current: String, replacement: String): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiFactory.create().changePassword(ChangePasswordBody(current, replacement))
        }
        sessionStore.save(
            AuthSession(
                accessToken = response.access_token,
                refreshToken = response.refresh_token,
                userId = response.user.id,
                username = response.user.username,
                displayName = response.user.display_name,
                role = response.user.role,
                mustChangePassword = response.user.must_change_password,
            ),
        )
    }

    suspend fun adminUsers(): List<ApiUser> =
        withContext(Dispatchers.IO) { apiFactory.create().users() }

    suspend fun createUser(body: AdminUserBody): Result<ApiUser> = runCatching {
        withContext(Dispatchers.IO) { apiFactory.create().createUser(body) }
    }

    suspend fun resetPassword(userId: String, temporaryPassword: String): Result<ApiUser> =
        runCatching {
            withContext(Dispatchers.IO) {
                apiFactory.create().resetPassword(
                    userId,
                    com.omnidapt.pd.real.network.PasswordResetBody(temporaryPassword),
                )
            }
        }

    suspend fun bindCare(doctorUserId: String, patientId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            apiFactory.create().bindCare(CareBindingBody(doctorUserId, patientId))
        }
    }

    suspend fun createAndBindSimulator(serial: String, patientId: String): Result<ApiDevice> =
        runCatching {
            withContext(Dispatchers.IO) {
                val api = apiFactory.create()
                val device = api.createDevice(DeviceCreateBody(serial_number = serial))
                check(device.simulated) { "server returned a non-simulated device" }
                api.bindDevice(DeviceBindingBody(patientId, device.id))
                device
            }
        }

    suspend fun refreshPatients(): List<ApiPatient> {
        val remote = withContext(Dispatchers.IO) { apiFactory.create().patients() }
        database.patients().clear()
        database.patients().replaceAll(
            remote.map {
                CachedPatientEntity(
                    id = it.id,
                    code = it.code,
                    name = it.name,
                    gender = it.gender,
                    age = it.age,
                    implantDate = it.implant_date,
                    summary = it.summary,
                    emergencyContact = it.emergency_contact,
                    emergencyPhone = it.emergency_phone,
                    updatedAtMs = System.currentTimeMillis(),
                )
            },
        )
        remote.forEach { patient ->
            runCatching { refreshModels(patient.id) }
        }
        return remote
    }

    suspend fun refreshModels(patientId: String): List<ApiModel> {
        val remote = withContext(Dispatchers.IO) { apiFactory.create().models(patientId) }
        remote.forEach { model ->
            database.models().upsert(
                CachedModelEntity(
                    id = model.id,
                    patientId = model.patient_id,
                    version = model.version,
                    payloadJson = gson.toJson(model.payload),
                    approved = model.status == "approved",
                    updatedAtMs = System.currentTimeMillis(),
                ),
            )
        }
        return remote
    }

    suspend fun latestApprovedModel(patientId: String): CachedModelEntity? =
        database.models().latestApproved(patientId)

    suspend fun cachedPatients(): List<CachedPatientEntity> = database.patients().all()

    suspend fun enqueueSymptom(
        patientId: String?,
        tremor: Int,
        rigidity: Int,
        speech: Int,
        note: String = "",
    ): String {
        val eventId = UUID.randomUUID().toString()
        enqueue(
            eventId,
            EVENT_SYMPTOM,
            SymptomBody(
                event_id = eventId,
                patient_id = patientId,
                tremor = tremor,
                rigidity = rigidity,
                speech = speech,
                note = note,
                recorded_at = Instant.now().toString(),
            ),
        )
        return eventId
    }

    suspend fun enqueueMedication(patientId: String?, status: String): String {
        val eventId = UUID.randomUUID().toString()
        enqueue(
            eventId,
            EVENT_MEDICATION,
            MedicationBody(
                event_id = eventId,
                patient_id = patientId,
                status = status,
                recorded_at = Instant.now().toString(),
            ),
        )
        return eventId
    }

    suspend fun enqueueInference(body: InferenceBody) {
        enqueue(body.event_id, EVENT_INFERENCE, body)
    }

    suspend fun syncPending(): Int {
        if (sessionStore.load() == null) return 0
        val api = apiFactory.create()
        var completed = 0
        for (event in database.pendingEvents().pending()) {
            runCatching {
                when (event.type) {
                    EVENT_SYMPTOM -> api.createSymptom(
                        gson.fromJson(event.payloadJson, SymptomBody::class.java),
                    )
                    EVENT_MEDICATION -> api.createMedication(
                        gson.fromJson(event.payloadJson, MedicationBody::class.java),
                    )
                    EVENT_INFERENCE -> api.createInference(
                        gson.fromJson(event.payloadJson, InferenceBody::class.java),
                    )
                    else -> error("unsupported pending event type: ${event.type}")
                }
            }.onSuccess {
                database.pendingEvents().delete(event.eventId)
                completed++
            }.onFailure {
                database.pendingEvents().markFailure(event.eventId, it.message.orEmpty().take(500))
                return completed
            }
        }
        return completed
    }

    suspend fun pendingCount(): Int = database.pendingEvents().count()

    suspend fun symptomHistory(patientId: String): List<Map<String, Any>> =
        withContext(Dispatchers.IO) { apiFactory.create().symptoms(patientId) }

    suspend fun medicationHistory(patientId: String): List<Map<String, Any>> =
        withContext(Dispatchers.IO) { apiFactory.create().medications(patientId) }

    suspend fun pendingCommands(patientId: String): List<PendingCommand> =
        withContext(Dispatchers.IO) { apiFactory.create().pendingCommands(patientId) }

    suspend fun acknowledgeCommand(
        commandId: String,
        sequence: Long,
        success: Boolean,
        statusCode: String,
        detail: String = "",
    ) {
        withContext(Dispatchers.IO) {
            apiFactory.create().acknowledge(
                commandId,
                DeviceAckBody(commandId, sequence, success, statusCode, detail),
            )
        }
    }

    suspend fun proposals(patientId: String): List<ApiProposal> =
        withContext(Dispatchers.IO) { apiFactory.create().proposals(patientId) }

    suspend fun createOptimizationTask(body: OptimizationTaskBody): ApiOptimizationTask =
        withContext(Dispatchers.IO) { apiFactory.create().createOptimizationTask(body) }

    suspend fun optimizationTasks(patientId: String): List<ApiOptimizationTask> =
        withContext(Dispatchers.IO) { apiFactory.create().optimizationTasks(patientId) }

    suspend fun optimizationTask(taskId: String): ApiOptimizationTask =
        withContext(Dispatchers.IO) { apiFactory.create().optimizationTask(taskId) }

    suspend fun submitOptimizationFeedback(
        taskId: String,
        body: OptimizationFeedbackBody,
    ): OptimizationFeedbackResponse =
        withContext(Dispatchers.IO) {
            apiFactory.create().submitOptimizationFeedback(taskId, body)
        }

    suspend fun reviewProposal(proposalId: String, approve: Boolean, note: String): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                apiFactory.create().reviewProposal(
                    proposalId,
                    ProposalReviewBody(if (approve) "approve" else "reject", note),
                )
            }
        }

    suspend fun exportPatient(patientId: String, format: String): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            val api = apiFactory.create()
            val job = api.createExport(ExportBody(patientId, format))
            check(job.status == "completed") { job.error ?: "导出任务失败" }
            val target = File(appContext.filesDir, "exports/${job.id}.${format.lowercase()}")
            target.parentFile?.mkdirs()
            api.downloadExport(job.id).byteStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target
        }
    }

    suspend fun ensureChatSession(patientId: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val api = apiFactory.create()
            val doctor = api.careTeam(patientId).firstOrNull()
                ?: error("尚未绑定负责医生")
            api.createChatSession(ChatSessionBody(patientId, doctor.id)).id
        }
    }

    suspend fun chatMessages(sessionId: String): List<ApiChatMessage> =
        withContext(Dispatchers.IO) { apiFactory.create().chatMessages(sessionId) }

    suspend fun sendChatMessage(sessionId: String, content: String): Result<ApiChatMessage> =
        runCatching {
            withContext(Dispatchers.IO) {
                apiFactory.create().sendChatMessage(
                    sessionId,
                    ChatMessageBody(UUID.randomUUID().toString(), content),
                )
            }
        }

    suspend fun uploadManualLfp(
        patientId: String,
        interleavedSamples: ShortArray,
        packetLossCount: Int,
    ): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val api = apiFactory.create()
            val device = api.devices(patientId).firstOrNull { it.simulated }
                ?: error("患者尚未绑定模拟设备")
            val session = api.createLfpSession(
                LfpSessionBody(patient_id = patientId, device_id = device.id),
            )
            val npz = NpzWriter.twoChannelInt16(interleavedSamples, 256)
            api.uploadLfpWaveform(
                session.id,
                MultipartBody.Part.createFormData(
                    "waveform",
                    "manual-${System.currentTimeMillis()}.npz",
                    npz.toRequestBody("application/octet-stream".toMediaType()),
                ),
            )
            api.completeLfpSession(
                session.id,
                LfpCompleteBody(interleavedSamples.size / 2, packetLossCount),
            )
            session.id
        }
    }

    suspend fun simulatorDevice(patientId: String): ApiDevice =
        withContext(Dispatchers.IO) {
            apiFactory.create().devices(patientId).firstOrNull { it.simulated }
                ?: error("患者尚未绑定模拟设备")
        }

    suspend fun createInitialization(
        patientId: String,
        mode: String,
        electrodeConfig: Map<String, Any>,
    ): ApiInitialization = withContext(Dispatchers.IO) {
        val api = apiFactory.create()
        val device = api.devices(patientId).firstOrNull { it.simulated }
            ?: error("患者尚未绑定模拟设备")
        api.createInitialization(
            InitializationBody(
                patient_id = patientId,
                device_id = device.id,
                mode = mode,
                electrode_config = electrodeConfig,
            ),
        )
    }

    suspend fun uploadInitializationSegment(
        initializationId: String,
        patientId: String,
        deviceId: String,
        stateLabel: String,
        interleavedSamples: ShortArray,
        receivedFrames: Int,
        packetLossCount: Int,
        crcErrorCount: Int,
        saturatedSampleCount: Int,
        impedance: Map<String, Float>,
    ): ApiInitialization = withContext(Dispatchers.IO) {
        val api = apiFactory.create()
        val session = api.createLfpSession(
            LfpSessionBody(
                patient_id = patientId,
                device_id = deviceId,
                purpose = "baseline",
                state_label = stateLabel,
                recording_enabled = true,
            ),
        )
        val npz = NpzWriter.twoChannelInt16(interleavedSamples, 256)
        api.uploadLfpWaveform(
            session.id,
            MultipartBody.Part.createFormData(
                "waveform",
                "baseline-$stateLabel-${System.currentTimeMillis()}.npz",
                npz.toRequestBody("application/octet-stream".toMediaType()),
            ),
        )
        api.completeLfpSession(
            session.id,
            LfpCompleteBody(interleavedSamples.size / 2, packetLossCount),
        )
        api.attachInitializationSegment(
            initializationId,
            InitializationSegmentBody(
                lfp_session_id = session.id,
                state_label = stateLabel,
                received_frames = receivedFrames,
                packet_loss_count = packetLossCount,
                crc_error_count = crcErrorCount,
                saturated_sample_count = saturatedSampleCount,
                impedance = impedance,
            ),
        )
    }

    suspend fun analyzeInitialization(initializationId: String): ApiInitialization =
        withContext(Dispatchers.IO) {
            apiFactory.create().analyzeInitialization(initializationId)
        }

    suspend fun initialization(initializationId: String): ApiInitialization =
        withContext(Dispatchers.IO) {
            apiFactory.create().initialization(initializationId)
        }

    suspend fun initializations(patientId: String): List<ApiInitialization> =
        withContext(Dispatchers.IO) {
            apiFactory.create().initializations(patientId)
        }

    suspend fun approveInitialization(initializationId: String): ApiInitialization =
        withContext(Dispatchers.IO) {
            apiFactory.create().approveInitialization(
                initializationId,
                InitializationApproveBody("医生确认脱敏科研模拟器初始化结果"),
            )
        }

    fun openMonitorSocket(
        patientId: String,
        onMessage: (String) -> Unit,
        onStatus: (String) -> Unit,
    ): WebSocket? {
        val session = currentSession() ?: return null
        val base = serverUrl()
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
            .trimEnd('/')
        val request = Request.Builder()
            .url("$base/ws/patients/$patientId/monitor?token=${session.accessToken}")
            .build()
        return webSocketClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    onStatus("实时 WebSocket 已连接")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    onMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    onStatus("实时连接中断：${t.message}")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onStatus("实时连接已关闭")
                }
            },
        )
    }

    private suspend fun enqueue(eventId: String, type: String, body: Any) {
        database.pendingEvents().insert(
            PendingEventEntity(
                eventId = eventId,
                type = type,
                payloadJson = gson.toJson(body),
                createdAtMs = System.currentTimeMillis(),
            ),
        )
        scheduleSync()
    }

    private fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "omnidapt-pending-sync",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val EVENT_SYMPTOM = "symptom"
        const val EVENT_MEDICATION = "medication"
        const val EVENT_INFERENCE = "inference"
    }
}
