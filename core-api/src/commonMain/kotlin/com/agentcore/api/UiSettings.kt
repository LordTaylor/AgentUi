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
    val parallelToolsHint: Boolean = true,
    /** ReAct mode: agent emits Thought: blocks before each tool call. */
    val reactMode: Boolean = false,
    /** Backend Health Dashboard panel visible in right side panel. */
    val showBackendHealth: Boolean = false,
    /** Session Archive Browser panel (reads ~/.agentcore/sessions/archive/). */
    val showArchiveBrowser: Boolean = false,
    /** Hook Management panel (reads ~/.agentcore/hooks/). */
    val showHookManager: Boolean = false,
    /** Advanced Prompt Library panel with pre-defined system prompts. */
    val showPromptLibrary: Boolean = false,
    /** Inline Tool Editor: edit Python tool files from ~/.agentcore/tools/. */
    val showToolEditor: Boolean = false,
    /** Scheduler Panel: create one-shot and cron scheduled tasks. */
    val showScheduler: Boolean = false,
    /** Pinned Context Panel: files always included in agent messages. */
    val showPinnedContext: Boolean = false,
    /** Prometheus Metrics panel (GET /metrics from backend). */
    val showMetrics: Boolean = false,
    /** Local Model Manager: browse/pull Ollama models. */
    val showLocalModels: Boolean = false,
    /** Visual Session Timeline: Gantt-style message timing view. */
    val showTimeline: Boolean = false,
    /** Smart Context Pruning: identify and remove context bloat. */
    val showContextPruning: Boolean = false,
    /** Personality Lab: design and test agent personas. */
    val showPersonalityLab: Boolean = false,
    /** Multi-Agent Heatmap: token usage per agent visualization. */
    val showHeatmap: Boolean = false,
    /** Files pinned for permanent inclusion in agent context. */
    val pinnedContextFiles: List<String> = emptyList()
)

/** Returns a copy with all right-side panel flags set to false. */
fun UiSettings.withAllPanelsClosed(): UiSettings = copy(
    showStats = false, showFiles = false, showSkills = false,
    showLogs = false, showScratchpad = false, showTerminal = false,
    showPluginManager = false, showWorkflowBuilder = false, showCanvas = false,
    showHelp = false, showOrchestrator = false, showBackendHealth = false,
    showArchiveBrowser = false, showHookManager = false, showPromptLibrary = false,
    showToolEditor = false, showScheduler = false, showPinnedContext = false,
    showMetrics = false, showLocalModels = false, showTimeline = false,
    showContextPruning = false, showPersonalityLab = false, showHeatmap = false
)
