// Draws the bubbling liquid surface and rising bubble particles inside the cauldron.
package com.agentcore.ui.components.cauldron

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*

internal fun DrawScope.drawBubblingLiquid(
    gridSize: Int,
    pixelSize: Float,
    liquidY: Int,
    color: Color,
    scale: Float,
    time: Float,
    bounceOffset: Float
) {
    val c = WitchCauldronConstants
    val centerX = gridSize / 2
    val width = (c.LIQUID_WIDTH_MULT * scale).toInt()
    val horizontalSway = (sin(time * PI * c.LIQUID_HORIZ_SWAY_FREQ) * c.LIQUID_HORIZ_SWAY_AMPLITUDE * scale).toInt()
    val sloshIntensity = if (bounceOffset < 0) abs(bounceOffset) * c.LIQUID_SLOSH_INTENSITY_FACTOR else 0f

    for (dx in -width..width) {
        val edgeFactor = cos((dx.toFloat() / width) * (PI / 2).toFloat()).pow(c.LIQUID_EDGE_FACTOR_EXPONENT)
        val wave = (sin(time * PI * c.LIQUID_WAVE_FREQ + dx * c.LIQUID_WAVE_DX_FACTOR) *
            (c.LIQUID_WAVE_AMPLITUDE_BASE * scale + sloshIntensity) * edgeFactor).toInt()
        val centerJump = if (abs(dx) < width / 3) (sloshIntensity * c.LIQUID_CENTER_JUMP_MULTIPLIER).toInt() else 0
        val surfaceY = liquidY + wave - centerJump

        val x = centerX + dx + horizontalSway
        for (dy in 0..(c.LIQUID_DEPTH_MULT * scale).toInt()) {
            drawPixel(x, surfaceY + dy, color, pixelSize)
        }
        drawPixel(x, surfaceY, color.copy(alpha = c.LIQUID_SURFACE_ALPHA), pixelSize)
    }
}

internal fun DrawScope.drawPixelBubbles(
    gridSize: Int,
    pixelSize: Float,
    progress: Float,
    density: Int,
    scale: Float,
    liquidY: Int,
    liquidColor: Color
) {
    val c = WitchCauldronConstants
    val centerX = gridSize / 2

    repeat(density) { i ->
        val rng = kotlin.random.Random(i.toLong() * 1000L)
        val p = (progress + i.toFloat() / density) % 1.1f
        val spread = (c.BUBBLE_SPREAD_MULT * scale).toInt()
        val bx = centerX + (rng.nextInt(spread * 2) - spread)
        val by = liquidY - (p * c.BUBBLE_RISE_HEIGHT_MULT * scale).toInt()

        val alpha = (1f - p).coerceAtLeast(0f).pow(c.BUBBLE_ALPHA_EXPONENT)
        val bubbleColor = Color(
            red = (liquidColor.red * c.BUBBLE_COLOR_BRIGHTNESS_FACTOR + c.BUBBLE_COLOR_BASE_FACTOR).coerceAtMost(1f),
            green = (liquidColor.green * c.BUBBLE_COLOR_BRIGHTNESS_FACTOR + c.BUBBLE_COLOR_BASE_FACTOR).coerceAtMost(1f),
            blue = (liquidColor.blue * c.BUBBLE_COLOR_BRIGHTNESS_FACTOR + c.BUBBLE_COLOR_BASE_FACTOR).coerceAtMost(1f),
            alpha = alpha
        )

        if (by in 1 until liquidY) {
            val r = c.BUBBLE_RADIUS
            for (dx in -r..r) for (dy in -r..r) {
                val distSq = dx * dx + dy * dy
                if (distSq <= c.BUBBLE_MAX_DIST_SQ) {
                    val pa = if (distSq >= c.BUBBLE_EDGE_DIST_SQ) alpha * c.BUBBLE_EDGE_ALPHA_FACTOR else alpha
                    drawPixel(bx + dx, by + dy, bubbleColor.copy(alpha = pa), pixelSize)
                }
            }
            drawPixel(bx - 1, by - 1, Color.White.copy(alpha = alpha * c.BUBBLE_HIGHLIGHT_ALPHA_FACTOR), pixelSize)
        }
    }
}
