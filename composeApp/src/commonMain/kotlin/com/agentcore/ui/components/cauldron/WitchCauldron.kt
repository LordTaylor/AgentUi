// Main WitchCauldron composable and CauldronState enum.
// Orchestrates all animation values and delegates rendering to draw helpers.
// See: CauldronDrawBase, CauldronDrawFire, CauldronDrawLiquid, CauldronDrawEffects.
package com.agentcore.ui.components.cauldron

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI

enum class CauldronState {
    IDLE, SENDING, RECEIVING, THINKING, LOADING
}

@Composable
fun WitchCauldron(
    state: CauldronState,
    modifier: Modifier = Modifier.size(200.dp),
    gridSize: Int = WitchCauldronConstants.GRID_SIZE_DEFAULT,
    liquidColors: Map<CauldronState, Color> = mapOf(
        CauldronState.IDLE      to Color(0xFF2E7D32),
        CauldronState.SENDING   to Color(0xFF4CAF50),
        CauldronState.RECEIVING to Color(0xFF00FF00),
        CauldronState.THINKING  to Color(0xFF81C784),
        CauldronState.LOADING   to Color(0xFF7B1FA2)
    ),
) {
    val c = WitchCauldronConstants
    val transition = rememberInfiniteTransition()

    val fireFrame by transition.animateValue(
        initialValue = 0, targetValue = c.FIRE_FRAME_MAX,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(c.ANIM_FIRE_FRAME_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val fireTime by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(c.ANIM_FIRE_TIME_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val bounceOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (state == CauldronState.THINKING) -c.BOUNCE_OFFSET_THINKING else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(c.ANIM_BOUNCE_DURATION, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val bubbleProgress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(c.ANIM_BUBBLE_PROGRESS_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val ingredientProgress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(c.ANIM_INGREDIENT_PROGRESS_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = c.PULSE_ALPHA_MIN, targetValue = c.PULSE_ALPHA_MAX,
        animationSpec = infiniteRepeatable(
            animation = tween(c.ANIM_PULSE_ALPHA_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val spoonAngle by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(c.ANIM_SPOON_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val palette = if (isSystemInDarkTheme()) CauldronBodyPalette.DARK else CauldronBodyPalette.LIGHT

    Canvas(modifier = modifier) {
        val pixelSize = size.minDimension / gridSize
        val scale = gridSize.toFloat() / c.GRID_SIZE_NORMALIZED
        if (pixelSize < c.SCALE_MIN) return@Canvas

        val bounceY = bounceOffset.toInt()
        val liquidY = (gridSize / 2 - 24 * scale + bounceY).toInt()
        val liquidColor = liquidColors[state] ?: Color(0xFF1B5E20)
        val offset1 = (kotlin.math.sin(fireTime * PI * 2.0) * 8 * scale).toInt()
        val offset2 = (kotlin.math.sin(fireTime * PI * 2.5) * 8 * scale).toInt()

        val steamIntensity = when (state) {
            CauldronState.IDLE      -> 0.6f
            CauldronState.SENDING   -> 1.6f
            CauldronState.RECEIVING -> 1.8f
            CauldronState.THINKING  -> 1.7f
            CauldronState.LOADING   -> 2.0f
        }
        val density = when (state) {
            CauldronState.IDLE      -> c.BUBBLE_DENSITY_IDLE
            CauldronState.SENDING   -> c.BUBBLE_DENSITY_SENDING
            CauldronState.RECEIVING -> c.BUBBLE_DENSITY_RECEIVING
            CauldronState.THINKING  -> c.BUBBLE_DENSITY_THINKING
            CauldronState.LOADING   -> c.BUBBLE_DENSITY_LOADING
        }

        // Draw order: back fire → liquid → bubbles → state effects → steam → cauldron → spoon → front fire
        drawPixelFire(gridSize, pixelSize, fireFrame, offset1, scale, 0.82f, fireTime)
        drawBubblingLiquid(gridSize, pixelSize, liquidY, liquidColor, scale, fireTime, bounceOffset)
        drawPixelBubbles(gridSize, pixelSize, bubbleProgress, density, scale, liquidY, liquidColor)
        if (state == CauldronState.SENDING)   drawPixelIngredients(gridSize, pixelSize, ingredientProgress, scale, liquidY)
        if (state == CauldronState.RECEIVING) drawPixelPowerStream(gridSize, pixelSize, pulseAlpha, scale, liquidY)
        drawSteam(gridSize, pixelSize, fireTime, scale, liquidY, steamIntensity)
        drawPixelCauldronBase(gridSize, pixelSize, bounceY, scale, palette)
        drawPixelFire(gridSize, pixelSize, fireFrame, offset2, scale, 0.86f, fireTime)
    }
}
