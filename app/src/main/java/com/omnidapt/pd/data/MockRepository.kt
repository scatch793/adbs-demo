package com.omnidapt.pd.data

import kotlin.math.PI
import kotlin.math.sin

class MockRepository {
    private val currentPatient = Patient(
        id = "2306151123",
        name = "陈建国",
        gender = "男",
        age = 62,
        number = "2306151123"
    )

    private val doctorPatients = mutableListOf(
        DoctorPatientRecord(
            id = "2403021728",
            name = "王伟",
            gender = "男",
            age = 68,
            number = "2403021728",
            implantDate = "2024/03/02",
            summary = "2013年5月出现右手静止性震颤，近期准备初始化刺激参数。",
            group = PatientListGroup.PendingInitialization
        ),
        DoctorPatientRecord(
            id = currentPatient.id,
            name = currentPatient.name,
            gender = currentPatient.gender,
            age = currentPatient.age,
            number = currentPatient.number,
            implantDate = "2023/06/15",
            summary = "2014年7月出现左侧肢体震颤伴行动迟缓，目前需重点跟踪。",
            group = PatientListGroup.PendingInitialization
        ),
        DoctorPatientRecord(
            id = "2210284567",
            name = "张秀兰",
            gender = "女",
            age = 59,
            number = "2210284567",
            implantDate = "2022/10/28",
            summary = "右侧肢体僵硬，步态不稳，待完成初始基线检测。",
            group = PatientListGroup.PendingInitialization
        ),
        DoctorPatientRecord(
            id = "2401101234",
            name = "周丽萍",
            gender = "女",
            age = 64,
            number = "2401101234",
            implantDate = "2024/01/10",
            summary = "近1周DBS刺激后出现头晕、肢体异动，需要重点关注。",
            group = PatientListGroup.Focus
        ),
        DoctorPatientRecord(
            id = "2309225678",
            name = "吴建军",
            gender = "男",
            age = 66,
            number = "2309225678",
            implantDate = "2023/09/22",
            summary = "参数调整后出现面部抽搐和肢体麻木，建议复核。",
            group = PatientListGroup.Focus
        ),
        DoctorPatientRecord(
            id = "2207149012",
            name = "郑晓华",
            gender = "女",
            age = 61,
            number = "2207149012",
            implantDate = "2022/07/14",
            summary = "近3天刺激时出现颈部僵硬、异动，需观察趋势。",
            group = PatientListGroup.Focus
        ),
        DoctorPatientRecord(
            id = "2104097890",
            name = "刘德明",
            gender = "男",
            age = 65,
            number = "2104097890",
            implantDate = "2021/04/09",
            summary = "双手静止性震颤，长期参数稳定，常规监控。",
            group = PatientListGroup.Routine
        ),
        DoctorPatientRecord(
            id = "2008162345",
            name = "赵桂英",
            gender = "女",
            age = 67,
            number = "2008162345",
            implantDate = "2020/08/16",
            summary = "起步困难伴肢体震颤，近期反馈稳定。",
            group = PatientListGroup.Routine
        ),
        DoctorPatientRecord(
            id = "1912036789",
            name = "孙志强",
            gender = "男",
            age = 63,
            number = "1912036789",
            implantDate = "2023/12/03",
            summary = "右侧肢体运动迟缓，当前常规随访。",
            group = PatientListGroup.Routine
        )
    )

    private val initializationWorkflows = mutableMapOf<String, InitializationWorkflowState>().apply {
        doctorPatients.forEach { patient ->
            put(
                patient.id,
                InitializationWorkflowState(
                    step = if (patient.group == PatientListGroup.PendingInitialization) {
                        InitializationStep.ElectrodeConfig
                    } else {
                        InitializationStep.Completed
                    }
                )
            )
        }
    }

    private var deviceState = DeviceConnectionState.Connected
    private var doctorSettings = DoctorSettings()
    private var exportCounter = 6

