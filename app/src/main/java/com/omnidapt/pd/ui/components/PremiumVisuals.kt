package com.omnidapt.pd.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.omnidapt.pd.ui.motion.OmniMotion
import com.omnidapt.pd.ui.motion.omniPressEffect
import com.omnidapt.pd.ui.theme.AuraBlue
import com.omnidapt.pd.ui.theme.AuraCyan
import com.omnidapt.pd.ui.theme.AuraIndigo
import com.omnidapt.pd.ui.theme.PremiumBorder
import com.omnidapt.pd.ui.theme.PremiumSurface

enum class AmbientStyle {
    Login,
    Patient,
    Doctor
}

@Composable
fun AmbientBackdrop(
    style: AmbientStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseColors = when (style) {
        AmbientStyle.Login -> listOf(Color(0xFFF8FBFF), Color(0xFFF0F6FF), Color(0xFFF8FAFE))
        AmbientStyle.Patient -> listOf(Color(0xFFFBFDFF), Color(0xFFF2F8FF), Color(0xFFF8FAFE))
        AmbientStyle.Doctor -> listOf(Color(0xFFF8FAFE), Color(0xFFF0F5FC), Color(0xFFF7F9FD))
    }
    val opacity = when (style) {
        AmbientStyle.Login -> 0.13f
        AmbientStyle.Patient -> 0.11f
        AmbientStyle.Doctor -> 0.075f
    }

    Box(
        modifier = modifier.background(Brush.verticalGradient(baseColors))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .semantics { hideFromAccessibility() }
        ) {
            GlowOrb(
                color = AuraBlue,
                opacity = opacity,
                size = 430f,
                alignment = Alignment.TopStart,
                xRange = -90f to 140f,
                yRange = -120f to 90f,
                durationMillis = OmniMotion.AmbientMediumMillis
            )
            GlowOrb(
                color = AuraCyan,
                opacity = opacity * 0.82f,
                size = 360f,
                alignment = Alignment.CenterEnd,
                xRange = 100f to -110f,
                yRange = -120f to 150f,
                durationMillis = OmniMotion.AmbientSlowMillis
            )
            GlowOrb(
                color = AuraIndigo,
                opacity = opacity * 0.68f,
                size = 400f,
                alignment = Alignment.BottomStart,
                xRange = -100f to 180f,
                yRange = 120f to -80f,
                durationMillis = OmniMotion.AmbientFastMillis
            )
        }
        content()
    }
}

@Composable
private fun BoxScope.GlowOrb(
    color: Color,
    opacity: Float,
    size: Float,
    alignment: Alignment,
    xRange: Pair<Float, Float>,
    yRange: Pair<Float, Float>,
    durationMillis: Int
) {
    val transition = rememberInfiniteTransition(label = "ambientGlow")
    val x by transition.animateFloat(
        initialValue = xRange.first,
        targetValue = xRange.second,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientGlowX"
    )
    val y by transition.animateFloat(
        initialValue = yRange.first,
        targetValue = yRange.second,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis + 3_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientGlowY"
    )
    Canvas(
        modifier = Modifier
            .size(size.dp)
            .align(alignment)
            .graphicsLayer {
                translationX = x.dp.toPx()
                translationY = y.dp.toPx()
            }
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = opacity),
                    color.copy(alpha = opacity * 0.34f),
                    Color.Transparent
                ),
                radius = this.size.minDimension / 2f
            ),
            radius = this.size.minDimension / 2f
        )
    }
}

@Composable
fun OmniCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, PremiumBorder.copy(alpha = 0.88f))
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun OmniDoctorPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    OmniCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun OmniButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    MaterialButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 40.dp)
            .omniPressEffect(source, shape, enabled, restingElevation = 6.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content
    )
}

@Composable
fun OmniOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 40.dp)
            .omniPressEffect(source, shape, enabled, restingElevation = 1.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content
    )
}

@Composable
fun OmniTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    MaterialTextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 36.dp)
            .omniPressEffect(source, shape, enabled),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content
    )
}

@Composable
fun OmniIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    MaterialIconButton(
        onClick = onClick,
        modifier = modifier.omniPressEffect(source, CircleShape, enabled),
        enabled = enabled,
        colors = colors,
        interactionSource = source,
        content = content
    )
}

@Composable
fun Modifier.omniClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    restingElevation: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    return this
        .omniPressEffect(
            interactionSource = source,
            shape = shape,
            enabled = enabled,
            restingElevation = restingElevation
        )
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
