// State-driven effects: steam/smoke clouds, falling tech-objects (RECEIVING), ejecting tech-objects (SENDING).
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
                        drawPixel(round(particleX).toInt() + dx, particleY + dy, steamColor, pixelSize)
                    }
                }
            }
        }
    }
}

// ── Tech-object pixel art ────────────────────────────────────────────────────
// Each pattern is a list of strings where '#' = filled pixel, '_' = empty.

private val TECH_PATTERNS = arrayOf(
    // 0: MOUSE
    arrayOf("_##_", "####", "#_##", "####", "_##_", "__#_"),
    // 1: FLOPPY DISK
    arrayOf("#####", "##__#", "#___#", "#####", "#####"),
    // 2: PHONE
    arrayOf("###", "#_#", "#_#", "###", "_#_"),
    // 3: KEYBOARD
    arrayOf("#####", "#_#_#", "#####"),
)

private val TECH_COLORS = arrayOf(
    Color(0xFF00BFFF),   // blue — mouse
    Color(0xFFFFD700),   // gold — floppy
    Color(0xFF90EE90),   // green — phone
    Color(0xFFFF6B35),   // orange — keyboard
)

private fun DrawScope.drawTechObject(type: Int, cx: Int, cy: Int, alpha: Float, pixelSize: Float) {
    val pattern = TECH_PATTERNS[type % TECH_PATTERNS.size]
    val color   = TECH_COLORS[type % TECH_COLORS.size].copy(alpha = alpha)
    val rows    = pattern.size
    val cols    = pattern[0].length
    val ox      = cx - cols / 2
    val oy      = cy - rows / 2
    for (row in pattern.indices) {
        for (col in pattern[row].indices) {
            if (pattern[row][col] == '#') drawPixel(ox + col, oy + row, color, pixelSize)
        }
    }
}

// Objects falling INTO the cauldron (RECEIVING state)
internal fun DrawScope.drawTechObjectsFalling(
    gridSize: Int, pixelSize: Float, progress: Float, scale: Float, liquidY: Int
) {
    val c       = WitchCauldronConstants
    val centerX = gridSize / 2
    repeat(c.TECH_OBJ_COUNT_FALLING) { i ->
        val rng   = kotlin.random.Random(i.toLong() * 777L)
        val p     = (progress + i.toFloat() / c.TECH_OBJ_COUNT_FALLING) % 1.0f
        val spreadX = rng.nextInt(50) - 25
        val wobble  = (sin(p.toDouble() * 6.0 + i) * 3.0 * scale).toInt()
        val objX    = centerX + spreadX + wobble
        val objY    = (p * (liquidY + 4)).toInt()
        val alpha   = (1f - p * 0.5f).coerceIn(0.3f, 1f)
        if (objY < liquidY + 6) drawTechObject(i % TECH_PATTERNS.size, objX, objY, alpha, pixelSize)
    }
}

// Objects ejected FROM the cauldron in parabolic arcs (SENDING state)
internal fun DrawScope.drawTechObjectsEjecting(
    gridSize: Int, pixelSize: Float, progress: Float, scale: Float, liquidY: Int
) {
    val c       = WitchCauldronConstants
    val centerX = gridSize / 2
    repeat(c.TECH_OBJ_COUNT_EJECTING) { i ->
        val rng    = kotlin.random.Random(i.toLong() * 333L)
        val p      = (progress + i.toFloat() / c.TECH_OBJ_COUNT_EJECTING) % 1.0f
        val angle  = rng.nextFloat() * 2.4f - 1.2f      // spread ±69°
        val speed  = 0.7f + rng.nextFloat() * 0.5f
        val vx     = sin(angle.toDouble()).toFloat() * speed
        val vy     = cos(angle.toDouble()).toFloat() * speed
        val gravity = 1.2f * p * p
        val objX   = (centerX + vx * p * 55f).toInt()
        val objY   = (liquidY + (-vy * p * 65f + gravity * 55f)).toInt()
        val alpha  = (1f - p).coerceAtLeast(0f)
        if (alpha > 0.05f) drawTechObject((i + 1) % TECH_PATTERNS.size, objX, objY, alpha, pixelSize)
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
