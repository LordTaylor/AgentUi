// Handles all incoming IpcEvents from the Rust backend and routes state updates.
// Used by ChatViewModel.handleIpcEvent(); all callbacks are extracted as named functions.
// See: docs/COMMUNICATION.md for full event catalogue (v1.6+).
package com.agentcore.ui.chat

import androidx.compose.runtime.MutableState
import com.agentcore.api.*
import com.agentcore.logic.IpcHandler
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.StdioExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

class ChatIpcEventHandlers(
    private val uiState: MutableState<ChatUiState>,
    private val sessionVM: SessionViewModel,
    private val lastRestartTime: () -> Long,
    private val setLastRestartTime: (Long) -> Unit,
    private val stdioExecutor: StdioExecutor,
    private val log: (String, String, String) -> Unit,
    private val syncSessions: () -> Unit,
    private val syncApprovalMode: () -> Unit,
    private val getScope: () -> kotlinx.coroutines.CoroutineScope?,
) {
    private fun update(block: ChatUiState.() -> ChatUiState) { uiState.value = uiState.value.block() }
    private val st get() = uiState.value

    private var messageStartTime = 0L

    fun handle(event: IpcEvent) {
        if (event is IpcEvent.Status)     log("←", "status_raw",  "state=${event.payload.state} agent=${event.agentId}")
        if (event is IpcEvent.ToolCall)   log("←", "tool_call",   "${event.payload.tool}(${event.payload.id.take(6)})")
        if (event is IpcEvent.ToolResult) log("←", "tool_result", "id=${event.payload.id.take(6)} len=${event.payload.result.length}")
        if (event is IpcEvent.MessageEnd) log("←", "message_end", "in=${event.payload.usage?.input_tokens} out=${event.payload.usage?.output_tokens}")
        if (event is IpcEvent.Stats)      log("←", "stats",       "ctx=${event.payload["context_window_tokens"]}/${event.payload["context_window_limit"]} iter=${event.payload["agent_iterations"]}")

        // A12: memory list handled directly before IpcHandler routing
        if (event is IpcEvent.MemoryList) { update { copy(memoryFacts = event.payload.facts) }; return }

        if (event is IpcEvent.MessageStart) messageStartTime = System.currentTimeMillis()

        IpcHandler.handleIpcEvent(
            event                  = event,
            getCurrentMessages     = { st.messages },
            onMessageAdded         = { msg -> update { copy(messages = messages + msg) }; sessionVM.saveSessionCache() },
            onLastMessageUpdated   = { msg ->
                val msgs = st.messages.toMutableList()
                val idx = msgs.indexOfLast { it.id == msg.id }
                if (idx >= 0) { msgs[idx] = msg; update { copy(messages = msgs) }; sessionVM.saveSessionCache() }
            },
            onStatusChange         = { state -> onStatusChange(state, event) },
            onStatsUpdate          = { onStatsUpdate(it) },
            onApprovalRequest      = { update { copy(pendingApproval = it) } },
            onLogReceived          = { update { copy(logs = logs + it) } },
            onScratchpadUpdate     = { update { copy(scratchpadContent = it) } },
            onTerminalTraffic      = { update { copy(terminalTraffic = terminalTraffic + it) } },
            onIndexingProgress     = { },
            onPluginsLoaded        = { update { copy(plugins = it) } },
            onWorkflowsUpdate      = { update { copy(workflows = it) } },
            onVoiceUpdate          = { },
            onContextSuggestions   = { update { copy(suggestedContext = it) } },
            onSkillsUpdate         = { update { copy(availableSkills = it) } },
            onSessionsUpdate       = { sessions -> update { copy(sessions = sessions) }; sessionVM.saveSessionCache() },
            onError                = { onError(it) },
            onSessionData          = { onSessionData(it) },
            onHumanInputRequest    = { update { copy(pendingHumanInput = it) } },
            onAgentGroupUpdate     = { update { copy(agentGroup = it) } },
            onSessionForked        = { payload ->
                update { copy(currentSessionId = payload.new_session_id, messages = emptyList()) }
                syncSessions()
            },
            onTaskScheduled        = { update { copy(statusState = "Task Scheduled: ${it.next_fire}") } },
            onScheduledTasksList   = { },
            onModelsList           = { backend, models ->
                update { copy(availableModels = availableModels + (backend to models)) }
            },
            onToolOutputDelta      = { delta ->
                val msgs = st.messages.toMutableList()
                val idx = msgs.indexOfLast { it.id == "tool-${delta.id}" }
                if (idx >= 0) { msgs[idx] = msgs[idx].copy(text = msgs[idx].text + "\n" + delta.line); update { copy(messages = msgs) } }
                update { copy(toolOutput = toolOutput + delta.line, showToolOutput = true) }
            },
            onSubAgentDone         = { log("←", "sub_agent_done", "agent=${it.agent_id.take(8)} success=${it.success}") },
            onSessionStart         = { sessionId ->
                if (st.currentSessionId != sessionId) {
                    update { copy(currentSessionId = sessionId) }
                    log("←", "session_bound", "sid=${sessionId.take(8)}")
                    sessionVM.saveSessionCache()
                    syncSessions()
                }
            },
            onBackendReady         = { syncApprovalMode() },
            onSubAgentLog          = { agentId, type, text -> log("←", "[sub:${agentId.take(6)}] $type", text.take(60)) },
            onPlanReady            = { update { copy(pendingPlan = it) } },
            onToolProgress         = { log("IN", "tool_progress", it.message) },
            onWorkflowGroupStatus  = { onWorkflowGroupStatus(it) }
        )

        if (event is IpcEvent.MessageEnd) {
            val elapsed = System.currentTimeMillis() - messageStartTime
            if (elapsed > 300) {
                val lastAgentMsg = st.messages.lastOrNull { !it.isFromUser && it.type == MessageType.TEXT }
                if (lastAgentMsg != null) {
                    val outputTokens = event.payload.usage?.output_tokens?.toFloat()
                        ?: (lastAgentMsg.text.length / 4.0f)
                    val tps = outputTokens / (elapsed / 1000f)
                    if (tps > 0.1f) {
                        val msgs = st.messages.toMutableList()
                        val idx = msgs.indexOfLast { it.id == lastAgentMsg.id }
                        if (idx >= 0) {
                            msgs[idx] = msgs[idx].copy(tokensPerSec = tps)
                            update { copy(messages = msgs) }
                        }
                    }
                }
            }
        }
    }

    private fun onStatusChange(state: String, event: IpcEvent) {
        update { copy(statusState = state) }
        if (state == "IDLE") syncSessions()
        if (event is IpcEvent.Status) {
            val payload = event.payload
            if (payload.updated_key == "approval_mode") {
                val value = payload.value?.toString()?.toBoolean() ?: true
                update { copy(approvalMode = value) }
            }
            payload.backend?.let { backend ->
                update { copy(currentBackend = backend, currentModelName = uiSettings.providerConfigs[backend]?.model ?: currentModelName) }
            }
        }
    }

    private fun onStatsUpdate(stats: kotlinx.serialization.json.JsonObject) {
        val current = st.sessionStats
        val updated = when {
            stats.containsKey("usage") -> stats
            stats.containsKey("input_tokens") && current != null ->
                buildJsonObject { current.forEach { (k, v) -> put(k, v) }; stats.forEach { (k, v) -> put(k, v) } }
            else -> stats
        }
        update { copy(sessionStats = updated) }
        try {
            val usage = if (updated.containsKey("usage")) {
                updated["usage"]?.let { Json.decodeFromJsonElement(UsagePayload.serializer(), it) }
            } else if (updated.containsKey("input_tokens")) {
                Json.decodeFromJsonElement(UsagePayload.serializer(), updated)
            } else null
            if (usage != null) update { copy(tokenHistory = tokenHistory + usage) }
        } catch (_: Exception) {}
    }

    private fun onError(error: ErrorPayload) {
        val currentStatus = st.statusState
        if (error.code == "STDIO_EXITED" && error.message.contains("137")) {
            log("→", "crash_detected", "OOM (137) detected")
            val now = System.currentTimeMillis()
            val tooFrequent = (now - lastRestartTime()) < 5000
            if (currentStatus == "IDLE" && !tooFrequent) {
                log("→", "auto_restart", "Attempting automatic restart...")
                setLastRestartTime(now)
                getScope()?.launch(Dispatchers.IO) { stdioExecutor.restart() }
            } else {
                if (tooFrequent) log("→", "crash_loop_prevented", "Too frequent — stopping auto-restart")
                update { copy(statusState = "CRASHED") }
            }
        } else if (error.code == "BACKEND_ERROR" || error.code == "CONNECTION_FAILED") {
            val backend = st.currentBackend
            if (backend == "ollama" || backend == "lmstudio") {
                log("→", "recovery_suggested", "Backend $backend failed. Triggering auto-recovery...")
                // Note: RestartProvider intent handled by ProviderViewModel — emit directly
                val scope = getScope() ?: return
                val cmd = IpcCommand.RestartProvider(RestartProviderPayload(backend, st.uiSettings.providerConfigs[backend]?.model))
                scope.launch { stdioExecutor.sendCommand(cmd) }
            }
        }
    }

    private fun onSessionData(payload: SessionDataPayload) {
        if (st.currentSessionId != payload.session_id) {
            update { copy(messages = emptyList(), currentSessionId = payload.session_id) }
        }
        update {
            copy(
                currentSystemPrompt = payload.system_prompt ?: "",
                currentBackend = payload.backend,
                currentModelName = uiSettings.providerConfigs[payload.backend]?.model ?: ""
            )
        }
        sessionVM.saveSessionCache()
    }

    private fun onWorkflowGroupStatus(payload: AgentWorkflowStatusPayload) {
        update { copy(workflowGroupStatus = payload) }
        log("←", "agent_workflow_status", "gid=${payload.group_id.take(8)} state=${payload.state} step=${payload.step}/${payload.total_steps}")
        if (payload.state == "complete" || payload.state == "failed") {
            val terminalMsg = Message(
                id = "workflow-${payload.group_id.take(8)}",
                sender = "System",
                text = if (payload.state == "complete") "✅ Workflow complete (${payload.total_steps} steps)" else "❌ Workflow failed at step ${payload.step}/${payload.total_steps}",
                isFromUser = false,
                type = MessageType.SYSTEM
            )
            update { copy(messages = messages + terminalMsg) }
        }
    }
}
