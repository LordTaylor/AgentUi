package com.agentcore.ui.components

import androidx.compose.animation.core.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

object WitchCauldronConstants {
    // === Grid & Scale ===
    const val GRID_SIZE_DEFAULT: Int = 128
    const val GRID_SIZE_NORMALIZED: Float = 128f
    const val SCALE_MIN = 0.1f

    // === Animation Durations (ms) ===
    const val ANIM_FIRE_FRAME_DURATION = 600
    const val ANIM_FIRE_TIME_DURATION = 4000
    const val ANIM_BOUNCE_DURATION = 400 // Slower bounce
    const val ANIM_BUBBLE_PROGRESS_DURATION = 12000
    const val ANIM_INGREDIENT_PROGRESS_DURATION = 3000
    const val ANIM_PULSE_ALPHA_DURATION = 1200

    // === Animation Values ===
    const val FIRE_FRAME_MAX = 7
    const val BOUNCE_OFFSET_THINKING = 10f // More bounce
    const val PULSE_ALPHA_MIN = 0.4f
    const val PULSE_ALPHA_MAX = 0.9f

    // === Bubble Density by State ===
    const val BUBBLE_DENSITY_IDLE = 4
    const val BUBBLE_DENSITY_SENDING = 8
    const val BUBBLE_DENSITY_RECEIVING = 12
    const val BUBBLE_DENSITY_THINKING = 8
    const val BUBBLE_DENSITY_LOADING = 16

    // === Fire Rendering ===
    const val FIRE_FRAME_COUNT = 4
    const val FIRE_OFFSET_AMPLITUDE = 8
    const val FIRE_OFFSET_FREQ_1 = 2.0
    const val FIRE_OFFSET_FREQ_2 = 2.5
    const val FIRE_BACK_VERTICAL_POS = 0.82f
    const val FIRE_FRONT_VERTICAL_POS = 0.86f
    const val FIRE_HALF_WIDTH_MULT = 48
    const val FIRE_INTENSITY_EXPONENT = 1.2f
    const val FIRE_NOISE_FREQ_1 = 12.0
    const val FIRE_NOISE_DX_FACTOR_1 = 0.3
    const val FIRE_NOISE_AMPLITUDE_1 = 0.4f
    const val FIRE_NOISE_FREQ_2 = 5.0
    const val FIRE_NOISE_DX_FACTOR_2 = 0.5
    const val FIRE_NOISE_AMPLITUDE_2 = 0.3f
    const val FIRE_NOISE_DX_FACTOR_3 = 0.8
    const val FIRE_NOISE_AMPLITUDE_3 = 0.3f
    const val FIRE_LICK_FREQ = 6.0
    const val FIRE_LICK_DY_FACTOR = 0.2
    const val FIRE_LICK_DX_FACTOR = 0.1
    const val FIRE_LICK_AMPLITUDE = 2.5
    const val FIRE_FICKER_MULTIPLIER = 2f
    const val FIRE_BASE_INTENSITY_OFFSET = 0.3f
    const val FIRE_NOISE_HEIGHT_OFFSET = 1.2f

    // Fire layers (color, baseHeight, heightMult)
    const val FIRE_LAYER_1_ALPHA = 0.4f
    const val FIRE_LAYER_1_HEIGHT_BASE = 14f
    const val FIRE_LAYER_1_HEIGHT_MULT = 1.2f
    const val FIRE_LAYER_2_HEIGHT_BASE = 10f
    const val FIRE_LAYER_2_HEIGHT_MULT = 1.0f
    const val FIRE_LAYER_3_HEIGHT_BASE = 6f
    const val FIRE_LAYER_3_HEIGHT_MULT = 0.9f

