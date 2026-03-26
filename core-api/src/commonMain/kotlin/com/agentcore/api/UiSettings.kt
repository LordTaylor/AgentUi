package com.agentcore.api

import kotlinx.serialization.Serializable

/**
 * Per-provider connection configuration stored locally in UiSettings.
 * [model]   — model name to pass to set_backend
 * [baseUrl] — server address (LM Studio, Ollama, HuggingFace)
 * [apiKey]  — API key (Claude → ANTHROPIC_API_KEY, OpenAI → OPENAI_API_KEY, …)
 */
@Serializable
data class ProviderConfig(
    val model: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    // LM Studio specific
    val contextLength: Int? = null,
    val evalBatchSize: Int? = null,
    val flashAttention: Boolean? = null,
    val numExperts: Int? = null,
    val offloadKvCacheToGpu: Boolean? = null
)

@Serializable
data class SavedProviderConfig(
    val name: String,
    val config: ProviderConfig
)

@Serializable
data class DevModeOptions(
    val showToolCalls: Boolean = true,
    val showToolOutput: Boolean = false,
    val showThoughts: Boolean = true,
    val showSubAgentMessages: Boolean = false
)

@Serializable
data class UiSettings(
    val sidebarVisible: Boolean = true,
    val sidePanelWidth: Float = 400f,
    val showStats: Boolean = false,
    val showFiles: Boolean = true,
    val showSkills: Boolean = false,
    val showLogs: Boolean = false,
    val showScratchpad: Boolean = false,
    val showTerminal: Boolean = false,
    val showPluginManager: Boolean = false,
    val showWorkflowBuilder: Boolean = false,
    val showCanvas: Boolean = false,
    val showHelp: Boolean = false,
    val showOrchestrator: Boolean = false,
    val workingDir: String = "",
    val autoAccept: Boolean = false,
    val bypassAllPermissions: Boolean = false, // UNSAFE
    val themeMode: String = "DARK", // "LIGHT", "DARK", "SYSTEM"
    val developerMode: Boolean = true,
    val cauldronGridSize: Int = 128,
    val chatFontSize: Float = 14f,
    val codeFontSize: Float = 13f,
    /** Per-provider configs keyed by backend name (lmstudio, ollama, claude, openai, google, huggingface) */
    val providerConfigs: Map<String, ProviderConfig> = emptyMap(),
    /** Saved named configurations keyed by backend name */
    val savedProviderConfigs: Map<String, List<SavedProviderConfig>> = emptyMap(),
    val devModeOptions: DevModeOptions = DevModeOptions(),
    val parallelToolsHint: Boolean = true
)
