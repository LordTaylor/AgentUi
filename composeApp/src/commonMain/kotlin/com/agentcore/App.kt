package com.agentcore

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.*
import com.agentcore.shared.*
import com.agentcore.ui.*
import com.agentcore.ui.components.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.File
import javax.swing.JFileChooser

enum class MessageType {
    TEXT, ACTION, SYSTEM
}

data class Message(
    val id: String,
    val sender: String,
    val text: String,
    val isFromUser: Boolean,
    val type: MessageType = MessageType.TEXT,
    val attachments: List<String>? = null,
    val extraContent: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun App() {
    var connectionMode by remember { mutableStateOf<ConnectionMode?>(null) }

    MaterialTheme(
        colorScheme = AgentColorScheme,
        typography = AgentTypography
    ) {
        AnimatedContent(targetState = connectionMode, label = "ConnectionScreenTransition") { mode ->
            if (mode == null) {
                ConnectionScreen { connectionMode = it }
            } else {
                ChatMainScreen(mode)
            }
        }
    }
}

@Composable
fun ChatMainScreen(mode: ConnectionMode) {
    val client = remember { AgentClient() }
    val cliExecutor = remember { CliExecutor() }
    val stdioExecutor = remember { StdioExecutor() }
    val unixSocketExecutor = remember { UnixSocketExecutor() }
    val scope = rememberCoroutineScope()
    
    val messages = remember { mutableStateListOf<Message>() }
    val sessions = remember { mutableStateListOf<String>() }
    val pendingAttachments = remember { mutableStateListOf<String>() }
    val availableTools = remember { mutableStateListOf<JsonObject>() }
    
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("") }
    var statusState by remember { mutableStateOf("IDLE") }
    var currentBackend by remember { mutableStateOf("ollama") }
    var currentRole by remember { mutableStateOf("base") }
    
    var showSettings by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showScratchpad by remember { mutableStateOf(false) }
    var showTerminal by remember { mutableStateOf(false) }
    var sessionStats by remember { mutableStateOf<JsonObject?>(null) }
    var pendingApproval by remember { mutableStateOf<ApprovalRequestPayload?>(null) }
    val logs = remember { mutableStateListOf<LogPayload>() }
    val terminalTraffic = remember { mutableStateListOf<TerminalTrafficPayload>() }
    var scratchpadContent by remember { mutableStateOf("") }
    var indexingProgress by remember { mutableStateOf<IndexingProgressPayload?>(null) }
    val plugins = remember { mutableStateListOf<PluginMetadataPayload>() }
    var showPluginManager by remember { mutableStateOf(false) }
    val workflows = remember { mutableStateListOf<WorkflowStatusPayload>() }
    var showWorkflowBuilder by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var voiceLevel by remember { mutableStateOf(0f) }
    var autoTts by remember { mutableStateOf(false) }
    var showCanvas by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showOrchestrator by remember { mutableStateOf(false) }
    var agentGroup by remember { mutableStateOf<AgentGroupPayload?>(null) }
    var suggestedContext by remember { mutableStateOf<List<ContextItem>>(emptyList()) }
    var sidePanelWidth by remember { mutableStateOf(400.dp) }
    val canvasElements = remember { mutableStateListOf<CanvasElement>() }
    
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        when (mode) {
            ConnectionMode.STDIO -> {
                stdioExecutor.start()
                stdioExecutor.events.collectLatest { event ->
                    handleIpcEvent(event, messages, { statusState = it }, { sessionStats = it }, { pendingApproval = it }, { logs.add(it) }, { scratchpadContent = it }, { terminalTraffic.add(it) }, { indexingProgress = it }, { plugins.clear(); plugins.addAll(it) }, { workflows.clear(); workflows.addAll(it) }, { inputText = it }, { isRecording = it.isRecording; voiceLevel = it.level }, { if (autoTts && event is IpcEvent.MessageComplete) messages.lastOrNull { m -> m.sender == "Agent" && m.type == MessageType.TEXT }?.let { m -> scope.launch { when(mode) { ConnectionMode.IPC -> client.sendCommand(IpcCommand.SpeakText(m.text)); else -> {} } } } }, { canvasElements.clear(); canvasElements.addAll(it.elements) }, { agentGroup = it }, { suggestedContext = it.suggestions })
                }
            }
            ConnectionMode.UNIX_SOCKET -> {
                unixSocketExecutor.start(scope)
                unixSocketExecutor.events.collectLatest { event ->
                    handleIpcEvent(event, messages, { statusState = it }, { sessionStats = it }, { pendingApproval = it }, { logs.add(it) }, { scratchpadContent = it }, { terminalTraffic.add(it) }, { indexingProgress = it }, { plugins.clear(); plugins.addAll(it) }, { workflows.clear(); workflows.addAll(it) }, { inputText = it }, { isRecording = it.isRecording; voiceLevel = it.level }, { if (autoTts && event is IpcEvent.MessageComplete) messages.lastOrNull { m -> m.sender == "Agent" && m.type == MessageType.TEXT }?.let { m -> scope.launch { when(mode) { ConnectionMode.IPC -> client.sendCommand(IpcCommand.SpeakText(m.text)); else -> {} } } } }, { canvasElements.clear(); canvasElements.addAll(it.elements) }, { agentGroup = it }, { suggestedContext = it.suggestions })
                }
            }
            ConnectionMode.IPC -> {
                sessions.clear()
                sessions.addAll(client.listSessions())
                
                scope.launch {
                    val tools = client.listTools()
                    availableTools.clear()
                    availableTools.addAll(tools)
                }

                client.observeEvents().collectLatest { event ->
                    handleIpcEvent(event, messages, { statusState = it }, { sessionStats = it }, { pendingApproval = it }, { logs.add(it) }, { scratchpadContent = it }, { terminalTraffic.add(it) }, { indexingProgress = it }, { plugins.clear(); plugins.addAll(it) }, { workflows.clear(); workflows.addAll(it) }, { inputText = it }, { isRecording = it.isRecording; voiceLevel = it.level }, { if (autoTts) messages.lastOrNull { m -> m.sender == "Agent" && m.type == MessageType.TEXT }?.let { m -> scope.launch { client.sendCommand(IpcCommand.SpeakText(m.text)) } } }, { canvasElements.clear(); canvasElements.addAll(it.elements) }, { agentGroup = it }, { suggestedContext = it.suggestions })
                }
            }
            else -> {}
        }

        // Mock Logs for Phase 5 Demonstration
        scope.launch {
            val mockMessages = listOf(
                "Initializing AgentCore v1.3...",
                "Connecting to local Ollama instance...",
                "Loading vector database for workspace indexing...",
                "Found 12 source files in project root.",
                "Tool 'bash' loaded with restricted permissions.",
                "Failed to reach remote registry, using local cache.",
                "High memory usage detected. Optimizing context window..."
            )
            val levels = listOf("INFO", "INFO", "DEBUG", "INFO", "WARN", "ERROR", "WARN")
            
            mockMessages.forEachIndexed { i, msg ->
                kotlinx.coroutines.delay(2000L * (i + 1))
                logs.add(LogPayload(
                    level = levels[i],
                    message = msg,
                    timestamp = (System.currentTimeMillis() / 1000).toString(),
                    source = "core-engine"
                ))
            }
        }
    }
    
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                sessions = sessions,
                onSessionSelect = { sessionId ->
                    currentSessionId = sessionId
                    messages.clear()
                    messages.add(Message("sys-${System.currentTimeMillis()}", "System", "Joined session: $sessionId", false, MessageType.SYSTEM))
                },
                modifier = Modifier.width(240.dp).background(MaterialTheme.colorScheme.surface)
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(if (statusState == "IDLE") Color.Green else Color.Yellow, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${mode.name} MODE - ${statusState.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (currentSessionId != null) {
                             Text(" | ${currentSessionId!!.take(8)}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TokenTracker(sessionStats)
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = { 
                            scope.launch { 
                                if (mode == ConnectionMode.IPC) {
                                    sessionStats = client.getStats()
                                    showStats = !showStats
                                }
                            }
                        }) { Icon(Icons.Default.Info, contentDescription = "Stats", tint = if (showStats) MaterialTheme.colorScheme.primary else Color.Gray) }
                        
                        IconButton(onClick = { showTools = !showTools }) { 
                            Icon(Icons.Default.List, contentDescription = "Tools", tint = if (showTools) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                        IconButton(onClick = { showSettings = !showSettings }) { 
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (showSettings) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    }
                }

                Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Chat Area
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(messages) { msg ->
                                    ChatMessage(msg, onActionClick = { action ->
                                        scope.launch {
                                            when (mode) {
                                                ConnectionMode.IPC -> client.sendCommand(IpcCommand.ExecuteAction(action))
                                                else -> {}
                                            }
                                        }
                                    })
                                }
                                
                                if (statusState == "THINKING") {
                                    item { ThinkingIndicator() }
                                }
                            }

                            LaunchedEffect(messages.size) {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }

                            // Input Area
                            ChatInput(
                                text = inputText,
                                onTextChange = { inputText = it },
                                attachments = pendingAttachments,
                                onAddAttachment = {
                                    val chooser = JFileChooser()
                                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                        pendingAttachments.add(chooser.selectedFile.absolutePath)
                                    }
                                },
                                onRemoveAttachment = { pendingAttachments.remove(it) },
                                onSend = {
                                    if (inputText.isNotBlank() || pendingAttachments.isNotEmpty()) {
                                        performSendMessage(
                                            scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode,
                                            inputText, pendingAttachments.toList(), currentSessionId, messages,
                                            { inputText = "" }, { pendingAttachments.clear() }, { statusState = it }
                                        )
                                    }
                                },
                                isRecording = isRecording,
                                voiceLevel = voiceLevel,
                                onToggleRecording = {
                                    scope.launch {
                                        if (mode == ConnectionMode.IPC) {
                                            if (isRecording) client.sendCommand(IpcCommand.StopRecording)
                                            else client.sendCommand(IpcCommand.StartRecording)
                                        }
                                    }
                                },
                                suggestedContext = suggestedContext,
                                onSelectContext = { item ->
                                    inputText += " @${item.label}"
                                }
                            )
                        }
                        
                        // Right Side Panel
                        if (showStats || showTools || showLogs || showScratchpad || showTerminal || showPluginManager || showWorkflowBuilder || showCanvas || showHelp || showOrchestrator) {
                            DraggableDivider { delta ->
                                sidePanelWidth = (sidePanelWidth - delta.dp).coerceIn(300.dp, 800.dp)
                            }
                            
                            Box(modifier = Modifier.width(sidePanelWidth).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                                if (showStats) StatsPanel(sessionStats) { showStats = false }
                                if (showTools) ToolsPanel(availableTools) { showTools = false }
                                if (showLogs) LogsPanel(logs) { showLogs = false }
                                if (showScratchpad) ScratchpadPanel(scratchpadContent, { scratchpadContent = it }, { 
                                    scope.launch {
                                        if (mode == ConnectionMode.IPC) client.sendCommand(IpcCommand.UpdateScratchpad(it))
                                    }
                                }) { showScratchpad = false }
                                if (showTerminal) TerminalPanel(terminalTraffic) { showTerminal = false }
                                if (showPluginManager) PluginManagerPanel(plugins) { showPluginManager = false }
                                if (showWorkflowBuilder) WorkflowBuilderPanel(workflows) { showWorkflowBuilder = false }
                                if (showCanvas) CanvasPanel(canvasElements) { showCanvas = false }
                                if (showHelp) HelpPanel { showHelp = false }
                                if (showOrchestrator) OrchestratorPanel(agentGroup) { showOrchestrator = false }
                            }
                        }
                    }
                    
                    // Approval Overlay
                    if (pendingApproval != null) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                            ApprovalCard(
                                request = pendingApproval!!,
                                onApprove = {
                                    scope.launch {
                                        if (mode == ConnectionMode.IPC) client.sendCommand(IpcCommand.ApproveAction(pendingApproval!!.id, true))
                                        pendingApproval = null
                                    }
                                },
                                onDeny = {
                                    scope.launch {
                                        if (mode == ConnectionMode.IPC) client.sendCommand(IpcCommand.ApproveAction(pendingApproval!!.id, false))
                                        pendingApproval = null
                                    }
                                }
                            )
                        }
                    }
                    
                    // Floating Action Buttons for quick toggles
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (indexingProgress != null && indexingProgress!!.progress < 1.0f) {
                                LinearProgressIndicator(
                                    progress = indexingProgress!!.progress,
                                    modifier = Modifier.width(100.dp).height(4.dp)
                                )
                                Text("Indexing: ${(indexingProgress!!.progress * 100).toInt()}%", fontSize = 10.sp)
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallFloatingActionButton(onClick = { showHelp = !showHelp }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                    Icon(Icons.Default.Info, "Help")
                                }
                                SmallFloatingActionButton(onClick = { showCanvas = !showCanvas }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                    Icon(Icons.Default.Edit, "Canvas")
                                }
                                SmallFloatingActionButton(onClick = { showLogs = !showLogs }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                    Icon(Icons.Default.List, "Logs")
                                }
                                SmallFloatingActionButton(onClick = { showTerminal = !showTerminal }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                    Icon(Icons.Default.Share, "Terminal")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentBackend = currentBackend,
            currentRole = currentRole,
            autoTts = autoTts,
            onDismiss = { showSettings = false },
            onSave = { b, r, tts ->
                currentBackend = b
                currentRole = r
                autoTts = tts
                showSettings = false
                
                val cmd = IpcCommand.SetBackend(b)
                val roleCmd = IpcCommand.SetRole(r)
                
                scope.launch {
                    when (mode) {
                        ConnectionMode.IPC -> {
                            client.sendCommand(cmd)
                            client.sendCommand(roleCmd)
                        }
                        ConnectionMode.STDIO -> {
                            stdioExecutor.sendCommand(cmd)
                            stdioExecutor.sendCommand(roleCmd)
                        }
                        ConnectionMode.UNIX_SOCKET -> {
                            unixSocketExecutor.sendCommand(cmd)
                            unixSocketExecutor.sendCommand(roleCmd)
                        }
                        else -> {}
                    }
                    messages.add(Message("sys-${System.currentTimeMillis()}", "System", "Updated settings: $b / $r", false, MessageType.SYSTEM))
                }
            }
        )
    }
}

