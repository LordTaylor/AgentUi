// Draws the cauldron body with squash-and-stretch, cartoon outline, rounded handles,
// highlights, rivets, cute splayed feet, and the animated stirring spoon.
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
    squash: Float = 1.0f,   // 1.0 = normal, >1 = squashed (wider X, shorter Y)
) {
    val c = WitchCauldronConstants
    val centerX = gridSize / 2
    val centerY = gridSize / 2 + (c.CAULDRON_CENTER_Y_OFFSET * scale).toInt() + bounceY

    drawLegs(centerX, centerY, scale, pixelSize, palette.outline, palette.body)
    drawBody(centerX, centerY, scale, pixelSize, palette.outline, palette.body, squash)
    drawRim(centerX, centerY, scale, pixelSize, palette.outline, palette.body, palette.rim)
    drawHandles(centerX, centerY, scale, pixelSize, palette.outline, palette.body)
    drawHighlightAndRivets(centerX, centerY, scale, pixelSize, palette)
}

// ── Cute splayed legs with rounded feet ──────────────────────────────────────

private fun DrawScope.drawLegs(
    cx: Int, cy: Int, scale: Float, pixelSize: Float,
    outline: Color, fill: Color
) {
    val c = WitchCauldronConstants
    val legHalfW   = ((c.LEG_WIDTH_MULT * scale) / 2f).toInt().coerceAtLeast(c.LEG_WIDTH_MIN)
    val legX       = (c.LEG_POS_X_MULT * scale).toInt()
    val yStart     = (c.LEG_Y_START_MULT * scale).toInt()
    val yEnd       = (c.LEG_Y_END_MULT * scale).toInt()
    val legHeight  = (yEnd - yStart).coerceAtLeast(1)
    val footH      = (c.LEG_FOOT_HEIGHT_MULT * scale).toInt().coerceAtLeast(2)
    val footExtra  = (c.LEG_FOOT_EXTRA_HALF_WIDTH * scale).toInt().coerceAtLeast(1)
    val totalSplay = (c.LEG_SPLAY_PIXELS * scale).toInt().coerceAtLeast(1)

    for (sign in listOf(-1, 1)) {
        val baseCx = cx + sign * legX

        // Draw leg column by column (row by row)
        for (dy in yStart..yEnd) {
            val progress  = (dy - yStart).toFloat() / legHeight
            // Outward splay: bottom of leg is pushed outward — gives cute "planted" stance
            val splay     = (sign * progress * totalSplay).toInt()
            // Foot zone: gradually widens near the bottom
            val inFoot    = dy > yEnd - footH
            val footProg  = if (inFoot) (dy - (yEnd - footH)).toFloat() / footH else 0f
            val halfW     = legHalfW + (footExtra * footProg).toInt()

            for (dx in -(halfW + 1)..(halfW + 1)) {
                drawPixel(baseCx + splay + dx, cy + dy, outline, pixelSize)
            }
            for (dx in -halfW..halfW) {
                drawPixel(baseCx + splay + dx, cy + dy, fill, pixelSize)
            }
        }

        // Rounded bottom cap — semicircle below each foot for that cute cartoon look
        val splayFull = sign * totalSplay
        val footCx    = baseCx + splayFull
        val footCy    = cy + yEnd
        val capR      = legHalfW + footExtra
        val capRSq    = capR * capR
        val capOutSq  = (capR + 1) * (capR + 1)

        for (dy in 0..capR + 1) for (dx in -(capR + 1)..(capR + 1)) {
            if (dx * dx + dy * dy < capOutSq) drawPixel(footCx + dx, footCy + dy, outline, pixelSize)
        }
        for (dy in 0..capR) for (dx in -capR..capR) {
            if (dx * dx + dy * dy < capRSq) drawPixel(footCx + dx, footCy + dy, fill, pixelSize)
        }

        // Tiny shine dot on each foot — makes them look shiny/cute
        drawPixel(footCx - (capR / 2), footCy + 1, Color.White.copy(alpha = 0.35f), pixelSize)
    }
}

// ── Squash-and-stretch body ───────────────────────────────────────────────────