    private val deviceFileTemplates = listOf(
        ExportFileRecord(
            id = "export-001",
            patientId = currentPatient.id,
            patientName = currentPatient.name,
            type = ExportFileType.ParameterRecord,
            format = ExportFormat.CSV,
            fileName = "陈建国_参数调整记录_2026-07-25.csv",
            size = "12KB",
            generatedAt = "2026-07-25 15:32",
            exported = false
        ),
        ExportFileRecord(
            id = "export-002",
            patientId = currentPatient.id,
            patientName = currentPatient.name,
            type = ExportFileType.BrainSignal,
            format = ExportFormat.EDF,
            fileName = "陈建国_脑电数据_2026-07-25_0921.edf",
            size = "1.2MB",
            generatedAt = "2026-07-25 09:21",
            exported = false
        ),
        ExportFileRecord(
            id = "export-003",
            patientId = currentPatient.id,
            patientName = currentPatient.name,
            type = ExportFileType.TelehealthNote,
            format = ExportFormat.PDF,
            fileName = "陈建国_远程诊疗记录_2026-07-24.pdf",
            size = "74KB",
            generatedAt = "2026-07-24 16:52",
            exported = false
        ),
        ExportFileRecord(
            id = "export-004",
            patientId = currentPatient.id,
            patientName = currentPatient.name,
            type = ExportFileType.BrainSignal,
            format = ExportFormat.MAT,
            fileName = "陈建国_脑电数据_2026-07-24_1126.mat",
            size = "1.8MB",
            generatedAt = "2026-07-24 11:26",
            exported = false
        ),
        ExportFileRecord(
            id = "export-005",
            patientId = currentPatient.id,
            patientName = currentPatient.name,
            type = ExportFileType.BrainSignal,
            format = ExportFormat.EDF,
            fileName = "陈建国_脑电数据_2026-07-23_1720.edf",
            size = "1.1MB",
            generatedAt = "2026-07-23 17:20",
            exported = false
        )
    )
    private val exportFiles = deviceFileTemplates.toMutableList()

    private fun ensureDeviceFilesForPatient(patientId: String) {
        if (exportFiles.any { it.patientId == patientId }) return
        val patient = doctorPatients.firstOrNull { it.id == patientId } ?: return
        exportFiles += deviceFileTemplates.map { template ->
            template.copy(
                id = "${patient.id}-${template.id}",
                patientId = patient.id,
                patientName = patient.name,
                fileName = template.fileName.replace(currentPatient.name, patient.name),
                exported = false
            )
        }
    }

    private val actionLogs = mutableListOf<DoctorActionLog>()
    private val telehealthSessions = mutableListOf<TelehealthSession>()
    private val realtimeStates = mutableMapOf<String, RealtimeMonitorState>()
    private val frequencyBands = mutableMapOf<String, FrequencyBands>()
    private val optimizationSettings = mutableMapOf<String, ParameterOptimizationSettings>()

    private val medicationRecords = mutableListOf(
        MedicationRecord("06-08\n周六", true, true),
        MedicationRecord("06-09\n周日", true, true),
        MedicationRecord("06-10\n周一", true, true),
        MedicationRecord("06-11\n周二", true, true),
        MedicationRecord("06-12\n周三", true, true),
        MedicationRecord("06-13\n周四", false, false),
        MedicationRecord("06-14\n周五", false, false)
    )

    private val alertEvents = mutableListOf(
        AlertEvent("06-07", 1, 0, 0),
        AlertEvent("06-08", 2, 0, 1),
        AlertEvent("06-09", 3, 1, 1),
        AlertEvent("06-10", 1, 0, 0),
        AlertEvent("06-11", 3, 2, 1),
        AlertEvent("06-12", 2, 0, 1),
        AlertEvent("06-13", 2, 1, 1),
        AlertEvent("06-14", 1, 0, 0)
    )

    private val parameterHistory = mutableListOf(
        TherapyParameters("2024-03-26", 2.4f, 130, 70, "4+1-", 45),
        TherapyParameters("2023-11-15", 2.6f, 130, 70, "4+1-", 45),
        TherapyParameters("2023-08-02", 2.5f, 130, 70, "4+1-", 45),
        TherapyParameters("2023-07-15", 2.7f, 130, 70, "4+1-", 45)
    )

