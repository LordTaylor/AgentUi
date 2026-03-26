// Shared primitive: draws a single pixel-sized rectangle at grid coordinates.
// Every draw file depends on this; kept separate to avoid duplication.
package com.agentcore.ui.components.cauldron

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawPixel(x: Int, y: Int, color: Color, pixelSize: Float) {
    drawRect(
        color = color,
        topLeft = Offset(x * pixelSize, y * pixelSize),
        size = Size(pixelSize, pixelSize)
    )
}
