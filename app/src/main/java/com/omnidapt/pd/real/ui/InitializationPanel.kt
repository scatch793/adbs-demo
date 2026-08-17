package com.omnidapt.pd.real.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.omnidapt.pd.real.initialization.InitializationController
import com.omnidapt.pd.real.network.ApiInitialization

@Composable
fun InitializationPanel(
    controller: InitializationController,
    patientId: String,
    onStart: (String) -> Unit,
    onApprove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsState()
    Card(modifier.fillMaxWidth().heightIn(max = 320.dp)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("四状态初始化与个体化频段", style = MaterialTheme.typography.titleLarge)
            Text(
                "采集顺序：药前静息 → 药前运动 → 药后静息 → 药后运动。所有场景仅由电脑模拟器产生。",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onStart("demo") },
                    enabled = !state.running,
                ) { Text("演示模式 30秒/状态") }
                OutlinedButton(
                    onClick = { onStart("research") },
                    enabled = !state.running,
                ) { Text("科研模式 3分钟/状态") }
            }
            Text("${state.phase}${state.stateLabel?.let { " · $it" }.orEmpty()}")
            if (state.remainingSeconds > 0) {
                Text("剩余 ${state.remainingSeconds} 秒")
            }
            if (state.targetSamples > 0) {
                val progress = state.collectedSamples.toFloat() / state.targetSamples
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${state.collectedSamples}/${state.targetSamples} 样本/通道")
            } else if (state.running) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.result?.let { result ->
                InitializationResult(result, onApprove)
            }
            Text(
                "患者ID：$patientId · 短时演示模型与科研模型均需医生审核后才能成为当前模型。",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun InitializationResult(result: ApiInitialization, onApprove: () -> Unit) {
    Text("任务 ${result.id.take(8)} · ${result.mode} · ${result.status}")
    result.segments.sortedBy { it.order_index }.forEach { segment ->
        Text(
            "${if (segment.accepted) "✓" else "✗"} ${segment.state_label} · " +
                "${segment.sample_count}样本 · 丢包${segment.packet_loss_count} · " +
                "CRC${segment.crc_error_count}",
            color = if (segment.accepted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
    val bands = result.frequency_results["bands"] as? Map<*, *>
    if (bands != null) {
        Text("个体化频段", style = MaterialTheme.typography.titleMedium)
        listOf(
            "medication_beta" to "药物敏感β",
            "movement_beta" to "运动敏感β",
            "movement_gamma" to "运动敏感γ",
        ).forEach { (key, label) ->
            val range = bands[key] as? List<*>
            if (range != null && range.size >= 2) {
                Text(
                    "$label：${formatNumber(range[0])}–${formatNumber(range[1])} Hz",
                )
            }
        }
        FisherPlot(result.frequency_results)
    }
    val metrics = result.quality_summary["metrics"] as? Map<*, *>
    if (metrics != null) {
        Text(
            "验证准确率 ${formatPercent(metrics["accuracy"])} · " +
                "Macro-F1 ${formatPercent(metrics["macro_f1"])} · " +
                "验证窗 ${formatNumber(metrics["validation_windows"])}",
        )
    }
    if (result.status == "review") {
        Button(onClick = onApprove) { Text("审核并启用该模型") }
    }
    if (result.status == "approved") {
        Text(
            "模型已审核启用：${result.model_version_id}",
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun FisherPlot(values: Map<String, Any>) {
    val medication = (values["fisher_medication_beta"] as? List<*>)
        ?.mapNotNull { (it as? Number)?.toFloat() }.orEmpty()
    val movement = (values["fisher_movement_beta"] as? List<*>)
        ?.mapNotNull { (it as? Number)?.toFloat() }.orEmpty()
    if (medication.size < 2 || movement.size != medication.size) return
    val firstColor = MaterialTheme.colorScheme.primary
    val secondColor = MaterialTheme.colorScheme.tertiary
    val peak = maxOf(
        medication.maxOrNull() ?: 1f,
        movement.maxOrNull() ?: 1f,
        1e-6f,
    )
    Text("β段 Fisher 曲线（蓝：药物，橙：运动）", style = MaterialTheme.typography.bodySmall)
    Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        for (index in 1 until medication.size) {
            val x0 = (index - 1f) / (medication.size - 1f) * size.width
            val x1 = index.toFloat() / (medication.size - 1f) * size.width
            drawLine(
                firstColor,
                Offset(x0, size.height - medication[index - 1] / peak * size.height),
                Offset(x1, size.height - medication[index] / peak * size.height),
                strokeWidth = 2f,
            )
            drawLine(
                secondColor,
                Offset(x0, size.height - movement[index - 1] / peak * size.height),
                Offset(x1, size.height - movement[index] / peak * size.height),
                strokeWidth = 2f,
            )
        }
    }
}

private fun formatNumber(value: Any?): String =
    (value as? Number)?.toDouble()?.let { "%.1f".format(it) } ?: "—"

private fun formatPercent(value: Any?): String =
    (value as? Number)?.toDouble()?.let { "%.1f%%".format(it * 100) } ?: "—"
