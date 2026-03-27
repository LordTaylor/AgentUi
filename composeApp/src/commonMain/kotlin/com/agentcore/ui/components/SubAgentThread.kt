// I35: Renders a collapsible sub-agent thread in the chat message list.
// Thread header shows agent ID, status icon, and expand/collapse button.
// Expanded view shows mini chat bubbles with all sub-agent events.
package com.agentcore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.ui.chat.SubAgentThread
import com.agentcore.ui.chat.SubAgentMessage

private val threadBorderColor = Color(0xFF5C4A7A)
private val threadBgColor = Color(0xFF1A1225)

@Composable
fun SubAgentThreadItem(thread: SubAgentThread, modifier: Modifier = Modifier) {
    var expanded by remember(thread.agentId) { mutableStateOf(false) }

    val statusIcon = when {
        thread.done && thread.success -> "✅"
        thread.done && !thread.success -> "❌"
        else -> "⏳"
    }
    val statusColor = when {
        thread.done && thread.success -> Color(0xFF81C784)
        thread.done && !thread.success -> Color(0xFFE57373)
        else -> Color(0xFFFFB74D)
    }
    val shortId = thread.agentId.take(8)
    val roleLabel = if (thread.role.isNotEmpty()) "[${thread.role}]" else ""
    val msgCount = thread.messages.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, threadBorderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .background(threadBgColor)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(statusIcon, fontSize = 14.sp)
                Text(
                    text = "Sub-agent $roleLabel $shortId",
                    style = MaterialTheme.typography.bodySmall.copy(color = statusColor, fontFamily = FontFamily.Monospace),
                )
                if (!thread.done) {
                    Text("($msgCount events)", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp))
                } else if (thread.summary.isNotEmpty()) {
                    Text(
                        text = thread.summary.take(60) + if (thread.summary.length > 60) "…" else "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                    )
                }
            }
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        // Expanded content
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HorizontalDivider(color = threadBorderColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                Spacer(Modifier.height(4.dp))
                thread.messages.forEach { SubAgentMiniMessage(it) }
            }
        }
    }
}

@Composable
private fun SubAgentMiniMessage(msg: SubAgentMessage) {
    val (icon, color) = when (msg.type) {
        "text" -> "💬" to Color(0xFFB0BEC5)
        "tool_call" -> "⚙️" to Color(0xFF90CAF9)
        "tool_result" -> "📤" to Color(0xFFA5D6A7)
        "status" -> "🔄" to Color(0xFFCE93D8)
        else -> "•" to Color.Gray
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(icon, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
        Text(
            text = msg.text.take(200) + if (msg.text.length > 200) "…" else "",
            style = MaterialTheme.typography.bodySmall.copy(color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(1f)
        )
    }
}
