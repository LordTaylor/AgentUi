// State-driven effects: steam/smoke clouds, falling ingredients, power stream beam.
package com.agentcore.ui.components.cauldron

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*

internal fun DrawScope.drawSteam(
    gridSize: Int,
    pixelSize: Float,
    time: Float,
    scale: Float,
    liquidY: Int,
    intensity: Float
) {
    val c = WitchCauldronConstants
    val centerX = gridSize / 2
    val numStreams = (c.STEAM_NUM_BASE + intensity * c.STEAM_NUM_INTENSITY_MULT).toInt().coerceIn(2, 5)

    for (i in 0 until numStreams) {
        val streamOffset = ((i.toFloat() - numStreams / 2) / numStreams * c.STEAM_X_SPREAD_MULT * scale).toInt()
        val startX = centerX + streamOffset
        val timeOffset = i * c.STEAM_TIME_OFFSET_PER_STREAM
        val particleCount = (c.STEAM_PARTICLE_COUNT_BASE + intensity * c.STEAM_PARTICLE_COUNT_INTENSITY_MULT).toInt()

        for (j in 0 until particleCount) {
            val raw = time * c.STEAM_ANIMATION_SPEED + j * c.STEAM_PARTICLE_SPACING - timeOffset
            val progress = (raw % 1.0f).let { if (it < 0) it + 1f else it }

            val riseHeight = (c.STEAM_RISE_HEIGHT_MULT * scale * intensity).toInt()
            val particleY = (liquidY + c.STEAM_START_OFFSET_FROM_LIQUID * scale).toInt() - (progress * riseHeight).toInt()
            val sway = sin(progress * PI * 2 + timeOffset) * (c.STEAM_SWAY_AMPLITUDE * scale)
            val drift = cos(time * PI * c.STEAM_DRIFT_SPEED + i.toFloat()) * (c.STEAM_DRIFT_AMPLITUDE * scale)
            val particleX = startX + sway + drift

            val baseSize = (c.STEAM_SIZE_BASE_MULT * scale).toInt().coerceAtLeast(1)
            val particleSize = (baseSize + progress * (c.STEAM_SIZE_GROWTH_MULT * scale)).toInt()
            val alpha = ((1 - progress) * intensity * 0.25f).coerceIn(0f, c.STEAM_ALPHA_MAX)
            val steamColor = Color.LightGray.copy(alpha = alpha)

            for (dx in -particleSize..particleSize) {
                for (dy in -particleSize..particleSize) {
                    if (dx * dx + dy * dy <= particleSize * particleSize) {
                        drawPixel(particleX.toInt() + dx, particleY + dy, steamColor, pixelSize)
                    }
                }
            }
        }
    }
}

internal fun DrawScope.drawPixelIngredients(
    gridSize: Int,
    pixelSize: Float,
    progress: Float,
    scale: Float,
    liquidY: Int
) {
    val c = WitchCauldronConstants
    val centerX = gridSize / 2
    repeat(c.INGREDIENT_COUNT) { i ->
        val p = progress % 1f
        val ix = centerX + (sin(i * c.INGREDIENT_FREQ_BASE + p * c.INGREDIENT_FREQ_SPEED) * c.INGREDIENT_X_SPREAD_MULT * scale).toInt()
        val iy = (p * liquidY).toInt()
        val color = when (i) { 0 -> Color.White; 1 -> Color(0xFFFF5722); else -> Color(0xFFFFEB3B) }
        val h = c.INGREDIENT_SIZE_HALF
        for (dx in -h..h) for (dy in -h..h) {
            drawPixel(ix + dx, iy + dy + c.INGREDIENT_SIZE_OFFSET, color, pixelSize)
        }
        drawPixel(ix, iy, Color.White, pixelSize)
    }
}

internal fun DrawScope.drawPixelPowerStream(
    gridSize: Int,
    pixelSize: Float,
    alpha: Float,
    scale: Float,
    liquidY: Int
) {
    val c = WitchCauldronConstants
    val centerX = gridSize / 2
    if (liquidY <= 0) return
    for (dy in 0..liquidY) {
        val y = liquidY - dy
        val a = alpha * (1f - dy.toFloat() / liquidY)
        for (dx in -c.POWER_STREAM_WIDTH / 2..c.POWER_STREAM_WIDTH / 2) {
            val distFactor = 1f - abs(dx).toFloat() / (c.POWER_STREAM_WIDTH / 2)
            val finalAlpha = a * distFactor.pow(c.POWER_STREAM_ALPHA_EXPONENT)
            val color = if (abs(dx) < c.POWER_STREAM_CORE_WIDTH) Color.Cyan else Color(0xFF00FF00)
            drawPixel(centerX + dx, y, color.copy(alpha = finalAlpha), pixelSize)
        }
    }
}
