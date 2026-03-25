package com.agentcore.ui.chat

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.agentcore.api.*
import com.agentcore.logic.IpcHandler
import com.agentcore.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class ChatViewModel(
    private val client: AgentClient,
    private val cliExecutor: CliExecutor,
    private val stdioExecutor: StdioExecutor,
    private val unixSocketExecutor: UnixSocketExecutor,
    private val settingsManager: SettingsManager,
    private val sessionCache: SettingsManager
) {
    private var cachedData = SessionCache()
    private var isInitialized = false
    private var viewModelScope: CoroutineScope? = null
    private var currentMode: ConnectionMode = ConnectionMode.STDIO
    // Counts of incoming events by type — for debug log
    private val incomingEventCounts = mutableMapOf<String, Int>()
    private var saveJob: Job? = null
    private var lastRestartTime: Long = 0L
    private val _uiState = mutableStateOf(ChatUiState())
    val uiState: State<ChatUiState> = _uiState

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    private val logDateFmt = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)

    private fun addIpcLog(direction: String, name: String, summary: String = "") {
        val ts = logDateFmt.format(java.util.Date())
        val entry = "$ts  $direction  ${name.padEnd(22)}$summary".trimEnd()
        _uiState.value = _uiState.value.copy(
            ipcLogs = _uiState.value.ipcLogs.let { list ->
                val next = list + entry
                if (next.size > 1000) next.drop(next.size - 1000) else next
            }
        )
        if (direction == "←") {
            incomingEventCounts[name] = (incomingEventCounts[name] ?: 0) + 1
        }
    }

    private fun ipcEventName(event: IpcEvent) = when (event) {
        is IpcEvent.TextDelta -> "text_delta"
        is IpcEvent.Status -> "status"
        is IpcEvent.MessageStart -> "message_start"
        is IpcEvent.MessageEnd -> "message_end"
        is IpcEvent.ToolCall -> "tool_call"
        is IpcEvent.ToolResult -> "tool_result"
        is IpcEvent.Error -> "error"
        is IpcEvent.Log -> "log"
        is IpcEvent.Stats -> "stats"
        is IpcEvent.ApprovalRequest -> "approval_request"
        is IpcEvent.HumanInputRequest -> "human_input_request"
        is IpcEvent.SessionData -> "session_data"
        is IpcEvent.SessionForked -> "session_forked"
        is IpcEvent.Ready -> "ready"
        is IpcEvent.Thought -> "thought"
        is IpcEvent.TerminalTraffic -> "terminal_traffic"
        is IpcEvent.Scratchpad -> "scratchpad"
        is IpcEvent.ContextSuggestions -> "context_suggestions"
        is IpcEvent.TaskScheduled -> "task_scheduled"
        is IpcEvent.ScheduledTasksList -> "scheduled_tasks_list"
        is IpcEvent.IndexingProgress -> "indexing_progress"
        is IpcEvent.PluginMetadata -> "plugin_metadata"
        is IpcEvent.WorkflowStatus -> "workflow_status"
        is IpcEvent.ModelsList -> "models_list"
        is IpcEvent.ToolOutputDelta -> "tool_output_delta"
        is IpcEvent.SubAgentDone -> "sub_agent_done"
        else -> event::class.simpleName ?: "unknown"
    }

    private fun ipcEventSummary(event: IpcEvent) = when (event) {
        is IpcEvent.TextDelta -> "+${event.payload.text.length}ch"
        is IpcEvent.Status -> event.payload.state
        is IpcEvent.ToolCall -> event.payload.tool
        is IpcEvent.ToolResult -> if (event.payload.error != null) "ERR" else "ok"
        is IpcEvent.Error -> "[${event.payload.code}] ${event.payload.message.take(40)}"
        is IpcEvent.Log -> "${event.payload.level}: ${event.payload.message.take(40)}"
        is IpcEvent.ApprovalRequest -> event.payload.tool
        is IpcEvent.HumanInputRequest -> "\"${event.payload.question.take(30)}\""
        is IpcEvent.SessionData -> "sid=${event.payload.session_id?.take(8)}"
        is IpcEvent.SessionForked -> "new=${event.payload.new_session_id.take(8)}"
        is IpcEvent.ModelsList -> "${event.payload.models.size} models"
        is IpcEvent.SubAgentDone -> "agent=${event.payload.agent_id.take(8)} ${if (event.payload.success) "ok" else "FAIL"}"
        else -> ""
    }

    fun init(scope: CoroutineScope, mode: ConnectionMode) {
        if (isInitialized) return
        isInitialized = true
        this.viewModelScope = scope
        this.currentMode = mode
        
        // Load persistent UI settings (including workingDir and providerConfigs)
        settingsManager.load(UiSettings.serializer())?.let { saved ->
            _uiState.value = _uiState.value.copy(
                uiSettings = saved,
                workingDir = saved.workingDir.ifEmpty { System.getProperty("user.home") ?: "" },
                currentModelName = saved.providerConfigs[_uiState.value.currentBackend]?.model ?: ""
            )
            // Sync auto-accept preference to backend
            onIntent(ChatIntent.UpdateConfig("approval_mode", kotlinx.serialization.json.JsonPrimitive(!saved.autoAccept)), scope, mode)

            // Inject provider env vars before starting the executor
            if (mode == ConnectionMode.STDIO) {
                val envVars = com.agentcore.ui.components.buildAllEnvVars(saved.providerConfigs)
                if (envVars.isNotEmpty()) stdioExecutor.setEnvVars(envVars)
            }
        }

        // Load session cache for offline support
        sessionCache.load(SessionCache.serializer())?.let { cache ->
            this.cachedData = cache
            _uiState.value = _uiState.value.copy(
                sessions = cache.sessions,
                sessionFolders = cache.sessionFolders
            )
        }

        scope.launch {
            val eventHandler = { event: IpcEvent ->
                handleIpcEvent(event)
            }

            when (mode) {
                ConnectionMode.STDIO -> {
                    stdioExecutor.start()
                    stdioExecutor.events.collect { eventHandler(it) }
                }
                ConnectionMode.UNIX_SOCKET -> {
                    unixSocketExecutor.start(scope)
                    unixSocketExecutor.events.collect { eventHandler(it) }
                }
                ConnectionMode.IPC -> {
                    val initialSessions = client.listSessions()
                    if (initialSessions.isNotEmpty()) {
                        cachedData = cachedData.copy(sessions = initialSessions)
                        scope.launch(Dispatchers.IO) {
                            sessionCache.save(cachedData, SessionCache.serializer())
                        }
                    }
                    _uiState.value = _uiState.value.copy(sessions = if (initialSessions.isEmpty()) cachedData.sessions else initialSessions)

                    scope.launch {
                        val tools = client.listTools()
                        val backends = client.listBackends()
                        val skills = client.sendCommand(IpcCommand.ListSkills()) // assuming this returns something or we wait for event
                        _uiState.value = _uiState.value.copy(availableTools = tools, availableBackends = backends)
                        // Trigger skill fetch for all modes
                        onIntent(ChatIntent.ReloadSkills, scope, mode)
                    }
                    client.observeEvents().collect { eventHandler(it) }
                }
                else -> {}
            }
        }
    }

    private fun handleIpcEvent(event: IpcEvent) {
        // Detailed logging for status debugging
        if (event is IpcEvent.Status) {
            addIpcLog("←", "status_raw", "state=${event.payload.state} agent=${event.agentId}")
        }
        addIpcLog("←", ipcEventName(event), ipcEventSummary(event))
        val currentState = _uiState.value

        IpcHandler.handleIpcEvent(
            event = event,
            currentMessages = currentState.messages,
            onMessageAdded = { msg ->
                _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
                saveSessionCache()
            },
            onLastMessageUpdated = { msg ->
                val msgs = _uiState.value.messages.toMutableList()
                if (msgs.isNotEmpty()) {
                    msgs[msgs.size - 1] = msg
                    _uiState.value = _uiState.value.copy(messages = msgs)
                    saveSessionCache()
                }
            },
            onStatusChange = { state -> 
                _uiState.value = _uiState.value.copy(statusState = state)
                if (state == "IDLE") {
                    syncSessions()
                }
                if (event is IpcEvent.Status) {
                    val payload = event.payload
                    if (payload.updated_key == "approval_mode") {
                        val value = payload.value?.toString()?.toBoolean() ?: true
                        _uiState.value = _uiState.value.copy(approvalMode = value)
                    }
                    val backend = payload.backend
                    if (backend != null) {
                        _uiState.value = _uiState.value.copy(
                            currentBackend = backend,
                            currentModelName = _uiState.value.uiSettings.providerConfigs[backend]?.model ?: _uiState.value.currentModelName
                        )
                    }
                }
            },
            onStatsUpdate = { stats ->
                val current = _uiState.value.sessionStats
                // Merge if it's partial usage from MessageEnd, otherwise replace
                val updated = if (stats.containsKey("usage")) {
                    stats // it's a full stats object
                } else if (stats.containsKey("input_tokens") && current != null) {
                    // It's a partial usage update (from MessageEnd) — merge with current to preserve context_window_tokens
                    buildJsonObject {
                        current.forEach { (k, v) -> put(k, v) }
                        stats.forEach { (k, v) -> put(k, v) }
                    }
                } else {
                    stats
                }
                
                _uiState.value = _uiState.value.copy(sessionStats = updated)
                
                // Also extract usage for historical tracking
                try {
                    val usage = if (updated.containsKey("usage")) {
                        updated["usage"]?.let { u -> Json.decodeFromJsonElement<UsagePayload>(u) }
                    } else if (updated.containsKey("input_tokens")) {
                        Json.decodeFromJsonElement<UsagePayload>(updated)
                    } else null
                    
                    if (usage != null) {
                        _uiState.value = _uiState.value.copy(tokenHistory = _uiState.value.tokenHistory + usage)
                    }
                } catch (_: Exception) {}
            },
            onApprovalRequest = { _uiState.value = _uiState.value.copy(pendingApproval = it) },
            onLogReceived = { log -> 
                _uiState.value = _uiState.value.copy(logs = _uiState.value.logs + log) 
            },
            onScratchpadUpdate = { _uiState.value = _uiState.value.copy(scratchpadContent = it) },
            onTerminalTraffic = { traffic -> 
                _uiState.value = _uiState.value.copy(terminalTraffic = _uiState.value.terminalTraffic + traffic) 
            },
            onIndexingProgress = { },
            onPluginsLoaded = { _uiState.value = _uiState.value.copy(plugins = it) },
            onWorkflowsUpdate = { _uiState.value = _uiState.value.copy(workflows = it) },
            onVoiceUpdate = { },
            onContextSuggestions = { _uiState.value = _uiState.value.copy(suggestedContext = it) },
            onSkillsUpdate = { skills ->
                _uiState.value = _uiState.value.copy(availableSkills = skills)
            },
            onSessionsUpdate = { sessions ->
                _uiState.value = _uiState.value.copy(sessions = sessions)
                saveSessionCache()
            },
            onError = { error ->
                if (error.code == "STDIO_EXITED" && error.message.contains("137")) {
                    addIpcLog("→", "crash_detected", "OOM (137) detected")
                    
                    val now = System.currentTimeMillis()
                    val tooFrequent = (now - lastRestartTime) < 5000 // 5 seconds threshold

                    // If we were just IDLE and it's not a rapid-fire crash, try one auto-restart
                    if (currentState.statusState == "IDLE" && !tooFrequent) {
                        addIpcLog("→", "auto_restart", "Attempting automatic restart...")
                        lastRestartTime = now
                        viewModelScope?.launch(Dispatchers.IO) {
                            stdioExecutor.restart()
                        }
                    } else {
                        // If it's too frequent or during work, STOP and let user handle it
                        if (tooFrequent) {
                            addIpcLog("→", "crash_loop_prevented", "Crash too frequent — stopping auto-restart")
                        }
                        _uiState.value = _uiState.value.copy(statusState = "CRASHED")
                    }
                } else if (error.code == "BACKEND_ERROR" || error.code == "CONNECTION_FAILED") {
                    val backend = currentState.currentBackend
                    if (backend == "ollama" || backend == "lmstudio") {
                        addIpcLog("→", "recovery_suggested", "Backend $backend failed. Triggering auto-recovery...")
                        onIntent(ChatIntent.RestartProvider(backend), viewModelScope!!, currentMode)
                    }
                }
            },
            onSessionData = { payload ->
                // Only clear messages if we're not already loading them or if the session changed
                if (_uiState.value.currentSessionId != payload.session_id) {
                    _uiState.value = _uiState.value.copy(messages = emptyList(), currentSessionId = payload.session_id)
                }
                _uiState.value = _uiState.value.copy(
                    currentSystemPrompt = payload.system_prompt ?: "",
                    currentBackend = payload.backend,
                    currentModelName = _uiState.value.uiSettings.providerConfigs[payload.backend]?.model ?: ""
                )
                saveSessionCache()
            },
            onHumanInputRequest = { _uiState.value = _uiState.value.copy(pendingHumanInput = it) },
            onAgentGroupUpdate = { _uiState.value = _uiState.value.copy(agentGroup = it) },
            onSessionForked = { payload ->
                _uiState.value = _uiState.value.copy(currentSessionId = payload.new_session_id, messages = emptyList())
                val currentScope = viewModelScope
                if (currentScope != null) {
                    currentScope.launch {
                        val sessionData = client.getSession(payload.new_session_id)
                        syncSessions()
                    }
                }
            },
            onTaskScheduled = { payload ->
                _uiState.value = _uiState.value.copy(statusState = "Task Sceduled: ${payload.next_fire}")
            },
            onScheduledTasksList = { payload ->
                // Handle as needed, e.g. update a dedicated list
            },
            onModelsList = { backend, models ->
                _uiState.value = _uiState.value.copy(
                    availableModels = _uiState.value.availableModels + (backend to models)
                )
            },
            // B02: append streaming subprocess output lines to the matching tool bubble
            onToolOutputDelta = { delta ->
                val msgs = _uiState.value.messages.toMutableList()
                val idx = msgs.indexOfLast { it.id == "tool-${delta.id}" }
                if (idx >= 0) {
                    msgs[idx] = msgs[idx].copy(text = msgs[idx].text + "\n" + delta.line)
                    _uiState.value = _uiState.value.copy(messages = msgs)
                }
                // B02: also append to dedicated tool output list
                _uiState.value = _uiState.value.copy(
                    toolOutput = _uiState.value.toolOutput + delta.line,
                    showToolOutput = true // auto-show on activity
                )
            },
            onSubAgentDone = { payload ->
                addIpcLog("←", "sub_agent_done", "agent=${payload.agent_id.take(8)} success=${payload.success}")
            },
            // FIX 1: capture session_id from backend so conversation is remembered
            onSessionStart = { sessionId ->
                if (_uiState.value.currentSessionId != sessionId) {
                    _uiState.value = _uiState.value.copy(currentSessionId = sessionId)
                    addIpcLog("←", "session_bound", "sid=${sessionId.take(8)}")
                    saveSessionCache()
                    syncSessions()
                }
            },
            // FIX 2: sync approval_mode AFTER backend is ready to avoid race condition
            onBackendReady = {
                syncApprovalMode()
            },
            // FIX 3: route sub-agent events to IPC console log
            onSubAgentLog = { agentId, type, text ->
                val short = agentId.take(6)
                addIpcLog("←", "[sub:$short] $type", text.take(60))
            },
            onPlanReady = { payload ->
                _uiState.value = _uiState.value.copy(pendingPlan = payload)
            },
            onToolProgress = { payload ->
                // B3 fix: field renamed from chunk → message to match Rust ToolProgressPayload
                addIpcLog("IN", "tool_progress", payload.message)
            }
        )
    }

    /** Send approval_mode config to backend — called on Ready event to ensure process is up. */
    private fun syncApprovalMode() {
        val autoAccept = _uiState.value.uiSettings.autoAccept
        addIpcLog("→", "sync_approval_mode", "autoAccept=$autoAccept → approval_mode=${!autoAccept}")
        viewModelScope?.launch {
            val cmd = IpcCommand.UpdateConfig(UpdateConfigPayload("approval_mode", kotlinx.serialization.json.JsonPrimitive(!autoAccept)))
            when (currentMode) {
                ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                ConnectionMode.IPC -> client.sendCommand(cmd)
                else -> {}
            }
        }
    }

    private fun syncSessions() {
        val currentScope = viewModelScope ?: return
        currentScope.launch(Dispatchers.IO) {
            delay(500) // Small delay to let backend finalize DB writes
            if (currentMode == ConnectionMode.IPC) {
                try {
                    val sessions = client.listSessions()
                    _uiState.value = _uiState.value.copy(sessions = sessions)
                    saveSessionCache()
                } catch (e: Exception) {
                    addIpcLog("←", "sync_sessions_err", e.message ?: "unknown")
                }
            } else if (currentMode == ConnectionMode.STDIO || currentMode == ConnectionMode.UNIX_SOCKET) {
                // For non-HTTP modes, send command and wait for SessionsList event
                val cmd = IpcCommand.ListSessions()
                if (currentMode == ConnectionMode.STDIO) stdioExecutor.sendCommand(cmd)
                else unixSocketExecutor.sendCommand(cmd)
            }
        }
    }

    fun onIntent(intent: ChatIntent, scope: CoroutineScope, mode: ConnectionMode) {
        when (intent) {
            is ChatIntent.FetchModels -> {
                addIpcLog("→", "list_models", intent.backend)
                scope.launch {
                    val cmd = IpcCommand.ListModels(ListModelsPayload(intent.backend, intent.url))
                    when (mode) {
                        ConnectionMode.IPC -> client.sendCommand(cmd)
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                        else -> {}
                    }
                }
            }
            is ChatIntent.SendMessage -> {
                if (handleSlashCommand(intent.text, scope, mode)) return
                
                // Add to history if not empty and not same as last
                val history = _uiState.value.messageHistory.toMutableList()
                if (intent.text.isNotBlank() && history.lastOrNull() != intent.text) {
                    history.add(intent.text)
                }

                addIpcLog("→", "send_message", "\"${intent.text.take(40)}${if (intent.text.length > 40) "…" else ""}\"")
                IpcHandler.performSendMessage(
                    scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode,
                    intent.text, intent.images, _uiState.value.currentSessionId,
                    onMessageAdded = { msg ->
                        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
                        saveSessionCache()
                    },
                    onClearInput = { },
                    onClearAttachments = { },
                    onStatusChange = { _uiState.value = _uiState.value.copy(statusState = it) },
                    workingDir = _uiState.value.workingDir.takeIf { it.isNotBlank() } // B06
                )
                
                // Reset history index and input
                _uiState.value = _uiState.value.copy(
                    messageHistory = history,
                    historyIndex = null,
                    draftMessage = "",
                    inputText = ""
                )
            }
            is ChatIntent.SelectSession -> {
                addIpcLog("→", "get_session", intent.id.take(8))
                val cachedMsgs = cachedData.sessionMessages[intent.id] ?: emptyList()
                _uiState.value = _uiState.value.copy(currentSessionId = intent.id, messages = cachedMsgs)

                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.sendCommand(IpcCommand.GetSession(GetSessionPayload(intent.id)))
                    }
                }
            }
            ChatIntent.ToggleSettings -> {
                _uiState.value = _uiState.value.copy(showSettings = !_uiState.value.showSettings)
            }
            ChatIntent.RefreshStats -> {
                scope.launch {
                    val cmd = IpcCommand.GetStats()
                    if (mode == ConnectionMode.IPC) {
                        val stats = client.getStats()
                        _uiState.value = _uiState.value.copy(sessionStats = stats)
                    } else if (mode == ConnectionMode.STDIO) {
                        stdioExecutor.sendCommand(cmd)
                    }
                }
            }
            is ChatIntent.ResolveApproval -> {
                scope.launch {
                    val pending = _uiState.value.pendingApproval
                    if (pending != null) {
                        val cmd = IpcCommand.ApprovalResponse(ApprovalResponsePayload(pending.id, intent.approved))
                        if (mode == ConnectionMode.IPC) {
                            client.sendCommand(cmd)
                        } else if (mode == ConnectionMode.STDIO) {
                            stdioExecutor.sendCommand(cmd)
                        }
                    }
                    _uiState.value = _uiState.value.copy(pendingApproval = null)
                }
            }
            is ChatIntent.RespondHumanInput -> {
                // B2 fix: was incorrectly calling performSendMessage() which starts a NEW turn.
                // Must send human_input_response command so the agent unblocks its pending
                // HumanInputRequest channel — not inject a new user message.
                val pending = _uiState.value.pendingHumanInput
                _uiState.value = _uiState.value.copy(pendingHumanInput = null)
                if (pending != null) {
                    val cmd = IpcCommand.HumanInputResponse(
                        HumanInputResponsePayload(id = pending.id, answer = intent.answer)
                    )
                    scope.launch {
                        when (mode) {
                            ConnectionMode.IPC -> client.sendCommand(cmd)
                            ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                            ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                            ConnectionMode.CLI -> { /* CLI uses stdin directly */ }
                        }
                    }
                }
            }
            ChatIntent.CancelAction -> {
                scope.launch {
                    val sid = _uiState.value.currentSessionId
                    if (sid != null) {
                        // B03: cancel in all transport modes (was IPC-only before)
                        val cmd = IpcCommand.Cancel(CancelPayload(sid))
                        when (mode) {
                            ConnectionMode.IPC -> client.sendCommand(cmd)
                            ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                            ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                            ConnectionMode.CLI -> {}
                        }
                    }
                    _uiState.value = _uiState.value.copy(statusState = "IDLE")
                }
            }
            ChatIntent.ClearChat -> {
                _uiState.value = _uiState.value.copy(messages = emptyList())
            }
            is ChatIntent.UpdateSettings -> {
                _uiState.value = _uiState.value.copy(
                    currentBackend = intent.backend,
                    currentRole = intent.role,
                    showSettings = false
                )
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.sendCommand(IpcCommand.SetBackend(SetBackendPayload(intent.backend)))
                        client.sendCommand(IpcCommand.SetRole(SetRolePayload(intent.role)))
                    }
                }
            }
            is ChatIntent.UpdateScratchpad -> {
                _uiState.value = _uiState.value.copy(scratchpadContent = intent.content)
            }
            is ChatIntent.DeleteSession -> {
                val oldSessions = _uiState.value.sessions
                val oldCurrentId = _uiState.value.currentSessionId
                val oldMessages = _uiState.value.messages

                // Optimistic UI update
                _uiState.value = _uiState.value.copy(
                    sessions = _uiState.value.sessions.filter { it.id != intent.id },
                    currentSessionId = if (oldCurrentId == intent.id) null else oldCurrentId,
                    messages = if (oldCurrentId == intent.id) emptyList() else oldMessages
                )
                saveSessionCache()

                scope.launch {
                    try {
                        if (mode == ConnectionMode.IPC) {
                            client.deleteSession(intent.id)
                            // Final sync to ensure consistency
                            val updatedSessions = client.listSessions()
                            _uiState.value = _uiState.value.copy(sessions = updatedSessions)
                            saveSessionCache()
                        }
                    } catch (e: Exception) {
                        // Rollback on failure
                        _uiState.value = _uiState.value.copy(
                            sessions = oldSessions,
                            currentSessionId = oldCurrentId,
                            messages = oldMessages
                        )
                        saveSessionCache()
                        addIpcLog("←", "delete_failed", e.message ?: "network error")
                    }
                }
            }
            ChatIntent.ReloadTools -> {
                scope.launch {
                    val cmd = IpcCommand.ReloadTools()
                    when (mode) {
                        ConnectionMode.IPC -> client.sendCommand(cmd)
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                        else -> {}
                    }
                    if (mode == ConnectionMode.IPC) {
                        val tools = client.listTools()
                        _uiState.value = _uiState.value.copy(availableTools = tools)
                    }
                }
            }
            ChatIntent.ReloadSkills -> {
                scope.launch {
                    val cmd = IpcCommand.ListSkills()
                    when (mode) {
                        ConnectionMode.IPC -> client.sendCommand(cmd)
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                        else -> {}
                    }
                }
            }
            is ChatIntent.PruneSession -> {
                scope.launch {
                    val cmd = IpcCommand.PruneSession(PruneSessionPayload(intent.id, 6)) // Default to 6 recent
                    when (mode) {
                        ConnectionMode.IPC -> client.sendCommand(cmd)
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                        else -> {}
                    }
                    if (mode == ConnectionMode.IPC) {
                        client.pruneSession(intent.id)
                        if (_uiState.value.currentSessionId == intent.id) {
                            val sessions = client.listSessions()
                            _uiState.value = _uiState.value.copy(sessions = sessions)
                        }
                    }
                }
            }
            is ChatIntent.TagSession -> {
                scope.launch {
                    val cmd = IpcCommand.TagSession(TagSessionPayload(intent.id, intent.tags))
                    when (mode) {
                        ConnectionMode.IPC -> client.sendCommand(cmd)
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                        else -> {}
                    }
                    if (mode == ConnectionMode.IPC) {
                        client.tagSession(intent.id, intent.tags)
                        val updatedSessions = client.listSessions()
                        _uiState.value = _uiState.value.copy(sessions = updatedSessions)
                    }
                }
            }
            is ChatIntent.ToggleFilter -> {
                val filters = _uiState.value.activeFilters.toMutableList()
                if (filters.contains(intent.tag)) filters.remove(intent.tag) else filters.add(intent.tag)
                _uiState.value = _uiState.value.copy(activeFilters = filters)
            }
            is ChatIntent.SummarizeContext -> {
                _uiState.value = _uiState.value.copy(isSummarizing = true)
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.summarizeContext(intent.sessionId)
                    }
                    _uiState.value = _uiState.value.copy(isSummarizing = false)
                }
            }
            is ChatIntent.ForkSession -> {
                scope.launch {
                    val cmd = IpcCommand.ForkSession(ForkSessionPayload(intent.sessionId, intent.messageIdx))
                    when (mode) {
                        ConnectionMode.IPC -> client.sendCommand(cmd)
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                        else -> {}
                    }
                }
            }
            is ChatIntent.SetSystemPrompt -> {
                _uiState.value = _uiState.value.copy(currentSystemPrompt = intent.prompt)
                scope.launch {
                    val sid = _uiState.value.currentSessionId
                    if (mode == ConnectionMode.IPC && sid != null) {
                        client.sendCommand(IpcCommand.SetSystemPrompt(SetSystemPromptPayload(sid, intent.prompt)))
                    }
                }
            }
            is ChatIntent.UpdateConfig -> {
                scope.launch {
                    val cmd = IpcCommand.UpdateConfig(UpdateConfigPayload(intent.key, intent.value))
                    if (mode == ConnectionMode.IPC) {
                        client.updateConfig(intent.key, intent.value)
                    } else if (mode == ConnectionMode.STDIO) {
                        stdioExecutor.sendCommand(cmd)
                    }
                }
            }
            is ChatIntent.ScheduleTask -> {
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.scheduleTask(intent.text, intent.at, intent.cron, _uiState.value.currentSessionId)
                    }
                }
            }
            is ChatIntent.UpdateUiSettings -> {
                val oldAutoAccept = _uiState.value.uiSettings.autoAccept
                _uiState.value = _uiState.value.copy(uiSettings = intent.settings)
                settingsManager.save(intent.settings, UiSettings.serializer())
                
                if (oldAutoAccept != intent.settings.autoAccept) {
                    onIntent(ChatIntent.UpdateConfig("approval_mode", kotlinx.serialization.json.JsonPrimitive(!intent.settings.autoAccept)), scope, mode)
                }
            }
            ChatIntent.ToggleIpcLog -> {
                _uiState.value = _uiState.value.copy(ipcLogExpanded = !_uiState.value.ipcLogExpanded)
            }
            is ChatIntent.SetWorkingDir -> {
                val newSettings = _uiState.value.uiSettings.copy(workingDir = intent.path)
                _uiState.value = _uiState.value.copy(workingDir = intent.path, uiSettings = newSettings)
                settingsManager.save(newSettings, UiSettings.serializer())
            }
            ChatIntent.NewSession -> {
                // Clear current session — next send_message will create a new one on backend
                _uiState.value = _uiState.value.copy(
                    currentSessionId = null,
                    messages = emptyList()
                )
                addIpcLog("→", "new_session", "cleared local session")
            }
            ChatIntent.DumpDebugLog -> {
                scope.launch(Dispatchers.IO) {
                    dumpDebugLog()
                }
            }
            ChatIntent.ToggleProviderDialog -> {
                _uiState.value = _uiState.value.copy(showProviderDialog = !_uiState.value.showProviderDialog)
            }
            is ChatIntent.ActivateProvider -> {
                addIpcLog("→", "set_backend", "${intent.backend} / ${intent.model}")
                scope.launch {
                    val cmd = IpcCommand.SetBackend(SetBackendPayload(intent.backend, intent.model.ifEmpty { null }))
                    when (mode) {
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                        ConnectionMode.IPC -> client.sendCommand(cmd)
                        else -> {}
                    }
                    _uiState.value = _uiState.value.copy(
                        currentBackend = intent.backend,
                        currentModelName = intent.model
                    )
                }
            }
            is ChatIntent.ActivateProviderAndRestart -> {
                val newSettings = _uiState.value.uiSettings.copy(providerConfigs = intent.updatedConfigs)
                _uiState.value = _uiState.value.copy(uiSettings = newSettings)
                settingsManager.save(newSettings, UiSettings.serializer())
                addIpcLog("→", "restart_backend", "${intent.backend} → ${intent.envVars.entries.joinToString(", ") { "${it.key}=${it.value}" }.ifEmpty { "(no env vars)" }}")
                scope.launch(Dispatchers.IO) {
                    if (mode == ConnectionMode.STDIO) {
                        stdioExecutor.restart(intent.envVars)
                    }
                }
                // Also send set_backend after restart (brief delay for process startup)
                scope.launch {
                    delay(1500)
                    val model = intent.updatedConfigs[intent.backend]?.model ?: ""
                    val cmd = IpcCommand.SetBackend(SetBackendPayload(intent.backend, model.ifEmpty { null }))
                    when (mode) {
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        else -> {}
                    }
                    _uiState.value = _uiState.value.copy(
                        currentBackend = intent.backend,
                        currentModelName = model
                    )
                }
            }
            is ChatIntent.SaveProviderConfigs -> {
                val newSettings = _uiState.value.uiSettings.copy(providerConfigs = intent.configs)
                _uiState.value = _uiState.value.copy(uiSettings = newSettings)
                settingsManager.save(newSettings, UiSettings.serializer())
            }
            is ChatIntent.SaveNamedProviderConfig -> {
                val settings = _uiState.value.uiSettings
                val saved = settings.savedProviderConfigs.toMutableMap()
                val list = saved[intent.backend]?.toMutableList() ?: mutableListOf()
                list.removeAll { it.name == intent.name }
                list.add(SavedProviderConfig(intent.name, intent.config))
                saved[intent.backend] = list
                val newSettings = settings.copy(savedProviderConfigs = saved)
                _uiState.value = _uiState.value.copy(uiSettings = newSettings)
                settingsManager.save(newSettings, UiSettings.serializer())
            }
            is ChatIntent.DeleteNamedProviderConfig -> {
                val settings = _uiState.value.uiSettings
                val saved = settings.savedProviderConfigs.toMutableMap()
                val list = saved[intent.backend]?.toMutableList() ?: return@onIntent
                list.removeAll { it.name == intent.name }
                saved[intent.backend] = list
                val newSettings = settings.copy(savedProviderConfigs = saved)
                _uiState.value = _uiState.value.copy(uiSettings = newSettings)
                settingsManager.save(newSettings, UiSettings.serializer())
            }
            is ChatIntent.LoadNamedProviderConfig -> {
                val settings = _uiState.value.uiSettings
                val list = settings.savedProviderConfigs[intent.backend] ?: return@onIntent
                val named = list.find { it.name == intent.name } ?: return@onIntent
                val updatedProviderConfigs = settings.providerConfigs.toMutableMap()
                updatedProviderConfigs[intent.backend] = named.config
                val newSettings = settings.copy(providerConfigs = updatedProviderConfigs)
                _uiState.value = _uiState.value.copy(uiSettings = newSettings)
                settingsManager.save(newSettings, UiSettings.serializer())
                // Re-fetch models if URL changed? Optional, but good for UX
                if (intent.backend in listOf("lmstudio", "ollama", "huggingface")) {
                    onIntent(ChatIntent.FetchModels(intent.backend, named.config.baseUrl), scope, mode)
                }
            }
            is ChatIntent.LmsLoadModel -> {
                scope.launch {
                    try {
                        addIpcLog("→", "lms_load_model", intent.model)
                        val response = httpClient.post("${intent.url}/api/v1/models/load") {
                            contentType(ContentType.Application.Json)
                            setBody(buildJsonObject {
                                put("model", intent.model)
                                intent.config.contextLength?.let { put("context_length", it) }
                                intent.config.evalBatchSize?.let { put("eval_batch_size", it) }
                                intent.config.flashAttention?.let { put("flash_attention", it) }
                                intent.config.numExperts?.let { put("num_experts", it) }
                                intent.config.offloadKvCacheToGpu?.let { put("offload_kv_cache_to_gpu", it) }
                            })
                        }
                        if (response.status.isSuccess()) {
                            addIpcLog("←", "lms_load_ok", "Model loaded")
                            // Update current model name in state
                            _uiState.value = _uiState.value.copy(currentModelName = intent.model)
                        } else {
                            addIpcLog("←", "lms_load_fail", "HTTP ${response.status.value}")
                        }
                    } catch (e: Exception) {
                        addIpcLog("←", "lms_load_err", e.message ?: "unknown error")
                    }
                }
            }
            ChatIntent.RestartAgent -> {
                addIpcLog("→", "manual_restart", "User requested restart")
                scope.launch(Dispatchers.IO) {
                    if (mode == ConnectionMode.STDIO) {
                        stdioExecutor.restart()
                    }
                }
            }
            is ChatIntent.RestartProvider -> {
                addIpcLog("→", "restart_provider", intent.provider)
                scope.launch {
                    val model = _uiState.value.uiSettings.providerConfigs[intent.provider]?.model
                    val cmd = IpcCommand.RestartProvider(RestartProviderPayload(intent.provider, model))
                    when (mode) {
                        ConnectionMode.IPC -> client.sendCommand(cmd)
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                        else -> {}
                    }
                }
            }
            is ChatIntent.CreateTool -> {
                addIpcLog("→", "create_tool", intent.name)
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.createTool(intent.name, intent.template)
                        val tools = client.listTools()
                        _uiState.value = _uiState.value.copy(availableTools = tools)
                    }
                }
            }
            is ChatIntent.DeleteTool -> {
                addIpcLog("→", "delete_tool", intent.name)
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.deleteTool(intent.name)
                        val tools = client.listTools()
                        _uiState.value = _uiState.value.copy(availableTools = tools)
                    }
                }
            }
            is ChatIntent.UpdateSearchQuery -> {
                _uiState.value = _uiState.value.copy(messageSearchQuery = intent.query)
            }
            ChatIntent.RetryMessage -> {
                val lastUserMsg = _uiState.value.messages.lastOrNull { it.isFromUser }
                if (lastUserMsg != null) {
                    onIntent(ChatIntent.SendMessage(lastUserMsg.text, lastUserMsg.attachments ?: emptyList()), scope, mode)
                }
            }
            ChatIntent.ToggleSidebar -> {
                val newSettings = _uiState.value.uiSettings.copy(sidebarVisible = !_uiState.value.uiSettings.sidebarVisible)
                onIntent(ChatIntent.UpdateUiSettings(newSettings), scope, mode)
            }
            ChatIntent.NavigateHistoryUp -> {
                val history = _uiState.value.messageHistory
                if (history.isEmpty()) return
                
                val currentIndex = _uiState.value.historyIndex
                val newIndex = if (currentIndex == null) {
                    // Start navigating
                    _uiState.value = _uiState.value.copy(draftMessage = _uiState.value.inputText)
                    history.size - 1
                } else {
                    (currentIndex - 1).coerceAtLeast(0)
                }
                
                _uiState.value = _uiState.value.copy(
                    historyIndex = newIndex,
                    inputText = history[newIndex]
                )
            }
            ChatIntent.NavigateHistoryDown -> {
                val history = _uiState.value.messageHistory
                val currentIndex = _uiState.value.historyIndex ?: return
                
                if (currentIndex >= history.size - 1) {
                    // Back to draft
                    _uiState.value = _uiState.value.copy(
                        historyIndex = null,
                        inputText = _uiState.value.draftMessage
                    )
                } else {
                    val newIndex = currentIndex + 1
                    _uiState.value = _uiState.value.copy(
                        historyIndex = newIndex,
                        inputText = history[newIndex]
                    )
                }
            }
            is ChatIntent.UpdateInputText -> {
                _uiState.value = _uiState.value.copy(inputText = intent.text)
                // If user edits while in history, treat as new draft?
                // For now just allow editing the history line without dropping out of history mode
            }
            ChatIntent.ExportSession -> {
                exportCurrentSession()
            }
            is ChatIntent.PasteToInput -> {
                _uiState.value = _uiState.value.copy(inputText = intent.text)
            }
            is ChatIntent.MoveSessionToFolder -> {
                val folders = _uiState.value.sessionFolders.toMutableMap()
                if (intent.folderName == null) {
                    folders.remove(intent.sessionId)
                } else {
                    folders[intent.sessionId] = intent.folderName
                }
                _uiState.value = _uiState.value.copy(sessionFolders = folders)
                saveSessionCache()
            }
            is ChatIntent.ResolvePlan -> {
                scope.launch {
                    val cmd = IpcCommand.ApprovePlan(ApprovePlanPayload(intent.planId, intent.approved))
                    when (mode) {
                        ConnectionMode.IPC -> client.sendCommand(cmd)
                        ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                        else -> {}
                    }
                    _uiState.value = _uiState.value.copy(pendingPlan = null)
                }
            }
            ChatIntent.ToggleToolOutput -> {
                _uiState.value = _uiState.value.copy(showToolOutput = !_uiState.value.showToolOutput)
            }
            ChatIntent.ClearToolOutput -> {
                _uiState.value = _uiState.value.copy(toolOutput = emptyList())
            }
            ChatIntent.ToggleTokenAnalytics -> {
                _uiState.value = _uiState.value.copy(showTokenAnalytics = !_uiState.value.showTokenAnalytics)
            }
            ChatIntent.ToggleSearch -> {
                _uiState.value = _uiState.value.copy(showSearch = !_uiState.value.showSearch)
            }
        }
    }

    private fun dumpDebugLog() {
        val state = _uiState.value
        val baseDir = state.workingDir.ifEmpty { System.getProperty("user.home") ?: "." }
        val debugDir = java.io.File(baseDir, "DebugLog")

        try {
            if (debugDir.exists()) debugDir.deleteRecursively()
            debugDir.mkdirs()

            val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())

            // Pre-compute shared values used in multiple files
            val outgoingCount = state.ipcLogs.count { "  →  " in it }
            val incomingCount = state.ipcLogs.count { "  ←  " in it }
            val errorLines = state.ipcLogs.filter { line ->
                line.contains("error", ignoreCase = true) ||
                line.contains("STDIO_ERROR") || line.contains("STDIO_EXITED") ||
                line.contains("DISCONNECTED") || line.contains("CONNECTION_FAILED") ||
                line.contains("BACKEND_ERROR")
            }
            val stderrLines = stdioExecutor.stderrSnapshot()
            val activeBackend = state.currentBackend
            val providerCfg = state.uiSettings.providerConfigs[activeBackend]
            val binaryPath = com.agentcore.shared.CoreLauncher.findBinary() ?: "(not found)"
            val binaryFile = java.io.File(binaryPath)
            val binaryTimestamp = if (binaryFile.exists())
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date(binaryFile.lastModified()))
            else "(file not found)"
            val envVars = com.agentcore.ui.components.buildAllEnvVars(state.uiSettings.providerConfigs)
            val displayUrl = providerCfg?.baseUrl?.ifEmpty { "(default)" } ?: "(none)"

            // ── 1. ipc_traffic.txt ─────────────────────────────────────────────
            java.io.File(debugDir, "ipc_traffic.txt").writeText(buildString {
                appendLine("# IPC Traffic Log — $ts")
                appendLine("# Format: HH:mm:ss.SSS  direction  event  summary")
                appendLine("# Outgoing (→): $outgoingCount   Incoming (←): $incomingCount")
                appendLine()
                if (state.ipcLogs.isEmpty()) {
                    appendLine("(no IPC traffic recorded)")
                } else {
                    state.ipcLogs.forEach { appendLine(it) }
                }
            })

            // ── 2. connection_errors.txt ───────────────────────────────────────
            java.io.File(debugDir, "connection_errors.txt").writeText(buildString {
                appendLine("# Connection & Error Lines — $ts")
                appendLine()
                if (errorLines.isEmpty()) appendLine("(no errors recorded)")
                else errorLines.forEach { appendLine(it) }
            })

            // ── 3. system_info.txt ─────────────────────────────────────────────
            java.io.File(debugDir, "system_info.txt").writeText(buildString {
                appendLine("# System & Configuration — $ts")
                appendLine()
                appendLine("── Connection ──────────────────────────────────")
                appendLine("Mode              : $currentMode")
                appendLine("Active backend    : $activeBackend")
                appendLine("Configured model  : ${providerCfg?.model?.ifEmpty { "(default)" } ?: "(none)"}")
                val displayUrl = providerCfg?.baseUrl?.ifEmpty { "(default)" } ?: "(none)"
                appendLine("Provider URL      : $displayUrl")
                appendLine()
                appendLine("── Binary ──────────────────────────────────────")
                appendLine("agent-core path   : $binaryPath")
                appendLine("Binary timestamp  : $binaryTimestamp")
                appendLine("Binary size       : ${if (binaryFile.exists()) "${binaryFile.length() / 1024}KB" else "N/A"}")
                appendLine()
                appendLine("── Environment variables passed to agent-core ──")
                if (envVars.isEmpty()) {
                    appendLine("(none — using backend defaults)")
                } else {
                    envVars.forEach { (key, value) ->
                        // Mask API keys but show URLs in full
                        val display = if (key.endsWith("_KEY") || key.endsWith("_TOKEN"))
                            value.take(8) + "***" else value
                        appendLine("$key = $display")
                    }
                }
                appendLine()
                appendLine("── All provider configs ────────────────────────")
                state.uiSettings.providerConfigs.forEach { (id, cfg) ->
                    appendLine("[$id]")
                    if (cfg.model.isNotEmpty()) appendLine("  model   = ${cfg.model}")
                    if (cfg.baseUrl.isNotEmpty()) appendLine("  baseUrl = ${cfg.baseUrl}")
                    if (cfg.apiKey.isNotEmpty()) appendLine("  apiKey  = ${cfg.apiKey.take(8)}***")
                }
                if (state.uiSettings.providerConfigs.isEmpty()) appendLine("(no provider configs saved)")
                appendLine()
                appendLine("── JVM / OS ─────────────────────────────────────")
                appendLine("Java version      : ${System.getProperty("java.version")}")
                appendLine("JVM vendor        : ${System.getProperty("java.vendor")}")
                appendLine("OS                : ${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}")
                appendLine("User home         : ${System.getProperty("user.home")}")
            })

            // ── 4. connectivity_test.txt ───────────────────────────────────────
            java.io.File(debugDir, "connectivity_test.txt").writeText(buildString {
                appendLine("# Connectivity Test — $ts")
                appendLine()
                // Test URLs for local providers
                val urlsToTest = mutableListOf<Pair<String, String>>() // label → url
                // From provider configs
                val lmUrl = state.uiSettings.providerConfigs["lmstudio"]?.baseUrl?.ifEmpty { "http://localhost:1234" } ?: "http://localhost:1234"
                val ollamaUrl = state.uiSettings.providerConfigs["ollama"]?.baseUrl?.ifEmpty { "http://localhost:11434" } ?: "http://localhost:11434"
                // Also from env vars (might differ)
                val lmUrlEnv = envVars["LMSTUDIO_BASE_URL"]
                val ollamaUrlEnv = envVars["OLLAMA_BASE_URL"]

                urlsToTest.add("LM Studio (config)" to "$lmUrl/v1/models")
                if (lmUrlEnv != null && lmUrlEnv != lmUrl) {
                    urlsToTest.add("LM Studio (env var)" to "$lmUrlEnv/v1/models")
                }
                urlsToTest.add("Ollama (config)" to "$ollamaUrl/api/tags")
                if (ollamaUrlEnv != null && ollamaUrlEnv != ollamaUrl) {
                    urlsToTest.add("Ollama (env var)" to "$ollamaUrlEnv/api/tags")
                }
                urlsToTest.add("agent-core HTTP" to "http://localhost:7700")

                urlsToTest.forEach { (label, url) ->
                    val result = testHttpUrl(url)
                    appendLine("$label")
                    appendLine("  URL    : $url")
                    appendLine("  Result : ${result.first}")
                    if (result.second.isNotEmpty()) appendLine("  Body   : ${result.second.take(300)}")
                    appendLine()
                }
            })

            // ── 5. app_events.txt ──────────────────────────────────────────────
            java.io.File(debugDir, "app_events.txt").writeText(buildString {
                appendLine("# App Events Log (backend LogPayload) — $ts")
                appendLine()
                if (state.logs.isEmpty()) {
                    appendLine("(no app events recorded)")
                } else {
                    state.logs.forEach { log ->
                        appendLine("[${log.timestamp}] [${log.level}] ${log.source?.let { "($it) " } ?: ""}${log.message}")
                    }
                }
            })

            // ── 6. agent_stderr.txt ────────────────────────────────────────────
            java.io.File(debugDir, "agent_stderr.txt").writeText(buildString {
                appendLine("# Agent-Core Stderr (STDIO mode only) — $ts")
                appendLine()
                if (stderrLines.isEmpty()) {
                    appendLine("(no stderr — either IPC/HTTP mode, or backend running without warnings)")
                } else {
                    stderrLines.forEach { appendLine(it) }
                }
            })

            // ── 7. summary.txt ─────────────────────────────────────────────────
            java.io.File(debugDir, "summary.txt").writeText(buildString {
                appendLine("# Debug Log Summary — $ts")
                appendLine()
                appendLine("── Session ──────────────────────────────────────")
                appendLine("Session ID        : ${state.currentSessionId ?: "(none — no message sent yet)"}")
                appendLine("Status            : ${state.statusState}")
                appendLine("Messages in UI    : ${state.messages.size}")
                appendLine("Working Dir       : ${state.workingDir.ifEmpty { "(not set)" }}")
                appendLine()
                appendLine("── Transport ────────────────────────────────────")
                appendLine("Connection mode   : $currentMode")
                appendLine("Active backend    : ${state.currentBackend}")
                appendLine("Configured model  : ${providerCfg?.model?.ifEmpty { "(default)" } ?: "(none)"}")
                appendLine("Provider URL      : $displayUrl")
                appendLine()
                appendLine("── IPC Traffic ──────────────────────────────────")
                appendLine("Total log lines   : ${state.ipcLogs.size}")
                appendLine("Outgoing (→)      : $outgoingCount")
                appendLine("Incoming (←)      : $incomingCount")
                if (incomingEventCounts.isEmpty()) {
                    appendLine("Incoming by type  : (none received)")
                } else {
                    incomingEventCounts.entries.sortedByDescending { it.value }.forEach { (name, count) ->
                        appendLine("  ${"←  $name".padEnd(30)} $count")
                    }
                }
                appendLine("Error lines       : ${errorLines.size}")
                appendLine()
                appendLine("── Agent Process ────────────────────────────────")
                appendLine("Stderr lines      : ${stderrLines.size}")
                appendLine("App events        : ${state.logs.size}")
                appendLine("Binary            : $binaryPath")
                appendLine("Binary built      : $binaryTimestamp")
                appendLine()
                appendLine("Files written to  : ${debugDir.absolutePath}")
            })

            addIpcLog("→", "dump_debug_log", "OK → ${debugDir.absolutePath} (7 files)")
        } catch (e: Exception) {
            addIpcLog("→", "dump_debug_log", "FAIL: ${e.message?.take(60)}")
        }
    }

    /** Quick HTTP GET with 3s timeout — returns (status description, body snippet). */
    private fun testHttpUrl(url: String): Pair<String, String> {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            conn.connect()
            val code = conn.responseCode
            val body = try {
                conn.inputStream.bufferedReader().readText().take(500)
            } catch (_: Exception) { "" }
            conn.disconnect()
            "HTTP $code OK" to body
        } catch (e: java.net.ConnectException) {
            "UNREACHABLE (connection refused)" to ""
        } catch (e: java.net.SocketTimeoutException) {
            "TIMEOUT (>3s)" to ""
        } catch (e: Exception) {
            "ERROR: ${e.message?.take(80)}" to ""
        }
    }

    private fun saveSessionCache() {
        val sid = _uiState.value.currentSessionId ?: return
        // Update in-memory state immediately (on Main — fast, no IO)
        val snapshot = cachedData.copy(
            sessions = _uiState.value.sessions,
            sessionMessages = cachedData.sessionMessages.toMutableMap().also {
                it[sid] = _uiState.value.messages
            }
        )
        cachedData = snapshot
        // Debounced file write on IO thread — avoids blocking UI on every text_delta
        saveJob?.cancel()
        saveJob = viewModelScope?.launch(Dispatchers.IO) {
            delay(500)
            try {
                sessionCache.save(snapshot, SessionCache.serializer())
            } catch (_: Exception) {}
        }
    }
    private fun handleSlashCommand(text: String, scope: CoroutineScope, mode: ConnectionMode): Boolean {
        if (!text.startsWith("/")) return false
        val cmd = text.lowercase().substringBefore(" ")
        when (cmd) {
            "/clear" -> {
                _uiState.value = _uiState.value.copy(messages = emptyList())
                saveSessionCache()
            }
            "/reset" -> {
                _uiState.value = _uiState.value.copy(messages = emptyList(), statusState = "IDLE")
                saveSessionCache()
            }
            "/help" -> {
                val helpText = """
                    **Dostępne komendy:**
                    - `/clear` - Wyczyść widok czatu locally
                    - `/reset` - Wyczyść czat i zresetuj status
                    - `/stats` - Odśwież statystyki tokenów
                    - `/help` - Wyświetl tę pomoc
                """.trimIndent()
                val helpMsg = Message(
                    id = "help-${System.currentTimeMillis()}",
                    sender = "System",
                    text = helpText,
                    isFromUser = false,
                    type = MessageType.SYSTEM
                )
                _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + helpMsg)
            }
            "/stats" -> {
                onIntent(ChatIntent.RefreshStats, scope, mode)
            }
            "/bash" -> {
                val code = text.removePrefix("/bash").trim()
                if (code.isNotEmpty()) {
                    IpcHandler.performSendMessage(scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode, "Executing: $code", emptyList(), _uiState.value.currentSessionId ?: "", {}, {}, {}, {})
                    // The actual execution is handled by the backend when it sees this message if we have a tool for it.
                    // Or we can explicitly call a tool here if needed.
                }
            }
            "/export" -> {
                exportCurrentSession()
            }
            else -> return false
        }
        return true
    }

    private fun exportCurrentSession() {
        val state = _uiState.value
        val sid = state.currentSessionId ?: return
        val messages = state.messages
        if (messages.isEmpty()) return

        val baseDir = state.workingDir.ifEmpty { System.getProperty("user.home") ?: "." }
        val exportsDir = java.io.File(baseDir, "Exports")
        exportsDir.mkdirs()

        val fileName = "session_${sid.take(8)}_${System.currentTimeMillis()}.md"
        val file = java.io.File(exportsDir, fileName)

        val content = buildString {
            appendLine("# Chat Session Export")
            appendLine("Session ID: $sid")
            appendLine("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
            appendLine()
            appendLine("---")
            appendLine()
            messages.forEach { msg ->
                appendLine("### ${msg.sender}${if (msg.isFromUser) " (User)" else ""}")
                appendLine(msg.text)
                appendLine()
                val atts = msg.attachments
                if (!atts.isNullOrEmpty()) {
                    appendLine("Attached Images/Files: ${atts.size}")
                    appendLine()
                }
            }
        }

        try {
            file.writeText(content)
            val successMsg = Message(
                id = "export-${System.currentTimeMillis()}",
                sender = "System",
                text = "Sesja została wyeksportowana do: `${file.absolutePath}`",
                isFromUser = false,
                type = MessageType.SYSTEM
            )
            _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + successMsg)
        } catch (e: Exception) {
            addIpcLog("←", "export_failed", e.message ?: "unknown error")
        }
    }
}
