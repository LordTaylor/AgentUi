package com.agentcore.ui.chat

import com.agentcore.api.*
import com.agentcore.model.Message
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val sessions: List<SessionInfo> = emptyList(),
    val availableTools: List<JsonObject> = emptyList(),
    val availableSkills: List<SkillInfo> = emptyList(),
    val availableBackends: List<BackendInfo> = emptyList(),
    val currentSessionId: String? = null,
    val historySearchText: String = "",
    val statusState: String = "IDLE",
    val currentBackend: String = "lmstudio",
    val currentRole: String = "base",
    val showSettings: Boolean = false,
    val sessionStats: JsonObject? = null,
    val pendingApproval: ApprovalRequestPayload? = null,
    val pendingHumanInput: HumanInputPayload? = null,
    val logs: List<LogPayload> = emptyList(),
    val terminalTraffic: List<TerminalTrafficPayload> = emptyList(),
    val scratchpadContent: String = "",
    val plugins: List<PluginMetadataPayload> = emptyList(),
    val workflows: List<WorkflowStatusPayload> = emptyList(),
    val agentGroup: AgentGroupPayload? = null,
    val suggestedContext: List<ContextItem> = emptyList(),
    val canvasElements: List<CanvasElement> = emptyList(),
    val activeFilters: List<String> = emptyList(),
    val currentSystemPrompt: String = "",
    val isSummarizing: Boolean = false,
    val uiSettings: UiSettings = UiSettings(),
    val ipcLogs: List<String> = emptyList(),
    val ipcLogExpanded: Boolean = false,
    val workingDir: String = System.getProperty("user.home") ?: "",
    val showProviderDialog: Boolean = false,
    val availableModels: Map<String, List<String>> = emptyMap(),
    val approvalMode: Boolean = true,
    val currentModelName: String = "",
    val messageSearchQuery: String = "",
    val showSearch: Boolean = false,
    val pendingPlan: com.agentcore.api.PlanReadyPayload? = null,
    val inputText: String = "",
    val messageHistory: List<String> = emptyList(),
    val historyIndex: Int? = null,
    val draftMessage: String = "",
    val sessionFolders: Map<String, String> = emptyMap(), // sessionId -> folderName
    val toolOutput: List<String> = emptyList(),
    val showToolOutput: Boolean = false,
    val tokenHistory: List<UsagePayload> = emptyList(),
    val showTokenAnalytics: Boolean = false,
    val loadingModelName: String? = null,
    val modelLoadingProgress: Float? = null,
    // A10 IPC 1.7: live AgentGroup workflow progress (null when no workflow is running)
    val workflowGroupStatus: AgentWorkflowStatusPayload? = null,
    val showWorkflowDialog: Boolean = false,
    val showCreateToolDialog: Boolean = false,
    // A12: Enhanced KV Store
    val memoryFacts: Map<String, String> = emptyMap(),
    val showMemoryPanel: Boolean = false
)

sealed class ChatIntent {
    data class FetchModels(val backend: String, val url: String? = null) : ChatIntent()
    data class SendMessage(val text: String, val images: List<String> = emptyList()) : ChatIntent()
    data class SelectSession(val id: String) : ChatIntent()
    object ToggleSettings : ChatIntent()
    object RefreshStats : ChatIntent()
    data class ResolveApproval(val approved: Boolean) : ChatIntent()
    data class RespondHumanInput(val answer: String) : ChatIntent()
    data class DeleteSession(val id: String) : ChatIntent()
    data class PruneSession(val id: String) : ChatIntent()
    object ReloadTools : ChatIntent()
    object ReloadSkills : ChatIntent()
    object CancelAction : ChatIntent()
    object ClearChat : ChatIntent()
    data class UpdateSettings(val backend: String, val role: String) : ChatIntent()
    data class UpdateScratchpad(val content: String) : ChatIntent()
    data class TagSession(val id: String, val tags: List<String>) : ChatIntent()
    data class ToggleFilter(val tag: String) : ChatIntent()
    data class SummarizeContext(val sessionId: String) : ChatIntent()
    data class ForkSession(val sessionId: String, val messageIdx: Int) : ChatIntent()
    data class SetSystemPrompt(val prompt: String) : ChatIntent()
    data class UpdateConfig(val key: String, val value: JsonElement) : ChatIntent()
    data class ScheduleTask(val text: String, val at: String? = null, val cron: String? = null) : ChatIntent()
    data class UpdateUiSettings(val settings: UiSettings) : ChatIntent()
    object ToggleIpcLog : ChatIntent()
    data class SetWorkingDir(val path: String) : ChatIntent()
    object NewSession : ChatIntent()
    object DumpDebugLog : ChatIntent()
    object ToggleProviderDialog : ChatIntent()
    /** Activate a backend via set_backend IPC (no restart) */
    data class ActivateProvider(val backend: String, val model: String) : ChatIntent()
    /** Apply URL/API key changes and restart agent-core with new env vars */
    data class ActivateProviderAndRestart(
        val backend: String,
        val envVars: Map<String, String>,
        val updatedConfigs: Map<String, ProviderConfig>
    ) : ChatIntent()
    data class SaveProviderConfigs(val configs: Map<String, ProviderConfig>) : ChatIntent()
    data class SaveNamedProviderConfig(val backend: String, val name: String, val config: ProviderConfig) : ChatIntent()
    data class DeleteNamedProviderConfig(val backend: String, val name: String) : ChatIntent()
    data class LoadNamedProviderConfig(val backend: String, val name: String) : ChatIntent()
    data class LmsLoadModel(val url: String, val model: String, val config: ProviderConfig) : ChatIntent()
    object RestartAgent : ChatIntent()
    data class RestartProvider(val provider: String) : ChatIntent()
    data class CreateTool(val name: String, val template: String) : ChatIntent()
    data class DeleteTool(val name: String) : ChatIntent()
    data class UpdateSearchQuery(val query: String) : ChatIntent()
    data class ResolvePlan(val planId: String, val approved: Boolean) : ChatIntent()
    object RetryMessage : ChatIntent()
    object ToggleSidebar : ChatIntent()
    object NavigateHistoryUp : ChatIntent()
    object NavigateHistoryDown : ChatIntent()
    data class UpdateInputText(val text: String) : ChatIntent()
    data class UpdateHistorySearch(val query: String) : ChatIntent()
    object ExportSession : ChatIntent()
    data class PasteToInput(val text: String) : ChatIntent()
    data class MoveSessionToFolder(val sessionId: String, val folderName: String?) : ChatIntent()
    data class RenameSession(val sessionId: String, val title: String) : ChatIntent()
    object ToggleToolOutput : ChatIntent()
    object ClearToolOutput : ChatIntent()
    object ToggleTokenAnalytics : ChatIntent()
    object ToggleSearch : ChatIntent()
    // A10 IPC 1.7: run a multi-step AgentGroup workflow
    data class RunWorkflow(val payload: RunWorkflowPayload) : ChatIntent()
    object ToggleWorkflowDialog : ChatIntent()
    object ToggleCreateToolDialog : ChatIntent()
    // A12: Enhanced KV Store
    data class LoadMemory(val sessionId: String) : ChatIntent()
    data class DeleteMemoryKey(val sessionId: String, val key: String) : ChatIntent()
    object ToggleMemoryPanel : ChatIntent()
}
