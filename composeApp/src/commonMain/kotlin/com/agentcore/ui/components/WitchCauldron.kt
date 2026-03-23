package com.agentcore.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.random.Random

enum class CauldronState {
    IDLE, SENDING, RECEIVING, THINKING
}

@Composable
fun WitchCauldron(
    state: CauldronState,
    modifier: Modifier = Modifier.size(200.dp)
) {
    val transition = rememberInfiniteTransition()
    
    // Fire animation
    val fireScale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Bouncing animation for THINKING state
    val bounceOffset by animateFloatAsState(
        targetValue = if (state == CauldronState.THINKING) -15f else 0f,
        animationSpec = if (state == CauldronState.THINKING) {
            infiniteRepeatable(
                animation = tween(200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(500)
        }
    )

    // Bubbles animation
    val bubbleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Ingredients animation for SENDING state
    val ingredientProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Power stream animation for RECEIVING state
    val powerAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        
        // 1. Draw Fire
        drawFire(centerX, centerY + 40f, fireScale)

        // 2. Draw Cauldron (with bounce)
        withTransform({
            translate(top = bounceOffset)
        }) {
            drawCauldronBody(centerX, centerY)
            
            // 3. Draw State Effects
            when (state) {
                CauldronState.IDLE -> {
                    drawBubbles(centerX, centerY, bubbleProgress, density = 3)
                    drawSteam(centerX, centerY, bubbleProgress)
                }
                CauldronState.THINKING -> {
                    drawBubbles(centerX, centerY, bubbleProgress, density = 8)
                }
                CauldronState.SENDING -> {
                    drawIngredients(centerX, centerY, ingredientProgress)
                    drawBubbles(centerX, centerY, bubbleProgress, density = 5)
                }
                CauldronState.RECEIVING -> {
                    drawPowerStream(centerX, centerY, powerAlpha)
                    drawBubbles(centerX, centerY, bubbleProgress, density = 10)
                }
            }
        }
    }
}

private fun DrawScope.drawFire(x: Float, y: Float, scale: Float) {
    val firePath = Path().apply {
        moveTo(x - 40f * scale, y)
        quadraticBezierTo(x, y - 60f * scale, x + 40f * scale, y)
        close()
    }
    drawPath(firePath, Color(0xFFFF4500), alpha = 0.6f)
    
    val innerFirePath = Path().apply {
        moveTo(x - 20f * scale, y - 5f)
        quadraticBezierTo(x, y - 40f * scale, x + 20f * scale, y - 5f)
        close()
    }
    drawPath(innerFirePath, Color(0xFFFFD700), alpha = 0.8f)
}

private fun DrawScope.drawCauldronBody(x: Float, y: Float) {
    // Legs
    drawRect(Color(0xFF1A1A1A), Offset(x - 45f, y + 30f), Size(10f, 20f))
    drawRect(Color(0xFF1A1A1A), Offset(x + 35f, y + 30f), Size(10f, 20f))

    // Main pot
    drawCircle(
        color = Color(0xFF2B2B2B),
        radius = 50f,
        center = Offset(x, y)
    )
    
    // Rim
    drawRoundRect(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(x - 55f, y - 40f),
        size = Size(110f, 15f),
        cornerRadius = CornerRadius(5f, 5f)
    )

    // Highlight
    drawArc(
        color = Color.White.copy(alpha = 0.1f),
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(x - 40f, y - 40f),
        size = Size(80f, 80f),
        style = Stroke(width = 4f)
    )
}

private fun DrawScope.drawBubbles(x: Float, y: Float, progress: Float, density: Int) {
    val random = Random(42) // Stable seed
    repeat(density) { i ->
        val individualProgress = (progress + i.toFloat() / density) % 1f
        val bx = x + (random.nextFloat() - 0.5f) * 80f
        val by = y - 30f - individualProgress * 100f
        val radius = 4f + random.nextFloat() * 6f
        val alpha = (1f - individualProgress) * 0.7f
        
        drawCircle(
            color = Color(0xFF00FF00),
            radius = radius,
            center = Offset(bx, by),
            alpha = alpha
        )
    }
}

private fun DrawScope.drawSteam(x: Float, y: Float, progress: Float) {
    repeat(3) { i ->
        val individualProgress = (progress + i.toFloat() / 3f) % 1f
        val sx = x + (i - 1) * 30f
        val sy = y - 50f - individualProgress * 80f
        val alpha = (1f - individualProgress) * 0.3f
        
        val steamPath = Path().apply {
            moveTo(sx, sy)
            relativeQuadraticBezierTo(10f, -10f, 0f, -20f)
            relativeQuadraticBezierTo(-10f, -10f, 0f, -20f)
        }
        drawPath(
            path = steamPath,
            color = Color.White,
            alpha = alpha,
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawIngredients(x: Float, y: Float, progress: Float) {
    val random = Random(123)
    repeat(5) { i ->
        val individualProgress = (progress + i.toFloat() / 5f) % 1f
        val ix = x + (random.nextFloat() - 0.5f) * 120f
        val iy = -50f + individualProgress * (y + 50f)
        val rotation = individualProgress * 360f
        
        withTransform({
            rotate(rotation, Offset(ix, iy))
        }) {
            when (i % 4) {
                0 -> drawEye(ix, iy)
                1 -> drawWing(ix, iy)
                2 -> drawGnome(ix, iy)
                else -> drawJar(ix, iy)
            }
        }
    }
}

private fun DrawScope.drawEye(x: Float, y: Float) {
    drawCircle(Color.White, radius = 8f, center = Offset(x, y))
    drawCircle(Color.Black, radius = 3f, center = Offset(x, y))
}

private fun DrawScope.drawWing(x: Float, y: Float) {
    val path = Path().apply {
        moveTo(x, y)
        lineTo(x + 10f, y - 5f)
        lineTo(x + 5f, y + 10f)
        close()
    }
    drawPath(path, Color(0xFF8B4513))
}

private fun DrawScope.drawGnome(x: Float, y: Float) {
    drawRect(Color(0xFF4682B4), Offset(x - 4f, y), Size(8f, 10f))
    val hat = Path().apply {
        moveTo(x - 5f, y)
        lineTo(x + 5f, y)
        lineTo(x, y - 10f)
        close()
    }
    drawPath(hat, Color.Red)
}

private fun DrawScope.drawJar(x: Float, y: Float) {
    drawRoundRect(Color(0xFFA9A9A9), Offset(x - 5f, y - 7f), Size(10f, 14f), CornerRadius(2f, 2f))
    drawRect(Color(0xFF696969), Offset(x - 6f, y - 9f), Size(12f, 3f))
}

private fun DrawScope.drawPowerStream(x: Float, y: Float, alpha: Float) {
    val beamWidth = 40f
    val beamHeight = y + 100f
    
    // Gradient beam
    val brush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color(0xFF00FF00), Color(0xFF00FF00)),
        startY = y - beamHeight,
        endY = y - 40f
    )
    
    drawRect(
        brush = brush,
        topLeft = Offset(x - beamWidth / 2, y - beamHeight),
        size = Size(beamWidth, beamHeight),
        alpha = alpha
    )
    
    // Particles
    val random = Random(99)
    repeat(15) {
        val px = x + (random.nextFloat() - 0.5f) * 60f
        val py = y - 40f - random.nextFloat() * 200f
        drawCircle(Color(0xFFCCFFCC), radius = 2f, center = Offset(px, py), alpha = alpha)
    }
}
