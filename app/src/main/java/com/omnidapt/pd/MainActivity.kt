package com.omnidapt.pd

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidapt.pd.data.AlertEvent
import com.omnidapt.pd.data.BrainSignalPoint
import com.omnidapt.pd.data.DeviceConnectionState
import com.omnidapt.pd.data.DoctorPatientRecord
import com.omnidapt.pd.data.DoctorScreen
import com.omnidapt.pd.data.DoctorSettings
import com.omnidapt.pd.data.BaselineSamplingState
import com.omnidapt.pd.data.ElectrodeSelection
import com.omnidapt.pd.data.ExportFileRecord
import com.omnidapt.pd.data.ExportFileType
import com.omnidapt.pd.data.ExportFormat
import com.omnidapt.pd.data.ExportSettings
import com.omnidapt.pd.data.FrequencyBands
import com.omnidapt.pd.data.ImpedanceMode
import com.omnidapt.pd.data.ImpedancePoint as StoredImpedancePoint
import com.omnidapt.pd.data.ImpedanceSide
import com.omnidapt.pd.data.InitializationWorkflowState
import com.omnidapt.pd.data.InitializationStep
import com.omnidapt.pd.data.MockRepository
import com.omnidapt.pd.data.ParameterOptimizationSettings
import com.omnidapt.pd.data.Patient
import com.omnidapt.pd.data.PatientListGroup
import com.omnidapt.pd.data.PatientReport
import com.omnidapt.pd.data.PatientSortField
import com.omnidapt.pd.data.PatientTab
import com.omnidapt.pd.data.RealtimeMonitorState
import com.omnidapt.pd.data.StimulationParameterDraft as StoredStimulationParameter
import com.omnidapt.pd.data.SymptomFeedback
import com.omnidapt.pd.data.TelehealthSession
import com.omnidapt.pd.data.TherapyParameters
import com.omnidapt.pd.data.UserRole
import com.omnidapt.pd.real.OminidaptApplication
import com.omnidapt.pd.real.RealRepository
import com.omnidapt.pd.real.ReminderWorker
import com.omnidapt.pd.real.ble.BleCentralClient
import com.omnidapt.pd.real.ble.BleLinkState
import com.omnidapt.pd.real.ble.DeviceCommandDispatcher
import com.omnidapt.pd.real.algorithm.EdgeInferenceController
import com.omnidapt.pd.real.initialization.InitializationController
import com.omnidapt.pd.real.initialization.InitializationUiState
import com.omnidapt.pd.real.network.ApiInitialization
import com.omnidapt.pd.real.network.ApiOptimizationTask
import com.omnidapt.pd.real.network.OptimizationFeedbackBody
import com.omnidapt.pd.real.ui.AdminShell
import com.omnidapt.pd.real.ui.RealDevicePanel
import com.omnidapt.pd.real.ui.RealDoctorShell
import com.omnidapt.pd.real.ui.RealChatPanel
import com.omnidapt.pd.real.ui.RealPatientShell
import com.omnidapt.pd.real.ui.RealLoginScreen
import com.omnidapt.pd.ui.components.AmbientBackdrop
import com.omnidapt.pd.ui.components.AmbientStyle
import com.omnidapt.pd.ui.components.OmniButton as Button
import com.omnidapt.pd.ui.components.OmniIconButton as IconButton
import com.omnidapt.pd.ui.components.OmniOutlinedButton as OutlinedButton
import com.omnidapt.pd.ui.components.OmniTextButton as TextButton
import com.omnidapt.pd.ui.components.omniClickable
import com.omnidapt.pd.ui.motion.OmniMotion
import com.omnidapt.pd.ui.theme.Border
import com.omnidapt.pd.ui.theme.BrandBlue
import com.omnidapt.pd.ui.theme.DeepBlue
import com.omnidapt.pd.ui.theme.Ink
import com.omnidapt.pd.ui.theme.MedicalGreen
import com.omnidapt.pd.ui.theme.MutedText
import com.omnidapt.pd.ui.theme.OminidaptTheme
import com.omnidapt.pd.ui.theme.PageBg
import com.omnidapt.pd.ui.theme.PanelBg
import com.omnidapt.pd.ui.theme.PremiumBorder
import com.omnidapt.pd.ui.theme.PremiumSurface
import com.omnidapt.pd.ui.theme.PremiumSurfaceStrong
import com.omnidapt.pd.ui.theme.SoftRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OminidaptTheme {
                OminidaptApp(
                    realRepository = (application as OminidaptApplication).realRepository,
                )
            }
        }
    }
}

@Composable
fun OminidaptApp(
    repository: MockRepository = remember { MockRepository() },
    realRepository: RealRepository? = null,
) {
    var role by remember(realRepository) {
        val session = realRepository?.currentSession()
        mutableStateOf(
            session?.takeUnless { it.mustChangePassword }?.role?.toAppRole(),
        )
    }

    val ambientStyle = when (role) {
        null -> AmbientStyle.Login
        UserRole.Patient -> AmbientStyle.Patient
        UserRole.Doctor -> AmbientStyle.Doctor
        UserRole.Admin -> AmbientStyle.Doctor
    }
    AmbientBackdrop(style = ambientStyle, modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = role,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(
                        tween(OmniMotion.RoleMillis, easing = FastOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.985f,
                        animationSpec = tween(OmniMotion.RoleMillis, easing = FastOutSlowInEasing)
                    ),
                    initialContentExit = fadeOut(tween(OmniMotion.StateMillis)) + scaleOut(
                        targetScale = 1.01f,
                        animationSpec = tween(OmniMotion.StateMillis)
                    ),
                    sizeTransform = SizeTransform(clip = false)
                )
            },
            label = "roleTransition"
        ) { activeRole ->
            when (activeRole) {
                null -> if (realRepository == null) {
                    LoginScreen(onLogin = { role = it })
                } else {
                    RealLoginScreen(
                        repository = realRepository,
                        onLogin = { role = it },
                    )
                }
                UserRole.Patient -> PatientShell(
                    repository = repository,
                    realRepository = realRepository,
                    bleClient = realRepository?.let {
                        (androidx.compose.ui.platform.LocalContext.current.applicationContext as OminidaptApplication).bleClient
                    },
                    onLogout = {
                        realRepository?.logout()
                        role = null
                    },
                )
                UserRole.Doctor -> DoctorShell(
                    repository = repository,
                    realRepository = realRepository,
                    bleClient = realRepository?.let {
                        (androidx.compose.ui.platform.LocalContext.current.applicationContext as OminidaptApplication).bleClient
                    },
                    onLogout = {
                        realRepository?.logout()
                        role = null
                    },
                )
                UserRole.Admin -> AdminShell(
                    repository = requireNotNull(realRepository),
                    onLogout = {
                        realRepository.logout()
                        role = null
                    },
                )
            }
        }
    }
}

private fun String.toAppRole(): UserRole? = when (lowercase()) {
    "doctor" -> UserRole.Doctor
    "patient" -> UserRole.Patient
    "admin" -> UserRole.Admin
    else -> null
}

