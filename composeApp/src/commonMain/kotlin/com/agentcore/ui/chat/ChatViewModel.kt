package com.agentcore.ui.chat

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.agentcore.api.*
import com.agentcore.logic.IpcHandler
import com.agentcore.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
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
    private val _uiState = mutableStateOf(ChatUiState())
    val uiState: State<ChatUiState> = _uiState

    fun init(scope: CoroutineScope, mode: ConnectionMode) {
        if (isInitialized) return
        isInitialized = true
        this.viewModelScope = scope
        
        // Load persistent UI settings
        settingsManager.load(UiSettings.serializer())?.let { saved ->
            _uiState.value = _uiState.value.copy(uiSettings = saved)
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
                    stdioExecutor.events.collectLatest { eventHandler(it) }
                }
                ConnectionMode.UNIX_SOCKET -> {
                    unixSocketExecutor.start(scope)
                    unixSocketExecutor.events.collectLatest { eventHandler(it) }
                }
                ConnectionMode.IPC -> {
                    val initialSessions = client.listSessions()
                    if (initialSessions.isNotEmpty()) {
                        cachedData = cachedData.copy(sessions = initialSessions)
                        sessionCache.save(cachedData, SessionCache.serializer())
                    }
                    _uiState.value = _uiState.value.copy(sessions = if (initialSessions.isEmpty()) cachedData.sessions else initialSessions)
                    
                    scope.launch { 
                        val tools = client.listTools()
                        val backends = client.listBackends()
                        _uiState.value = _uiState.value.copy(availableTools = tools, availableBackends = backends)
                    }
                    client.observeEvents().collectLatest { eventHandler(it) }
                }
                else -> {}
            }
        }
    }

    private fun handleIpcEvent(event: IpcEvent) {
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
            onStatusChange = { _uiState.value = _uiState.value.copy(statusState = it) },
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
            onError = { },
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
            }
        )
    }

    fun onIntent(intent: ChatIntent, scope: CoroutineScope, mode: ConnectionMode) {
        when (intent) {
            is ChatIntent.SendMessage -> {
                val userMsg = Message(
                    id = "user-${System.currentTimeMillis()}",
                    sender = "You",
                    text = intent.text,
                    isFromUser = true
                )
                _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + userMsg)
                
                // We'll use a hacky list for and from IpcHandler for now or refactor performSendMessage too.
                // Let's refactor IpcHandler.performSendMessage to be more flexible if needed.
                // For now, let's keep it simple.
                val messagesRef = mutableStateListOf<Message>().apply { addAll(_uiState.value.messages) }
                IpcHandler.performSendMessage(
                    scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode,
                    intent.text, emptyList(), _uiState.value.currentSessionId, messagesRef,
                    { }, { }, { _uiState.value = _uiState.value.copy(statusState = it) }
                )
                // Note: IpcHandler might add messages to messagesRef (like errors)
                // We should sync back if performSendMessage is async.
                // Actually it is async, so we should really pass callbacks to it too.
            }
            is ChatIntent.SelectSession -> {
                val cachedMsgs = cachedData.sessionMessages[intent.id] ?: emptyList()
                _uiState.value = _uiState.value.copy(currentSessionId = intent.id, messages = cachedMsgs)
                
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.sendCommand(IpcCommand.GetSession(GetSessionPayload(intent.id)))
                        // The result comes back via handleIpcEvent -> onSessionData
                    }
                }
            }
            ChatIntent.ToggleSettings -> {
                _uiState.value = _uiState.value.copy(showSettings = !_uiState.value.showSettings)
            }
            ChatIntent.RefreshStats -> {
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        val stats = client.getStats()
                        _uiState.value = _uiState.value.copy(sessionStats = stats)
                    }
                }
            }
            is ChatIntent.ResolveApproval -> {
                scope.launch {
                    val pending = _uiState.value.pendingApproval
                    if (mode == ConnectionMode.IPC && pending != null) {
                        client.sendCommand(IpcCommand.ApprovalResponse(ApprovalResponsePayload(pending.id, intent.approved)))
                    }
                    _uiState.value = _uiState.value.copy(pendingApproval = null)
                }
            }
            is ChatIntent.RespondHumanInput -> {
                val userMsg = Message(
                    id = "user-${System.currentTimeMillis()}",
                    sender = "You",
                    text = intent.answer,
                    isFromUser = true
                )
                _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + userMsg, pendingHumanInput = null)
                
                val messagesRef = mutableStateListOf<Message>().apply { addAll(_uiState.value.messages) }
                IpcHandler.performSendMessage(
                    scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode,
                    intent.answer, emptyList(), _uiState.value.currentSessionId, messagesRef,
                    { }, { }, { _uiState.value = _uiState.value.copy(statusState = it) }
                )
            }
            ChatIntent.CancelAction -> {
                scope.launch {
                    val sid = _uiState.value.currentSessionId
                    if (mode == ConnectionMode.IPC && sid != null) {
                        client.sendCommand(IpcCommand.Cancel(CancelPayload(sid)))
                    }
                    // Relying on backend message_end to set IDLE
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
                    if (mode == ConnectionMode.IPC) {
                        client.updateConfig(intent.key, intent.value)
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
                _uiState.value = _uiState.value.copy(uiSettings = intent.settings)
                settingsManager.save(intent.settings, UiSettings.serializer())
            }
        }
    }

    private fun saveSessionCache() {
        val sid = _uiState.value.currentSessionId
        if (sid != null) {
            val updatedMessages = cachedData.sessionMessages.toMutableMap()
            updatedMessages[sid] = _uiState.value.messages
            
            cachedData = cachedData.copy(
                sessions = _uiState.value.sessions,
                sessionMessages = updatedMessages
            )
            sessionCache.save(cachedData, SessionCache.serializer())
        }
    }
}