@Composable
fun DraggableDivider(onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    onDrag(dragAmount)
                }
            }
            .background(Color.Gray.copy(alpha = 0.1f))
    )
}

private fun performSendMessage(
    scope: CoroutineScope,
    client: AgentClient,
    stdioExecutor: StdioExecutor,
    unixSocketExecutor: UnixSocketExecutor,
    cliExecutor: CliExecutor,
    mode: ConnectionMode,
    text: String,
    attachments: List<String>,
    currentSessionId: String?,
    messages: MutableList<Message>,
    onClearInput: () -> Unit,
    onClearAttachments: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    messages.add(Message("u-${System.currentTimeMillis()}", "You", text, true, attachments = attachments, timestamp = System.currentTimeMillis()))
    onClearInput()
    onClearAttachments()
    
    scope.launch {
        when (mode) {
            ConnectionMode.IPC -> {
                client.sendMessage(text, attachments, currentSessionId)
            }
            ConnectionMode.STDIO -> {
                stdioExecutor.sendMessage(text, attachments)
            }
            ConnectionMode.UNIX_SOCKET -> {
                unixSocketExecutor.sendMessage(text, attachments)
            }
            ConnectionMode.CLI -> {
                val response = cliExecutor.execute(text)
                messages.add(Message("a-${System.currentTimeMillis()}", "Agent", response, false))
            }
        }
    }
}

