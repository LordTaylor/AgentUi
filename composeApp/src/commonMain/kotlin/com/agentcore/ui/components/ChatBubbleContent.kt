// Internal composables for rendering the text body and action row inside a chat bubble surface.
// BubbleTextContent handles markdown/LaTeX/plain text, streaming cursor, and inline image rendering.
// BubbleActionRow renders copy/fork/edit/retry icon buttons with timestamps.
package com.agentcore.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.skia.Image as SkiaImage

/** Image extensions considered as generated output from the agent. */
private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

/** Extract file paths from text that look like agent-generated images. */
internal fun extractImagePaths(text: String): List<String> {
    val regex = Regex("""(?:^|[\s(])(/[^\s)'"]+\.(?:png|jpg|jpeg|gif|webp|bmp))""",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
    return regex.findAll(text).map { it.groupValues[1] }.filter {
        java.io.File(it).exists()
    }.toList()
}

@Composable
internal fun InlineAgentImage(path: String) {
    val bitmap: ImageBitmap? = remember(path) {
        try {
            val bytes = java.io.File(path).readBytes()
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Exception) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Agent generated image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(top = 6.dp)
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

@Composable
internal fun BubbleTextContent(
    msg: Message,
    fontSize: Float,
    isStreaming: Boolean,
    onRunCode: (String) -> Unit,
    onRunInTerminal: (String) -> Unit
) {
    val text = msg.text
    val cursorAlpha by if (isStreaming) {
        rememberInfiniteTransition(label = "cursor").animateFloat(
            initialValue = 1f, targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
            label = "cursorAlpha"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val contentColor = MaterialTheme.colorScheme.onSurface
    // Extract image paths from text OR attachments (for agent-generated images)
    val imagePaths = remember(text) {
        if (msg.isFromUser) emptyList()
        else extractImagePaths(text)
    }
    SelectionContainer {
        // Render Markdown only when streaming is complete — the Markdown library
        // cannot handle partial/incomplete fenced code blocks during streaming.
        if (text.contains("```") && !msg.isFromUser && !isStreaming) {
            Markdown(
                content = text,
                components = agentMarkdownComponents(
                    onRunCode = onRunCode,
                    onRunInTerminal = onRunInTerminal
                )
            )
        } else {
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = fontSize.sp,
                    color = contentColor
                ),
                lineHeight = (fontSize * 1.4f).sp
            )
            if (isStreaming) {
                Text(
                    text = "▋",
                    fontSize = fontSize.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha)
                )
            }
            // Render agent-generated images found in the text
            imagePaths.forEach { path -> InlineAgentImage(path) }
        }
    }
}

@Composable
internal fun BubbleActionRow(
    msg: Message,
    fontSize: Float,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    onEdit: (String) -> Unit,
    onRetry: () -> Unit,
    onFork: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (msg.isFromUser) {
            AppTooltip("Edytuj wiadomość") {
                IconButton(onClick = { onEdit(msg.text) }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Edit, null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            AppTooltip("Powtórz to zapytanie") {
                IconButton(onClick = onRetry, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Refresh, null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp))
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
        if (!msg.isFromUser && msg.type != MessageType.ACTION) {
            Spacer(modifier = Modifier.width(10.dp))
            AppTooltip("Kopiuj wiadomość") {
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(msg.text)) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            AppTooltip("Rozgałęź sesję od tej wiadomości") {
                IconButton(onClick = onFork, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Share, null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
