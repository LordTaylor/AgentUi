package com.agentcore.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
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
    onFork: () -> Unit = {},
    onRetry: () -> Unit = {},
    onRunCode: (String) -> Unit = {},
    onRunInTerminal: (String) -> Unit = {}
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
                top = if (isGrouped) 1.dp else 12.dp, 
                bottom = 1.dp,
                start = if (!msg.isFromUser) 8.dp else 48.dp,
                end = if (msg.isFromUser) 8.dp else 48.dp
            ),
        horizontalAlignment = if (msg.isFromUser) Alignment.End else Alignment.Start
    ) {
        if (!isGrouped) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = if (msg.isFromUser) Arrangement.End else Arrangement.Start
            ) {
                if (!msg.isFromUser) {
                    ChatAvatar(sender = msg.sender, isUser = false, agentId = msg.agentId)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = msg.sender,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (msg.isFromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                
                if (msg.isFromUser) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ChatAvatar(sender = msg.sender, isUser = true)
                }
            }
        }

        val bubbleShape = RoundedCornerShape(
            topStart = if (isGrouped && !msg.isFromUser) 4.dp else 20.dp,
            topEnd = if (isGrouped && msg.isFromUser) 4.dp else 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp
        )

        Surface(
            shape = bubbleShape,
            color = if (msg.isFromUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
            modifier = Modifier.padding(
                start = if (!msg.isFromUser && isGrouped) 40.dp else 0.dp,
                end = if (msg.isFromUser && isGrouped) 40.dp else 0.dp
            )
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
                        Markdown(
                            content = text,
                            components = agentMarkdownComponents(
                                onRunCode = onRunCode,
                                onRunInTerminal = onRunInTerminal
                            )
                        )
                    } else {
                        // Simple LaTeX-like styling for $ ... $
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val annotatedText = remember(text, primaryColor) {
                            val builder = AnnotatedString.Builder()
                            val parts = text.split("$")
                            parts.forEachIndexed { index, part ->
                                if (index % 2 == 1 && part.isNotEmpty()) {
                                    builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    ))
                                    builder.append(part)
                                    builder.pop()
                                } else {
                                    builder.append(part)
                                }
                            }
                            builder.toAnnotatedString()
                        }
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
                            lineHeight = (fontSize * 1.4f).sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp).align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (msg.isFromUser) {
                        AppTooltip("Powtórz to zapytanie") {
                            IconButton(
                                onClick = onRetry,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    val timeStr = com.agentcore.shared.DateTimeUtils.formatRelativeTime(msg.timestamp)
                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )

                    if (!msg.isFromUser && msg.type == MessageType.TEXT && msg.tokensPerSec != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%.1f tok/s".format(msg.tokensPerSec),
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }

                    if (!msg.isFromUser && msg.type != MessageType.ACTION) {
                        Spacer(modifier = Modifier.width(10.dp))
                        AppTooltip("Kopiuj wiadomość") {
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(msg.text)) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
