package com.omnidapt.pd.ui.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

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
    pressedScale: Float = 0.985f
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
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = if (active) 0.96f else 1f
        this.shape = shape
    }
}
