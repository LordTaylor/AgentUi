package com.agentcore.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.*
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.components.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@Composable
fun MainScreen(
    scope: CoroutineScope,
    client: AgentClient,
    mode: ConnectionMode,
    sessions: List<SessionInfo>,
    currentSessionId: String?,
    onSessionSelect: (String) -> Unit,
    availableTools: List<JsonObject>,
    messages: List<Message>,
    statusState: String,
    onStatusChange: (String) -> Unit,
    sessionStats: JsonObject?,
    onStatsRefresh: () -> Unit,
    logs: List<LogPayload>,
    scratchpadContent: String,
    onScratchpadUpdate: (String) -> Unit,
    terminalTraffic: List<TerminalTrafficPayload>,
    plugins: List<PluginMetadataPayload>,
    workflows: List<WorkflowStatusPayload>,
    canvasElements: List<CanvasElement>,
    agentGroup: AgentGroupPayload?,
    contextSuggestions: List<ContextItem>,
    pendingApproval: ApprovalRequestPayload?,
    onResolveApproval: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    showSettings: Boolean,
    onToggleSettings: () -> Unit,
    onSessionDelete: (String) -> Unit,
    onReloadTools: () -> Unit,
    onCancel: () -> Unit = {},
    onClearChat: () -> Unit = {}
) {
    var sidebarVisible by remember { mutableStateOf(true) }
    var sidePanelWidth by remember { mutableStateOf(400.dp) }

    // Panel Visibility State
    var showStats by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showScratchpad by remember { mutableStateOf(false) }
    var showTerminal by remember { mutableStateOf(false) }
    var showPluginManager by remember { mutableStateOf(false) }
    var showWorkflowBuilder by remember { mutableStateOf(false) }
    var showCanvas by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showOrchestrator by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    val isDisconnected = statusState.uppercase() in listOf("ERROR", "DISCONNECTED", "CONNECTION_FAILED")

    val shortcuts = AppShortcuts(
        onNewSession = { /* TODO: new session */ },
        onClearChat = onClearChat,
        onToggleSettings = onToggleSettings,
        onToggleSidebar = { sidebarVisible = !sidebarVisible },
        onFocusInput = { /* TODO: focus input field */ }
    )

    Surface(
        modifier = Modifier.fillMaxSize()
            .onPreviewKeyEvent { event -> handleKeyboardShortcut(event, shortcuts) },
        color = MaterialTheme.colorScheme.background
    ) {
        Row(modifier = Modifier.fillMaxSize().animateContentSize()) {
            AnimatedVisibility(
                visible = sidebarVisible,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Sidebar(
                    sessions = sessions,
                    onSessionSelect = onSessionSelect,
                    onSessionDelete = onSessionDelete,
                    modifier = Modifier
                        .width(300.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(end = 1.dp)
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { sidebarVisible = !sidebarVisible }) {
                            Icon(Icons.Default.Menu, contentDescription = "Toggle Sidebar", tint = if (sidebarVisible) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(if (statusState == "IDLE") Color.Green else Color.Yellow, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${mode.name} MODE - ${statusState.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (currentSessionId != null) {
                            Text(" | ${currentSessionId.take(8)}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TokenTracker(sessionStats)
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = {
                            onStatsRefresh()
                            showStats = !showStats
                        }) { Icon(Icons.Default.Info, contentDescription = "Stats", tint = if (showStats) MaterialTheme.colorScheme.primary else Color.Gray) }

                        IconButton(onClick = { showTools = !showTools }) {
                            Icon(Icons.Default.List, contentDescription = "Tools", tint = if (showTools) MaterialTheme.colorScheme.primary else Color.Gray)
                        }

                        // Cancel button — shown only when THINKING
                        AnimatedVisibility(
                            visible = statusState.uppercase() == "THINKING",
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        IconButton(onClick = onToggleSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (showSettings) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Connection Status Banner
                AnimatedVisibility(
                    visible = isDisconnected,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        color = Color(0xFFB71C1C)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Backend niedostępny — sprawdź połączenie",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

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
                                if (messages.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillParentMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.MailOutline,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp),
                                                    tint = Color.Gray.copy(alpha = 0.3f)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Brak wiadomości",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = Color.Gray.copy(alpha = 0.5f)
                                                )
                                                Text(
                                                    text = "Napisz coś żeby zacząć",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    itemsIndexed(messages) { index, msg ->
                                        val isGrouped = index > 0 &&
                                            messages[index - 1].sender == msg.sender &&
                                            messages[index - 1].isFromUser == msg.isFromUser &&
                                            msg.type != MessageType.SYSTEM

                                        ChatBubble(msg, isGrouped)
                                    }

                                    if (statusState == "THINKING") {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                            }
                                        }
                                    }
                                }
                            }

                            LaunchedEffect(messages.size) {
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                            }

                            // Input Area
                            Column(modifier = Modifier.fillMaxWidth().animateContentSize().padding(16.dp)) {
                                if (contextSuggestions.isNotEmpty()) {
                                    PredictiveContext(
                                        suggestions = contextSuggestions,
                                        onAttach = { path ->
                                            inputText += " [Context: $path]"
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    TextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Type a message...") }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = {
                                        if (inputText.isNotBlank()) {
                                            onSendMessage(inputText)
                                            inputText = ""
                                        }
                                    }) {
                                        Text("Send")
                                    }
                                }
                            }
                        }

                        // Right Side Panel
                        val isAnyPanelVisible = showStats || showTools || showLogs || showScratchpad || showTerminal || showPluginManager || showWorkflowBuilder || showCanvas || showHelp || showOrchestrator

                        AnimatedVisibility(
                            visible = isAnyPanelVisible,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) {
                            Row(modifier = Modifier.fillMaxHeight()) {
                                DraggableDivider { delta ->
                                    sidePanelWidth = (sidePanelWidth - delta.dp).coerceIn(300.dp, 800.dp)
                                }

                                Box(modifier = Modifier
                                    .width(sidePanelWidth)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(start = 1.dp)
                                ) {
                                    if (showStats) StatsPanel(sessionStats) { showStats = false }
                                    if (showTools) ToolsPanel(availableTools = availableTools, onReloadTools = onReloadTools, onDismiss = { showTools = false })
                                    if (showLogs) LogsPanel(logs) { showLogs = false }
                                    if (showScratchpad) ScratchpadPanel(scratchpadContent, onScratchpadUpdate, { onScratchpadUpdate(it) }) { showScratchpad = false }
                                    if (showTerminal) TerminalPanel(terminalTraffic) { showTerminal = false }
                                    if (showPluginManager) PluginManagerPanel(plugins) { showPluginManager = false }
                                    if (showWorkflowBuilder) WorkflowBuilderPanel(workflows) { showWorkflowBuilder = false }
                                    if (showCanvas) CanvasPanel(canvasElements) { showCanvas = false }
                                    if (showHelp) HelpPanel { showHelp = false }
                                    if (showOrchestrator) OrchestratorPanel(agentGroup) { showOrchestrator = false }
                                }
                            }
                        }
                    }

                    // Approval Overlay
                    if (pendingApproval != null) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                            ApprovalDialog(
                                request = pendingApproval,
                                onRespond = onResolveApproval
                            )
                        }
                    }
                }
            }
        }
    }
}

