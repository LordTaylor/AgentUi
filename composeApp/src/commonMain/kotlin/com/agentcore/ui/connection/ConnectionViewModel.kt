package com.agentcore.ui.connection

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.agentcore.shared.ConnectionMode

class ConnectionViewModel {
    private val _uiState = mutableStateOf(ConnectionUiState())
    val uiState: State<ConnectionUiState> = _uiState

    fun onIntent(intent: ConnectionIntent) {
        when (intent) {
            is ConnectionIntent.SelectMode -> {
                _uiState.value = _uiState.value.copy(
                    selectedMode = intent.mode,
                    isSetupConfirmed = false
                )
            }
            ConnectionIntent.ConfirmSetup -> {
                _uiState.value = _uiState.value.copy(isSetupConfirmed = true)
            }
            ConnectionIntent.GoBack -> {
                _uiState.value = _uiState.value.copy(
                    selectedMode = null,
                    isSetupConfirmed = false
                )
            }
        }
    }
}
