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
        is IpcEvent.ToolOutputDelta -> "tool_output_delta" // B02
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
        is IpcEvent.HumanInputRequest -> "\"${event.payload.prompt.take(30)}\""
        is IpcEvent.SessionData -> "sid=${event.payload.session_id?.take(8)}"
        is IpcEvent.SessionForked -> "new=${event.payload.new_session_id.take(8)}"
        is IpcEvent.ModelsList -> "${event.payload.models.size} models"
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
                workingDir = saved.workingDir.ifEmpty { System.getProperty("user.home") ?: "" }
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
            _uiState.value = _uiState.value.copy(sessions = cache.sessions)
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
                        _uiState.value = _uiState.value.copy(availableTools = tools, availableBackends = backends)
                    }
                    client.observeEvents().collect { eventHandler(it) }
                }
                else -> {}
            }
        }
    }

    private fun handleIpcEvent(event: IpcEvent) {
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
                // If this status event came from handle_update_config
                if (event is IpcEvent.Status) {
                    val payload = event.payload
                    if (payload.updated_key == "approval_mode") {
                        val value = payload.value?.toString()?.toBoolean() ?: true
                        _uiState.value = _uiState.value.copy(approvalMode = value)
                    }
                }
            },
            onStatsUpdate = { _uiState.value = _uiState.value.copy(sessionStats = it) },
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
                }
            },
            onSessionData = { payload ->
                // Only clear messages if we're not already loading them or if the session changed
                if (_uiState.value.currentSessionId != payload.session_id) {
                    _uiState.value = _uiState.value.copy(messages = emptyList(), currentSessionId = payload.session_id)
                }
                _uiState.value = _uiState.value.copy(currentSystemPrompt = payload.system_prompt ?: "")
                saveSessionCache()
            },
            onHumanInputRequest = { _uiState.value = _uiState.value.copy(pendingHumanInput = it) },
            onAgentGroupUpdate = { _uiState.value = _uiState.value.copy(agentGroup = it) },
            onSessionForked = { payload ->
                _uiState.value = _uiState.value.copy(currentSessionId = payload.new_session_id, messages = emptyList())
                val currentScope = viewModelScope
                if (currentScope != null) {
                    currentScope.launch {
                        val data = client.getSession(payload.new_session_id)
                        val sessions = client.listSessions()
                        _uiState.value = _uiState.value.copy(sessions = sessions)
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
            }
        )
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
                addIpcLog("→", "send_message", "\"${intent.text.take(40)}${if (intent.text.length > 40) "…" else ""}\"")
                IpcHandler.performSendMessage(
                    scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode,
                    intent.text, emptyList(), _uiState.value.currentSessionId,
                    onMessageAdded = { msg ->
                        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
                        saveSessionCache()
                    },
                    onClearInput = { },
                    onClearAttachments = { },
                    onStatusChange = { _uiState.value = _uiState.value.copy(statusState = it) },
                    workingDir = _uiState.value.workingDir.takeIf { it.isNotBlank() } // B06
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
                _uiState.value = _uiState.value.copy(pendingHumanInput = null)
                
                IpcHandler.performSendMessage(
                    scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode,
                    intent.answer, emptyList(), _uiState.value.currentSessionId,
                    onMessageAdded = { msg ->
                        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
                        saveSessionCache()
                    },
                    onClearInput = { },
                    onClearAttachments = { },
                    onStatusChange = { _uiState.value = _uiState.value.copy(statusState = it) },
                    workingDir = _uiState.value.workingDir.takeIf { it.isNotBlank() } // B06
                )
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
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.deleteSession(intent.id)
                        val updatedSessions = client.listSessions()
                        _uiState.value = _uiState.value.copy(
                            sessions = updatedSessions,
                            currentSessionId = if (_uiState.value.currentSessionId == intent.id) null else _uiState.value.currentSessionId,
                            messages = if (_uiState.value.currentSessionId == intent.id) emptyList() else _uiState.value.messages
                        )
                    }
                }
            }
            ChatIntent.ReloadTools -> {
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.sendCommand(IpcCommand.ReloadTools())
                        val tools = client.listTools()
                        _uiState.value = _uiState.value.copy(availableTools = tools)
                    }
                }
            }
            is ChatIntent.PruneSession -> {
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.pruneSession(intent.id)
                        if (_uiState.value.currentSessionId == intent.id) {
                            val sessions = client.listSessions()
                            _uiState.value = _uiState.value.copy(sessions = sessions)
                            val data = client.getSession(intent.id)
                        }
                    }
                }
            }
            is ChatIntent.TagSession -> {
                scope.launch {
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
                    if (mode == ConnectionMode.IPC) {
                        client.sendCommand(IpcCommand.ForkSession(ForkSessionPayload(intent.sessionId, intent.messageIdx)))
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
                    _uiState.value = _uiState.value.copy(currentBackend = intent.backend)
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
                    _uiState.value = _uiState.value.copy(currentBackend = intent.backend)
                }
            }
            is ChatIntent.SaveProviderConfigs -> {
                val newSettings = _uiState.value.uiSettings.copy(providerConfigs = intent.configs)
                _uiState.value = _uiState.value.copy(uiSettings = newSettings)
                settingsManager.save(newSettings, UiSettings.serializer())
            }
            ChatIntent.RestartAgent -> {
                addIpcLog("→", "manual_restart", "User requested restart")
                scope.launch(Dispatchers.IO) {
                    if (mode == ConnectionMode.STDIO) {
                        stdioExecutor.restart()
                    }
                }
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
}
