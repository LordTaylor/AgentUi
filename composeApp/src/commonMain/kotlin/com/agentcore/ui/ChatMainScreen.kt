package com.agentcore.ui

import androidx.compose.runtime.*
import com.agentcore.api.*
import com.agentcore.logic.IpcHandler
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.*
import com.agentcore.ui.MainScreen
import com.agentcore.ui.components.cauldron.CauldronState
import com.agentcore.ui.components.HumanInputDialog
import com.agentcore.ui.components.ProviderDialog
import com.agentcore.ui.components.SettingsDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

import com.agentcore.ui.chat.ChatIntent
import com.agentcore.ui.chat.ChatViewModel
import org.koin.compose.koinInject

@Composable
fun ChatMainScreen(mode: ConnectionMode) {
    val viewModel: ChatViewModel = koinInject()
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState

    LaunchedEffect(mode) {
        viewModel.init(scope, mode)
    }

    val themeModeString = state.uiSettings.themeMode
    val themeMode = remember(themeModeString) {
        try { ThemeMode.valueOf(themeModeString) } catch (e: Exception) { ThemeMode.DARK }
    }

    AgentTheme(themeMode = themeMode) {
        val lastMsg = state.messages.lastOrNull()
        val cauldronState = when {
            state.loadingModelName != null -> CauldronState.LOADING
            state.statusState.uppercase() in listOf("THINKING", "BACKTRACKING") &&
                lastMsg != null && !lastMsg.isFromUser && lastMsg.type == MessageType.TEXT
                -> CauldronState.RECEIVING
            state.statusState.uppercase() in listOf("THINKING", "BACKTRACKING") -> CauldronState.THINKING
            state.statusState.uppercase() == "EXECUTING" -> CauldronState.SENDING
            else -> CauldronState.IDLE
        }

    MainScreen(
            scope = scope,
            client = koinInject(), 
        mode = mode,
        sessions = state.sessions,
        currentSessionId = state.currentSessionId,
        onSessionSelect = { id ->
            viewModel.onIntent(ChatIntent.SelectSession(id), scope, mode)
        },
        onSendMessage = { text, images ->
            viewModel.onIntent(ChatIntent.SendMessage(text, images), scope, mode)
        },
        onReloadTools = { viewModel.onIntent(ChatIntent.ReloadTools, scope, mode) },
        onReloadSkills = { viewModel.onIntent(ChatIntent.ReloadSkills, scope, mode) },
        onCreateTool = { name, template -> 
            viewModel.onIntent(ChatIntent.CreateTool(name, template), scope, mode)
        },
        onDeleteTool = { name ->
            viewModel.onIntent(ChatIntent.DeleteTool(name), scope, mode)
        },
        availableTools = state.availableTools,
        availableSkills = state.availableSkills,
        messages = state.messages,
        statusState = state.statusState,
        onStatusChange = { /* Handled via VM intents in onSendMessage etc */ },
        sessionStats = state.sessionStats,
        onStatsRefresh = { viewModel.onIntent(ChatIntent.RefreshStats, scope, mode) },
        logs = state.logs,
        scratchpadContent = state.scratchpadContent,
        onScratchpadUpdate = { viewModel.onIntent(ChatIntent.UpdateScratchpad(it), scope, mode) },
        terminalTraffic = state.terminalTraffic,
        plugins = state.plugins,
        workflows = state.workflows,
        canvasElements = state.canvasElements,
        agentGroup = state.agentGroup,
        contextSuggestions = state.suggestedContext,
        pendingApproval = state.pendingApproval,
        onResolveApproval = { approved ->
            viewModel.onIntent(ChatIntent.ResolveApproval(approved), scope, mode)
        },
        pendingPlan = state.pendingPlan,
        onResolvePlan = { approved ->
            state.pendingPlan?.let {
                viewModel.onIntent(ChatIntent.ResolvePlan(it.plan_id, approved), scope, mode)
            }
        },
        showSettings = state.showSettings,
        onToggleSettings = { viewModel.onIntent(ChatIntent.ToggleSettings, scope, mode) },
        onSessionDelete = { id -> viewModel.onIntent(ChatIntent.DeleteSession(id), scope, mode) },
        onSessionPrune = { id -> viewModel.onIntent(ChatIntent.PruneSession(id), scope, mode) },
        onSessionRename = { id, title -> viewModel.onIntent(ChatIntent.RenameSession(id, title), scope, mode) },
        onClearChat = { viewModel.onIntent(ChatIntent.ClearChat, scope, mode) },
        onCancel = {
            viewModel.onIntent(ChatIntent.CancelAction, scope, mode)
        },
        activeFilters = state.activeFilters,
        onToggleFilter = { tag -> viewModel.onIntent(ChatIntent.ToggleFilter(tag), scope, mode) },
        historySearchText = state.historySearchText,
        onHistorySearchChange = { query -> 
            viewModel.onIntent(ChatIntent.UpdateHistorySearch(query), scope, mode)
        },
        inputText = state.inputText,
        onIntent = { intent, sc, m -> viewModel.onIntent(intent, sc, m) },
        isSummarizing = state.isSummarizing,
        onSummarize = { 
            state.currentSessionId?.let { 
                viewModel.onIntent(ChatIntent.SummarizeContext(it), scope, mode) 
            }
        },
        onFork = { index ->
            state.currentSessionId?.let { sid ->
                viewModel.onIntent(ChatIntent.ForkSession(sid, index), scope, mode)
            }
        },
        uiSettings = state.uiSettings,
        onUpdateUiSettings = { viewModel.onIntent(ChatIntent.UpdateUiSettings(it), scope, mode) },
        workingDir = state.workingDir,
        onSetWorkingDir = { viewModel.onIntent(ChatIntent.SetWorkingDir(it), scope, mode) },
        ipcLogs = state.ipcLogs,
        ipcLogExpanded = state.ipcLogExpanded,
        onToggleIpcLog = { viewModel.onIntent(ChatIntent.ToggleIpcLog, scope, mode) },
        onNewSession = { viewModel.onIntent(ChatIntent.NewSession, scope, mode) },
        onDumpDebugLog = { viewModel.onIntent(ChatIntent.DumpDebugLog, scope, mode) },
        onToggleProviderDialog = { viewModel.onIntent(ChatIntent.ToggleProviderDialog, scope, mode) },
        onRestartAgent = { viewModel.onIntent(ChatIntent.RestartAgent, scope, mode) },
        onActivateProvider = { backend, model -> 
            viewModel.onIntent(ChatIntent.ActivateProvider(backend, model), scope, mode) 
        },
        currentModelName = state.currentModelName,
        loadingModelName = state.loadingModelName,
        modelLoadingProgress = state.modelLoadingProgress,
        cauldronState = cauldronState,
        onRetryMessage = { viewModel.onIntent(ChatIntent.RetryMessage, scope, mode) },
        showSearch = state.showSearch,
        toolOutput = state.toolOutput,
        showToolOutput = state.showToolOutput,
        onToggleToolOutput = { viewModel.onIntent(ChatIntent.ToggleToolOutput, scope, mode) },
        onClearToolOutput = { viewModel.onIntent(ChatIntent.ClearToolOutput, scope, mode) },
        tokenHistory = state.tokenHistory,
        showTokenAnalytics = state.showTokenAnalytics,
        onToggleTokenAnalytics = { viewModel.onIntent(ChatIntent.ToggleTokenAnalytics, scope, mode) },
        sessionFolders = state.sessionFolders,
        onMoveToFolder = { sessionId, folderName ->
            viewModel.onIntent(ChatIntent.MoveSessionToFolder(sessionId, folderName), scope, mode)
        },
        // A10 IPC 1.7 — AgentGroup workflow
        workflowGroupStatus = state.workflowGroupStatus,
        showWorkflowDialog = state.showWorkflowDialog,
        showCreateToolDialog = state.showCreateToolDialog,
        // A12: Enhanced KV Store
        memoryFacts = state.memoryFacts,
        showMemoryPanel = state.showMemoryPanel,
        onToggleMemoryPanel = { viewModel.onIntent(ChatIntent.ToggleMemoryPanel, scope, mode) },
        onDeleteMemoryKey = { key ->
            state.currentSessionId?.let { sid ->
                viewModel.onIntent(ChatIntent.DeleteMemoryKey(sid, key), scope, mode)
            }
        }
    )

    if (state.showSettings) {
        SettingsDialog(
            currentBackend = state.currentBackend,
            currentRole = state.currentRole,
            initialSystemPrompt = state.currentSystemPrompt,
            availableBackends = state.availableBackends,
            uiSettings = state.uiSettings,
            onUpdateUiSettings = { viewModel.onIntent(ChatIntent.UpdateUiSettings(it), scope, mode) },
            onDismiss = { viewModel.onIntent(ChatIntent.ToggleSettings, scope, mode) },
            onSave = { b, r, p ->
                viewModel.onIntent(ChatIntent.UpdateSettings(b, r), scope, mode)
                if (p != state.currentSystemPrompt) {
                    viewModel.onIntent(ChatIntent.SetSystemPrompt(p), scope, mode)
                }
            }
        )
    }

    if (state.showProviderDialog) {
        ProviderDialog(
            activeBackend = state.currentBackend,
            providerConfigs = state.uiSettings.providerConfigs,
            availableModels = state.availableModels,
            savedConfigs = state.uiSettings.savedProviderConfigs,
            onDismiss = { viewModel.onIntent(ChatIntent.ToggleProviderDialog, scope, mode) },
            onActivate = { backend, model ->
                viewModel.onIntent(ChatIntent.ActivateProvider(backend, model), scope, mode)
            },
            onActivateAndRestart = { backend, envVars, updatedConfigs ->
                viewModel.onIntent(
                    ChatIntent.ActivateProviderAndRestart(backend, envVars, updatedConfigs),
                    scope, mode
                )
            },
            onSaveConfigs = { configs ->
                viewModel.onIntent(ChatIntent.SaveProviderConfigs(configs), scope, mode)
            },
            onSaveNamedConfig = { backend, name, config ->
                viewModel.onIntent(ChatIntent.SaveNamedProviderConfig(backend, name, config), scope, mode)
            },
            onDeleteNamedConfig = { backend, name ->
                viewModel.onIntent(ChatIntent.DeleteNamedProviderConfig(backend, name), scope, mode)
            },
            onLoadNamedConfig = { backend, name ->
                viewModel.onIntent(ChatIntent.LoadNamedProviderConfig(backend, name), scope, mode)
            },
            onLmsLoadModel = { url, model, config ->
                viewModel.onIntent(ChatIntent.LmsLoadModel(url, model, config), scope, mode)
            },
            onFetchModels = { backend, url ->
                viewModel.onIntent(ChatIntent.FetchModels(backend, url), scope, mode)
            }
        )
    }

    if (state.pendingHumanInput != null) {
        HumanInputDialog(
            request = state.pendingHumanInput!!,
            onRespond = { answer ->
                viewModel.onIntent(ChatIntent.RespondHumanInput(answer), scope, mode)
            }
        )
    }

    if (state.pendingPlan != null) {
        com.agentcore.ui.components.PlanApprovalDialog(
            plan = state.pendingPlan!!,
            onResolve = { approved ->
                viewModel.onIntent(ChatIntent.ResolvePlan(state.pendingPlan!!.plan_id, approved), scope, mode)
            }
        )
    }
  }
}
