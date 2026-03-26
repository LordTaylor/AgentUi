package com.agentcore.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
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
    isStreaming: Boolean = false,
    onFork: () -> Unit = {},
    onRetry: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onRunCode: (String) -> Unit = {},
    onRunInTerminal: (String) -> Unit = {}
) {
    if (msg.type == MessageType.ERROR) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF5252).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = msg.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp
                    )
                }
            }
        }
        return
    }

    if (msg.type == MessageType.SYSTEM) {
        if (msg.sender == "Thought") {
            ThinkingBubble(text = msg.text)
        } else {
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
                val cursorAlpha by if (isStreaming) {
                    rememberInfiniteTransition(label = "cursor").animateFloat(
                        initialValue = 1f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                        label = "cursorAlpha"
                    )
                } else {
                    androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
                }
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
                        // LaTeX-like styling for $expr$ — regex matches $non-space content$
                        // Safe: won't match currency like $100 (requires closing $)
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val annotatedText = remember(text, primaryColor) {
                            val builder = AnnotatedString.Builder()
                            val mathRegex = Regex("""\$([^\s$][^$]*)\$""")
                            var last = 0
                            for (match in mathRegex.findAll(text)) {
                                builder.append(text.substring(last, match.range.first))
                                builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                ))
                                builder.append(match.groupValues[1])
                                builder.pop()
                                last = match.range.last + 1
                            }
                            builder.append(text.substring(last))
                            builder.toAnnotatedString()
                        }
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
                            lineHeight = (fontSize * 1.4f).sp
                        )
                        if (isStreaming) {
                            Text(
                                text = "▋",
                                fontSize = fontSize.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp).align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (msg.isFromUser) {
                        AppTooltip("Edytuj wiadomość") {
                            IconButton(
                                onClick = { onEdit(msg.text) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
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

@Composable
private fun ThinkingBubble(text: String) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🧠", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (expanded) "Myślenie" else preview,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF9C6FFF),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 9.sp,
                    color = Color(0xFF9C6FFF).copy(alpha = 0.6f)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cleanText,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF9C6FFF).copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
