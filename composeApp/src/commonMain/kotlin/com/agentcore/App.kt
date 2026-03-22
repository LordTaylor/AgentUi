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

@Composable
fun App() {
    var connectionMode by remember { mutableStateOf<ConnectionMode?>(null) }
    var isSetupConfirmed by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = AgentColorScheme,
        typography = AgentTypography
    ) {
        AnimatedContent(targetState = connectionMode, label = "ConnectionScreenTransition") { mode ->
            when {
                mode == null -> {
                    ConnectionScreen { 
                        connectionMode = it 
                        isSetupConfirmed = false
                    }
                }
                !isSetupConfirmed -> {
                    SetupInstructions(
                        mode = mode,
                        onBack = { connectionMode = null },
                        onReady = { isSetupConfirmed = true }
                    )
                }
                else -> {
                    ChatMainScreen(mode)
                }
            }
        }
    }
}
