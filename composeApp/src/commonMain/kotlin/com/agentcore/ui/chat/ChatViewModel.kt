// Facade ViewModel: initialises sub-ViewModels, collects IPC events, and delegates intents.
// Sub-VMs: MessageViewModel, SessionViewModel, ProviderViewModel, SettingsViewModel.
// IPC event routing: ChatIpcEventHandlers.
package com.agentcore.ui.chat

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.agentcore.api.*
import com.agentcore.logic.AutoAcceptUseCase
import com.agentcore.logic.IpcHandler
import com.agentcore.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class ChatViewModel(
    private val client: AgentClient,
    private val cliExecutor: CliExecutor,
    private val stdioExecutor: StdioExecutor,
    private val unixSocketExecutor: UnixSocketExecutor,
    private val settingsManager: SettingsManager,
    private val sessionCacheMgr: SettingsManager
) {
    private val _uiState = mutableStateOf(ChatUiState())
    val uiState: State<ChatUiState> = _uiState

    private var isInitialized = false
    private var viewModelScope: CoroutineScope? = null
    private var currentMode: ConnectionMode = ConnectionMode.STDIO
    private var lastRestartTime: Long = 0L
    private val incomingEventCounts = mutableMapOf<String, Int>()
    private val logDateFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private val autoAcceptUseCase by lazy { AutoAcceptUseCase(client, stdioExecutor, unixSocketExecutor) }
    private var eventCollectionJob: Job? = null

    // ── Sub-ViewModels ─────────────────────────────────────────────────────────
    internal val sessionVM = SessionViewModel(_uiState, client, stdioExecutor, unixSocketExecutor, sessionCacheMgr, ::addIpcLog)

    private val messageVM = MessageViewModel(_uiState, client, stdioExecutor, unixSocketExecutor, cliExecutor, ::addIpcLog, sessionVM::saveSessionCache)

    private val providerVM = ProviderViewModel(_uiState, client, stdioExecutor, unixSocketExecutor, settingsManager, ::addIpcLog)

    private val toolsHealthVM = ToolsHealthViewModel(_uiState, client, stdioExecutor, unixSocketExecutor, ::addIpcLog)

    private val settingsVM = SettingsViewModel(
        uiState             = _uiState,
        settingsManager     = settingsManager,
        stdioExecutor       = stdioExecutor,
        log                 = ::addIpcLog,
        getMode             = { currentMode },
        incomingEventCounts = incomingEventCounts,
        autoAcceptUseCase   = autoAcceptUseCase,
        client              = client,
        unixSocketExecutor  = unixSocketExecutor,
    )

    private val ipcEventHandlers = ChatIpcEventHandlers(
        uiState            = _uiState,
        sessionVM          = sessionVM,
        lastRestartTime    = { lastRestartTime },
        setLastRestartTime = { lastRestartTime = it },
        stdioExecutor      = stdioExecutor,
        log                = ::addIpcLog,
        syncSessions       = ::syncSessions,
        syncApprovalMode   = ::syncApprovalMode,
        getScope           = { viewModelScope },
    )

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    fun init(scope: CoroutineScope, mode: ConnectionMode) {
        if (isInitialized) return
        isInitialized = true
        viewModelScope = scope
        currentMode = mode
        sessionVM.scope = scope
        sessionVM.mode = mode

        settingsManager.load(UiSettings.serializer())?.let { saved ->
            _uiState.value = _uiState.value.copy(
                uiSettings      = saved,
                workingDir      = saved.workingDir.ifEmpty { System.getProperty("user.home") ?: "" },
                currentModelName = saved.providerConfigs[_uiState.value.currentBackend]?.model ?: ""
            )
            autoAcceptUseCase.sync(scope, mode, saved.autoAccept, saved.bypassAllPermissions)
            if (mode == ConnectionMode.STDIO) {
                val envVars = com.agentcore.ui.components.buildAllEnvVars(saved.providerConfigs)
                if (envVars.isNotEmpty()) stdioExecutor.setEnvVars(envVars)
            }
        }
        sessionVM.loadCache()

        val handler: (IpcEvent) -> Unit = { ipcEventHandlers.handle(it) }
        eventCollectionJob = scope.launch {
            when (mode) {
                ConnectionMode.STDIO -> {
                    stdioExecutor.start()
                    stdioExecutor.events.collect { handler(it) }
                }
                ConnectionMode.UNIX_SOCKET -> {
                    unixSocketExecutor.start(scope)
                    unixSocketExecutor.events.collect { handler(it) }
                }
                ConnectionMode.IPC -> {
                    val initialSessions = client.listSessions()
                    if (initialSessions.isNotEmpty()) {
                        sessionVM.cachedData = sessionVM.cachedData.copy(sessions = initialSessions)
                        scope.launch(Dispatchers.IO) { sessionCacheMgr.save(sessionVM.cachedData, SessionCache.serializer()) }
                    }
                    _uiState.value = _uiState.value.copy(sessions = if (initialSessions.isEmpty()) sessionVM.cachedData.sessions else initialSessions)
                    scope.launch {
                        _uiState.value = _uiState.value.copy(availableTools = client.listTools(), availableBackends = client.listBackends())
                        onIntent(ChatIntent.ReloadSkills, scope, mode)
                    }
                    client.observeEvents().collect { handler(it) }
                }
                else -> {}
            }
        }
    }

    fun clear() {
        eventCollectionJob?.cancel()
        eventCollectionJob = null
    }

    // ── Intent routing ─────────────────────────────────────────────────────────
    fun onIntent(intent: ChatIntent, scope: CoroutineScope, mode: ConnectionMode) {
        if (intent is ChatIntent.ExecuteSlashCommand) {
            handleSlashCommand(intent.command, scope, mode)
            return
        }
        messageVM.handle(intent, scope, mode)
        sessionVM.handle(intent, scope, mode)
        providerVM.handle(intent, scope, mode)
        toolsHealthVM.handle(intent, scope, mode)
        settingsVM.handle(intent, scope, mode)
    }

    // I36: Translate SlashCommand → existing ChatIntents.
    private fun handleSlashCommand(
        cmd: com.agentcore.ui.components.SlashCommand,
        scope: CoroutineScope,
        mode: ConnectionMode
    ) {
        when (cmd) {
            is com.agentcore.ui.components.SlashCommand.Role ->
                onIntent(ChatIntent.UpdateSettings(_uiState.value.currentBackend, cmd.name), scope, mode)
            is com.agentcore.ui.components.SlashCommand.Backend ->
                onIntent(ChatIntent.ActivateProvider(cmd.name, cmd.model.ifEmpty {
                    _uiState.value.uiSettings.providerConfigs[cmd.name]?.model ?: ""
                }), scope, mode)
            is com.agentcore.ui.components.SlashCommand.SystemPrompt ->
                onIntent(ChatIntent.SetSystemPrompt(cmd.prompt), scope, mode)
            is com.agentcore.ui.components.SlashCommand.Bash ->
                onIntent(ChatIntent.SendMessage("/bash ${cmd.cmd}"), scope, mode)
            is com.agentcore.ui.components.SlashCommand.NewSession ->
                onIntent(ChatIntent.NewSession, scope, mode)
            is com.agentcore.ui.components.SlashCommand.Help -> {
                val helpText = com.agentcore.ui.components.SlashCommandParser.helpText()
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + com.agentcore.model.Message(
                        id = "help-${System.currentTimeMillis()}",
                        sender = "System",
                        text = "📋 Slash commands:\n$helpText",
                        isFromUser = false,
                        type = com.agentcore.model.MessageType.SYSTEM
                    )
                )
            }
        }
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────
    private fun syncApprovalMode() {
        val state = _uiState.value
        addIpcLog("→", "sync_approval_mode", "autoAccept=${state.uiSettings.autoAccept} bypassAll=${state.uiSettings.bypassAllPermissions}")
        viewModelScope?.let { autoAcceptUseCase.sync(it, currentMode, state.uiSettings.autoAccept, state.uiSettings.bypassAllPermissions) }
    }

    private fun syncSessions() = sessionVM.syncSessions()

    private fun addIpcLog(direction: String, name: String, summary: String = "") {
        val ts = java.time.LocalTime.now().format(logDateFmt)
        val entry = "$ts  $direction  ${name.padEnd(22)}$summary".trimEnd()
        _uiState.value = _uiState.value.copy(
            ipcLogs = _uiState.value.ipcLogs.let { list ->
                val next = list + entry
                if (next.size > 1000) next.drop(next.size - 1000) else next
            }
        )
        if (direction == "←") incomingEventCounts[name] = (incomingEventCounts[name] ?: 0) + 1
    }

    // ipcEventName / ipcEventSummary kept here for use by ChatIpcEventHandlers via log callbacks
    internal fun ipcEventName(event: IpcEvent) = when (event) {
        is IpcEvent.TextDelta        -> "text_delta"
        is IpcEvent.Status           -> "status"
        is IpcEvent.MessageStart     -> "message_start"
        is IpcEvent.MessageEnd       -> "message_end"
        is IpcEvent.ToolCall         -> "tool_call"
        is IpcEvent.ToolResult       -> "tool_result"
        is IpcEvent.Error            -> "error"
        is IpcEvent.Log              -> "log"
        is IpcEvent.Stats            -> "stats"
        is IpcEvent.ApprovalRequest  -> "approval_request"
        is IpcEvent.HumanInputRequest -> "human_input_request"
        is IpcEvent.SessionData      -> "session_data"
        is IpcEvent.SessionForked    -> "session_forked"
        is IpcEvent.Ready            -> "ready"
        is IpcEvent.Thought          -> "thought"
        is IpcEvent.TerminalTraffic  -> "terminal_traffic"
        is IpcEvent.Scratchpad       -> "scratchpad"
        is IpcEvent.ContextSuggestions -> "context_suggestions"
        is IpcEvent.TaskScheduled    -> "task_scheduled"
        is IpcEvent.ScheduledTasksList -> "scheduled_tasks_list"
        is IpcEvent.IndexingProgress -> "indexing_progress"
        is IpcEvent.PluginMetadata   -> "plugin_metadata"
        is IpcEvent.WorkflowStatus   -> "workflow_status"
        is IpcEvent.ModelsList       -> "models_list"
        is IpcEvent.ToolOutputDelta  -> "tool_output_delta"
        is IpcEvent.SubAgentDone     -> "sub_agent_done"
        is IpcEvent.AgentWorkflowStatus -> "agent_workflow_status"
        else -> event::class.simpleName ?: "unknown"
    }

    internal fun ipcEventSummary(event: IpcEvent) = when (event) {
        is IpcEvent.TextDelta    -> "+${event.payload.text.length}ch"
        is IpcEvent.Status       -> event.payload.state
        is IpcEvent.ToolCall     -> event.payload.tool
        is IpcEvent.ToolResult   -> if (event.payload.error != null) "ERR" else "ok"
        is IpcEvent.Error        -> "[${event.payload.code}] ${event.payload.message.take(40)}"
        is IpcEvent.Log          -> "${event.payload.level}: ${event.payload.message.take(40)}"
        is IpcEvent.ApprovalRequest -> event.payload.tool
        is IpcEvent.HumanInputRequest -> "\"${event.payload.question.take(30)}\""
        is IpcEvent.SessionData  -> "sid=${event.payload.session_id?.take(8)}"
        is IpcEvent.SessionForked -> "new=${event.payload.new_session_id.take(8)}"
        is IpcEvent.ModelsList   -> "${event.payload.models.size} models"
        is IpcEvent.SubAgentDone -> "agent=${event.payload.agent_id.take(8)} ${if (event.payload.success) "ok" else "FAIL"}"
        is IpcEvent.Stats        -> {
            val i = event.payload["input_tokens"]?.jsonPrimitive?.content ?: "0"
            val o = event.payload["output_tokens"]?.jsonPrimitive?.content ?: "0"
            "tokens in=$i out=$o"
        }
        is IpcEvent.MessageEnd   -> {
            val i = event.payload.usage?.input_tokens ?: 0
            val o = event.payload.usage?.output_tokens ?: 0
            "done in=$i out=$o"
        }
        else -> ""
    }
}