// Panel Wrappers (should be in separate files eventually)

@Composable
fun StatsPanel(stats: JsonObject?, onDismiss: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        stats?.let { StatsDashboard(it) }
    }
}

@Composable
fun ToolsPanel(availableTools: List<JsonObject>, onReloadTools: () -> Unit, onDismiss: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        ToolExplorer(availableTools, onReloadTools)
    }
}

@Composable
fun LogsPanel(logs: List<LogPayload>, onDismiss: () -> Unit) {
    LogViewer(logs = logs, onClear = { /* logic */ })
}

@Composable
fun ScratchpadPanel(content: String, onUpdate: (String) -> Unit, onSaveRequest: (String) -> Unit, onDismiss: () -> Unit) {
    Scratchpad(
        content = content,
        onSave = { onSaveRequest(it) },
        onRefresh = { /* logic */ }
    )
}

@Composable
fun TerminalPanel(traffic: List<TerminalTrafficPayload>, onDismiss: () -> Unit) {
    TerminalViewer(traffic = traffic, onClear = { /* logic */ })
}

@Composable
fun PluginManagerPanel(plugins: List<PluginMetadataPayload>, onDismiss: () -> Unit) {
    PluginManager(
        plugins = plugins,
        onTogglePlugin = { _, _ -> },
        onRefresh = { }
    )
}

@Composable
fun WorkflowBuilderPanel(workflows: List<WorkflowStatusPayload>, onDismiss: () -> Unit) {
    WorkflowBuilder(
        workflows = workflows,
        onStartWorkflow = { },
        onStopWorkflow = { },
        onRefresh = { }
    )
}

@Composable
fun CanvasPanel(elements: List<CanvasElement>, onDismiss: () -> Unit) {
    InteractiveCanvas(
        elements = elements,
        onClear = { },
        onRefresh = { }
    )
}

@Composable
fun HelpPanel(onDismiss: () -> Unit) {
    HelpSystem(onClose = onDismiss, onTryExample = { })
}

@Composable
fun OrchestratorPanel(group: AgentGroupPayload?, onDismiss: () -> Unit) {
    AgentOrchestrator(
        group = group,
        onAssignTask = { _, _ -> },
        onRefresh = { }
    )
}
