package com.agentcore.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.agentcore.Message
import com.mikepenz.markdown.m3.Markdown

@Composable
fun ChatBubble(msg: Message, isGrouped: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = if (isGrouped) 2.dp else 8.dp, bottom = 2.dp),
        horizontalAlignment = if (msg.isFromUser) androidx.compose.ui.Alignment.End else androidx.compose.ui.Alignment.Start
    ) {
        if (!isGrouped && !msg.isFromUser) {
            Text(
                text = msg.sender,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (msg.isFromUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
            border = if (!msg.isFromUser) BorderStroke(1.dp, Color.DarkGray) else null
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (msg.extraContent?.startsWith("data:image") == true) {
                    // Vision support: render image
                    Text("📷 IMAGE ATTACHED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Note: Actual image decoding would happen here using Skia on Desktop
                }

                val text = msg.text
                if (text.contains("```") && !msg.isFromUser) {
                    val parts = text.split("```")
                    parts.forEachIndexed { index, part ->
                        if (index % 2 == 0) {
                            if (part.isNotBlank()) Markdown(content = part.trim())
                        } else {
                            val lines = part.trim().lines()
                            val lang = lines.firstOrNull()?.trim()
                            val code = if (lines.size > 1) lines.drop(1).joinToString("\n") else lines.firstOrNull() ?: ""
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                color = Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (!lang.isNullOrBlank() && lines.size > 1) {
                                        Text(
                                            text = lang.uppercase(),
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = SyntaxHighlighter.highlight(code, lang),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val timeStr = com.agentcore.shared.DateTimeUtils.formatTime(msg.timestamp)
                Text(
                    text = timeStr,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                )
            }
        }
    }
}
