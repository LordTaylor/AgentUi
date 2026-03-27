// Routes each chat message to the correct variant composable based on MessageType.
// Delegates rendering to ChatBubbleError, ChatBubbleSystem, ActionPill, or ChatBubbleMain.
// Right-click context menu wraps all bubbles via ContextMenuArea (Session Branching UI).
// See: ChatBubbleVariants.kt and ChatBubbleActions.kt for implementation details.
package com.agentcore.ui.components

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.ui.chat.SubAgentThread
import kotlinx.serialization.json.Json

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
    val clipboard = LocalClipboardManager.current
    val menuItems = remember(msg) {
        buildList {
            if (msg.type == MessageType.TEXT || msg.type == MessageType.ERROR) {
                add(ContextMenuItem("Fork session from here") { onFork() })
                add(ContextMenuItem("Copy text") { clipboard.setText(AnnotatedString(msg.text)) })
            }
            if (msg.isFromUser) {
                add(ContextMenuItem("Edit message") { onEdit(msg.text) })
                add(ContextMenuItem("Retry") { onRetry() })
            }
        }
    }
    ContextMenuArea(items = { menuItems }) {
        when (msg.type) {
            MessageType.ERROR -> ChatBubbleError(msg = msg)
            MessageType.SYSTEM -> ChatBubbleSystem(msg = msg)
            MessageType.ACTION -> ActionPill(msg = msg)
            MessageType.SUB_AGENT_THREAD -> {
                val thread = try { Json.decodeFromString<SubAgentThread>(msg.extraContent ?: "{}") }
                             catch (_: Exception) { null }
                if (thread != null) SubAgentThreadItem(thread = thread)
            }
            else -> ChatBubbleMain(
                msg = msg,
                isGrouped = isGrouped,
                fontSize = fontSize,
                codeFontSize = codeFontSize,
                isStreaming = isStreaming,
                onFork = onFork,
                onRetry = onRetry,
                onEdit = onEdit,
                onRunCode = onRunCode,
                onRunInTerminal = onRunInTerminal
            )
        }
    }
}
