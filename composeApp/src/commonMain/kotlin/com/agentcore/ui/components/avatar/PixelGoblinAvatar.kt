// Pixel-art goblin avatars for agent hierarchy in chat.
// isLeader=true → Goblin Lord (crown, staff, purple robe, glowing red eyes).
// isLeader=false → Sub-Goblin (smaller, green tunic, blinking yellow eyes).
package com.agentcore.ui.components.avatar

import androidx.compose.animation.core.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PixelGoblinAvatar(
    isLeader: Boolean,
    modifier: Modifier = Modifier.size(32.dp)
) {
    val transition = rememberInfiniteTransition()

    // Menacing slow glow: 1400ms, holds at peak — more sinister than fast 600ms pulse
    val eyePulse by transition.animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0.6f at 0
                1.0f at 500
                1.0f at 1100   // hold at full brightness
                0.6f at 1400
            }
        )
    )
    val blinkOpen by transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                1f at 0
                1f at 2000
                0f at 2100
                1f at 2400
            }
        )
    )
    // Idle body sway: two independent axes at different frequencies → organic noise-like motion
    // Pixel-snapped to ±1px so pixel grid is never broken
    val swayX by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val swayY by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val ps = size.minDimension / 32f
        // Round to nearest pixel — sub-pixel positioning breaks pixel art grid
        val cx = 16 + kotlin.math.round(swayX).toInt()
        val cy = 16 + kotlin.math.round(swayY).toInt()
        if (isLeader) drawGoblinLord(cx, cy, ps, eyePulse)
        else drawSubGoblin(cx, cy, ps, blinkOpen)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGoblinLord(
    cx: Int, cy: Int, ps: Float, eyePulse: Float
) {
    val c = AvatarColors

    // Crown (gold, 3 teeth)
    for (dx in -4..4) drawPixel(cx + dx, cy - 8, c.GOLD, ps)          // crown base
    drawPixel(cx - 3, cy - 9, c.GOLD, ps); drawPixel(cx - 3, cy - 10, c.GOLD, ps)  // tooth L
    drawPixel(cx,     cy - 9, c.GOLD, ps); drawPixel(cx,     cy - 10, c.GOLD, ps)  // tooth C
    drawPixel(cx + 3, cy - 9, c.GOLD, ps); drawPixel(cx + 3, cy - 10, c.GOLD, ps) // tooth R

    // Head outline
    for (dx in -4..4) for (dy in -7..0) {
        if (dx == -4 || dx == 4 || dy == -7 || dy == 0) drawPixel(cx + dx, cy + dy, c.BLACK, ps)
    }
    // Head fill — 3-tone shading: highlight left | mid-green | shadow right
    for (dx in -3..3) for (dy in -6..-1) {
        val color = when (dx) { -3 -> c.GOBLIN_LIGHT; 3 -> c.GOBLIN_DARK; else -> c.GOBLIN_GREEN }
        drawPixel(cx + dx, cy + dy, color, ps)
    }

    // Angry eyebrows: \ and / shapes (inner corners lower = menacing)
    drawPixel(cx - 3, cy - 6, c.GOBLIN_DARK, ps); drawPixel(cx - 2, cy - 5, c.GOBLIN_DARK, ps)
    drawPixel(cx + 2, cy - 5, c.GOBLIN_DARK, ps); drawPixel(cx + 3, cy - 6, c.GOBLIN_DARK, ps)

    // Eyes (red, pulsing)
    val eyeColor = c.EYE_RED.copy(alpha = eyePulse)
    drawPixel(cx - 2, cy - 4, eyeColor, ps); drawPixel(cx - 1, cy - 4, eyeColor, ps)
    drawPixel(cx + 1, cy - 4, eyeColor, ps); drawPixel(cx + 2, cy - 4, eyeColor, ps)
    // Nose (nostrils)
    drawPixel(cx - 1, cy - 3, c.GOBLIN_DARK, ps); drawPixel(cx + 1, cy - 3, c.GOBLIN_DARK, ps)
    // Smile corners + white fangs inside
    drawPixel(cx - 2, cy - 1, c.BLACK, ps)
    drawPixel(cx - 1, cy,     Color.White, ps)   // left fang
    drawPixel(cx,     cy,     c.BLACK, ps)
    drawPixel(cx + 1, cy,     Color.White, ps)   // right fang
    drawPixel(cx + 2, cy - 1, c.BLACK, ps)

    // Robe (purple trapezoid)
    for (dx in -4..4) for (dy in 1..6) {
        val width = 4 - (dy - 1) / 3
        if (kotlin.math.abs(dx) <= width) drawPixel(cx + dx, cy + dy, c.ROBE_PURPLE, ps)
    }
    // Robe highlight
    for (dy in 1..4) drawPixel(cx - 3, cy + dy, c.ROBE_LIGHT, ps)

    // Staff (right side, gold top star)
    for (dy in -6..6) drawPixel(cx + 6, cy + dy, c.GOLD, ps)
    drawPixel(cx + 5, cy - 7, c.GOLD, ps); drawPixel(cx + 6, cy - 8, c.GOLD, ps)
    drawPixel(cx + 7, cy - 7, c.GOLD, ps); drawPixel(cx + 6, cy - 6, c.GOLD, ps)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSubGoblin(
    cx: Int, cy: Int, ps: Float, blinkOpen: Float
) {
    val c = AvatarColors

    // Head outline (smaller)
    for (dx in -3..3) for (dy in -5..1) {
        if (dx == -3 || dx == 3 || dy == -5 || dy == 1) drawPixel(cx + dx, cy + dy, c.BLACK, ps)
    }
    // Head fill — 2-tone: bright left/center | mid-green right shadow
    for (dx in -2..2) for (dy in -4..0) {
        val color = if (dx == 2) c.GOBLIN_GREEN else c.GOBLIN_LIGHT
        drawPixel(cx + dx, cy + dy, color, ps)
    }

    // Pointy ears with highlight
    drawPixel(cx - 4, cy - 3, c.GOBLIN_LIGHT, ps)
    drawPixel(cx + 4, cy - 3, c.GOBLIN_LIGHT, ps)
    drawPixel(cx - 4, cy - 4, c.GOBLIN_GREEN, ps)   // ear tip
    drawPixel(cx + 4, cy - 4, c.GOBLIN_GREEN, ps)

    // Worried eyebrows: /\ shape (inner corners high = scared/submissive)
    drawPixel(cx - 2, cy - 4, c.GOBLIN_DARK, ps); drawPixel(cx - 1, cy - 5, c.GOBLIN_DARK, ps)
    drawPixel(cx + 1, cy - 5, c.GOBLIN_DARK, ps); drawPixel(cx + 2, cy - 4, c.GOBLIN_DARK, ps)

    // Eyes (yellow, blinking)
    if (blinkOpen > 0.5f) {
        val eyeAlpha = ((blinkOpen - 0.5f) * 2f).coerceIn(0f, 1f)
        drawPixel(cx - 2, cy - 3, c.EYE_YELLOW.copy(alpha = eyeAlpha), ps)
        drawPixel(cx + 2, cy - 3, c.EYE_YELLOW.copy(alpha = eyeAlpha), ps)
    }
    // Nose
    drawPixel(cx, cy - 2, c.GOBLIN_DARK, ps)
    // Toothy grin with center fang
    drawPixel(cx - 1, cy, c.BLACK, ps)
    drawPixel(cx,     cy, Color.White, ps)   // center fang
    drawPixel(cx + 1, cy, c.BLACK, ps)

    // Tunic (green)
    for (dx in -3..3) for (dy in 2..6) {
        val w = 3 - (dy - 2) / 4
        if (kotlin.math.abs(dx) <= w + 1) drawPixel(cx + dx, cy + dy, c.SUB_CLOTHES, ps)
    }
    // Belt
    for (dx in -3..3) drawPixel(cx + dx, cy + 3, c.GOLD.copy(alpha = 0.7f), ps)
}

@Preview
@Composable
private fun GoblinAvatarPreview() {
    Row {
        PixelGoblinAvatar(isLeader = true,  modifier = Modifier.size(64.dp))
        PixelGoblinAvatar(isLeader = false, modifier = Modifier.size(64.dp))
    }
}
