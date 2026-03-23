package com.agentcore.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
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
import com.agentcore.api.*
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.chat.ChatIntent
import com.agentcore.ui.components.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    scope: CoroutineScope,
    client: AgentClient,
    mode: ConnectionMode,
    sessions: List<SessionInfo>,
    currentSessionId: String?,
    onSessionSelect: (String) -> Unit,
    onSendMessage: (String, List<String>) -> Unit,
    onReloadTools: () -> Unit,
    onCreateTool: (String, String) -> Unit,
    onDeleteTool: (String) -> Unit,
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
    showSettings: Boolean,
    onToggleSettings: () -> Unit,
    onSessionDelete: (String) -> Unit,
    onSessionPrune: (String) -> Unit,
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
    onRestartAgent: () -> Unit = {},
    onActivateProvider: (String, String) -> Unit = { _, _ -> },
    currentModelName: String = "",
    cauldronState: CauldronState = CauldronState.IDLE
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
    var selectedImages = remember { mutableStateListOf<String>() }
    var showCreateToolDialog by remember { mutableStateOf(false) }
    var newToolName by remember { mutableStateOf("") }

    val isDisconnected = statusState.uppercase() in listOf("ERROR", "DISCONNECTED", "CONNECTION_FAILED", "CRASHED")

    val shortcuts = AppShortcuts(
        onNewSession = onNewSession,
        onClearChat = onClearChat,
        onToggleSettings = onToggleSettings,
        onToggleSidebar = { onUpdateUiSettings(uiSettings.copy(sidebarVisible = !uiSettings.sidebarVisible)) },
        onFocusInput = { /* TODO: focus input field */ }
    )

    var activeTab by remember { mutableStateOf("Chat") }

    Surface(
        modifier = Modifier.fillMaxSize()
            .onPreviewKeyEvent { event -> handleKeyboardShortcut(event, shortcuts) },
        color = MaterialTheme.colorScheme.background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ── Fixed Narrow Sidebar ─────────────────────────────────────────
            NarrowSidebar(
                activeTab = activeTab,
                onTabSelect = { activeTab = it },
                onNewSession = onNewSession
            )

            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // ── Top Bar ──────────────────────────────────────────────────
                MainTopBar(
                    projectName = "DigitalArchitect",
                    onSearch = { /* TODO */ },
                    onToggleLeftSidebar = { onUpdateUiSettings(uiSettings.copy(sidebarVisible = !uiSettings.sidebarVisible)) },
                    onToggleRightSidebar = { onUpdateUiSettings(uiSettings.copy(showFiles = !uiSettings.showFiles)) },
                    isLeftSidebarVisible = sidebarVisible,
                    isRightSidebarVisible = uiSettings.showFiles,
                    isToolsVisible = uiSettings.showTools,
            onToggleTools = { onUpdateUiSettings(uiSettings.copy(showTools = !uiSettings.showTools)) },
            autoAccept = uiSettings.autoAccept,
            onToggleAutoAccept = { onUpdateUiSettings(uiSettings.copy(autoAccept = !uiSettings.autoAccept)) },
            onQuickConnect = { backend ->
                // Basic model mapping for quick connect
                val model = when(backend) {
                    "ollama" -> "llama3"
                    "lmstudio" -> "" // default
                    else -> ""
                }
                onActivateProvider(backend, model)
            },
            cauldronState = cauldronState,
            themeMode = uiSettings.themeMode,
                    onToggleTheme = { 
                        val newMode = if (uiSettings.themeMode == "LIGHT") "DARK" else "LIGHT"
                        onUpdateUiSettings(uiSettings.copy(themeMode = newMode))
                    }
                )

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(modifier = Modifier.weight(1f).fillMaxWidth().animateContentSize()) {

                    // ── Collapsible Sessions Sidebar (Middle-Left) ───────────────
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
                                    .width(260.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            )
                            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }

                    // ── Main Content Area (Center) ──────────────────────────────
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (statusState == "CRASHED") "Agent uległ awarii (OOM / SIGKILL)" else "Backend niedostępny — sprawdź połączenie",
                                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium
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
                            val showScrollButton by remember {
                                derivedStateOf {
                                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    lastVisible < messages.size - 1
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                // Chat Area
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (messages.isEmpty()) {
                                        item {
                                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.MailOutline, null, Modifier.size(64.dp), Color.Gray.copy(alpha = 0.3f))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("Brak wiadomości", style = MaterialTheme.typography.bodyLarge, color = Color.Gray.copy(alpha = 0.5f))
                                                }
                                            }
                                        }
                                    } else {
                                        itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                                            val isGrouped = index > 0 &&
                                                messages[index - 1].sender == msg.sender &&
                                                messages[index - 1].isFromUser == msg.isFromUser &&
                                                msg.type != MessageType.SYSTEM
                                            ChatBubble(
                                                msg = msg,
                                                isGrouped = isGrouped,
                                                fontSize = uiSettings.chatFontSize,
                                                codeFontSize = uiSettings.codeFontSize,
                                                onFork = { onFork(index) }
                                            )
                                        }
                                        if (statusState == "THINKING") {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth().padding(start = 24.dp), contentAlignment = Alignment.CenterStart) {
                                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                }
                                            }
                                        }
                                    }
                                }

                                LaunchedEffect(messages.size) {
                                    if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                                }

                                // Input Area
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (selectedImages.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                selectedImages.forEach { imagePath ->
                                                    Box(modifier = Modifier.size(60.dp)) {
                                                        AsyncImage(
                                                            model = imagePath,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                        IconButton(
                                                            onClick = { selectedImages.remove(imagePath) },
                                                            modifier = Modifier.align(Alignment.TopEnd).size(20.dp).offset(x = 6.dp, y = (-6).dp)
                                                        ) {
                                                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.error) {
                                                                Icon(Icons.Default.Close, null, Modifier.size(12.dp), Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Box(modifier = Modifier.weight(1f, fill = false)) {
                                            BasicTextField(
                                                value = inputText,
                                                onValueChange = { inputText = it },
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxWidth()
                                                    .onPreviewKeyEvent { event ->
                                                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                                                        if (inputText.isNotBlank() || selectedImages.isNotEmpty()) {
                                                            onSendMessage(inputText, selectedImages.toList())
                                                            inputText = ""
                                                            selectedImages.clear()
                                                        }
                                                            true
                                                        } else false
                                                    },
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                decorationBox = { innerTextField ->
                                                    if (inputText.isEmpty() && selectedImages.isEmpty()) {
                                                        Text("Wpisz wiadomość...", color = Color.Gray, fontSize = 14.sp)
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { 
                                                    scope.launch(Dispatchers.IO) {
                                                        pickImageDialog()?.let { selectedImages.add(it) }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Image, "Attach Image", modifier = Modifier.size(20.dp), tint = Color.Gray)
                                                }
                                                IconButton(onClick = {
                                                    messages.lastOrNull { it.isFromUser }?.let { inputText = it.text }
                                                }) {
                                                    Icon(Icons.Default.History, "History", modifier = Modifier.size(20.dp), tint = Color.Gray)
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                     if (inputText.isNotBlank() || selectedImages.isNotEmpty()) {
                                                         onSendMessage(inputText, selectedImages.toList())
                                                         inputText = ""
                                                         selectedImages.clear()
                                                     }
                                                },
                                                enabled = inputText.isNotBlank() || selectedImages.isNotEmpty(),
                                                modifier = Modifier.size(32.dp).background(
                                                    if (inputText.isNotBlank() || selectedImages.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                            ) {
                                                Icon(Icons.Default.Send, null, Modifier.size(16.dp), Color.White)
                                            }
                                        }
                                    }
                                }

                                IpcLogPanel(logs = ipcLogs, expanded = ipcLogExpanded, onToggle = onToggleIpcLog)
                            }

                            // Floating Scroll Button
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showScrollButton,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp)
                            ) {
                                SmallFloatingActionButton(
                                    onClick = { scope.launch { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) } },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // ── Right Side Panel (Working Directory / Tools) ──────────────
                    val isAnyPanelVisible = uiSettings.showFiles || uiSettings.showTools || showStats || showLogs || showScratchpad || showTerminal || showPluginManager || showWorkflowBuilder || showCanvas || showHelp || showOrchestrator

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isAnyPanelVisible,
                        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    ) {
                        Row(modifier = Modifier.fillMaxHeight()) {
                            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            Column(modifier = Modifier.width(sidePanelWidth).background(MaterialTheme.colorScheme.surface)) {
                                // Panel Selector / Working Directory Header
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("WORKING DIRECTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    IconButton(onClick = onStatsRefresh, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Refresh, null, Modifier.size(14.dp), Color.Gray)
                                    }
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    // Always show FileTree in Working Directory if tools aren't explicitly overriding
                                    if (uiSettings.showFiles) {
                                        FileTree(
                                            rootPath = workingDir.ifEmpty { System.getProperty("user.home") ?: "" },
                                            selectedFilePath = selectedFilePath,
                                            onFileSelected = { selectedFilePath = it }
                                        )
                                    }
                                    if (uiSettings.showTools) {
                                        ToolExplorer(
                                            tools = availableTools,
                                            onReloadTools = onReloadTools,
                                            onCreateTool = { showCreateToolDialog = true },
                                            onDeleteTool = onDeleteTool
                                        )
                                    }
                                    // Overlays for other panels
                                    if (showStats) StatsPanel(sessionStats) { onUpdateUiSettings(uiSettings.copy(showStats = false)) }
                                    if (showLogs) LogsPanel(logs) { onUpdateUiSettings(uiSettings.copy(showLogs = false)) }
                                    if (showScratchpad) ScratchpadPanel(scratchpadContent, onScratchpadUpdate, { onScratchpadUpdate(it) }) { onUpdateUiSettings(uiSettings.copy(showScratchpad = false)) }
                                    if (showTerminal) TerminalPanel(terminalTraffic) { onUpdateUiSettings(uiSettings.copy(showTerminal = false)) }
                                    if (showPluginManager) PluginManagerPanel(plugins) { onUpdateUiSettings(uiSettings.copy(showPluginManager = false)) }
                                    if (showWorkflowBuilder) WorkflowBuilderPanel(workflows) { onUpdateUiSettings(uiSettings.copy(showWorkflowBuilder = false)) }
                                    if (showCanvas) CanvasPanel(canvasElements) { onUpdateUiSettings(uiSettings.copy(showCanvas = false)) }
                                    if (showHelp) HelpPanel { onUpdateUiSettings(uiSettings.copy(showHelp = false)) }
                                    if (showOrchestrator) OrchestratorPanel(agentGroup) { onUpdateUiSettings(uiSettings.copy(showOrchestrator = false)) }
                                }

                                // Cauldron Engine Placeholder (Mockup style)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .padding(16.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Science, null, Modifier.size(32.dp), Color.Gray.copy(alpha = 0.5f))
                                        Text("CAULDRON ENGINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(alpha = 0.5f))
                                        Text("Procedural Animation Placeholder", fontSize = 8.sp, color = Color.Gray.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Status Bar ───────────────────────────────────────────────
                BottomStatusBar(sessionStats, ipcLogs.lastOrNull() ?: "CONNECTED", currentModelName)
            }
        }
    }

    if (showCreateToolDialog) {
        AlertDialog(
            onDismissRequest = { showCreateToolDialog = false },
            title = { Text("Create New Tool") },
            text = {
                Column {
                    Text("Tool Name", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = newToolName,
                        onValueChange = { newToolName = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        placeholder = { Text("e.g. calculator") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Template", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = true, onClick = {}, label = { Text("Python") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newToolName.isNotBlank()) {
                            onCreateTool(newToolName, "python")
                            newToolName = ""
                            showCreateToolDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateToolDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BottomStatusBar(stats: JsonObject?, lastIpc: String, currentModel: String) {
    val inTokens = stats?.get("input_tokens")?.let { it.toString().removeSurrounding("\"") } ?: "0"
    val outTokens = stats?.get("output_tokens")?.let { it.toString().removeSurrounding("\"") } ?: "0"
    val context = stats?.get("context_window_tokens")?.let { it.toString().removeSurrounding("\"") } ?: "0"
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().height(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Model and Tokens
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(Color.Cyan, RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(8.dp))
                Text("MODEL: ${currentModel.uppercase().ifEmpty { "UNKNOWN" }}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            
            Text("TOKENS: $inTokens ↑ $outTokens ↓", fontSize = 9.sp, color = Color.Gray)
            Text("CONTEXT: $context", fontSize = 9.sp, color = Color.Gray)

            Spacer(modifier = Modifier.weight(1f))

            Text("IPC: ${lastIpc.take(30)}", fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.7f))
            
            VerticalDivider(modifier = Modifier.height(12.dp), color = Color.Gray.copy(alpha = 0.2f))
            
            Text("THREAD: 0x${(stats?.hashCode() ?: 0).toString(16).uppercase().take(4)}", fontSize = 9.sp, color = Color.Gray)
        }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

private fun shortenPath(path: String): String {
    val items = path.split("/")
    return if (items.size > 2) ".../${items.takeLast(2).joinToString("/")}" else path
}

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

/** Blocking image picker dialog — call only from Dispatchers.IO. */
private fun pickImageDialog(): String? {
    return try {
        var result: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            val chooser = javax.swing.JFileChooser().apply {
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp", "gif")
                dialogTitle = "Wybierz obraz"
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
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        stats?.let { StatsDashboard(it) }
    }
}

@Composable
fun ToolsPanel(availableTools: List<JsonObject>, onReloadTools: () -> Unit, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        ToolExplorer(availableTools, onReloadTools)
    }
}

@Composable
fun LogsPanel(logs: List<LogPayload>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LogViewer(logs = logs, onClear = { /* logic */ })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun ScratchpadPanel(content: String, onUpdate: (String) -> Unit, onSaveRequest: (String) -> Unit, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Scratchpad(content = content, onSave = { onSaveRequest(it) }, onRefresh = { /* logic */ })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun TerminalPanel(traffic: List<TerminalTrafficPayload>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TerminalViewer(traffic = traffic, onClear = { /* logic */ })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun PluginManagerPanel(plugins: List<PluginMetadataPayload>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        PluginManager(plugins = plugins, onTogglePlugin = { _, _ -> }, onRefresh = { })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun WorkflowBuilderPanel(workflows: List<WorkflowStatusPayload>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        WorkflowBuilder(workflows = workflows, onStartWorkflow = { }, onStopWorkflow = { }, onRefresh = { })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun CanvasPanel(elements: List<CanvasElement>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        InteractiveCanvas(elements = elements, onClear = { }, onRefresh = { })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
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
