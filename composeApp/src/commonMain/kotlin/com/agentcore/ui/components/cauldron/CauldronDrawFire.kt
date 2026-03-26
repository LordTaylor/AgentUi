// Draws the pixel fire behind and in front of the cauldron body.
// Called twice per frame: once at back (verticalPos=0.82) and once at front (0.86).
package com.agentcore.ui.components.cauldron

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*

internal fun DrawScope.drawPixelFire(
    gridSize: Int,
    pixelSize: Float,
    frame: Int,
    offset: Int,
    scale: Float,
    verticalPos: Float,
    time: Float
) {
    val c = WitchCauldronConstants
    val centerX = gridSize / 2
    val centerY = (gridSize * verticalPos).toInt()
    val center = centerX + offset
    val fireHalfWidth = (c.FIRE_HALF_WIDTH_MULT * scale).toInt().coerceAtLeast(1)

    val layers = listOf(
        Triple(Color(0xFF8B0000).copy(alpha = c.FIRE_LAYER_1_ALPHA), c.FIRE_LAYER_1_HEIGHT_BASE, c.FIRE_LAYER_1_HEIGHT_MULT),
        Triple(Color(0xFFFF4500), c.FIRE_LAYER_2_HEIGHT_BASE, c.FIRE_LAYER_2_HEIGHT_MULT),
        Triple(Color(0xFFFFD700), c.FIRE_LAYER_3_HEIGHT_BASE, c.FIRE_LAYER_3_HEIGHT_MULT)
    )

    for (dx in -fireHalfWidth..fireHalfWidth) {
        val baseIntensity = cos((dx.toFloat() / fireHalfWidth) * (PI / 2).toFloat()).pow(c.FIRE_INTENSITY_EXPONENT)
        val noise = (
            sin(time * PI * c.FIRE_NOISE_FREQ_1 + dx * c.FIRE_NOISE_DX_FACTOR_1) * c.FIRE_NOISE_AMPLITUDE_1 +
            cos(time * PI * c.FIRE_NOISE_FREQ_2 - dx * c.FIRE_NOISE_DX_FACTOR_2) * c.FIRE_NOISE_AMPLITUDE_2 +
            sin(dx * c.FIRE_NOISE_DX_FACTOR_3) * c.FIRE_NOISE_AMPLITUDE_3
        )
        val flicker = (frame % c.FIRE_FRAME_COUNT).toFloat()

        for ((color, baseHeight, heightMult) in layers) {
            val h = ((baseHeight + flicker * c.FIRE_FICKER_MULTIPLIER) * scale * heightMult *
                (baseIntensity + c.FIRE_BASE_INTENSITY_OFFSET) * (c.FIRE_NOISE_HEIGHT_OFFSET + noise)).toInt()
            for (dy in 0 until h) {
                val lick = (sin(time * PI * c.FIRE_LICK_FREQ + dy * c.FIRE_LICK_DY_FACTOR + dx * c.FIRE_LICK_DX_FACTOR) *
                    c.FIRE_LICK_AMPLITUDE * scale * (dy.toFloat() / h.coerceAtLeast(1))).toInt()
                drawPixel(center + dx + lick, centerY - dy, color, pixelSize)
            }
        }
    }
}