    // === Cauldron Geometry ===
    const val CAULDRON_CENTER_Y_OFFSET = 8
    const val LEG_WIDTH_MULT = 10 // Thicker legs
    const val LEG_WIDTH_MIN = 3
    const val LEG_POS_X_MULT = 22
    const val LEG_Y_START_MULT = 22
    const val LEG_Y_END_MULT = 36
    const val CAULDRON_RADIUS_MULT = 42 // Rounder body
    const val CAULDRON_DIY_MIN_MULT = -26
    const val CAULDRON_DIY_MAX_MULT = 34
    const val CAULDRON_DX_MIN_MULT = -46
    const val CAULDRON_DX_MAX_MULT = 46
    const val CAULDRON_ELLIPSE_Y_SCALE = 1.2f // Less oval, more round
    const val CAULDRON_RIM_DX_MIN_MULT = -40
    const val CAULDRON_RIM_DX_MAX_MULT = 39
    const val CAULDRON_RIM_DY_MIN_MULT = -30
    const val CAULDRON_RIM_DY_MAX_MULT = -22
    const val CAULDRON_REFLECT_SIZE_MULT = 6
    const val CAULDRON_REFLECT_DX_OFFSET = -22
    const val CAULDRON_REFLECT_DY_OFFSET = -10
    const val CAULDRON_REFLECT_ALPHA = 0.2f

    // === Liquid Rendering ===
    const val LIQUID_Y_MULT = 24
    const val LIQUID_WIDTH_MULT = 36
    const val LIQUID_HORIZ_SWAY_FREQ = 1.5 // Slower sway
    const val LIQUID_HORIZ_SWAY_AMPLITUDE = 3
    const val LIQUID_SLOSH_INTENSITY_FACTOR = 0.8f
    const val LIQUID_WAVE_FREQ = 6.0
    const val LIQUID_WAVE_DX_FACTOR = 0.2
    const val LIQUID_WAVE_AMPLITUDE_BASE = 6
    const val LIQUID_CENTER_JUMP_THRESHOLD = 3
    const val LIQUID_CENTER_JUMP_MULTIPLIER = 1.8f
    const val LIQUID_EDGE_FACTOR_EXPONENT = 0.6f
    const val LIQUID_DEPTH_MULT = 8
    const val LIQUID_SURFACE_ALPHA = 0.9f

    // === Bubble Rendering ===
    const val BUBBLE_SPREAD_MULT = 28
    const val BUBBLE_RISE_HEIGHT_MULT = 55
    const val BUBBLE_ALPHA_EXPONENT = 0.6f
    const val BUBBLE_COLOR_BRIGHTNESS_FACTOR = 0.9f
    const val BUBBLE_COLOR_BASE_FACTOR = 0.3f
    const val BUBBLE_RADIUS = 3 // Bigger, softer bubbles
    const val BUBBLE_MAX_DIST_SQ = 10
    const val BUBBLE_EDGE_DIST_SQ = 8
    const val BUBBLE_EDGE_ALPHA_FACTOR = 0.5f
    const val BUBBLE_HIGHLIGHT_ALPHA_FACTOR = 0.6f

    // === Ingredient Rendering ===
    const val INGREDIENT_COUNT = 3
    const val INGREDIENT_FREQ_BASE = 2.0
    const val INGREDIENT_FREQ_SPEED = 5.0
    const val INGREDIENT_X_SPREAD_MULT = 20
    const val INGREDIENT_SIZE_HALF = 1
    const val INGREDIENT_SIZE_OFFSET = 5

    // === Power Stream Rendering ===
    const val POWER_STREAM_WIDTH = 52
    const val POWER_STREAM_ALPHA_EXPONENT = 1.5f
    const val POWER_STREAM_CORE_WIDTH = 2

