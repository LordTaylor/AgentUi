// Root screen composable: declares all parameters, manages local UI state,
// and delegates layout to MainScreenContent and overlays to MainScreenDialogs.
// Keyboard shortcuts are registered here via AppShortcuts + handleKeyboardShortcut.

package com.agentcore.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.agentcore.api.*
import com.agentcore.api.UiSettings
import com.agentcore.model.Message
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.chat.ChatIntent
import com.agentcore.ui.components.*
import com.agentcore.ui.components.cauldron.CauldronState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    pendingPlan: PlanReadyPayload? = null,
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
    onSessionCheckpoint: (String) -> Unit = {},
    showSearch: Boolean = false,
    workflowGroupStatus: AgentWorkflowStatusPayload? = null,
    showWorkflowDialog: Boolean = false,
    showCreateToolDialog: Boolean = false,
    memoryFacts: Map<String, String> = emptyMap(),
    showMemoryPanel: Boolean = false,
    onToggleMemoryPanel: () -> Unit = {},
    onDeleteMemoryKey: (String) -> Unit = {},
    selectedToolDetail: kotlinx.serialization.json.JsonObject? = null,
    showCheckpointDialog: Boolean = false,
    checkpoints: List<Int> = emptyList(),
    checkpointSessionId: String = "",
    backendHealth: Map<String, com.agentcore.api.PingResultPayload> = emptyMap(),
    onLoadBackendHealth: () -> Unit = {},
    currentSystemPrompt: String = "",
    onIntent: (ChatIntent, CoroutineScope, ConnectionMode) -> Unit = { _, _, _ -> }
) {
    val listState = rememberLazyListState()
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    val selectedImages = remember { mutableStateListOf<String>() }
    var newToolName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val showScrollButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val isDisconnected = statusState.uppercase() in listOf("ERROR", "DISCONNECTED", "CONNECTION_FAILED", "CRASHED")

    val shortcuts = AppShortcuts(
        onNewSession = onNewSession,
        onClearChat = onClearChat,
        onToggleSettings = onToggleSettings,
        onToggleSidebar = { onUpdateUiSettings(uiSettings.copy(sidebarVisible = !uiSettings.sidebarVisible)) },
        onFocusInput = { focusRequester.requestFocus() },
        onFocusSearch = {
            onIntent(ChatIntent.ToggleSearch, scope, mode)
            scope.launch { delay(100); searchFocusRequester.requestFocus() }
        }
    )

    var activeTab by remember { mutableStateOf("Chat") }
    LaunchedEffect(uiSettings.sidebarVisible) {
        if (!uiSettings.sidebarVisible && activeTab == "History") activeTab = "Chat"
    }
    var showAutoAcceptDialog by remember { mutableStateOf(false) }
    var showDevOptionsDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize()
            .onPreviewKeyEvent { event -> handleKeyboardShortcut(event, shortcuts) },
        color = MaterialTheme.colorScheme.background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            NarrowSidebar(
                activeTab = activeTab,
                onTabSelect = { tab ->
                    activeTab = tab
                    // Radio behaviour: opening a panel closes all others.
                    // Clicking the already-open panel closes it.
                    fun radio(isOpen: Boolean, open: com.agentcore.api.UiSettings.() -> com.agentcore.api.UiSettings) =
                        onUpdateUiSettings(if (isOpen) uiSettings.withAllPanelsClosed() else uiSettings.withAllPanelsClosed().open())
                    when (tab) {
                        "History"    -> onUpdateUiSettings(uiSettings.copy(sidebarVisible = !uiSettings.sidebarVisible))
                        "Library"    -> radio(uiSettings.showSkills)    { copy(showSkills = true) }
                        "Files"      -> radio(uiSettings.showFiles)     { copy(showFiles = true) }
                        "Health"     -> radio(uiSettings.showBackendHealth)  { copy(showBackendHealth = true) }
                        "Archive"    -> radio(uiSettings.showArchiveBrowser) { copy(showArchiveBrowser = true) }
                        "Hooks"      -> radio(uiSettings.showHookManager)    { copy(showHookManager = true) }
                        "Prompts"    -> radio(uiSettings.showPromptLibrary)  { copy(showPromptLibrary = true) }
                        "ToolEditor" -> radio(uiSettings.showToolEditor)     { copy(showToolEditor = true) }
                        "Scheduler"  -> radio(uiSettings.showScheduler)      { copy(showScheduler = true) }
                        "Pinned"     -> radio(uiSettings.showPinnedContext)  { copy(showPinnedContext = true) }
                        "Metrics"    -> radio(uiSettings.showMetrics)        { copy(showMetrics = true) }
                        "Personas"   -> radio(uiSettings.showPersonalityLab) { copy(showPersonalityLab = true) }
                    }
                },
                onNewSession = onNewSession
            )
            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            MainScreenContent(
                uiSettings = uiSettings, onUpdateUiSettings = onUpdateUiSettings,
                sessions = sessions, currentSessionId = currentSessionId,
                activeFilters = activeFilters, historySearchText = historySearchText,
                onHistorySearchChange = onHistorySearchChange, onSessionSelect = onSessionSelect,
                onSessionDelete = onSessionDelete, onSessionPrune = onSessionPrune,
                onToggleFilter = onToggleFilter, onNewSession = onNewSession,
                sessionFolders = sessionFolders, onMoveToFolder = onMoveToFolder,
                onSessionRename = onSessionRename, pinnedSessions = pinnedSessions,
                onSessionPin = onSessionPin, onSessionExport = onSessionExport,
                onSessionCheckpoint = onSessionCheckpoint,
                messages = messages, statusState = statusState, cauldronState = cauldronState,
                listState = listState, showSearch = showSearch, messageSearchQuery = messageSearchQuery,
                loadingModelName = loadingModelName, onIntent = onIntent, scope = scope, mode = mode,
                showScrollButton = showScrollButton, searchFocusRequester = searchFocusRequester,
                chatFocusRequester = focusRequester,
                workflowGroupStatus = workflowGroupStatus, toolOutput = toolOutput,
                showToolOutput = showToolOutput, onToggleToolOutput = onToggleToolOutput,
                onClearToolOutput = onClearToolOutput, inputText = inputText,
                onSendMessage = onSendMessage, selectedImages = selectedImages,
                onRetryMessage = onRetryMessage, onCancel = onCancel,
                ipcLogs = ipcLogs, ipcLogExpanded = ipcLogExpanded, onToggleIpcLog = onToggleIpcLog,
                isDisconnected = isDisconnected, onRestartAgent = onRestartAgent,
                workingDir = workingDir, onSetWorkingDir = onSetWorkingDir,
                selectedFilePath = selectedFilePath, onFileSelected = { selectedFilePath = it },
                availableTools = availableTools, availableSkills = availableSkills,
                onReloadTools = onReloadTools, onReloadSkills = onReloadSkills,
                onDeleteTool = onDeleteTool, sessionStats = sessionStats,
                logs = logs, scratchpadContent = scratchpadContent, onScratchpadUpdate = onScratchpadUpdate,
                terminalTraffic = terminalTraffic, plugins = plugins, workflows = workflows,
                canvasElements = canvasElements, agentGroup = agentGroup, onStatsRefresh = onStatsRefresh,
                currentModelName = currentModelName, tokenHistory = tokenHistory,
                showTokenAnalytics = showTokenAnalytics, onToggleTokenAnalytics = onToggleTokenAnalytics,
                activeTab = activeTab, onSetActiveTab = { activeTab = it },
                onToggleProviderDialog = onToggleProviderDialog, onDumpDebugLog = onDumpDebugLog,
                onActivateProvider = onActivateProvider, onToggleMemoryPanel = onToggleMemoryPanel,
                onOpenDevOptions = { showDevOptionsDialog = true },
                onShowAutoAcceptDialog = { showAutoAcceptDialog = true },
                backendHealth = backendHealth,
                onLoadBackendHealth = onLoadBackendHealth,
                currentSystemPrompt = currentSystemPrompt,
                onSetSystemPrompt = { onIntent(ChatIntent.SetSystemPrompt(it), scope, mode) },
                modifier = Modifier.weight(1f)
            )
        }

        MainScreenDialogs(
            showDevOptionsDialog = showDevOptionsDialog,
            devModeOptions = uiSettings.devModeOptions,
            onDismissDevOptions = { showDevOptionsDialog = false },
            onApplyDevOptions = { opts -> onUpdateUiSettings(uiSettings.copy(devModeOptions = opts)) },
            showAutoAcceptDialog = showAutoAcceptDialog,
            autoAccept = uiSettings.autoAccept,
            bypassAllPermissions = uiSettings.bypassAllPermissions,
            onDismissAutoAccept = { showAutoAcceptDialog = false },
            onConfirmAutoAccept = { auto, bypass ->
                onUpdateUiSettings(uiSettings.copy(autoAccept = auto, bypassAllPermissions = bypass))
                showAutoAcceptDialog = false
            },
            showCreateToolDialog = showCreateToolDialog,
            newToolName = newToolName,
            onNewToolNameChange = { newToolName = it },
            onConfirmCreateTool = {
                onIntent(ChatIntent.CreateTool(newToolName, ""), scope, mode)
                onIntent(ChatIntent.ToggleCreateToolDialog, scope, mode)
                newToolName = ""
            },
            onDismissCreateTool = { onIntent(ChatIntent.ToggleCreateToolDialog, scope, mode) },
            pendingApproval = pendingApproval,
            onResolveApproval = onResolveApproval,
            pendingPlan = pendingPlan,
            onResolvePlan = onResolvePlan,
            showTokenAnalytics = showTokenAnalytics,
            tokenHistory = tokenHistory,
            onDismissTokenAnalytics = onToggleTokenAnalytics,
            showWorkflowDialog = showWorkflowDialog,
            onIntent = onIntent,
            scope = scope,
            mode = mode,
            onDismissWorkflow = { onIntent(ChatIntent.ToggleWorkflowDialog, scope, mode) },
            showMemoryPanel = showMemoryPanel,
            currentSessionId = currentSessionId,
            memoryFacts = memoryFacts,
            onLoadMemory = { sid -> onIntent(ChatIntent.LoadMemory(sid), scope, mode) },
            onDeleteMemoryKey = onDeleteMemoryKey,
            onToggleMemoryPanel = onToggleMemoryPanel,
            selectedToolDetail = selectedToolDetail,
            onDismissToolDetail = { onIntent(ChatIntent.DismissToolDetail, scope, mode) },
            showCheckpointDialog = showCheckpointDialog,
            checkpoints = checkpoints,
            checkpointSessionId = checkpointSessionId,
            onRestoreCheckpoint = { n -> onIntent(ChatIntent.RestoreCheckpoint(checkpointSessionId, n), scope, mode) },
            onDismissCheckpointDialog = { onIntent(ChatIntent.DismissCheckpointDialog, scope, mode) }
        )
    }
}
