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
    const val DEFAULT_GRID_SIZE = 64
    const val FIRE_FRAME_COUNT = 4
    const val BUBBLE_DENSITY_IDLE = 8
    const val BUBBLE_DENSITY_SENDING = 16
    const val BUBBLE_DENSITY_RECEIVING = 24
    const val BUBBLE_DENSITY_THINKING = 12
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
        targetValue = if (state == CauldronState.THINKING) -6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val bubbleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
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
            // liquidY to poziom wlotu kociołka
            val liquidY = (gridSize / 2 - (22 * scale) + bounceY).toInt()
            val liquidColor = liquidColors[state] ?: Color(0xFF1B5E20)

            val offset1 = (sin(fireTime * PI * 2.0) * 8 * scale).toInt()
            val offset2 = (sin(fireTime * PI * 2.5) * 8 * scale).toInt()

            // 1. Ogień z tyłu
            drawPixelFire(gridSize, pixelSize, fireFrame, offset1, scale, 0.82f)
            // 3. Ciecz "bulgocząca" na wierzchu kociołka
            drawBubblingLiquid(gridSize, pixelSize, liquidY, liquidColor, scale, fireTime)
            // 2. Kociołek (Sam korpus, bez cieczy w środku)
            drawPixelCauldronBase(gridSize, pixelSize, bounceY, scale)

            // 4. Efekty stanu (Bąbelki lecą Z cieczy)
            val density = when(state) {
                CauldronState.IDLE -> WitchCauldronConstants.BUBBLE_DENSITY_IDLE
                CauldronState.SENDING -> WitchCauldronConstants.BUBBLE_DENSITY_SENDING
                CauldronState.RECEIVING -> WitchCauldronConstants.BUBBLE_DENSITY_RECEIVING
                CauldronState.THINKING -> WitchCauldronConstants.BUBBLE_DENSITY_THINKING
            }

            drawPixelBubbles(gridSize, pixelSize, bubbleProgress, bounceY, density, scale, liquidY, liquidColor)

            if (state == CauldronState.SENDING) {
                drawPixelIngredients(gridSize, pixelSize, ingredientProgress, scale, liquidY)
            }
            if (state == CauldronState.RECEIVING) {
                drawPixelPowerStream(gridSize, pixelSize, pulseAlpha, scale, liquidY)
            }

            // 5. Ogień z przodu
            drawPixelFire(gridSize, pixelSize, fireFrame, offset2, scale, 0.86f)
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

private fun DrawScope.drawPixelFire(gridSize: Int, pixelSize: Float, frame: Int, offset: Int, scale: Float, verticalPos: Float) {
    val centerX = gridSize / 2
    val centerY = (gridSize * verticalPos).toInt()
    val center = centerX + offset
    val fireHalfWidth = (10 * scale).toInt().coerceAtLeast(1)

    for (dx in -fireHalfWidth..fireHalfWidth) {
        val absDx = abs(dx)
        val relDx = absDx / scale
        val flicker = frame % WitchCauldronConstants.FIRE_FRAME_COUNT

        val layers = listOf(
            Triple(Color(0xFFFF4500), if (relDx <= 8) 10 else 4, 1.0f),
            Triple(Color(0xFFFFD700), if (relDx <= 5) 6 else 1, 0.8f)
        )

        for ((color, baseHeight, heightMult) in layers) {
            val h = ((baseHeight + flicker * 2) * scale * heightMult).toInt()
            for (dy in 0 until h) {
                drawPixel(center + dx, centerY - dy, color, pixelSize)
            }
        }
    }
}

