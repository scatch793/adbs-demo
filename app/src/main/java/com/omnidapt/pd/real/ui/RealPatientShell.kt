package com.omnidapt.pd.real.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnidapt.pd.real.RealRepository
import com.omnidapt.pd.real.ReminderWorker
import com.omnidapt.pd.real.algorithm.EdgeInferenceController
import com.omnidapt.pd.real.ble.BleCentralClient
import com.omnidapt.pd.real.ble.DeviceCommandDispatcher
import com.omnidapt.pd.real.local.CachedPatientEntity
import kotlinx.coroutines.launch

private enum class RealPatientTab { HOME, HISTORY, CHAT }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RealPatientShell(
    repository: RealRepository,
    bleClient: BleCentralClient,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var patient by remember { mutableStateOf<CachedPatientEntity?>(null) }
    var tab by remember { mutableStateOf(RealPatientTab.HOME) }
    var symptoms by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var medications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var status by remember { mutableStateOf("正在读取离线缓存…") }
    val inference = remember { EdgeInferenceController(bleClient, repository) }
    val dispatcher = remember { DeviceCommandDispatcher(bleClient, repository) }

    suspend fun refreshHistory(id: String) {
        runCatching {
            symptoms = repository.symptomHistory(id)
            medications = repository.medicationHistory(id)
        }.onFailure { status = "历史记录暂时离线：${it.message}" }
    }
    LaunchedEffect(Unit) {
        runCatching { repository.refreshPatients() }
        patient = repository.cachedPatients().firstOrNull()
        patient?.let {
            inference.start(it.id)
            dispatcher.start(it.id)
            refreshHistory(it.id)
            status = "数据来自服务器与本机离线队列"
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            inference.stop()
            dispatcher.stop()
            bleClient.disconnect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ominidapt 患者端 · ${patient?.code ?: "加载中"}") },
                actions = { OutlinedButton(onClick = onLogout) { Text("退出") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RealPatientTab.entries.forEach { option ->
                    if (tab == option) {
                        Button(onClick = { tab = option }) { Text(option.label()) }
                    } else {
                        OutlinedButton(onClick = { tab = option }) { Text(option.label()) }
                    }
                }
            }
            Text(status, style = MaterialTheme.typography.bodySmall)
            when (tab) {
                RealPatientTab.HOME -> {
                    RealDevicePanel(
                        client = bleClient,
                        inference = inference,
                        dispatcher = dispatcher,
                        repository = repository,
                        patientId = patient?.id,
                    )
                    SymptomEntryPanel(
                        enabled = patient != null,
                        onSubmit = { tremor, rigidity, speech ->
                            patient?.let { current ->
                                scope.launch {
                                    repository.enqueueSymptom(current.id, tremor, rigidity, speech)
                                    status = "症状已按事件 ID 保存；联网时会幂等补传。"
                                    refreshHistory(current.id)
                                }
                            }
                        },
                        onMedication = { snoozed ->
                            patient?.let { current ->
                                scope.launch {
                                    repository.enqueueMedication(current.id, if (snoozed) "snoozed" else "taken")
                                    if (snoozed) ReminderWorker.schedule(context)
                                    status = if (snoozed) "已安排 15 分钟后提醒。" else "用药记录已保存。"
                                    refreshHistory(current.id)
                                }
                            }
                        },
                    )
                }
                RealPatientTab.HISTORY -> {
                    Text("真实记录", style = MaterialTheme.typography.titleLarge)
                    Text("症状 ${symptoms.size} 条，用药 ${medications.size} 条")
                    symptoms.take(20).forEach { RecordCard("症状", it) }
                    medications.take(20).forEach { RecordCard("用药", it) }
                }
                RealPatientTab.CHAT -> {
                    RealChatPanel(
                        repository = repository,
                        patientId = patient?.id,
                        currentUserId = repository.currentSession()?.userId,
                        onDial = {
                            patient?.emergencyPhone?.let { phone ->
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SymptomEntryPanel(
    enabled: Boolean,
    onSubmit: (Int, Int, Int) -> Unit,
    onMedication: (Boolean) -> Unit,
) {
    var tremor by remember { mutableFloatStateOf(0f) }
    var rigidity by remember { mutableFloatStateOf(0f) }
    var speech by remember { mutableFloatStateOf(0f) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("今日症状与用药", style = MaterialTheme.typography.titleMedium)
            SymptomSlider("震颤", tremor) { tremor = it }
            SymptomSlider("僵硬", rigidity) { rigidity = it }
            SymptomSlider("言语", speech) { speech = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSubmit(tremor.toInt(), rigidity.toInt(), speech.toInt()) },
                    enabled = enabled,
                ) { Text("保存症状") }
                Button(onClick = { onMedication(false) }, enabled = enabled) { Text("已用药") }
                OutlinedButton(onClick = { onMedication(true) }, enabled = enabled) { Text("稍后提醒") }
            }
        }
    }
}

@Composable
private fun SymptomSlider(label: String, value: Float, onValue: (Float) -> Unit) {
    Text("$label：${value.toInt()}（0无 / 3重）")
    Slider(value, onValueChange = onValue, valueRange = 0f..3f, steps = 2)
}

@Composable
private fun RecordCard(kind: String, values: Map<String, Any>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Text(kind, color = MaterialTheme.colorScheme.primary)
            Text(values.entries.joinToString(" · ") { "${it.key}=${it.value}" })
        }
    }
}

private fun RealPatientTab.label(): String = when (this) {
    RealPatientTab.HOME -> "设备与记录"
    RealPatientTab.HISTORY -> "历史"
    RealPatientTab.CHAT -> "联系医生"
}
