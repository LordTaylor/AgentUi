package com.agentcore.ui.connection

import com.agentcore.shared.ConnectionMode

data class ConnectionUiState(
    val selectedMode: ConnectionMode? = null,
    val isSetupConfirmed: Boolean = false,
    val isAutoConnected: Boolean = false
)

sealed class ConnectionIntent {
    data class SelectMode(val mode: ConnectionMode) : ConnectionIntent()
    /** Skips ConnectionScreen + SetupInstructions, goes straight to chat. */
    data class AutoConnect(val mode: ConnectionMode) : ConnectionIntent()
    object ConfirmSetup : ConnectionIntent()
    object GoBack : ConnectionIntent()
}
