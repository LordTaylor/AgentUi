package com.agentcore.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.animation.core.*
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
    const val FIRE_FRAME_COUNT = 4
    const val BUBBLE_DENSITY_IDLE = 6
    const val BUBBLE_DENSITY_SENDING = 12
    const val BUBBLE_DENSITY_RECEIVING = 18
    const val BUBBLE_DENSITY_THINKING = 10
    const val SCALE_MIN = 0.1f
}

enum class CauldronState {
    IDLE, SENDING, RECEIVING, THINKING
}

@Composable
fun WitchCauldron(
    state: CauldronState,
    modifier: Modifier = Modifier.size(200.dp),
    gridSize: Int = 64,
    liquidColors: Map<CauldronState, Color> = mapOf(
        CauldronState.IDLE to Color(0xFF2E7D32),
        CauldronState.SENDING to Color(0xFF4CAF50),
        CauldronState.RECEIVING to Color(0xFF00FF00),
        CauldronState.THINKING to Color(0xFF81C784)
    ),
) {
    val transition = rememberInfiniteTransition()

    val fireFrame by transition.animateValue(
        initialValue = 0,
        targetValue = 7,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val fireTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val bounceOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (state == CauldronState.THINKING) -7f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val bubbleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing), // Wolno - 15 sekund
            repeatMode = RepeatMode.Restart
        )
    )

    val ingredientProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val pixelSize = size.minDimension / gridSize
        val scale = gridSize.toFloat() / 128f
        
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
            
            // 3. Kociołek
            drawPixelCauldronBase(gridSize, pixelSize, bounceY, scale)

            // 4. Efekty stanu
            val density = when(state) {
                CauldronState.IDLE -> WitchCauldronConstants.BUBBLE_DENSITY_IDLE
                CauldronState.SENDING -> WitchCauldronConstants.BUBBLE_DENSITY_SENDING
                CauldronState.RECEIVING -> WitchCauldronConstants.BUBBLE_DENSITY_RECEIVING
                CauldronState.THINKING -> WitchCauldronConstants.BUBBLE_DENSITY_THINKING
            }

            drawPixelBubbles(gridSize, pixelSize, bubbleProgress, density, scale, liquidY, liquidColor)

            if (state == CauldronState.SENDING) {
                drawPixelIngredients(gridSize, pixelSize, ingredientProgress, scale, liquidY)
            }
            if (state == CauldronState.RECEIVING) {
                drawPixelPowerStream(gridSize, pixelSize, pulseAlpha, scale, liquidY)
            }

            // 5. Ogień z przodu
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
    val fireHalfWidth = (48 * scale).toInt().coerceAtLeast(1)

    for (dx in -fireHalfWidth..fireHalfWidth) {
        val baseIntensity = cos((dx.toFloat() / fireHalfWidth) * (PI / 2).toFloat()).pow(1.2f)
        val noise = (sin(time * PI * 12 + dx * 0.3) * 0.4 + 
                    cos(time * PI * 5 - dx * 0.5) * 0.3 +
                    sin(dx * 0.8) * 0.3)
        val flicker = (frame % WitchCauldronConstants.FIRE_FRAME_COUNT).toFloat()

        val layers = listOf(
            Triple(Color(0xFF8B0000).copy(alpha = 0.4f), 14f, 1.4f),
            Triple(Color(0xFFFF4500), 10f, 1.2f),
            Triple(Color(0xFFFFD700), 6f, 0.9f)
        )

        for ((color, baseHeight, heightMult) in layers) {
            val h = ((baseHeight + flicker * 2) * scale * heightMult * (baseIntensity + 0.3f) * (1.2 + noise)).toInt()
            for (dy in 0 until h) {
                val lick = (sin(time * PI * 6 + dy * 0.2 + dx * 0.1) * 2.5 * scale * (dy.toFloat() / h.coerceAtLeast(1))).toInt()
                drawPixel(center + dx + lick, centerY - dy, color, pixelSize)
            }
        }
    }
}

