// Variant composables for different ChatBubble message types: error, system, and the main agent/user bubble.
// Each function handles a distinct visual presentation; routing is done in ChatBubble.kt.
// Text and action row sub-composables live in ChatBubbleContent.kt.
package com.agentcore.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agentcore.model.Message
import com.agentcore.model.MessageType

@Composable
internal fun ChatBubbleError(msg: Message) {
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
}

@Composable
internal fun ChatBubbleSystem(msg: Message) {
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
}

@Composable
internal fun ChatBubbleMain(
    msg: Message,
    isGrouped: Boolean,
    fontSize: Float,
    codeFontSize: Float,
    isStreaming: Boolean,
    onFork: () -> Unit,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
    onRunCode: (String) -> Unit,
    onRunInTerminal: (String) -> Unit
) {
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
                if (!msg.isFromUser && msg.tokensPerSec != null) {
                    Spacer(Modifier.width(6.dp))
                    ToksChip(tps = msg.tokensPerSec!!)
                }
                if (msg.isFromUser) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ChatAvatar(sender = msg.sender, isUser = true)
                }
            }
        }

        if (isGrouped && !msg.isFromUser && msg.tokensPerSec != null) {
            ToksChip(tps = msg.tokensPerSec!!, modifier = Modifier.padding(start = 48.dp, bottom = 2.dp))
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
                // Render all user-attached images from the attachments list
                msg.attachments?.forEach { path ->
                    InlineAgentImage(path)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                BubbleTextContent(
                    msg = msg,
                    fontSize = fontSize,
                    isStreaming = isStreaming,
                    onRunCode = onRunCode,
                    onRunInTerminal = onRunInTerminal
                )

                BubbleActionRow(
                    msg = msg,
                    fontSize = fontSize,
                    clipboardManager = clipboardManager,
                    onEdit = onEdit,
                    onRetry = onRetry,
                    onFork = onFork
                )
            }
        }
    }
}
