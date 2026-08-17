package com.omnidapt.pd.real.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnidapt.pd.real.RealRepository
import com.omnidapt.pd.real.network.ApiChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun RealChatPanel(
    repository: RealRepository,
    patientId: String?,
    currentUserId: String?,
    onDial: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var sessionId by remember(patientId) { mutableStateOf<String?>(null) }
    var messages by remember(patientId) { mutableStateOf<List<ApiChatMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("正在连接已绑定医生…") }

    LaunchedEffect(patientId) {
        val id = patientId ?: return@LaunchedEffect
        repository.ensureChatSession(id).fold(
            onSuccess = { created ->
                sessionId = created
                status = "文字咨询已连接，消息将留存在服务器。"
                while (isActive) {
                    runCatching { repository.chatMessages(created) }
                        .onSuccess { messages = it }
                        .onFailure { status = "消息同步暂时中断：${it.message}" }
                    delay(2_000)
                }
            },
            onFailure = { status = it.message ?: "无法建立聊天会话" },
        )
    }

    Text("联系医生", style = MaterialTheme.typography.headlineSmall)
    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        messages.forEach { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(if (message.sender_user_id == currentUserId) "我" else "医生")
                    Text(message.content)
                    Text(message.created_at, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("输入咨询内容") },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                val activeSession = sessionId ?: return@Button
                val content = input.trim()
                if (content.isEmpty()) return@Button
                input = ""
                scope.launch {
                    repository.sendChatMessage(activeSession, content).fold(
                        onSuccess = { messages = repository.chatMessages(activeSession) },
                        onFailure = {
                            input = content
                            status = "发送失败：${it.message}"
                        },
                    )
                }
            },
            enabled = sessionId != null && input.isNotBlank(),
        ) { Text("发送") }
        OutlinedButton(onClick = onDial) { Text("系统拨号") }
    }
    Text(
        "远程功能仅提供文字留档和系统拨号，不包含 WebRTC 音视频诊疗。",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}