    // === Steam/Smoke Rendering ===
    const val STEAM_CENTER_Y_RATIO = 0.45f
    const val STEAM_NUM_BASE = 2 // Fewer, chunkier clouds
    const val STEAM_NUM_INTENSITY_MULT = 2
    const val STEAM_NUM_MAX = 5
    const val STEAM_X_SPREAD_MULT = 22
    const val STEAM_TIME_OFFSET_PER_STREAM = 1.8f
    const val STEAM_PARTICLE_COUNT_BASE = 15
    const val STEAM_PARTICLE_COUNT_INTENSITY_MULT = 8
    const val STEAM_ANIMATION_SPEED = 0.4f // Slower steam
    const val STEAM_PARTICLE_SPACING = 0.12f
    const val STEAM_RISE_HEIGHT_MULT = 50
    const val STEAM_START_OFFSET_FROM_LIQUID = 4
    const val STEAM_SWAY_AMPLITUDE = 12
    const val STEAM_DRIFT_AMPLITUDE = 8
    const val STEAM_DRIFT_SPEED = 0.2f
    const val STEAM_SIZE_BASE_MULT = 6 // Much chunkier
    const val STEAM_SIZE_GROWTH_MULT = 10
    const val STEAM_ALPHA_MAX = 0.4f

    // === Steam Intensity by State ===
    const val STEAM_INTENSITY_IDLE = 0.3f
    const val STEAM_INTENSITY_SENDING = 0.6f
    const val STEAM_INTENSITY_RECEIVING = 0.8f
    const val STEAM_INTENSITY_THINKING = 0.7f
    const val STEAM_INTENSITY_LOADING = 1.0f

    // === Preview Dimensions ===
    const val PREVIEW_SIZE_DP = 200
    const val PREVIEW_PADDING_DP = 16
    const val PREVIEW_ITEM_SIZE_DP = 100
    const val PREVIEW_ITEM_PADDING_DP = 8
    const val PREVIEW_LABEL_FONT_SIZE_SP = 10
}

enum class CauldronState {
    IDLE, SENDING, RECEIVING, THINKING, LOADING
}

