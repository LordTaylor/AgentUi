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
    val horizontalSway = (sin(time * PI * c.LIQUID_HORIZ_SWAY_FREQ) * c.LIQUID_HORIZ_SWAY_AMPLITUDE * scale).toInt()
    val sloshIntensity = if (bounceOffset < 0) abs(bounceOffset) * c.LIQUID_SLOSH_INTENSITY_FACTOR else 0f

    // Squash & Stretch: liquid spreads wider and shallower at bounce apex
    val squashAmount = if (bounceOffset < 0) (abs(bounceOffset) / 13f).coerceIn(0f, 1f) else 0f
    val widthMult = 1f + squashAmount * (c.LIQUID_SQUASH_FACTOR - 1f)
    val depthMult = 1f - squashAmount * (1f - c.LIQUID_STRETCH_FACTOR)
    val width = (c.LIQUID_WIDTH_MULT * scale * widthMult).toInt()
    val depth = (c.LIQUID_DEPTH_MULT * scale * depthMult).toInt()

    for (dx in -width..width) {
        val edgeFactor = cos((dx.toFloat() / width) * (PI / 2).toFloat()).pow(c.LIQUID_EDGE_FACTOR_EXPONENT)
        // Deformed wave: pow() gives sharper crests and rounder troughs (cartoon look)
        val rawWave = sin(time * PI * c.LIQUID_WAVE_FREQ + dx * c.LIQUID_WAVE_DX_FACTOR).toFloat()
        val deformedWave = sign(rawWave) * abs(rawWave).pow(c.LIQUID_WAVE_DEFORM_FACTOR)
        val wave = (deformedWave * (c.LIQUID_WAVE_AMPLITUDE_BASE * scale + sloshIntensity) * edgeFactor).toInt()
        val centerJump = if (abs(dx) < width / 3) (sloshIntensity * c.LIQUID_CENTER_JUMP_MULTIPLIER).toInt() else 0
        val surfaceY = liquidY + wave - centerJump

        val x = centerX + dx + horizontalSway
        for (dy in 0..depth) {
            drawPixel(x, surfaceY + dy, color, pixelSize)
        }
        drawPixel(x, surfaceY, color.copy(alpha = c.LIQUID_SURFACE_ALPHA), pixelSize)

        // Ripple micro-waves: extra translucent pixels above surface
        for (ripple in 1..c.LIQUID_RIPPLE_COUNT) {
            val rPhase = (time * PI * c.LIQUID_RIPPLE_SPEED_FACTOR + ripple * 0.7f + dx * 0.05f).toFloat()
            val rY = (sin(rPhase) * (scale * 0.7f)).toInt()
            drawPixel(x, surfaceY - rY - 1, color.copy(alpha = 0.18f), pixelSize)
        }
    }

    // Splash particles: sparse pixels at the surface boundary
    val splashCount = (width / c.LIQUID_SPLASH_DENSITY_DIVISOR).coerceAtLeast(1)
    repeat(splashCount) { i ->
        val sRng = kotlin.random.Random(i.toLong() * 31L + (time * 4).toLong())
        val sx = centerX + (sRng.nextInt(width * 2) - width) + horizontalSway
        drawPixel(sx, liquidY - 1, color.copy(alpha = 0.35f), pixelSize)
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

        // Hesitation: ~15% of bubbles pulse rather than fade smoothly
        val hesitates = rng.nextFloat() < 0.15f
        val hesitationMult = if (hesitates) (sin(p * PI * 4f) * 0.25f + 0.75f).toFloat() else 1f
        val alpha = (1f - p).coerceAtLeast(0f).pow(c.BUBBLE_ALPHA_EXPONENT) * hesitationMult
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
