package com.agentcore.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.mikepenz.markdown.m3.Markdown

@Composable
fun ChatBubble(msg: Message, isGrouped: Boolean = false) {
    if (msg.type == MessageType.SYSTEM) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                    color = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isGrouped) 1.dp else 4.dp, bottom = 1.dp),
        horizontalAlignment = if (msg.isFromUser) androidx.compose.ui.Alignment.End else androidx.compose.ui.Alignment.Start
    ) {
        if (!isGrouped) {
            Text(
                text = msg.sender,
                style = MaterialTheme.typography.labelSmall,
                color = if (msg.isFromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isGrouped && !msg.isFromUser) 4.dp else 12.dp,
                topEnd = if (isGrouped && msg.isFromUser) 4.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            color = if (msg.isFromUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            border = if (!msg.isFromUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            tonalElevation = if (msg.isFromUser) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (msg.extraContent?.startsWith("data:image") == true) {
                    Text("📷 IMAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val text = msg.text
                if (text.contains("```") && !msg.isFromUser) {
                    // Render Markdown/Code parts
                    Markdown(content = text)
                } else {
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp).align(androidx.compose.ui.Alignment.End),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    val timeStr = com.agentcore.shared.DateTimeUtils.formatRelativeTime(msg.timestamp)
                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
