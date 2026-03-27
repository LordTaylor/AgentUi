// Thin IPC event dispatcher: routes backend events to router functions and ViewModel callbacks.
// Message/thought events → IpcMessageEventRouter; tool events → IpcToolEventRouter.
// performSendMessage handles outbound send_message command for all connection modes.

package com.agentcore.logic

import com.agentcore.api.*
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object IpcHandler {
    private var idCounter = 0

    /** Formats tool args as a compact human-readable string instead of raw JSON.
     *  Single-arg tools show just the value; multi-arg tools show key=value pairs.
     *  Long values are truncated at 80 chars. */
    internal fun formatToolArgs(args: JsonElement): String {
        if (args !is JsonObject || args.isEmpty()) return ""
        val entries = args.entries.toList()
        return if (entries.size == 1) {
            val v = entries[0].value
            val raw = if (v is JsonPrimitive) v.content else v.toString()
            raw.take(80) + if (raw.length > 80) "…" else ""
        } else {
            entries.take(3).joinToString(", ") { (k, v) ->
                val raw = if (v is JsonPrimitive) v.content else v.toString()
                val short = raw.take(50) + if (raw.length > 50) "…" else ""
                "$k=$short"
            }
        }
    }

    fun nextId(prefix: String): String {
        return "$prefix-${System.currentTimeMillis()}-${idCounter++}"
    }

    fun handleIpcEvent(
        event: IpcEvent,
        getCurrentMessages: () -> List<Message>,
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
        onToolOutputDelta: (ToolOutputDeltaPayload) -> Unit = {},
        onSubAgentDone: (SubAgentDonePayload) -> Unit = {},
        onSessionStart: (String) -> Unit = {},
        onBackendReady: () -> Unit = {},
        onSubAgentLog: (agentId: String, type: String, text: String) -> Unit = { _, _, _ -> },
        onPlanReady: (PlanReadyPayload) -> Unit = {},
        onSubAgentThought: (agentId: String, text: String) -> Unit = { _, _ -> },
        onToolProgress: (ToolProgressPayload) -> Unit = {},
        onSkillsUpdate: (List<SkillInfo>) -> Unit = {},
        onSessionsUpdate: (List<SessionInfo>) -> Unit = {},
        onWorkflowGroupStatus: (AgentWorkflowStatusPayload) -> Unit = {}
    ) {
        when (event) {
            is IpcEvent.SessionsList -> onSessionsUpdate(event.payload.sessions)
            is IpcEvent.Status -> {
                // B04: map "backtracking" (A03) to THINKING so UI spinner stays active.
                val state = event.payload.state.uppercase()
                val agentId = event.agentId
                if (agentId != null) { onSubAgentLog(agentId, "status", state); return }
                onStatusChange(if (state == "BACKTRACKING") "THINKING" else state)
            }
            is IpcEvent.MessageStart -> handleMessageStart(event, onStatusChange, onSessionStart)
            is IpcEvent.TextDelta -> handleTextDelta(event, getCurrentMessages, onMessageAdded, onLastMessageUpdated, onSubAgentLog)
            is IpcEvent.MessageEnd -> handleMessageEnd(event, onStatusChange, onStatsUpdate)
            is IpcEvent.Stats -> onStatsUpdate(event.payload)
            is IpcEvent.ApprovalRequest -> onApprovalRequest(event.payload)
            is IpcEvent.Error -> {
                onMessageAdded(
                    Message(
                        id = nextId("err"),
                        sender = "System",
                        text = "❌ [${event.payload.code}] ${event.payload.message}",
                        isFromUser = false,
                        type = MessageType.ERROR,
                        agentId = event.agentId
                    )
                )
                onError(event.payload)
                onStatusChange("IDLE")
            }
            is IpcEvent.ToolCall -> handleToolCall(event, onMessageAdded, onSubAgentLog, ::formatToolArgs)
            is IpcEvent.ToolOutputDelta -> handleToolOutputDelta(event, getCurrentMessages, onMessageAdded, onLastMessageUpdated, onToolOutputDelta)
            is IpcEvent.ToolResult -> handleToolResult(event, onMessageAdded, onSubAgentLog)
            is IpcEvent.Thought -> handleThought(event, getCurrentMessages, onMessageAdded, onLastMessageUpdated, onSubAgentThought)
            is IpcEvent.Log -> onLogReceived(event.payload)
            is IpcEvent.Scratchpad -> onScratchpadUpdate(event.payload.content)
            is IpcEvent.TerminalTraffic -> onTerminalTraffic(event.payload)
            is IpcEvent.IndexingProgress -> onIndexingProgress(event.payload)
            is IpcEvent.PluginMetadata -> onPluginsLoaded(event.payload)
            is IpcEvent.WorkflowStatus -> onWorkflowsUpdate(event.payload)
            is IpcEvent.VoiceStatus -> onVoiceUpdate(event.payload)
            is IpcEvent.ContextSuggestions -> onContextSuggestions(event.payload.suggestions)
            is IpcEvent.SessionData -> onSessionData(event.payload)
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
            is IpcEvent.Ready -> { onStatusChange("IDLE"); onBackendReady() }
            is IpcEvent.PlanReady -> { onPlanReady(event.payload); onStatusChange("WAITING_APPROVAL") }
            is IpcEvent.ToolProgress -> onToolProgress(event.payload)
            is IpcEvent.ToolCreated -> {
                onMessageAdded(
                    Message(
                        id = nextId("tool-created"),
                        sender = "System",
                        text = "🛠️ New tool created: ${event.payload.name} (${event.payload.path})",
                        isFromUser = false,
                        type = MessageType.SYSTEM
                    )
                )
            }
            is IpcEvent.AgentQuery -> {
                onMessageAdded(
                    Message(
                        id = "query-${event.payload.query_id}",
                        sender = "System",
                        text = "🔍 Delegating to ${event.payload.role}: ${event.payload.question}",
                        isFromUser = false,
                        type = MessageType.SYSTEM,
                        agentId = event.agentId
                    )
                )
            }
            is IpcEvent.AgentQueryResponse -> {
                onMessageAdded(
                    Message(
                        id = "response-${event.payload.query_id}",
                        sender = "System",
                        text = "💡 Response from delegation: ${event.payload.answer}",
                        isFromUser = false,
                        type = MessageType.SYSTEM,
                        agentId = event.agentId
                    )
                )
            }
            is IpcEvent.SkillsList -> onSkillsUpdate(event.payload.skills)
            is IpcEvent.AgentWorkflowStatus -> onWorkflowGroupStatus(event.payload)
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
        workingDir: String? = null
    ) {
        val msgId = nextId("msg")
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
                    images = attachList,
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
