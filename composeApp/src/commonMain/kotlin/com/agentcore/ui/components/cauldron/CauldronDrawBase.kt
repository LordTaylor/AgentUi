// Draws the cauldron body with cartoon outline, rounded handles, highlights, rivets,
// and the animated stirring spoon that rotates around the liquid surface.
package com.agentcore.ui.components.cauldron

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*

internal fun DrawScope.drawPixelCauldronBase(
    gridSize: Int,
    pixelSize: Float,
    bounceY: Int,
    scale: Float,
    palette: CauldronBodyPalette,
) {
    val c = WitchCauldronConstants
    val centerX = gridSize / 2
    val centerY = gridSize / 2 + (c.CAULDRON_CENTER_Y_OFFSET * scale).toInt() + bounceY

    drawLegs(centerX, centerY, scale, pixelSize, palette.outline, palette.body)
    drawBody(centerX, centerY, scale, pixelSize, palette.outline, palette.body)
    drawRim(centerX, centerY, scale, pixelSize, palette.outline, palette.body, palette.rim)
    drawHandles(centerX, centerY, scale, pixelSize, palette.outline, palette.body)
    drawHighlightAndRivets(centerX, centerY, scale, pixelSize, palette)
}

private fun DrawScope.drawLegs(
    cx: Int, cy: Int, scale: Float, pixelSize: Float,
    outline: Color, fill: Color
) {
    val c = WitchCauldronConstants
    val legW = (c.LEG_WIDTH_MULT * scale).toInt().coerceAtLeast(c.LEG_WIDTH_MIN)
    val legX = (c.LEG_POS_X_MULT * scale).toInt()
    val yStart = (c.LEG_Y_START_MULT * scale).toInt()
    val yEnd = (c.LEG_Y_END_MULT * scale).toInt()

    for (side in listOf(-legX, legX)) {
        for (dx in -legW / 2 - 1..legW / 2 + 1) for (dy in yStart - 1..yEnd + 1) {
            drawPixel(cx + side + dx, cy + dy, outline, pixelSize)
        }
        for (dx in -legW / 2..legW / 2) for (dy in yStart..yEnd) {
            drawPixel(cx + side + dx, cy + dy, fill, pixelSize)
        }
    }
}

private fun DrawScope.drawBody(
    cx: Int, cy: Int, scale: Float, pixelSize: Float,
    outline: Color, fill: Color
) {
    val c = WitchCauldronConstants
    val radius = c.CAULDRON_RADIUS_MULT * scale
    val outlineRadius = radius + c.CAULDRON_OUTLINE_EXTRA * scale
    val outlineRSq = outlineRadius * outlineRadius
    val radiusSq = radius * radius
    val yScale = c.CAULDRON_ELLIPSE_Y_SCALE
    val dyMin = (c.CAULDRON_DIY_MIN_MULT * scale).toInt()
    val dyMax = (c.CAULDRON_DIY_MAX_MULT * scale).toInt()
    val dxMin = (c.CAULDRON_DX_MIN_MULT * scale).toInt()
    val dxMax = (c.CAULDRON_DX_MAX_MULT * scale).toInt()

    for (dy in dyMin - 2..dyMax + 2) for (dx in dxMin - 2..dxMax + 2) {
        val dist = dx * dx + (dy * yScale) * (dy * yScale)
        if (dist < outlineRSq) drawPixel(cx + dx, cy + dy, outline, pixelSize)
    }
    for (dy in dyMin..dyMax) for (dx in dxMin..dxMax) {
        val dist = dx * dx + (dy * yScale) * (dy * yScale)
        if (dist < radiusSq) drawPixel(cx + dx, cy + dy, fill, pixelSize)
    }

    // Reflection highlight
    val reflSize = (c.CAULDRON_REFLECT_SIZE_MULT * scale).toInt().coerceAtLeast(1)
    for (dx in 0 until reflSize) for (dy in 0 until reflSize) {
        drawPixel(
            cx + (c.CAULDRON_REFLECT_DX_OFFSET * scale).toInt() + dx,
            cy + (c.CAULDRON_REFLECT_DY_OFFSET * scale).toInt() + dy,
            Color.White.copy(alpha = c.CAULDRON_REFLECT_ALPHA), pixelSize
        )
    }
}