private fun DrawScope.drawBody(
    cx: Int, cy: Int, scale: Float, pixelSize: Float,
    outline: Color, fill: Color,
    squash: Float = 1.0f
) {
    val c = WitchCauldronConstants
    val radius        = c.CAULDRON_RADIUS_MULT * scale
    val outlineRadius = radius + c.CAULDRON_OUTLINE_EXTRA * scale
    val outlineRSq    = outlineRadius * outlineRadius
    val radiusSq      = radius * radius
    val baseYScale    = c.CAULDRON_ELLIPSE_Y_SCALE
    val dyMin = (c.CAULDRON_DIY_MIN_MULT * scale).toInt()
    val dyMax = (c.CAULDRON_DIY_MAX_MULT * scale).toInt()
    val dxMin = (c.CAULDRON_DX_MIN_MULT * scale).toInt()
    val dxMax = (c.CAULDRON_DX_MAX_MULT * scale).toInt()

    // Squash: invSquashX shrinks the effective dx → body appears wider
    // squashYScale increases effective dy scaling → body appears shorter
    val extra      = (squash - 1f).coerceAtLeast(0f)
    val invSquashX = 1f / (1f + extra * 0.55f)
    val squashYSc  = baseYScale * (1f + extra * 0.42f)

    for (dy in dyMin - 2..dyMax + 2) for (dx in dxMin - 2..dxMax + 2) {
        val ndx = dx * invSquashX
        val ndy = dy * squashYSc
        if (ndx * ndx + ndy * ndy < outlineRSq) drawPixel(cx + dx, cy + dy, outline, pixelSize)
    }
    for (dy in dyMin..dyMax) for (dx in dxMin..dxMax) {
        val ndx = dx * invSquashX
        val ndy = dy * squashYSc
        if (ndx * ndx + ndy * ndy < radiusSq) drawPixel(cx + dx, cy + dy, fill, pixelSize)
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
    val r       = (c.HANDLE_RADIUS * scale).toInt().coerceAtLeast(2)
    val rSq     = r * r
    val rOutSq  = (r + 1) * (r + 1)

    for (side in listOf(-offsetX, offsetX)) {
        val hx = cx + side
        val hy = cy + offsetY
        for (dx in -r - 1..r + 1) for (dy in -r - 1..r + 1) {
            if (dx * dx + dy * dy < rOutSq) drawPixel(hx + dx, hy + dy, outline, pixelSize)
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
// Two-pass rendering: call once with clipMinY=liquidY (bowl submerged, before liquid),
// then once with clipMaxY=liquidY-1 (handle visible, after cauldron body).

internal fun DrawScope.drawPixelSpoon(
    gridSize: Int,
    pixelSize: Float,
    angle: Float,           // pendulum angle in radians (negative = left, positive = right)
    scale: Float,
    liquidY: Int,           // Y of liquid surface (already includes bounceY)
    palette: CauldronBodyPalette,
    clipMinY: Int = Int.MIN_VALUE,  // draw only pixels where y >= clipMinY
    clipMaxY: Int = Int.MAX_VALUE,  // draw only pixels where y <= clipMaxY
) {
    val c = WitchCauldronConstants
    val pivotX = gridSize / 2
    val pivotY = liquidY   // pivot sits at the liquid surface

    val sinA = sin(angle.toDouble()).toFloat()
    val cosA = cos(angle.toDouble()).toFloat()

    // Handle: extends UPWARD from pivot (visible above liquid)
    // 3-pixel thick: draw center + ±1 in the direction perpendicular to the handle axis
    // Perpendicular to (sinA, -cosA) is (cosA, sinA)
    val len = (c.SPOON_HANDLE_LENGTH * scale).toInt().coerceAtLeast(4)
    for (t in 0..len) {
        val hx = pivotX + sinA * t
        val hy = pivotY - cosA * t
        for (w in -1..1) {
            val px = (hx + cosA * w).toInt()
            val py = (hy + sinA * w).toInt()
            if (py in clipMinY..clipMaxY)
                drawPixel(px, py, palette.spoonHandle, pixelSize)
        }
    }

    // Bowl stem: extends DOWNWARD from pivot (submerged in liquid), also 3px thick
    val bowlDepth = (c.SPOON_BOWL_DEPTH * scale).toInt().coerceAtLeast(2)
    val bowlCx = (pivotX - sinA * bowlDepth).toInt()
    val bowlCy = (pivotY + cosA * bowlDepth).toInt()
    for (t in 1..bowlDepth) {
        val sx = pivotX - sinA * t
        val sy = pivotY + cosA * t
        for (w in -1..1) {
            val px = (sx + cosA * w).toInt()
            val py = (sy + sinA * w).toInt()
            if (py in clipMinY..clipMaxY)
                drawPixel(px, py, palette.spoonHandle, pixelSize)
        }
    }

    // Bowl circle at the deep end
    val r      = (c.SPOON_BOWL_RADIUS * scale).toInt().coerceAtLeast(1)
    val rSq    = r * r
    val rOutSq = (r + 1) * (r + 1)
    for (dx in -r - 1..r + 1) for (dy in -r - 1..r + 1) {
        val py = bowlCy + dy
        if (dx * dx + dy * dy < rOutSq && py in clipMinY..clipMaxY)
            drawPixel(bowlCx + dx, py, palette.spoonBowlOutline, pixelSize)
    }
    for (dx in -r..r) for (dy in -r..r) {
        val py = bowlCy + dy
        if (dx * dx + dy * dy < rSq && py in clipMinY..clipMaxY)
            drawPixel(bowlCx + dx, py, palette.spoonBowl, pixelSize)
    }
    if (bowlCy - 1 in clipMinY..clipMaxY)
        drawPixel(bowlCx - 1, bowlCy - 1, Color.White.copy(alpha = 0.7f), pixelSize)
}