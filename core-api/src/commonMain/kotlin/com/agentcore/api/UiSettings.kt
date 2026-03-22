package com.agentcore.api

import kotlinx.serialization.Serializable

@Serializable
data class UiSettings(
    val sidebarVisible: Boolean = true,
    val sidePanelWidth: Float = 400f,
    val showStats: Boolean = false,
    val showTools: Boolean = false,
    val showLogs: Boolean = false,
    val showScratchpad: Boolean = false,
    val showTerminal: Boolean = false,
    val showPluginManager: Boolean = false,
    val showWorkflowBuilder: Boolean = false,
    val showCanvas: Boolean = false,
    val showHelp: Boolean = false,
    val showOrchestrator: Boolean = false,
)
