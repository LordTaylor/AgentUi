// Inner column content: TopBar, animated Sidebar, ChatArea, RightSidePanel, StatusBar.
// Receives only the state slices it needs; dialog state is handled by MainScreenDialogs.
// Called by MainScreen after the NarrowSidebar strip.

package com.agentcore.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agentcore.api.*
import com.agentcore.api.UiSettings
import com.agentcore.model.Message
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.chat.ChatIntent
import com.agentcore.ui.components.*
import com.agentcore.ui.components.cauldron.CauldronState
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject

@Composable
internal fun MainScreenContent(
    uiSettings: UiSettings,
    onUpdateUiSettings: (UiSettings) -> Unit,
    sessions: List<SessionInfo>,
    currentSessionId: String?,
    activeFilters: List<String>,
    historySearchText: String,
    onHistorySearchChange: (String) -> Unit,
    onSessionSelect: (String) -> Unit,
    onSessionDelete: (String) -> Unit,
    onSessionPrune: (String) -> Unit,
    onToggleFilter: (String) -> Unit,
    onNewSession: () -> Unit,
    sessionFolders: Map<String, String>,
    onMoveToFolder: (String, String?) -> Unit,
    onSessionRename: (String, String) -> Unit,
    pinnedSessions: Set<String>,
    onSessionPin: (String) -> Unit,
    onSessionExport: (String) -> Unit,
    onSessionCheckpoint: (String) -> Unit = {},
    messages: List<Message>,
    statusState: String,
    cauldronState: CauldronState,
    listState: LazyListState,
    showSearch: Boolean,
    messageSearchQuery: String,
    loadingModelName: String?,
    onIntent: (ChatIntent, CoroutineScope, ConnectionMode) -> Unit,
    scope: CoroutineScope,
    mode: ConnectionMode,
    showScrollButton: Boolean,
    searchFocusRequester: FocusRequester,
    chatFocusRequester: FocusRequester,
    workflowGroupStatus: AgentWorkflowStatusPayload?,
    toolOutput: List<String>,
    showToolOutput: Boolean,
    onToggleToolOutput: () -> Unit,
    onClearToolOutput: () -> Unit,
    inputText: String,
    onSendMessage: (String, List<String>) -> Unit,
    selectedImages: MutableList<String>,
    onRetryMessage: () -> Unit,
    onCancel: () -> Unit,
    ipcLogs: List<String>,
    ipcLogExpanded: Boolean,
    onToggleIpcLog: () -> Unit,
    isDisconnected: Boolean,
    onRestartAgent: () -> Unit,
    workingDir: String,
    onSetWorkingDir: (String) -> Unit,
    selectedFilePath: String?,
    onFileSelected: (String?) -> Unit,
    availableTools: List<JsonObject>,
    availableSkills: List<SkillInfo>,
    onReloadTools: () -> Unit,
    onReloadSkills: () -> Unit,
    onDeleteTool: (String) -> Unit,
    sessionStats: JsonObject?,
    logs: List<LogPayload>,
    scratchpadContent: String,
    onScratchpadUpdate: (String) -> Unit,
    terminalTraffic: List<TerminalTrafficPayload>,
    plugins: List<PluginMetadataPayload>,
    workflows: List<WorkflowStatusPayload>,
    canvasElements: List<CanvasElement>,
    agentGroup: AgentGroupPayload?,
    onStatsRefresh: () -> Unit,
    currentModelName: String,
    tokenHistory: List<UsagePayload>,
    showTokenAnalytics: Boolean,
    onToggleTokenAnalytics: () -> Unit,
    activeTab: String,
    onSetActiveTab: (String) -> Unit,
    onToggleProviderDialog: () -> Unit,
    onDumpDebugLog: () -> Unit,
    onActivateProvider: (String, String) -> Unit,
    onToggleMemoryPanel: () -> Unit,
    onOpenDevOptions: () -> Unit,
    onShowAutoAcceptDialog: () -> Unit,
    backendHealth: Map<String, com.agentcore.api.PingResultPayload> = emptyMap(),
    onLoadBackendHealth: () -> Unit = {},
    currentSystemPrompt: String = "",
    onSetSystemPrompt: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sidebarVisible = uiSettings.sidebarVisible
    val sidePanelWidth: Dp = uiSettings.sidePanelWidth.dp

    Column(modifier = modifier.fillMaxHeight()) {
        MainTopBar(
            projectName = "DigitalArchitect",
            onSearch = { /* TODO */ },
            onToggleProviderDialog = onToggleProviderDialog,
            autoAccept = uiSettings.autoAccept,
            onToggleAutoAccept = onShowAutoAcceptDialog,
            onQuickConnect = { backend ->
                val model = when (backend) { "ollama" -> "llama3"; "lmstudio" -> ""; else -> "" }
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
            onOpenDevOptions = onOpenDevOptions,
            onToggleWorkflowDialog = { onIntent(ChatIntent.ToggleWorkflowDialog, scope, mode) },
            onToggleMemoryPanel = onToggleMemoryPanel,
            isRightSidebarVisible = uiSettings.showFiles,
            onToggleRightSidebar = {
                onUpdateUiSettings(
                    if (uiSettings.showFiles) uiSettings.withAllPanelsClosed()
                    else uiSettings.withAllPanelsClosed().copy(showFiles = true)
                )
            }
        )

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        Row(modifier = Modifier.weight(1f).fillMaxWidth().animateContentSize()) {
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
                            onSetActiveTab("Chat")
                        },
                        onNewSession = onNewSession,
                        sessionFolders = sessionFolders,
                        onMoveToFolder = onMoveToFolder,
                        onSessionRename = onSessionRename,
                        pinnedSessions = pinnedSessions,
                        onSessionPin = onSessionPin,
                        onSessionExport = onSessionExport,
                        onSessionCheckpoint = onSessionCheckpoint,
                        modifier = Modifier
                            .width(260.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    )
                    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }

            ChatColumnSection(
                messages = messages,
                messageSearchQuery = messageSearchQuery,
                uiSettings = uiSettings,
                statusState = statusState,
                cauldronState = cauldronState,
                listState = listState,
                showSearch = showSearch,
                currentSessionId = currentSessionId,
                loadingModelName = loadingModelName,
                workflowGroupStatus = workflowGroupStatus,
                toolOutput = toolOutput,
                showToolOutput = showToolOutput,
                onToggleToolOutput = onToggleToolOutput,
                onClearToolOutput = onClearToolOutput,
                inputText = inputText,
                onSendMessage = onSendMessage,
                selectedImages = selectedImages,
                onRetryMessage = onRetryMessage,
                onCancel = onCancel,
                ipcLogs = ipcLogs,
                ipcLogExpanded = ipcLogExpanded,
                onToggleIpcLog = onToggleIpcLog,
                isDisconnected = isDisconnected,
                onRestartAgent = onRestartAgent,
                searchFocusRequester = searchFocusRequester,
                chatFocusRequester = chatFocusRequester,
                onIntent = onIntent,
                scope = scope,
                mode = mode,
                showScrollButton = showScrollButton,
                modifier = Modifier.weight(1f)
            )

            RightSidePanel(
                uiSettings = uiSettings,
                sidePanelWidth = sidePanelWidth,
                workingDir = workingDir,
                selectedFilePath = selectedFilePath,
                onFileSelected = onFileSelected,
                availableTools = availableTools,
                availableSkills = availableSkills,
                onReloadTools = onReloadTools,
                onReloadSkills = onReloadSkills,
                onDeleteTool = onDeleteTool,
                onCreateTool = { onIntent(ChatIntent.ToggleCreateToolDialog, scope, mode) },
                onGetToolDetail = { name -> onIntent(ChatIntent.GetToolDetail(name), scope, mode) },
                sessionStats = sessionStats,
                logs = logs,
                scratchpadContent = scratchpadContent,
                onScratchpadUpdate = onScratchpadUpdate,
                terminalTraffic = terminalTraffic,
                plugins = plugins,
                onToggleTool = { name, enable -> onIntent(ChatIntent.ToggleTool(name, enable), scope, mode) },
                workflows = workflows,
                canvasElements = canvasElements,
                agentGroup = agentGroup,
                onUpdateUiSettings = onUpdateUiSettings,
                onStatsRefresh = onStatsRefresh,
                onSetWorkingDir = onSetWorkingDir,
                scope = scope,
                backendHealth = backendHealth,
                onLoadBackendHealth = onLoadBackendHealth,
                currentSystemPrompt = currentSystemPrompt,
                onSetSystemPrompt = onSetSystemPrompt,
                messages = messages,
                onScheduleTask = { text, at, cron -> onIntent(ChatIntent.ScheduleTask(text, at, cron), scope, mode) }
            )
        }

        BottomStatusBar(tokenHistory, ipcLogs.lastOrNull() ?: "CONNECTED", currentModelName, sessionStats)
    }
}
