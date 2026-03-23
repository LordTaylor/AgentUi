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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.json.*

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
    onSessionPrune: (String) -> Unit,
    onReloadTools: () -> Unit,
    activeFilters: List<String> = emptyList(),
    onToggleFilter: (String) -> Unit = {},
    onSessionTag: (String, List<String>) -> Unit = { _, _ -> },
    isSummarizing: Boolean = false,
    onSummarize: () -> Unit = {},
    onFork: (Int) -> Unit = {},
    onCancel: () -> Unit = {},
    onClearChat: () -> Unit = {},
    uiSettings: UiSettings = UiSettings(),
    onUpdateUiSettings: (UiSettings) -> Unit = {},
    workingDir: String = "",
    onSetWorkingDir: (String) -> Unit = {},
    ipcLogs: List<String> = emptyList(),
    ipcLogExpanded: Boolean = false,
    onToggleIpcLog: () -> Unit = {},
    onNewSession: () -> Unit = {},
    onDumpDebugLog: () -> Unit = {},
    onToggleProviderDialog: () -> Unit = {},
    onRestartAgent: () -> Unit = {}
) {
    val sidebarVisible = uiSettings.sidebarVisible
    val sidePanelWidth = uiSettings.sidePanelWidth.dp

    val showStats = uiSettings.showStats
    val showTools = uiSettings.showTools
    val showLogs = uiSettings.showLogs
    val showScratchpad = uiSettings.showScratchpad
    val showTerminal = uiSettings.showTerminal
    val showPluginManager = uiSettings.showPluginManager
    val showWorkflowBuilder = uiSettings.showWorkflowBuilder
    val showCanvas = uiSettings.showCanvas
    val showHelp = uiSettings.showHelp
    val showOrchestrator = uiSettings.showOrchestrator

    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var selectedFilePath by remember { mutableStateOf<String?>(null) }

    val isDisconnected = statusState.uppercase() in listOf("ERROR", "DISCONNECTED", "CONNECTION_FAILED", "CRASHED")

    val shortcuts = AppShortcuts(
        onNewSession = { /* TODO: new session */ },
        onClearChat = onClearChat,
        onToggleSettings = onToggleSettings,
        onToggleSidebar = { onUpdateUiSettings(uiSettings.copy(sidebarVisible = !uiSettings.sidebarVisible)) },
        onFocusInput = { /* TODO: focus input field */ }
    )

    Surface(
        modifier = Modifier.fillMaxSize()
            .onPreviewKeyEvent { event -> handleKeyboardShortcut(event, shortcuts) },
        color = MaterialTheme.colorScheme.background
    ) {
        Row(modifier = Modifier.fillMaxSize().animateContentSize()) {

            // ── Left sidebar (sessions + file tree) ──────────────────────────
            AnimatedVisibility(
                visible = sidebarVisible,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Row {
                    Sidebar(
                        sessions = sessions,
                        activeFilters = activeFilters,
                        onSessionSelect = onSessionSelect,
                        onSessionDelete = onSessionDelete,
                        onSessionPrune = onSessionPrune,
                        onToggleFilter = onToggleFilter,
                        onSessionTag = onSessionTag,
                        workingDir = workingDir,
                        onFileSelected = { path -> selectedFilePath = path },
                        selectedFilePath = selectedFilePath,
                        onCollapse = { onUpdateUiSettings(uiSettings.copy(sidebarVisible = false)) },
                        onNewSession = onNewSession,
                        modifier = Modifier
                            .width(280.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // Expand button shown when sidebar is collapsed
            if (!sidebarVisible) {
                AppTooltip("Rozwiń panel boczny") {
                    IconButton(
                        onClick = { onUpdateUiSettings(uiSettings.copy(sidebarVisible = true)) },
                        modifier = Modifier.width(24.dp).fillMaxHeight()
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }

            // ── File preview panel ────────────────────────────────────────────
            AnimatedVisibility(
                visible = selectedFilePath != null,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Row {
                    selectedFilePath?.let { path ->
                        FilePreviewPanel(
                            filePath = path,
                            onClose = { selectedFilePath = null },
                            modifier = Modifier.width(360.dp)
                        )
                    }
                    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTooltip(if (sidebarVisible) "Zwiń panel boczny" else "Rozwiń panel boczny") {
                            IconButton(onClick = {
                                onUpdateUiSettings(uiSettings.copy(sidebarVisible = !uiSettings.sidebarVisible))
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = null, tint = if (sidebarVisible) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(if (statusState == "IDLE") Color.Green else Color.Yellow, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${mode.name} MODE - ${statusState.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (currentSessionId != null) {
                            Text(" | ${currentSessionId.take(8)}", fontSize = 10.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.width(24.dp))
                        
                        // Auto-Accept Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AUTO-AKCEPTACJA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (uiSettings.autoAccept) MaterialTheme.colorScheme.primary else Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = uiSettings.autoAccept,
                                onCheckedChange = { checked ->
                                    val newSettings = uiSettings.copy(autoAccept = checked)
                                    onUpdateUiSettings(newSettings)
                                    // UpdateConfig intent handled in ViewModel will sync to backend
                                },
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TokenTracker(sessionStats, isSummarizing, onSummarize)
                        Spacer(modifier = Modifier.width(16.dp))
                        AppTooltip(if (showStats) "Ukryj statystyki" else "Pokaż statystyki") {
                            IconButton(onClick = {
                                onStatsRefresh()
                                onUpdateUiSettings(uiSettings.copy(showStats = !showStats))
                            }) { Icon(Icons.Default.Info, contentDescription = null, tint = if (showStats) MaterialTheme.colorScheme.primary else Color.Gray) }
                        }
                        AppTooltip(if (showTools) "Ukryj narzędzia" else "Pokaż narzędzia") {
                            IconButton(onClick = {
                                onUpdateUiSettings(uiSettings.copy(showTools = !uiSettings.showTools))
                            }) {
                                Icon(Icons.Default.List, contentDescription = null, tint = if (showTools) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                        }

                        // Cancel button — shown only when THINKING
                        AnimatedVisibility(
                            visible = statusState.uppercase() == "THINKING",
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AppTooltip("Anuluj działanie agenta") {
                                IconButton(onClick = onCancel) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        AppTooltip("Wybierz provider / konfiguruj adresy i klucze API") {
                            IconButton(onClick = onToggleProviderDialog) {
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        AppTooltip("Zbierz logi do katalogu DebugLog") {
                            IconButton(onClick = onDumpDebugLog) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        AppTooltip("Ustawienia (backend, rola, system prompt)") {
                            IconButton(onClick = onToggleSettings) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = if (showSettings) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
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
                                text = if (statusState == "CRASHED") "Agent uległ awarii (OOM / SIGKILL)" else "Backend niedostępny — sprawdź połączenie",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (statusState == "CRASHED" || isDisconnected) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = onRestartAgent,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFB71C1C)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("Uruchom ponownie", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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
                                    itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                                        val isGrouped = index > 0 &&
                                            messages[index - 1].sender == msg.sender &&
                                            messages[index - 1].isFromUser == msg.isFromUser &&
                                            msg.type != MessageType.SYSTEM

                                        ChatBubble(msg, isGrouped, onFork = { onFork(index) })
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
                            Column(modifier = Modifier.fillMaxWidth().animateContentSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                // Working directory row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFFFFB74D)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    AppTooltip(
                                        if (workingDir.isEmpty()) "Kliknij, aby wybrać folder roboczy agenta"
                                        else "Folder roboczy: $workingDir\nKliknij, aby zmienić"
                                    ) {
                                        TextButton(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    val picked = pickFolderDialog(workingDir)
                                                    picked?.let { withContext(Dispatchers.Main) { onSetWorkingDir(it) } }
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(22.dp)
                                        ) {
                                            Text(
                                                text = if (workingDir.isEmpty()) "Ustaw folder roboczy…"
                                                       else shortenDisplayPath(workingDir),
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (contextSuggestions.isNotEmpty()) {
                                    PredictiveContext(
                                        suggestions = contextSuggestions,
                                        onAttach = { path -> inputText += " [Context: $path]" }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    TextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        modifier = Modifier.weight(1f)
                                            .onPreviewKeyEvent { event ->
                                                if (event.type == KeyEventType.KeyDown &&
                                                    event.key == Key.Enter &&
                                                    !event.isShiftPressed) {
                                                    if (inputText.isNotBlank()) {
                                                        onSendMessage(inputText)
                                                        inputText = ""
                                                    }
                                                    true
                                                } else false
                                            },
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

                            // IPC Log Panel (collapsible, pinned to bottom)
                            IpcLogPanel(
                                logs = ipcLogs,
                                expanded = ipcLogExpanded,
                                onToggle = onToggleIpcLog
                            )
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
                                    val newWidth = (uiSettings.sidePanelWidth - delta).coerceIn(300f, 800f)
                                    onUpdateUiSettings(uiSettings.copy(sidePanelWidth = newWidth))
                                }

                               Box(modifier = Modifier
                                    .width(sidePanelWidth)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(start = 1.dp)
                                ) {
                                    if (showStats) StatsPanel(sessionStats) { onUpdateUiSettings(uiSettings.copy(showStats = false)) }
                                    if (showTools) ToolsPanel(availableTools = availableTools, onReloadTools = onReloadTools, onDismiss = { onUpdateUiSettings(uiSettings.copy(showTools = false)) })
                                    if (showLogs) LogsPanel(logs) { onUpdateUiSettings(uiSettings.copy(showLogs = false)) }
                                    if (showScratchpad) ScratchpadPanel(scratchpadContent, onScratchpadUpdate, { onScratchpadUpdate(it) }) { onUpdateUiSettings(uiSettings.copy(showScratchpad = false)) }
                                    if (showTerminal) TerminalPanel(terminalTraffic) { onUpdateUiSettings(uiSettings.copy(showTerminal = false)) }
                                    if (showPluginManager) PluginManagerPanel(plugins) { onUpdateUiSettings(uiSettings.copy(showPluginManager = false)) }
                                    if (showWorkflowBuilder) WorkflowBuilderPanel(workflows) { onUpdateUiSettings(uiSettings.copy(showWorkflowBuilder = false)) }
                                    if (showCanvas) CanvasPanel(canvasElements) { onUpdateUiSettings(uiSettings.copy(showCanvas = false)) }
                                    if (showHelp) HelpPanel { onUpdateUiSettings(uiSettings.copy(showHelp = false)) }
                                    if (showOrchestrator) OrchestratorPanel(agentGroup) { onUpdateUiSettings(uiSettings.copy(showOrchestrator = false)) }
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

                    BottomStatusBar(sessionStats)
                }
            }
        }
    }
}

@Composable
fun BottomStatusBar(stats: JsonObject?) {
    val inTokens = stats?.get("input_tokens")?.toString()?.replace("\"", "") ?: "0"
    val outTokens = stats?.get("output_tokens")?.toString()?.replace("\"", "") ?: "0"
    val ctxTokens = stats?.get("context_window_tokens")?.toString()?.replace("\"", "") ?: "0"
    val backend = stats?.get("backend")?.toString()?.replace("\"", "") ?: "N/A"

    Surface(
        color = Color(0xFF121212),
        modifier = Modifier.fillMaxWidth().height(26.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "BACKEND: ${backend.uppercase()}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.weight(1f))

            StatusBarItem("IN", inTokens)
            StatusBarItem("OUT", outTokens)
            StatusBarItem("CONTEXT", ctxTokens)
            
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun StatusBarItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64B5F6) // Hardcoded light blue
        )
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

private fun shortenDisplayPath(path: String): String {
    val home = System.getProperty("user.home") ?: ""
    return if (path.startsWith(home)) "~${path.removePrefix(home)}" else path
}

/** Blocking folder picker dialog — call only from Dispatchers.IO. */
private fun pickFolderDialog(currentPath: String): String? {
    return try {
        var result: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            val chooser = javax.swing.JFileChooser(currentPath.ifEmpty { System.getProperty("user.home") }).apply {
                fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Wybierz folder roboczy"
            }
            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                result = chooser.selectedFile.absolutePath
            }
            latch.countDown()
        }
        latch.await(60, java.util.concurrent.TimeUnit.SECONDS)
        result
    } catch (_: Exception) { null }
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
