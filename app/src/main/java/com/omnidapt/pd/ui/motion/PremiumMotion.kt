package com.omnidapt.pd.ui.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

object OmniMotion {
    const val PressInMillis = 90
    const val StateMillis = 220
    const val PageMillis = 260
    const val RoleMillis = 300
    const val DialogMillis = 200
    const val AmbientFastMillis = 18_000
    const val AmbientMediumMillis = 23_000
    const val AmbientSlowMillis = 28_000
}

/**
 * A shared, restrained press treatment for Material and custom controls.
 * It uses a graphics-layer transform, so pressing a control does not trigger
 * a surrounding layout pass.
 */
@Composable
fun Modifier.omniPressEffect(
    interactionSource: MutableInteractionSource,
    shape: Shape,
    enabled: Boolean = true,
    restingElevation: androidx.compose.ui.unit.Dp = 0.dp,
    pressedScale: Float = 0.97f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val active = enabled && pressed
    val scale by animateFloatAsState(
        targetValue = if (active) pressedScale else 1f,
        animationSpec = if (active) {
            tween(OmniMotion.PressInMillis, easing = FastOutSlowInEasing)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "omniPressScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (active) 2.dp else restingElevation,
        animationSpec = tween(
            durationMillis = if (active) OmniMotion.PressInMillis else OmniMotion.StateMillis,
            easing = FastOutSlowInEasing
        ),
        label = "omniPressElevation"
    )
    return this
        .shadow(elevation = elevation, shape = shape, clip = false)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}