private fun DrawScope.drawPixelCauldronBase(gridSize: Int, pixelSize: Float, bounceY: Int, scale: Float) {
    val centerX = gridSize / 2
    val centerY = gridSize / 2 + (8 * scale).toInt() + bounceY
    val cauldronColor = Color(0xFF222222)

    val legWidth = (8 * scale).toInt().coerceAtLeast(2)
    val legPosX = (20 * scale).toInt()
    
    for (dx in -legPosX - legWidth / 2..-legPosX + legWidth / 2) {
        for (dy in (24 * scale).toInt()..(34 * scale).toInt()) {
            drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
        }
    }
    for (dx in legPosX - legWidth / 2..legPosX + legWidth / 2) {
        for (dy in (24 * scale).toInt()..(34 * scale).toInt()) {
            drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
        }
    }

    val radius = 40 * scale
    val radiusSq = radius * radius
    for (dy in (-28 * scale).toInt()..(32 * scale).toInt()) {
        for (dx in (-44 * scale).toInt()..(44 * scale).toInt()) {
            val dist = dx * dx + (dy * 1.3).let { it * it }
            if (dist < radiusSq) {
                drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
            }
        }
    }

    for (dx in (-38 * scale).toInt()..(37 * scale).toInt()) {
        for (dy in (-32 * scale).toInt()..(-24 * scale).toInt()) {
            drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
        }
    }

    val reflectSize = (5 * scale).toInt().coerceAtLeast(1)
    for (dx in 0 until reflectSize) {
        for (dy in 0 until reflectSize) {
            drawPixel(centerX - (24 * scale).toInt() + dx, centerY - (8 * scale).toInt() + dy, Color.White.copy(alpha = 0.15f), pixelSize)
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
    val width = (34 * scale).toInt()
    val horizontalSway = (sin(time * PI * 2) * 2 * scale).toInt()
    val sloshIntensity = if (bounceOffset < 0) abs(bounceOffset) * 0.7f else 0f
    
    for (dx in -width..width) {
        val edgeFactor = cos((dx.toFloat() / width) * (PI / 2).toFloat()).pow(0.5f)
        val wave = (sin(time * PI * 8 + dx * 0.2) * (5 * scale + sloshIntensity) * edgeFactor).toInt()
        val centerJump = if (abs(dx) < width / 3) (sloshIntensity * 1.5f).toInt() else 0
        val surfaceY = liquidY + wave - centerJump
        
        val x = centerX + dx + horizontalSway
        // Rysujemy ciecz tylko w obrębie obręczy (rim), ograniczając dy
        for (dy in 0..(6 * scale).toInt()) {
            drawPixel(x, surfaceY + dy, color, pixelSize)
        }
        drawPixel(x, surfaceY, color.copy(alpha = 0.9f), pixelSize)
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
    val random = kotlin.random.Random((progress * 500).toInt())
    val centerX = gridSize / 2

    repeat(density) { i ->
        val p = (progress + i.toFloat() / density) % 1f
        val spread = (30 * scale).toInt()
        val bx = centerX + (random.nextInt(spread * 2) - spread)
        val by = liquidY - (p * 60 * scale).toInt()

        val alpha = (1f - p).pow(0.7f)
        val bubbleBaseColor = liquidColor
        val bubbleColor = Color(
            red = (bubbleBaseColor.red * 0.8f + 0.2f).coerceAtMost(1f),
            green = (bubbleBaseColor.green * 0.8f + 0.2f).coerceAtMost(1f),
            blue = (bubbleBaseColor.blue * 0.8f + 0.2f).coerceAtMost(1f),
            alpha = alpha
        )

        if (by in 1 until liquidY) {
            for (dx in -2..2) {
                for (dy in -2..2) {
                    val distSq = dx * dx + dy * dy
                    if (distSq <= 6) { 
                        val pixelAlpha = if (distSq >= 5) alpha * 0.4f else alpha
                        drawPixel(bx + dx, by + dy, bubbleColor.copy(alpha = pixelAlpha), pixelSize)
                    }
                }
            }
            drawPixel(bx - 1, by - 1, Color.White.copy(alpha = alpha * 0.5f), pixelSize)
        }
    }
}

private fun DrawScope.drawPixelIngredients(gridSize: Int, pixelSize: Float, progress: Float, scale: Float, liquidY: Int) {
    val centerX = gridSize / 2
    repeat(3) { i ->
        val p = progress % 1f
        val ix = centerX + (sin(i * 2.0 + p * 5) * 15 * scale).toInt()
        val iy = (p * liquidY).toInt()

        val color = when (i) {
            0 -> Color.White
            1 -> Color(0xFFFF5722) // Jaskrawy pomarańcz
            else -> Color(0xFFFFEB3B) // Jaskrawy żółty
        }
        
        // Większe składniki (3x3 z rdzeniem)
        for (dx in -1..1) {
            for (dy in -1..1) {
                drawPixel(ix + dx, iy + dy, color, pixelSize)
            }
        }
        // Biały błysk dla widoczności
        drawPixel(ix, iy, Color.White, pixelSize)
    }
}

private fun DrawScope.drawPixelPowerStream(gridSize: Int, pixelSize: Float, alpha: Float, scale: Float, liquidY: Int) {
    val centerX = gridSize / 2
    // Szeroki strumień (dopasowany do wlotu kociołka)
    val streamWidth = (32 * scale).toInt()
    
    for (dy in 0..(liquidY)) {
        val y = liquidY - dy
        val a = alpha * (1f - dy.toFloat() / liquidY)
        
        for (dx in -streamWidth / 2..streamWidth / 2) {
            val distFactor = 1f - abs(dx).toFloat() / (streamWidth / 2)
            val finalAlpha = a * distFactor.pow(1.5f)
            
            // Biały rdzeń, zielone brzegi
            val color = if (abs(dx) < 2) Color.White else Color(0xFF00FF00)
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

@Composable
private fun CauldronPreviewItem(label: String, state: CauldronState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Text(label, color = Color.White, fontSize = 10.sp)
        WitchCauldron(state = state, gridSize = 64, modifier = Modifier.size(100.dp))
    }
}
