// Shared drawPixel primitive and color palette for pixel art avatar components.
package com.agentcore.ui.components.avatar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawPixel(x: Int, y: Int, color: Color, ps: Float) {
    if (color.alpha == 0f) return
    drawRect(color = color, topLeft = Offset(x * ps, y * ps), size = Size(ps, ps))
}

internal object AvatarColors {
    val BLACK        = Color(0xFF000000)
    val GOLD         = Color(0xFFFFD700)
    val GOBLIN_GREEN = Color(0xFF22A83A)   // vivid mid-green skin (was muted Material 500)
    val GOBLIN_LIGHT = Color(0xFF55D46A)   // bright highlight skin (was washed-out 400)
    val GOBLIN_DARK  = Color(0xFF0C6622)   // deep shadow / nostrils (was too grey-green)
    val ROBE_PURPLE  = Color(0xFF7B1FA2)
    val ROBE_LIGHT   = Color(0xFFAB47BC)
    val EYE_RED      = Color(0xFFFF1111)
    val EYE_YELLOW   = Color(0xFFFFEB3B)
    val BOOK_BROWN   = Color(0xFF3E2005)
    val BOOK_IVORY   = Color(0xFFFDF5E6)
    val BOOK_GOLD    = Color(0xFFFFD700)
    val RUNE_GREEN   = Color(0xFF00FF88)
    val SUB_CLOTHES  = Color(0xFF1A5C10)   // darker tunic for contrast against bright skin
}
