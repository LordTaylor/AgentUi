// Action-related composables for chat bubbles: ActionPill, ThinkingBubble, ToksChip.
// These are small, self-contained UI elements used inside the chat message list.
// See: ChatBubble.kt for the main routing composable.
package com.agentcore.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.model.Message

@Composable
internal fun ToksChip(tps: Float, modifier: Modifier = Modifier) {
    val color = when {
        tps >= 15f -> Color(0xFF4CAF50)
        tps >= 6f  -> Color(0xFFFFB300)
        else       -> Color(0xFFEF5350)
    }
    val label = if (tps >= 10f) "%.0f/s".format(tps) else "%.1f/s".format(tps)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
internal fun ActionPill(msg: Message) {
    var expanded by remember { mutableStateOf(false) }
    val isToolCall = msg.text.startsWith("⚙️")
    val isLong = msg.text.length > 72
    val pillColor = if (isToolCall) Color(0xFF4CAF50) else Color(0xFF29B6F6)
    val bgColor   = if (isToolCall) Color(0xFF1B3A24) else Color(0xFF1A2B3A)
    val indent    = if (msg.agentId != null) 24.dp else 8.dp

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, pillColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp, horizontal = indent)
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = isLong) { if (isLong) expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = if (isToolCall) "⚙" else "↳",
                fontSize = 9.sp,
                color = pillColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 1.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = msg.text.removePrefix("⚙️ "),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = pillColor.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isLong) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 8.sp,
                    color = pillColor.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
internal fun ThinkingBubble(text: String) {
    var expanded by remember { mutableStateOf(false) }
    val cleanText = text.removePrefix("💭 ").trim()
    val preview = if (cleanText.length > 60) cleanText.take(60) + "…" else cleanText

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF7C4DFF).copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text("🧠", fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (expanded) cleanText else preview,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF9C6FFF),
                    modifier = Modifier.weight(1f),
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (cleanText.length > 60) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (expanded) "▲" else "▼",
                        fontSize = 9.sp,
                        color = Color(0xFF9C6FFF).copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