@Composable
fun WitchCauldron(
    state: CauldronState,
    modifier: Modifier = Modifier.size(200.dp),
    gridSize: Int = WitchCauldronConstants.GRID_SIZE_DEFAULT,
    liquidColors: Map<CauldronState, Color> = mapOf(
        CauldronState.IDLE to Color(0xFF2E7D32),
        CauldronState.SENDING to Color(0xFF4CAF50),
        CauldronState.RECEIVING to Color(0xFF00FF00),
        CauldronState.THINKING to Color(0xFF81C784),
        CauldronState.LOADING to Color(0xFF7B1FA2) // Purple magic
    ),
) {
    val transition = rememberInfiniteTransition()

    val fireFrame by transition.animateValue(
        initialValue = 0,
        targetValue = WitchCauldronConstants.FIRE_FRAME_MAX,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(WitchCauldronConstants.ANIM_FIRE_FRAME_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val fireTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(WitchCauldronConstants.ANIM_FIRE_TIME_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val bounceOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (state == CauldronState.THINKING) -WitchCauldronConstants.BOUNCE_OFFSET_THINKING else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(WitchCauldronConstants.ANIM_BOUNCE_DURATION, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val bubbleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(WitchCauldronConstants.ANIM_BUBBLE_PROGRESS_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val ingredientProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(WitchCauldronConstants.ANIM_INGREDIENT_PROGRESS_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseAlpha by transition.animateFloat(
        initialValue = WitchCauldronConstants.PULSE_ALPHA_MIN,
        targetValue = WitchCauldronConstants.PULSE_ALPHA_MAX,
        animationSpec = infiniteRepeatable(
            animation = tween(WitchCauldronConstants.ANIM_PULSE_ALPHA_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val pixelSize = size.minDimension / gridSize
        val scale = gridSize.toFloat() / WitchCauldronConstants.GRID_SIZE_NORMALIZED

        if (pixelSize >= WitchCauldronConstants.SCALE_MIN) {
            val bounceY = bounceOffset.toInt()
            val liquidY = (gridSize / 2 - (24 * scale) + bounceY).toInt()
            val liquidColor = liquidColors[state] ?: Color(0xFF1B5E20)

            val offset1 = (sin(fireTime * PI * 2.0) * 8 * scale).toInt()
            val offset2 = (sin(fireTime * PI * 2.5) * 8 * scale).toInt()

            // 1. Ogień z tyłu
            drawPixelFire(gridSize, pixelSize, fireFrame, offset1, scale, 0.82f, fireTime)

            // 2. Ciecz
            drawBubblingLiquid(gridSize, pixelSize, liquidY, liquidColor, scale, fireTime, bounceOffset)

            // 4. Efekty stanu
            val density = when(state) {
                CauldronState.IDLE -> WitchCauldronConstants.BUBBLE_DENSITY_IDLE
                CauldronState.SENDING -> WitchCauldronConstants.BUBBLE_DENSITY_SENDING
                CauldronState.RECEIVING -> WitchCauldronConstants.BUBBLE_DENSITY_RECEIVING
                CauldronState.THINKING -> WitchCauldronConstants.BUBBLE_DENSITY_THINKING
                CauldronState.LOADING -> WitchCauldronConstants.BUBBLE_DENSITY_LOADING
            }

            drawPixelBubbles(gridSize, pixelSize, bubbleProgress, density, scale, liquidY, liquidColor)

            if (state == CauldronState.SENDING) {
                drawPixelIngredients(gridSize, pixelSize, ingredientProgress, scale, liquidY)
            }
            if (state == CauldronState.RECEIVING) {
                drawPixelPowerStream(gridSize, pixelSize, pulseAlpha, scale, liquidY)
            }

            // 5. Para/dym wylatujący z kotła
            val steamIntensity = when(state) {
                CauldronState.IDLE -> 0.6f
                CauldronState.SENDING -> 1.6f
                CauldronState.RECEIVING -> 1.8f
                CauldronState.THINKING -> 1.7f
                CauldronState.LOADING -> 2.0f
            }
            drawSteam(gridSize, pixelSize, fireTime, scale, liquidY, steamIntensity)

            // 3. Kociołek
            drawPixelCauldronBase(gridSize, pixelSize, bounceY, scale)

            // 6. Ogień z przodu
            drawPixelFire(gridSize, pixelSize, fireFrame, offset2, scale, 0.86f, fireTime)
        }
    }
}

private fun DrawScope.drawPixel(x: Int, y: Int, color: Color, pixelSize: Float) {
    drawRect(
        color = color,
        topLeft = Offset(x * pixelSize, y * pixelSize),
        size = Size(pixelSize, pixelSize)
    )
}


private fun DrawScope.drawSteam(
    gridSize: Int,
    pixelSize: Float,
    time: Float,
    scale: Float,
    liquidY: Int,
    intensity: Float
) {
    val centerX = gridSize / 2

    val numStreams = (WitchCauldronConstants.STEAM_NUM_BASE + intensity * WitchCauldronConstants.STEAM_NUM_INTENSITY_MULT).toInt().coerceIn(2, 5)

    for (i in 0 until numStreams) {
        val streamOffset = ((i.toFloat() - numStreams / 2) / numStreams * WitchCauldronConstants.STEAM_X_SPREAD_MULT * scale).toInt()
        val startX = centerX + streamOffset

        val timeOffset = i * WitchCauldronConstants.STEAM_TIME_OFFSET_PER_STREAM

        val particleCount = (WitchCauldronConstants.STEAM_PARTICLE_COUNT_BASE + intensity * WitchCauldronConstants.STEAM_PARTICLE_COUNT_INTENSITY_MULT).toInt()
        for (j in 0 until particleCount) {
            val progress = ((time * WitchCauldronConstants.STEAM_ANIMATION_SPEED + j * WitchCauldronConstants.STEAM_PARTICLE_SPACING - timeOffset) % 1.0f).let { if (it < 0) it + 1f else it }

            val riseHeight = (WitchCauldronConstants.STEAM_RISE_HEIGHT_MULT * scale * intensity).toInt()
            val particleY = (liquidY + WitchCauldronConstants.STEAM_START_OFFSET_FROM_LIQUID * scale).toInt() - (progress * riseHeight).toInt()

            val sway = sin(progress * PI * 2 + timeOffset) * (WitchCauldronConstants.STEAM_SWAY_AMPLITUDE * scale)
            val drift = cos(time * PI * WitchCauldronConstants.STEAM_DRIFT_SPEED + i.toFloat()) * (WitchCauldronConstants.STEAM_DRIFT_AMPLITUDE * scale)
            val particleX = startX + sway + drift

            val baseSize = (WitchCauldronConstants.STEAM_SIZE_BASE_MULT * scale).toInt().coerceAtLeast(1)
            val sizeGrowth = progress * (WitchCauldronConstants.STEAM_SIZE_GROWTH_MULT * scale)
            val particleSize = (baseSize + sizeGrowth).toInt()

            val alpha = ((1 - progress) * intensity * 0.25f).coerceIn(0f, WitchCauldronConstants.STEAM_ALPHA_MAX)

            // Chunkier smoke color
            val steamColor = Color.LightGray.copy(alpha = alpha)

            // Cartoonish chunky cloud (pixel circle)
            for (dx in -particleSize..particleSize) {
                for (dy in -particleSize..particleSize) {
                    if (dx * dx + dy * dy <= particleSize * particleSize) {
                        drawPixel(
                            (particleX).toInt() + dx,
                            particleY + dy,
                            steamColor,
                            pixelSize
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPixelFire(
    gridSize: Int,
    pixelSize: Float,
    frame: Int,
    offset: Int,
    scale: Float,
    verticalPos: Float,
    time: Float
) {
    val centerX = gridSize / 2
    val centerY = (gridSize * verticalPos).toInt()
    val center = centerX + offset
    val fireHalfWidth = (WitchCauldronConstants.FIRE_HALF_WIDTH_MULT * scale).toInt().coerceAtLeast(1)

    for (dx in -fireHalfWidth..fireHalfWidth) {
        val baseIntensity = cos((dx.toFloat() / fireHalfWidth) * (PI / 2).toFloat()).pow(WitchCauldronConstants.FIRE_INTENSITY_EXPONENT)
        val noise = (sin(time * PI * WitchCauldronConstants.FIRE_NOISE_FREQ_1 + dx * WitchCauldronConstants.FIRE_NOISE_DX_FACTOR_1) * WitchCauldronConstants.FIRE_NOISE_AMPLITUDE_1 +
                    cos(time * PI * WitchCauldronConstants.FIRE_NOISE_FREQ_2 - dx * WitchCauldronConstants.FIRE_NOISE_DX_FACTOR_2) * WitchCauldronConstants.FIRE_NOISE_AMPLITUDE_2 +
                    sin(dx * WitchCauldronConstants.FIRE_NOISE_DX_FACTOR_3) * WitchCauldronConstants.FIRE_NOISE_AMPLITUDE_3)
        val flicker = (frame % WitchCauldronConstants.FIRE_FRAME_COUNT).toFloat()

        val layers = listOf(
            Triple(Color(0xFF8B0000).copy(alpha = WitchCauldronConstants.FIRE_LAYER_1_ALPHA), WitchCauldronConstants.FIRE_LAYER_1_HEIGHT_BASE, WitchCauldronConstants.FIRE_LAYER_1_HEIGHT_MULT),
            Triple(Color(0xFFFF4500), WitchCauldronConstants.FIRE_LAYER_2_HEIGHT_BASE, WitchCauldronConstants.FIRE_LAYER_2_HEIGHT_MULT),
            Triple(Color(0xFFFFD700), WitchCauldronConstants.FIRE_LAYER_3_HEIGHT_BASE, WitchCauldronConstants.FIRE_LAYER_3_HEIGHT_MULT)
        )

        for ((color, baseHeight, heightMult) in layers) {
            val h = ((baseHeight + flicker * WitchCauldronConstants.FIRE_FICKER_MULTIPLIER) * scale * heightMult * (baseIntensity + WitchCauldronConstants.FIRE_BASE_INTENSITY_OFFSET) * (WitchCauldronConstants.FIRE_NOISE_HEIGHT_OFFSET + noise)).toInt()
            for (dy in 0 until h) {
                val lick = (sin(time * PI * WitchCauldronConstants.FIRE_LICK_FREQ + dy * WitchCauldronConstants.FIRE_LICK_DY_FACTOR + dx * WitchCauldronConstants.FIRE_LICK_DX_FACTOR) * WitchCauldronConstants.FIRE_LICK_AMPLITUDE * scale * (dy.toFloat() / h.coerceAtLeast(1))).toInt()
                drawPixel(center + dx + lick, centerY - dy, color, pixelSize)
            }
        }
    }
}

private fun DrawScope.drawPixelCauldronBase(gridSize: Int, pixelSize: Float, bounceY: Int, scale: Float) {
    val centerX = gridSize / 2
    val centerY = gridSize / 2 + (WitchCauldronConstants.CAULDRON_CENTER_Y_OFFSET * scale).toInt() + bounceY
    val cauldronColor = Color(0xFF222222)

    val legWidth = (WitchCauldronConstants.LEG_WIDTH_MULT * scale).toInt().coerceAtLeast(WitchCauldronConstants.LEG_WIDTH_MIN)
    val legPosX = (WitchCauldronConstants.LEG_POS_X_MULT * scale).toInt()

    // Thicker, softer legs
    for (dx in -legPosX - legWidth / 2..-legPosX + legWidth / 2) {
        for (dy in (WitchCauldronConstants.LEG_Y_START_MULT * scale).toInt()..(WitchCauldronConstants.LEG_Y_END_MULT * scale).toInt()) {
            drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
        }
    }
    for (dx in legPosX - legWidth / 2..legPosX + legWidth / 2) {
        for (dy in (WitchCauldronConstants.LEG_Y_START_MULT * scale).toInt()..(WitchCauldronConstants.LEG_Y_END_MULT * scale).toInt()) {
            drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
        }
    }

    val radius = WitchCauldronConstants.CAULDRON_RADIUS_MULT * scale
    val radiusSq = radius * radius
    // Rounder body geometry
    for (dy in (WitchCauldronConstants.CAULDRON_DIY_MIN_MULT * scale).toInt()..(WitchCauldronConstants.CAULDRON_DIY_MAX_MULT * scale).toInt()) {
        for (dx in (WitchCauldronConstants.CAULDRON_DX_MIN_MULT * scale).toInt()..(WitchCauldronConstants.CAULDRON_DX_MAX_MULT * scale).toInt()) {
            val dist = dx * dx + (dy * WitchCauldronConstants.CAULDRON_ELLIPSE_Y_SCALE).let { it * it }
            if (dist < radiusSq) {
                drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
            }
        }
    }

    // Softer rim
    for (dx in (WitchCauldronConstants.CAULDRON_RIM_DX_MIN_MULT * scale).toInt()..(WitchCauldronConstants.CAULDRON_RIM_DX_MAX_MULT * scale).toInt()) {
        for (dy in (WitchCauldronConstants.CAULDRON_RIM_DY_MIN_MULT * scale).toInt()..(WitchCauldronConstants.CAULDRON_RIM_DY_MAX_MULT * scale).toInt()) {
            drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
        }
    }

    val reflectSize = (WitchCauldronConstants.CAULDRON_REFLECT_SIZE_MULT * scale).toInt().coerceAtLeast(1)
    for (dx in 0 until reflectSize) {
        for (dy in 0 until reflectSize) {
            drawPixel(centerX + (WitchCauldronConstants.CAULDRON_REFLECT_DX_OFFSET * scale).toInt() + dx, centerY + (WitchCauldronConstants.CAULDRON_REFLECT_DY_OFFSET * scale).toInt() + dy, Color.White.copy(alpha = WitchCauldronConstants.CAULDRON_REFLECT_ALPHA), pixelSize)
        }
    }
}

private fun DrawScope.drawBubblingLiquid(
    gridSize: Int,
    pixelSize: Float,
    liquidY: Int,
    color: Color,
    scale: Float,
    time: Float,
    bounceOffset: Float
) {
    val centerX = gridSize / 2
    val width = (WitchCauldronConstants.LIQUID_WIDTH_MULT * scale).toInt()
    val horizontalSway = (sin(time * PI * WitchCauldronConstants.LIQUID_HORIZ_SWAY_FREQ) * WitchCauldronConstants.LIQUID_HORIZ_SWAY_AMPLITUDE * scale).toInt()
    val sloshIntensity = if (bounceOffset < 0) abs(bounceOffset) * WitchCauldronConstants.LIQUID_SLOSH_INTENSITY_FACTOR else 0f

    for (dx in -width..width) {
        val edgeFactor = cos((dx.toFloat() / width) * (PI / 2).toFloat()).pow(WitchCauldronConstants.LIQUID_EDGE_FACTOR_EXPONENT)
        val wave = (sin(time * PI * WitchCauldronConstants.LIQUID_WAVE_FREQ + dx * WitchCauldronConstants.LIQUID_WAVE_DX_FACTOR) * (WitchCauldronConstants.LIQUID_WAVE_AMPLITUDE_BASE * scale + sloshIntensity) * edgeFactor).toInt()
        val centerJump = if (abs(dx) < width / 3) (sloshIntensity * WitchCauldronConstants.LIQUID_CENTER_JUMP_MULTIPLIER).toInt() else 0
        val surfaceY = liquidY + wave - centerJump

        val x = centerX + dx + horizontalSway
        for (dy in 0..(WitchCauldronConstants.LIQUID_DEPTH_MULT * scale).toInt()) {
            drawPixel(x, surfaceY + dy, color, pixelSize)
        }
        drawPixel(x, surfaceY, color.copy(alpha = WitchCauldronConstants.LIQUID_SURFACE_ALPHA), pixelSize)
    }
}

private fun DrawScope.drawPixelBubbles(
    gridSize: Int,
    pixelSize: Float,
    progress: Float,
    density: Int,
    scale: Float,
    liquidY: Int,
    liquidColor: Color
) {
    val centerX = gridSize / 2

    repeat(density) { i ->
        val bubbleRandom = kotlin.random.Random(i.toLong() * 1000L)

        val p = (progress + i.toFloat() / density) % 1.1f
        val spread = (WitchCauldronConstants.BUBBLE_SPREAD_MULT * scale).toInt()
        val bx = centerX + (bubbleRandom.nextInt(spread * 2) - spread)
        val by = liquidY - (p * WitchCauldronConstants.BUBBLE_RISE_HEIGHT_MULT * scale).toInt()

        val alpha = (1f - p).coerceAtLeast(0f).pow(WitchCauldronConstants.BUBBLE_ALPHA_EXPONENT)
        val bubbleBaseColor = liquidColor
        val bubbleColor = Color(
            red = (bubbleBaseColor.red * WitchCauldronConstants.BUBBLE_COLOR_BRIGHTNESS_FACTOR + WitchCauldronConstants.BUBBLE_COLOR_BASE_FACTOR).coerceAtMost(1f),
            green = (bubbleBaseColor.green * WitchCauldronConstants.BUBBLE_COLOR_BRIGHTNESS_FACTOR + WitchCauldronConstants.BUBBLE_COLOR_BASE_FACTOR).coerceAtMost(1f),
            blue = (bubbleBaseColor.blue * WitchCauldronConstants.BUBBLE_COLOR_BRIGHTNESS_FACTOR + WitchCauldronConstants.BUBBLE_COLOR_BASE_FACTOR).coerceAtMost(1f),
            alpha = alpha
        )

        if (by in 1 until liquidY) {
            val r = WitchCauldronConstants.BUBBLE_RADIUS
            for (dx in -r..r) {
                for (dy in -r..r) {
                    val distSq = dx * dx + dy * dy
                    if (distSq <= WitchCauldronConstants.BUBBLE_MAX_DIST_SQ) {
                        val pixelAlpha = if (distSq >= WitchCauldronConstants.BUBBLE_EDGE_DIST_SQ) alpha * WitchCauldronConstants.BUBBLE_EDGE_ALPHA_FACTOR else alpha
                        drawPixel(bx + dx, by + dy, bubbleColor.copy(alpha = pixelAlpha), pixelSize)
                    }
                }
            }
            drawPixel(bx - 1, by - 1, Color.White.copy(alpha = alpha * WitchCauldronConstants.BUBBLE_HIGHLIGHT_ALPHA_FACTOR), pixelSize)
        }
    }
}

private fun DrawScope.drawPixelIngredients(gridSize: Int, pixelSize: Float, progress: Float, scale: Float, liquidY: Int) {
    val centerX = gridSize / 2
    repeat(WitchCauldronConstants.INGREDIENT_COUNT) { i ->
        val p = progress % 1f
        val ix = centerX + (sin(i * WitchCauldronConstants.INGREDIENT_FREQ_BASE + p * WitchCauldronConstants.INGREDIENT_FREQ_SPEED) * WitchCauldronConstants.INGREDIENT_X_SPREAD_MULT * scale).toInt()
        val iy = (p * liquidY).toInt()

        val color = when (i) {
            0 -> Color.White
            1 -> Color(0xFFFF5722)
            else -> Color(0xFFFFEB3B)
        }

        for (dx in -WitchCauldronConstants.INGREDIENT_SIZE_HALF..WitchCauldronConstants.INGREDIENT_SIZE_HALF) {
            for (dy in -WitchCauldronConstants.INGREDIENT_SIZE_HALF..WitchCauldronConstants.INGREDIENT_SIZE_HALF) {
                drawPixel(ix + dx, iy + dy + WitchCauldronConstants.INGREDIENT_SIZE_OFFSET, color, pixelSize)
            }
        }
        drawPixel(ix, iy, Color.White, pixelSize)
    }
}

private fun DrawScope.drawPixelPowerStream(gridSize: Int, pixelSize: Float, alpha: Float, scale: Float, liquidY: Int) {
    val centerX = gridSize / 2
    val streamWidth = WitchCauldronConstants.POWER_STREAM_WIDTH

    if (liquidY <= 0) return
    for (dy in 0..liquidY) {
        val y = liquidY - dy
        val a = alpha * (1f - dy.toFloat() / liquidY)

        for (dx in -streamWidth / 2..streamWidth / 2) {
            val distFactor = 1f - abs(dx).toFloat() / (streamWidth / 2)
            val finalAlpha = a * distFactor.pow(WitchCauldronConstants.POWER_STREAM_ALPHA_EXPONENT)

            val color = if (abs(dx) < WitchCauldronConstants.POWER_STREAM_CORE_WIDTH) Color.Cyan else Color(0xFF00FF00)
            drawPixel(centerX + dx, y, color.copy(alpha = finalAlpha), pixelSize)
        }
    }
}

@Preview
@Composable
fun WitchCauldronPreview() {
    MaterialTheme {
        Surface(color = Color(0xFF121212)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    CauldronPreviewItem("IDLE", CauldronState.IDLE)
                    CauldronPreviewItem("THINKING", CauldronState.THINKING)
                }
                Row {
                    CauldronPreviewItem("SENDING", CauldronState.SENDING)
                    CauldronPreviewItem("RECEIVING", CauldronState.RECEIVING)
                }
            }
        }
    }
}

@Preview
@Composable
private fun CauldronPreviewItem(label: String, state: CauldronState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Text(label, color = Color.White, fontSize = 10.sp)
        WitchCauldron(state = state, gridSize = 64, modifier = Modifier.size(100.dp))
    }
}
