package com.omnidapt.pd.real.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnidapt.pd.real.RealRepository
import com.omnidapt.pd.real.algorithm.EdgeInferenceController
import com.omnidapt.pd.real.ble.BleCentralClient
import com.omnidapt.pd.real.ble.DeviceCommandDispatcher
import com.omnidapt.pd.real.initialization.InitializationController
import com.omnidapt.pd.real.local.CachedPatientEntity
import com.omnidapt.pd.real.network.ApiProposal
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RealDoctorShell(
    repository: RealRepository,
    bleClient: BleCentralClient,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var patients by remember { mutableStateOf<List<CachedPatientEntity>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var proposals by remember { mutableStateOf<List<ApiProposal>>(emptyList()) }
    var status by remember { mutableStateOf("正在同步服务器数据…") }
    var liveEvents by remember { mutableStateOf<List<String>>(emptyList()) }
    val inference = remember { EdgeInferenceController(bleClient, repository) }
    val dispatcher = remember { DeviceCommandDispatcher(bleClient, repository) }
    val initialization = remember { InitializationController(bleClient, repository) }

    fun load() {
        scope.launch {
            runCatching {
                repository.refreshPatients()
                patients = repository.cachedPatients()
                selectedId = selectedId ?: patients.firstOrNull()?.id
            }.onSuccess {
                status = "已从服务器同步 ${patients.size} 位授权患者"
            }.onFailure {
                patients = repository.cachedPatients()
                selectedId = selectedId ?: patients.firstOrNull()?.id
                status = "当前显示离线缓存：${readableDoctorError(it)}"
            }
        }
    }
    fun loadProposals(patientId: String) {
        scope.launch {
            runCatching { repository.proposals(patientId) }
                .onSuccess { proposals = it }
                .onFailure { status = readableDoctorError(it) }
        }
    }
    LaunchedEffect(Unit) { load() }
    LaunchedEffect(selectedId) {
        selectedId?.let {
            loadProposals(it)
            initialization.loadLatest(it)
        }
    }
    DisposableEffect(selectedId) {
        selectedId?.let {
            inference.start(it)
            dispatcher.start(it)
        }
        onDispose {
            inference.stop()
            dispatcher.stop()
        }
    }
    DisposableEffect(selectedId) {
        val socket = selectedId?.let { patientId ->
            repository.openMonitorSocket(
                patientId,
                onMessage = { message -> liveEvents = (listOf(message) + liveEvents).take(8) },
                onStatus = { status = it },
            )
        }
        onDispose { socket?.close(1000, "screen changed") }
    }
    DisposableEffect(Unit) {
        onDispose { bleClient.disconnect() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ominidapt 医生工作台") },
                actions = { OutlinedButton(onClick = onLogout) { Text("退出") } },
            )
        },
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier.weight(0.85f).verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("服务器授权患者", style = MaterialTheme.typography.titleMedium)
                patients.forEach { patient ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedId = patient.id },
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(patient.name, fontWeight = FontWeight.Bold)
                            Text("${patient.code} · ${patient.gender} · ${patient.age ?: "年龄未填"}")
                        }
                    }
                }
                OutlinedButton(onClick = ::load) { Text("重新同步") }
            }
            Column(
                modifier = Modifier.weight(1.4f).verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val selected = patients.firstOrNull { it.id == selectedId }
                Text(selected?.name ?: "请选择患者", style = MaterialTheme.typography.headlineSmall)
                Text(status)
                if (selected != null) {
                    RealDevicePanel(
                        client = bleClient,
                        inference = inference,
                        dispatcher = dispatcher,
                        repository = repository,
                        patientId = selected.id,
                    )
                    InitializationPanel(
                        controller = initialization,
                        patientId = selected.id,
                        onStart = { mode ->
                            scope.launch {
                                runCatching { initialization.run(selected.id, mode) }
                                    .onFailure { status = readableDoctorError(it) }
                            }
                        },
                        onApprove = {
                            scope.launch {
                                runCatching { initialization.approve() }
                                    .onSuccess {
                                        repository.refreshModels(selected.id)
                                        status = "初始化模型已审核启用，端侧将在下次加载时使用新模型。"
                                    }
                                    .onFailure { status = readableDoctorError(it) }
                            }
                        },
                    )
                    HorizontalDivider()
                    Text("真实文件导出", style = MaterialTheme.typography.titleMedium)
                    Text("生成后下载到应用私有 exports 目录，文件内容来自服务器数据库。")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("pdf", "csv", "mat", "edf", "eml", "zip").forEach { format ->
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        status = "正在生成 ${format.uppercase()}…"
                                        repository.exportPatient(selected.id, format).fold(
                                            onSuccess = { status = "已生成并下载：${it.absolutePath}" },
                                            onFailure = { status = readableDoctorError(it) },
                                        )
                                    }
                                },
                            ) { Text(format.uppercase()) }
                        }
                    }
                    HorizontalDivider()
                    Text("实时监测 WebSocket", style = MaterialTheme.typography.titleMedium)
                    if (liveEvents.isEmpty()) Text("等待患者端推理、告警或参数回执…")
                    liveEvents.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    HorizontalDivider()
                    Text("参数审核与模拟下发", style = MaterialTheme.typography.titleMedium)
                    if (proposals.isEmpty()) Text("当前没有参数建议。")
                    proposals.forEach { proposal ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${proposal.status} · 模型 ${proposal.model_version ?: "未标记"}", fontWeight = FontWeight.Bold)
                                Text(
                                    proposal.parameters.entries.joinToString("，") {
                                        "${it.key}=${"%.2f".format(it.value)}"
                                    },
                                )
                                Text("安全结果：${proposal.safety_result}")
                                if (proposal.status == "submitted") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    repository.reviewProposal(proposal.id, true, "医生端审核通过").fold(
                                                        onSuccess = { status = "已批准，仅进入模拟设备下发队列"; loadProposals(selected.id) },
                                                        onFailure = { status = readableDoctorError(it) },
                                                    )
                                                }
                                            },
                                        ) { Text("批准模拟下发") }
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    repository.reviewProposal(proposal.id, false, "医生端拒绝").fold(
                                                        onSuccess = { status = "建议已拒绝"; loadProposals(selected.id) },
                                                        onFailure = { status = readableDoctorError(it) },
                                                    )
                                                }
                                            },
                                        ) { Text("拒绝") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun readableDoctorError(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        message.contains("403") -> "无权访问该患者；服务器医患关系限制生效。"
        message.contains("422") -> "安全规则或严重副作用阻止了本次批准。"
        message.contains("409") -> "任务状态已变化，请刷新后重试。"
        else -> message.ifBlank { "操作失败" }
    }
}
