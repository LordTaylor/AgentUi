package com.agentcore.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
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
import com.agentcore.api.UiSettings
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.chat.ChatIntent
import com.agentcore.ui.components.*
import com.agentcore.ui.components.cauldron.CauldronState
import com.agentcore.ui.components.cauldron.WitchCauldron
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import kotlinx.serialization.json.*
import androidx.compose.ui.geometry.Offset // Added import for Offset

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
    onReloadSkills: () -> Unit,
    onCreateTool: (String, String) -> Unit,
    onDeleteTool: (String) -> Unit,
    availableTools: List<JsonObject>,
    availableSkills: List<SkillInfo>,
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
    onSessionRename: (String, String) -> Unit = { _, _ -> },
    activeFilters: List<String> = emptyList(),
    onToggleFilter: (String) -> Unit = {},
    historySearchText: String = "",
    onHistorySearchChange: (String) -> Unit = {},
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
    loadingModelName: String? = null,
    modelLoadingProgress: Float? = null,
    cauldronState: CauldronState = CauldronState.IDLE,
    messageSearchQuery: String = "",
    onUpdateSearchQuery: (String) -> Unit = {},
    onRetryMessage: () -> Unit = {},
    inputText: String = "",
    pendingPlan: com.agentcore.api.PlanReadyPayload? = null,
    onResolvePlan: (Boolean) -> Unit = {},
    toolOutput: List<String> = emptyList(),
    showToolOutput: Boolean = false,
    onToggleToolOutput: () -> Unit = {},
    onClearToolOutput: () -> Unit = {},
    tokenHistory: List<UsagePayload> = emptyList(),
    showTokenAnalytics: Boolean = false,
    onToggleTokenAnalytics: () -> Unit = {},
    sessionFolders: Map<String, String> = emptyMap(),
    onMoveToFolder: (String, String?) -> Unit = { _, _ -> },
    pinnedSessions: Set<String> = emptySet(),
    onSessionPin: (String) -> Unit = {},
    onSessionExport: (String) -> Unit = {},
    showSearch: Boolean = false,
    // A10 IPC 1.7
    workflowGroupStatus: com.agentcore.api.AgentWorkflowStatusPayload? = null,
    showWorkflowDialog: Boolean = false,
    showCreateToolDialog: Boolean = false,
    // A12: Enhanced KV Store
    memoryFacts: Map<String, String> = emptyMap(),
    showMemoryPanel: Boolean = false,
    onToggleMemoryPanel: () -> Unit = {},
    onDeleteMemoryKey: (String) -> Unit = {},
    onIntent: (ChatIntent, kotlinx.coroutines.CoroutineScope, com.agentcore.shared.ConnectionMode) -> Unit = { _, _, _ -> }
) {
    val sidebarVisible = uiSettings.sidebarVisible
    val sidePanelWidth = uiSettings.sidePanelWidth.dp

    val showStats = uiSettings.showStats
    val showFiles = uiSettings.showFiles
    val showSkills = uiSettings.showSkills
    val showLogs = uiSettings.showLogs
    val showScratchpad = uiSettings.showScratchpad
    val showTerminal = uiSettings.showTerminal
    val showPluginManager = uiSettings.showPluginManager
    val showWorkflowBuilder = uiSettings.showWorkflowBuilder
    val showCanvas = uiSettings.showCanvas
    val showHelp = uiSettings.showHelp
    val showOrchestrator = uiSettings.showOrchestrator

    val listState = rememberLazyListState()
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var selectedImages = remember { mutableStateListOf<String>() }
    var newToolName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    // ipcLogExpanded is now a parameter
    val showScrollButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }


    val isDisconnected = statusState.uppercase() in listOf("ERROR", "DISCONNECTED", "CONNECTION_FAILED", "CRASHED")

    val shortcuts = AppShortcuts(
        onNewSession = onNewSession,
        onClearChat = onClearChat,
        onToggleSettings = onToggleSettings,
        onToggleSidebar = { onUpdateUiSettings(uiSettings.copy(sidebarVisible = !uiSettings.sidebarVisible)) },
        onFocusInput = { focusRequester.requestFocus() },
        onFocusSearch = { 
            onIntent(ChatIntent.ToggleSearch, scope, mode)
            scope.launch {
                delay(100)
                searchFocusRequester.requestFocus()
            }
        }
    )

    var activeTab by remember { mutableStateOf("Chat") }
    // Keep History tab highlight in sync with sidebar visibility
    LaunchedEffect(sidebarVisible) {
        if (!sidebarVisible && activeTab == "History") activeTab = "Chat"
    }

    var showAutoAcceptDialog by remember { mutableStateOf(false) }
    var showDevOptionsDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize()
            .onPreviewKeyEvent { event -> handleKeyboardShortcut(event, shortcuts) },
        color = MaterialTheme.colorScheme.background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ── Fixed Narrow Sidebar ─────────────────────────────────────────
            NarrowSidebar(
                activeTab = activeTab,
                onTabSelect = { tab ->
                    activeTab = tab
                    when (tab) {
                        "History" -> onUpdateUiSettings(uiSettings.copy(sidebarVisible = !sidebarVisible))
                        "Library" -> onUpdateUiSettings(uiSettings.copy(showSkills = !uiSettings.showSkills))
                        "Files"   -> onUpdateUiSettings(uiSettings.copy(showFiles = !uiSettings.showFiles))
                    }
                },
                onNewSession = onNewSession
            )

            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // ── Top Bar ──────────────────────────────────────────────────
                MainTopBar(
                    projectName = "DigitalArchitect",
                    onSearch = { /* TODO */ },
                    onToggleProviderDialog = onToggleProviderDialog,
                    autoAccept = uiSettings.autoAccept,
                    onToggleAutoAccept = { showAutoAcceptDialog = true },
                    onQuickConnect = { backend ->
                        val model = when(backend) {
                            "ollama" -> "llama3"
                            "lmstudio" -> ""
                            else -> ""
                        }
                        onActivateProvider(backend, model)
                    },
                    onDumpDebugLog = onDumpDebugLog,
                    themeMode = uiSettings.themeMode,
                    onToggleTheme = {
                        val newMode = if (uiSettings.themeMode == "LIGHT") "DARK" else "LIGHT"
                        onUpdateUiSettings(uiSettings.copy(themeMode = newMode))
                    },
                    showToolOutput = showToolOutput,
                    onToggleToolOutput = onToggleToolOutput,
                    showTokenAnalytics = showTokenAnalytics,
                    onToggleTokenAnalytics = onToggleTokenAnalytics,
                    developerMode = uiSettings.developerMode,
                    onToggleDeveloperMode = { onUpdateUiSettings(uiSettings.copy(developerMode = !uiSettings.developerMode)) },
                    onOpenDevOptions = { showDevOptionsDialog = true },
                    onToggleWorkflowDialog = { onIntent(ChatIntent.ToggleWorkflowDialog, scope, mode) },
                    onToggleMemoryPanel = onToggleMemoryPanel
                )

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(modifier = Modifier.weight(1f).fillMaxWidth().animateContentSize()) {
                    // ── Middle-Left Sidebar (Sessions) ──────────────────────────
                    AnimatedVisibility(
                        visible = sidebarVisible,
                        enter = slideInHorizontally() + fadeIn(),
                        exit = slideOutHorizontally() + fadeOut()
                    ) {
                        Row {
                            Sidebar(
                                sessions = sessions,
                                activeFilters = activeFilters,
                                currentSessionId = currentSessionId,
                                searchText = historySearchText,
                                onSearchChange = onHistorySearchChange,
                                onSessionSelect = onSessionSelect,
                                onSessionDelete = onSessionDelete,
                                onSessionPrune = onSessionPrune,
                                onToggleFilter = onToggleFilter,
                                onCollapse = {
                                    onUpdateUiSettings(uiSettings.copy(sidebarVisible = false))
                                    activeTab = "Chat"
                                },
                                onNewSession = onNewSession,
                                sessionFolders = sessionFolders,
                                onMoveToFolder = onMoveToFolder,
                                onSessionRename = onSessionRename,
                                pinnedSessions = pinnedSessions,
                                onSessionPin = onSessionPin,
                                onSessionExport = onSessionExport,
                                modifier = Modifier
                                    .width(260.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            )
                            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }

                    // ── Main Content Area ─────────────────────────────────────
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        ConnectionStatusBanner(
                            statusState = statusState,
                            isDisconnected = isDisconnected,
                            onRestartAgent = onRestartAgent
                        )

                        val filteredMessages = remember(messages, messageSearchQuery, uiSettings.developerMode, uiSettings.devModeOptions) {
                            val base = if (messageSearchQuery.isBlank()) messages
                            else messages.filter { it.text.contains(messageSearchQuery, ignoreCase = true) }

                            if (uiSettings.developerMode) {
                                val opts = uiSettings.devModeOptions
                                base.filter { msg ->
                                    when {
                                        msg.type == MessageType.ACTION -> opts.showToolCalls
                                        msg.sender == "Thought"       -> opts.showThoughts
                                        msg.agentId != null           -> opts.showSubAgentMessages
                                        else                          -> true
                                    }
                                }
                            } else {
                                base.filter { it.type == MessageType.TEXT || it.type == MessageType.ERROR }
                            }
                        }

                        // ── Chat Area ──────────────────────────────────────────────────
                        ChatArea(
                            messages = messages,
                            filteredMessages = filteredMessages,
                            statusState = statusState,
                            cauldronState = cauldronState,
                            cauldronGridSize = uiSettings.cauldronGridSize,
                            listState = listState,
                            showSearch = showSearch,
                            messageSearchQuery = messageSearchQuery,
                            onFork = { onIntent(ChatIntent.ForkSession(currentSessionId ?: "", it), scope, mode) },
                            onSendMessage = { text, imgs ->
                                onSendMessage(text, imgs)
                                selectedImages.clear()
                            },
                            loadingModelName = loadingModelName,
                            onIntent = onIntent,
                            scope = scope,
                            mode = mode,
                            chatFontSize = uiSettings.chatFontSize,
                            codeFontSize = uiSettings.codeFontSize,
                            showScrollToBottom = showScrollButton,
                            searchFocusRequester = searchFocusRequester,
                            modifier = Modifier.weight(1f)
                        )

                        // A10 IPC 1.7: live workflow progress bar (visible while workflow runs)
                        AgentGroupWorkflowPanel(
                            status = workflowGroupStatus,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        ToolOutputPanel(
                            output = toolOutput,
                            isVisible = showToolOutput,
                            onToggle = onToggleToolOutput,
                            onClear = onClearToolOutput
                        )

                        // ── Bottom Fixed Area: Input + IPC Log ────────────────
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ChatInputArea(
                                inputText = inputText,
                                onInputTextChange = { onIntent(ChatIntent.UpdateInputText(it), scope, mode) },
                                selectedImages = selectedImages,
                                onRemoveImage = { selectedImages.remove(it) },
                                onAttachImage = {
                                    scope.launch(Dispatchers.IO) {
                                        pickImageDialog()?.let { selectedImages.add(it) }
                                    }
                                },
                                onSendMessage = { text, imgs ->
                                    onSendMessage(text, imgs)
                                    selectedImages.clear()
                                },
                                onRetryLast = onRetryMessage,
                                onShowHistory = {
                                    onIntent(ChatIntent.NavigateHistoryUp, scope, mode)
                                },
                                onNavigateHistoryUp = {
                                    onIntent(ChatIntent.NavigateHistoryUp, scope, mode)
                                },
                                onNavigateHistoryDown = {
                                    onIntent(ChatIntent.NavigateHistoryDown, scope, mode)
                                },
                                onCancel = onCancel,
                                isThinking = statusState.uppercase() in listOf("THINKING", "BACKTRACKING", "EXECUTING", "GENERATING"),
                                focusRequester = focusRequester
                            )

                            IpcLogPanel(logs = ipcLogs, expanded = ipcLogExpanded, onToggle = onToggleIpcLog)
                        }
                    }

                    // ── Right Side Panel ──────────────────────────────────────
                    RightSidePanel(
                        uiSettings = uiSettings,
                        sidePanelWidth = sidePanelWidth,
                        workingDir = workingDir,
                        selectedFilePath = selectedFilePath,
                        onFileSelected = { selectedFilePath = it },
                        availableTools = availableTools,
                        availableSkills = availableSkills,
                        onReloadTools = onReloadTools,
                        onReloadSkills = onReloadSkills,
                        onDeleteTool = onDeleteTool,
                        onCreateTool = { onIntent(ChatIntent.ToggleCreateToolDialog, scope, mode) },
                        sessionStats = sessionStats,
                        logs = logs,
                        scratchpadContent = scratchpadContent,
                        onScratchpadUpdate = onScratchpadUpdate,
                        terminalTraffic = terminalTraffic,
                        plugins = plugins,
                        workflows = workflows,
                        canvasElements = canvasElements,
                        agentGroup = agentGroup,
                        onUpdateUiSettings = onUpdateUiSettings,
                        onStatsRefresh = onStatsRefresh,
                        onSetWorkingDir = onSetWorkingDir,
                        scope = scope
                    )
                }

                // ── Status Bar ───────────────────────────────────────────────
                BottomStatusBar(tokenHistory, ipcLogs.lastOrNull() ?: "CONNECTED", currentModelName, sessionStats)
            }
        }

        if (showDevOptionsDialog) {
            ChatDisplaySettingsDialog(
                options = uiSettings.devModeOptions,
                onDismiss = { showDevOptionsDialog = false },
                onApply = { opts -> onUpdateUiSettings(uiSettings.copy(devModeOptions = opts)) }
            )
        }

        if (showAutoAcceptDialog) {
            AutoAcceptDialog(
                currentAutoAccept = uiSettings.autoAccept,
                currentBypassAll = uiSettings.bypassAllPermissions,
                onDismiss = { showAutoAcceptDialog = false },
                onConfirm = { auto, bypass ->
                    onUpdateUiSettings(uiSettings.copy(autoAccept = auto, bypassAllPermissions = bypass))
                    showAutoAcceptDialog = false
                }
            )
        }

        if (showCreateToolDialog) {
            AlertDialog(
                onDismissRequest = { onIntent(ChatIntent.ToggleCreateToolDialog, scope, mode) },
                title = { Text("Create New Tool") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newToolName,
                            onValueChange = { newToolName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tool Name") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onIntent(ChatIntent.CreateTool(newToolName, ""), scope, mode)
                        onIntent(ChatIntent.ToggleCreateToolDialog, scope, mode)
                        newToolName = ""
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onIntent(ChatIntent.ToggleCreateToolDialog, scope, mode) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (pendingApproval != null) {
            ApprovalDialog(
                request = pendingApproval,
                onRespond = onResolveApproval
            )
        }

        if (pendingPlan != null) {
            PlanApprovalDialog(
                plan = pendingPlan,
                onResolve = onResolvePlan
            )
        }

        if (showTokenAnalytics) {
            TokenAnalyticsDialog(
                history = tokenHistory,
                onDismiss = onToggleTokenAnalytics
            )
        }

        // A10 IPC 1.7: workflow builder dialog
        if (showWorkflowDialog) {
            WorkflowRunDialog(
                onIntent = onIntent,
                scope = scope,
                mode = mode,
                onDismiss = { onIntent(ChatIntent.ToggleWorkflowDialog, scope, mode) }
            )
        }

        // A12: Memory panel overlay
        if (showMemoryPanel) {
            androidx.compose.ui.window.Dialog(onDismissRequest = onToggleMemoryPanel) {
                MemoryPanel(
                    sessionId = currentSessionId,
                    facts = memoryFacts,
                    onRefresh = {
                        val sid = currentSessionId
                        if (sid != null) onIntent(ChatIntent.LoadMemory(sid), scope, mode)
                    },
                    onDeleteKey = onDeleteMemoryKey,
                    onClose = onToggleMemoryPanel,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
            }
        }
    }
}



