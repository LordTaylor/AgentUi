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
        onCancel = {
            viewModel.onIntent(ChatIntent.CancelAction, scope, mode)
        },
        onClearChat = { viewModel.onIntent(ChatIntent.ClearChat, scope, mode) }
    )

    if (state.showSettings) {
        SettingsDialog(
            currentBackend = state.currentBackend,
            currentRole = state.currentRole,
            onDismiss = { viewModel.onIntent(ChatIntent.ToggleSettings, scope, mode) },
            onSave = { b, r ->
                viewModel.onIntent(ChatIntent.UpdateSettings(b, r), scope, mode)
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
