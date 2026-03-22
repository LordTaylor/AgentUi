package com.agentcore.ui

import androidx.compose.runtime.*
import com.agentcore.api.*
import com.agentcore.logic.IpcHandler
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.*
import com.agentcore.ui.components.HumanInputDialog
import com.agentcore.ui.components.SettingsDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@Composable
fun ChatMainScreen(mode: ConnectionMode) {
    val client = remember { AgentClient() }
    val cliExecutor = remember { CliExecutor() }
    val stdioExecutor = remember { StdioExecutor() }
    val unixSocketExecutor = remember { UnixSocketExecutor() }
    val scope = rememberCoroutineScope()

    val messages = remember { mutableStateListOf<Message>() }
    val sessions = remember { mutableStateListOf<SessionInfo>() }
    val availableTools = remember { mutableStateListOf<JsonObject>() }

    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var statusState by remember { mutableStateOf("IDLE") }
    var currentBackend by remember { mutableStateOf("ollama") }
    var currentRole by remember { mutableStateOf("base") }

    var showSettings by remember { mutableStateOf(false) }
    var sessionStats by remember { mutableStateOf<JsonObject?>(null) }
    var pendingApproval by remember { mutableStateOf<ApprovalRequestPayload?>(null) }
    var pendingHumanInput by remember { mutableStateOf<HumanInputPayload?>(null) }
    val logs = remember { mutableStateListOf<LogPayload>() }
    val terminalTraffic = remember { mutableStateListOf<TerminalTrafficPayload>() }
    var scratchpadContent by remember { mutableStateOf("") }
    val plugins = remember { mutableStateListOf<PluginMetadataPayload>() }
    val workflows = remember { mutableStateListOf<WorkflowStatusPayload>() }
    var agentGroup by remember { mutableStateOf<AgentGroupPayload?>(null) }
    var suggestedContext by remember { mutableStateOf<List<ContextItem>>(emptyList()) }
    val canvasElements = remember { mutableStateListOf<CanvasElement>() }

    LaunchedEffect(Unit) {
        val eventHandler = { event: IpcEvent ->
            IpcHandler.handleIpcEvent(
                event = event,
                messages = messages,
                onStatusChange = { statusState = it },
                onStatsUpdate = { sessionStats = it },
                onApprovalRequest = { pendingApproval = it },
                onLogReceived = { logs.add(it) },
                onScratchpadUpdate = { scratchpadContent = it },
                onTerminalTraffic = { terminalTraffic.add(it) },
                onIndexingProgress = { /* IndexingStatus panel */ },
                onPluginsLoaded = { plugins.clear(); plugins.addAll(it) },
                onWorkflowsUpdate = { workflows.clear(); workflows.addAll(it) },
                onInputTextChange = { },
                onVoiceUpdate = { },
                onContextSuggestions = { suggestedContext = it },
                onError = { /* błąd wyświetlany jako wiadomość SYSTEM w chacie */ },
                onSessionData = { /* metadane sesji dostępne; wiadomości historyczne nie są częścią protokołu */ },
                onHumanInputRequest = { pendingHumanInput = it },
                onAgentGroupUpdate = { agentGroup = it }
            )
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
                sessions.clear()
                sessions.addAll(client.listSessions())
                scope.launch { availableTools.addAll(client.listTools()) }
                client.observeEvents().collectLatest { eventHandler(it) }
            }
            else -> {}
        }
    }

    MainScreen(
        scope = scope,
        client = client,
        mode = mode,
        sessions = sessions,
        currentSessionId = currentSessionId,
        onSessionSelect = { id ->
            currentSessionId = id
            messages.clear()
            scope.launch {
                if (mode == ConnectionMode.IPC) client.sendCommand(IpcCommand.GetSession(GetSessionPayload(id)))
            }
        },
        messages = messages,
        statusState = statusState,
        onStatusChange = { statusState = it },
        sessionStats = sessionStats,
        onStatsRefresh = { scope.launch { if (mode == ConnectionMode.IPC) sessionStats = client.getStats() } },
        logs = logs,
        scratchpadContent = scratchpadContent,
        onScratchpadUpdate = { scratchpadContent = it },
        terminalTraffic = terminalTraffic,
        plugins = plugins,
        workflows = workflows,
        canvasElements = canvasElements,
        agentGroup = agentGroup,
        contextSuggestions = suggestedContext,
        pendingApproval = pendingApproval,
        onResolveApproval = { approved ->
            scope.launch {
                if (mode == ConnectionMode.IPC) client.sendCommand(
                    IpcCommand.ApprovalResponse(ApprovalResponsePayload(pendingApproval!!.id, approved))
                )
                pendingApproval = null
            }
        },
        onSendMessage = { text ->
            IpcHandler.performSendMessage(
                scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode,
                text, emptyList(), currentSessionId, messages,
                { }, { }, { statusState = it }
            )
        },
        showSettings = showSettings,
        onToggleSettings = { showSettings = !showSettings },
        onCancel = {
            scope.launch {
                if (mode == ConnectionMode.IPC && currentSessionId != null) {
                    client.sendCommand(IpcCommand.Cancel(CancelPayload(currentSessionId!!)))
                }
                statusState = "IDLE"
            }
        },
        onClearChat = { messages.clear() }
    )

    if (showSettings) {
        SettingsDialog(
            currentBackend = currentBackend,
            currentRole = currentRole,
            onDismiss = { showSettings = false },
            onSave = { b, r ->
                currentBackend = b
                currentRole = r
                showSettings = false
                scope.launch {
                    if (mode == ConnectionMode.IPC) {
                        client.sendCommand(IpcCommand.SetBackend(SetBackendPayload(b)))
                        client.sendCommand(IpcCommand.SetRole(SetRolePayload(r)))
                    }
                }
            }
        )
    }

    if (pendingHumanInput != null) {
        HumanInputDialog(
            request = pendingHumanInput!!,
            onRespond = { answer ->
                IpcHandler.performSendMessage(
                    scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode,
                    answer, emptyList(), currentSessionId, messages,
                    { }, { }, { statusState = it }
                )
                pendingHumanInput = null
            }
        )
    }
}
