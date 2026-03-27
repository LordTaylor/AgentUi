// Animated pixel-art magic book — used as the main agent avatar in chat.
// 32×32 grid: floating book with flipping pages and pulsing rune glyphs.
package com.agentcore.ui.components.avatar

import androidx.compose.animation.core.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.*

private const val GRID = 32
private const val GRID_F = 32f

@Composable
fun PixelMagicBook(modifier: Modifier = Modifier.size(32.dp)) {
    val transition = rememberInfiniteTransition()

    val pageFlip by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart)
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val floatOffset by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Canvas(modifier = modifier) {
        val ps = size.minDimension / GRID
        val cx = GRID / 2
        val cy = (GRID / 2 + floatOffset).toInt()
        drawBookBody(cx, cy, ps, pageFlip, glowAlpha)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBookBody(
    cx: Int, cy: Int, ps: Float, pageFlip: Float, glowAlpha: Float
) {
    val c = AvatarColors
    // Outer glow ring
    for (dx in -6..6) for (dy in -8..8) {
        val d = sqrt((dx * dx + dy * dy * 0.7f).toDouble())
        if (d in 5.5..6.5) drawPixel(cx + dx, cy + dy, c.RUNE_GREEN.copy(alpha = glowAlpha * 0.25f), ps)
    }

    // Left page (ivory, trapezoid shape, dy offset by pageFlip sine)
    val leftLift = (sin(pageFlip * PI * 2) * 2).toInt()
    for (dx in -5..-1) for (dy in -4..3) {
        val edge = if (dx == -5) 1 else 0
        val topTrim = if (dy == -4 - leftLift + edge) 1 else 0
        drawPixel(cx + dx, cy + dy - leftLift / 2 + topTrim, c.BOOK_IVORY, ps)
    }

    // Right page
    val rightLift = (cos(pageFlip * PI * 2) * 2).toInt()
    for (dx in 1..5) for (dy in -4..3) {
        val edge = if (dx == 5) 1 else 0
        val topTrim = if (dy == -4 - rightLift + edge) 1 else 0
        drawPixel(cx + dx, cy + dy - rightLift / 2 + topTrim, c.BOOK_IVORY, ps)
    }

    // Book cover outline (black, slightly larger)
    for (dx in -5..5) for (dy in -5..4) {
        if (dx == -5 || dx == 5 || dy == -5 || dy == 4) drawPixel(cx + dx, cy + dy, c.BLACK, ps)
    }
    // Cover fill (dark brown)
    for (dx in -4..4) for (dy in -4..3) drawPixel(cx + dx, cy + dy, c.BOOK_BROWN, ps)
    // Spine (gold center line)
    for (dy in -4..3) drawPixel(cx, cy + dy, c.BOOK_GOLD, ps)
    drawPixel(cx, cy - 4, c.BOOK_GOLD, ps)

    // Rune glyphs (3 pixels on cover — left side)
    drawPixel(cx - 2, cy - 2, c.RUNE_GREEN.copy(alpha = glowAlpha), ps)
    drawPixel(cx - 3, cy,     c.RUNE_GREEN.copy(alpha = glowAlpha * 0.8f), ps)
    drawPixel(cx - 2, cy + 2, c.RUNE_GREEN.copy(alpha = glowAlpha), ps)
    // Rune glyphs — right side
    drawPixel(cx + 2, cy - 2, c.RUNE_GREEN.copy(alpha = glowAlpha * 0.7f), ps)
    drawPixel(cx + 3, cy,     c.RUNE_GREEN.copy(alpha = glowAlpha), ps)
    drawPixel(cx + 2, cy + 2, c.RUNE_GREEN.copy(alpha = glowAlpha * 0.8f), ps)

    // Corner clasps (gold)
    drawPixel(cx - 4, cy - 4, c.BOOK_GOLD, ps)
    drawPixel(cx + 4, cy - 4, c.BOOK_GOLD, ps)
    drawPixel(cx - 4, cy + 3, c.BOOK_GOLD, ps)
    drawPixel(cx + 4, cy + 3, c.BOOK_GOLD, ps)
}

@Preview
@Composable
private fun PixelMagicBookPreview() {
    PixelMagicBook(modifier = Modifier.size(64.dp))
}
