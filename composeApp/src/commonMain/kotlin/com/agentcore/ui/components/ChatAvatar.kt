// Message sender avatar: Person icon for users, pixel-art for agents.
// Main agent (no agentId) → PixelMagicBook.
// Sub-agent (has agentId) → PixelGoblinAvatar(isLeader=false).
package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agentcore.ui.components.avatar.PixelGoblinAvatar
import com.agentcore.ui.components.avatar.PixelMagicBook

@Composable
fun ChatAvatar(
    sender: String,
    isUser: Boolean,
    agentId: String? = null,
    modifier: Modifier = Modifier
) {
    if (isUser) {
        Box(
            modifier = modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }
        return
    }

    // Agent avatar — pixel art, no circle background (drawn on Canvas)
    // 96dp = 3× the original 32dp so each pixel-art pixel is clearly visible.
    val isSubAgent = agentId != null && agentId.isNotEmpty() && agentId != "main"
    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
        if (isSubAgent) {
            PixelGoblinAvatar(isLeader = false, modifier = Modifier.size(96.dp))
        } else {
            PixelMagicBook(modifier = Modifier.size(96.dp))
        }
    }
}