private fun DrawScope.drawRim(
    cx: Int, cy: Int, scale: Float, pixelSize: Float,
    outline: Color, fill: Color, highlight: Color
) {
    val c = WitchCauldronConstants
    val dxMin = (c.CAULDRON_RIM_DX_MIN_MULT * scale).toInt()
    val dxMax = (c.CAULDRON_RIM_DX_MAX_MULT * scale).toInt()
    val dyMin = (c.CAULDRON_RIM_DY_MIN_MULT * scale).toInt()
    val dyMax = (c.CAULDRON_RIM_DY_MAX_MULT * scale).toInt()

    for (dx in dxMin - 1..dxMax + 1) for (dy in dyMin - 1..dyMax + 1) {
        drawPixel(cx + dx, cy + dy, outline, pixelSize)
    }
    for (dx in dxMin..dxMax) for (dy in dyMin..dyMax) {
        drawPixel(cx + dx, cy + dy, fill, pixelSize)
    }
    // Rim highlight strip (top edge)
    for (dx in dxMin..dxMax) {
        drawPixel(cx + dx, cy + dyMin, highlight, pixelSize)
    }
}

private fun DrawScope.drawHandles(
    cx: Int, cy: Int, scale: Float, pixelSize: Float,
    outline: Color, fill: Color
) {
    val c = WitchCauldronConstants
    val offsetX = (c.HANDLE_OFFSET_X_MULT * scale).toInt()
    val offsetY = (c.HANDLE_OFFSET_Y_MULT * scale).toInt()
    val r = (c.HANDLE_RADIUS * scale).toInt().coerceAtLeast(2)
    val rSq = r * r
    val rOutSq = (r + 1) * (r + 1)

    for (side in listOf(-offsetX, offsetX)) {
        val hx = cx + side
        val hy = cy + offsetY
        for (dx in -r - 1..r + 1) for (dy in -r - 1..r + 1) {
            val d = dx * dx + dy * dy
            if (d < rOutSq) drawPixel(hx + dx, hy + dy, outline, pixelSize)
        }
        for (dx in -r..r) for (dy in -r..r) {
            if (dx * dx + dy * dy < rSq) drawPixel(hx + dx, hy + dy, fill, pixelSize)
        }
    }
}

private fun DrawScope.drawHighlightAndRivets(
    cx: Int, cy: Int, scale: Float, pixelSize: Float,
    palette: CauldronBodyPalette,
) {
    val c = WitchCauldronConstants
    val hlX = cx + (c.HIGHLIGHT_DX * scale).toInt()
    val hlY = cy + (c.HIGHLIGHT_DY * scale).toInt()
    for (dx in 0 until c.HIGHLIGHT_W) for (dy in 0 until c.HIGHLIGHT_H) {
        drawPixel(hlX + dx, hlY + dy, palette.highlight, pixelSize)
    }
    for ((rx, ry) in c.RIVET_POSITIONS) {
        drawPixel(cx + (rx * scale).toInt(), cy + (ry * scale).toInt(), palette.rivet, pixelSize)
    }
}

// ── Animated stirring spoon ─────────────────────────────────────────────────

internal fun DrawScope.drawPixelSpoon(
    gridSize: Int,
    pixelSize: Float,
    angle: Float,
    scale: Float,
    liquidY: Int,
    bounceY: Int,
    palette: CauldronBodyPalette,
) {
    val c = WitchCauldronConstants
    val pivotX = gridSize / 2
    val pivotY = liquidY + bounceY
    val handleColor = palette.spoonHandle
    val bowlFill = palette.spoonBowl
    val bowlOutline = palette.spoonBowlOutline

    val sinA = sin(angle.toDouble()).toFloat()
    val cosA = cos(angle.toDouble()).toFloat()

    // Handle: line from pivot outward
    val len = (c.SPOON_HANDLE_LENGTH * scale).toInt().coerceAtLeast(4)
    for (t in 0..len) {
        drawPixel(
            (pivotX + sinA * t).toInt(),
            (pivotY - cosA * t).toInt(),
            handleColor, pixelSize
        )
    }

    // Bowl (circle) at handle tip
    val tipX = (pivotX + sinA * len).toInt()
    val tipY = (pivotY - cosA * len).toInt()
    val r = (c.SPOON_BOWL_RADIUS * scale).toInt().coerceAtLeast(1)
    val rSq = r * r
    val rOutSq = (r + 1) * (r + 1)

    for (dx in -r - 1..r + 1) for (dy in -r - 1..r + 1) {
        val d = dx * dx + dy * dy
        if (d < rOutSq) drawPixel(tipX + dx, tipY + dy, bowlOutline, pixelSize)
    }
    for (dx in -r..r) for (dy in -r..r) {
        if (dx * dx + dy * dy < rSq) drawPixel(tipX + dx, tipY + dy, bowlFill, pixelSize)
    }
    // Tiny highlight on bowl
    drawPixel(tipX - 1, tipY - 1, Color.White.copy(alpha = 0.7f), pixelSize)
}
