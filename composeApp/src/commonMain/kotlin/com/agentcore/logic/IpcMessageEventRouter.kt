// Routes message-lifecycle IPC events: message_start, text_delta, message_end, thought.
// Each function is called from IpcHandler.handleIpcEvent when-branch.
// See: CoreApp/docs/communication.md §7 for event protocol details.

package com.agentcore.logic

import com.agentcore.api.*
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject

internal fun handleMessageStart(
    event: IpcEvent.MessageStart,
    onStatusChange: (String) -> Unit,
    onSessionStart: (String) -> Unit
) {
    onStatusChange("THINKING")
    onSessionStart(event.payload.session_id)
}

internal fun handleTextDelta(
    event: IpcEvent.TextDelta,
    getCurrentMessages: () -> List<Message>,
    onMessageAdded: (Message) -> Unit,
    onLastMessageUpdated: (Message) -> Unit,
    onSubAgentLog: (agentId: String, type: String, text: String) -> Unit
) {
    val agentId = event.agentId
    if (agentId != null) {
        onSubAgentLog(agentId, "text", event.payload.text)
        return
    }
    val lastMsg = getCurrentMessages().lastOrNull()
    if (lastMsg != null && !lastMsg.isFromUser && lastMsg.type == MessageType.TEXT) {
        onLastMessageUpdated(lastMsg.copy(text = lastMsg.text + event.payload.text))
    } else {
        onMessageAdded(
            Message(
                id = IpcHandler.nextId("agent"),
                sender = "Agent",
                text = event.payload.text,
                isFromUser = false
            )
        )
    }
}

internal fun handleMessageEnd(
    event: IpcEvent.MessageEnd,
    onStatusChange: (String) -> Unit,
    onStatsUpdate: (JsonObject) -> Unit
) {
    onStatusChange("IDLE")
    event.payload.usage?.let { usage ->
        val stats = buildJsonObject {
            put("input_tokens", kotlinx.serialization.json.JsonPrimitive(usage.input_tokens))
            put("output_tokens", kotlinx.serialization.json.JsonPrimitive(usage.output_tokens))
        }
        onStatsUpdate(stats)
    }
}

internal fun handleThought(
    event: IpcEvent.Thought,
    getCurrentMessages: () -> List<Message>,
    onMessageAdded: (Message) -> Unit,
    onLastMessageUpdated: (Message) -> Unit,
    onSubAgentThought: (agentId: String, text: String) -> Unit
) {
    val agentId = event.agentId
    if (agentId != null) {
        onSubAgentThought(agentId, event.payload.text)
        return
    }
    val lastMsg = getCurrentMessages().lastOrNull()
    if (lastMsg != null && lastMsg.sender == "Thought" && lastMsg.type == MessageType.SYSTEM) {
        onLastMessageUpdated(lastMsg.copy(text = "💭 ${event.payload.text}"))
    } else {
        onMessageAdded(
            Message(
                id = IpcHandler.nextId("thought"),
                sender = "Thought",
                text = "💭 ${event.payload.text}",
                isFromUser = false,
                type = MessageType.SYSTEM,
                agentId = agentId
            )
        )
    }
}
