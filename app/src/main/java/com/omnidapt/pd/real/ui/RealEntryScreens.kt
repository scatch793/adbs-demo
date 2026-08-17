package com.omnidapt.pd.real.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.omnidapt.pd.data.UserRole
import com.omnidapt.pd.real.RealRepository
import com.omnidapt.pd.real.network.AdminUserBody
import com.omnidapt.pd.real.network.ApiUser
import com.omnidapt.pd.real.local.CachedPatientEntity
import kotlinx.coroutines.launch

@Composable
fun RealLoginScreen(
    repository: RealRepository,
    onLogin: (UserRole) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf(repository.serverUrl()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var replacementPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var mustChangePassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("请输入管理员分配的账号；身份由服务器决定。") }
    var pendingRole by remember { mutableStateOf<UserRole?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Ominidapt PD", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("科研演示闭环 · 仅连接模拟设备", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("服务器地址") },
            supportingText = { Text("真机示例：http://电脑局域网IP:8000") },
            enabled = !mustChangePassword,
        )
        OutlinedButton(
            onClick = {
                loading = true
                status = "正在检测服务器…"
                scope.launch {
                    repository.testServer(server).fold(
                        onSuccess = { status = "服务器可用" },
                        onFailure = { status = readableError(it) },
                    )
                    loading = false
                }
            },
            enabled = !loading && !mustChangePassword,
        ) { Text("检测连接") }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("账号") },
            enabled = !mustChangePassword,
            singleLine = true,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (mustChangePassword) "当前临时密码" else "密码") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        if (mustChangePassword) {
            Text(
                "首次登录必须设置至少 12 位的新密码，修改前不能进入业务页面。",
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedTextField(
                value = replacementPassword,
                onValueChange = { replacementPassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("新密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("确认新密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                loading = true
                scope.launch {
                    if (mustChangePassword) {
                        if (replacementPassword.length < 12 || replacementPassword != confirmPassword) {
                            status = "新密码至少 12 位，且两次输入必须一致。"
                        } else {
                            repository.changePassword(password, replacementPassword).fold(
                                onSuccess = { onLogin(requireNotNull(pendingRole)) },
                                onFailure = { status = readableError(it) },
                            )
                        }
                    } else {
                        repository.login(server, username, password).fold(
                            onSuccess = { session ->
                                pendingRole = session.role.toRole()
                                if (pendingRole == null) {
                                    repository.logout()
                                    status = "服务器返回了未知角色。"
                                } else if (session.mustChangePassword) {
                                    mustChangePassword = true
                                    status = "请先修改临时密码。"
                                } else {
                                    onLogin(requireNotNull(pendingRole))
                                }
                            },
                            onFailure = { status = readableError(it) },
                        )
                    }
                    loading = false
                }
            },
            enabled = !loading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) CircularProgressIndicator() else Text(if (mustChangePassword) "修改密码并进入" else "登录")
        }
        Text(status, modifier = Modifier.padding(top = 12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShell(repository: RealRepository, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<ApiUser>>(emptyList()) }
    var patients by remember { mutableStateOf<List<CachedPatientEntity>>(emptyList()) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var temporaryPassword by remember { mutableStateOf("") }
    var patientCode by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("doctor") }
    var expanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("管理员操作会写入服务器审计日志。") }
    var simulatorSerial by remember { mutableStateOf("SIM-P001-002") }

    fun refresh() {
        scope.launch {
            runCatching { repository.adminUsers() }
                .onSuccess { users = it }
                .onFailure { status = readableError(it) }
            runCatching {
                repository.refreshPatients()
                patients = repository.cachedPatients()
            }.onFailure { status = readableError(it) }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统管理") },
                actions = { OutlinedButton(onClick = onLogout) { Text("退出") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text("创建账号", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(username, { username = it }, label = { Text("登录名") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(displayName, { displayName = it }, label = { Text("显示名称") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                temporaryPassword,
                { temporaryPassword = it },
                label = { Text("临时密码（至少12位）") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("服务器角色") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("doctor", "patient", "admin").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { role = option; expanded = false },
                        )
                    }
                }
            }
            if (role == "patient") {
                OutlinedTextField(patientCode, { patientCode = it }, label = { Text("患者编号") }, modifier = Modifier.fillMaxWidth())
            }
            Button(
                onClick = {
                    scope.launch {
                        repository.createUser(
                            AdminUserBody(
                                username = username,
                                temporary_password = temporaryPassword,
                                role = role,
                                display_name = displayName,
                                patient_code = patientCode.takeIf { role == "patient" },
                            ),
                        ).fold(
                            onSuccess = {
                                status = "账号 ${it.username} 已创建，首次登录必须改密。"
                                username = ""; displayName = ""; temporaryPassword = ""; patientCode = ""
                                refresh()
                            },
                            onFailure = { status = readableError(it) },
                        )
                    }
                },
                enabled = username.isNotBlank() && displayName.isNotBlank() && temporaryPassword.length >= 12,
            ) { Text("创建账号") }
            Text(status, modifier = Modifier.padding(vertical = 12.dp))
            Text("医患关系与模拟设备", style = MaterialTheme.typography.titleLarge)
            val firstDoctor = users.firstOrNull { it.role == "doctor" }
            val firstPatient = patients.firstOrNull()
            Text(
                "当前目标：医生 ${firstDoctor?.display_name ?: "无"} / 患者 ${firstPatient?.name ?: "无"}",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (firstDoctor != null && firstPatient != null) {
                            scope.launch {
                                repository.bindCare(firstDoctor.id, firstPatient.id).fold(
                                    onSuccess = { status = "医患关系已绑定并写入审计日志。" },
                                    onFailure = { status = readableError(it) },
                                )
                            }
                        }
                    },
                    enabled = firstDoctor != null && firstPatient != null,
                ) { Text("绑定医患关系") }
            }
            OutlinedTextField(
                simulatorSerial,
                { simulatorSerial = it },
                label = { Text("模拟设备序列号") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (firstPatient != null) {
                        scope.launch {
                            repository.createAndBindSimulator(simulatorSerial, firstPatient.id).fold(
                                onSuccess = { status = "模拟设备 ${it.serial_number} 已绑定；非模拟设备会被服务器拒绝。" },
                                onFailure = { status = readableError(it) },
                            )
                        }
                    }
                },
                enabled = firstPatient != null && simulatorSerial.startsWith("SIM-"),
            ) { Text("创建并绑定模拟设备") }
            Text("现有账号", style = MaterialTheme.typography.titleLarge)
            users.forEach { user ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(user.display_name, fontWeight = FontWeight.Bold)
                            Text("${user.username} · ${user.role}${if (user.must_change_password) " · 待改密" else ""}")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.resetPassword(user.id, "Ominidapt-Temp-2026").fold(
                                        onSuccess = { status = "${user.username} 已重置为比赛环境临时密码，登录后必须修改。" },
                                        onFailure = { status = readableError(it) },
                                    )
                                    refresh()
                                }
                            },
                        ) { Text("重置密码") }
                    }
                }
            }
        }
    }
}

private fun String.toRole(): UserRole? = when (lowercase()) {
    "doctor" -> UserRole.Doctor
    "patient" -> UserRole.Patient
    "admin" -> UserRole.Admin
    else -> null
}

private fun readableError(error: Throwable): String {
    val raw = error.message.orEmpty()
    return when {
        raw.contains("401") -> "账号或密码错误，或登录已过期。"
        raw.contains("403") -> "当前账号没有执行此操作的权限。"
        raw.contains("409") -> "数据已存在，请检查账号或编号。"
        raw.contains("Failed to connect", ignoreCase = true) -> "无法连接服务器，请检查地址与局域网。"
        else -> raw.ifBlank { "操作失败，请查看服务器日志。" }
    }
}
