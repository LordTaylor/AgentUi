// Routes tool-related IPC events: tool_call, tool_output_delta, tool_result.
// Each function is called from IpcHandler.handleIpcEvent when-branch.
// See: CoreApp/docs/communication.md §7 for tool event protocol.

package com.agentcore.logic

import com.agentcore.api.*
import com.agentcore.model.Message
import com.agentcore.model.MessageType

internal fun handleToolCall(
    event: IpcEvent.ToolCall,
    onMessageAdded: (Message) -> Unit,
    onSubAgentLog: (agentId: String, type: String, text: String) -> Unit,
    formatToolArgs: (kotlinx.serialization.json.JsonElement) -> String
) {
    val agentId = event.agentId
    if (agentId != null) {
        onSubAgentLog(agentId, "tool_call", "⚙️ ${event.payload.tool}(${formatToolArgs(event.payload.args)})")
        return
    }
    onMessageAdded(
        Message(
            id = "tool-${event.payload.id}",
            sender = "Tool",
            text = "⚙️ ${event.payload.tool}(${formatToolArgs(event.payload.args)})",
            isFromUser = false,
            type = MessageType.ACTION
        )
    )
}

internal fun handleToolOutputDelta(
    event: IpcEvent.ToolOutputDelta,
    getCurrentMessages: () -> List<Message>,
    onMessageAdded: (Message) -> Unit,
    onLastMessageUpdated: (Message) -> Unit,
    onToolOutputDelta: (ToolOutputDeltaPayload) -> Unit
) {
    val toolMsgId = "tool-${event.payload.id}"
    val existing = getCurrentMessages().lastOrNull { it.id == toolMsgId }
    if (existing != null) {
        onLastMessageUpdated(existing.copy(text = existing.text + "\n" + event.payload.line))
    } else {
        onMessageAdded(
            Message(
                id = "$toolMsgId-out",
                sender = "Tool",
                text = event.payload.line,
                isFromUser = false,
                type = MessageType.ACTION,
                agentId = event.agentId
            )
        )
    }
    onToolOutputDelta(event.payload)
}

internal fun handleToolResult(
    event: IpcEvent.ToolResult,
    onMessageAdded: (Message) -> Unit,
    onSubAgentLog: (agentId: String, type: String, text: String) -> Unit
) {
    val body = if (event.payload.error != null)
        "❌ ${event.payload.error}"
    else
        "✅ ${event.payload.result.take(300)}${if (event.payload.result.length > 300) "…" else ""}"
    val agentId = event.agentId
    if (agentId != null) {
        onSubAgentLog(agentId, "tool_result", body)
        return
    }
    onMessageAdded(
        Message(
            id = "result-${event.payload.id}",
            sender = "Tool",
            text = body,
            isFromUser = false,
            type = MessageType.ACTION
        )
    )
}