    private var latestFeedback = SymptomFeedback(tremor = 1, rigidity = 0, speech = 0)
    private val medicationRecordsByPatient = mutableMapOf(currentPatient.id to medicationRecords)
    private val alertEventsByPatient = mutableMapOf(currentPatient.id to alertEvents)
    private val parameterHistoryByPatient = mutableMapOf(currentPatient.id to parameterHistory)
    private val feedbackByPatient = mutableMapOf(currentPatient.id to latestFeedback)

    fun getCurrentPatient(): Patient = currentPatient

    fun getDeviceState(): DeviceConnectionState = deviceState

    fun connectDevice(): DeviceConnectionState {
        deviceState = DeviceConnectionState.Connected
        doctorPatients.firstOrNull { it.id == currentPatient.id }?.let {
            appendDoctorActionLog(it.id, "设备已重新连接")
        }
        return deviceState
    }

    fun disconnectDevice(): DeviceConnectionState {
        deviceState = DeviceConnectionState.Disconnected
        doctorPatients.firstOrNull { it.id == currentPatient.id }?.let {
            appendDoctorActionLog(it.id, "设备已断开")
        }
        return deviceState
    }

    fun getDoctorPatients(
        query: String = "",
        sortField: PatientSortField = PatientSortField.ImplantDate,
        ascending: Boolean = false
    ): List<DoctorPatientRecord> {
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isBlank()) {
            doctorPatients
        } else {
            doctorPatients.filter { patient ->
                patient.name.contains(normalizedQuery, ignoreCase = true) ||
                    patient.number.contains(normalizedQuery, ignoreCase = true) ||
                    patient.implantDate.contains(normalizedQuery, ignoreCase = true) ||
                    patient.summary.contains(normalizedQuery, ignoreCase = true)
            }
        }

