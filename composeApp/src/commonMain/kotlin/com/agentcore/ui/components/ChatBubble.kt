package com.agentcore.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.mikepenz.markdown.m3.Markdown

@Composable
fun ChatBubble(
    msg: Message,
    isGrouped: Boolean = false,
    fontSize: Float = 14f,
    codeFontSize: Float = 13f,
    onFork: () -> Unit = {}
) {
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
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                    color = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    val userGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    )
    val agentGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    )

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (isGrouped) 1.dp else 6.dp, 
                bottom = 1.dp,
                start = if (!msg.isFromUser && msg.agentId != null) 32.dp else 0.dp
            ),
        horizontalAlignment = if (msg.isFromUser) Alignment.End else Alignment.Start
    ) {
        if (!isGrouped) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                val senderColor = if (msg.isFromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                val agentId = msg.agentId
                if (!msg.isFromUser && agentId != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = agentId.take(4).uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Text(
                    text = msg.sender,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = senderColor
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isGrouped && !msg.isFromUser) 8.dp else 24.dp,
                topEnd = if (isGrouped && msg.isFromUser) 8.dp else 24.dp,
                bottomStart = 24.dp,
                bottomEnd = 24.dp
            ),
            color = if (msg.isFromUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = if (!msg.isFromUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) else null,
            tonalElevation = 0.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                val content = msg.extraContent
                if (content != null && (content.startsWith("data:image") || content.startsWith("http") || content.startsWith("/"))) {
                    AsyncImage(
                        model = content,
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val text = msg.text
                SelectionContainer {
                    if (text.contains("```") && !msg.isFromUser) {
                        Markdown(content = text)
                    } else {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
                            lineHeight = (fontSize * 1.4f).sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp).align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeStr = com.agentcore.shared.DateTimeUtils.formatRelativeTime(msg.timestamp)
                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    
                    if (!msg.isFromUser && msg.type != MessageType.ACTION) {
                        Spacer(modifier = Modifier.width(10.dp))
                        AppTooltip("Rozgałęź sesję od tej wiadomości") {
                            IconButton(
                                onClick = onFork,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