private fun DrawScope.drawPixelCauldronBase(gridSize: Int, pixelSize: Float, bounceY: Int, scale: Float) {
    val centerX = gridSize / 2
    val centerY = gridSize / 2 + (8 * scale).toInt() + bounceY
    val cauldronColor = Color(0xFF222222) // Jednolity ciemny kolor

    // Nogi
    val legOffsetsX = listOf(-20, -19, -18, 17, 18, 19).map { (it * scale).toInt() }
    for (dx in legOffsetsX) {
        for (dy in (24 * scale).toInt()..(27 * scale).toInt()) {
            drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
        }
    }

    // Korpus
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

    // Obrzeże (Rim)
    for (dx in (-38 * scale).toInt()..(37 * scale).toInt()) {
        for (dy in (-30 * scale).toInt()..(-24 * scale).toInt()) {
            drawPixel(centerX + dx, centerY + dy, cauldronColor, pixelSize)
        }
    }

    // Odblask (jedyny detal)
    val reflectSize = (5 * scale).toInt().coerceAtLeast(1)
    for (dx in 0 until reflectSize) {
        for (dy in 0 until reflectSize) {
            drawPixel(centerX - (22 * scale).toInt() + dx, centerY - (6 * scale).toInt() + dy, Color.White.copy(alpha = 0.2f), pixelSize)
        }
    }
}

private fun DrawScope.drawBubblingLiquid(gridSize: Int, pixelSize: Float, liquidY: Int, color: Color, scale: Float, time: Float) {
    val centerX = gridSize / 2
    val width = (30 * scale).toInt()
    
    for (dx in -width..width) {
        // Efekt falowania powierzchni
        val wave = (sin(time * PI * 10 + dx * 0.5) * 2 * scale).toInt()
        val surfaceY = liquidY + wave
        
        // Rysujemy wypełnienie wlotu
        for (dy in 0..(6 * scale).toInt()) {
            drawPixel(centerX + dx, surfaceY + dy, color, pixelSize)
        }
        
        // Jaśniejszy "blask" na szczycie bąbelków cieczy
        drawPixel(centerX + dx, surfaceY, color.copy(alpha = 0.8f), pixelSize)
    }
}

private fun DrawScope.drawPixelBubbles(gridSize: Int, pixelSize: Float, progress: Float, bounceY: Int, density: Int, scale: Float, liquidY: Int, liquidColor: Color) {
    val random = kotlin.random.Random((progress * 1000).toInt())
    val centerX = gridSize / 2

    repeat(density) { i ->
        val p = (progress + i.toFloat() / density) % 1f
        val spread = (28 * scale).toInt()
        val bx = centerX + (random.nextInt(spread * 2) - spread)
        val by = liquidY - (p * 40 * scale).toInt()

        val alpha = 1f - p
        val bubbleColor = if (i % 2 == 0) Color(0xFFCCFF90) else liquidColor.copy(alpha = 0.7f)

        if (by < liquidY && by > 0) {
            // "Okrągły" bąbelek w pixel-art (krzyżyk 3x3)
            drawPixel(bx, by, bubbleColor.copy(alpha = alpha), pixelSize) // środek
            drawPixel(bx - 1, by, bubbleColor.copy(alpha = alpha * 0.6f), pixelSize) // lewo
            drawPixel(bx + 1, by, bubbleColor.copy(alpha = alpha * 0.6f), pixelSize) // prawo
            drawPixel(bx, by - 1, bubbleColor.copy(alpha = alpha * 0.6f), pixelSize) // góra
            drawPixel(bx, by + 1, bubbleColor.copy(alpha = alpha * 0.6f), pixelSize) // dół
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
            0 -> Color.White; 1 -> Color(0xFF8B4513); else -> Color.Red
        }
        drawPixel(ix, iy, color, pixelSize)
    }
}

private fun DrawScope.drawPixelPowerStream(gridSize: Int, pixelSize: Float, alpha: Float, scale: Float, liquidY: Int) {
    val centerX = gridSize / 2
    for (dy in 0..(liquidY)) {
        val y = liquidY - dy
        val a = alpha * (1f - dy.toFloat() / liquidY)
        drawPixel(centerX, y, Color.Green.copy(alpha = a), pixelSize)
        drawPixel(centerX - 1, y, Color.Green.copy(alpha = a * 0.5f), pixelSize)
        drawPixel(centerX + 1, y, Color.Green.copy(alpha = a * 0.5f), pixelSize)
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
