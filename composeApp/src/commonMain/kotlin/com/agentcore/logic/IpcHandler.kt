package com.agentcore.logic

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.agentcore.api.*
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

object IpcHandler {
    fun handleIpcEvent(
        event: IpcEvent,
        messages: SnapshotStateList<Message>,
        onStatusChange: (String) -> Unit,
        onStatsUpdate: (JsonObject) -> Unit,
        onApprovalRequest: (ApprovalRequestPayload) -> Unit,
        onLogReceived: (LogPayload) -> Unit,
        onScratchpadUpdate: (String) -> Unit,
        onTerminalTraffic: (TerminalTrafficPayload) -> Unit,
        onIndexingProgress: (IndexingProgressPayload) -> Unit,
        onPluginsLoaded: (List<PluginMetadataPayload>) -> Unit,
        onWorkflowsUpdate: (List<WorkflowStatusPayload>) -> Unit,
        onInputTextChange: (String) -> Unit,
        onVoiceUpdate: (VoiceStatusPayload) -> Unit,
        onContextSuggestions: (List<ContextItem>) -> Unit,
        onError: (ErrorPayload) -> Unit,
        onSessionData: (SessionDataPayload) -> Unit,
        onHumanInputRequest: (HumanInputPayload) -> Unit,
        onAgentGroupUpdate: (AgentGroupPayload) -> Unit
    ) {
        when (event) {
            is IpcEvent.Status -> onStatusChange(event.payload.state)
            is IpcEvent.MessageStart -> onStatusChange("THINKING")
            is IpcEvent.TextDelta -> {
                val lastMsg = messages.lastOrNull()
                if (lastMsg != null && !lastMsg.isFromUser && lastMsg.type == MessageType.TEXT) {
                    messages[messages.size - 1] = lastMsg.copy(text = lastMsg.text + event.payload.text)
                } else {
                    messages.add(
                        Message(
                            id = "agent-${System.currentTimeMillis()}",
                            sender = "Agent",
                            text = event.payload.text,
                            isFromUser = false
                        )
                    )
                }
            }
            is IpcEvent.MessageEnd -> onStatusChange("IDLE")
            is IpcEvent.Stats -> onStatsUpdate(event.payload)
            is IpcEvent.ApprovalRequest -> onApprovalRequest(event.payload)
            is IpcEvent.Error -> {
                messages.add(
                    Message(
                        id = "err-${System.currentTimeMillis()}",
                        sender = "System",
                        text = "❌ [${event.payload.code}] ${event.payload.message}",
                        isFromUser = false,
                        type = MessageType.SYSTEM
                    )
                )
                onError(event.payload)
                onStatusChange("IDLE")
            }
            is IpcEvent.ToolCall -> {
                messages.add(
                    Message(
                        id = "tool-${event.payload.id}",
                        sender = "Tool",
                        text = "⚙️ ${event.payload.tool}(${event.payload.args})",
                        isFromUser = false,
                        type = MessageType.ACTION
                    )
                )
            }
            is IpcEvent.ToolResult -> {
                val body = if (event.payload.error != null)
                    "❌ ${event.payload.error}"
                else
                    "✅ ${event.payload.result.take(300)}${if (event.payload.result.length > 300) "…" else ""}"
                messages.add(
                    Message(
                        id = "result-${event.payload.id}",
                        sender = "Tool",
                        text = body,
                        isFromUser = false,
                        type = MessageType.ACTION
                    )
                )
            }
            is IpcEvent.Thought -> {
                messages.add(
                    Message(
                        id = "thought-${System.currentTimeMillis()}",
                        sender = "Thought",
                        text = "💭 ${event.payload.text}",
                        isFromUser = false,
                        type = MessageType.SYSTEM
                    )
                )
            }
            is IpcEvent.SessionData -> onSessionData(event.payload)
            is IpcEvent.HumanInputRequest -> onHumanInputRequest(event.payload)
            is IpcEvent.AgentGroupUpdate -> onAgentGroupUpdate(event.payload)
            is IpcEvent.Log -> onLogReceived(event.payload)
            is IpcEvent.Scratchpad -> onScratchpadUpdate(event.payload.content)
            is IpcEvent.TerminalTraffic -> onTerminalTraffic(event.payload)
            is IpcEvent.IndexingProgress -> onIndexingProgress(event.payload)
            is IpcEvent.PluginMetadata -> onPluginsLoaded(event.payload)
            is IpcEvent.WorkflowStatus -> onWorkflowsUpdate(event.payload)
            is IpcEvent.VoiceStatus -> onVoiceUpdate(event.payload)
            is IpcEvent.ContextSuggestions -> onContextSuggestions(event.payload.suggestions)
            else -> {}
        }
    }

    fun performSendMessage(
        scope: CoroutineScope,
        client: AgentClient,
        stdioExecutor: StdioExecutor,
        unixSocketExecutor: UnixSocketExecutor,
        cliExecutor: CliExecutor,
        mode: ConnectionMode,
        text: String,
        attachments: List<String>,
        sessionId: String?,
        messages: SnapshotStateList<Message>,
        onClearInput: () -> Unit,
        onClearAttachments: () -> Unit,
        onStatusChange: (String) -> Unit
    ) {
        val msgId = "msg-${System.currentTimeMillis()}"
        val attachList = attachments.takeIf { it.isNotEmpty() }
        val userMsg = Message(msgId, "User", text, true, MessageType.TEXT, attachList)
        messages.add(userMsg)
        onStatusChange("THINKING")
        onClearInput()
        onClearAttachments()

        scope.launch {
            try {
                val payload = SendMessagePayload(session_id = sessionId, text = text, attachments = attachList)
                when (mode) {
                    ConnectionMode.IPC -> client.sendCommand(IpcCommand.SendMessage(payload))
                    ConnectionMode.STDIO -> stdioExecutor.sendCommand(IpcCommand.SendMessage(payload))
                    ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(IpcCommand.SendMessage(payload))
                    ConnectionMode.CLI -> {
                        val result = cliExecutor.executeCommand(text)
                        messages.add(Message("cli-${System.currentTimeMillis()}", "Agent", result, false, MessageType.TEXT))
                        onStatusChange("IDLE")
                    }
                }
            } catch (e: Exception) {
                messages.add(Message("err-${System.currentTimeMillis()}", "System", "Error: ${e.message}", false, MessageType.SYSTEM))
                onStatusChange("IDLE")
            }
        }
    }
}
