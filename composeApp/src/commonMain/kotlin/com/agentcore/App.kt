package com.agentcore

import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.AgentColorScheme
import com.agentcore.ui.AgentTypography
import com.agentcore.ui.ChatMainScreen
import com.agentcore.ui.ConnectionScreen
import com.agentcore.ui.SetupInstructions

import com.agentcore.ui.connection.ConnectionIntent
import com.agentcore.ui.connection.ConnectionViewModel
import org.koin.compose.koinInject

@Composable
fun App() {
    val viewModel: ConnectionViewModel = koinInject()
    val state by viewModel.uiState

    MaterialTheme(
        colorScheme = AgentColorScheme,
        typography = AgentTypography
    ) {
        AnimatedContent(targetState = state.selectedMode, label = "ConnectionScreenTransition") { mode ->
            when {
                mode == null -> {
                    ConnectionScreen(
                        state = state,
                        onIntent = { viewModel.onIntent(it) },
                        onConnect = { viewModel.onIntent(ConnectionIntent.SelectMode(it)) }
                    )
                }
                !state.isSetupConfirmed -> {
                    SetupInstructions(
                        mode = mode,
                        onBack = { viewModel.onIntent(ConnectionIntent.GoBack) },
                        onReady = { viewModel.onIntent(ConnectionIntent.ConfirmSetup) }
                    )
                }
                else -> {
                    ChatMainScreen(mode)
                }
            }
        }
    }
}
