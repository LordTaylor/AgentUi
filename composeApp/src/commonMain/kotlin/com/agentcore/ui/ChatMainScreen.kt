package com.agentcore.ui

import androidx.compose.runtime.*
import com.agentcore.api.*
import com.agentcore.logic.IpcHandler
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.*
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

    MainScreen(
        scope = scope,
        client = koinInject(), // Direct inject or pass from VM? Better to pass if possible, but let's keep it simple for now.
        mode = mode,
        sessions = state.sessions,
        currentSessionId = state.currentSessionId,
        onSessionSelect = { id ->
            viewModel.onIntent(ChatIntent.SelectSession(id), scope, mode)
        },
        availableTools = state.availableTools,
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
        onSendMessage = { text ->
            viewModel.onIntent(ChatIntent.SendMessage(text), scope, mode)
        },
        showSettings = state.showSettings,
        onToggleSettings = { viewModel.onIntent(ChatIntent.ToggleSettings, scope, mode) },
        onSessionDelete = { id -> viewModel.onIntent(ChatIntent.DeleteSession(id), scope, mode) },
        onSessionPrune = { id -> viewModel.onIntent(ChatIntent.PruneSession(id), scope, mode) },
        onReloadTools = { viewModel.onIntent(ChatIntent.ReloadTools, scope, mode) },
        onCancel = {
            viewModel.onIntent(ChatIntent.CancelAction, scope, mode)
        },
        onClearChat = { viewModel.onIntent(ChatIntent.ClearChat, scope, mode) },
        activeFilters = state.activeFilters,
        onToggleFilter = { tag -> viewModel.onIntent(ChatIntent.ToggleFilter(tag), scope, mode) },
        onSessionTag = { id, tags -> viewModel.onIntent(ChatIntent.TagSession(id, tags), scope, mode) },
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
        onRestartAgent = { viewModel.onIntent(ChatIntent.RestartAgent, scope, mode) }
    )

    if (state.showSettings) {
        SettingsDialog(
            currentBackend = state.currentBackend,
            currentRole = state.currentRole,
            initialSystemPrompt = state.currentSystemPrompt,
            availableBackends = state.availableBackends,
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
}
