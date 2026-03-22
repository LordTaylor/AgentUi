package com.agentcore

import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.AgentColorScheme
import com.agentcore.ui.AgentTypography
import com.agentcore.ui.ChatMainScreen
import com.agentcore.ui.ConnectionScreen

@Composable
fun App() {
    var connectionMode by remember { mutableStateOf<ConnectionMode?>(null) }

    MaterialTheme(
        colorScheme = AgentColorScheme,
        typography = AgentTypography
    ) {
        AnimatedContent(targetState = connectionMode, label = "ConnectionScreenTransition") { mode ->
            if (mode == null) {
                ConnectionScreen { connectionMode = it }
            } else {
                ChatMainScreen(mode)
            }
        }
    }
}
