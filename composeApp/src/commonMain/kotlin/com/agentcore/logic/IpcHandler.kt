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
        currentMessages: List<Message>,
        onMessageAdded: (Message) -> Unit,
        onLastMessageUpdated: (Message) -> Unit,
        onStatusChange: (String) -> Unit,
        onStatsUpdate: (JsonObject) -> Unit,
        onApprovalRequest: (ApprovalRequestPayload) -> Unit,
        onLogReceived: (LogPayload) -> Unit,
        onScratchpadUpdate: (String) -> Unit,
        onTerminalTraffic: (TerminalTrafficPayload) -> Unit,
        onIndexingProgress: (IndexingProgressPayload) -> Unit,
        onPluginsLoaded: (List<PluginMetadataPayload>) -> Unit,
        onWorkflowsUpdate: (List<WorkflowStatusPayload>) -> Unit,
        onVoiceUpdate: (VoiceStatusPayload) -> Unit,
        onContextSuggestions: (List<ContextItem>) -> Unit,
        onError: (ErrorPayload) -> Unit,
        onSessionData: (SessionDataPayload) -> Unit,
        onHumanInputRequest: (HumanInputPayload) -> Unit,
        onAgentGroupUpdate: (AgentGroupPayload) -> Unit,
        onSessionForked: (SessionForkedPayload) -> Unit = {},
        onTaskScheduled: (TaskScheduledPayload) -> Unit = {},
        onScheduledTasksList: (ScheduledTasksListPayload) -> Unit = {},
        onModelsList: (String, List<String>) -> Unit = { _, _ -> },
        // B02: streaming subprocess tool output (I01)
        onToolOutputDelta: (ToolOutputDeltaPayload) -> Unit = {},
        onSubAgentDone: (SubAgentDonePayload) -> Unit = {},
        // FIX: capture session_id from message_start so conversation is remembered
        onSessionStart: (String) -> Unit = {},
        // FIX: notify caller when backend is ready (after process start)
        onBackendReady: () -> Unit = {},
        // FIX: route sub-agent events to console log
        onSubAgentLog: (agentId: String, type: String, text: String) -> Unit = { _, _, _ -> }
    ) {
        when (event) {
            is IpcEvent.Status -> {
                // B04: map "backtracking" (A03) to THINKING so UI spinner stays active.
                // The full set of backend states: idle | thinking | executing |
                // waiting_approval | backtracking
                val state = event.payload.state.uppercase()
                // Sub-agent status changes go to log only, not main spinner
                val agentId = event.agentId
                if (agentId != null) {
                    onSubAgentLog(agentId, "status", state)
                    return
                }
                onStatusChange(if (state == "BACKTRACKING") "THINKING" else state)
            }
            is IpcEvent.MessageStart -> {
                // FIX: capture session_id so subsequent messages continue the same session
                onStatusChange("THINKING")
                onSessionStart(event.payload.session_id)
            }
            is IpcEvent.TextDelta -> {
                val agentId = event.agentId
                if (agentId != null) {
                    // Sub-agent text goes to console log only (not main conversation)
                    onSubAgentLog(agentId, "text", event.payload.text.take(80))
                    return
                }
                val lastMsg = currentMessages.lastOrNull()
                if (lastMsg != null && !lastMsg.isFromUser && lastMsg.type == MessageType.TEXT) {
                    onLastMessageUpdated(lastMsg.copy(text = lastMsg.text + event.payload.text))
                } else {
                    onMessageAdded(
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
                onMessageAdded(
                    Message(
                        id = "err-${System.currentTimeMillis()}",
                        sender = "System",
                        text = "❌ [${event.payload.code}] ${event.payload.message}",
                        isFromUser = false,
                        type = MessageType.SYSTEM,
                        agentId = event.agentId
                    )
                )
                onError(event.payload)
                onStatusChange("IDLE")
            }
            is IpcEvent.ToolCall -> {
                val agentId = event.agentId
                if (agentId != null) {
                    // Sub-agent tool call: goes to console log with full details
                    onSubAgentLog(agentId, "tool_call", "⚙️ ${event.payload.tool}(${event.payload.args})")
                    return
                }
                onMessageAdded(
                    Message(
                        id = "tool-${event.payload.id}",
                        sender = "Tool",
                        text = "⚙️ ${event.payload.tool}(${event.payload.args})",
                        isFromUser = false,
                        type = MessageType.ACTION
                    )
                )
            }
            // B02: append streaming subprocess output lines to the tool call bubble
            is IpcEvent.ToolOutputDelta -> {
                val toolMsgId = "tool-${event.payload.id}"
                val existing = currentMessages.lastOrNull { it.id == toolMsgId }
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
            is IpcEvent.ToolResult -> {
                val body = if (event.payload.error != null)
                    "❌ ${event.payload.error}"
                else
                    "✅ ${event.payload.result.take(300)}${if (event.payload.result.length > 300) "…" else ""}"
                val agentId = event.agentId
                if (agentId != null) {
                    // Sub-agent result: goes to console log
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
            is IpcEvent.Thought -> {
                onMessageAdded(
                    Message(
                        id = "thought-${System.currentTimeMillis()}",
                        sender = if (event.agentId != null) "Sub-Agent Thought" else "Thought",
                        text = "💭 ${event.payload.text}",
                        isFromUser = false,
                        type = MessageType.SYSTEM,
                        agentId = event.agentId
                    )
                )
            }
            is IpcEvent.Log -> onLogReceived(event.payload)
            is IpcEvent.Scratchpad -> onScratchpadUpdate(event.payload.content)
            is IpcEvent.TerminalTraffic -> onTerminalTraffic(event.payload)
            is IpcEvent.IndexingProgress -> onIndexingProgress(event.payload)
            is IpcEvent.PluginMetadata -> onPluginsLoaded(event.payload)
            is IpcEvent.WorkflowStatus -> onWorkflowsUpdate(event.payload)
            is IpcEvent.VoiceStatus -> onVoiceUpdate(event.payload)
            is IpcEvent.ContextSuggestions -> onContextSuggestions(event.payload.suggestions)
            is IpcEvent.SessionData -> {
                onSessionData(event.payload)
            }
            is IpcEvent.HumanInputRequest -> onHumanInputRequest(event.payload)
            is IpcEvent.AgentGroupUpdate -> onAgentGroupUpdate(event.payload)
            is IpcEvent.SessionForked -> onSessionForked(event.payload)
            is IpcEvent.TaskScheduled -> onTaskScheduled(event.payload)
            is IpcEvent.ScheduledTasksList -> onScheduledTasksList(event.payload)
            is IpcEvent.ModelsList -> onModelsList(event.payload.backend, event.payload.models)
            is IpcEvent.SubAgentDone -> {
                onSubAgentDone(event.payload)
                onSubAgentLog(
                    event.payload.agent_id,
                    "done",
                    "${if (event.payload.success) "✅" else "❌"} ${event.payload.summary.take(120)}"
                )
            }
            is IpcEvent.Ready -> {
                onStatusChange("IDLE")
                onBackendReady()
            }
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
        onMessageAdded: (Message) -> Unit,
        onClearInput: () -> Unit,
        onClearAttachments: () -> Unit,
        onStatusChange: (String) -> Unit,
        // B06: pass working directory to backend
        workingDir: String? = null
    ) {
        val msgId = "msg-${System.currentTimeMillis()}"
        val attachList = attachments.takeIf { it.isNotEmpty() }
        val userMsg = Message(msgId, "User", text, true, MessageType.TEXT, attachList)
        onStatusChange("THINKING")
        onClearInput()
        onClearAttachments()
        onMessageAdded(userMsg)

        scope.launch {
            try {
                val payload = SendMessagePayload(
                    session_id = sessionId,
                    text = text,
                    attachments = attachList,
                    include_stats = true,
                    working_dir = workingDir?.takeIf { it.isNotBlank() }
                )
                when (mode) {
                    ConnectionMode.IPC -> client.sendCommand(IpcCommand.SendMessage(payload))
                    ConnectionMode.STDIO -> stdioExecutor.sendCommand(IpcCommand.SendMessage(payload))
                    ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(IpcCommand.SendMessage(payload))
                    ConnectionMode.CLI -> {
                        val result = cliExecutor.executeCommand(text)
                        onMessageAdded(Message("cli-${System.currentTimeMillis()}", "Agent", result, false, MessageType.TEXT))
                        onStatusChange("IDLE")
                    }
                }
            } catch (e: Exception) {
                onMessageAdded(Message("err-${System.currentTimeMillis()}", "System", "Error: ${e.message}", false, MessageType.SYSTEM))
                onStatusChange("IDLE")
            }
        }
    }
}