@Composable
private fun LoginScreen(onLogin: (UserRole) -> Unit) {
    var selectedRole by remember { mutableStateOf(UserRole.Doctor) }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        val wideLayout = maxWidth >= 760.dp
        val compactHeight = maxHeight < 640.dp
        val formHorizontalPadding = if (maxWidth < 980.dp) 28.dp else 54.dp

        if (wideLayout) {
            Row(Modifier.fillMaxSize()) {
                LoginBrandPanel(
                    modifier = Modifier
                        .weight(0.84f)
                        .fillMaxHeight(),
                    compact = compactHeight
                )
                Box(
                    modifier = Modifier
                        .weight(1.16f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = formHorizontalPadding,
                            vertical = if (compactHeight) 18.dp else 34.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    LoginForm(
                        selectedRole = selectedRole,
                        account = account,
                        password = password,
                        passwordVisible = passwordVisible,
                        compact = compactHeight,
                        onRoleChange = { selectedRole = it },
                        onAccountChange = { account = it },
                        onPasswordChange = { password = it },
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        onRegister = {
                            dialogMessage = "演示版本暂未开放自助注册，请联系系统管理员创建账号。"
                        },
                        onForgotPassword = {
                            dialogMessage = "演示版本可直接选择身份登录；正式版本将接入账号找回流程。"
                        },
                        onLogin = { onLogin(selectedRole) }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoginBrandLockup(compact = true)
                Spacer(Modifier.height(22.dp))
                LoginForm(
                    selectedRole = selectedRole,
                    account = account,
                    password = password,
                    passwordVisible = passwordVisible,
                    compact = compactHeight,
                    onRoleChange = { selectedRole = it },
                    onAccountChange = { account = it },
                    onPasswordChange = { password = it },
                    onTogglePassword = { passwordVisible = !passwordVisible },
                    onRegister = {
                        dialogMessage = "演示版本暂未开放自助注册，请联系系统管理员创建账号。"
                    },
                    onForgotPassword = {
                        dialogMessage = "演示版本可直接选择身份登录；正式版本将接入账号找回流程。"
                    },
                    onLogin = { onLogin(selectedRole) }
                )
            }
        }
    }

    dialogMessage?.let { message ->
        PremiumAlertDialog(
            onDismissRequest = { dialogMessage = null },
            containerColor = PremiumSurfaceStrong,
            tonalElevation = 0.dp,
            title = {
                Text(
                    "Ominidapt PD",
                    color = Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(message, color = MutedText, fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = { dialogMessage = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = BrandBlue)
                ) {
                    Text("知道了")
                }
            }
        )
    }
}

@Composable
private fun LoginBrandPanel(modifier: Modifier = Modifier, compact: Boolean) {
    Surface(
        modifier = modifier.border(width = 1.dp, color = Border),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 34.dp else 48.dp,
                vertical = 34.dp
            ),
            verticalArrangement = Arrangement.Center
        ) {
            LoginBrandLockup(compact = compact)
            Spacer(Modifier.height(if (compact) 22.dp else 34.dp))
            Box(
                modifier = Modifier
                    .width(if (compact) 42.dp else 54.dp)
                    .height(3.dp)
                    .background(BrandBlue, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.height(if (compact) 18.dp else 26.dp))
            Text(
                "帕金森病个体化闭环调控平台",
                color = Ink,
                fontSize = if (compact) 18.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = if (compact) 27.sp else 32.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "医生工作台 · 患者随访端",
                color = MutedText,
                fontSize = if (compact) 13.sp else 15.sp
            )
        }
    }
}

@Composable
private fun LoginBrandLockup(compact: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.mg_logo_mark_transparent),
            contentDescription = "Ominidapt PD Logo",
            modifier = Modifier.size(if (compact) 72.dp else 92.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(if (compact) 12.dp else 16.dp))
        Text(
            "Ominidapt PD",
            color = BrandBlue,
            fontSize = if (compact) 23.sp else 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun LoginForm(
    selectedRole: UserRole,
    account: String,
    password: String,
    passwordVisible: Boolean,
    compact: Boolean,
    onRoleChange: (UserRole) -> Unit,
    onAccountChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onLogin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 470.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumSurfaceStrong),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 24.dp else 32.dp,
                vertical = if (compact) 22.dp else 30.dp
            )
        ) {
            Text(
                "欢迎登录",
                color = Ink,
                fontSize = if (compact) 25.sp else 29.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "请选择身份并登录系统",
                color = MutedText,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(if (compact) 16.dp else 22.dp))
            RoleSelector(selectedRole = selectedRole, onRoleChange = onRoleChange)
            Spacer(Modifier.height(if (compact) 15.dp else 20.dp))
            Text(
                "账号",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = account,
                onValueChange = onAccountChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                placeholder = { Text("请输入手机号、邮箱或工号", fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = loginTextFieldColors()
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "密码",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                placeholder = { Text("请输入密码", fontSize = 14.sp) },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                shape = RoundedCornerShape(8.dp),
                colors = loginTextFieldColors()
            )
            Spacer(Modifier.height(if (compact) 17.dp else 22.dp))
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    if (selectedRole == UserRole.Doctor) "进入医生工作台" else "进入患者端",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(15.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "注册账号",
                    modifier = Modifier.clickable(onClick = onRegister),
                    color = BrandBlue,
                    fontSize = 14.sp
                )
                Text(
                    "忘记密码？",
                    modifier = Modifier.clickable(onClick = onForgotPassword),
                    color = BrandBlue,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RoleSelector(selectedRole: UserRole, onRoleChange: (UserRole) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PremiumSurfaceStrong)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RoleButton(
            text = "医生端",
            icon = Icons.Filled.MedicalServices,
            selected = selectedRole == UserRole.Doctor,
            modifier = Modifier.weight(1f)
        ) {
            onRoleChange(UserRole.Doctor)
        }
        RoleButton(
            text = "患者端",
            icon = Icons.Filled.Person,
            selected = selectedRole == UserRole.Patient,
            modifier = Modifier.weight(1f)
        ) {
            onRoleChange(UserRole.Patient)
        }
    }
}

@Composable
private fun RoleButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) BrandBlue else Color.Transparent)
            .omniClickable(shape = RoundedCornerShape(6.dp), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MutedText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text,
                color = if (selected) Color.White else MutedText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandBlue,
    unfocusedBorderColor = Border,
    focusedLeadingIconColor = BrandBlue,
    unfocusedLeadingIconColor = MutedText,
    focusedTrailingIconColor = BrandBlue,
    unfocusedTrailingIconColor = MutedText,
    cursorColor = BrandBlue,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

@Composable
private fun PatientShell(
    repository: MockRepository,
    onLogout: () -> Unit,
    realRepository: RealRepository? = null,
    bleClient: BleCentralClient? = null,
) {
    val patient = repository.getCurrentPatient()
    val localContext = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var serverPatientId by remember { mutableStateOf<String?>(null) }
    var emergencyPhone by remember { mutableStateOf<String?>(null) }
    val edgeInference = remember(bleClient, realRepository) {
        if (bleClient != null && realRepository != null) {
            EdgeInferenceController(bleClient, realRepository)
        } else {
            null
        }
    }
    val commandDispatcher = remember(bleClient, realRepository) {
        if (bleClient != null && realRepository != null) {
            DeviceCommandDispatcher(bleClient, realRepository)
        } else {
            null
        }
    }
    var tab by remember { mutableStateOf(PatientTab.Home) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showTuningFeedback by remember { mutableStateOf(false) }
    var optimizationTask by remember { mutableStateOf<ApiOptimizationTask?>(null) }
    var optimizationFeedbackLoading by remember { mutableStateOf(false) }
    var optimizationFeedbackError by remember { mutableStateOf<String?>(null) }
    val report = remember(refreshKey) { repository.getPatientReport(patient.id) }
    val tabScrollStates = remember {
        PatientTab.entries.associateWith { ScrollState(initial = 0) }
    }
    val pageOffset = with(LocalDensity.current) { 24.dp.roundToPx() }
    LaunchedEffect(realRepository) {
        realRepository?.cachedPatients()?.firstOrNull()?.let {
            serverPatientId = it.id
            emergencyPhone = it.emergencyPhone
        }
    }
    LaunchedEffect(serverPatientId, edgeInference) {
        serverPatientId?.let {
            edgeInference?.start(it)
            commandDispatcher?.start(it)
        }
    }
    LaunchedEffect(realRepository, serverPatientId) {
        val patientId = serverPatientId ?: return@LaunchedEffect
        val real = realRepository ?: return@LaunchedEffect
        while (true) {
            runCatching { real.optimizationTasks(patientId) }
                .onSuccess { tasks ->
                    optimizationTask = tasks.firstOrNull {
                        it.status !in setOf("approved", "rejected", "failed")
                    }
                }
                .onFailure { optimizationFeedbackError = it.message }
            delay(1_000)
        }
    }
    DisposableEffect(edgeInference, commandDispatcher) {
        onDispose {
            edgeInference?.stop()
            commandDispatcher?.stop()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.Center),
            bottomBar = {
                PatientBottomBar(selected = tab, onSelected = { tab = it })
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 25.dp, vertical = 6.dp)
            ) {
                PatientTopBar(onLogout = onLogout)
                if (bleClient != null && tab == PatientTab.Home) {
                    RealDevicePanel(
                        client = bleClient,
                        inference = edgeInference,
                        dispatcher = commandDispatcher,
                        repository = realRepository,
                        patientId = serverPatientId,
                    )
                }
                Spacer(Modifier.height(4.dp))
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    transitionSpec = {
                        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        (slideInHorizontally(
                            animationSpec = tween(OmniMotion.PageMillis, easing = FastOutSlowInEasing)
                        ) { direction * pageOffset } + fadeIn(tween(OmniMotion.PageMillis)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(OmniMotion.PageMillis, easing = FastOutSlowInEasing)
                                ) { -direction * pageOffset } + fadeOut(tween(OmniMotion.StateMillis))
                            )
                            .using(SizeTransform(clip = false))
                    },
                    label = "patientTabTransition"
                ) { activeTab ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(tabScrollStates.getValue(activeTab))
                    ) {
                        when (activeTab) {
                            PatientTab.Home -> PatientHome(
                                report = report,
                                onSubmitFeedback = {
                                    repository.submitSymptomFeedback(it)
                                    realRepository?.let { real ->
                                        scope.launch {
                                            real.enqueueSymptom(
                                                patientId = serverPatientId,
                                                tremor = it.tremor,
                                                rigidity = it.rigidity,
                                                speech = it.speech,
                                                note = it.note,
                                            )
                                        }
                                    }
                                    refreshKey++
                                },
                                onMedicationTaken = {
                                    repository.markMedicationTaken(patient.id, System.currentTimeMillis())
                                    realRepository?.let { real ->
                                        scope.launch {
                                            real.enqueueMedication(serverPatientId, "taken")
                                        }
                                    }
                                    refreshKey++
                                },
                                onMedicationSnoozed = {
                                    ReminderWorker.schedule(localContext)
                                    realRepository?.let { real ->
                                        scope.launch {
                                            real.enqueueMedication(serverPatientId, "snoozed")
                                        }
                                    }
                                },
                            )
                            PatientTab.Report -> PatientReportScreen(
                                report = report,
                                onStartTuning = {
                                    optimizationFeedbackError = if (optimizationTask == null) {
                                        "医生尚未创建参数优化任务"
                                    } else {
                                        null
                                    }
                                    showTuningFeedback = true
                                }
                            )
                            PatientTab.Telehealth -> if (realRepository != null) {
                                RealChatPanel(
                                    repository = realRepository,
                                    patientId = serverPatientId,
                                    currentUserId = realRepository.currentSession()?.userId,
                                    onDial = {
                                        emergencyPhone?.let { phone ->
                                            localContext.startActivity(
                                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")),
                                            )
                                        }
                                    },
                                )
                            } else {
                                PatientTelehealthScreen(
                                    onCallDoctor = {
                                        emergencyPhone?.let { phone ->
                                            localContext.startActivity(
                                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")),
                                            )
                                        }
                                    },
                                    onEmergency = {
                                        emergencyPhone?.let { phone ->
                                            localContext.startActivity(
                                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")),
                                            )
                                        }
                                    },
                                )
                            }
                            PatientTab.Profile -> PatientProfileScreen(patient = patient, onLogout = onLogout)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showTuningFeedback,
            enter = fadeIn(tween(OmniMotion.DialogMillis)) + scaleIn(
                initialScale = 0.97f,
                animationSpec = tween(OmniMotion.DialogMillis, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(OmniMotion.DialogMillis)) + scaleOut(
                targetScale = 0.985f,
                animationSpec = tween(OmniMotion.DialogMillis)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B1730).copy(alpha = 0.38f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                RealTuningFeedbackDialog(
                    task = optimizationTask,
                    loading = optimizationFeedbackLoading,
                    externalError = optimizationFeedbackError,
                    onDismiss = { showTuningFeedback = false },
                    onSubmit = { answers, sideEffects ->
                        val task = optimizationTask
                        val real = realRepository
                        if (task == null || real == null) {
                            optimizationFeedbackError = "当前没有可提交的优化任务"
                        } else {
                            optimizationFeedbackLoading = true
                            optimizationFeedbackError = null
                            scope.launch {
                                runCatching {
                                    real.submitOptimizationFeedback(
                                        task.id,
                                        OptimizationFeedbackBody(
                                            event_id = UUID.randomUUID().toString(),
                                            task_id = task.id,
                                            answers = answers,
                                            side_effects = sideEffects,
                                            parameters = task.current_parameters,
                                        ),
                                    )
                                }.onSuccess {
                                    optimizationTask = it.task
                                    showTuningFeedback = false
                                }.onFailure {
                                    optimizationFeedbackError = it.message ?: "问卷提交失败"
                                }
                                optimizationFeedbackLoading = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PatientTopBar(onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.mg_logo_mark_transparent),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text("Ominidapt PD", color = BrandBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onLogout) {
            Image(
                painter = painterResource(id = R.drawable.mg_notification),
                contentDescription = "退出",
                modifier = Modifier.size(27.dp)
            )
        }
    }
}

@Composable
private fun PatientBottomBar(selected: PatientTab, onSelected: (PatientTab) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        color = PremiumSurfaceStrong,
        shadowElevation = 12.dp,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PremiumBorder.copy(alpha = 0.75f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PatientNavItem(PatientTab.Home, selected, "首页", Icons.Filled.Home, onSelected)
            PatientNavItem(PatientTab.Report, selected, "数据报告", Icons.Filled.BarChart, onSelected)
            PatientNavItem(PatientTab.Telehealth, selected, "远程诊疗", Icons.Filled.MedicalServices, onSelected)
            PatientNavItem(PatientTab.Profile, selected, "个人信息", Icons.Filled.Person, onSelected)
        }
    }
}

@Composable
private fun RowScope.PatientNavItem(
    tab: PatientTab,
    selected: PatientTab,
    label: String,
    icon: ImageVector,
    onSelected: (PatientTab) -> Unit
) {
    val active = selected == tab
    val indicatorColor by animateColorAsState(
        targetValue = if (active) Color(0xFFE8F2FF) else Color.Transparent,
        animationSpec = tween(OmniMotion.StateMillis),
        label = "patientNavIndicator"
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) Color(0xFF1069E3) else Color(0xFF666E7E),
        animationSpec = tween(OmniMotion.StateMillis),
        label = "patientNavContent"
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(horizontal = 4.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(indicatorColor)
            .omniClickable(shape = RoundedCornerShape(15.dp)) { onSelected(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val iconRes = when (tab) {
            PatientTab.Home -> R.drawable.mg_nav_home
            PatientTab.Report -> R.drawable.mg_nav_report
            PatientTab.Telehealth -> R.drawable.mg_nav_telehealth
            PatientTab.Profile -> R.drawable.mg_nav_profile
        }
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier
                .size(25.dp)
                .graphicsLayer {
                    scaleX = if (active) 1.06f else 1f
                    scaleY = if (active) 1.06f else 1f
                    alpha = if (active) 1f else 0.72f
                }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PatientHome(
    report: PatientReport,
    onSubmitFeedback: (SymptomFeedback) -> Unit,
    onMedicationTaken: () -> Unit,
    onMedicationSnoozed: () -> Unit = {},
) {
    var tremor by remember { mutableStateOf(report.latestFeedback.tremor.toFloat()) }
    var rigidity by remember { mutableStateOf(report.latestFeedback.rigidity.toFloat()) }
    var speech by remember { mutableStateOf(report.latestFeedback.speech.toFloat()) }
    var saved by remember { mutableStateOf(false) }

    Text("晚上好，请完成今日症状反馈", color = Color(0xFF1A1A1A), fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text("坚持记录，帮助医生更好地了解您的状态", color = Color(0xFF808593), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(14.dp))
    MasterGoSymptomCard("震颤", R.drawable.mg_tremor, tremor, { tremor = it })
    Spacer(Modifier.height(20.dp))
    MasterGoSymptomCard("僵硬", R.drawable.mg_rigidity, rigidity, { rigidity = it })
    Spacer(Modifier.height(20.dp))
    MasterGoSymptomCard("吐词不清", R.drawable.mg_speech, speech, { speech = it })
    Spacer(Modifier.height(19.dp))
    MasterGoHomeCard(height = 119.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(R.drawable.mg_alarm),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("当前时间19：30，是否已按时用药？", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF16B565), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("今日用药提醒已开启", color = Color(0xFF16B565), fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MedicationActionButton(
                text = if (saved) "已保存" else "已用药",
                selected = true,
                onClick = {
                    onMedicationTaken()
                    onSubmitFeedback(SymptomFeedback(tremor.toInt(), rigidity.toInt(), speech.toInt()))
                    saved = true
                },
                modifier = Modifier.weight(1f)
            )
            MedicationActionButton(
                text = "稍后提醒",
                selected = false,
                onClick = onMedicationSnoozed,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    MasterGoHomeCard(height = 153.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.mg_tip),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("小贴士：", color = Color(0xFF1069E3), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Text("完成症状自评后再休息可帮助医生更准确了解您的状态", color = Color(0xFF808593), fontSize = 13.sp, lineHeight = 25.sp)
        Text("状态记录越完整，参数调整越准确", color = Color(0xFF808593), fontSize = 13.sp, lineHeight = 25.sp)
        Text("若出现明显不适请及时联系医生", color = Color(0xFF808593), fontSize = 13.sp, lineHeight = 25.sp)
    }
}

@Composable
private fun MedicationActionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Color(0xFF1069E3) else Color.White
    val fg = if (selected) Color.White else Color(0xFF1069E3)
    val borderColor = if (selected) Color(0xFF1069E3) else Color(0xFF1069E3)
    Box(
        modifier = modifier
            .height(35.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .omniClickable(shape = RoundedCornerShape(18.dp), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.AccessTime,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = text,
                color = fg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MasterGoSymptomCard(
    title: String,
    iconRes: Int,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    MasterGoHomeCard(height = 119.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color(0xFF717789), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("无症状", "轻度", "中度", "重度").forEach { label ->
                Text(label, color = Color(0xFF808593), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        SeveritySelector(value = value, onValueChange = onValueChange)
    }
}

@Composable
private fun SeveritySelector(value: Float, onValueChange: (Float) -> Unit) {
    val selected = value.toInt().coerceIn(0, 3)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index < selected) Color(0xFF1069E3) else Color(0xFFD9D9D9))
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(if (index <= selected) Color(0xFF1069E3) else Color.White)
                        .border(3.dp, if (index <= selected) Color(0xFF1069E3) else Color(0xFFD0D4DC), CircleShape)
                        .omniClickable(shape = CircleShape) { onValueChange(index.toFloat()) }
                )
            }
        }
    }
}

@Composable
private fun MasterGoHomeCard(
    height: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = height),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, PremiumBorder.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            content = content
        )
    }
}

@Composable
private fun MasterGoPatientCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PremiumBorder.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            content = content
        )
    }
}

@Composable
private fun PatientReportScreen(report: PatientReport, onStartTuning: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("数据报告", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.mg_report_calendar),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text("日历视图", color = Color(0xFF1069E3), fontSize = 13.sp)
        }
    }
    Spacer(Modifier.height(4.dp))
    Text("查看近期用药、症状反馈与参数调整记录并进行参数调整", color = Color(0xFF808593), fontSize = 13.sp)
    Spacer(Modifier.height(16.dp))
    MasterGoPatientCard {
        PatientSectionTitle("用药记录", "最近7天", R.drawable.mg_report_calendar)
        MedicationTable(report)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF1069E3), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text("已服药", color = Color(0xFF808593), fontSize = 12.sp)
            Spacer(Modifier.width(14.dp))
            Text("◇", color = Color(0xFF808593), fontSize = 15.sp)
            Spacer(Modifier.width(5.dp))
            Text("未服药", color = Color(0xFF808593), fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(14.dp))
    MasterGoPatientCard {
        PatientSectionTitle("异常报告概览", null, R.drawable.mg_profile_help)
        AlertTimeline(report.alerts)
        Text("该时段内震颤次数较多，请及时向医生反馈", color = SoftRed, fontSize = 12.sp)
    }
    Spacer(Modifier.height(14.dp))
    MasterGoPatientCard {
        PatientSectionTitle("近几次参数调整", null, R.drawable.mg_profile_device)
        ParameterHistoryTable(report.parameterHistory.take(4))
    }
    Spacer(Modifier.height(10.dp))
    MasterGoPatientCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, Color(0xFF1069E3), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = Color(0xFF1069E3), modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("开始调参", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("开始新一轮参数调整，过程可能需要一定时间", color = MutedText, fontSize = 12.sp)
                Text("请保持设备连接并按提示完成反馈", color = MutedText, fontSize = 12.sp)
            }
            Button(
                onClick = onStartTuning,
                modifier = Modifier.width(126.dp).height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1069E3))
            ) {
                Text("开始调参 >", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PatientSectionTitle(title: String, suffix: String?, iconRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (suffix != null) {
            Spacer(Modifier.width(4.dp))
            Text("($suffix)", color = Color(0xFF808593), fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun MedicationTable(report: PatientReport) {
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        Column {
            Row {
                TableCell("日期", width = 72.dp, bold = true)
                report.medications.forEach { TableCell(it.date, width = 58.dp, bold = true) }
            }
            Row {
                TableCell("上午用药", width = 72.dp)
                report.medications.forEach { StatusCell(it.morningTaken) }
            }
            Row {
                TableCell("晚上用药", width = 72.dp)
                report.medications.forEach { StatusCell(it.eveningTaken) }
            }
        }
    }
}

@Composable
private fun StatusCell(done: Boolean) {
    Box(modifier = Modifier.width(58.dp).height(42.dp), contentAlignment = Alignment.Center) {
        if (done) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
        } else {
            Text("-", color = MutedText, fontSize = 22.sp)
        }
    }
}

@Composable
private fun AlertTimeline(alerts: List<AlertEvent>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        alerts.forEach { alert ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(alert.date, color = Ink, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                DotRow(alert.tremorCount)
                Spacer(Modifier.height(6.dp))
                Text(if (alert.rigidityCount > 0) "\\" else "", color = MutedText)
                Spacer(Modifier.height(2.dp))
                Text(if (alert.dysarthriaCount > 0) "\\" else "", color = MutedText)
            }
        }
    }
}

@Composable
private fun AlertSummaryBars(alerts: List<AlertEvent>) {
    val tremor = alerts.sumOf { it.tremorCount }
    val rigidity = alerts.sumOf { it.rigidityCount }
    val speech = alerts.sumOf { it.dysarthriaCount }
    val max = listOf(tremor, rigidity, speech).maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AlertSummaryBar("震颤", tremor, max, BrandBlue)
        AlertSummaryBar("僵硬", rigidity, max, Color(0xFF28B6A6))
        AlertSummaryBar("吐词不清", speech, max, Color(0xFFFF9D28))
    }
}

@Composable
private fun AlertSummaryBar(label: String, value: Int, max: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF5F687B), fontSize = 13.sp, modifier = Modifier.width(70.dp))
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFE7EBF2))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((value.toFloat() / max).coerceIn(0.05f, 1f)).background(color))
        }
        Spacer(Modifier.width(10.dp))
        Text("$value 次", color = Color(0xFF3D3D3D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DoctorAlertTimelineWithLegend(alerts: List<AlertEvent>, timelineMode: Boolean) {
    if (timelineMode) {
        LabeledAlertTimeline(alerts)
    } else {
        AlertSummaryBars(alerts)
    }
}

@Composable
private fun LabeledAlertTimeline(alerts: List<AlertEvent>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.width(74.dp)) {
            Spacer(Modifier.height(25.dp))
            SymptomLabel("震颤", BrandBlue)
            SymptomLabel("僵硬", Color(0xFF28B6A6))
            SymptomLabel("吐词不清", Color(0xFFFF9D28))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            alerts.forEach { alert ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(alert.date, color = Ink, fontSize = 11.sp)
                    SymptomDotRow(alert.tremorCount, BrandBlue)
                    SymptomDotRow(alert.rigidityCount, Color(0xFF28B6A6))
                    SymptomDotRow(alert.dysarthriaCount, Color(0xFFFF9D28))
                }
            }
        }
    }
}

@Composable
private fun SymptomLabel(label: String, color: Color) {
    Row(Modifier.height(19.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color(0xFF5F687B), fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun SymptomDotRow(count: Int, color: Color) {
    Row(Modifier.height(19.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        val visibleCount = count.coerceIn(0, 3)
        if (visibleCount == 0) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFE1E6EE)))
        } else {
            repeat(visibleCount) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
private fun DotRow(count: Int) {
    SymptomDotRow(count, BrandBlue)
}

@Composable
private fun ParameterHistoryTable(parameters: List<TherapyParameters>) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(Color(0xFFF5F7FB)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeightedTableCell("日期", 1.35f, true)
            WeightedTableCell("电流强度", 1.05f, true)
            WeightedTableCell("频率", 0.9f, true)
            WeightedTableCell("脉宽", 0.9f, true)
            WeightedTableCell("接触点", 0.85f, true)
        }
        parameters.forEach {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeightedTableCell(it.date, 1.35f)
                WeightedTableCell("${it.currentMa}mA", 1.05f)
                WeightedTableCell("${it.frequencyHz}Hz", 0.9f)
                WeightedTableCell("${it.pulseWidthUs}μs", 0.9f)
                WeightedTableCell(it.contact, 0.85f)
            }
        }
    }
}

@Composable
private fun RowScope.WeightedTableCell(text: String, weight: Float, bold: Boolean = false) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (bold) Color(0xFF2C3442) else Color(0xFF717789),
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PatientTelehealthScreen(
    onCallDoctor: () -> Unit = {},
    onEmergency: () -> Unit = {},
) {
    Text("联系医生", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text("与当前对接医生沟通症状和用药情况", color = Color(0xFF808593), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    MasterGoPatientCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DoctorAvatar(size = 62.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("王医生", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("神经内科  主任医师", color = Color(0xFF717789), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("市中心医院   帕金森专病门诊", color = Color(0xFF717789), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF15C46B)))
                    Spacer(Modifier.width(5.dp))
                    Text("在线", color = Color(0xFF15C46B), fontSize = 12.sp)
                }
            }
            Text("查看详细资料 ›", color = Color(0xFF1069E3), fontSize = 13.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
    MasterGoPatientCard(modifier = Modifier.height(496.dp)) {
        Text("医生工作时间：周一至周五 08:30-17:30", color = Color(0xFF808593), fontSize = 12.sp)
        Spacer(Modifier.height(9.dp))
        Text("今天 6-14", color = Color(0xFFB0B5BF), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        ChatBubble("王医生，您好！今天手部震颤比昨天明显，肢体有些僵硬，但是我按时服药了", fromPatient = true, time = "19:12")
        ChatBubble("您好，感谢及时反馈。服药后症状有改善吗？", fromPatient = false, time = "19:14")
        ChatBubble("服药之后可以一段时间之内还好，但是时间长了就不行了", fromPatient = true, time = "19:16")
        ChatBubble("好的，如果接下来几天症状继续加重的话可以先考虑调节刺激。", fromPatient = false, time = "19:16")
        ChatBubble("行，谢谢医生，我还有什么要注意的吗？", fromPatient = true, time = "19:18")
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(43.dp)
                .border(1.dp, Color(0xFFE7EBF1), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("输入想咨询的问题......", color = Color(0xFF9DA3AE), fontSize = 13.sp)
        }
        Button(
            onClick = {},
            modifier = Modifier.width(94.dp).height(43.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1069E3))
        ) {
            Image(painterResource(R.drawable.mg_send), contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
            Text("发送", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onCallDoctor,
            modifier = Modifier.weight(1f).height(35.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("电话通话", color = Color(0xFF1069E3), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onEmergency,
            modifier = Modifier.weight(1f).height(35.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3B48))
        ) {
            Text("紧急求助", color = Color(0xFFFF3B48), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DoctorAvatar(size: Dp) {
    Image(
        painter = painterResource(R.drawable.mg_doctor_avatar),
        contentDescription = null,
        modifier = Modifier.size(size).clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ChatBubble(text: String, fromPatient: Boolean, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (fromPatient) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!fromPatient) {
            DoctorAvatar(size = 36.dp)
            Spacer(Modifier.width(8.dp))
        } else {
            Text(time, color = Color(0xFF9DA3AE), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (fromPatient) Alignment.End else Alignment.Start) {
            if (!fromPatient) {
                Text(time, color = Color(0xFF9DA3AE), fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 245.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (fromPatient) Color(0xFFEAF2FF) else Color(0xFFF5F6F8))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(text, color = Color(0xFF4B5363), fontSize = 15.sp, lineHeight = 22.sp)
            }
            if (fromPatient) {
                Text("已读", color = Color(0xFF9DA3AE), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (fromPatient) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFDCEBFF)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF1069E3), modifier = Modifier.size(31.dp))
            }
        }
    }
}

@Composable
private fun PatientProfileScreen(patient: Patient, onLogout: () -> Unit) {
    Text("个人信息", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text("查看个人资料、设备、记录和账户设置", color = Color(0xFF808593), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    ProfileSummaryCard(patient)
    Spacer(Modifier.height(10.dp))
    ProfileMenuRow(R.drawable.mg_profile_record, "诊疗记录", "查看门诊复查、参数调整与随访记录")
    ProfileMenuRow(R.drawable.mg_profile_device, "当前设备", "刺激设备已连接")
    ProfileMenuRow(R.drawable.mg_profile_account, "账号管理", "手机号、登录方式和安全设置")
    ProfileMenuRow(R.drawable.mg_profile_help, "帮助中心", "常见问题、使用说明与在线帮助")
    ProfileMenuRow(R.drawable.mg_profile_emergency, "紧急联系人", "管理紧急联系人与联系方式")
    ProfileMenuRow(R.drawable.mg_profile_privacy, "隐私与权限", "查看隐私政策与授权管理")
    ProfileMenuRow(R.drawable.mg_profile_about, "关于本应用", "版本信息与服务调控")
    Spacer(Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(35.dp))
            .border(2.dp, Color(0xFF1069E3), RoundedCornerShape(35.dp))
            .omniClickable(shape = RoundedCornerShape(35.dp)) { onLogout() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).background(Color(0xFF1069E3)), contentAlignment = Alignment.Center) {
                Text("→", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(9.dp))
            Text("退出登录", color = Color(0xFF1069E3), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileSummaryCard(patient: Patient) {
    MasterGoPatientCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCEBFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF1069E3), modifier = Modifier.size(43.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(patient.name, color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF15C46B), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("设备已连接", color = Color(0xFF15C46B), fontSize = 11.sp, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(8.dp))
                ProfileInfoPair("年龄：${patient.age}岁", "设备植入日期：2023-06-15")
                ProfileInfoPair("性别：${patient.gender}", "患者编号：${patient.number}")
                ProfileInfoPair("体重：61kg", "联系电话：186****4028")
            }
        }
    }
}

@Composable
private fun ProfileInfoPair(left: String, right: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(25.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            left,
            color = Color(0xFF717789),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.82f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            right,
            color = Color(0xFF717789),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.45f)
        )
    }
}

@Composable
private fun ProfileMenuRow(iconRes: Int, title: String, subtitle: String) {
    MasterGoPatientCard(modifier = Modifier.padding(bottom = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color(0xFF717789), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text("›", color = Color(0xFFA8ADB7), fontSize = 34.sp)
        }
    }
}

@Composable
private fun RealTuningFeedbackDialog(
    task: ApiOptimizationTask?,
    loading: Boolean,
    externalError: String?,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Double>, Map<String, Double>) -> Unit,
) {
    var tremorImproved by remember(task?.current_round) { mutableStateOf(true) }
    var rigidImproved by remember(task?.current_round) { mutableStateOf(true) }
    var speechImproved by remember(task?.current_round) { mutableStateOf(true) }
    var motionImproved by remember(task?.current_round) { mutableStateOf(true) }
    var sideEffectSeverity by remember(task?.current_round) { mutableIntStateOf(0) }
    var score by remember(task?.current_round) { mutableIntStateOf(3) }
    val unlocked = task?.questionnaire_unlocked == true

    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumSurfaceStrong),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "参数优化反馈问卷",
                color = Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BrandBlue.copy(alpha = 0.08f),
                modifier = Modifier.border(1.dp, BrandBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            ) {
                Text(
                    text = task?.let { "第 ${it.current_round} / ${it.rounds} 轮 · 当前 ${"%.2f".format(it.current_parameters["current_ma"] ?: 0.0)} mA" }
                        ?: "等待医生创建优化任务",
                    color = BrandBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    task == null -> "当前没有可填写的问卷"
                    unlocked -> "观察期已完成，请根据当前刺激感受填写"
                    else -> "参数已确认，正在完成 ${task.observation_seconds} 秒观察期"
                },
                color = if (unlocked) MedicalGreen else MutedText,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            RealTuningYesNoRow(1, "当前震颤是否改善？", tremorImproved) { tremorImproved = it }
            RealTuningYesNoRow(2, "当前僵硬感是否改善？", rigidImproved) { rigidImproved = it }
            RealTuningYesNoRow(3, "当前发声是否更顺畅？", speechImproved) { speechImproved = it }
            RealTuningYesNoRow(4, "当前动作是否更流畅？", motionImproved) { motionImproved = it }
            TuningQuestionCard(height = 84.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberBadge(5)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "副作用严重度",
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(0 to "无", 3 to "轻微", 6 to "中度", 8 to "严重").forEach { (value, label) ->
                        TogglePill(label, sideEffectSeverity == value) { sideEffectSeverity = value }
                    }
                }
            }
            TuningScoreRow(score) { score = it }
            if (!externalError.isNullOrBlank()) {
                Text(
                    externalError,
                    color = SoftRed,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.width(112.dp).heightIn(min = 44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("稍后填写", color = MutedText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Button(
                    onClick = {
                        val binary: (Boolean) -> Double = { if (it) 10.0 else 0.0 }
                        onSubmit(
                            mapOf(
                                "tremor_relief" to binary(tremorImproved),
                                "rigidity_relief" to binary(rigidImproved),
                                "speech_fluency" to binary(speechImproved),
                                "movement_fluency" to binary(motionImproved),
                                "task_ease" to score * 2.0,
                                "parameter_preference" to score * 2.0,
                            ),
                            mapOf("reported_severity" to sideEffectSeverity.toDouble()),
                        )
                    },
                    enabled = unlocked && !loading,
                    modifier = Modifier.width(128.dp).heightIn(min = 44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text("提交反馈", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RealTuningYesNoRow(
    index: Int,
    question: String,
    yesSelected: Boolean,
    onChange: (Boolean) -> Unit,
) {
    TuningQuestionCard(height = 51.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(index)
            Spacer(Modifier.width(14.dp))
            Text(
                question,
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TogglePill("是", yesSelected) { onChange(true) }
            Spacer(Modifier.width(8.dp))
            TogglePill("否", !yesSelected) { onChange(false) }
        }
    }
}

@Composable
private fun TuningFeedbackDialog(onDismiss: () -> Unit, onSubmit: () -> Unit) {
    var tremorImproved by remember { mutableStateOf(true) }
    var rigidImproved by remember { mutableStateOf(true) }
    var speechImproved by remember { mutableStateOf(true) }
    var motionImproved by remember { mutableStateOf(true) }
    var sideEffect by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(3) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumSurfaceStrong),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("参数优化反馈问卷", color = Color(0xFF3D3D3D), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(231.dp)
                    .heightIn(min = 34.dp)
                    .border(1.dp, Color(0xFF1069E3), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("当前正在第3轮刺激/共12轮", color = Color(0xFF1069E3), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("预计用时", color = Color(0xFF717789), fontSize = 15.sp)
                Text("1", color = Color(0xFF1069E3), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("分钟，请根据当前刺激感受完成反馈", color = Color(0xFF717789), fontSize = 15.sp)
            }
            Spacer(Modifier.height(14.dp))
            TuningYesNoRow(1, "当前震颤是否改善？", tremorImproved) { tremorImproved = it }
            TuningYesNoRow(2, "当前僵硬感是否改善？", rigidImproved) { rigidImproved = it }
            TuningYesNoRow(3, "当前发声是否更顺畅？", speechImproved) { speechImproved = it }
            TuningYesNoRow(4, "当前动作是否更顺畅？", motionImproved) { motionImproved = it }
            TuningSideEffectRow(sideEffect) { sideEffect = it }
            TuningScoreRow(score) { score = it }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.width(112.dp).heightIn(min = 44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("暂不提交", color = Color(0xFF717789), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.width(112.dp).heightIn(min = 44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1069E3))
                ) {
                    Text("提交反馈", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TuningYesNoRow(index: Int, question: String, yesSelected: Boolean, onChange: (Boolean) -> Unit) {
    TuningQuestionCard(height = 51.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(index)
            Spacer(Modifier.width(14.dp))
            Text(question, color = Color(0xFF3D3D3D), fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TogglePill("是", yesSelected) { onChange(true) }
            Spacer(Modifier.width(8.dp))
            TogglePill("否", !yesSelected) { onChange(false) }
        }
    }
}

@Composable
private fun TuningSideEffectRow(sideEffect: Boolean, onChange: (Boolean) -> Unit) {
    TuningQuestionCard(height = 103.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(5)
            Spacer(Modifier.width(14.dp))
            Text("是否出现不适或副作用？", color = Color(0xFF3D3D3D), fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TogglePill("是", sideEffect) { onChange(true) }
            Spacer(Modifier.width(8.dp))
            TogglePill("否", !sideEffect) { onChange(false) }
        }
        Spacer(Modifier.height(9.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .border(1.dp, Color(0xFFE0E4EA), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("请输入不适或副作用描述（选填）", color = Color(0xFFC1C5CC), fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("0/80", color = Color(0xFF3D3D3D), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TuningScoreRow(score: Int, onScoreChange: (Int) -> Unit) {
    TuningQuestionCard(height = 113.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(6)
            Spacer(Modifier.width(14.dp))
            Text(
                "与上一组参数相比，当前方案是否更好？",
                color = Color(0xFF3D3D3D),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..5).forEach {
                Text("$it", color = Color(0xFF3D3D3D), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("明显更差", "略差", "相似", "略好", "明显更好").forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (score == i + 1) Color(0xFFEAF2FF) else Color(0xFFF3F5F8))
                        .border(1.dp, if (score == i + 1) Color(0xFF1069E3) else Color(0xFFD0D5DE), RoundedCornerShape(15.dp))
                        .omniClickable(shape = RoundedCornerShape(15.dp)) { onScoreChange(i + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (score == i + 1) Color(0xFF1069E3) else Color(0xFF808593), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TuningQuestionCard(height: Dp, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .padding(bottom = 7.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, PremiumBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            content = content
        )
    }
}

@Composable
private fun NumberBadge(index: Int) {
    Box(
        modifier = Modifier
            .size(23.dp)
            .clip(CircleShape)
            .border(2.dp, Color(0xFF1069E3), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(index.toString(), color = Color(0xFF1069E3), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TogglePill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(50.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) Color(0xFFEAF2FF) else Color(0xFFF2F4F7))
            .border(1.dp, if (selected) Color(0xFF1069E3) else Color(0xFFD0D5DE), RoundedCornerShape(15.dp))
            .omniClickable(shape = RoundedCornerShape(15.dp), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color(0xFF1069E3) else Color(0xFF808593), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DoctorShell(
    repository: MockRepository,
    onLogout: () -> Unit,
    realRepository: RealRepository? = null,
    bleClient: BleCentralClient? = null,
) {
    TabletDoctorShell(
        repository = repository,
        onLogout = onLogout,
        realRepository = realRepository,
        bleClient = bleClient,
    )
}

@Composable
private fun TabletDoctorShell(
    repository: MockRepository,
    onLogout: () -> Unit,
    realRepository: RealRepository? = null,
    bleClient: BleCentralClient? = null,
) {
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(DoctorScreen.PatientList) }
    var selectedPatientId by remember { mutableStateOf<String?>(null) }
    var viewedPatientId by remember { mutableStateOf<String?>(null) }
    var listVersion by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var deviceState by remember { mutableStateOf(repository.getDeviceState()) }
    var appMessage by remember { mutableStateOf<String?>(null) }
    var serverPatientId by remember { mutableStateOf<String?>(null) }
    val bleSnapshot = bleClient?.snapshot?.collectAsState()?.value
    val initialization = remember(realRepository, bleClient) {
        if (realRepository != null && bleClient != null) {
            InitializationController(bleClient, realRepository)
        } else {
            null
        }
    }
    val edgeInference = remember(realRepository, bleClient) {
        if (realRepository != null && bleClient != null) EdgeInferenceController(bleClient, realRepository) else null
    }
    val commandDispatcher = remember(realRepository, bleClient) {
        if (realRepository != null && bleClient != null) DeviceCommandDispatcher(bleClient, realRepository) else null
    }
    val selectedPatientRecord = remember(listVersion, selectedPatientId) {
        selectedPatientId?.let { repository.getDoctorPatient(it) }
    }
    val patientInfoRecord = remember(listVersion, viewedPatientId, selectedPatientRecord) {
        viewedPatientId?.let { repository.getDoctorPatient(it) } ?: selectedPatientRecord
    }
    val selectedPatient = remember(selectedPatientRecord) { selectedPatientRecord?.toPatient() }
    val report = remember(refreshKey, selectedPatient?.id) {
        selectedPatient?.let { repository.getPatientReport(it.id) }
    }
    LaunchedEffect(realRepository, selectedPatientRecord?.number) {
        if (realRepository != null) {
            runCatching { realRepository.refreshPatients() }
            val cached = realRepository.cachedPatients()
            repository.replaceDoctorPatients(
                cached.map {
                    DoctorPatientRecord(
                        id = it.id,
                        name = it.name.ifBlank { it.code },
                        gender = it.gender.ifBlank { "未填写" },
                        age = it.age ?: 0,
                        number = it.code,
                        implantDate = it.implantDate ?: "未填写",
                        summary = it.summary,
                        group = PatientListGroup.PendingInitialization,
                    )
                },
            )
            listVersion++
            serverPatientId = cached.firstOrNull {
                it.code == selectedPatientRecord?.number || it.code == selectedPatientRecord?.name
            }?.id ?: cached.firstOrNull()?.id
            serverPatientId?.let { initialization?.loadLatest(it) }
        }
    }
    LaunchedEffect(bleSnapshot?.linkState) {
        if (bleClient != null) {
            deviceState = if (bleSnapshot?.linkState == BleLinkState.CONNECTED) {
                DeviceConnectionState.Connected
            } else {
                DeviceConnectionState.Disconnected
            }
        }
    }
    DisposableEffect(serverPatientId, edgeInference, commandDispatcher) {
        serverPatientId?.let {
            edgeInference?.start(it)
            commandDispatcher?.start(it)
        }
        onDispose {
            edgeInference?.stop()
            commandDispatcher?.stop()
        }
    }
    LaunchedEffect(appMessage) {
        if (appMessage != null) {
            delay(2200)
            appMessage = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        val compact = maxWidth < 980.dp
        val denseSidebar = maxHeight < 720.dp
        val sidebarWidth = when {
            maxWidth >= 1180.dp -> 264.dp
            maxWidth >= 980.dp -> 232.dp
            else -> 198.dp
        }
        val pagePadding = when {
            maxWidth >= 1180.dp -> 18.dp
            maxWidth >= 980.dp -> 14.dp
            else -> 10.dp
        }
        val gap = if (compact) 10.dp else 12.dp

        Row(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TabletDoctorSidebar(
                patient = selectedPatientRecord,
                selected = selected,
                sidebarWidth = sidebarWidth,
                compact = compact,
                dense = denseSidebar,
                deviceState = deviceState,
                onSelected = { screen ->
                    if (!screen.requiresSelectedPatient() || selectedPatientRecord != null) {
                        selected = screen
                        if (screen == DoctorScreen.PatientInfo) viewedPatientId = selectedPatientId
                    } else {
                        appMessage = "请先在患者列表中长按选中患者"
                    }
                },
                onConnectDevice = {
                    if (bleClient != null) {
                        bleClient.connect()
                        appMessage = "正在扫描电脑科研模拟设备"
                    } else {
                        deviceState = repository.connectDevice()
                        appMessage = "设备连接成功"
                    }
                },
                onDisconnectDevice = {
                    if (bleClient != null) {
                        bleClient.disconnect()
                        appMessage = "电脑科研模拟设备已断开"
                    } else {
                        deviceState = repository.disconnectDevice()
                        appMessage = "设备已断开"
                    }
                },
                onOpenSettings = {
                    selected = DoctorScreen.Settings
                },
                onLogout = onLogout
            )
            TabletDoctorContent(
                repository = repository,
                patient = selectedPatient,
                patientRecord = selectedPatientRecord,
                patientInfoRecord = patientInfoRecord,
                report = report,
                selected = selected,
                compact = compact,
                gap = gap,
                deviceState = deviceState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(pagePadding),
                onLogout = onLogout,
                listVersion = listVersion,
                selectedPatientId = selectedPatientId,
                onPatientSelected = { patientId ->
                    selectedPatientId = patientId
                    viewedPatientId = patientId
                    selected = DoctorScreen.PatientInfo
                    refreshKey++
                    appMessage = "已选择当前患者"
                },
                onPatientViewed = { patientId ->
                    viewedPatientId = patientId
                    selected = DoctorScreen.PatientInfo
                    appMessage = "正在查看患者档案，长按患者行可设为当前患者"
                },
                onPatientDeleted = { patientId ->
                    if (patientId == selectedPatientId) {
                        selectedPatientId = null
                        selected = DoctorScreen.PatientList
                    }
                    if (patientId == viewedPatientId) viewedPatientId = null
                    listVersion++
                    refreshKey++
                    appMessage = "患者已删除"
                },
                onPatientsChanged = {
                    listVersion++
                    refreshKey++
                },
                onParametersChanged = { refreshKey++ },
                onDeviceStateChanged = { next ->
                    deviceState = next
                    refreshKey++
                },
                realRepository = realRepository,
                bleClient = bleClient,
                initialization = initialization,
                edgeInference = edgeInference,
                commandDispatcher = commandDispatcher,
                serverPatientId = serverPatientId,
                showMessage = { appMessage = it }
            )
        }
        AnimatedContent(
            targetState = appMessage,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            transitionSpec = {
                (slideInVertically(
                    animationSpec = tween(OmniMotion.StateMillis, easing = FastOutSlowInEasing)
                ) { it / 2 } + fadeIn(tween(OmniMotion.StateMillis)))
                    .togetherWith(
                        slideOutVertically(tween(OmniMotion.StateMillis)) { it / 2 } +
                            fadeOut(tween(OmniMotion.StateMillis))
                    )
            },
            label = "doctorToastTransition"
        ) { message ->
            if (message != null) {
                DoctorToast(message = message)
            }
        }
    }
}

@Composable
private fun TabletDoctorSidebar(
    patient: DoctorPatientRecord?,
    selected: DoctorScreen,
    sidebarWidth: Dp,
    compact: Boolean,
    dense: Boolean,
    deviceState: DeviceConnectionState,
    onSelected: (DoctorScreen) -> Unit,
    onConnectDevice: () -> Unit,
    onDisconnectDevice: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(
                        PremiumSurfaceStrong,
                        Color(0xF7F8FBFF),
                        Color(0xF2F1F6FF)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = PremiumBorder.copy(alpha = 0.8f),
                shape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
            )
            .padding(
                horizontal = if (compact) 14.dp else 18.dp,
                vertical = if (dense) 8.dp else 16.dp
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.mg_logo_mark_transparent),
                contentDescription = "Ominidapt PD",
                modifier = Modifier.size(if (dense || compact) 50.dp else 58.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Ominidapt PD",
                    color = Color(0xFF1069E3),
                    fontSize = if (dense || compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "CLINICAL INTELLIGENCE",
                    color = Color(0xFF7D91B2),
                    fontSize = if (dense || compact) 7.sp else 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(if (dense) 8.dp else if (compact) 18.dp else 22.dp))
        DoctorSidePatientCard(patient = patient, compact = compact || dense)
        Spacer(Modifier.height(if (dense) 8.dp else if (compact) 16.dp else 18.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (dense) 2.dp else if (compact) 5.dp else 7.dp)
        ) {
            TabletMenuItem("患者列表", R.drawable.doctor_nav_patients, DoctorScreen.PatientList, selected, compact || dense, true, onSelected)
            TabletMenuItem("文件导出", R.drawable.doctor_nav_export, DoctorScreen.Export, selected, compact || dense, true, onSelected)
            TabletMenuItem("个人设置", R.drawable.doctor_nav_settings, DoctorScreen.Settings, selected, compact || dense, true, onSelected)
            TabletMenuItem("患者信息", R.drawable.doctor_nav_patient_info, DoctorScreen.PatientInfo, selected, compact || dense, patient != null, onSelected)
            TabletMenuItem("初始化与参数调整", R.drawable.doctor_nav_initialization, DoctorScreen.ParameterAdjustment, selected, compact || dense, patient != null, onSelected)
            TabletMenuItem("实时观测", R.drawable.doctor_nav_realtime, DoctorScreen.RealtimeMonitor, selected, compact || dense, patient != null, onSelected)
        }
        Spacer(Modifier.weight(1f))
        DoctorDeviceStatusCard(
            deviceState = deviceState,
            compact = compact || dense,
            onToggleConnection = {
                if (deviceState == DeviceConnectionState.Connected) onDisconnectDevice() else onConnectDevice()
            },
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
private fun DoctorSidePatientCard(patient: DoctorPatientRecord?, compact: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumSurfaceStrong),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PremiumBorder, RoundedCornerShape(16.dp))
                .padding(if (compact) 10.dp else 14.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color(0xFF717789), modifier = Modifier.size(if (compact) 20.dp else 24.dp))
            Spacer(Modifier.width(8.dp))
            Text("当前选中患者", color = Color(0xFF3D3D3D), fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
        if (patient != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PatientAvatar(if (compact) 38.dp else 50.dp)
                Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
                Column {
                    Text(patient.name, color = Color(0xFF3D3D3D), fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.Bold)
                    Text("${patient.gender}  ${patient.age}岁", color = Color(0xFF717789), fontSize = if (compact) 10.sp else 12.sp)
                    Text("患者编号：${patient.number}", color = Color(0xFF717789), fontSize = if (compact) 10.sp else 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Text("未选择患者", color = Color(0xFF3D3D3D), fontSize = if (compact) 18.sp else 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("请在患者列表中长按选中患者", color = Color(0xFF808593), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        }
    }
}

@Composable
private fun TabletMenuItem(
    label: String,
    iconRes: Int,
    screen: DoctorScreen,
    selected: DoctorScreen,
    compact: Boolean,
    enabled: Boolean,
    onSelected: (DoctorScreen) -> Unit
) {
    val active = selected == screen && enabled
    val contentColor = when {
        active -> Color.White
        enabled -> Color(0xFF6D7486)
        else -> Color(0xFFB8BEC9)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 42.dp else 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    if (active) {
                        listOf(Color(0xFF0D6DE8), Color(0xFF287FF0), Color(0xFF416FE2))
                    } else {
                        listOf(Color.Transparent, Color.Transparent)
                    }
                )
            )
            .omniClickable(
                enabled = enabled,
                shape = RoundedCornerShape(12.dp)
            ) { onSelected(screen) }
            .padding(horizontal = if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(if (active) 24.dp else 12.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFF9FE8FF) else Color.Transparent)
        )
        Spacer(Modifier.width(if (compact) 7.dp else 9.dp))
        Box(
            Modifier
                .size(if (compact) 28.dp else 30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (active) Color.White.copy(alpha = 0.14f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (compact) 21.dp else 23.dp)
            )
        }
        Spacer(Modifier.width(if (compact) 7.dp else 9.dp))
        Text(
            label,
            color = contentColor,
            fontSize = if (compact) 14.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DoctorDeviceStatusCard(
    deviceState: DeviceConnectionState,
    compact: Boolean,
    onToggleConnection: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val connected = deviceState == DeviceConnectionState.Connected
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(if (connected) Color(0xFF52E68A) else Color(0xFFD7DADF)))
            Spacer(Modifier.width(8.dp))
            Text(
                if (connected) "设备已连接" else "暂无设备连接",
                color = Color(0xFF717789),
                fontSize = if (compact) 11.sp else 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onToggleConnection,
                modifier = Modifier.width(if (compact) 68.dp else 78.dp).height(28.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
            ) {
                Text(if (connected) "断开" else "连接", fontSize = 10.sp, maxLines = 1)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 7.dp), color = Color(0xFFE8ECF2))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .omniClickable(shape = RoundedCornerShape(10.dp), onClick = onOpenSettings)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.doctor_system_settings),
                contentDescription = null,
                tint = Color(0xFF717789),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("系统设置", color = Color(0xFF717789), fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DoctorDeviceCard(
    deviceState: DeviceConnectionState,
    onToggleConnection: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val connected = deviceState == DeviceConnectionState.Connected
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(if (connected) Color(0xFF52E68A) else Color(0xFFD7DADF)))
            Spacer(Modifier.width(12.dp))
            Text(if (connected) "设备已连接" else "暂无设备连接", color = Color(0xFF717789), fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onToggleConnection, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(16.dp)) {
                Text(if (connected) "断开设备" else "连接设备", fontSize = 12.sp)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFE8ECF2))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .omniClickable(shape = RoundedCornerShape(10.dp), onClick = onOpenSettings)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = Color(0xFF717789), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("系统设置", color = Color(0xFF717789), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DoctorToast(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(min = 220.dp, max = 420.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E2A3A),
        shadowElevation = 10.dp
    ) {
        Text(
            message,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun PremiumAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    containerColor: Color = PremiumSurfaceStrong,
    tonalElevation: Dp = 8.dp
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.widthIn(max = 600.dp),
        dismissButton = dismissButton,
        icon = icon,
        title = title?.let { titleContent ->
            {
                Column {
                    Box(
                        Modifier
                            .width(42.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandBlue, Color(0xFF45C9ED), Color(0xFF8274E9))
                                )
                            )
                    )
                    Spacer(Modifier.height(9.dp))
                    titleContent()
                }
            }
        },
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = BrandBlue,
        titleContentColor = Ink,
        textContentColor = MutedText,
        tonalElevation = tonalElevation
    )
}

@Composable
private fun TabletDoctorContent(
    repository: MockRepository,
    patient: Patient?,
    patientRecord: DoctorPatientRecord?,
    patientInfoRecord: DoctorPatientRecord?,
    report: PatientReport?,
    selected: DoctorScreen,
    compact: Boolean,
    gap: Dp,
    deviceState: DeviceConnectionState,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    listVersion: Int,
    selectedPatientId: String?,
    onPatientSelected: (String) -> Unit,
    onPatientViewed: (String) -> Unit,
    onPatientDeleted: (String) -> Unit,
    onPatientsChanged: () -> Unit,
    onParametersChanged: () -> Unit,
    onDeviceStateChanged: (DeviceConnectionState) -> Unit,
    realRepository: RealRepository? = null,
    bleClient: BleCentralClient? = null,
    initialization: InitializationController? = null,
    edgeInference: EdgeInferenceController? = null,
    commandDispatcher: DeviceCommandDispatcher? = null,
    serverPatientId: String? = null,
    showMessage: (String) -> Unit
) {
    val screenOffset = with(LocalDensity.current) { 18.dp.roundToPx() }
    val screenScrollStates = remember {
        DoctorScreen.entries.associateWith { ScrollState(initial = 0) }
    }
    AnimatedContent(
        targetState = selected,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(OmniMotion.PageMillis, easing = FastOutSlowInEasing)
            ) { direction * screenOffset } + fadeIn(tween(OmniMotion.PageMillis)))
                .togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(OmniMotion.StateMillis, easing = FastOutSlowInEasing)
                    ) { -direction * screenOffset } + fadeOut(tween(OmniMotion.StateMillis))
                )
                .using(SizeTransform(clip = false))
        },
        label = "doctorPageTransition"
    ) { activeScreen ->
        when (activeScreen) {
            DoctorScreen.PatientList -> DoctorScrollableContent(screenScrollStates.getValue(activeScreen)) {
                TabletPatientListPage(
                    repository = repository,
                    compact = compact,
                    gap = gap,
                    listVersion = listVersion,
                    selectedPatientId = selectedPatientId,
                    onPatientSelected = onPatientSelected,
                    onPatientViewed = onPatientViewed,
                    onPatientDeleted = onPatientDeleted,
                    onPatientsChanged = onPatientsChanged
                )
            }
            DoctorScreen.PatientInfo -> DoctorScrollableContent(screenScrollStates.getValue(activeScreen)) {
                if (patientInfoRecord != null) {
                    TabletPatientInfoPage(
                        repository = repository,
                        patient = patientInfoRecord,
                        compact = compact,
                        gap = gap,
                        deviceState = deviceState,
                        onPatientUpdated = onPatientsChanged,
                        onDeviceStateChanged = onDeviceStateChanged,
                        showMessage = showMessage
                    )
                } else {
                    MissingPatientPanel()
                }
            }
            DoctorScreen.Export -> DoctorScrollableContent(screenScrollStates.getValue(activeScreen)) {
                TabletExportPageV2(
                    repository = repository,
                    compact = compact,
                    gap = gap,
                    patient = patientRecord,
                    realRepository = realRepository,
                    serverPatientId = serverPatientId,
                    showMessage = showMessage
                )
            }
            DoctorScreen.Settings -> DoctorScrollableContent(screenScrollStates.getValue(activeScreen)) {
                TabletSettingsPage(
                    repository = repository,
                    compact = compact,
                    gap = gap,
                    onLogout = onLogout,
                    showMessage = showMessage
                )
            }
            DoctorScreen.ParameterAdjustment -> if (patient != null && report != null) {
                TabletParameterPageV3(
                    repository = repository,
                    patient = patient,
                    report = report,
                    compact = compact,
                    gap = gap,
                    onParametersChanged = onParametersChanged,
                    showMessage = showMessage,
                    initialization = initialization,
                    bleClient = bleClient,
                    edgeInference = edgeInference,
                    serverPatientId = serverPatientId,
                    realRepository = realRepository,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                DoctorScrollableContent(screenScrollStates.getValue(activeScreen)) { MissingPatientPanel() }
            }
            DoctorScreen.RealtimeMonitor -> DoctorScrollableContent(screenScrollStates.getValue(activeScreen)) {
                if (patient != null) {
                    TabletRealtimePage(
                        repository = repository,
                        patient = patient,
                        compact = compact,
                        gap = gap,
                        showMessage = showMessage,
                        realRepository = realRepository,
                        bleClient = bleClient,
                        edgeInference = edgeInference,
                        commandDispatcher = commandDispatcher,
                        serverPatientId = serverPatientId,
                    )
                } else {
                    MissingPatientPanel()
                }
            }
        }
    }
}

@Composable
private fun DoctorScrollableContent(content: @Composable ColumnScope.() -> Unit) {
    DoctorScrollableContent(rememberScrollState(), content)
}

@Composable
private fun DoctorScrollableContent(
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        content = content
    )
}

@Composable
private fun TabletPageTitle(title: String) {
    val eyebrow = when (title) {
        "患者列表" -> "PATIENT DIRECTORY"
        "患者信息" -> "PATIENT INSIGHT"
        "文件导出" -> "DATA EXPORT HUB"
        "个人设置" -> "CLINICIAN WORKSPACE"
        "初始化与参数调整" -> "NEUROMODULATION WORKFLOW"
        "实时观测" -> "REAL-TIME SIGNAL CENTER"
        else -> "CLINICAL CONTROL CENTER"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        PremiumSurfaceStrong,
                        Color(0xEEF1F8FF),
                        Color(0xEDEEF1FF)
                    )
                )
            )
            .border(1.dp, PremiumBorder.copy(alpha = 0.94f), RoundedCornerShape(18.dp))
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(38.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF34C7F3), BrandBlue, Color(0xFF6D5DE7))
                    )
                )
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                eyebrow,
                color = Color(0xFF6F86A8),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
            Text(
                title,
                color = Color(0xFF202B43),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFEAF5FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAE2FA))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(MedicalGreen))
                Spacer(Modifier.width(6.dp))
                Text(
                    "SYSTEM ONLINE",
                    color = Color(0xFF426387),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun TabletPatientListPage(
    repository: MockRepository,
    compact: Boolean,
    gap: Dp,
    listVersion: Int,
    selectedPatientId: String?,
    onPatientSelected: (String) -> Unit,
    onPatientViewed: (String) -> Unit,
    onPatientDeleted: (String) -> Unit,
    onPatientsChanged: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sortField by remember { mutableStateOf(PatientSortField.ImplantDate) }
    var ascending by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var addDialogOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DoctorPatientRecord?>(null) }
    var expandedGroups by remember {
        mutableStateOf(
            setOf(
                PatientListGroup.PendingInitialization,
                PatientListGroup.Focus,
                PatientListGroup.Routine
            )
        )
    }
    val patients = remember(listVersion, query, sortField, ascending) {
        repository.getDoctorPatients(query, sortField, ascending)
    }
    val grouped = patients.groupBy { it.group }

    if (addDialogOpen) {
        AddDoctorPatientDialog(
            onDismiss = { addDialogOpen = false },
            onSave = { record ->
                val saved = repository.addDoctorPatient(record)
                if (saved) {
                    onPatientsChanged()
                    addDialogOpen = false
                }
                saved
            }
        )
    }

    pendingDelete?.let { patient ->
        PremiumAlertDialog(
            containerColor = PremiumSurfaceStrong,
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除患者") },
            text = { Text("确认删除 ${patient.name}（${patient.number}）吗？该操作只影响本地演示列表。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.deleteDoctorPatient(patient.id)
                        onPatientDeleted(patient.id)
                        pendingDelete = null
                    }
                ) {
                    Text("删除", color = SoftRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }

    TabletPageTitle("患者列表")
    DoctorPanel {
        val searchField: @Composable (Modifier) -> Unit = { fieldModifier ->
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = fieldModifier.height(50.dp),
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color(0xFF6681A7),
                        modifier = Modifier.size(21.dp)
                    )
                },
                placeholder = {
                    Text(
                        "搜索姓名、编号、植入日期或情况简介",
                        color = Color(0xFF8B96A9),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = Color(0xFFDCE6F3),
                    focusedContainerColor = Color(0xF7FFFFFF),
                    unfocusedContainerColor = Color(0xF3FFFFFF),
                    cursorColor = BrandBlue
                )
            )
        }
        val actionButtons: @Composable () -> Unit = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(46.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("${sortField.label()} · ${if (ascending) "升序" else "降序"}", fontSize = 13.sp)
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        containerColor = PremiumSurfaceStrong
                    ) {
                        PatientSortField.values().forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field.label()) },
                                onClick = {
                                    if (sortField == field) {
                                        ascending = !ascending
                                    } else {
                                        sortField = field
                                        ascending = field == PatientSortField.Name
                                    }
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Button(
                    onClick = { addDialogOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(46.dp),
                    contentPadding = PaddingValues(horizontal = 17.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("新增患者", fontSize = 13.sp)
                }
            }
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                searchField(Modifier.fillMaxWidth())
                Box(Modifier.align(Alignment.End)) { actionButtons() }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                searchField(Modifier.weight(1f))
                actionButtons()
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    DoctorPanel {
        Text(
            "待初始化 ${grouped[PatientListGroup.PendingInitialization].orEmpty().size} 人，重点关注 ${grouped[PatientListGroup.Focus].orEmpty().size} 人，常规监控 ${grouped[PatientListGroup.Routine].orEmpty().size} 人",
            color = Color(0xFF4B5363),
            fontSize = 15.sp
        )
        Spacer(Modifier.height(4.dp))
        Text("长按患者行可设为当前患者；受限功能需要先选中患者。", color = Color(0xFF8A91A0), fontSize = 12.sp)
    }
    Spacer(Modifier.height(gap))
    listOf(
        PatientListGroup.PendingInitialization,
        PatientListGroup.Focus,
        PatientListGroup.Routine
    ).forEach { group ->
        PatientGroupCard(
            group = group,
            rows = grouped[group].orEmpty(),
            expanded = group in expandedGroups,
            selectedPatientId = selectedPatientId,
            onToggleExpanded = {
                expandedGroups = if (group in expandedGroups) {
                    expandedGroups - group
                } else {
                    expandedGroups + group
                }
            },
            onPatientSelected = onPatientSelected,
            onPatientViewed = onPatientViewed,
            onDeleteRequested = { pendingDelete = it }
        )
        Spacer(Modifier.height(gap))
    }
}

@Composable
private fun PatientGroupCard(
    group: PatientListGroup,
    rows: List<DoctorPatientRecord>,
    expanded: Boolean,
    selectedPatientId: String?,
    onToggleExpanded: () -> Unit,
    onPatientSelected: (String) -> Unit,
    onPatientViewed: (String) -> Unit,
    onDeleteRequested: (DoctorPatientRecord) -> Unit
) {
    DoctorPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .omniClickable(shape = RoundedCornerShape(10.dp), onClick = onToggleExpanded),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(group.label(), color = Color(0xFF717789), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text("${rows.size}人", color = Color(0xFF9AA1AD), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "收起 ▲" else "展开 ▼", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        if (expanded) {
            Spacer(Modifier.height(14.dp))
            PatientListHeader()
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
                    Text("暂无匹配患者", color = Color(0xFF9AA1AD), fontSize = 14.sp)
                }
            }
            rows.forEachIndexed { index, patient ->
                val selected = patient.id == selectedPatientId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(
                            when {
                                selected -> Color(0xFFE6F0FF)
                                index % 2 == 0 -> Color(0xFFF0F3F8)
                                else -> Color.White
                            }
                        )
                        .pointerInput(patient.id) {
                            detectTapGestures(
                                onLongPress = { onPatientSelected(patient.id) }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(4.dp).fillMaxHeight().background(if (selected) BrandBlue else Color.Transparent))
                    WeightedDoctorCell(patient.name, 0.85f, selected)
                    WeightedDoctorCell(patient.number, 1.15f, selected)
                    WeightedDoctorCell(patient.implantDate, 1.15f, selected)
                    WeightedDoctorCell("${patient.gender}，${patient.age}岁，${patient.summary}", 2.8f, selected)
                    TextButton(onClick = { onPatientViewed(patient.id) }, modifier = Modifier.weight(0.55f)) {
                        Text("查看", color = BrandBlue, fontSize = 12.sp)
                    }
                    TextButton(onClick = { onDeleteRequested(patient) }, modifier = Modifier.weight(0.55f)) {
                        Text("删除", color = SoftRed, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientListHeader() {
    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(4.dp))
        WeightedDoctorCell("姓名", 0.85f, true)
        WeightedDoctorCell("患者编号", 1.15f, true)
        WeightedDoctorCell("设备植入时间", 1.15f, true)
        WeightedDoctorCell("情况简介", 2.8f, true)
        WeightedDoctorCell("操作", 1.1f, true)
    }
}

@Composable
private fun RowScope.WeightedDoctorCell(text: String, weight: Float, bold: Boolean = false) {
    Text(
        text,
        color = if (bold) Color(0xFF4B5363) else Color(0xFF5F687B),
        fontSize = 13.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(weight).padding(horizontal = 8.dp)
    )
}

@Composable
private fun AddDoctorPatientDialog(
    onDismiss: () -> Unit,
    onSave: (DoctorPatientRecord) -> Boolean
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("男") }
    var age by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var implantDate by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(PatientListGroup.PendingInitialization) }
    var error by remember { mutableStateOf<String?>(null) }
    val canSave = name.isNotBlank() && gender.isNotBlank() && age.toIntOrNull() != null &&
        number.isNotBlank() && implantDate.isNotBlank() && summary.isNotBlank()

    PremiumAlertDialog(
        containerColor = PremiumSurfaceStrong,
        onDismissRequest = onDismiss,
        title = { Text("新增患者") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("姓名") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(gender, { gender = it }, label = { Text("性别") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    OutlinedTextField(age, { age = it }, label = { Text("年龄") }, singleLine = true, modifier = Modifier.weight(0.7f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(number, { number = it }, label = { Text("患者编号") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(implantDate, { implantDate = it }, label = { Text("植入日期 2026/07/25") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(summary, { summary = it }, label = { Text("情况简介") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PatientListGroup.values().forEach { item ->
                        val selected = group == item
                        if (selected) {
                            Button(onClick = { group = item }, shape = RoundedCornerShape(18.dp)) {
                                Text(item.label())
                            }
                        } else {
                            OutlinedButton(onClick = { group = item }, shape = RoundedCornerShape(18.dp)) {
                                Text(item.label())
                            }
                        }
                    }
                }
                error?.let { Text(it, color = SoftRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val saved = onSave(
                        DoctorPatientRecord(
                            id = number.trim(),
                            name = name.trim(),
                            gender = gender.trim(),
                            age = age.toInt(),
                            number = number.trim(),
                            implantDate = implantDate.trim(),
                            summary = summary.trim(),
                            group = group
                        )
                    )
                    if (!saved) {
                        error = "患者编号已存在，请更换编号。"
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun TabletPatientInfoPage(
    repository: MockRepository,
    patient: DoctorPatientRecord,
    compact: Boolean,
    gap: Dp,
    deviceState: DeviceConnectionState,
    onPatientUpdated: () -> Unit,
    onDeviceStateChanged: (DeviceConnectionState) -> Unit,
    showMessage: (String) -> Unit
) {
    var editOpen by remember(patient.id) { mutableStateOf(false) }
    var telehealthOpen by remember(patient.id) { mutableStateOf(false) }
    val workflow = remember(patient.id) { repository.getInitializationWorkflow(patient.id) }
    val patientReport = remember(patient.id) { repository.getPatientReport(patient.id) }
    val latestParameterDate = patientReport.parameterHistory.firstOrNull()?.date ?: "暂无"
    val latestAlertCount = patientReport.alerts.lastOrNull()?.let {
        it.tremorCount + it.rigidityCount + it.dysarthriaCount
    } ?: 0
    val implantTimelineDate = patient.implantDate.split("/").let { parts ->
        if (parts.size == 3) "${parts[0]}年${parts[1].toIntOrNull() ?: parts[1]}月${parts[2].toIntOrNull() ?: parts[2]}日" else patient.implantDate
    }
    if (editOpen) {
        EditDoctorPatientDialog(
            patient = patient,
            onDismiss = { editOpen = false },
            onSave = { updated ->
                val saved = repository.updateDoctorPatient(updated)
                if (saved) {
                    onPatientUpdated()
                    editOpen = false
                    showMessage("患者档案已保存")
                }
                saved
            }
        )
    }
    if (telehealthOpen) {
        TelehealthDialog(
            repository = repository,
            patient = patient,
            onDismiss = { telehealthOpen = false },
            showMessage = showMessage
        )
    }
    TabletPageTitle("患者信息")
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(patient.name, color = Color.Black, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                PatientInfoLine("患者编号：${patient.number}", "植入日期：${patient.implantDate}")
                PatientInfoLine("性别：${patient.gender}", "年龄：${patient.age}岁", "体重：61kg", "身高：165cm")
                PatientInfoLine("联系电话：18624514028", "紧急联系人电话：18639085113")
                Text(patient.summary, color = Color(0xFF717789), fontSize = 14.sp, lineHeight = 20.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DoctorActionButton("编辑档案", R.drawable.doctor_action_edit) { editOpen = true }
                DoctorActionButton("重新连接", R.drawable.doctor_action_reconnect) {
                    val next = repository.connectDevice()
                    onDeviceStateChanged(next)
                    showMessage(if (deviceState == DeviceConnectionState.Connected) "设备已刷新连接状态" else "设备重新连接成功")
                }
                DoctorActionButton("远程诊疗", R.drawable.doctor_action_telehealth) { telehealthOpen = true }
            }
        }
    }
    Spacer(Modifier.height(gap))
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        StatCard("当前治疗模式", "概率闭环刺激", R.drawable.doctor_stat_treatment, Modifier.weight(1f))
        StatCard(
            "初始化状态",
            if (workflow.step == InitializationStep.Completed) "已完成初始化" else workflow.step.displayLabel(),
            R.drawable.doctor_stat_initialization,
            Modifier.weight(1f)
        )
        StatCard("最近参数更新", latestParameterDate, R.drawable.doctor_stat_parameter, Modifier.weight(1f))
        StatCard("最近异常提醒", if (latestAlertCount == 0) "暂无" else "${latestAlertCount}项", R.drawable.doctor_stat_alert, Modifier.weight(1f))
    }
    Spacer(Modifier.height(gap))
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        Column(Modifier.weight(1.65f)) {
            DoctorPanel {
                Text("当前刺激参数", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                DoctorParameterTable(workflow.stimulationParameters)
            }
            Spacer(Modifier.height(gap))
            DoctorPanel {
                Text("当前用药方案", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                MedicationPlanTable()
            }
        }
        DoctorPanel(modifier = Modifier.weight(0.85f)) {
            Text("既往病史与关键事件", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            TimelineItem("2014年9月8日", "确诊帕金森病", "出现静止性震颤、运动迟缓等症状")
            TimelineItem("2020年3月27日", "出现明显药效波动", "药物疗效下降，出现“开-关”波动现象")
            TimelineItem(implantTimelineDate, "完成DBS植入", "双侧STN植入，术后恢复良好")
            TimelineItem("2024年1月3日", "开始闭环调控", "启动闭环刺激方案，进行长期随访检测")
        }
    }
}

@Composable
private fun PatientInfoLine(vararg items: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        items.forEach { item ->
            Text(item, color = Color(0xFF5F687B), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DoctorActionButton(text: String, iconRes: Int, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.width(150.dp).height(44.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
    ) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF1069E3))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color(0xFF1069E3), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun EditDoctorPatientDialog(
    patient: DoctorPatientRecord,
    onDismiss: () -> Unit,
    onSave: (DoctorPatientRecord) -> Boolean
) {
    var name by remember(patient.id) { mutableStateOf(patient.name) }
    var gender by remember(patient.id) { mutableStateOf(patient.gender) }
    var age by remember(patient.id) { mutableStateOf(patient.age.toString()) }
    var number by remember(patient.id) { mutableStateOf(patient.number) }
    var implantDate by remember(patient.id) { mutableStateOf(patient.implantDate) }
    var summary by remember(patient.id) { mutableStateOf(patient.summary) }
    var group by remember(patient.id) { mutableStateOf(patient.group) }
    var error by remember { mutableStateOf<String?>(null) }
    val canSave = name.isNotBlank() && gender.isNotBlank() && age.toIntOrNull() != null &&
        number.isNotBlank() && implantDate.isNotBlank() && summary.isNotBlank()

    PremiumAlertDialog(
        containerColor = PremiumSurfaceStrong,
        onDismissRequest = onDismiss,
        title = { Text("编辑患者档案") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("姓名") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(gender, { gender = it }, label = { Text("性别") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    OutlinedTextField(age, { age = it }, label = { Text("年龄") }, singleLine = true, modifier = Modifier.weight(0.7f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(number, { number = it }, label = { Text("患者编号") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(implantDate, { implantDate = it }, label = { Text("植入日期") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(summary, { summary = it }, label = { Text("情况简介") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PatientListGroup.values().forEach { item ->
                        if (group == item) {
                            Button(onClick = { group = item }, shape = RoundedCornerShape(18.dp)) { Text(item.label()) }
                        } else {
                            OutlinedButton(onClick = { group = item }, shape = RoundedCornerShape(18.dp)) { Text(item.label()) }
                        }
                    }
                }
                error?.let { Text(it, color = SoftRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val saved = onSave(
                        patient.copy(
                            name = name.trim(),
                            gender = gender.trim(),
                            age = age.toInt(),
                            number = number.trim(),
                            implantDate = implantDate.trim(),
                            summary = summary.trim(),
                            group = group
                        )
                    )
                    if (!saved) error = "患者编号已存在，请更换编号。"
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun TelehealthDialog(
    repository: MockRepository,
    patient: DoctorPatientRecord,
    onDismiss: () -> Unit,
    showMessage: (String) -> Unit
) {
    var session by remember(patient.id) { mutableStateOf<TelehealthSession?>(null) }
    var input by remember { mutableStateOf("") }
    LaunchedEffect(patient.id) {
        session = repository.startTelehealth(patient.id)
    }
    PremiumAlertDialog(
        containerColor = PremiumSurfaceStrong,
        onDismissRequest = onDismiss,
        title = { Text("远程诊疗 - ${patient.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (session?.active == true) "会话进行中，患者端已收到模拟呼叫。" else "会话已结束，可关闭窗口。",
                    color = Color(0xFF5F687B),
                    fontSize = 13.sp
                )
                DoctorPanel {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(min = 120.dp, max = 220.dp).verticalScroll(rememberScrollState())) {
                        session?.messages.orEmpty().forEach { message ->
                            Text("${message.time}  ${message.sender}: ${message.content}", color = Color(0xFF3D3D3D), fontSize = 13.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("发送医嘱或随访说明") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank() && session?.active == true,
                onClick = {
                    session = repository.addTelehealthMessage(session!!.id, input.trim())
                    input = ""
                    showMessage("远程诊疗消息已发送")
                }
            ) {
                Text("发送")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = session?.active == true,
                    onClick = {
                        session = repository.endTelehealth(session!!.id)
                        showMessage("远程诊疗会话已结束")
                    }
                ) {
                    Text("结束会话", color = SoftRed)
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

@Composable
private fun StatCard(title: String, value: String, iconRes: Int, modifier: Modifier = Modifier) {
    DoctorPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFEAF2FF)), contentAlignment = Alignment.Center) {
                Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color(0xFF717789), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(value, color = Color(0xFF3D3D3D), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DoctorParameterTable(parameters: List<StoredStimulationParameter>) {
    val rows = parameters.map {
        listOf(it.condition, it.frequencyHz.toString(), it.amplitudeMv.toString(), it.pulseWidthUs.toString(), if (it.isSafe()) "已审核" else "需复核")
    }
    DoctorTable(listOf("基线状态", "频率(HZ)", "幅值(mV)", "脉宽(μS)", "审核状态"), rows)
}

@Composable
private fun MedicationPlanTable() {
    DoctorTable(listOf("药物", "剂量", "服药时间", "频次"), listOf(listOf("普拉克索片", "250mg", "8:00,21:00", "每日2次")))
}

@Composable
private fun DoctorTable(headers: List<String>, rows: List<List<String>>) {
    Row(Modifier.fillMaxWidth().height(40.dp).background(Color(0xFFF6F8FC)), verticalAlignment = Alignment.CenterVertically) {
        headers.forEach { Text(it, color = Color(0xFF3D3D3D), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
    }
    rows.forEachIndexed { index, row ->
        Row(Modifier.fillMaxWidth().height(42.dp).background(if (index % 2 == 0) Color.White else Color(0xFFFAFBFD)), verticalAlignment = Alignment.CenterVertically) {
            row.forEach { cell ->
                Text(cell, color = Color(0xFF4B5363), fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TimelineItem(date: String, title: String, detail: String) {
    Row(Modifier.padding(bottom = 18.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF1069E3)))
            Box(Modifier.width(1.dp).height(46.dp).background(Color(0xFFB7C7E6)))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(date, color = Color(0xFF5F687B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color(0xFF3D3D3D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = Color(0xFF717789), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TabletExportPageV2(
    repository: MockRepository,
    compact: Boolean,
    gap: Dp,
    patient: DoctorPatientRecord?,
    realRepository: RealRepository? = null,
    serverPatientId: String? = null,
    showMessage: (String) -> Unit
) {
    val realScope = rememberCoroutineScope()
    var searchDraft by remember { mutableStateOf("") }
    var dateRange by remember { mutableStateOf(DoctorDateRange.recentThreeDays) }
    var dateMenu by remember { mutableStateOf(false) }
    var typeFilter by remember { mutableStateOf<ExportFileType?>(null) }
    var typeMenu by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var refresh by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(ExportSettings()) }
    var exporting by remember { mutableStateOf(false) }
    val files = remember(searchDraft, typeFilter, patient?.id, dateRange, refresh) {
        repository.getExportFiles(
            query = searchDraft.trim(),
            type = typeFilter,
            patientId = patient?.id,
            dateFrom = dateRange.from,
            dateTo = dateRange.to
        )
    }
    val selectedFile = files.firstOrNull { it.id in selectedIds }

    LaunchedEffect(exporting) {
        if (exporting) {
            delay(1100)
            repository.createExport(selectedIds, selectedFormat, settings)
            exporting = false
            showMessage("已按 ${selectedFormat.name} 格式完成模拟导出")
        }
    }

    TabletPageTitle("文件导出")
    if (realRepository != null && serverPatientId != null) {
        DoctorPanel {
            Text("服务器真实导出", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("从当前患者数据库生成文件并下载到应用私有 exports 目录。", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("pdf", "csv", "mat", "edf", "eml", "zip").forEach { format ->
                    OutlinedButton(
                        onClick = {
                            realScope.launch {
                                showMessage("正在生成 ${format.uppercase()}…")
                                realRepository.exportPatient(serverPatientId, format).fold(
                                    onSuccess = { showMessage("真实文件已保存：${it.name}") },
                                    onFailure = { showMessage(it.message ?: "真实文件导出失败") },
                                )
                            }
                        },
                    ) { Text(format.uppercase()) }
                }
            }
        }
        Spacer(Modifier.height(gap))
    }
    DoctorPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            Box(Modifier.weight(1f)) {
                ClickableFilterChip("日期范围", dateRange.label) { dateMenu = true }
                DropdownMenu(
                    expanded = dateMenu,
                    onDismissRequest = { dateMenu = false },
                    containerColor = PremiumSurfaceStrong
                ) {
                    DoctorDateRange.options.forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = { dateRange = item; dateMenu = false })
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                ClickableFilterChip("文件类型", typeFilter?.label() ?: "全部类型") { typeMenu = true }
                DropdownMenu(
                    expanded = typeMenu,
                    onDismissRequest = { typeMenu = false },
                    containerColor = PremiumSurfaceStrong
                ) {
                    DropdownMenuItem(text = { Text("全部类型") }, onClick = { typeFilter = null; typeMenu = false })
                    ExportFileType.values().forEach { type ->
                        DropdownMenuItem(text = { Text(type.label()) }, onClick = { typeFilter = type; typeMenu = false })
                    }
                }
            }
            FilterChipLike("当前患者", patient?.let { "${it.name}（${it.number}）" } ?: "全部患者", Modifier.weight(1.25f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchDraft,
                onValueChange = { searchDraft = it },
                singleLine = true,
                label = { Text("搜索文件") },
                modifier = Modifier.weight(1f).height(56.dp)
            )
            Button(
                onClick = {
                    selectedIds = emptySet()
                    showMessage(if (searchDraft.isBlank()) "已显示全部设备文件" else "已搜索：${searchDraft.trim()}")
                },
                modifier = Modifier.width(112.dp).height(42.dp),
                shape = RoundedCornerShape(21.dp)
            ) { Text("搜索文件", fontSize = 13.sp, maxLines = 1) }
        }
    }
    Spacer(Modifier.height(gap))
    BoxWithConstraints {
        val stackPanels = maxWidth < 840.dp
        val listPanel: @Composable () -> Unit = {
            DoctorPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(typeFilter?.label() ?: "全部类型", color = Color(0xFF3D3D3D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { refresh++; selectedIds = emptySet(); showMessage("设备文件列表已刷新") }, shape = RoundedCornerShape(8.dp)) { Text("刷新") }
                }
                Spacer(Modifier.height(14.dp))
                ExportFileHeader()
                files.forEach { file ->
                    ExportFileRow(
                        file = file,
                        selected = file.id in selectedIds,
                        onToggle = { selectedIds = if (file.id in selectedIds) selectedIds - file.id else selectedIds + file.id }
                    )
                }
                if (files.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
                        Text("暂无匹配设备文件", color = Color(0xFFC3C7CF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("已选择${selectedIds.size}项", color = Color(0xFF5F687B), fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        enabled = selectedIds.isNotEmpty() && !exporting,
                        onClick = { exporting = true },
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (exporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BrandBlue)
                            Spacer(Modifier.width(8.dp))
                            Text("导出中", fontSize = 12.sp)
                        } else {
                            Text("批量导出", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        enabled = selectedIds.isNotEmpty() && !exporting,
                        onClick = {
                            val deleted = repository.deleteExportFiles(selectedIds)
                            selectedIds = emptySet()
                            refresh++
                            showMessage("已删除${deleted}条设备文件记录")
                        },
                        modifier = Modifier.height(36.dp)
                    ) { Text("删除", color = SoftRed, fontSize = 12.sp) }
                }
            }
        }
        val sidePanel: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                DoctorPanel {
                    Text("导出类型", color = Color(0xFF3D3D3D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        ExportTypeIconBox(ExportFormat.PDF, selectedFormat) { selectedFormat = it }
                        ExportTypeIconBox(ExportFormat.CSV, selectedFormat) { selectedFormat = it }
                        ExportTypeIconBox(ExportFormat.MAT, selectedFormat) { selectedFormat = it }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        ExportTypeIconBox(ExportFormat.EDF, selectedFormat) { selectedFormat = it }
                        ExportTypeIconBox(ExportFormat.EML, selectedFormat) { selectedFormat = it }
                        ExportTypeIconBox(ExportFormat.ZIP, selectedFormat) { selectedFormat = it }
                    }
                }
                DoctorPanel {
                    Text("导出设置", color = Color(0xFF3D3D3D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    SettingLine("包含患者身份信息", settings.includeIdentity) { settings = settings.copy(includeIdentity = !settings.includeIdentity) }
                    SettingLine("包含患者主观报表", settings.includePatientFeedback) { settings = settings.copy(includePatientFeedback = !settings.includePatientFeedback) }
                    SettingLine("包含脑电片段", settings.includeSignalClips) { settings = settings.copy(includeSignalClips = !settings.includeSignalClips) }
                    SettingLine("包含刺激参数", settings.includeParameterTimeline) { settings = settings.copy(includeParameterTimeline = !settings.includeParameterTimeline) }
                }
                DoctorPanel(modifier = Modifier.height(170.dp)) {
                    Text("导出预览", color = Color(0xFF3D3D3D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (selectedFile != null) {
                        Text(selectedFile.fileName, color = Color(0xFF3D3D3D), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Text("${selectedFile.type.label()} / ${selectedFormat.name} / ${selectedFile.size}", color = Color(0xFF717789), fontSize = 13.sp)
                        Text("记录时间：${selectedFile.generatedAt}", color = Color(0xFF717789), fontSize = 13.sp)
                    } else {
                        Spacer(Modifier.weight(1f))
                        Text("暂无选中文件", color = Color(0xFFC3C7CF), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (stackPanels) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                listPanel()
                sidePanel()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Box(Modifier.weight(1.45f)) { listPanel() }
                Box(Modifier.weight(0.9f)) { sidePanel() }
            }
        }
    }
}

private data class DoctorDateRange(
    val label: String,
    val from: String?,
    val to: String?
) {
    override fun toString(): String = label

    companion object {
        val recentThreeDays = DoctorDateRange("最近三天", "2026-07-23", "2026-07-25")
        val options = listOf(
            recentThreeDays,
            DoctorDateRange("最近一周", "2026-07-19", "2026-07-25"),
            DoctorDateRange("2026-07-23 → 2026-07-25", "2026-07-23", "2026-07-25"),
            DoctorDateRange("全部记录", null, null)
        )
    }
}

@Composable
private fun ClickableFilterChip(label: String, value: String, onClick: () -> Unit) {
    Column {
        Text(label, color = Color(0xFF717789), fontSize = 14.sp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFFE1E6EE), RoundedCornerShape(18.dp))
                .omniClickable(shape = RoundedCornerShape(18.dp), onClick = onClick)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(value, color = Color(0xFF4B5363), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ExportTypeIconBox(format: ExportFormat, selected: ExportFormat, onSelected: (ExportFormat) -> Unit) {
    val active = selected == format
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0xFFE7F0FF) else Color.Transparent)
            .omniClickable(shape = RoundedCornerShape(8.dp)) { onSelected(format) }
            .padding(vertical = 8.dp)
    ) {
        Image(painterResource(exportIconRes(format)), contentDescription = null, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(4.dp))
        Text(format.name, color = if (active) BrandBlue else Color(0xFF3D3D3D), fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun exportIconRes(format: ExportFormat): Int =
    when (format) {
        ExportFormat.PDF -> R.drawable.doctor_export_pdf
        ExportFormat.CSV -> R.drawable.doctor_export_csv
        ExportFormat.MAT -> R.drawable.doctor_export_mat
        ExportFormat.EDF -> R.drawable.doctor_export_edf
        ExportFormat.EML -> R.drawable.doctor_export_eml
        ExportFormat.ZIP -> R.drawable.doctor_export_zip
    }

@Composable
private fun TabletExportPage(
    repository: MockRepository,
    compact: Boolean,
    gap: Dp,
    patient: DoctorPatientRecord?,
    showMessage: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<ExportFileType?>(null) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var refresh by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(ExportSettings()) }
    val files = remember(query, typeFilter, patient?.id, refresh) {
        repository.getExportFiles(query = query, type = typeFilter, patientId = patient?.id)
    }
    val selectedFile = files.firstOrNull { it.id in selectedIds }

    TabletPageTitle("文件导出")
    DoctorPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            FilterChipLike("日期范围", "最近三天", Modifier.weight(0.8f))
            Box(Modifier.weight(0.9f)) {
                Column {
                    Text("文件类型", color = Color(0xFF717789), fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { typeFilter = null }, shape = RoundedCornerShape(18.dp), modifier = Modifier.height(40.dp)) { Text("全部", fontSize = 12.sp) }
                        OutlinedButton(onClick = { typeFilter = nextExportType(typeFilter) }, shape = RoundedCornerShape(18.dp), modifier = Modifier.height(40.dp)) {
                            Text(typeFilter?.label() ?: "切换类型", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
            FilterChipLike("当前患者", patient?.let { "${it.name}（${it.number}）" } ?: "全部患者", Modifier.weight(1.2f))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("搜索") },
                modifier = Modifier.weight(1.2f).height(56.dp)
            )
            Button(onClick = { showMessage("筛选已应用") }, modifier = Modifier.height(40.dp), shape = RoundedCornerShape(20.dp)) { Text("搜索文件") }
        }
    }
    Spacer(Modifier.height(gap))
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        DoctorPanel(modifier = Modifier.weight(1.35f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(typeFilter?.label() ?: "全部类型", color = Color(0xFF3D3D3D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { refresh++; showMessage("文件列表已刷新") }, shape = RoundedCornerShape(8.dp)) { Text("刷新") }
            }
            Spacer(Modifier.height(14.dp))
            ExportFileHeader()
            files.forEach { file ->
                ExportFileRow(
                    file = file,
                    selected = file.id in selectedIds,
                    onToggle = {
                        selectedIds = if (file.id in selectedIds) selectedIds - file.id else selectedIds + file.id
                    }
                )
            }
            if (files.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("暂无匹配文件", color = Color(0xFFC3C7CF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("已选择${selectedIds.size}项", color = Color(0xFF5F687B), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    enabled = selectedIds.isNotEmpty(),
                    onClick = {
                        val exported = repository.createExport(selectedIds, selectedFormat, settings)
                        selectedIds = setOf(exported.id)
                        refresh++
                        showMessage("已生成 ${exported.fileName}")
                    }
                ) { Text("批量导出") }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    enabled = selectedIds.isNotEmpty(),
                    onClick = {
                        val deleted = repository.deleteExportFiles(selectedIds)
                        selectedIds = emptySet()
                        refresh++
                        showMessage("已删除${deleted}个导出记录")
                    }
                ) { Text("删除", color = SoftRed) }
            }
        }
        Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(gap)) {
            DoctorPanel {
                Text("导出类型", color = Color(0xFF3D3D3D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    ExportTypeBox(ExportFormat.PDF, selectedFormat, Color(0xFFE93E3E)) { selectedFormat = it }
                    ExportTypeBox(ExportFormat.CSV, selectedFormat, Color(0xFF67B347)) { selectedFormat = it }
                    ExportTypeBox(ExportFormat.MAT, selectedFormat, Color(0xFF4B89DC)) { selectedFormat = it }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    ExportTypeBox(ExportFormat.EDF, selectedFormat, Color(0xFFF5B941)) { selectedFormat = it }
                    ExportTypeBox(ExportFormat.ZIP, selectedFormat, Color(0xFF1D6ED8)) { selectedFormat = it }
                }
            }
            DoctorPanel {
                Text("导出设置", color = Color(0xFF3D3D3D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                SettingLine("包含患者身份信息", settings.includeIdentity) { settings = settings.copy(includeIdentity = !settings.includeIdentity) }
                SettingLine("包含患者主观报表", settings.includePatientFeedback) { settings = settings.copy(includePatientFeedback = !settings.includePatientFeedback) }
                SettingLine("包含脑电片段", settings.includeSignalClips) { settings = settings.copy(includeSignalClips = !settings.includeSignalClips) }
                SettingLine("包含刺激参数", settings.includeParameterTimeline) { settings = settings.copy(includeParameterTimeline = !settings.includeParameterTimeline) }
            }
            DoctorPanel(modifier = Modifier.height(180.dp)) {
                Text("导出预览", color = Color(0xFF3D3D3D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                if (selectedFile != null) {
                    Text(selectedFile.fileName, color = Color(0xFF3D3D3D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("${selectedFile.type.label()} / ${selectedFile.format.name} / ${selectedFile.size}", color = Color(0xFF717789), fontSize = 13.sp)
                    Text("生成时间：${selectedFile.generatedAt}", color = Color(0xFF717789), fontSize = 13.sp)
                    Text(if (selectedFile.exported) "状态：已导出" else "状态：待导出", color = if (selectedFile.exported) MedicalGreen else Color(0xFFFF9D28), fontSize = 13.sp)
                } else {
                    Spacer(Modifier.weight(1f))
                    Text("暂无选中文件", color = Color(0xFFC3C7CF), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FilterChipLike(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color(0xFF717789), fontSize = 14.sp)
        Box(Modifier.fillMaxWidth().height(40.dp).border(1.dp, Color(0xFFE1E6EE), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            Text(value, color = Color(0xFF4B5363), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ExportTypeBox(format: ExportFormat, selected: ExportFormat, color: Color, onSelected: (ExportFormat) -> Unit) {
    val active = selected == format
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0xFFE7F0FF) else Color.Transparent)
            .omniClickable(shape = RoundedCornerShape(8.dp)) { onSelected(format) }
            .padding(6.dp)
    ) {
        Box(Modifier.size(42.dp).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Download, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        }
        Text(format.name, color = if (active) BrandBlue else Color(0xFF3D3D3D), fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun SettingLine(text: String, on: Boolean, onClick: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .omniClickable(shape = RoundedCornerShape(10.dp), onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("i", color = Color(0xFF9ABEF2), fontSize = 12.sp, modifier = Modifier.width(18.dp))
        Text(text, color = Color(0xFF5F687B), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .width(30.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (on) Color(0xFF1069E3) else Color(0xFF9AA1AD)),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun ExportFileHeader() {
    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        WeightedDoctorCell("选择", 0.55f, true)
        WeightedDoctorCell("时间", 1.2f, true)
        WeightedDoctorCell("文件类型", 1.1f, true)
        WeightedDoctorCell("文件名", 2.0f, true)
        WeightedDoctorCell("大小", 0.7f, true)
    }
}

@Composable
private fun ExportFileRow(file: ExportFileRecord, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Color(0xFFE6F0FF) else Color.White)
            .omniClickable(shape = RoundedCornerShape(4.dp), onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeightedDoctorCell(if (selected) "☑" else "□", 0.55f, selected)
        WeightedDoctorCell(file.generatedAt, 1.2f, selected)
        WeightedDoctorCell(file.type.label(), 1.1f, selected)
        WeightedDoctorCell(file.fileName, 2.0f, selected)
        WeightedDoctorCell(file.size, 0.7f, selected)
    }
}

private fun nextExportType(current: ExportFileType?): ExportFileType? {
    val values = ExportFileType.values()
    if (current == null) return values.first()
    val nextIndex = values.indexOf(current) + 1
    return if (nextIndex >= values.size) null else values[nextIndex]
}

private fun ExportFileType.label(): String =
    when (this) {
        ExportFileType.PatientReport -> "数据报告"
        ExportFileType.BrainSignal -> "脑电数据"
        ExportFileType.ParameterRecord -> "参数记录"
        ExportFileType.TelehealthNote -> "诊疗记录"
    }

@Composable
private fun TabletSettingsPage(
    repository: MockRepository,
    compact: Boolean,
    gap: Dp,
    onLogout: () -> Unit,
    showMessage: (String) -> Unit
) {
    var settings by remember { mutableStateOf(repository.getDoctorSettings()) }
    var detail by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    fun save(next: DoctorSettings, message: String) {
        settings = repository.updateDoctorSettings(next)
        showMessage(message)
    }

    detail?.let { title ->
        PremiumAlertDialog(
            containerColor = PremiumSurfaceStrong,
            onDismissRequest = { detail = null },
            title = { Text(title) },
            text = {
                when (title) {
                    "个人信息管理" -> Text("演示版展示医生姓名、科室、工号和联系方式；真实医院账号接入后可在此修改。", color = Color(0xFF5F687B), lineHeight = 20.sp)
                    "账号与权限" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("当前账号拥有患者管理、参数审核、文件导出和远程诊疗演示权限。", color = Color(0xFF5F687B), lineHeight = 20.sp)
                        SettingLine("自动连接模拟设备", settings.autoConnectDevice) {
                            save(settings.copy(autoConnectDevice = !settings.autoConnectDevice), "自动连接设置已更新")
                        }
                        SettingLine("隐私脱敏模式", settings.privacyMode) {
                            save(settings.copy(privacyMode = !settings.privacyMode), "隐私模式已更新")
                        }
                    }
                    "通知设置" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("控制报警、待审核参数和患者消息提醒。", color = Color(0xFF5F687B))
                        SettingLine("启用医生端通知", settings.notificationsEnabled) {
                            save(settings.copy(notificationsEnabled = !settings.notificationsEnabled), "通知设置已更新")
                        }
                    }
                    "安全与密码修改" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("密码修改在真实账号服务接入后启用。当前可演示双重验证开关。", color = Color(0xFF5F687B), lineHeight = 20.sp)
                        SettingLine("双重验证", settings.twoFactorEnabled) {
                            save(settings.copy(twoFactorEnabled = !settings.twoFactorEnabled), "双重验证设置已更新")
                        }
                    }
                    "操作记录" -> Text("本次演示会记录患者选择、设备连接、远程诊疗、初始化保存和参数下发等关键操作。", color = Color(0xFF5F687B), lineHeight = 20.sp)
                    else -> Text("Ominidapt PD Demo 1.0.0\n当前已是首版比赛演示构建。", color = Color(0xFF5F687B), lineHeight = 20.sp)
                }
            },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("知道了") } }
        )
    }
    if (confirmLogout) {
        PremiumAlertDialog(
            containerColor = PremiumSurfaceStrong,
            onDismissRequest = { confirmLogout = false },
            title = { Text("退出登录") },
            text = { Text("确认返回登录页？当前演示数据会保留在内存中，重启应用后恢复默认数据。") },
            confirmButton = { TextButton(onClick = onLogout) { Text("退出", color = SoftRed) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } }
        )
    }

    TabletPageTitle("个人设置")
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.mg_doctor_profile), contentDescription = null, modifier = Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(22.dp))
            Column {
                Text("张度星", color = Color.Black, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("神经内科主任医师", color = Color(0xFF717789), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("运动障碍中心  |  参数审核医生", color = Color(0xFF717789), fontSize = 17.sp)
                Text("工号：D-1024", color = Color(0xFF717789), fontSize = 17.sp)
            }
        }
    }
    Spacer(Modifier.height(gap))
    DoctorInteractiveSettingRow("个人信息管理", "查看并修改姓名、科室、联系方式", R.drawable.doctor_setting_profile) {
        detail = "个人信息管理"
    }
    DoctorInteractiveSettingRow("账号与权限", "查看当前角色与权限范围", R.drawable.doctor_setting_account) {
        detail = "账号与权限"
    }
    DoctorInteractiveSettingRow("通知设置", if (settings.notificationsEnabled) "报警、待审核参数、患者消息提醒已开启" else "通知提醒已关闭", R.drawable.doctor_setting_notification) {
        detail = "通知设置"
    }
    DoctorInteractiveSettingRow("安全与密码修改", if (settings.twoFactorEnabled) "双重验证已开启" else "修改登录密码与双重验证", R.drawable.doctor_setting_security) {
        detail = "安全与密码修改"
    }
    DoctorInteractiveSettingRow("操作记录", "查看登录与关键操作审计", R.drawable.doctor_setting_log) {
        detail = "操作记录"
    }
    DoctorInteractiveSettingRow("版本与更新", "Ominidapt PD Demo 1.0.0", R.drawable.doctor_setting_version) {
        detail = "版本与更新"
    }
    Spacer(Modifier.height(36.dp))
    DoctorPanel {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .omniClickable(shape = RoundedCornerShape(12.dp)) { confirmLogout = true },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).background(Color(0xFF1069E3)), contentAlignment = Alignment.Center) { Text("→", color = Color.White) }
                Spacer(Modifier.width(12.dp))
                Text("退出登录", color = Color(0xFF3D3D3D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DoctorInteractiveSettingRow(title: String, subtitle: String, iconRes: Int, onClick: () -> Unit) {
    DoctorPanel(modifier = Modifier.padding(bottom = 2.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .omniClickable(shape = RoundedCornerShape(12.dp), onClick = onClick)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color(0xFF3D3D3D), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFF717789), fontSize = 13.sp)
            }
            Text("›", color = Color(0xFF717789), fontSize = 34.sp)
        }
    }
}

@Composable
private fun TabletParameterPageV3(
    repository: MockRepository,
    patient: Patient,
    report: PatientReport,
    compact: Boolean,
    gap: Dp,
    onParametersChanged: () -> Unit,
    showMessage: (String) -> Unit,
    initialization: InitializationController? = null,
    bleClient: BleCentralClient? = null,
    edgeInference: EdgeInferenceController? = null,
    serverPatientId: String? = null,
    realRepository: RealRepository? = null,
    modifier: Modifier = Modifier
) {
    val realScope = rememberCoroutineScope()
    val realInitializationState = initialization?.state?.collectAsState()?.value
    val liveBleSnapshot = bleClient?.snapshot?.collectAsState()?.value
    val liveBleSamples = bleClient?.recentSamples?.collectAsState()?.value ?: ShortArray(0)
    var workflow by remember(patient.id) { mutableStateOf(repository.getInitializationWorkflow(patient.id)) }
    var selection by remember(patient.id) { mutableStateOf(workflow.electrodeSelection) }
    var stimulationParameters by remember(patient.id) { mutableStateOf(workflow.stimulationParameters) }
    var bands by remember(patient.id) { mutableStateOf(workflow.frequencyBands) }
    var initializationMode by remember(patient.id) { mutableStateOf("demo") }
    var measuredImpedance by remember(patient.id) {
        mutableStateOf<com.omnidapt.protocol.ImpedanceSnapshot?>(null)
    }
    var impedanceLoading by remember { mutableStateOf(false) }
    var impedanceError by remember { mutableStateOf<String?>(null) }
    var impedanceRefreshKey by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }
    val workflowScrollState = remember(workflow.step) { ScrollState(0) }

    LaunchedEffect(realInitializationState?.result?.id, realInitializationState?.result?.status) {
        realInitializationState?.result?.toFrequencyBands()?.let { computed ->
            bands = computed
            repository.saveInitializationFrequencyBands(patient.id, computed)
        }
    }

    LaunchedEffect(
        selection.leftPositive,
        selection.leftNegative,
        selection.rightPositive,
        selection.rightNegative,
        liveBleSnapshot?.verifiedSimulator,
        impedanceRefreshKey,
    ) {
        measuredImpedance = null
        impedanceError = null
        if (bleClient == null || liveBleSnapshot?.verifiedSimulator != true) return@LaunchedEffect
        delay(300)
        impedanceLoading = true
        runCatching {
            val pairs = listOf(
                selection.leftPositive to selection.leftNegative,
                selection.rightPositive to selection.rightNegative,
            )
            val sequence = bleClient.measureImpedance(pairs)
                ?: error("无法发送阻抗测量命令")
            val ack = withTimeout(8_000) {
                bleClient.acknowledgements.first { it.acknowledgedSequence == sequence }
            }
            check(ack.success) { "模拟器拒绝阻抗测量，状态码 ${ack.statusCode}" }
            withTimeout(8_000) {
                bleClient.impedanceMeasurements.first { it.measurementSequence == sequence }
            }
        }.onSuccess {
            measuredImpedance = it
        }.onFailure {
            impedanceError = it.message ?: "阻抗测量失败"
        }
        impedanceLoading = false
    }

    if (showResetDialog) {
        PremiumAlertDialog(
            containerColor = PremiumSurfaceStrong,
            onDismissRequest = { showResetDialog = false },
            title = { Text("重新进行初始化") },
            text = { Text("将清空当前患者的电极配置、采样进度和频段结果，并回到第一步。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.resetInitialization(patient.id)
                        workflow = repository.getInitializationWorkflow(patient.id)
                        selection = workflow.electrodeSelection
                        stimulationParameters = workflow.stimulationParameters
                        bands = workflow.frequencyBands
                        showResetDialog = false
                        showMessage("初始化流程已重置")
                    }
                ) { Text("重新开始", color = BrandBlue) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("取消", color = Color(0xFF717789)) } }
        )
    }

    LaunchedEffect(patient.id, workflow.baseline.sampling) {
        while (repository.getInitializationWorkflow(patient.id).baseline.sampling) {
            delay(1000)
            val current = repository.getInitializationWorkflow(patient.id)
            workflow = repository.saveBaselineSamplingState(
                patient.id,
                current.baseline.copy(elapsedSeconds = current.baseline.elapsedSeconds + 1)
            )
        }
    }

    val workflowOffset = with(LocalDensity.current) { 12.dp.roundToPx() }
    Column(modifier) {
        TabletPageTitle("初始化与参数调整")
        DoctorWorkflowHeader(
            step = workflow.step,
            initializationMode = initializationMode,
            realStatus = realInitializationState?.phase,
            onModeChange = { initializationMode = it },
            onPrevious = {
                repository.previousInitialization(patient.id)
                workflow = repository.getInitializationWorkflow(patient.id)
                selection = workflow.electrodeSelection
                stimulationParameters = workflow.stimulationParameters
                bands = workflow.frequencyBands
                showMessage("已返回上一步，已保存内容保持不变")
            },
            onReset = { showResetDialog = true }
        )
        Spacer(Modifier.height(gap))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(workflowScrollState)
                    .padding(bottom = 8.dp)
            ) {
                AnimatedContent(
                    targetState = workflow.step,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(OmniMotion.StateMillis, easing = FastOutSlowInEasing)
                        ) { workflowOffset } + fadeIn(tween(OmniMotion.StateMillis)))
                            .togetherWith(
                                slideOutVertically(tween(OmniMotion.StateMillis)) { -workflowOffset } +
                                    fadeOut(tween(OmniMotion.StateMillis))
                            )
                            .using(SizeTransform(clip = false))
                    },
                    label = "initializationStepTransition"
                ) { activeStep ->
                    Column {
                        when (activeStep) {
                            InitializationStep.ElectrodeConfig -> ElectrodeConfigurationContentV3(
                                repository = repository,
                                patientId = patient.id,
                                compact = compact,
                                selection = selection,
                                onSelectionChange = { selection = it },
                                parameters = stimulationParameters,
                                onParametersChange = { stimulationParameters = it },
                                liveImpedance = measuredImpedance?.readings?.map {
                                    StoredImpedancePoint(
                                        contact = "C${it.leftContact}-C${it.rightContact}",
                                        valueKOhm = it.kiloOhms,
                                    )
                                }.orEmpty(),
                                liveImpedanceQuality = measuredImpedance?.readings
                                    ?.associate {
                                        "C${it.leftContact}-C${it.rightContact}" to it.qualityCode
                                    }.orEmpty(),
                                impedanceLoading = impedanceLoading,
                                impedanceError = impedanceError,
                                onRetestImpedance = { impedanceRefreshKey++ },
                                simulatorOnline = liveBleSnapshot?.verifiedSimulator == true,
                            )
                            InitializationStep.BaselineDetection -> BaselineDetectionContentV3(
                                state = workflow.baseline,
                                signalValues = if (liveBleSamples.size >= 4) {
                                    liveBleSamples
                                        .asSequence()
                                        .filterIndexed { index, _ -> index % 2 == 0 }
                                        .map { it.toFloat() }
                                        .toList()
                                } else {
                                    repository.observeRealtimeSignals(
                                        patient.id,
                                        workflow.baseline.elapsedSeconds,
                                    ).map { it.microVolt }
                                },
                                realState = realInitializationState,
                                compact = compact
                            )
                            InitializationStep.FrequencyExtraction -> FrequencyExtractionContentV3(
                                bands = bands,
                                onBandsChange = { bands = it },
                                realResult = realInitializationState?.result,
                                compact = compact
                            )
                            InitializationStep.Completed -> FeedbackOptimizationContentV3(
                                repository = repository,
                                patient = patient,
                                report = report,
                                gap = gap,
                                onParametersChanged = onParametersChanged,
                                showMessage = showMessage,
                                realRepository = realRepository,
                                serverPatientId = serverPatientId,
                                currentDeviceParameters = liveBleSnapshot?.parameters,
                            )
                        }
                    }
                }
            }
        }

        if (workflow.step != InitializationStep.Completed) {
            Spacer(Modifier.height(8.dp))
            WorkflowBottomActionBar(
                workflow = workflow,
                selectionValid = selection.isValid() &&
                    stimulationParameters.size == 4 &&
                    stimulationParameters.all { it.isSafe() } &&
                    (
                        bleClient == null ||
                            measuredImpedance?.readings?.size == 2 &&
                            measuredImpedance!!.readings.all {
                                it.qualityCode == 0 && it.kiloOhms in 0.2f..5.0f
                            }
                    ),
                frequencyValid = bands.hasValidRanges(),
                realState = realInitializationState,
                onStartSampling = {
                    if (initialization != null && serverPatientId != null) {
                        val electrodeConfig = mapOf(
                            "leftPositive" to selection.leftPositive,
                            "leftNegative" to selection.leftNegative,
                            "rightPositive" to selection.rightPositive,
                            "rightNegative" to selection.rightNegative,
                            "stimulationParameters" to stimulationParameters.map {
                                mapOf(
                                    "condition" to it.condition,
                                    "frequencyHz" to it.frequencyHz,
                                    "amplitudeMv" to it.amplitudeMv,
                                    "pulseWidthUs" to it.pulseWidthUs,
                                    "dutyCycle" to it.dutyCycle,
                                )
                            },
                        )
                        realScope.launch {
                            runCatching {
                                initialization.run(
                                    serverPatientId,
                                    initializationMode,
                                    electrodeConfig,
                                )
                            }.onFailure {
                                showMessage(it.message ?: "真实初始化启动失败")
                            }
                        }
                    } else {
                        workflow = repository.saveBaselineSamplingState(
                            patient.id,
                            workflow.baseline.copy(sampling = true, sampleEnded = false)
                        )
                    }
                },
                onAnalyze = {
                    if (initialization != null) {
                        realScope.launch {
                            runCatching { initialization.analyze() }
                                .onFailure { showMessage(it.message ?: "模型分析失败") }
                        }
                    }
                },
                onEndSampling = {
                    workflow = repository.saveBaselineSamplingState(
                        patient.id,
                        workflow.baseline.copy(sampling = false, sampleEnded = true)
                    )
                },
                onPauseSampling = {
                    workflow = repository.saveBaselineSamplingState(
                        patient.id,
                        workflow.baseline.copy(sampling = false, sampleEnded = false)
                    )
                },
                onUseRecommendedBands = {
                    bands = realInitializationState?.result?.toFrequencyBands() ?: FrequencyBands()
                    showMessage("已采用模型计算频段")
                },
                onConfirm = {
                    when (workflow.step) {
                        InitializationStep.ElectrodeConfig -> {
                            repository.saveElectrodeConfiguration(patient.id, selection, stimulationParameters)
                            repository.advanceInitialization(patient.id)
                            workflow = repository.getInitializationWorkflow(patient.id)
                            showMessage("电极与刺激参数已保存，进入基线状态检测")
                        }
                        InitializationStep.BaselineDetection -> {
                            val realFinished = realInitializationState
                                ?.result
                                ?.status in setOf("review", "approved")
                            val baseline = workflow.baseline
                            val completed = if (realFinished) {
                                setOf(0, 1, 2, 3)
                            } else {
                                baseline.completedTasks + baseline.activeTask
                            }
                            if (realFinished || baseline.activeTask == 3) {
                                repository.saveBaselineSamplingState(
                                    patient.id,
                                    baseline.copy(
                                        completedTasks = completed,
                                        sampling = false,
                                        sampleEnded = false
                                    )
                                )
                                repository.advanceInitialization(patient.id)
                                workflow = repository.getInitializationWorkflow(patient.id)
                                showMessage("四项基线采样已完成，进入个性化频段提取")
                            } else {
                                workflow = repository.saveBaselineSamplingState(
                                    patient.id,
                                    baseline.copy(
                                        activeTask = baseline.activeTask + 1,
                                        completedTasks = completed,
                                        sampling = false,
                                        sampleEnded = false,
                                        elapsedSeconds = 0
                                    )
                                )
                                showMessage("本步采样已确认")
                            }
                        }
                        InitializationStep.FrequencyExtraction -> {
                            val resultStatus = realInitializationState?.result?.status
                            if (initialization != null && resultStatus == "review") {
                                realScope.launch {
                                    runCatching { initialization.approve() }.fold(
                                        onSuccess = {
                                            repository.saveInitializationFrequencyBands(patient.id, bands)
                                            repository.advanceInitialization(patient.id)
                                            workflow = repository.getInitializationWorkflow(patient.id)
                                            serverPatientId?.let { edgeInference?.start(it) }
                                            showMessage("模型已审核启用，频段结果已保存")
                                        },
                                        onFailure = {
                                            showMessage(it.message ?: "模型审核失败")
                                        },
                                    )
                                }
                            } else {
                                repository.saveInitializationFrequencyBands(patient.id, bands)
                                repository.advanceInitialization(patient.id)
                                workflow = repository.getInitializationWorkflow(patient.id)
                                showMessage("频段结果已保存，进入反馈优化")
                            }
                        }
                        InitializationStep.Completed -> Unit
                    }
                }
            )
        }
    }
}

@Composable
private fun DoctorWorkflowHeader(
    step: InitializationStep,
    initializationMode: String,
    realStatus: String?,
    onModeChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onReset: () -> Unit
) {
    DoctorPanel {
        BoxWithConstraints {
            val narrow = maxWidth < 720.dp
            if (narrow) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WorkflowStepPill("电极信息配置", step, InitializationStep.ElectrodeConfig)
                        WorkflowConnector(step.ordinal >= 1, Modifier.weight(1f))
                        WorkflowStepPill("基线状态检测", step, InitializationStep.BaselineDetection)
                        WorkflowConnector(step.ordinal >= 2, Modifier.weight(1f))
                        WorkflowStepPill("个性化频段提取", step, InitializationStep.FrequencyExtraction)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                        WorkflowHeaderButtons(step, onPrevious, onReset)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WorkflowStepPill("电极信息配置", step, InitializationStep.ElectrodeConfig)
                    WorkflowConnector(step.ordinal >= 1, Modifier.weight(1f))
                    WorkflowStepPill("基线状态检测", step, InitializationStep.BaselineDetection)
                    WorkflowConnector(step.ordinal >= 2, Modifier.weight(1f))
                    WorkflowStepPill("个性化频段提取", step, InitializationStep.FrequencyExtraction)
                    Spacer(Modifier.width(14.dp))
                    WorkflowHeaderButtons(step, onPrevious, onReset)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFE8ECF2))
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "采集模式",
                color = Color(0xFF4B5363),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            InitializationModeOption(
                label = "演示  30秒/状态",
                selected = initializationMode == "demo",
                enabled = step == InitializationStep.ElectrodeConfig,
                onClick = { onModeChange("demo") },
            )
            InitializationModeOption(
                label = "科研  3分钟/状态",
                selected = initializationMode == "research",
                enabled = step == InitializationStep.ElectrodeConfig,
                onClick = { onModeChange("research") },
            )
            Spacer(Modifier.weight(1f))
            Text(
                realStatus ?: if (step == InitializationStep.ElectrodeConfig) {
                    "配置完成后进入四状态真实采集"
                } else {
                    step.displayLabel()
                },
                color = if (realStatus?.contains("中止") == true) SoftRed else Color(0xFF717789),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InitializationModeOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFE7F0FF) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) BrandBlue else Color(0xFFDCE5F1),
        ),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = if (selected) BrandBlue else Color(0xFF717789),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun WorkflowHeaderButtons(
    step: InitializationStep,
    onPrevious: () -> Unit,
    onReset: () -> Unit
) {
    OutlinedButton(
        onClick = onPrevious,
        enabled = step != InitializationStep.ElectrodeConfig,
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 0.dp)
    ) { Text("←  上一步", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    OutlinedButton(
        onClick = onReset,
        enabled = step == InitializationStep.Completed,
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 0.dp)
    ) { Text("↻  重新初始化", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ElectrodeConfigurationContentV3(
    repository: MockRepository,
    patientId: String,
    compact: Boolean,
    selection: ElectrodeSelection,
    onSelectionChange: (ElectrodeSelection) -> Unit,
    parameters: List<StoredStimulationParameter>,
    onParametersChange: (List<StoredStimulationParameter>) -> Unit,
    liveImpedance: List<StoredImpedancePoint> = emptyList(),
    liveImpedanceQuality: Map<String, Int> = emptyMap(),
    impedanceLoading: Boolean = false,
    impedanceError: String? = null,
    onRetestImpedance: () -> Unit = {},
    simulatorOnline: Boolean = false,
) {
    BoxWithConstraints {
        val stacked = maxWidth < 760.dp
        val electrode: @Composable () -> Unit = {
            DoctorPanel(modifier = Modifier.height(if (stacked) 410.dp else 405.dp)) {
                ResourceSectionTitle("电极配置", R.drawable.doctor_section_electrode)
                Text("左右脑各选择一对正负触点。点击数字、柱段或右侧极性区均可完成选择。", color = Color(0xFF717789), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ElectrodeColumnV3(
                        title = "左脑",
                        contacts = listOf(8, 7, 6, 5),
                        positive = selection.leftPositive,
                        negative = selection.leftNegative,
                        onPositive = { if (it != selection.leftNegative) onSelectionChange(selection.copy(leftPositive = it)) },
                        onNegative = { if (it != selection.leftPositive) onSelectionChange(selection.copy(leftNegative = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    ElectrodeColumnV3(
                        title = "右脑",
                        contacts = listOf(4, 3, 2, 1),
                        positive = selection.rightPositive,
                        negative = selection.rightNegative,
                        onPositive = { if (it != selection.rightNegative) onSelectionChange(selection.copy(rightPositive = it)) },
                        onNegative = { if (it != selection.rightPositive) onSelectionChange(selection.copy(rightNegative = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "推荐组合：左脑 ${selection.leftPositive}+ / ${selection.leftNegative}-   右脑 ${selection.rightPositive}+ / ${selection.rightNegative}-",
                    color = BrandBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        val impedance: @Composable () -> Unit = {
            DoctorPanel(modifier = Modifier.height(if (stacked) 470.dp else 405.dp)) {
                ResourceSectionTitle("阻抗测试", R.drawable.doctor_section_impedance)
                Text(
                    if (impedanceLoading) {
                        "正在向 SIM-PC-P001 注入测试电流并等待测量结果…"
                    } else if (simulatorOnline && liveImpedance.isNotEmpty()) {
                        "当前所选左右电极对的实测模拟阻抗，单位 kΩ。"
                    } else {
                        "等待连接科研模拟设备并读取阻抗。"
                    },
                    color = Color(0xFF717789),
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(6.dp))
                if (impedanceLoading) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BrandBlue, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("阻抗测量中", color = BrandBlue, fontSize = 13.sp)
                        }
                    }
                } else if (liveImpedance.isNotEmpty()) {
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        liveImpedance.take(2).forEachIndexed { index, reading ->
                            ImpedanceMiniChart(
                                if (index == 0) "左侧 · ${reading.contact}" else "右侧 · ${reading.contact}",
                                listOf(reading),
                                Modifier.weight(1f),
                            )
                        }
                    }
                    liveImpedance.forEach { reading ->
                        val quality = liveImpedanceQuality[reading.contact] ?: 4
                        Text(
                            "${reading.contact}  ${"%.2f".format(reading.valueKOhm)} kΩ · ${
                                when (quality) {
                                    0 -> "良好"
                                    1 -> "接触不良"
                                    2 -> "开路"
                                    3 -> "短路"
                                    else -> "未知"
                                }
                            }",
                            color = if (quality == 0 && reading.valueKOhm in 0.2f..5f) MedicalGreen else SoftRed,
                            fontSize = 11.sp,
                        )
                    }
                } else {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("未收到阻抗数据", color = Color(0xFF9AA1AD), fontSize = 13.sp)
                    }
                }
                impedanceError?.let { Text(it, color = SoftRed, fontSize = 11.sp) }
                OutlinedButton(
                    onClick = onRetestImpedance,
                    enabled = simulatorOnline && !impedanceLoading,
                    modifier = Modifier.align(Alignment.End).height(36.dp),
                ) { Text("重新测试当前电极对", fontSize = 11.sp) }
            }
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                electrode()
                impedance()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(0.92f)) { electrode() }
                Box(Modifier.weight(1.28f)) { impedance() }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    DoctorPanel {
        ResourceSectionTitle("当前刺激参数", R.drawable.doctor_section_stimulation)
        StimulationParameterTableV3(parameters, onParametersChange, compact)
    }
}

@Composable
private fun ResourceSectionTitle(text: String, iconRes: Int) {
    ResourceTitleInline(text, iconRes)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ResourceTitleInline(text: String, iconRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ElectrodeColumnV3(
    title: String,
    contacts: List<Int>,
    positive: Int,
    negative: Int,
    onPositive: (Int) -> Unit,
    onNegative: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF7FBFF), Color(0xFFF2F6FC), Color(0xFFF8F7FF))
                )
            )
            .border(1.dp, Color(0xFFD9E5F3), RoundedCornerShape(16.dp))
            .padding(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(27.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0E70E8), Color(0xFF43BCE8))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (title == "左脑") "L" else "R",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(7.dp))
            Text(title, color = Color(0xFF26344D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        contacts.forEach { contact ->
            val role = when (contact) {
                positive -> "+"
                negative -> "-"
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (role) {
                            "+" -> Color(0xFFE7F3FF)
                            "-" -> Color(0xFFF0EDFF)
                            else -> Color(0xF8FFFFFF)
                        }
                    )
                    .border(
                        1.dp,
                        when (role) {
                            "+" -> Color(0xFFB9D9FF)
                            "-" -> Color(0xFFD6CDFB)
                            else -> Color(0xFFE2E8F1)
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .omniClickable(shape = RoundedCornerShape(12.dp)) {
                        when (role) {
                            "+" -> onNegative(contact)
                            "-" -> onPositive(contact)
                            else -> onPositive(contact)
                        }
                    }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(34.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            when (role) {
                                "+" -> BrandBlue
                                "-" -> Color(0xFF6F5BDC)
                                else -> Color(0xFFEDF1F6)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "C$contact",
                        color = if (role.isNotEmpty()) Color.White else Color(0xFF5E687B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                PolaritySelectorV3("+", role == "+") { onPositive(contact) }
                Spacer(Modifier.width(4.dp))
                PolaritySelectorV3("-", role == "-") { onNegative(contact) }
            }
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun PolaritySelectorV3(label: String, selected: Boolean, onClick: () -> Unit) {
    val selectedColor = if (label == "+") BrandBlue else Color(0xFF6F5BDC)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    Brush.linearGradient(listOf(selectedColor, selectedColor.copy(alpha = 0.82f)))
                } else {
                    Brush.linearGradient(listOf(Color.White, Color(0xFFF6F9FD)))
                }
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) selectedColor.copy(alpha = 0.45f) else Color(0xFFD4DDE9),
                CircleShape
            )
            .omniClickable(shape = CircleShape, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else Color(0xFF5E6A7F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ImpedanceMiniChart(
    title: String,
    points: List<StoredImpedancePoint>,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(title, color = Color(0xFF3D3D3D), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Row(Modifier.weight(1f)) {
            Column(
                Modifier.width(24.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("4.0", color = Color(0xFF8A91A0), fontSize = 7.sp)
                Text("2.0", color = Color(0xFF8A91A0), fontSize = 7.sp)
                Text("0", color = Color(0xFF8A91A0), fontSize = 7.sp)
            }
            Canvas(Modifier.weight(1f).fillMaxHeight().padding(start = 4.dp, top = 3.dp)) {
                repeat(3) { index ->
                    val y = size.height * index / 2f
                    drawLine(Color(0xFFE6EAF0), Offset(0f, y), Offset(size.width, y), 1f)
                }
                drawLine(Color(0xFF9AA1AD), Offset(0f, 0f), Offset(0f, size.height), 1.4f)
                drawLine(Color(0xFF9AA1AD), Offset(0f, size.height), Offset(size.width, size.height), 1.4f)
                val slot = size.width / points.size
                points.forEachIndexed { index, point ->
                    val height = (point.valueKOhm / 4f).coerceIn(0f, 1f) * size.height
                    drawRect(
                        color = BrandBlue,
                        topLeft = Offset(slot * index + slot * 0.25f, size.height - height),
                        size = Size(slot * 0.5f, height)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 28.dp), horizontalArrangement = Arrangement.SpaceAround) {
            points.forEach { Text(it.contact, color = Color(0xFF7D8493), fontSize = 7.sp) }
        }
    }
}

@Composable
private fun StimulationParameterTableV3(
    parameters: List<StoredStimulationParameter>,
    onParametersChange: (List<StoredStimulationParameter>) -> Unit,
    compact: Boolean
) {
    val scroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val minimumTableWidth = if (compact) 720.dp else 760.dp
        val tableWidth = if (maxWidth > minimumTableWidth) maxWidth else minimumTableWidth
        Column(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
        ) {
            Column(Modifier.width(tableWidth)) {
        Row(Modifier.fillMaxWidth().height(34.dp).background(Color(0xFFF5F7FB)), verticalAlignment = Alignment.CenterVertically) {
            StimHeaderV3("基线状态", 1.45f)
            StimHeaderV3("频率 (Hz)", 1f)
            StimHeaderV3("幅值 (mV)", 1f)
            StimHeaderV3("脉宽 (μs)", 1f)
            StimHeaderV3("占空比 (%)", 1f)
            StimHeaderV3("安全性", 0.8f)
        }
        parameters.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.condition, color = Color(0xFF3D3D3D), fontSize = 12.sp, modifier = Modifier.weight(1.45f).padding(horizontal = 8.dp))
                StimValueFieldV3(item.frequencyHz.toString(), Modifier.weight(1f)) {
                    it.toIntOrNull()?.let { value -> onParametersChange(parameters.updateStored(index, item.copy(frequencyHz = value))) }
                }
                StimValueFieldV3(item.amplitudeMv.toString(), Modifier.weight(1f)) {
                    it.toFloatOrNull()?.let { value -> onParametersChange(parameters.updateStored(index, item.copy(amplitudeMv = value))) }
                }
                StimValueFieldV3(item.pulseWidthUs.toString(), Modifier.weight(1f)) {
                    it.toIntOrNull()?.let { value -> onParametersChange(parameters.updateStored(index, item.copy(pulseWidthUs = value))) }
                }
                StimValueFieldV3(item.dutyCycle.toString(), Modifier.weight(1f)) {
                    it.toIntOrNull()?.let { value -> onParametersChange(parameters.updateStored(index, item.copy(dutyCycle = value))) }
                }
                Box(Modifier.weight(0.8f), contentAlignment = Alignment.Center) {
                    Text(
                        if (item.isSafe()) "安全" else "需复核",
                        color = if (item.isSafe()) MedicalGreen else SoftRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (item.isSafe()) Color(0xFFEAF8F1) else Color(0xFFFFEEEE))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
            if (index != parameters.lastIndex) HorizontalDivider(color = Color(0xFFE8ECF2))
        }
            }
        }
    }
}

private fun List<StoredStimulationParameter>.updateStored(
    index: Int,
    value: StoredStimulationParameter
): List<StoredStimulationParameter> = toMutableList().also { it[index] = value }

@Composable
private fun RowScope.StimHeaderV3(text: String, weight: Float) {
    Text(text, color = Color(0xFF4B5363), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(weight))
}

@Composable
private fun StimValueFieldV3(value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    var draft by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = draft,
        onValueChange = {
            draft = it
            onValueChange(it)
        },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
        modifier = modifier.padding(horizontal = 4.dp).height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = Color(0xFFDCE5F1),
            focusedContainerColor = Color(0xF7FFFFFF),
            unfocusedContainerColor = Color(0xF2FFFFFF),
            cursorColor = BrandBlue
        )
    )
}

@Composable
private fun BaselineDetectionContentV3(
    state: BaselineSamplingState,
    signalValues: List<Float>,
    realState: InitializationUiState?,
    compact: Boolean
) {
    val tasks = listOf("药物失效-静息状态", "药物失效-运动状态", "药物生效-静息状态", "药物生效-运动状态")
    val stateLabels = listOf("OFF-Rest", "OFF-Move", "ON-Rest", "ON-Move")
    val realTask = realState?.stateLabel?.let(stateLabels::indexOf)?.takeIf { it >= 0 }
    val activeTask = realTask ?: state.activeTask
    val realCompleted = realState?.result?.segments
        ?.filter { it.accepted }
        ?.mapNotNull { segment -> stateLabels.indexOf(segment.state_label).takeIf { it >= 0 } }
        ?.toSet()
        .orEmpty()
    val completedTasks = if (realState != null) realCompleted else state.completedTasks
    val realSampling = realState?.running == true && realState.phase.contains("采集")
    val sampling = if (realState != null) realSampling else state.sampling
    BoxWithConstraints {
        val stacked = maxWidth < 720.dp
        val taskPanel: @Composable () -> Unit = {
            DoctorPanel(modifier = Modifier.height(340.dp)) {
                ResourceSectionTitle("左旋多巴冲击测试", R.drawable.doctor_section_baseline_test)
                Text("按照 OFF/ON 与静息/运动顺序逐项采集，当前步骤完成后才能继续。", color = Color(0xFF717789), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                tasks.forEachIndexed { index, label ->
                    BaselineTaskRow(
                        number = index + 1,
                        label = if (index == activeTask) "$label（当前步骤）" else label,
                        done = index in completedTasks,
                        current = index == activeTask,
                        onClick = {}
                    )
                }
            }
        }
        val guidePanel: @Composable () -> Unit = {
            DoctorPanel(modifier = Modifier.height(340.dp)) {
                SectionTitle("测试内容", Icons.Filled.Info)
                Text(baselineInstruction(activeTask), color = Color(0xFF3D3D3D), fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(if (activeTask % 2 == 1) "示例动作：" else "静息采集要求：", color = Color(0xFF3D3D3D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (activeTask % 2 == 1) {
                    Image(
                        painterResource(R.drawable.doctor_baseline_motion),
                        contentDescription = "坐站运动示例",
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    RestStateGuideCard(Modifier.fillMaxWidth().height(130.dp))
                }
                Text(
                    if (activeTask < 2) "模拟器处于服药前 OFF 场景。" else "模拟器处于服药后 ON 场景。",
                    color = Color(0xFF5F687B),
                    fontSize = 12.sp
                )
            }
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                taskPanel()
                guidePanel()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(0.78f)) { taskPanel() }
                Box(Modifier.weight(1.35f)) { guidePanel() }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("数据观察", Icons.AutoMirrored.Filled.ShowChart)
            Spacer(Modifier.weight(1f))
            Text(
                "SIM-PC-P001  ${
                    when {
                        realState?.running == true -> "${realState.phase} · ${realState.stateLabel.orEmpty()}"
                        realState?.result?.status == "review" -> "四状态采集完成，等待频段审核"
                        realState?.result?.status == "approved" -> "模型已审核启用"
                        sampling -> "正在采样"
                        state.sampleEnded -> "等待确认"
                        else -> "等待开始"
                    }
                }",
                color = Color(0xFF686F82),
                fontSize = 12.sp
            )
        }
        if (realState?.targetSamples ?: 0 > 0) {
            val progress = realState!!.collectedSamples.toFloat() / realState.targetSamples
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = BrandBlue,
            )
            Text(
                "${realState.collectedSamples}/${realState.targetSamples} 样本/通道 · 剩余 ${realState.remainingSeconds} 秒",
                color = Color(0xFF717789),
                fontSize = 11.sp,
            )
        } else if (realState?.running == true && realState.result?.status == "analyzing") {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (realState.result.progress_percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = BrandBlue,
            )
            Text(
                "${realState.phase} · ${realState.result.progress_percent}%",
                color = BrandBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        AxisLineChart(
            values = signalValues,
            color = Color(0xFFFFA31A),
            min = -50f,
            max = 50f,
            unit = "μV",
            modifier = Modifier.fillMaxWidth().height(if (compact) 180.dp else 210.dp)
        )
    }
}

@Composable
private fun FrequencyExtractionContentV3(
    bands: FrequencyBands,
    onBandsChange: (FrequencyBands) -> Unit,
    realResult: ApiInitialization?,
    compact: Boolean
) {
    val medicationFisher = realResult?.frequency_results
        ?.floatList("fisher_medication_beta")
        .orEmpty()
    val movementFisher = realResult?.frequency_results
        ?.floatList("fisher_movement_beta")
        .orEmpty()
    val gammaFisher = realResult?.frequency_results
        ?.floatList("fisher_movement_gamma")
        .orEmpty()
    BoxWithConstraints {
        val stacked = maxWidth < 720.dp
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FrequencyChartPanel("β频段提取可视化", "真实 Fisher 曲线：橙色药物效应，蓝色运动效应。", Modifier.height(280.dp)) {
                    BetaBandChart(medicationFisher, movementFisher, bands.staticBeta, bands.motionBeta, Modifier.fillMaxSize())
                }
                FrequencyChartPanel("样本分布可视化", "四种状态的实际有效采样量。", Modifier.height(280.dp)) {
                    SampleDistributionChart(realResult, Modifier.fillMaxSize())
                }
                FrequencyChartPanel("γ频段提取可视化", "运动 Fisher 峰半高连续区间。", Modifier.height(260.dp)) {
                    GammaBandChart(gammaFisher, bands.gamma, Modifier.fillMaxSize())
                }
                FrequencyResultPanelV3(bands, onBandsChange, realResult, Modifier.height(280.dp))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FrequencyChartPanel("β频段提取可视化", "真实 Fisher 曲线：橙色药物效应，蓝色运动效应。", Modifier.weight(1.25f).height(255.dp)) {
                        BetaBandChart(medicationFisher, movementFisher, bands.staticBeta, bands.motionBeta, Modifier.fillMaxSize())
                    }
                    FrequencyChartPanel("样本分布可视化", "四种状态的实际有效采样量。", Modifier.weight(1f).height(255.dp)) {
                        SampleDistributionChart(realResult, Modifier.fillMaxSize())
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FrequencyChartPanel("γ频段提取可视化", "运动 Fisher 峰半高连续区间。", Modifier.weight(1.25f).height(225.dp)) {
                        GammaBandChart(gammaFisher, bands.gamma, Modifier.fillMaxSize())
                    }
                    FrequencyResultPanelV3(bands, onBandsChange, realResult, Modifier.weight(1f).height(225.dp))
                }
            }
        }
    }
}

@Composable
private fun FrequencyChartPanel(
    title: String,
    subtitle: String,
    modifier: Modifier,
    chart: @Composable () -> Unit
) {
    DoctorPanel(modifier) {
        val badge = when {
            title.startsWith("β") -> "β"
            title.startsWith("γ") -> "γ"
            else -> "S"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .border(1.5.dp, BrandBlue, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(badge, color = BrandBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(9.dp))
            Text(title, color = Color(0xFF3D3D3D), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(subtitle, color = Color(0xFF717789), fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) { chart() }
    }
}

@Composable
private fun FrequencyResultPanelV3(
    bands: FrequencyBands,
    onBandsChange: (FrequencyBands) -> Unit,
    realResult: ApiInitialization?,
    modifier: Modifier
) {
    DoctorPanel(modifier) {
        ResourceSectionTitle("频段提取结果", R.drawable.doctor_section_frequency_result)
        val computed = realResult != null
        FrequencyBandInputV3("β-药物敏感频段", bands.staticBeta, !computed) { onBandsChange(bands.copy(staticBeta = it)) }
        FrequencyBandInputV3("β-运动敏感频段", bands.motionBeta, !computed) { onBandsChange(bands.copy(motionBeta = it)) }
        FrequencyBandInputV3("γ-运动敏感频段", bands.gamma, !computed) { onBandsChange(bands.copy(gamma = it)) }
        realResult?.let { result ->
            val metrics = result.quality_summary["metrics"] as? Map<*, *>
            Text(
                "状态：${if (result.status == "review") "等待医生审核" else result.status} · " +
                    "准确率 ${metrics?.get("accuracy").asPercent()} · " +
                    "Macro-F1 ${metrics?.get("macro_f1").asPercent()}",
                color = if (result.status == "review") BrandBlue else MedicalGreen,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (computed) "频段来自本次四状态数据与 Fisher 分析，审核后启用。" else "尚无真实分析结果。",
            color = Color(0xFF8A91A0),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun FrequencyBandInputV3(
    label: String,
    value: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, color = Color(0xFF4B5363), fontSize = 11.sp, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
            modifier = Modifier.width(145.dp).height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = Color(0xFFDCE5F1),
                focusedContainerColor = Color(0xF7FFFFFF),
                unfocusedContainerColor = Color(0xF2FFFFFF),
                cursorColor = BrandBlue
            )
        )
    }
}

@Composable
private fun WorkflowBottomActionBar(
    workflow: InitializationWorkflowState,
    selectionValid: Boolean,
    frequencyValid: Boolean,
    realState: InitializationUiState? = null,
    onStartSampling: () -> Unit,
    onAnalyze: () -> Unit,
    onEndSampling: () -> Unit,
    onPauseSampling: () -> Unit,
    onUseRecommendedBands: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PremiumSurfaceStrong,
        border = androidx.compose.foundation.BorderStroke(1.dp, PremiumBorder),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (workflow.step) {
                InitializationStep.ElectrodeConfig -> {
                    Text(
                        if (selectionValid) "触点与四组刺激参数校验通过" else "请完成正负触点选择并检查参数安全性",
                        color = if (selectionValid) MedicalGreen else SoftRed,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onConfirm,
                        enabled = selectionValid,
                        modifier = Modifier.width(220.dp).height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("确认并进入基线检测", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
                InitializationStep.BaselineDetection -> {
                    val state = workflow.baseline
                    if (realState != null) {
                        val analyzed = realState.result?.status in setOf("review", "approved")
                        val completed = realState.result?.segments?.count { it.accepted } ?: 0
                        if (completed < 4) {
                            Button(
                                onClick = onStartSampling,
                                enabled = !realState.running && !analyzed,
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    if (completed == 0) "准备并采集第1状态" else "确认并采集下一状态",
                                    fontSize = 12.sp,
                                )
                            }
                        } else if (!analyzed) {
                            Button(
                                onClick = onAnalyze,
                                enabled = !realState.running,
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("启动个体化频段与模型计算", fontSize = 12.sp) }
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                realState.phase,
                                color = if (realState.error == null) Color(0xFF4B5363) else SoftRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                realState.error ?: realState.stateLabel.orEmpty(),
                                color = Color(0xFF717789),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Button(
                            onClick = onConfirm,
                            enabled = analyzed,
                            modifier = Modifier.width(190.dp).height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("进入频段提取", fontSize = 12.sp) }
                    } else {
                        Button(
                            onClick = onStartSampling,
                            enabled = !state.sampling && !state.sampleEnded,
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (state.elapsedSeconds > 0) "继续采样" else "开始采样", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onPauseSampling,
                            enabled = state.sampling,
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("暂停采样", fontSize = 12.sp) }
                        OutlinedButton(
                            onClick = onEndSampling,
                            enabled = state.sampling,
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("结束采样", fontSize = 12.sp) }
                        Spacer(Modifier.weight(1f))
                        Text("第 ${state.activeTask + 1}/4 步  ·  ${state.elapsedSeconds}s", color = Color(0xFF717789), fontSize = 12.sp)
                        Button(
                            onClick = onConfirm,
                            enabled = state.sampleEnded,
                            modifier = Modifier.width(190.dp).height(44.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (state.activeTask == 3) "确认并进入频段提取" else "确认本步", fontSize = 12.sp) }
                    }
                }
                InitializationStep.FrequencyExtraction -> {
                    OutlinedButton(onClick = onUseRecommendedBands, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(14.dp)) {
                        Text("采用推荐参数", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (realState?.result?.status == "review") {
                            "审核后模型才会成为患者当前模型"
                        } else {
                            "确认后保存至当前患者初始化档案"
                        },
                        color = Color(0xFF717789),
                        fontSize = 12.sp,
                    )
                    Button(
                        onClick = onConfirm,
                        enabled = frequencyValid && (
                            realState == null || realState.result?.status in setOf("review", "approved")
                        ),
                        modifier = Modifier.width(230.dp).height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (realState?.result?.status == "review") "审核并启用该模型" else "确认并保存频段结果",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                InitializationStep.Completed -> Unit
            }
        }
    }
}

private fun InitializationStep.displayLabel(): String = when (this) {
    InitializationStep.ElectrodeConfig -> "待配置电极"
    InitializationStep.BaselineDetection -> "正在采集基线"
    InitializationStep.FrequencyExtraction -> "正在提取频段"
    InitializationStep.Completed -> "已完成初始化"
}

private fun FrequencyBands.hasValidRanges(): Boolean =
    listOf(staticBeta, motionBeta, gamma).all { value ->
        val bounds = value
            .replace("Hz", "", ignoreCase = true)
            .split("-")
            .map { it.trim().toFloatOrNull() }
        bounds.size == 2 && bounds[0] != null && bounds[1] != null && bounds[0]!! < bounds[1]!!
    }

private fun ApiInitialization.toFrequencyBands(): FrequencyBands? {
    val values = frequency_results["bands"] as? Map<*, *> ?: return null
    fun band(key: String): String? {
        val range = values[key] as? List<*> ?: return null
        val lower = (range.getOrNull(0) as? Number)?.toDouble() ?: return null
        val upper = (range.getOrNull(1) as? Number)?.toDouble() ?: return null
        return "%.1f-%.1f Hz".format(lower, upper)
    }
    return FrequencyBands(
        staticBeta = band("medication_beta") ?: return null,
        motionBeta = band("movement_beta") ?: return null,
        gamma = band("movement_gamma") ?: return null,
    )
}

private fun Map<String, Any>.floatList(key: String): List<Float> =
    (this[key] as? List<*>)
        ?.mapNotNull { (it as? Number)?.toFloat() }
        .orEmpty()

private fun Any?.asPercent(): String =
    (this as? Number)?.toDouble()?.let { "%.1f%%".format(it * 100.0) } ?: "—"

@Composable
private fun FeedbackOptimizationContentV3(
    repository: MockRepository,
    patient: Patient,
    report: PatientReport,
    gap: Dp,
    onParametersChanged: () -> Unit,
    showMessage: (String) -> Unit,
    realRepository: RealRepository? = null,
    serverPatientId: String? = null,
    currentDeviceParameters: com.omnidapt.protocol.StimulationParameters? = null,
) {
    val scope = rememberCoroutineScope()
    val suggestion = remember(patient.id) { repository.getOptimizationSuggestion(patient.id) }
    var alertMode by remember { mutableStateOf("时间轴") }
    var settings by remember(patient.id) { mutableStateOf(repository.getParameterOptimizationSettings(patient.id)) }
    var currentMa by remember(patient.id) { mutableStateOf(suggestion.suggestedParameters.currentMa.toString()) }
    var frequencyHz by remember(patient.id) { mutableStateOf(suggestion.suggestedParameters.frequencyHz.toString()) }
    var pulseWidthUs by remember(patient.id) { mutableStateOf(suggestion.suggestedParameters.pulseWidthUs.toString()) }
    var dutyCycle by remember(patient.id) { mutableStateOf(suggestion.suggestedParameters.dutyCycle.toString()) }
    var confirmed by remember { mutableStateOf(false) }
    var realTask by remember(serverPatientId) {
        mutableStateOf<com.omnidapt.pd.real.network.ApiOptimizationTask?>(null)
    }
    var optimizationLoading by remember { mutableStateOf(false) }

    LaunchedEffect(realRepository, serverPatientId) {
        if (realRepository != null && serverPatientId != null) {
            while (true) {
                runCatching { realRepository.optimizationTasks(serverPatientId).firstOrNull() }
                    .onSuccess { task ->
                        realTask = task
                        task?.proposals?.lastOrNull { it.status == "submitted" }?.let { proposal ->
                            currentMa = proposal.parameters["current_ma"]?.toString() ?: currentMa
                            frequencyHz = proposal.parameters["frequency_hz"]?.toInt()?.toString() ?: frequencyHz
                            pulseWidthUs = proposal.parameters["pulse_width_us"]?.toInt()?.toString() ?: pulseWidthUs
                            dutyCycle = proposal.parameters["duty_cycle"]?.toInt()?.toString() ?: dutyCycle
                        }
                    }
                delay(2_000)
            }
        }
    }

    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ResourceTitleInline("异常报告概览", R.drawable.doctor_section_alert_overview)
            Spacer(Modifier.weight(1f))
            Text("日期范围  2024-06-07 → 2024-06-14", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            SegmentedTinyButton("时间轴", alertMode == "时间轴") { alertMode = "时间轴" }
            Spacer(Modifier.width(6.dp))
            SegmentedTinyButton("症状汇总", alertMode == "症状汇总") { alertMode = "症状汇总" }
        }
        Spacer(Modifier.height(10.dp))
        DoctorAlertTimelineWithLegend(report.alerts, alertMode == "时间轴")
        Text("该时段内震颤次数较多，请注意", color = SoftRed, fontSize = 12.sp)
    }
    Spacer(Modifier.height(gap))

    BoxWithConstraints {
        val stack = maxWidth < 560.dp
        val twoColumns = maxWidth < 700.dp
        val taskCard: @Composable () -> Unit = {
            DoctorPanel(modifier = Modifier.height(300.dp)) {
                ResourceTitleInline("优化任务设置", R.drawable.doctor_section_optimization_tasks)
                Text("任务量表编辑与得分权重", color = Color(0xFF8A91A0), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OptimizationTaskRows(settings = settings, onSettingsChange = { settings = it })
            }
        }
        val rangeCard: @Composable () -> Unit = {
            DoctorPanel(modifier = Modifier.height(300.dp)) {
                Text("可调参数范围", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                EditableRangeRow("电流强度", settings.currentMin.toString(), settings.currentMax.toString(), "mA") { min, max ->
                    settings = settings.copy(currentMin = min.toFloatOrNull() ?: settings.currentMin, currentMax = max.toFloatOrNull() ?: settings.currentMax)
                }
                EditableRangeRow("频率", settings.frequencyMin.toString(), settings.frequencyMax.toString(), "Hz") { min, max ->
                    settings = settings.copy(frequencyMin = min.toIntOrNull() ?: settings.frequencyMin, frequencyMax = max.toIntOrNull() ?: settings.frequencyMax)
                }
                EditableRangeRow("脉宽", settings.pulseWidthMin.toString(), settings.pulseWidthMax.toString(), "μs") { min, max ->
                    settings = settings.copy(pulseWidthMin = min.toIntOrNull() ?: settings.pulseWidthMin, pulseWidthMax = max.toIntOrNull() ?: settings.pulseWidthMax)
                }
                EditableRangeRow("占空比", settings.dutyCycleMin.toString(), settings.dutyCycleMax.toString(), "%") { min, max ->
                    settings = settings.copy(dutyCycleMin = min.toIntOrNull() ?: settings.dutyCycleMin, dutyCycleMax = max.toIntOrNull() ?: settings.dutyCycleMax)
                }
            }
        }
        val roundsCard: @Composable () -> Unit = {
            DoctorPanel(modifier = Modifier.height(300.dp)) {
                Text("调参轮数", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("建议范围 3-12 轮", color = Color(0xFF8A91A0), fontSize = 10.sp)
                Spacer(Modifier.height(12.dp))
                OptimizationRoundsControl(
                    rounds = settings.optimizationRounds,
                    onDecrease = { settings = settings.copy(optimizationRounds = (settings.optimizationRounds - 1).coerceAtLeast(3)) },
                    onIncrease = { settings = settings.copy(optimizationRounds = (settings.optimizationRounds + 1).coerceAtMost(12)) }
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactCardAction(
                        text = "重置",
                        filled = false,
                        onClick = { settings = ParameterOptimizationSettings() },
                        modifier = Modifier.weight(1f)
                    )
                    CompactCardAction(
                        text = "保存设置",
                        filled = true,
                        onClick = {
                            repository.saveParameterOptimizationSettings(patient.id, settings)
                            if (
                                realRepository != null &&
                                serverPatientId != null &&
                                currentDeviceParameters != null
                            ) {
                                optimizationLoading = true
                                scope.launch {
                                    runCatching {
                                        realRepository.createOptimizationTask(
                                            com.omnidapt.pd.real.network.OptimizationTaskBody(
                                                patient_id = serverPatientId,
                                                settings = mapOf(
                                                    "tremor_weight" to settings.tremorWeight,
                                                    "rigidity_weight" to settings.rigidityWeight,
                                                    "speech_weight" to settings.speechWeight,
                                                    "movement_weight" to settings.movementWeight,
                                                ),
                                                safety_bounds = mapOf(
                                                    "current_min_ma" to settings.currentMin.toDouble(),
                                                    "current_max_ma" to settings.currentMax.toDouble(),
                                                    "pulse_width_min_us" to settings.pulseWidthMin.toDouble(),
                                                    "pulse_width_max_us" to settings.pulseWidthMax.toDouble(),
                                                    "frequency_min_hz" to settings.frequencyMin.toDouble(),
                                                    "frequency_max_hz" to settings.frequencyMax.toDouble(),
                                                    "max_delta_current_ma" to 0.2,
                                                ),
                                                rounds = settings.optimizationRounds,
                                                observation_seconds = 30,
                                                current_parameters = mapOf(
                                                    "current_ma" to currentDeviceParameters.currentMa.toDouble(),
                                                    "frequency_hz" to currentDeviceParameters.frequencyHz.toDouble(),
                                                    "pulse_width_us" to currentDeviceParameters.pulseWidthUs.toDouble(),
                                                    "duty_cycle" to currentDeviceParameters.dutyCycle.toDouble(),
                                                    "left_contact" to currentDeviceParameters.leftContact.toDouble(),
                                                    "right_contact" to currentDeviceParameters.rightContact.toDouble(),
                                                ),
                                            ),
                                        )
                                    }.onSuccess {
                                        realTask = it
                                        showMessage("优化任务已创建，第1轮观察倒计时开始")
                                    }.onFailure {
                                        showMessage(it.message ?: "优化任务创建失败")
                                    }
                                    optimizationLoading = false
                                }
                            } else {
                                showMessage("优化设置已保存；连接模拟器后可创建真实任务")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                taskCard()
                rangeCard()
                roundsCard()
            }
        } else if (twoColumns) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Box(Modifier.weight(1.2f)) { taskCard() }
                    Box(Modifier.weight(1f)) { rangeCard() }
                }
                roundsCard()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Box(Modifier.weight(1.85f)) { taskCard() }
                Box(Modifier.weight(1f)) { rangeCard() }
                Box(Modifier.weight(0.62f)) { roundsCard() }
            }
        }
    }
    Spacer(Modifier.height(gap))

    BoxWithConstraints {
        val stack = maxWidth < 700.dp
        val chartCard: @Composable () -> Unit = {
            DoctorPanel(modifier = Modifier.height(360.dp)) {
                ResourceTitleInline("参数优化可视化", R.drawable.doctor_section_optimization_chart)
                Text(
                    realTask?.let {
                        "真实任务第 ${it.current_round}/${it.rounds} 轮 · ${it.status} · 一维电流 GP + EI"
                    } ?: "创建真实优化任务后显示观测点、GP曲线和置信区间",
                    color = Color(0xFF8A91A0),
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(8.dp))
                realTask?.let {
                    RealBayesianOptimizationChart(it.chart, Modifier.fillMaxWidth().weight(1f))
                } ?: BayesianOptimizationChart(suggestion.curve, Modifier.fillMaxWidth().weight(1f))
            }
        }
        val confirmCard: @Composable () -> Unit = {
            DoctorPanel(modifier = Modifier.height(360.dp)) {
                ResourceTitleInline("参数下发确认", R.drawable.doctor_section_parameter_download)
                Text("下发前可编辑，系统将按当前值写入参数历史", color = Color(0xFF717789), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                ParameterConfirmEditRows(
                    currentMa = currentMa,
                    onCurrentMa = { currentMa = it },
                    frequencyHz = frequencyHz,
                    onFrequencyHz = { frequencyHz = it },
                    pulseWidthUs = pulseWidthUs,
                    onPulseWidthUs = { pulseWidthUs = it },
                    dutyCycle = dutyCycle,
                    onDutyCycle = { dutyCycle = it }
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val pendingProposal = realTask?.proposals?.lastOrNull { it.status == "submitted" }
                    OutlinedButton(
                        onClick = {
                            if (pendingProposal != null && realRepository != null) {
                                scope.launch {
                                    realRepository.reviewProposal(
                                        pendingProposal.id,
                                        false,
                                        "医生拒绝，加入禁用邻域并请求替代建议",
                                    ).onSuccess {
                                        showMessage("已拒绝，后台正在生成替代建议")
                                        serverPatientId?.let { id ->
                                            realTask = realRepository.optimizationTasks(id).firstOrNull()
                                        }
                                    }.onFailure { showMessage(it.message ?: "拒绝失败") }
                                }
                            } else {
                                currentMa = suggestion.suggestedParameters.currentMa.toString()
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(19.dp)
                    ) { Text(if (pendingProposal != null) "拒绝并重算" else "恢复推荐值", fontSize = 11.sp, maxLines = 1) }
                    Button(
                        onClick = {
                            val current = currentMa.toFloatOrNull()
                            val frequency = frequencyHz.toIntOrNull()
                            val pulse = pulseWidthUs.toIntOrNull()
                            val duty = dutyCycle.toIntOrNull()
                            if (current == null || frequency == null || pulse == null || duty == null) {
                                showMessage("请填写有效的下发参数")
                            } else if (
                                current !in settings.currentMin..settings.currentMax ||
                                frequency !in settings.frequencyMin..settings.frequencyMax ||
                                pulse !in settings.pulseWidthMin..settings.pulseWidthMax ||
                                duty !in settings.dutyCycleMin..settings.dutyCycleMax
                            ) {
                                showMessage("下发参数超出当前允许范围，请调整后重试")
                            } else if (pendingProposal != null && realRepository != null) {
                                scope.launch {
                                    realRepository.reviewProposal(
                                        pendingProposal.id,
                                        true,
                                        "医生审核通过，限科研模拟器下发",
                                    ).onSuccess {
                                        confirmed = true
                                        showMessage("已批准，等待模拟器ACK")
                                    }.onFailure { showMessage(it.message ?: "批准下发失败") }
                                }
                            } else {
                                repository.confirmParameterDownload(
                                    patient.id,
                                    suggestion.suggestedParameters.copy(
                                        currentMa = current,
                                        frequencyHz = frequency,
                                        pulseWidthUs = pulse,
                                        dutyCycle = duty
                                    )
                                )
                                confirmed = true
                                onParametersChanged()
                                showMessage("参数已确认下发并写入历史记录")
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(19.dp)
                    ) { Text("确认下发", fontSize = 11.sp, maxLines = 1) }
                }
                if (confirmed) Text("已同步到患者数据报告", color = MedicalGreen, fontSize = 11.sp)
            }
        }
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                chartCard()
                confirmCard()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Box(Modifier.weight(1.65f)) { chartCard() }
                Box(Modifier.weight(0.9f)) { confirmCard() }
            }
        }
    }
}

@Composable
private fun TabletParameterPage(
    repository: MockRepository,
    patient: Patient,
    report: PatientReport,
    compact: Boolean,
    gap: Dp,
    onParametersChanged: () -> Unit,
    showMessage: (String) -> Unit
) {
    val suggestion = remember { repository.getOptimizationSuggestion(patient.id) }
    var confirmed by remember { mutableStateOf(false) }
    var step by remember(patient.id) { mutableStateOf(repository.getInitializationStep(patient.id)) }
    var showResetDialog by remember { mutableStateOf(false) }
    var alertMode by remember { mutableStateOf("时间轴") }
    var settings by remember(patient.id) { mutableStateOf(repository.getParameterOptimizationSettings(patient.id)) }
    var currentMa by remember(patient.id) { mutableStateOf(suggestion.suggestedParameters.currentMa) }
    var frequencyHz by remember(patient.id) { mutableIntStateOf(suggestion.suggestedParameters.frequencyHz) }
    var pulseWidthUs by remember(patient.id) { mutableIntStateOf(suggestion.suggestedParameters.pulseWidthUs) }
    var dutyCycle by remember(patient.id) { mutableIntStateOf(suggestion.suggestedParameters.dutyCycle) }
    if (showResetDialog) {
        PremiumAlertDialog(
            containerColor = PremiumSurfaceStrong,
            onDismissRequest = { showResetDialog = false },
            title = { Text("重新进行初始化") },
            text = { Text("将清空当前初始化流程进度，并回到电极信息配置。确认继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        step = repository.resetInitialization(patient.id)
                        showResetDialog = false
                        showMessage("初始化流程已重置到电极信息配置")
                    }
                ) {
                    Text("重新开始")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("取消") }
            }
        )
    }
    TabletPageTitle("初始化与参数调整")
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkflowStepPill("电极信息配置", step, InitializationStep.ElectrodeConfig)
            WorkflowConnector(step.ordinal >= InitializationStep.BaselineDetection.ordinal, Modifier.weight(1f))
            WorkflowStepPill("基线状态检测", step, InitializationStep.BaselineDetection)
            WorkflowConnector(step.ordinal >= InitializationStep.FrequencyExtraction.ordinal, Modifier.weight(1f))
            WorkflowStepPill("个性化频段提取", step, InitializationStep.FrequencyExtraction)
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    step = repository.previousInitialization(patient.id)
                    showMessage("已返回上一步")
                },
                enabled = step != InitializationStep.ElectrodeConfig,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("上一步", fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    step = repository.advanceInitialization(patient.id)
                    if (step == InitializationStep.Completed) {
                        repository.saveInitializationFrequencyBands(patient.id, FrequencyBands())
                    }
                    showMessage(if (step == InitializationStep.Completed) "已进入反馈优化" else "已进入下一步")
                },
                enabled = step != InitializationStep.Completed,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("下一步", fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                enabled = step == InitializationStep.Completed,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("重新进行初始化", fontSize = 12.sp)
            }
        }
    }
    Spacer(Modifier.height(gap))
    if (step != InitializationStep.Completed) {
        InitializationStepPanel(
            step = step,
            onAdvance = {
                step = repository.advanceInitialization(patient.id)
                if (step == InitializationStep.Completed) {
                    repository.saveInitializationFrequencyBands(patient.id, FrequencyBands())
                }
                showMessage(
                    when (step) {
                        InitializationStep.BaselineDetection -> "电极配置已保存，进入基线检测"
                        InitializationStep.FrequencyExtraction -> "基线检测已完成，进入频段提取"
                        InitializationStep.Completed -> "个体化频段已保存，进入反馈优化"
                        InitializationStep.ElectrodeConfig -> "流程已回到电极配置"
                    }
                )
            }
        )
        return
    }
    DoctorPanel {
        Text("异常报告概览", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentedTinyButton("时间轴", alertMode == "时间轴") { alertMode = "时间轴" }
            SegmentedTinyButton("症状汇总", alertMode == "症状汇总") { alertMode = "症状汇总" }
        }
        Spacer(Modifier.height(12.dp))
        DoctorAlertTimelineWithLegend(report.alerts, alertMode == "时间轴")
        Text("该时段内震颤次数较多，请注意", color = SoftRed, fontSize = 12.sp)
    }
    Spacer(Modifier.height(gap))
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        DoctorPanel(modifier = Modifier.weight(1.36f).height(260.dp)) {
            Text("优化任务设置", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OptimizationTaskRows(
                settings = settings,
                onSettingsChange = { settings = it }
            )
        }
        DoctorPanel(modifier = Modifier.weight(1f).height(260.dp)) {
            Text("可调参数范围", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            EditableRangeRow("电流强度", settings.currentMin.toString(), settings.currentMax.toString(), "mA") { min, max ->
                settings = settings.copy(currentMin = min.toFloatOrNull() ?: settings.currentMin, currentMax = max.toFloatOrNull() ?: settings.currentMax)
            }
            EditableRangeRow("频率", settings.frequencyMin.toString(), settings.frequencyMax.toString(), "Hz") { min, max ->
                settings = settings.copy(frequencyMin = min.toIntOrNull() ?: settings.frequencyMin, frequencyMax = max.toIntOrNull() ?: settings.frequencyMax)
            }
            EditableRangeRow("脉宽", settings.pulseWidthMin.toString(), settings.pulseWidthMax.toString(), "μs") { min, max ->
                settings = settings.copy(pulseWidthMin = min.toIntOrNull() ?: settings.pulseWidthMin, pulseWidthMax = max.toIntOrNull() ?: settings.pulseWidthMax)
            }
            EditableRangeRow("占空比", settings.dutyCycleMin.toString(), settings.dutyCycleMax.toString(), "%") { min, max ->
                settings = settings.copy(dutyCycleMin = min.toIntOrNull() ?: settings.dutyCycleMin, dutyCycleMax = max.toIntOrNull() ?: settings.dutyCycleMax)
            }
        }
        DoctorPanel(modifier = Modifier.weight(0.7f).height(260.dp)) {
            Text("调参轮数", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            OptimizationRoundsControl(
                rounds = settings.optimizationRounds,
                onDecrease = { settings = settings.copy(optimizationRounds = (settings.optimizationRounds - 1).coerceAtLeast(3)) },
                onIncrease = { settings = settings.copy(optimizationRounds = (settings.optimizationRounds + 1).coerceAtMost(12)) }
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    repository.saveParameterOptimizationSettings(patient.id, settings)
                    showMessage("反馈优化设置已保存")
                },
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(19.dp)
            ) { Text("保存设置", fontSize = 13.sp) }
        }
    }
    Spacer(Modifier.height(gap))
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        DoctorPanel(modifier = Modifier.weight(1.34f).height(330.dp)) {
            Text("参数优化可视化：", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            BayesianOptimizationChart(suggestion.curve, Modifier.fillMaxWidth().weight(1f))
        }
        DoctorPanel(modifier = Modifier.weight(0.86f).height(330.dp)) {
            Text("参数下发确认", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("可在下发前微调推荐参数", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            ParameterConfirmEditRows(
                currentMa = currentMa.toString(),
                onCurrentMa = { currentMa = it.toFloatOrNull() ?: currentMa },
                frequencyHz = frequencyHz.toString(),
                onFrequencyHz = { frequencyHz = it.toIntOrNull() ?: frequencyHz },
                pulseWidthUs = pulseWidthUs.toString(),
                onPulseWidthUs = { pulseWidthUs = it.toIntOrNull() ?: pulseWidthUs },
                dutyCycle = dutyCycle.toString(),
                onDutyCycle = { dutyCycle = it.toIntOrNull() ?: dutyCycle }
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        currentMa = suggestion.suggestedParameters.currentMa
                        frequencyHz = suggestion.suggestedParameters.frequencyHz
                        pulseWidthUs = suggestion.suggestedParameters.pulseWidthUs
                        dutyCycle = suggestion.suggestedParameters.dutyCycle
                        showMessage("已恢复推荐参数")
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(19.dp)
                ) { Text("恢复推荐值", fontSize = 12.sp) }
                Button(
                    onClick = {
                        repository.confirmParameterDownload(
                            patient.id,
                            suggestion.suggestedParameters.copy(
                                currentMa = currentMa,
                                frequencyHz = frequencyHz,
                                pulseWidthUs = pulseWidthUs,
                                dutyCycle = dutyCycle
                            )
                        )
                        confirmed = true
                        onParametersChanged()
                        showMessage("参数已确认下发并写入历史记录")
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(19.dp)
                ) { Text("确认下发", fontSize = 12.sp) }
            }
            if (confirmed) {
                Spacer(Modifier.height(10.dp))
                Text("参数已下发，并同步到患者数据报告", color = MedicalGreen, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun InitializationStepPanel(step: InitializationStep, onAdvance: () -> Unit) {
    when (step) {
        InitializationStep.ElectrodeConfig -> ElectrodeConfigurationStepV2(onAdvance)
        InitializationStep.BaselineDetection -> BaselineDetectionStepV2(onAdvance)
        InitializationStep.FrequencyExtraction -> FrequencyExtractionStep(onAdvance)
        InitializationStep.Completed -> Unit
    }
}

private data class ImpedancePoint(val label: String, val valueKOhm: Float)

private data class StimulationParameterDraft(
    val state: String,
    val frequency: String,
    val amplitude: String,
    val pulseWidth: String,
    val dutyCycle: String
)

private fun StimulationParameterDraft.isSafe(): Boolean {
    val frequencyValue = frequency.toIntOrNull() ?: return false
    val amplitudeValue = amplitude.toFloatOrNull() ?: return false
    val pulseValue = pulseWidth.toIntOrNull() ?: return false
    val dutyValue = dutyCycle.toIntOrNull() ?: return false
    return frequencyValue in 90..170 &&
        amplitudeValue in 0.5f..3.5f &&
        pulseValue in 40..90 &&
        dutyValue in 20..80
}

@Composable
private fun ElectrodeConfigurationStepV2(onAdvance: () -> Unit) {
    var leftPositive by remember { mutableStateOf("5") }
    var leftNegative by remember { mutableStateOf("6") }
    var rightPositive by remember { mutableStateOf("1") }
    var rightNegative by remember { mutableStateOf("2") }
    var impedanceMode by remember { mutableStateOf("单极") }
    var parameters by remember {
        mutableStateOf(
            listOf(
                StimulationParameterDraft("药物失效-静息", "130", "2.5", "60", "45"),
                StimulationParameterDraft("药物失效-运动", "130", "2.8", "65", "50"),
                StimulationParameterDraft("药物生效-静息", "120", "1.6", "55", "40"),
                StimulationParameterDraft("药物生效-运动", "135", "2.1", "60", "45")
            )
        )
    }
    val safe = leftPositive != leftNegative && rightPositive != rightNegative && parameters.all { it.isSafe() }

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DoctorPanel(modifier = Modifier.weight(0.88f).height(330.dp)) {
            SectionTitle("电极配置", Icons.Filled.Tune)
            Text("左右脑各选择一对正负触点；+ 为阳极，- 为阴极，蓝色为当前选择。", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                ElectrodePairColumn(
                    title = "左脑",
                    contacts = listOf("8", "7", "6", "5"),
                    positive = leftPositive,
                    negative = leftNegative,
                    onPositive = { if (it != leftNegative) leftPositive = it },
                    onNegative = { if (it != leftPositive) leftNegative = it }
                )
                ElectrodePairColumn(
                    title = "右脑",
                    contacts = listOf("4", "3", "2", "1"),
                    positive = rightPositive,
                    negative = rightNegative,
                    onPositive = { if (it != rightNegative) rightPositive = it },
                    onNegative = { if (it != rightPositive) rightNegative = it }
                )
            }
            Text(
                "推荐组合：左脑 $leftPositive+ / $leftNegative-    右脑 $rightPositive+ / $rightNegative-",
                color = BrandBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DoctorPanel(modifier = Modifier.weight(1.16f).height(330.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("阻抗测试", Icons.Filled.CheckCircle)
                Spacer(Modifier.weight(1f))
                SegmentedTinyButton("单极", impedanceMode == "单极") { impedanceMode = "单极" }
                Spacer(Modifier.width(6.dp))
                SegmentedTinyButton("双极", impedanceMode == "双极") { impedanceMode = "双极" }
            }
            Text("预留阻抗数据接口：后续可直接替换 impedanceSeries() 的模拟数组。", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                ImpedanceAxisChart("左脑 - $impedanceMode", impedanceSeries("left", impedanceMode), Modifier.weight(1f))
                ImpedanceAxisChart("右脑 - $impedanceMode", impedanceSeries("right", impedanceMode), Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    DoctorPanel {
        SectionTitle("当前刺激参数", Icons.Filled.Settings)
        Spacer(Modifier.height(10.dp))
        EditableStimulationParameterTable(
            rows = parameters,
            onRowsChange = { parameters = it }
        )
        Spacer(Modifier.height(8.dp))
        Text("安全判断规则：频率 90-170Hz、幅值 0.5-3.5mV、脉宽 40-90μS，且同侧正负触点不可相同。", color = Color(0xFF8A91A0), fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAdvance,
            enabled = safe,
            modifier = Modifier.width(168.dp).height(38.dp).align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(19.dp)
        ) {
            Text("确认")
        }
    }
}

@Composable
private fun ElectrodePairColumn(
    title: String,
    contacts: List<String>,
    positive: String,
    negative: String,
    onPositive: (String) -> Unit,
    onNegative: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color(0xFF3D3D3D), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(if (title == "左脑") "背侧触点 8 → 腹侧触点 5" else "背侧触点 4 → 腹侧触点 1", color = Color(0xFF8A91A0), fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(130.dp).height(190.dp), contentAlignment = Alignment.Center) {
            SegmentedElectrodeBody(Modifier.width(56.dp).fillMaxHeight())
            Column(verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                contacts.forEach { contact ->
                    ElectrodeContactButton(
                        contact = contact,
                        role = when (contact) {
                            positive -> "+"
                            negative -> "-"
                            else -> ""
                        },
                        onPositive = { onPositive(contact) },
                        onNegative = { onNegative(contact) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ElectrodeContactButton(contact: String, role: String, onPositive: () -> Unit, onNegative: () -> Unit) {
    Row(
        modifier = Modifier.width(124.dp).height(38.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(1.dp, if (role == "+") BrandBlue else Color(0xFFCCD5E2), CircleShape)
                .background(if (role == "-") BrandBlue else Color.White)
                .omniClickable(shape = CircleShape) {
                    if (role == "+") onNegative() else onPositive()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(contact, color = if (role == "-") Color.White else Color(0xFF6D7486), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PolarityChip("+", selected = role == "+", onClick = onPositive)
            PolarityChip("-", selected = role == "-", onClick = onNegative)
        }
    }
}

@Composable
private fun PolarityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(34.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) BrandBlue else Color.White)
            .border(1.dp, if (selected) BrandBlue else Color(0xFFD5DCE8), RoundedCornerShape(13.dp))
            .omniClickable(shape = RoundedCornerShape(13.dp), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else Color(0xFF717789), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SegmentedElectrodeBody(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val segmentHeight = size.height / 4f
        repeat(4) { index ->
            val top = index * segmentHeight
            drawRoundRect(
                color = Color(0xFFF8FAFE),
                topLeft = Offset(0f, top + 3f),
                size = Size(size.width, segmentHeight - 6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )
            drawOval(Color(0xFFE4EAF3), Offset(0f, top + 2f), Size(size.width, 16f))
            drawOval(Color(0xFFD4DBE7), Offset(0f, top + segmentHeight - 18f), Size(size.width, 16f))
            drawLine(
                Color(0xFFCAD3E1),
                Offset(7f, top + segmentHeight),
                Offset(size.width - 7f, top + segmentHeight),
                strokeWidth = 1.4f
            )
        }
    }
}

@Composable
private fun CompactEditField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Color(0xFF717789), fontSize = 12.sp)
        OutlinedTextField(value, onValueChange, singleLine = true, modifier = Modifier.fillMaxWidth().height(54.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp))
    }
}

@Composable
private fun EditableStimulationParameterTable(
    rows: List<StimulationParameterDraft>,
    onRowsChange: (List<StimulationParameterDraft>) -> Unit
) {
    Column(Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
        Row(
            modifier = Modifier.fillMaxWidth().height(38.dp).background(Color(0xFFF6F8FC)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StimParamHeaderCell("基线状态", 1.45f)
            StimParamHeaderCell("频率(Hz)", 0.78f)
            StimParamHeaderCell("幅值(mV)", 0.78f)
            StimParamHeaderCell("脉宽(μs)", 0.78f)
            StimParamHeaderCell("占空比", 0.78f)
            StimParamHeaderCell("安全性", 0.78f)
        }
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).background(if (index % 2 == 0) Color.White else Color(0xFFFBFCFF)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    row.state,
                    color = Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1.45f).padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StimParamInput(row.frequency, "Hz", Modifier.weight(0.78f)) { next ->
                    onRowsChange(rows.updateAt(index) { it.copy(frequency = next) })
                }
                StimParamInput(row.amplitude, "mV", Modifier.weight(0.78f)) { next ->
                    onRowsChange(rows.updateAt(index) { it.copy(amplitude = next) })
                }
                StimParamInput(row.pulseWidth, "μs", Modifier.weight(0.78f)) { next ->
                    onRowsChange(rows.updateAt(index) { it.copy(pulseWidth = next) })
                }
                StimParamInput(row.dutyCycle, "%", Modifier.weight(0.78f)) { next ->
                    onRowsChange(rows.updateAt(index) { it.copy(dutyCycle = next) })
                }
                Box(
                    Modifier
                        .weight(0.78f)
                        .padding(horizontal = 6.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (row.isSafe()) Color(0xFFE8F8F0) else Color(0xFFFFECEC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (row.isSafe()) "安全" else "需复核", color = if (row.isSafe()) MedicalGreen else SoftRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun List<StimulationParameterDraft>.updateAt(
    index: Int,
    transform: (StimulationParameterDraft) -> StimulationParameterDraft
): List<StimulationParameterDraft> = mapIndexed { rowIndex, row ->
    if (rowIndex == index) transform(row) else row
}

@Composable
private fun RowScope.StimParamHeaderCell(text: String, weight: Float) {
    Text(
        text,
        color = Color(0xFF3D3D3D),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(weight)
    )
}

@Composable
private fun StimParamInput(value: String, unit: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        suffix = { Text(unit, color = Color(0xFF8A91A0), fontSize = 10.sp) },
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center),
        modifier = modifier.padding(horizontal = 4.dp).height(44.dp)
    )
}

private fun impedanceSeries(side: String, mode: String): List<ImpedancePoint> =
    if (mode == "单极") {
        if (side == "left") {
            listOf("8", "7", "6", "5").zip(listOf(1.1f, 1.2f, 0.9f, 1.0f)) { label, value -> ImpedancePoint(label, value) }
        } else {
            listOf("4", "3", "2", "1").zip(listOf(1.0f, 2.8f, 1.1f, 0.9f)) { label, value -> ImpedancePoint(label, value) }
        }
    } else {
        if (side == "left") {
            listOf("8-7", "7-6", "6-5").zip(listOf(1.5f, 1.6f, 1.4f)) { label, value -> ImpedancePoint(label, value) }
        } else {
            listOf("4-3", "3-2", "2-1").zip(listOf(2.5f, 2.6f, 1.5f)) { label, value -> ImpedancePoint(label, value) }
        }
    }

@Composable
private fun ImpedanceAxisChart(title: String, values: List<ImpedancePoint>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = Color(0xFF3D3D3D), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Canvas(Modifier.fillMaxSize().padding(start = 30.dp, top = 12.dp, end = 8.dp, bottom = 26.dp)) {
                val axis = Color(0xFFD6DCE6)
                drawLine(axis, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.2f)
                drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.2f)
                repeat(4) { i ->
                    val y = size.height * i / 3f
                    drawLine(Color(0xFFECEFF5), Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
                }
                val barWidth = size.width / (values.size * 2f + 1f)
                values.forEachIndexed { index, value ->
                    val left = barWidth * (1 + index * 2)
                    val h = (value.valueKOhm / 3.2f).coerceIn(0f, 1f) * size.height
                    drawRoundRect(
                        color = BrandBlue,
                        topLeft = Offset(left, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
            Text("kΩ", color = Color(0xFF8A91A0), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopStart))
            Text("触点", color = Color(0xFF8A91A0), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd))
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 30.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            values.forEach { Text(it.label, color = Color(0xFF8A91A0), fontSize = 9.sp) }
        }
    }
}

@Composable
private fun BayesianOptimizationChart(scores: List<Float>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxSize().padding(start = 42.dp, top = 14.dp, end = 8.dp, bottom = 28.dp)) {
                    val axis = Color(0xFFD6DCE6)
                    drawLine(axis, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.2f)
                    drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.2f)
                    repeat(5) { i ->
                        val y = size.height * i / 4f
                        drawLine(Color(0xFFECEFF5), Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
                    }
                    val normalized = scores.map { ((it - 20f) / 80f).coerceIn(0f, 1f) }
                    val points = normalized.mapIndexed { index, value ->
                        Offset(size.width * index / normalized.lastIndex.coerceAtLeast(1), size.height * (1f - value))
                    }
                    val band = Path().apply {
                        points.forEachIndexed { index, point ->
                            val uncertainty = (34f - index * 2.2f).coerceAtLeast(8f)
                            if (index == 0) moveTo(point.x, (point.y - uncertainty).coerceAtLeast(0f))
                            else lineTo(point.x, (point.y - uncertainty).coerceAtLeast(0f))
                        }
                        points.asReversed().forEachIndexed { reverseIndex, point ->
                            val index = points.lastIndex - reverseIndex
                            val uncertainty = (34f - index * 2.2f).coerceAtLeast(8f)
                            lineTo(point.x, (point.y + uncertainty).coerceAtMost(size.height))
                        }
                        close()
                    }
                    drawPath(band, BrandBlue.copy(alpha = 0.16f))

                    val fit = Path()
                    points.forEachIndexed { index, point ->
                        if (index == 0) fit.moveTo(point.x, point.y) else fit.lineTo(point.x, point.y)
                    }
                    drawPath(fit, BrandBlue, style = Stroke(width = 3f, cap = StrokeCap.Round))

                    var runningBest = 0f
                    val bestPath = Path()
                    points.forEachIndexed { index, point ->
                        runningBest = maxOf(runningBest, normalized[index])
                        val bestPoint = Offset(point.x, size.height * (1f - runningBest))
                        if (index == 0) bestPath.moveTo(bestPoint.x, bestPoint.y)
                        else {
                            val previousX = points[index - 1].x
                            bestPath.lineTo(previousX, bestPoint.y)
                            bestPath.lineTo(bestPoint.x, bestPoint.y)
                        }
                    }
                    drawPath(bestPath, MedicalGreen, style = Stroke(width = 2.4f))
                    points.forEachIndexed { index, point ->
                        drawCircle(if (index == points.lastIndex) MedicalGreen else BrandBlue, radius = 4.5f, center = point)
                    }
                }
                Column(
                    modifier = Modifier.fillMaxHeight().width(34.dp).padding(top = 8.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf("100", "80", "60", "40", "20").forEach {
                        Text(it, color = Color(0xFF8A91A0), fontSize = 8.sp)
                    }
                }
                Text("得分", color = Color(0xFF8A91A0), fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart))
                Text("轮次", color = Color(0xFF8A91A0), fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomEnd))
            }
            Column(
                modifier = Modifier.width(112.dp).padding(start = 10.dp, top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ChartLegendItem(BrandBlue, "观测点", dot = true)
                ChartLegendItem(BrandBlue, "优化预测曲线")
                ChartLegendItem(MedicalGreen, "当前最优")
                ChartLegendItem(BrandBlue.copy(alpha = 0.18f), "不确定范围", thick = true)
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(start = 42.dp, end = 120.dp)) {
            scores.forEachIndexed { index, _ ->
                Text("${index + 1}", color = Color(0xFF8A91A0), fontSize = 9.sp)
            }
        }
        Text("浅蓝区域为不确定范围，蓝线为拟合曲线，点为每轮优化得分。", color = Color(0xFF717789), fontSize = 11.sp)
    }
}

@Composable
private fun RealBayesianOptimizationChart(
    chart: com.omnidapt.pd.real.network.ApiOptimizationChart,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                Modifier.width(40.dp).fillMaxHeight().padding(vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                listOf("100", "75", "50", "25", "0").forEach {
                    Text(it, color = Color(0xFF8A91A0), fontSize = 8.sp)
                }
            }
            Canvas(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                repeat(5) { index ->
                    val y = size.height * index / 4f
                    drawLine(Color(0xFFE8ECF2), Offset(0f, y), Offset(size.width, y), 1f)
                }
                val mean = chart.mean
                val std = chart.std
                if (mean.size > 1 && mean.size == std.size) {
                    val band = Path()
                    mean.indices.forEach { index ->
                        val x = size.width * index / mean.lastIndex
                        val y = size.height * (1f - ((mean[index] + 1.96 * std[index]) / 100.0).toFloat().coerceIn(0f, 1f))
                        if (index == 0) band.moveTo(x, y) else band.lineTo(x, y)
                    }
                    mean.indices.reversed().forEach { index ->
                        val x = size.width * index / mean.lastIndex
                        val y = size.height * (1f - ((mean[index] - 1.96 * std[index]) / 100.0).toFloat().coerceIn(0f, 1f))
                        band.lineTo(x, y)
                    }
                    band.close()
                    drawPath(band, BrandBlue.copy(alpha = 0.15f))
                    val fit = Path()
                    mean.forEachIndexed { index, value ->
                        val point = Offset(
                            size.width * index / mean.lastIndex,
                            size.height * (1f - (value / 100.0).toFloat().coerceIn(0f, 1f)),
                        )
                        if (index == 0) fit.moveTo(point.x, point.y) else fit.lineTo(point.x, point.y)
                    }
                    drawPath(fit, BrandBlue, style = Stroke(width = 2.8f))
                }
                val grid = chart.grid_current_ma
                val low = grid.firstOrNull() ?: 1.0
                val high = grid.lastOrNull() ?: 3.0
                chart.observations.forEach { observation ->
                    val x = (((observation.current_ma - low) / (high - low).coerceAtLeast(1e-6)) * size.width).toFloat()
                    val y = size.height * (1f - (observation.score / 100.0).toFloat().coerceIn(0f, 1f))
                    drawCircle(Color(0xFFFF8A1C), 5f, Offset(x, y))
                }
                chart.next_current_ma?.let { next ->
                    val x = (((next - low) / (high - low).coerceAtLeast(1e-6)) * size.width).toFloat()
                    drawLine(MedicalGreen, Offset(x, 0f), Offset(x, size.height), 2f)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 44.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val grid = chart.grid_current_ma
            val low = grid.firstOrNull() ?: 1.0
            val high = grid.lastOrNull() ?: 3.0
            listOf(low, (low + high) / 2.0, high).forEach {
                Text("%.2f mA".format(it), color = Color(0xFF8A91A0), fontSize = 8.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChartLegendItem(Color(0xFFFF8A1C), "患者观测", dot = true)
            ChartLegendItem(BrandBlue, "GP均值")
            ChartLegendItem(BrandBlue.copy(alpha = 0.15f), "95%置信区间", thick = true)
            ChartLegendItem(MedicalGreen, "下一候选")
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String, dot: Boolean = false, thick: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (dot) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        } else {
            Box(Modifier.width(22.dp).height(if (thick) 8.dp else 2.dp).background(color))
        }
        Spacer(Modifier.width(7.dp))
        Text(label, color = Color(0xFF5F687B), fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun ParameterConfirmEditRows(
    currentMa: String,
    onCurrentMa: (String) -> Unit,
    frequencyHz: String,
    onFrequencyHz: (String) -> Unit,
    pulseWidthUs: String,
    onPulseWidthUs: (String) -> Unit,
    dutyCycle: String,
    onDutyCycle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ConfirmParamInput("电流强度", currentMa, "mA", onCurrentMa)
        ConfirmParamInput("频率", frequencyHz, "Hz", onFrequencyHz)
        ConfirmParamInput("脉宽", pulseWidthUs, "μs", onPulseWidthUs)
        ConfirmParamInput("占空比", dutyCycle, "%", onDutyCycle)
    }
}

@Composable
private fun ConfirmParamInput(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(label, color = MutedText, fontSize = 12.sp, modifier = Modifier.width(68.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            suffix = { Text(unit, color = Color(0xFF8A91A0), fontSize = 11.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f).height(48.dp)
        )
    }
}

@Composable
private fun ElectrodeConfigurationStep(onAdvance: () -> Unit) {
    var leftContact by remember { mutableStateOf("6") }
    var rightContact by remember { mutableStateOf("2") }
    var impedanceMode by remember { mutableStateOf("单极") }
    var sweepRunning by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DoctorPanel(modifier = Modifier.weight(0.8f).height(330.dp)) {
            SectionTitle("电极配置", Icons.Filled.Tune)
            Text("点击触点完成智能扫频候选组合；蓝色为当前推荐闭环刺激触点。", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                BrainElectrodeColumn("左脑", listOf("8", "7", "6", "5"), leftContact) { leftContact = it }
                BrainElectrodeColumn("右脑", listOf("4", "3", "2", "1"), rightContact) { rightContact = it }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("推荐组合：左脑 $leftContact- / 右脑 $rightContact-", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { sweepRunning = !sweepRunning }, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(if (sweepRunning) "停止扫频" else "开始扫频", fontSize = 12.sp)
                }
            }
        }
        DoctorPanel(modifier = Modifier.weight(1.25f).height(330.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("阻抗测试", Icons.Filled.CheckCircle)
                Spacer(Modifier.weight(1f))
                SegmentedTinyButton("单极", impedanceMode == "单极") { impedanceMode = "单极" }
                Spacer(Modifier.width(6.dp))
                SegmentedTinyButton("双极", impedanceMode == "双极") { impedanceMode = "双极" }
            }
            Text("用于排除高阻抗触点，并把触点-抑制效率结果转化为医生可确认的组合。", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            val charts = if (impedanceMode == "单极") {
                listOf(
                    "左脑 - 单极阻抗（Case-触点）" to listOf(1.1f, 1.2f, 0.9f, 1.0f),
                    "右脑 - 单极阻抗（Case-触点）" to listOf(1.0f, 2.8f, 1.1f, 0.9f)
                )
            } else {
                listOf(
                    "左脑 - 双极阻抗（触点-触点）" to listOf(1.5f, 1.6f, 1.4f),
                    "右脑 - 双极阻抗（触点-触点）" to listOf(2.5f, 2.6f, 1.5f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                charts.forEach { (title, values) ->
                    ImpedanceBarChart(title, values, Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    DoctorPanel {
        SectionTitle("当前刺激参数", Icons.Filled.Settings)
        Spacer(Modifier.height(12.dp))
        DoctorTable(
            headers = listOf("基线状态", "频率(HZ)", "幅值(mV)", "脉宽(μS)", "安全性"),
            rows = listOf(
                listOf("药物关-静息", "130", "2.5", "60", "安全"),
                listOf("药物开-静息", "130", "1.5", "70", "安全"),
                listOf("药物关-运动", "130", "2.0", "50", "安全"),
                listOf("药物开-运动", "130", "1.0", "55", "安全")
            )
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAdvance, modifier = Modifier.width(160.dp).height(36.dp).align(Alignment.CenterHorizontally), shape = RoundedCornerShape(18.dp)) {
            Text("确认")
        }
    }
}

@Composable
private fun BaselineDetectionStepV2(onAdvance: () -> Unit) {
    val tasks = listOf(
        "检测药物失效-静息状态",
        "检测药物失效-运动状态",
        "检测药物生效-静息状态",
        "检测药物生效-运动状态"
    )
    var activeTask by remember { mutableIntStateOf(0) }
    var completedTasks by remember { mutableStateOf(emptySet<Int>()) }
    var sampling by remember { mutableStateOf(false) }
    var sampleEnded by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DoctorPanel(modifier = Modifier.weight(0.75f).height(310.dp)) {
            SectionTitle("左旋多巴冲击测试", Icons.Filled.MedicalServices)
            Text("按 OFF/ON 与静息/运动状态依次采集脑电，必须逐项完成后才能进入频段提取。", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            tasks.forEachIndexed { index, label ->
                BaselineTaskRow(
                    label = if (index == activeTask) "$label（当前步骤）" else label,
                    done = index in completedTasks,
                    current = index == activeTask,
                    onClick = {
                        if (index <= completedTasks.size) {
                            activeTask = index
                            sampling = false
                            sampleEnded = index in completedTasks
                        }
                    }
                )
            }
        }
        DoctorPanel(modifier = Modifier.weight(1.3f).height(310.dp)) {
            SectionTitle("测试内容", Icons.Filled.Info)
            Spacer(Modifier.height(12.dp))
            Text(baselineInstruction(activeTask), color = Color(0xFF3D3D3D), fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text(if (activeTask % 2 == 1) "示例动作：" else "静息采集要求：", color = Color(0xFF3D3D3D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            if (activeTask % 2 == 1) {
                Image(
                    painterResource(R.drawable.doctor_baseline_motion),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                RestStateGuideCard(Modifier.fillMaxWidth().height(132.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (activeTask < 2) "请在服药前 OFF 期完成采样，避免混入药效恢复阶段。" else "请确保采样时患者已达状态平台期，药物服用后约 20-30min。",
                color = Color(0xFF3D3D3D),
                fontSize = 13.sp
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("数据观察", Icons.AutoMirrored.Filled.ShowChart)
            Spacer(Modifier.weight(1f))
            Text(
                "EEG001(4+,1-)    ${when { sampling -> "正在采样"; sampleEnded -> "本步采样完成"; else -> "等待采样" }}",
                color = Color(0xFF686F82),
                fontSize = 12.sp
            )
            Spacer(Modifier.width(10.dp))
            OutlinedButton(onClick = {}, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(6.dp)) { Text("显示设置", fontSize = 12.sp) }
        }
        Spacer(Modifier.height(10.dp))
        LfpObservationChart(sampling, Modifier.fillMaxWidth().height(190.dp))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    sampling = true
                    sampleEnded = false
                },
                enabled = activeTask !in completedTasks && !sampling,
                modifier = Modifier.height(34.dp)
            ) { Text("开始采样") }
            OutlinedButton(
                onClick = {
                    sampling = false
                    sampleEnded = true
                },
                enabled = sampling,
                modifier = Modifier.height(34.dp)
            ) { Text("结束采样") }
            OutlinedButton(onClick = { sampling = false }, enabled = sampling, modifier = Modifier.height(34.dp)) { Text("暂停采样") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    completedTasks = completedTasks + activeTask
                    sampling = false
                    sampleEnded = false
                    if (activeTask == tasks.lastIndex) {
                        onAdvance()
                    } else {
                        activeTask += 1
                    }
                },
                enabled = sampleEnded || activeTask in completedTasks,
                modifier = Modifier.width(150.dp).height(36.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text(if (activeTask == tasks.lastIndex) "确认并进入下一步" else "确认本步") }
        }
    }
}

@Composable
private fun RestStateGuideCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFE))
            .border(1.dp, Color(0xFFE1E6EE), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(36.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("保持安静坐立 60 秒", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("闭眼或平视前方，减少主动运动、说话和吞咽动作。", color = Color(0xFF717789), fontSize = 13.sp)
            Text("采集过程中如出现明显震颤或不适，可暂停后重新采样。", color = Color(0xFF717789), fontSize = 13.sp)
        }
    }
}

@Composable
private fun BaselineDetectionStep(onAdvance: () -> Unit) {
    val tasks = listOf(
        "检测药物失效-静息状态",
        "检测药物失效-运动状态",
        "检测药物生效-静息状态",
        "检测药物生效-运动状态"
    )
    var activeTask by remember { mutableIntStateOf(2) }
    var completedTasks by remember { mutableStateOf(setOf(0, 1)) }
    var sampling by remember { mutableStateOf(false) }
    val allComplete = completedTasks.size == tasks.size

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DoctorPanel(modifier = Modifier.weight(0.75f).height(310.dp)) {
            SectionTitle("左旋多巴冲击测试", Icons.Filled.MedicalServices)
            Text("按 OFF/ON 期与静息/运动状态分时段截取纯净 LFP，建立一人一参基线。", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            tasks.forEachIndexed { index, label ->
                BaselineTaskRow(
                    label = if (index == activeTask) "$label（当前步骤）" else label,
                    done = index in completedTasks,
                    current = index == activeTask,
                    onClick = {
                        activeTask = index
                        sampling = false
                    }
                )
            }
        }
        DoctorPanel(modifier = Modifier.weight(1.3f).height(310.dp)) {
            SectionTitle("测试内容", Icons.Filled.Info)
            Spacer(Modifier.height(18.dp))
            Text(baselineInstruction(activeTask), color = Color(0xFF3D3D3D), fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Text("示例动作：", color = Color(0xFF3D3D3D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SitStandGuide(Modifier.fillMaxWidth().height(120.dp))
            Spacer(Modifier.height(10.dp))
            Text(if (activeTask >= 2) "请确保采样时患者已经状态平稳且药物服用后20-30min" else "请在服药前 OFF 期完成采样，避免混入药效恢复阶段。", color = Color(0xFF3D3D3D), fontSize = 15.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("数据观察", Icons.AutoMirrored.Filled.ShowChart)
            Spacer(Modifier.weight(1f))
            Text("EEG001(4+,1-)    ${if (sampling) "正在采样" else "已记录时间：36s"}", color = Color(0xFF686F82), fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            OutlinedButton(onClick = {}, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(6.dp)) { Text("显示设置", fontSize = 12.sp) }
        }
        Spacer(Modifier.height(10.dp))
        LfpObservationChart(sampling, Modifier.fillMaxWidth().height(190.dp))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { sampling = true }, enabled = activeTask !in completedTasks, modifier = Modifier.height(34.dp)) { Text("开始采样") }
            OutlinedButton(
                onClick = {
                    completedTasks = completedTasks + activeTask
                    sampling = false
                    activeTask = ((activeTask + 1)..3).firstOrNull { it !in completedTasks } ?: activeTask
                },
                enabled = sampling,
                modifier = Modifier.height(34.dp)
            ) { Text("结束采样") }
            OutlinedButton(onClick = { sampling = false }, enabled = sampling, modifier = Modifier.height(34.dp)) { Text("暂停采样") }
            Spacer(Modifier.weight(1f))
            Button(onClick = onAdvance, enabled = allComplete, modifier = Modifier.width(150.dp).height(36.dp), shape = RoundedCornerShape(18.dp)) { Text("确认") }
        }
    }
}

@Composable
private fun FrequencyExtractionStep(onAdvance: () -> Unit) {
    var drugBeta by remember { mutableStateOf("14.1-21.2Hz") }
    var motionBeta by remember { mutableStateOf("27.5-31.9Hz") }
    var motionGamma by remember { mutableStateOf("81.7-84.8Hz") }
    var saved by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DoctorPanel(modifier = Modifier.weight(1.25f).height(380.dp)) {
            SectionTitle("β频段提取可视化：", Icons.Filled.BarChart)
            Text("低β对应药效指示器，高β对应运动/发声行为触发器。", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            BetaBandChart(modifier = Modifier.fillMaxSize())
        }
        DoctorPanel(modifier = Modifier.weight(1f).height(380.dp)) {
            SectionTitle("样本分布可视化：", Icons.Filled.Group)
            Text("将 OFF/ON、静息/运动样本聚类，避免药物状态和运动意图混杂。", color = Color(0xFF717789), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            SampleDistributionChart(modifier = Modifier.fillMaxSize())
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DoctorPanel(modifier = Modifier.weight(1.25f).height(290.dp)) {
            SectionTitle("γ频段提取可视化：", Icons.AutoMirrored.Filled.ShowChart)
            Spacer(Modifier.height(8.dp))
            GammaBandChart(modifier = Modifier.fillMaxSize())
        }
        DoctorPanel(modifier = Modifier.weight(1f).height(290.dp)) {
            SectionTitle("频段提取结果：", Icons.Filled.Download)
            Spacer(Modifier.height(16.dp))
            FrequencyResultRow("β-药物敏感频段：", "14.1-21.2Hz", drugBeta) { drugBeta = it }
            FrequencyResultRow("β-运动敏感频段：", "27.5-31.9Hz", motionBeta) { motionBeta = it }
            FrequencyResultRow("γ-运动敏感频段：", "81.7-84.8Hz", motionGamma) { motionGamma = it }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        drugBeta = "14.1-21.2Hz"
                        motionBeta = "27.5-31.9Hz"
                        motionGamma = "81.7-84.8Hz"
                        saved = false
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(34.dp).weight(1f)
                ) {
                    Text("采用推荐参数", fontSize = 12.sp)
                }
                Button(
                    onClick = { saved = true },
                    enabled = drugBeta.isNotBlank() && motionBeta.isNotBlank() && motionGamma.isNotBlank(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(34.dp).weight(1f)
                ) {
                    Text("保存频段结果", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (saved) {
                    Text("已保存，可进入反馈优化", color = MedicalGreen, fontSize = 12.sp, modifier = Modifier.weight(1f))
                } else {
                    Text("请保存频段结果后进入反馈优化", color = Color(0xFF8A91A0), fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
                OutlinedButton(
                    onClick = onAdvance,
                    enabled = saved,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(34.dp).width(150.dp)
                ) {
                    Text("进入反馈优化", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ElectrodeConfigCard(title: String, contact: String, status: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFFE1E6EE), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(title, color = Color(0xFF3D3D3D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(contact, color = Color(0xFF5F687B), fontSize = 13.sp)
        Text(status, color = MedicalGreen, fontSize = 12.sp)
    }
}

@Composable
private fun BaselineMetric(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFFE1E6EE), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color(0xFF717789), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color(0xFF3D3D3D), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WorkflowStepPill(label: String, current: InitializationStep, target: InitializationStep) {
    val isCurrent = current == target
    val isDone = current.ordinal > target.ordinal || current == InitializationStep.Completed
    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isCurrent -> Color(0xFFE8F3FF)
                    isDone -> Color(0xFFEAF8F1)
                    else -> Color(0xFFF5F7FB)
                }
            )
            .border(
                1.dp,
                when {
                    isCurrent -> BrandBlue
                    isDone -> Color(0xFFBDEBD3)
                    else -> Color(0xFFE0E6EF)
                },
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> MedicalGreen
                        isCurrent -> BrandBlue
                        else -> Color(0xFFDDE3EC)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isDone) "✓" else "${target.ordinal + 1}",
                color = if (isDone || isCurrent) Color.White else Color(0xFF778196),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            color = when {
                isCurrent -> BrandBlue
                isDone -> Color(0xFF0D9B59)
                else -> Color(0xFF626B7D)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WorkflowConnector(done: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .background(
                if (done) {
                    Brush.horizontalGradient(listOf(MedicalGreen, Color(0xFF56D4A0)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFD7DFEA), Color(0xFFB9C5D5)))
                }
            )
    )
}

@Composable
private fun SegmentedTinyButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BrandBlue else Color.White)
            .border(1.dp, if (selected) BrandBlue else Color(0xFFD5DCE8), RoundedCornerShape(14.dp))
            .omniClickable(shape = RoundedCornerShape(14.dp), onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else Color(0xFF5F687B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BrainElectrodeColumn(
    title: String,
    contacts: List<String>,
    selectedContact: String,
    onContactSelected: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color(0xFF596073), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            contacts.forEach { contact ->
                val selected = contact == selectedContact
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (selected) BrandBlue else Color.White)
                        .border(1.dp, if (selected) BrandBlue else Color(0xFFD5DCE8), CircleShape)
                        .omniClickable(shape = CircleShape) { onContactSelected(contact) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(contact, color = if (selected) Color.White else Color(0xFF596073), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Canvas(Modifier.width(62.dp).height(220.dp)) {
            val segment = size.height / 4f
            repeat(4) { index ->
                val top = index * segment + 4f
                drawOval(Color(0xFFE9EDF3), topLeft = Offset(0f, top), size = Size(size.width, 24f))
                drawRect(Color(0xFFF8F9FB), topLeft = Offset(0f, top + 12f), size = Size(size.width, segment - 4f))
                drawOval(Color(0xFFDADFE8), topLeft = Offset(0f, top + segment - 18f), size = Size(size.width, 24f))
                drawOval(Color.White.copy(alpha = 0.8f), topLeft = Offset(3f, top + segment - 20f), size = Size(size.width - 6f, 18f))
            }
        }
    }
}

@Composable
private fun ImpedanceBarChart(title: String, values: List<Float>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, color = Color(0xFF3D3D3D), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left = 22f
            val bottom = size.height - 18f
            val top = 8f
            val chartHeight = bottom - top
            val barGap = (size.width - left - 12f) / values.size
            repeat(4) { line ->
                val y = top + chartHeight * line / 3f
                drawLine(Color(0xFFE8ECF2), Offset(left, y), Offset(size.width - 4f, y), strokeWidth = 1f)
            }
            values.forEachIndexed { index, value ->
                val barHeight = chartHeight * (value / 3.5f).coerceIn(0f, 1f)
                val x = left + index * barGap + barGap * 0.2f
                drawRect(BrandBlue, Offset(x, bottom - barHeight), Size(barGap * 0.5f, barHeight))
            }
            drawLine(Color(0xFF9AA1AD), Offset(left, top), Offset(left, bottom), strokeWidth = 1f)
            drawLine(Color(0xFF9AA1AD), Offset(left, bottom), Offset(size.width - 4f, bottom), strokeWidth = 1f)
        }
    }
}

@Composable
private fun BaselineTaskRow(
    label: String,
    done: Boolean,
    current: Boolean,
    number: Int = 1,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (current) {
                    Brush.horizontalGradient(listOf(Color(0xFFE8F3FF), Color(0xFFF6FAFF)))
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .border(
                1.dp,
                if (current) Color(0xFFC8DFFF) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .omniClickable(shape = RoundedCornerShape(12.dp), onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(26.dp)
                .clip(CircleShape)
                .background(if (current) BrandBlue else Color.Transparent)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (done) Color(0xFF0DA45D) else Color.White)
                .border(1.dp, if (current) BrandBlue else Color(0xFF9AA1AD), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (done) "✓" else number.toString(), color = if (done) Color.White else if (current) BrandBlue else Color(0xFF808593), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (done) Color(0xFF0DA45D) else if (current) BrandBlue else Color(0xFF808593), fontSize = 14.sp, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun baselineInstruction(activeTask: Int): String = when (activeTask) {
    0 -> "请指导患者在服药前 OFF 期平稳坐立，保持静息闭眼约60秒"
    1 -> "请指导患者在服药前 OFF 期完成站立或持续步行动作约60秒"
    2 -> "请指导患者在服药后 ON 期平稳坐立或站立约60秒"
    else -> "请指导患者在服药后 ON 期完成运动起步或持续步行动作约60秒"
}

@Composable
private fun SitStandGuide(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val seatColor = Color(0xFF8390A5)
        val bodyColor = Color(0xFF717789)
        val skin = Color(0xFFA9B4C8)
        val sitX = size.width * 0.35f
        val standX = size.width * 0.68f
        val baseY = size.height * 0.78f
        drawLine(seatColor, Offset(sitX - 34f, baseY), Offset(sitX + 18f, baseY), strokeWidth = 5f)
        drawLine(seatColor, Offset(sitX - 28f, baseY), Offset(sitX - 28f, baseY - 52f), strokeWidth = 5f)
        drawLine(seatColor, Offset(sitX - 28f, baseY - 52f), Offset(sitX + 18f, baseY - 52f), strokeWidth = 5f)
        drawCircle(skin, 10f, Offset(sitX + 16f, baseY - 90f))
        drawLine(bodyColor, Offset(sitX + 12f, baseY - 80f), Offset(sitX + 2f, baseY - 46f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawLine(bodyColor, Offset(sitX + 2f, baseY - 46f), Offset(sitX + 38f, baseY - 34f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawLine(bodyColor, Offset(sitX + 38f, baseY - 34f), Offset(sitX + 48f, baseY - 4f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawCircle(skin, 10f, Offset(standX, baseY - 108f))
        drawLine(bodyColor, Offset(standX, baseY - 96f), Offset(standX, baseY - 42f), strokeWidth = 8f, cap = StrokeCap.Round)
        drawLine(bodyColor, Offset(standX, baseY - 42f), Offset(standX - 12f, baseY - 2f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawLine(bodyColor, Offset(standX, baseY - 42f), Offset(standX + 16f, baseY - 2f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawLine(Color(0xFFE3E8F0), Offset(sitX - 50f, baseY + 4f), Offset(standX + 50f, baseY + 4f), strokeWidth = 2f)
    }
}

@Composable
private fun LfpObservationChart(sampling: Boolean, modifier: Modifier = Modifier) {
    val values = if (sampling) {
        listOf(8f, -12f, 22f, -16f, 11f, -8f, 28f, -14f, 16f, -20f, 18f, -6f, 26f, -18f, 12f, 20f, -14f, 11f, -9f, 24f, -15f, 7f, 18f, -16f, 12f, -9f, 21f, -12f, 8f, -18f, 14f, 22f, -10f, 12f, -14f, 18f)
    } else {
        listOf(3f, -6f, 14f, -10f, 8f, -4f, 22f, -8f, 7f, -13f, 10f, -3f, 18f, -15f, 5f, 12f, -9f, 7f, -4f, 16f, -8f, 3f, 13f, -11f, 8f, -5f, 17f, -7f, 4f, -12f, 9f, 14f, -6f, 6f, -9f, 11f)
    }
    Canvas(modifier) {
        val left = 42f
        val right = size.width - 16f
        val top = 10f
        val bottom = size.height - 24f
        repeat(4) { i ->
            val y = top + (bottom - top) * i / 3f
            drawLine(Color(0xFFE8ECF2), Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }
        repeat(8) { i ->
            val x = left + (right - left) * i / 7f
            drawLine(Color(0xFFE8ECF2), Offset(x, top), Offset(x, bottom), strokeWidth = 1f)
        }
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = left + (right - left) * index / (values.lastIndex.coerceAtLeast(1))
            val y = (top + (bottom - top) / 2f) - value * 2.2f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, if (sampling) BrandBlue else Color(0xFFFFA31A), style = Stroke(width = 2f))
    }
}

@Composable
private fun BetaBandChart(
    medicationFisher: List<Float> = emptyList(),
    movementFisher: List<Float> = emptyList(),
    medicationBand: String = "13-20 Hz",
    movementBand: String = "20.5-35 Hz",
    modifier: Modifier = Modifier,
) {
    val medication = medicationFisher.ifEmpty { listOf(0f, 0f) }
    val movement = movementFisher.ifEmpty { listOf(0f, 0f) }
    val maximum = maxOf(medication.maxOrNull() ?: 1f, movement.maxOrNull() ?: 1f, 0.01f)
    Column(modifier) {
        Row(Modifier.weight(1f)) {
            Column(
                Modifier.width(38.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                listOf(maximum, maximum / 2f, 0f).forEach {
                    Text("%.2f".format(it), color = Color(0xFF8A91A0), fontSize = 8.sp)
                }
            }
            DualLineBandChart(
                medication,
                movement,
                medicationBand.toBandFractions(13f, 35f),
                movementBand.toBandFractions(13f, 35f),
                Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 42.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("13", "18", "24", "30", "35 Hz").forEach {
                Text(it, color = Color(0xFF8A91A0), fontSize = 8.sp)
            }
        }
        Text("Fisher score", color = Color(0xFF8A91A0), fontSize = 8.sp)
    }
}

@Composable
private fun GammaBandChart(
    fisher: List<Float> = emptyList(),
    selectedBand: String = "75-85 Hz",
    modifier: Modifier = Modifier,
) {
    val values = fisher.ifEmpty { listOf(0f, 0f) }
    val maximum = (values.maxOrNull() ?: 1f).coerceAtLeast(0.01f)
    Column(modifier) {
        Row(Modifier.weight(1f)) {
            Column(
                Modifier.width(38.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                listOf(maximum, maximum / 2f, 0f).forEach {
                    Text("%.2f".format(it), color = Color(0xFF8A91A0), fontSize = 8.sp)
                }
            }
            SingleBandChart(
                values = values,
                color = Color(0xFF2EAD4B),
                highlight = selectedBand.toBandFractions(60f, 90f),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 42.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("60", "68", "75", "83", "90 Hz").forEach {
                Text(it, color = Color(0xFF8A91A0), fontSize = 8.sp)
            }
        }
        Text("Fisher score", color = Color(0xFF8A91A0), fontSize = 8.sp)
    }
}

@Composable
private fun DualLineBandChart(
    orange: List<Float>,
    blue: List<Float>,
    orangeBand: ClosedFloatingPointRange<Float>,
    blueBand: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val left = 46f
        val right = size.width - 20f
        val top = 12f
        val bottom = size.height - 34f
        drawRect(Color(0xFFFFF2E2), Offset(left + (right - left) * orangeBand.start, top), Size((right - left) * (orangeBand.endInclusive - orangeBand.start), bottom - top))
        drawRect(Color(0xFFEAF4FF), Offset(left + (right - left) * blueBand.start, top), Size((right - left) * (blueBand.endInclusive - blueBand.start), bottom - top))
        repeat(5) { i ->
            val y = top + (bottom - top) * i / 4f
            drawLine(Color(0xFFE8ECF2), Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }
        drawLinePath(orange, Color(0xFFFF8A1C), left, right, top, bottom, 1.4f)
        drawLinePath(blue, BrandBlue, left, right, top, bottom, 1.4f)
    }
}

@Composable
private fun SingleBandChart(
    values: List<Float>,
    color: Color,
    highlight: ClosedFloatingPointRange<Float> = 0.7f..0.88f,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val left = 46f
        val right = size.width - 20f
        val top = 12f
        val bottom = size.height - 34f
        drawRect(Color(0xFFEAF7EA), Offset(left + (right - left) * highlight.start, top), Size((right - left) * (highlight.endInclusive - highlight.start), bottom - top))
        repeat(5) { i ->
            val y = top + (bottom - top) * i / 4f
            drawLine(Color(0xFFE8ECF2), Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }
        drawLinePath(values, color, left, right, top, bottom, values.maxOrNull()?.coerceAtLeast(0.01f) ?: 1f)
    }
}

private fun String.toBandFractions(low: Float, high: Float): ClosedFloatingPointRange<Float> {
    val values = replace("Hz", "", ignoreCase = true)
        .split("-")
        .mapNotNull { it.trim().toFloatOrNull() }
    if (values.size != 2 || high <= low) return 0f..1f
    return ((values[0] - low) / (high - low)).coerceIn(0f, 1f)..
        ((values[1] - low) / (high - low)).coerceIn(0f, 1f)
}

@Composable
private fun SampleDistributionChart(
    result: ApiInitialization? = null,
    modifier: Modifier = Modifier,
) {
    val points = remember(result?.id, result?.frequency_results) {
        val rows = result?.frequency_results?.get("feature_points") as? List<*> ?: emptyList<Any>()
        rows.mapNotNull { raw ->
            val row = raw as? Map<*, *> ?: return@mapNotNull null
            FeaturePoint3D(
                state = row["state"]?.toString() ?: return@mapNotNull null,
                x = (row["medication_beta"] as? Number)?.toFloat() ?: return@mapNotNull null,
                y = (row["movement_beta"] as? Number)?.toFloat() ?: return@mapNotNull null,
                z = (row["movement_gamma"] as? Number)?.toFloat() ?: return@mapNotNull null,
            )
        }
    }
    var yaw by remember { mutableStateOf(-0.65f) }
    var pitch by remember { mutableStateOf(0.45f) }
    var zoom by remember { mutableStateOf(0.85f) }
    Column(modifier) {
        if (points.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("等待真实特征样本", color = Color(0xFF9AA1AD), fontSize = 12.sp)
            }
            return@Column
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(points.size) {
                    detectTransformGestures { _, pan, gestureZoom, rotation ->
                        yaw += rotation * 0.018f + pan.x * 0.006f
                        pitch = (pitch + pan.y * 0.006f).coerceIn(-1.2f, 1.2f)
                        zoom = (zoom * gestureZoom).coerceIn(0.55f, 1.6f)
                    }
                },
        ) {
            val minX = points.minOf { it.x }
            val maxX = points.maxOf { it.x }
            val minY = points.minOf { it.y }
            val maxY = points.maxOf { it.y }
            val minZ = points.minOf { it.z }
            val maxZ = points.maxOf { it.z }
            fun normalize(value: Float, low: Float, high: Float) =
                if (high - low < 1e-6f) 0f else ((value - low) / (high - low)) * 2f - 1f
            fun project(x: Float, y: Float, z: Float): Triple<Offset, Float, Float> {
                val cy = kotlin.math.cos(yaw)
                val sy = kotlin.math.sin(yaw)
                val cp = kotlin.math.cos(pitch)
                val sp = kotlin.math.sin(pitch)
                val rotatedX = x * cy - y * sy
                val rotatedY = (x * sy + y * cy) * cp - z * sp
                val depth = (x * sy + y * cy) * sp + z * cp
                val scale = minOf(size.width, size.height) * 0.34f * zoom
                return Triple(
                    Offset(size.width * 0.5f + rotatedX * scale, size.height * 0.53f - rotatedY * scale),
                    depth,
                    scale,
                )
            }
            val origin = project(-1f, -1f, -1f).first
            val axes = listOf(
                Triple(project(1f, -1f, -1f).first, Color(0xFFFF8A1C), "药物β"),
                Triple(project(-1f, 1f, -1f).first, BrandBlue, "运动β"),
                Triple(project(-1f, -1f, 1f).first, Color(0xFF22B34D), "运动γ"),
            )
            val paint = android.graphics.Paint().apply {
                textSize = 22f
                isAntiAlias = true
            }
            axes.forEach { (end, color, label) ->
                drawLine(color, origin, end, strokeWidth = 2.2f)
                paint.color = color.toArgb()
                drawContext.canvas.nativeCanvas.drawText(label, end.x + 4f, end.y, paint)
            }
            val stateColors = mapOf(
                "OFF-Rest" to BrandBlue,
                "OFF-Move" to Color(0xFFFF8A1C),
                "ON-Rest" to Color(0xFF22B34D),
                "ON-Move" to Color(0xFF8A3FFC),
            )
            points.map { point ->
                val projected = project(
                    normalize(point.x, minX, maxX),
                    normalize(point.y, minY, maxY),
                    normalize(point.z, minZ, maxZ),
                )
                Triple(point, projected.first, projected.second)
            }.sortedBy { it.third }.forEach { (point, offset, depth) ->
                drawCircle(
                    (stateColors[point.state] ?: Color.Gray).copy(alpha = 0.72f),
                    radius = (3.2f + (depth + 1f) * 0.8f).coerceAtLeast(2f),
                    center = offset,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                "OFF-Rest" to BrandBlue,
                "OFF-Move" to Color(0xFFFF8A1C),
                "ON-Rest" to Color(0xFF22B34D),
                "ON-Move" to Color(0xFF8A3FFC),
            ).forEach { (label, color) -> ChartLegendItem(color, label, dot = true) }
            TextButton(onClick = {
                yaw = -0.65f
                pitch = 0.45f
                zoom = 0.85f
            }) { Text("复位视角", fontSize = 9.sp) }
        }
    }
}

private data class FeaturePoint3D(
    val state: String,
    val x: Float,
    val y: Float,
    val z: Float,
)

@Composable
private fun FrequencyResultRow(
    label: String,
    recommended: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF3D3D3D), fontSize = 14.sp, modifier = Modifier.weight(1.2f))
        Text(recommended, color = BrandBlue, fontSize = 14.sp, modifier = Modifier.weight(0.8f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text("Input", fontSize = 12.sp) },
            modifier = Modifier.weight(0.9f).height(52.dp)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLinePath(
    values: List<Float>,
    color: Color,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    max: Float
) {
    val path = Path()
    values.forEachIndexed { index, value ->
        val x = left + (right - left) * index / values.lastIndex.coerceAtLeast(1)
        val y = bottom - ((value / max).coerceIn(0f, 1f) * (bottom - top))
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
}

@Composable
private fun MissingPatientPanel() {
    TabletPageTitle("请先选择患者")
    DoctorPanel {
        Text("请回到患者列表，长按某一位患者后再进入该功能。", color = Color(0xFF717789), fontSize = 16.sp)
    }
}

@Composable
private fun StepLabelClean(label: String, done: Boolean) {
    Text(label, color = if (done) Color(0xFF0DA45D) else Color(0xFF717789), fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun StepLineClean() {
    Box(Modifier.width(70.dp).height(2.dp).padding(horizontal = 12.dp).background(Color(0xFF0DA45D)))
}

@Composable
private fun TabletRealtimePage(
    repository: MockRepository,
    patient: Patient,
    compact: Boolean,
    gap: Dp,
    showMessage: (String) -> Unit,
    realRepository: RealRepository? = null,
    bleClient: BleCentralClient? = null,
    edgeInference: EdgeInferenceController? = null,
    commandDispatcher: DeviceCommandDispatcher? = null,
    serverPatientId: String? = null,
) {
    var tick by remember { mutableIntStateOf(0) }
    val bleSnapshot = bleClient?.snapshot?.collectAsState()?.value
    val liveSamples = bleClient?.recentSamples?.collectAsState()?.value ?: ShortArray(0)
    val inferenceSnapshot = edgeInference?.snapshot?.collectAsState()?.value
    val dispatchSnapshot = commandDispatcher?.snapshot?.collectAsState()?.value
    val medicationProbabilityHistory =
        remember(patient.id) { mutableStateListOf<Pair<Float?, Float?>>() }
    val movementProbabilityHistory =
        remember(patient.id) { mutableStateListOf<Pair<Float?, Float?>>() }
    var monitorState by remember(patient.id) { mutableStateOf(repository.getRealtimeMonitorState(patient.id)) }
    var displayDialog by remember { mutableStateOf(false) }
    var eventDialog by remember { mutableStateOf(false) }
    if (displayDialog) {
        PremiumAlertDialog(
            containerColor = PremiumSurfaceStrong,
            onDismissRequest = { displayDialog = false },
            title = { Text("显示设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("脑电 + 用药 + 运动", "仅脑电波形", "概率曲线").forEach { mode ->
                        OutlinedButton(
                            onClick = {
                                monitorState = repository.setRealtimeDisplayMode(patient.id, mode)
                                displayDialog = false
                                showMessage("显示模式已切换为：$mode")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(mode) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { displayDialog = false }) { Text("关闭") } }
        )
    }
    if (eventDialog) {
        PremiumAlertDialog(
            containerColor = PremiumSurfaceStrong,
            onDismissRequest = { eventDialog = false },
            title = { Text("标记事件") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("震颤加重", "步态异常", "语音任务").forEach { label ->
                        OutlinedButton(
                            onClick = {
                                monitorState = repository.recordRealtimeEvent(patient.id, label)
                                eventDialog = false
                                showMessage("已标记事件：$label")
                            }
                        ) { Text(label, fontSize = 12.sp) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { eventDialog = false }) { Text("取消") } }
        )
    }
    LaunchedEffect(monitorState.paused) {
        while (true) {
            delay(900)
            if (!monitorState.paused) tick++
        }
    }
    LaunchedEffect(
        inferenceSnapshot?.fastProbabilities,
        inferenceSnapshot?.stableProbabilities,
    ) {
        val fast = inferenceSnapshot?.fastProbabilities.orEmpty()
        val stable = inferenceSnapshot?.stableProbabilities.orEmpty()
        if (fast.isNotEmpty() && stable.isNotEmpty()) {
            val fastMedication = fast
                .filterKeys { it.startsWith("ON") }
                .values.sum().toFloat()
            val fastMovement = fast
                .filterKeys { it.endsWith("Move") }
                .values.sum().toFloat()
            val stableMedication = stable
                .filterKeys { it.startsWith("ON") }
                .values.sum().toFloat()
            val stableMovement = stable
                .filterKeys { it.endsWith("Move") }
                .values.sum().toFloat()
            medicationProbabilityHistory +=
                (fastMedication.takeUnless { inferenceSnapshot?.fastRejected == true } to
                    stableMedication.takeUnless { inferenceSnapshot?.stableRejected == true })
            movementProbabilityHistory +=
                (fastMovement.takeUnless { inferenceSnapshot?.fastRejected == true } to
                    stableMovement.takeUnless { inferenceSnapshot?.stableRejected == true })
            while (medicationProbabilityHistory.size > 120) {
                medicationProbabilityHistory.removeAt(0)
            }
            while (movementProbabilityHistory.size > 120) {
                movementProbabilityHistory.removeAt(0)
            }
        }
    }
    val signals = remember(tick, monitorState.paused) {
        repository.observeRealtimeSignals(patient.id, tick)
    }
    val channelOne = if (liveSamples.size >= 4) {
        liveSamples.asSequence().filterIndexed { index, _ -> index % 2 == 0 }.map { it.toFloat() }.toList()
    } else {
        signals.map { it.microVolt }
    }
    val channelTwo = if (liveSamples.size >= 4) {
        liveSamples.asSequence().filterIndexed { index, _ -> index % 2 == 1 }.map { it.toFloat() }.toList()
    } else {
        signals.map { -it.microVolt * 0.82f }
    }
    TabletPageTitle("实时观测")
    DoctorPanel {
        BoxWithConstraints {
            val controls: @Composable () -> Unit = {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { displayDialog = true }, modifier = Modifier.height(34.dp)) { Text("显示设置", fontSize = 12.sp) }
                    if (bleClient != null && bleSnapshot?.linkState == BleLinkState.IDLE) {
                        Button(
                            onClick = bleClient::connect,
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(17.dp),
                        ) { Text("连接模拟器", fontSize = 12.sp) }
                    }
                    OutlinedButton(
                        onClick = {
                            monitorState = repository.setRealtimePaused(patient.id, !monitorState.paused)
                            showMessage(if (monitorState.paused) "实时观测已暂停" else "实时观测已恢复")
                        },
                        modifier = Modifier.height(34.dp)
                    ) { Text(if (monitorState.paused) "恢复监测" else "暂停监测", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = {
                            monitorState = repository.toggleRealtimeRecording(patient.id)
                            showMessage(if (monitorState.recording) "开始记录片段" else "片段已保存")
                        },
                        modifier = Modifier.height(34.dp)
                    ) { Text(if (monitorState.recording) "结束记录" else "记录片段", fontSize = 12.sp) }
                    Button(onClick = { eventDialog = true }, modifier = Modifier.height(34.dp), shape = RoundedCornerShape(17.dp)) { Text("标记事件", fontSize = 12.sp) }
                }
            }
            if (maxWidth < 760.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("监测状态：${if (monitorState.paused) "已暂停" else "监测中"}  ·  ${monitorState.displayMode}", color = Color(0xFF3D3D3D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    controls()
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "监测状态：${
                            when {
                                monitorState.paused -> "已暂停"
                                bleSnapshot?.verifiedSimulator == true -> "设备在线"
                                bleSnapshot?.linkState == BleLinkState.SCANNING -> "正在扫描"
                                else -> "未连接"
                            }
                        }",
                        color = Color(0xFF3D3D3D),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("显示：${monitorState.displayMode}", color = Color(0xFF717789), fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    controls()
                }
            }
        }
        bleSnapshot?.let { snapshot ->
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE8ECF2))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                RealtimeDeviceMetric("设备", snapshot.deviceInfo?.serialNumber ?: "等待连接")
                RealtimeDeviceMetric("固件 / 协议", "${snapshot.deviceInfo?.firmwareVersion ?: "—"} / v${snapshot.deviceInfo?.protocolVersion ?: "—"}")
                RealtimeDeviceMetric("电量", snapshot.batteryPercent?.let { "$it%" } ?: "—")
                RealtimeDeviceMetric("链路", "MTU ${snapshot.negotiatedMtu} · 帧 ${snapshot.receivedFrames}")
                RealtimeDeviceMetric("质量", "丢包 ${snapshot.lostFrames} · CRC ${snapshot.crcErrors}")
                RealtimeDeviceMetric(
                    "阻抗",
                    snapshot.impedance?.readings?.joinToString(" / ") { "%.2fkΩ".format(it.kiloOhms) } ?: "—",
                )
                RealtimeDeviceMetric(
                    "当前刺激",
                    snapshot.parameters?.let { "%.2fmA · %dHz · %dμs".format(it.currentMa, it.frequencyHz, it.pulseWidthUs) } ?: "—",
                )
            }
            snapshot.lastError?.let {
                Text(it, color = SoftRed, fontSize = 11.sp)
            }
        }
        inferenceSnapshot?.topState?.let { topState ->
            Spacer(Modifier.height(8.dp))
            Text(
                "模拟场景 ${bleSnapshot?.simulatedState?.label ?: "—"}（仅用于生成LFP，不参与概率计算）",
                color = MedicalGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "端侧推断 $topState · 置信度 ${"%.1f".format((inferenceSnapshot.confidence ?: 0.0) * 100)}% · " +
                    "模型 ${inferenceSnapshot.modelVersionId ?: "内置预热模型"}",
                color = BrandBlue,
                fontSize = 11.sp,
            )
        }
        dispatchSnapshot?.let {
            Text(
                "参数闭环：${it.status} · 成功 ${it.successfulAcks} / 失败 ${it.failedAcks}",
                color = Color(0xFF717789),
                fontSize = 10.sp,
            )
        }
        if (monitorState.eventMarkers.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("最近事件：${monitorState.eventMarkers.joinToString("、")}  |  已保存片段 ${monitorState.recordedSegments} 段", color = Color(0xFF717789), fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(gap))
    if (monitorState.displayMode != "概率曲线") {
        RealtimeChartPanel("左侧 LFP（C6-C2）", channelOne, BrandBlue, -120f, 120f)
        Spacer(Modifier.height(gap))
        RealtimeChartPanel("右侧 LFP（C7-C3）", channelTwo, Color(0xFFFF8A1C), -120f, 120f)
        Spacer(Modifier.height(gap))
        RealtimeSpectrumPanel(liveSamples)
        Spacer(Modifier.height(gap))
    }
    if (monitorState.displayMode != "仅脑电波形") {
        RealtimeProbabilityPanel(
            title = "药物效果",
            fastValues = medicationProbabilityHistory.map { it.first },
            stableValues = medicationProbabilityHistory.map { it.second },
            accentColor = Color(0xFF08A522),
            warmup = "${inferenceSnapshot?.fastWarmedWindows ?: 0}/5 · ${inferenceSnapshot?.stableWarmedWindows ?: 0}/30",
        )
        Spacer(Modifier.height(gap))
        RealtimeProbabilityPanel(
            title = "运动强度",
            fastValues = movementProbabilityHistory.map { it.first },
            stableValues = movementProbabilityHistory.map { it.second },
            accentColor = Color(0xFF7A26FF),
            warmup = "${inferenceSnapshot?.fastWarmedWindows ?: 0}/5 · ${inferenceSnapshot?.stableWarmedWindows ?: 0}/30",
        )
    }
}

@Composable
private fun RealtimeDeviceMetric(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF8A91A0), fontSize = 9.sp)
        Text(value, color = Color(0xFF3D3D3D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RealtimeSpectrumPanel(interleaved: ShortArray) {
    val spectrum = remember(interleaved) { calculateLfpSpectrum(interleaved) }
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LFP 功率谱", color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                if (interleaved.size >= 512) "256 Hz · 最近1秒 · 2–100 Hz" else "等待完整1秒数据",
                color = Color(0xFF9AA1AD),
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        SingleBandChart(
            values = spectrum.ifEmpty { listOf(0f, 0f) },
            color = Color(0xFF2EAD4B),
            modifier = Modifier.fillMaxWidth().height(160.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(start = 46.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("2", "20", "40", "60", "80", "100 Hz").forEach {
                Text(it, color = Color(0xFF9AA1AD), fontSize = 9.sp)
            }
        }
    }
}

private fun calculateLfpSpectrum(interleaved: ShortArray): List<Float> {
    val sampleCount = minOf(interleaved.size / 2, 256)
    if (sampleCount < 256) return emptyList()
    val startSample = interleaved.size / 2 - sampleCount
    return (2..100 step 2).map { frequency ->
        var real = 0.0
        var imaginary = 0.0
        for (index in 0 until sampleCount) {
            val sample = interleaved[(startSample + index) * 2].toDouble()
            val window = 0.5 - 0.5 * kotlin.math.cos(2.0 * Math.PI * index / (sampleCount - 1))
            val angle = 2.0 * Math.PI * frequency * index / 256.0
            real += sample * window * kotlin.math.cos(angle)
            imaginary -= sample * window * kotlin.math.sin(angle)
        }
        ((real * real + imaginary * imaginary) / (sampleCount * sampleCount)).toFloat()
    }
}

@Composable
private fun RealtimeProbabilityPanel(
    title: String,
    fastValues: List<Float?>,
    stableValues: List<Float?>,
    accentColor: Color,
    warmup: String,
) {
    DoctorPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(8.dp).clip(CircleShape).background(accentColor))
            Spacer(Modifier.width(5.dp))
            Text(
                "快速 ${fastValues.lastOrNull()?.let { "%.0f%%".format(it * 100) } ?: "预热/拒识"}",
                color = Color(0xFF5F687B),
                fontSize = 11.sp,
            )
            Spacer(Modifier.width(14.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.75f)))
            Spacer(Modifier.width(5.dp))
            Text(
                "稳态 ${stableValues.lastOrNull()?.let { "%.0f%%".format(it * 100) } ?: "预热/拒识"} · $warmup",
                color = Color(0xFF5F687B),
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(190.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.width(48.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text("100%", color = Color(0xFF9AA1AD), fontSize = 9.sp)
                Text("50%", color = Color(0xFF9AA1AD), fontSize = 9.sp)
                Text("0%", color = Color(0xFF9AA1AD), fontSize = 9.sp)
            }
            Spacer(Modifier.width(7.dp))
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val left = 0f
                val right = size.width
                val top = 4f
                val bottom = size.height - 5f
                repeat(5) { index ->
                    val y = top + (bottom - top) * index / 4f
                    drawLine(Color(0xFFE8ECF2), Offset(left, y), Offset(right, y), strokeWidth = 1f)
                }
                fun drawNullable(values: List<Float?>, color: Color, width: Float) {
                    if (values.size < 2) return
                    val step = (right - left) / (values.size - 1).coerceAtLeast(1)
                    var previous: Offset? = null
                    values.forEachIndexed { index, value ->
                        if (value == null) {
                            previous = null
                        } else {
                            val point = Offset(
                                left + index * step,
                                bottom - value.coerceIn(0f, 1f) * (bottom - top),
                            )
                            previous?.let { drawLine(color, it, point, strokeWidth = width) }
                            previous = point
                        }
                    }
                }
                drawNullable(stableValues, BrandBlue.copy(alpha = 0.70f), 2.2f)
                drawNullable(fastValues, accentColor, 3.2f)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 55.dp, top = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("-120s", "-100", "-80", "-60", "-40", "-20", "现在").forEach {
                Text(it, color = Color(0xFF9AA1AD), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun RealtimeChartPanel(title: String, values: List<Float>, color: Color, min: Float, max: Float) {
    DoctorPanel {
        Row {
            Text(title, color = Color(0xFF3D3D3D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    min < 0f -> "实时缓冲 ${values.size} 点"
                    title.contains("模拟器真值") -> "电脑模拟器实时控制值"
                    else -> "端侧模型概率"
                },
                color = Color(0xFF9AA1AD),
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        AxisLineChart(
            values = values,
            color = color,
            min = min,
            max = max,
            unit = if (min < 0f) "μV" else "概率",
            modifier = Modifier.fillMaxWidth().height(190.dp)
        )
    }
}

@Composable
private fun AxisLineChart(
    values: List<Float>,
    color: Color,
    min: Float,
    max: Float,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.width(44.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("${formatAxis(max)}$unit", color = Color(0xFF9AA1AD), fontSize = 9.sp)
                Text("${formatAxis((max + min) / 2f)}$unit", color = Color(0xFF9AA1AD), fontSize = 9.sp)
                Text("${formatAxis(min)}$unit", color = Color(0xFF9AA1AD), fontSize = 9.sp)
            }
            Spacer(Modifier.width(6.dp))
            LineChart(values, color, Modifier.weight(1f).fillMaxHeight(), min, max)
        }
        Row(Modifier.fillMaxWidth().padding(start = 50.dp, top = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "1", "2", "3", "4", "5", "6", "7s").forEach {
                Text(it, color = Color(0xFF9AA1AD), fontSize = 9.sp)
            }
        }
    }
}

private fun formatAxis(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else String.format("%.1f", value)

@Composable
private fun DoctorPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PremiumBorder.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                BrandBlue.copy(alpha = 0.88f),
                                Color(0xFF43C7EE).copy(alpha = 0.7f),
                                Color(0xFF8174E8).copy(alpha = 0.54f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                content = content
            )
        }
    }
}

@Composable
private fun PatientAvatar(size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFFDCEBFF)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF1069E3), modifier = Modifier.size(size * 0.66f))
    }
}

private fun DoctorPatientRecord.toPatient(): Patient =
    Patient(id = id, name = name, gender = gender, age = age, number = number)

private fun DoctorScreen.requiresSelectedPatient(): Boolean =
    this == DoctorScreen.PatientInfo ||
        this == DoctorScreen.ParameterAdjustment ||
        this == DoctorScreen.RealtimeMonitor

private fun PatientListGroup.label(): String = when (this) {
    PatientListGroup.PendingInitialization -> "待初始化"
    PatientListGroup.Focus -> "重点关注"
    PatientListGroup.Routine -> "常规监控"
}

private fun PatientSortField.label(): String = when (this) {
    PatientSortField.Name -> "姓名"
    PatientSortField.Number -> "编号"
    PatientSortField.ImplantDate -> "植入日期"
    PatientSortField.Age -> "年龄"
}

@Composable
private fun DoctorSidebar(
    patient: Patient,
    selected: DoctorScreen,
    onSelected: (DoctorScreen) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(PanelBg)
            .padding(26.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.mg_logo_mark_transparent), contentDescription = null, modifier = Modifier.size(66.dp))
            Spacer(Modifier.width(10.dp))
            Text("Ominidapt PD", color = BrandBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(26.dp))
        AppCard {
            Text("当前选中患者", color = MutedText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFDCEBFF)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(patient.name, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${patient.gender}  ${patient.age}岁", color = MutedText, fontSize = 13.sp)
                    Text("患者编号：${patient.number}", color = MutedText, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        DoctorMenuItem("患者列表", Icons.Filled.Group, DoctorScreen.PatientList, selected, onSelected)
        DoctorMenuItem("数据导出", Icons.Filled.Download, DoctorScreen.Export, selected, onSelected)
        DoctorMenuItem("个人设置", Icons.Filled.Settings, DoctorScreen.Settings, selected, onSelected)
        DoctorMenuItem("患者信息", Icons.Filled.AccountCircle, DoctorScreen.PatientInfo, selected, onSelected)
        DoctorMenuItem("初始化与参数调整", Icons.Filled.Tune, DoctorScreen.ParameterAdjustment, selected, onSelected)
        DoctorMenuItem("实时观测", Icons.AutoMirrored.Filled.ShowChart, DoctorScreen.RealtimeMonitor, selected, onSelected)
        Spacer(Modifier.weight(1f))
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(MedicalGreen))
                Spacer(Modifier.width(10.dp))
                Text("设备已连接", color = MutedText)
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Border)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = MutedText)
                Spacer(Modifier.width(10.dp))
                Text("系统设置", color = MutedText)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("返回登录")
            }
        }
    }
}

@Composable
private fun DoctorMenuItem(
    label: String,
    icon: ImageVector,
    screen: DoctorScreen,
    selected: DoctorScreen,
    onSelected: (DoctorScreen) -> Unit
) {
    val active = selected == screen
    val bg = if (active) BrandBlue else Color.Transparent
    val fg = if (active) Color.White else MutedText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .omniClickable(shape = RoundedCornerShape(8.dp)) { onSelected(screen) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = fg, fontSize = 17.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold)
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun CompactDoctorHeader(
    selected: DoctorScreen,
    onSelected: (DoctorScreen) -> Unit,
    onLogout: () -> Unit
) {
    Column(Modifier.background(PanelBg).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.mg_logo_mark_transparent), contentDescription = null, modifier = Modifier.size(52.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ominidapt PD", color = BrandBlue, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onLogout) { Text("退出") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                DoctorScreen.ParameterAdjustment to "参数调整",
                DoctorScreen.RealtimeMonitor to "实时观测",
                DoctorScreen.PatientInfo to "患者信息",
                DoctorScreen.PatientList to "患者列表"
            ).forEach { (screen, label) ->
                Button(
                    onClick = { onSelected(screen) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected == screen) BrandBlue else Color(0xFFE9EEF7),
                        contentColor = if (selected == screen) Color.White else Ink
                    )
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun DoctorMainContent(
    repository: MockRepository,
    patient: Patient,
    report: PatientReport,
    selected: DoctorScreen,
    onParametersChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        when (selected) {
            DoctorScreen.ParameterAdjustment -> DoctorParameterScreen(repository, patient, report, onParametersChanged)
            DoctorScreen.RealtimeMonitor -> DoctorRealtimeScreen(repository, patient)
            DoctorScreen.PatientInfo -> PlaceholderPanel("患者信息", "${patient.name}，${patient.gender}，${patient.age}岁\n近期震颤、僵硬和构音障碍均已纳入报告。")
            DoctorScreen.PatientList -> PlaceholderPanel("患者列表", "当前演示版内置 1 位患者：${patient.name}。")
            DoctorScreen.Export -> PlaceholderPanel("数据导出", "首版展示导出入口，后续接入真实文件导出。")
            DoctorScreen.Settings -> PlaceholderPanel("个人设置", "设备连接、账号安全与通知偏好将在后续版本接入。")
        }
    }
}

@Composable
private fun DoctorParameterScreen(
    repository: MockRepository,
    patient: Patient,
    report: PatientReport,
    onParametersChanged: () -> Unit
) {
    val suggestion = remember { repository.getOptimizationSuggestion(patient.id) }
    var confirmed by remember { mutableStateOf(false) }

    Text("初始化与参数调整", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    ProgressSteps()
    Spacer(Modifier.height(14.dp))
    AppCard {
        SectionTitle("异常报告概览", Icons.Filled.Warning)
        AlertTimeline(report.alerts)
        Spacer(Modifier.height(6.dp))
        Text("该时段内震颤次数较多，请注意", color = SoftRed, fontSize = 12.sp)
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard(modifier = Modifier.weight(1.5f)) {
            SectionTitle("优化任务设置", Icons.Filled.Tune)
            TaskRows()
        }
        AppCard(modifier = Modifier.weight(1f)) {
            Text("可调参数范围", color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            ParamRange("电流强度：", "1.0", "4.0", "mA")
            ParamRange("频率：", "80", "180", "Hz")
            ParamRange("脉宽：", "40", "120", "μs")
            ParamRange("占空比：", "20", "80", "%")
        }
        AppCard(modifier = Modifier.weight(0.65f)) {
            Text("调参轮数", color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(42.dp).border(1.dp, Border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("7 轮", color = Ink)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("保存设置") }
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard(modifier = Modifier.weight(1.4f)) {
            SectionTitle("参数优化可视化：", Icons.AutoMirrored.Filled.ShowChart)
            LineChart(
                values = suggestion.curve,
                color = BrandBlue,
                modifier = Modifier.fillMaxWidth().height(190.dp),
                minValue = 0f,
                maxValue = 100f
            )
        }
        AppCard(modifier = Modifier.weight(0.85f)) {
            SectionTitle("参数下发确认", Icons.AutoMirrored.Filled.Send)
            ParamLine("电流强度：", "${suggestion.suggestedParameters.currentMa} mA")
            ParamLine("频率：", "${suggestion.suggestedParameters.frequencyHz} Hz")
            ParamLine("脉宽：", "${suggestion.suggestedParameters.pulseWidthUs} μs")
            ParamLine("占空比：", "${suggestion.suggestedParameters.dutyCycle} %")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text("恢复推荐值")
                }
                Button(
                    onClick = {
                        repository.confirmParameterDownload(patient.id, suggestion.suggestedParameters)
                        confirmed = true
                        onParametersChanged()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确认下发")
                }
            }
            if (confirmed) {
                Spacer(Modifier.height(10.dp))
                Text("参数已下发，并同步到患者数据报告", color = MedicalGreen, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ProgressSteps() {
    AppCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StepLabel("电极信息配置")
            StepConnector()
            StepLabel("基线状态检测")
            StepConnector()
            StepLabel("个性化频段提取")
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = {}, enabled = false) {
                Text("重新进行初始化")
            }
        }
    }
}

@Composable
private fun StepLabel(text: String) {
    Text(text, color = MedicalGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
}

@Composable
private fun StepConnector() {
    Box(Modifier.width(58.dp).height(2.dp).background(MedicalGreen))
}

@Composable
private fun TaskRows() {
    val tasks = listOf(
        "当前震颤是否改善？" to "15%",
        "当前僵硬是否改善？" to "20%",
        "当前效率是否更顺畅？" to "15%",
        "当前动作是否更顺畅？" to "20%",
        "是否出现不适或副作用？" to "15%",
        "与上一组参数相比，当前方案是否更好？" to "15%"
    )
    Column {
        Row(Modifier.background(Color(0xFFF6F8FC))) {
            TableCell("任务量表编辑", 220.dp, true)
            TableCell("得分权重", 78.dp, true)
        }
        tasks.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                TableCell("${index + 1}  ${item.first}", 220.dp)
                TableCell(item.second, 78.dp)
            }
        }
    }
}

@Composable
private fun OptimizationTaskRows(
    settings: ParameterOptimizationSettings,
    onSettingsChange: (ParameterOptimizationSettings) -> Unit
) {
    val rows = listOf(
        Triple("震颤改善", "当前震颤是否改善", settings.tremorWeight),
        Triple("僵硬改善", "当前僵硬是否改善", settings.rigidityWeight),
        Triple("吐词清晰", "当前吐词是否更清晰", settings.speechWeight),
        Triple("动作流畅", "当前动作是否更顺畅", settings.movementWeight),
        Triple("副作用", "是否出现不适或副作用", settings.sideEffectWeight),
        Triple("方案比较", "是否优于上一组参数", settings.comparisonWeight)
    )
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(26.dp).background(Color(0xFFF6F8FC)), verticalAlignment = Alignment.CenterVertically) {
            Text("优化目标", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.82f).padding(start = 6.dp))
            Text("任务量表", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
            Text("权重", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.78f))
        }
        rows.forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().height(25.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(row.first, color = Ink, fontSize = 10.sp, modifier = Modifier.weight(0.82f).padding(start = 6.dp), maxLines = 1)
                Text(row.second, color = Color(0xFF717789), fontSize = 9.sp, modifier = Modifier.weight(1.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                WeightStepper(
                    value = row.third,
                    modifier = Modifier.weight(0.78f),
                    onChange = { next ->
                        onSettingsChange(
                            when (index) {
                                0 -> settings.copy(tremorWeight = next)
                                1 -> settings.copy(rigidityWeight = next)
                                2 -> settings.copy(speechWeight = next)
                                3 -> settings.copy(movementWeight = next)
                                4 -> settings.copy(sideEffectWeight = next)
                                else -> settings.copy(comparisonWeight = next)
                            }
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        OutlinedButton(
            onClick = {
                onSettingsChange(
                    settings.copy(
                        tremorWeight = 0.15f,
                        rigidityWeight = 0.20f,
                        speechWeight = 0.15f,
                        movementWeight = 0.20f,
                        sideEffectWeight = 0.15f,
                        comparisonWeight = 0.15f
                    )
                )
            },
            modifier = Modifier.height(28.dp).align(Alignment.End),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("重置权重", fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun WeightStepper(value: Float, modifier: Modifier = Modifier, onChange: (Float) -> Unit) {
    Row(
        modifier = modifier.height(22.dp).border(1.dp, Border, RoundedCornerShape(11.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(24.dp)
                .fillMaxHeight()
                .omniClickable(shape = RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp)) {
                    onChange((value - 0.05f).coerceIn(0.05f, 0.9f))
                },
            contentAlignment = Alignment.Center
        ) { Text("-", color = BrandBlue, fontSize = 10.sp) }
        Text("${(value * 100).toInt()}%", color = Ink, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .width(24.dp)
                .fillMaxHeight()
                .omniClickable(shape = RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp)) {
                    onChange((value + 0.05f).coerceIn(0.05f, 0.9f))
                },
            contentAlignment = Alignment.Center
        ) { Text("+", color = BrandBlue, fontSize = 10.sp) }
    }
}

@Composable
private fun EditableRangeRow(label: String, min: String, max: String, unit: String, onChange: (String, String) -> Unit) {
    var minValue by remember(label, min) { mutableStateOf(min) }
    var maxValue by remember(label, max) { mutableStateOf(max) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Text(label, color = MutedText, fontSize = 12.sp, modifier = Modifier.width(68.dp), maxLines = 1)
        CompactInlineInput(minValue, Modifier.weight(1f)) {
            minValue = it
            onChange(it, maxValue)
        }
        Text(" - ", color = MutedText, fontSize = 12.sp)
        CompactInlineInput(maxValue, Modifier.weight(1f)) {
            maxValue = it
            onChange(minValue, it)
        }
        Text(unit, color = MutedText, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun CompactInlineInput(value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Border, RoundedCornerShape(6.dp))
            .background(Color.White)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Ink,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(BrandBlue),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CompactCardAction(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (filled) BrandBlue else PremiumSurfaceStrong)
            .border(1.dp, if (filled) BrandBlue else Border, RoundedCornerShape(20.dp))
            .omniClickable(
                shape = RoundedCornerShape(20.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (filled) Color.White else Color(0xFF717789),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun OptimizationRoundsControl(rounds: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).border(1.dp, Color(0xFFE1E6EE), RoundedCornerShape(23.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(38.dp)
                .fillMaxHeight()
                .omniClickable(shape = RoundedCornerShape(topStart = 23.dp, bottomStart = 23.dp), onClick = onDecrease),
            contentAlignment = Alignment.Center
        ) {
            Text("-", color = BrandBlue, fontSize = 16.sp)
        }
        Text("$rounds 轮", color = Color(0xFF3D3D3D), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Box(
            Modifier
                .width(38.dp)
                .fillMaxHeight()
                .omniClickable(shape = RoundedCornerShape(topEnd = 23.dp, bottomEnd = 23.dp), onClick = onIncrease),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = BrandBlue, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ParamRange(label: String, min: String, max: String, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
        Text(label, color = MutedText, fontSize = 13.sp, modifier = Modifier.width(80.dp))
        SmallBox(min)
        Text(" - ", color = MutedText)
        SmallBox(max)
        Text(" $unit", color = MutedText, fontSize = 13.sp)
    }
}

@Composable
private fun SmallBox(text: String) {
    Box(
        modifier = Modifier.width(52.dp).height(28.dp).border(1.dp, Border, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Ink, fontSize = 12.sp)
    }
}

@Composable
private fun ParamLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedText)
        Text(value, color = Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DoctorRealtimeScreen(repository: MockRepository, patient: Patient) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            tick++
        }
    }
    val signals = remember(tick) { repository.observeRealtimeSignals(patient.id, tick) }

    Text("实时观测", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(14.dp))
    MonitorPanel("脑电波形", signals, ChartMode.MicroVolt, BrandBlue)
    MonitorPanel("药物失效-静息概率", signals, ChartMode.StaticProbability, Color(0xFF1CB44C))
    MonitorPanel("药物失效-运动概率", signals, ChartMode.MotionProbability, Color(0xFFFF7A22))
    MonitorPanel("药物生效-静息概率", signals, ChartMode.StaticProbability, Color(0xFF8A48FF))
}

private enum class ChartMode {
    MicroVolt,
    StaticProbability,
    MotionProbability
}

@Composable
private fun MonitorPanel(title: String, signals: List<BrainSignalPoint>, mode: ChartMode, color: Color) {
    AppCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val values = signals.map {
            when (mode) {
                ChartMode.MicroVolt -> it.microVolt
                ChartMode.StaticProbability -> it.staticProbability
                ChartMode.MotionProbability -> it.motionProbability
            }
        }
        val min = if (mode == ChartMode.MicroVolt) -50f else 0f
        val max = if (mode == ChartMode.MicroVolt) 50f else 1f
        LineChart(
            values = values,
            color = color,
            modifier = Modifier.fillMaxWidth().height(136.dp),
            minValue = min,
            maxValue = max
        )
    }
}

@Composable
private fun PlaceholderPanel(title: String, body: String) {
    Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(14.dp))
    AppCard {
        Text(body, color = MutedText, lineHeight = 24.sp)
    }
}

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PremiumBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .border(2.dp, BrandBlue, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun TableCell(text: String, width: Dp, bold: Boolean = false) {
    Box(
        modifier = Modifier
            .width(width)
            .heightIn(min = 38.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (bold) Ink else MutedText,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LineChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    minValue: Float,
    maxValue: Float
) {
    var chartVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chartVisible = true }
    val revealProgress by animateFloatAsState(
        targetValue = if (chartVisible) 1f else 0f,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "chartReveal"
    )
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        val w = size.width
        val h = size.height
        repeat(4) { index ->
            val y = h * (index + 1) / 5f
            drawLine(Color(0xFFE3E8F0), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        repeat(5) { index ->
            val x = w * (index + 1) / 6f
            drawLine(Color(0xFFE3E8F0), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        if (values.size > 1) {
            val path = Path()
            val visibleLastIndex = (values.lastIndex * revealProgress)
                .toInt()
                .coerceIn(1, values.lastIndex)
            values.take(visibleLastIndex + 1).forEachIndexed { index, raw ->
                val x = index * w / (values.lastIndex)
                val normalized = ((raw - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
                val y = h - normalized * h
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
            val headValue = values[visibleLastIndex]
            val headX = visibleLastIndex * w / values.lastIndex
            val headY = h - ((headValue - minValue) / (maxValue - minValue))
                .coerceIn(0f, 1f) * h
            drawCircle(color.copy(alpha = 0.16f), radius = 10f, center = Offset(headX, headY))
            drawCircle(color, radius = 3.5f, center = Offset(headX, headY))
        }
    }
}

@Preview(showBackground = true, widthDp = 430, heightDp = 920)
@Composable
private fun LoginPreview() {
    OminidaptTheme {
        LoginScreen(onLogin = {})
    }
}

@Preview(
    name = "Patient premium 360dp",
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun PatientPremiumCompactPreview() {
    OminidaptTheme {
        AmbientBackdrop(AmbientStyle.Patient, Modifier.fillMaxSize()) {
            PatientShell(repository = MockRepository(), onLogout = {})
        }
    }
}

@Preview(
    name = "Patient premium large text",
    showBackground = true,
    widthDp = 430,
    heightDp = 920,
    fontScale = 1.3f
)
@Composable
private fun PatientPremiumLargeTextPreview() {
    OminidaptTheme {
        AmbientBackdrop(AmbientStyle.Patient, Modifier.fillMaxSize()) {
            PatientShell(repository = MockRepository(), onLogout = {})
        }
    }
}

@Preview(
    name = "Doctor premium tablet",
    showBackground = true,
    widthDp = 1192,
    heightDp = 834
)
@Composable
private fun DoctorPremiumTabletPreview() {
    OminidaptTheme {
        AmbientBackdrop(AmbientStyle.Doctor, Modifier.fillMaxSize()) {
            DoctorShell(repository = MockRepository(), onLogout = {})
        }
    }
}
