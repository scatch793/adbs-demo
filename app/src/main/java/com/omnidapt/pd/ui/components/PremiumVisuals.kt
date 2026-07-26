package com.omnidapt.pd.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
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
import com.omnidapt.pd.ui.theme.BrandBlue
import com.omnidapt.pd.ui.theme.DeepBlue
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
        AmbientStyle.Login -> listOf(Color(0xFFF9FCFF), Color(0xFFEAF4FF), Color(0xFFF6F4FF))
        AmbientStyle.Patient -> listOf(Color(0xFFFBFDFF), Color(0xFFEDF7FF), Color(0xFFF7F5FF))
        AmbientStyle.Doctor -> listOf(Color(0xFFF8FBFF), Color(0xFFEDF3FD), Color(0xFFF4F2FC))
    }
    val opacity = when (style) {
        AmbientStyle.Login -> 0.22f
        AmbientStyle.Patient -> 0.18f
        AmbientStyle.Doctor -> 0.13f
    }

    Box(
        modifier = modifier.background(Brush.verticalGradient(baseColors))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .semantics { hideFromAccessibility() }
        ) {
            FlowingLightVeil(
                opacity = opacity * 0.72f,
                durationMillis = OmniMotion.AmbientMediumMillis
            )
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
private fun BoxScope.FlowingLightVeil(
    opacity: Float,
    durationMillis: Int
) {
    val transition = rememberInfiniteTransition(label = "flowingLightVeil")
    val driftX by transition.animateFloat(
        initialValue = -46f,
        targetValue = 46f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flowingLightVeilX"
    )
    val driftY by transition.animateFloat(
        initialValue = -24f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis + 4_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flowingLightVeilY"
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = driftX.dp.toPx()
                translationY = driftY.dp.toPx()
                scaleX = 1.16f
                scaleY = 1.16f
                rotationZ = -3.5f
            }
    ) {
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.22f to AuraBlue.copy(alpha = opacity * 0.3f),
                    0.43f to AuraCyan.copy(alpha = opacity),
                    0.62f to AuraIndigo.copy(alpha = opacity * 0.74f),
                    0.82f to AuraBlue.copy(alpha = opacity * 0.25f),
                    1f to Color.Transparent
                ),
                start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.12f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.88f)
            )
        )
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
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = BrandBlue,
        contentColor = Color.White,
        disabledContainerColor = BrandBlue.copy(alpha = 0.12f),
        disabledContentColor = DeepBlue.copy(alpha = 0.38f)
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(
        defaultElevation = 2.dp,
        pressedElevation = 0.dp,
        focusedElevation = 2.dp,
        hoveredElevation = 3.dp,
        disabledElevation = 0.dp
    ),
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
            .omniPressEffect(source, shape, enabled),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = source,
        content = {
            ProvideTextStyle(
                LocalTextStyle.current.copy(
                    color = if (enabled) colors.contentColor else colors.disabledContentColor
                )
            ) {
                content()
            }
        }
    )
}

@Composable
fun OmniOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = DeepBlue,
        disabledContentColor = DeepBlue.copy(alpha = 0.32f)
    ),
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
            .omniPressEffect(source, shape, enabled),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = source,
        content = {
            ProvideTextStyle(
                LocalTextStyle.current.copy(
                    color = if (enabled) colors.contentColor else colors.disabledContentColor
                )
            ) {
                content()
            }
        }
    )
}

@Composable
fun OmniTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: ButtonColors = ButtonDefaults.textButtonColors(
        contentColor = BrandBlue,
        disabledContentColor = DeepBlue.copy(alpha = 0.32f)
    ),
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
        content = {
            ProvideTextStyle(
                LocalTextStyle.current.copy(
                    color = if (enabled) colors.contentColor else colors.disabledContentColor
                )
            ) {
                content()
            }
        }
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
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    return this
        .omniPressEffect(
            interactionSource = source,
            shape = shape,
            enabled = enabled
        )
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}