private fun handleIpcEvent(
    event: IpcEvent,
    messages: MutableList<Message>,
    onUpdateStatus: (String) -> Unit,
    onUpdateStats: (JsonObject) -> Unit,
    onPendingApproval: (ApprovalRequestPayload?) -> Unit,
    onLog: (LogPayload) -> Unit,
    onScratchpad: (String) -> Unit,
    onTerminal: (TerminalTrafficPayload) -> Unit,
    onIndexing: (IndexingProgressPayload?) -> Unit,
    onPlugins: (List<PluginMetadataPayload>) -> Unit,
    onWorkflows: (List<WorkflowStatusPayload>) -> Unit,
    onInputUpdate: (String) -> Unit,
    onVoiceUpdate: (VoiceLevelPayload) -> Unit,
    onMessageComplete: () -> Unit,
    onCanvasUpdate: (CanvasUpdatePayload) -> Unit,
    onAgentGroupUpdate: (AgentGroupPayload) -> Unit,
    onContextSuggestions: (ContextSuggestionsPayload) -> Unit
) {
    when (event) {
        is IpcEvent.MessageReceived -> {
            messages.add(Message("a-${System.currentTimeMillis()}", "Agent", event.text, false, timestamp = System.currentTimeMillis()))
        }
        is IpcEvent.StatusUpdated -> {
            onUpdateStatus(event.status)
        }
        is IpcEvent.StatsUpdated -> {
            onUpdateStats(event.stats)
        }
        is IpcEvent.ApprovalRequired -> {
            onPendingApproval(event.request)
        }
        is IpcEvent.LogEvent -> {
            onLog(event.log)
        }
        is IpcEvent.ScratchpadUpdated -> {
            onScratchpad(event.content)
        }
        is IpcEvent.TerminalTraffic -> {
            onTerminal(event.traffic)
        }
        is IpcEvent.IndexingProgress -> {
            onIndexing(event.progress)
        }
        is IpcEvent.PluginsUpdated -> {
            onPlugins(event.plugins)
        }
        is IpcEvent.WorkflowUpdated -> {
            onWorkflows(event.workflows)
        }
        is IpcEvent.InputTranscribed -> {
            onInputUpdate(event.text)
        }
        is IpcEvent.VoiceLevel -> {
            onVoiceUpdate(event.payload)
        }
        is IpcEvent.MessageComplete -> {
            onMessageComplete()
        }
        is IpcEvent.CanvasUpdated -> {
            onCanvasUpdate(event.payload)
        }
        is IpcEvent.AgentGroupUpdated -> {
            onAgentGroupUpdate(event.payload)
        }
        is IpcEvent.ContextSuggestions -> {
            onContextSuggestions(event.payload)
        }
        else -> {}
    }
}