        val sorted = when (sortField) {
            PatientSortField.Name -> filtered.sortedBy { it.name }
            PatientSortField.Number -> filtered.sortedBy { it.number }
            PatientSortField.ImplantDate -> filtered.sortedBy { it.implantDate }
            PatientSortField.Age -> filtered.sortedBy { it.age }
        }
        return if (ascending) sorted else sorted.reversed()
    }

    /**
     * Keeps the established presentation model while allowing the production
     * repository to provide the authorized patient directory.
     */
    fun replaceDoctorPatients(records: List<DoctorPatientRecord>) {
        doctorPatients.clear()
        doctorPatients.addAll(records)
        records.forEach { record ->
            initializationWorkflows.putIfAbsent(
                record.id,
                InitializationWorkflowState(
                    step = if (record.group == PatientListGroup.PendingInitialization) {
                        InitializationStep.ElectrodeConfig
                    } else {
                        InitializationStep.Completed
                    },
                ),
            )
            realtimeStates.putIfAbsent(record.id, RealtimeMonitorState())
        }
    }

    fun addDoctorPatient(record: DoctorPatientRecord): Boolean {
        if (doctorPatients.any { it.number == record.number || it.id == record.id }) return false
        doctorPatients.add(record)
        initializationWorkflows[record.id] = InitializationWorkflowState(
            step = if (record.group == PatientListGroup.PendingInitialization) {
                InitializationStep.ElectrodeConfig
            } else {
                InitializationStep.Completed
            }
        )
        realtimeStates[record.id] = RealtimeMonitorState()
        appendDoctorActionLog(record.id, "新增患者档案")
        return true
    }

    fun updateDoctorPatient(record: DoctorPatientRecord): Boolean {
        val index = doctorPatients.indexOfFirst { it.id == record.id }
        if (index < 0) return false
        if (doctorPatients.any { it.id != record.id && it.number == record.number }) return false
        doctorPatients[index] = record
        if (initializationWorkflows[record.id] == null) {
            initializationWorkflows[record.id] = InitializationWorkflowState(
                step = if (record.group == PatientListGroup.PendingInitialization) {
                    InitializationStep.ElectrodeConfig
                } else {
                    InitializationStep.Completed
                }
            )
        }
        appendDoctorActionLog(record.id, "更新患者档案")
        return true
    }

    fun deleteDoctorPatient(patientId: String): Boolean {
        val removed = doctorPatients.removeIf { it.id == patientId }
        if (removed) {
            initializationWorkflows.remove(patientId)
            realtimeStates.remove(patientId)
            frequencyBands.remove(patientId)
            optimizationSettings.remove(patientId)
            actionLogs.removeIf { it.patientId == patientId }
        }
        return removed
    }

    fun getDoctorPatient(patientId: String): DoctorPatientRecord? =
        doctorPatients.firstOrNull { it.id == patientId }

    fun getInitializationStep(patientId: String): InitializationStep {
        requirePatientExists(patientId)
        return getInitializationWorkflow(patientId).step
    }

    fun getInitializationWorkflow(patientId: String): InitializationWorkflowState {
        requirePatientExists(patientId)
        return initializationWorkflows.getOrPut(patientId) { InitializationWorkflowState() }
    }

    fun saveElectrodeConfiguration(
        patientId: String,
        selection: ElectrodeSelection,
        parameters: List<StimulationParameterDraft>
    ): InitializationWorkflowState {
        requirePatientExists(patientId)
        require(selection.isValid()) { "电极正负触点配置无效" }
        require(parameters.size == 4 && parameters.all { it.isSafe() }) { "刺激参数不在安全范围内" }
        val next = getInitializationWorkflow(patientId).copy(
            electrodeSelection = selection,
            stimulationParameters = parameters
        )
        initializationWorkflows[patientId] = next
        appendDoctorActionLog(patientId, "保存电极配置与基线刺激参数")
        return next
    }

    fun saveBaselineSamplingState(
        patientId: String,
        state: BaselineSamplingState
    ): InitializationWorkflowState {
        requirePatientExists(patientId)
        val next = getInitializationWorkflow(patientId).copy(baseline = state)
        initializationWorkflows[patientId] = next
        return next
    }

    fun advanceInitialization(patientId: String): InitializationStep {
        requirePatientExists(patientId)
        val next = when (getInitializationStep(patientId)) {
            InitializationStep.ElectrodeConfig -> InitializationStep.BaselineDetection
            InitializationStep.BaselineDetection -> InitializationStep.FrequencyExtraction
            InitializationStep.FrequencyExtraction -> InitializationStep.Completed
            InitializationStep.Completed -> InitializationStep.Completed
        }
        initializationWorkflows[patientId] = getInitializationWorkflow(patientId).copy(step = next)
        return next
    }

    fun previousInitialization(patientId: String): InitializationStep {
        requirePatientExists(patientId)
        val previous = when (getInitializationStep(patientId)) {
            InitializationStep.ElectrodeConfig -> InitializationStep.ElectrodeConfig
            InitializationStep.BaselineDetection -> InitializationStep.ElectrodeConfig
            InitializationStep.FrequencyExtraction -> InitializationStep.BaselineDetection
            InitializationStep.Completed -> InitializationStep.FrequencyExtraction
        }
        initializationWorkflows[patientId] = getInitializationWorkflow(patientId).copy(step = previous)
        return previous
    }

    fun resetInitialization(patientId: String): InitializationStep {
        requirePatientExists(patientId)
        initializationWorkflows[patientId] = InitializationWorkflowState()
        frequencyBands.remove(patientId)
        appendDoctorActionLog(patientId, "重新进行初始化")
        return InitializationStep.ElectrodeConfig
    }

    fun saveInitializationFrequencyBands(patientId: String, bands: FrequencyBands) {
        requirePatientExists(patientId)
        frequencyBands[patientId] = bands
        initializationWorkflows[patientId] = getInitializationWorkflow(patientId).copy(frequencyBands = bands)
        appendDoctorActionLog(patientId, "保存个体化频段")
    }

    fun getInitializationFrequencyBands(patientId: String): FrequencyBands {
        requirePatientExists(patientId)
        return frequencyBands[patientId] ?: getInitializationWorkflow(patientId).frequencyBands
    }

    fun getImpedanceSeries(
        patientId: String,
        side: ImpedanceSide,
        mode: ImpedanceMode
    ): List<ImpedancePoint> {
        requirePatientExists(patientId)
        return when (side to mode) {
            ImpedanceSide.Left to ImpedanceMode.Monopolar -> listOf(
                ImpedancePoint("C-5", 1.1f),
                ImpedancePoint("C-6", 1.2f),
                ImpedancePoint("C-7", 0.9f),
                ImpedancePoint("C-8", 1.0f)
            )
            ImpedanceSide.Right to ImpedanceMode.Monopolar -> listOf(
                ImpedancePoint("C-1", 1.0f),
                ImpedancePoint("C-2", 2.8f),
                ImpedancePoint("C-3", 1.1f),
                ImpedancePoint("C-4", 0.9f)
            )
            ImpedanceSide.Left to ImpedanceMode.Bipolar -> listOf(
                ImpedancePoint("5-6", 1.5f),
                ImpedancePoint("6-7", 1.6f),
                ImpedancePoint("7-8", 1.4f)
            )
            ImpedanceSide.Right to ImpedanceMode.Bipolar -> listOf(
                ImpedancePoint("1-2", 2.5f),
                ImpedancePoint("2-3", 2.6f),
                ImpedancePoint("3-4", 1.5f)
            )
            else -> emptyList()
        }
    }

    fun saveParameterOptimizationSettings(patientId: String, settings: ParameterOptimizationSettings) {
        requirePatientExists(patientId)
        optimizationSettings[patientId] = settings
        appendDoctorActionLog(patientId, "保存反馈优化设置")
    }

    fun getParameterOptimizationSettings(patientId: String): ParameterOptimizationSettings {
        requirePatientExists(patientId)
        return optimizationSettings[patientId] ?: ParameterOptimizationSettings()
    }

    fun appendDoctorActionLog(patientId: String, action: String): DoctorActionLog {
        val log = DoctorActionLog(patientId = patientId, action = action, time = "2026-07-25 16:${(actionLogs.size + 10) % 60}")
        actionLogs.add(0, log)
        return log
    }

    fun getDoctorActionLogs(patientId: String): List<DoctorActionLog> =
        actionLogs.filter { it.patientId == patientId }

    fun getExportFiles(
        query: String = "",
        type: ExportFileType? = null,
        patientId: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): List<ExportFileRecord> {
        patientId?.let(::ensureDeviceFilesForPatient)
        val normalizedQuery = query.trim()
        return exportFiles
            .filter { file -> type == null || file.type == type }
            .filter { file -> patientId.isNullOrBlank() || file.patientId == patientId }
            .filter { file ->
                val date = file.generatedAt.take(10)
                (dateFrom.isNullOrBlank() || date >= dateFrom) &&
                    (dateTo.isNullOrBlank() || date <= dateTo)
            }
            .filter { file ->
                normalizedQuery.isBlank() ||
                    file.fileName.contains(normalizedQuery, ignoreCase = true) ||
                    file.patientName.contains(normalizedQuery, ignoreCase = true) ||
                    file.generatedAt.contains(normalizedQuery, ignoreCase = true) ||
                    file.type.name.contains(normalizedQuery, ignoreCase = true) ||
                    file.format.name.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.generatedAt }
    }

    fun createExport(fileIds: Set<String>, format: ExportFormat, settings: ExportSettings): ExportFileRecord {
        exportCounter += 1
        val firstSource = exportFiles.firstOrNull { it.id in fileIds }
        val patient = firstSource?.let { getDoctorPatient(it.patientId) }
            ?: getDoctorPatient(currentPatient.id)
            ?: DoctorPatientRecord(
                id = currentPatient.id,
                name = currentPatient.name,
                gender = currentPatient.gender,
                age = currentPatient.age,
                number = currentPatient.number,
                implantDate = "2023/06/15",
                summary = "",
                group = PatientListGroup.Routine
            )
        val suffix = format.name.lowercase()
        val record = ExportFileRecord(
            id = "export-${exportCounter.toString().padStart(3, '0')}",
            patientId = patient.id,
            patientName = patient.name,
            type = ExportFileType.PatientReport,
            format = format,
            fileName = "${patient.name}_综合导出_${exportCounter}.$suffix",
            size = "${180 + fileIds.size * 42 + if (settings.includeSignalClips) 120 else 0}KB",
            generatedAt = "2026-07-25 16:${(exportCounter + 20) % 60}",
            exported = true
        )
        appendDoctorActionLog(patient.id, "生成${format.name}导出文件")
        return record
    }

    fun deleteExportFiles(ids: Set<String>): Int {
        val before = exportFiles.size
        exportFiles.removeIf { it.id in ids }
        return before - exportFiles.size
    }

    fun startTelehealth(patientId: String): TelehealthSession {
        requirePatientExists(patientId)
        val existing = telehealthSessions.firstOrNull { it.patientId == patientId && it.active }
        if (existing != null) return existing
        val session = TelehealthSession(
            id = "session-${telehealthSessions.size + 1}",
            patientId = patientId,
            active = true,
            startedAt = "2026-07-25 16:${(telehealthSessions.size + 18) % 60}",
            messages = listOf(
                TelehealthMessage("系统", "远程诊疗会话已建立，患者端将收到提醒。", "16:${(telehealthSessions.size + 18) % 60}")
            )
        )
        telehealthSessions.add(0, session)
        appendDoctorActionLog(patientId, "开启远程诊疗")
        return session
    }

    fun endTelehealth(sessionId: String): TelehealthSession? {
        val index = telehealthSessions.indexOfFirst { it.id == sessionId }
        if (index < 0) return null
        val ended = telehealthSessions[index].copy(active = false)
        telehealthSessions[index] = ended
        appendDoctorActionLog(ended.patientId, "结束远程诊疗")
        return ended
    }

    fun addTelehealthMessage(sessionId: String, message: String): TelehealthSession? {
        val index = telehealthSessions.indexOfFirst { it.id == sessionId }
        if (index < 0) return null
        val session = telehealthSessions[index]
        val next = session.copy(
            messages = session.messages + TelehealthMessage(
                sender = "医生",
                content = message,
                time = "16:${(session.messages.size + 21) % 60}"
            )
        )
        telehealthSessions[index] = next
        return next
    }

    fun getDoctorSettings(): DoctorSettings = doctorSettings

    fun updateDoctorSettings(settings: DoctorSettings): DoctorSettings {
        doctorSettings = settings
        return doctorSettings
    }

    fun getRealtimeMonitorState(patientId: String): RealtimeMonitorState {
        requirePatientExists(patientId)
        return realtimeStates.getOrPut(patientId) { RealtimeMonitorState() }
    }

    fun setRealtimePaused(patientId: String, paused: Boolean): RealtimeMonitorState {
        val current = getRealtimeMonitorState(patientId)
        val next = current.copy(paused = paused)
        realtimeStates[patientId] = next
        appendDoctorActionLog(patientId, if (paused) "暂停实时观测" else "恢复实时观测")
        return next
    }

    fun setRealtimeDisplayMode(patientId: String, mode: String): RealtimeMonitorState {
        val current = getRealtimeMonitorState(patientId)
        val next = current.copy(displayMode = mode)
        realtimeStates[patientId] = next
        return next
    }

    fun toggleRealtimeRecording(patientId: String): RealtimeMonitorState {
        val current = getRealtimeMonitorState(patientId)
        val next = current.copy(
            recording = !current.recording,
            recordedSegments = if (current.recording) current.recordedSegments + 1 else current.recordedSegments
        )
        realtimeStates[patientId] = next
        appendDoctorActionLog(patientId, if (next.recording) "开始记录实时片段" else "保存实时片段")
        return next
    }

    fun recordRealtimeEvent(patientId: String, label: String): RealtimeMonitorState {
        val current = getRealtimeMonitorState(patientId)
        val next = current.copy(eventMarkers = listOf(label) + current.eventMarkers.take(5))
        realtimeStates[patientId] = next
        appendDoctorActionLog(patientId, "标记事件：$label")
        return next
    }

    fun getPatientReport(patientId: String): PatientReport {
        requirePatientExists(patientId)
        return PatientReport(
            medications = medicationsFor(patientId).toList(),
            alerts = alertsFor(patientId).toList(),
            parameterHistory = parametersFor(patientId).toList(),
            latestFeedback = feedbackByPatient.getOrPut(patientId) { latestFeedback }
        )
    }

    fun submitSymptomFeedback(feedback: SymptomFeedback) {
        latestFeedback = feedback
        feedbackByPatient[currentPatient.id] = feedback
        val currentAlerts = alertsFor(currentPatient.id)
        if (feedback.tremor >= 2 || feedback.rigidity >= 2 || feedback.speech >= 2) {
            currentAlerts[currentAlerts.lastIndex] = AlertEvent(
                date = currentAlerts.last().date,
                tremorCount = currentAlerts.last().tremorCount + feedback.tremor,
                rigidityCount = currentAlerts.last().rigidityCount + feedback.rigidity,
                dysarthriaCount = currentAlerts.last().dysarthriaCount + feedback.speech
            )
        }
    }

    fun markMedicationTaken(patientId: String, timestamp: Long) {
        requirePatientExists(patientId)
        val records = medicationsFor(patientId)
        val last = records.last()
        records[records.lastIndex] = last.copy(eveningTaken = true)
    }

    fun observeRealtimeSignals(patientId: String): List<BrainSignalPoint> =
        observeRealtimeSignals(patientId, tick = 0)

    fun observeRealtimeSignals(patientId: String, tick: Int): List<BrainSignalPoint> {
        requirePatientExists(patientId)
        return (0..60).map { second ->
            val phase = (second + tick) / 4.0
            val fast = sin(phase * PI).toFloat()
            val slow = sin((phase / 2.4) * PI).toFloat()
            BrainSignalPoint(
                second = second.toFloat(),
                microVolt = 9f * fast + 5f * slow + ((second % 5) - 2) * 1.8f,
                staticProbability = (0.08f + 0.035f * slow + (second % 7) * 0.004f).coerceIn(0f, 1f),
                motionProbability = (0.06f + 0.03f * fast + (second % 9) * 0.003f).coerceIn(0f, 1f)
            )
        }
    }

    fun getOptimizationSuggestion(patientId: String): OptimizationSuggestion {
        requirePatientExists(patientId)
        return OptimizationSuggestion(
            currentScore = 84.6f,
            suggestedParameters = TherapyParameters("2024-06-14", 2.8f, 130, 70, "4+1-", 45),
            curve = listOf(35f, 48f, 60f, 72f, 76f, 80f, 83f, 84.6f, 88f, 91f, 94f, 96f)
        )
    }

    fun confirmParameterDownload(patientId: String, parameters: TherapyParameters) {
        requirePatientExists(patientId)
        parametersFor(patientId).add(0, parameters)
        appendDoctorActionLog(patientId, "确认下发参数 ${parameters.currentMa}mA/${parameters.frequencyHz}Hz")
    }

    private fun medicationsFor(patientId: String): MutableList<MedicationRecord> =
        medicationRecordsByPatient.getOrPut(patientId) {
            medicationRecords.map { it.copy() }.toMutableList()
        }

    private fun alertsFor(patientId: String): MutableList<AlertEvent> =
        alertEventsByPatient.getOrPut(patientId) {
            alertEvents.map { it.copy() }.toMutableList()
        }

    private fun parametersFor(patientId: String): MutableList<TherapyParameters> =
        parameterHistoryByPatient.getOrPut(patientId) {
            parameterHistory.map { it.copy() }.toMutableList()
        }

    private fun requirePatientExists(patientId: String) {
        require(patientId == currentPatient.id || doctorPatients.any { it.id == patientId })
    }
}
