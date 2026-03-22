package com.agentcore.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("cmd")
sealed class IpcCommand {
    @Serializable
    @SerialName("send_message")
    data class SendMessage(
        val payload: SendMessagePayload
    ) : IpcCommand()

    @Serializable
    @SerialName("list_sessions")
    class ListSessions : IpcCommand()

    @Serializable
    @SerialName("list_tools")
    class ListTools : IpcCommand()

    @Serializable
    @SerialName("get_session")
    data class GetSession(val payload: GetSessionPayload) : IpcCommand()

    @Serializable
    @SerialName("set_backend")
    data class SetBackend(val payload: SetBackendPayload) : IpcCommand()

    @Serializable
    @SerialName("approval_response")
    data class ApprovalResponse(val payload: ApprovalResponsePayload) : IpcCommand()

    @Serializable
    @SerialName("set_role")
    data class SetRole(val payload: SetRolePayload) : IpcCommand()

    @Serializable
    @SerialName("get_stats")
    class GetStats : IpcCommand()

    @Serializable
    @SerialName("get_scratchpad")
    class GetScratchpad : IpcCommand()

    @Serializable
    @SerialName("update_scratchpad")
    data class UpdateScratchpad(val payload: UpdateScratchpadPayload) : IpcCommand()

    @Serializable
    @SerialName("start_indexing")
    class StartIndexing : IpcCommand()

    @Serializable
    @SerialName("get_indexing_status")
    class GetIndexingStatus : IpcCommand()

    @Serializable
    @SerialName("list_plugins")
    class ListPlugins : IpcCommand()

    @Serializable
    @SerialName("enable_plugin")
    data class EnablePlugin(val pluginId: String) : IpcCommand()

    @Serializable
    @SerialName("disable_plugin")
    data class DisablePlugin(val pluginId: String) : IpcCommand()

    @Serializable
    @SerialName("list_workflows")
    class ListWorkflows : IpcCommand()

    @Serializable
    @SerialName("start_workflow")
    data class StartWorkflow(val workflowId: String) : IpcCommand()

    @Serializable
    @SerialName("stop_workflow")
    data class StopWorkflow(val workflowId: String) : IpcCommand()

    @Serializable
    @SerialName("start_voice_session")
    class StartVoiceSession : IpcCommand()

    @Serializable
    @SerialName("stop_voice_session")
    class StopVoiceSession : IpcCommand()

    @Serializable
    @SerialName("speak_text")
    data class SpeakText(val text: String) : IpcCommand()

    @Serializable
    @SerialName("get_canvas_state")
    class GetCanvasState : IpcCommand()

    @Serializable
    @SerialName("update_canvas")
    data class UpdateCanvas(val elements: List<CanvasElement>) : IpcCommand()

    @Serializable
    @SerialName("get_agent_groups")
    class GetAgentGroups : IpcCommand()

    @Serializable
    @SerialName("assign_task")
    data class AssignTask(val agentId: String, val task: String) : IpcCommand()

    @Serializable
    @SerialName("get_context_suggestions")
    class GetContextSuggestions : IpcCommand()

    @Serializable
    @SerialName("cancel")
    data class Cancel(val payload: CancelPayload) : IpcCommand()

    @Serializable
    @SerialName("ping")
    class Ping : IpcCommand()

    @Serializable
    @SerialName("list_backends")
    class ListBackends : IpcCommand()

    @Serializable
    @SerialName("delete_session")
    data class DeleteSession(val payload: DeleteSessionPayload) : IpcCommand()

    @Serializable
    @SerialName("prune_session")
    data class PruneSession(val payload: PruneSessionPayload) : IpcCommand()

    @Serializable
    @SerialName("fork_session")
    data class ForkSession(val payload: ForkSessionPayload) : IpcCommand()

    @Serializable
    @SerialName("get_config")
    class GetConfig : IpcCommand()

    @Serializable
    @SerialName("reload_tools")
    class ReloadTools : IpcCommand()

    @Serializable
    @SerialName("set_system_prompt")
    data class SetSystemPrompt(val payload: SetSystemPromptPayload) : IpcCommand()
}

@Serializable
data class SetBackendPayload(val backend: String, val model: String? = null)

@Serializable
data class GetStatsPayload(val dummy: String? = null)

@Serializable
data class ApprovalResponsePayload(val id: String, val approved: Boolean)

@Serializable
data class SetRolePayload(val role: String)

@Serializable
data class GetSessionPayload(val session_id: String)

@Serializable
data class SendMessagePayload(
    val session_id: String? = null,
    val text: String,
    val attachments: List<String>? = null,
    val include_stats: Boolean = false,
    val images: List<String>? = null
)

@Serializable
data class UpdateScratchpadPayload(val content: String)

@Serializable
data class CancelPayload(val session_id: String)

@Serializable
data class DeleteSessionPayload(val session_id: String)

@Serializable
data class PruneSessionPayload(val session_id: String, val keep_last: Int = 20)

@Serializable
data class ForkSessionPayload(val session_id: String, val from_message_idx: Int)

@Serializable
data class SetSystemPromptPayload(val session_id: String, val prompt: String)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("event")
sealed class IpcEvent {
    @Serializable
    @SerialName("message_start")
    data class MessageStart(
        val payload: MessageStartPayload
    ) : IpcEvent()

    @Serializable
    @SerialName("text_delta")
    data class TextDelta(
        val payload: TextDeltaPayload
    ) : IpcEvent()

    @Serializable
    @SerialName("message_end")
    data class MessageEnd(val payload: MessageEndPayload) : IpcEvent()

    @Serializable
    @SerialName("stats")
    data class Stats(val payload: JsonObject) : IpcEvent()

    @Serializable
    @SerialName("status")
    data class Status(
        val payload: StatusPayload
    ) : IpcEvent()

    @Serializable
    @SerialName("approval_request")
    data class ApprovalRequest(val payload: ApprovalRequestPayload) : IpcEvent()

    @Serializable
    @SerialName("error")
    data class Error(
        val payload: ErrorPayload
    ) : IpcEvent()

    @Serializable
    @SerialName("tool_call")
    data class ToolCall(
        val payload: ToolCallPayload
    ) : IpcEvent()

    @Serializable
    @SerialName("tool_result")
    data class ToolResult(
        val payload: ToolResultPayload
    ) : IpcEvent()

    @Serializable
    @SerialName("sessions_list")
    data class SessionsList(val payload: SessionsListPayload) : IpcEvent()

    @Serializable
    @SerialName("session_data")
    data class SessionData(val payload: SessionDataPayload) : IpcEvent()

    @Serializable
    @SerialName("tools_list")
    data class ToolList(val payload: ToolListPayload) : IpcEvent()

    @Serializable
    @SerialName("log")
    data class Log(val payload: LogPayload) : IpcEvent()

    @Serializable
    @SerialName("scratchpad_data")
    data class Scratchpad(val payload: ScratchpadPayload) : IpcEvent()

    @Serializable
    @SerialName("terminal_traffic")
    data class TerminalTraffic(val payload: TerminalTrafficPayload) : IpcEvent()

    @Serializable
    @SerialName("indexing_progress")
    data class IndexingProgress(val payload: IndexingProgressPayload) : IpcEvent()

    @Serializable
    @SerialName("plugin_metadata")
    data class PluginMetadata(val payload: List<PluginMetadataPayload>) : IpcEvent()

    @Serializable
    @SerialName("workflow_status")
    data class WorkflowStatus(val payload: List<WorkflowStatusPayload>) : IpcEvent()

    @Serializable
    @SerialName("voice_transcription")
    data class VoiceTranscription(val payload: VoiceTranscriptionPayload) : IpcEvent()

    @Serializable
    @SerialName("voice_status")
    data class VoiceStatus(val payload: VoiceStatusPayload) : IpcEvent()

    @Serializable
    @SerialName("canvas_update")
    data class CanvasUpdate(val payload: CanvasUpdatePayload) : IpcEvent()

    @Serializable
    @SerialName("agent_group_update")
    data class AgentGroupUpdate(val payload: AgentGroupPayload) : IpcEvent()

    @Serializable
    @SerialName("context_suggestions")
    data class ContextSuggestions(val payload: ContextSuggestionsPayload) : IpcEvent()

    @Serializable
    @SerialName("thought")
    data class Thought(val payload: ThoughtPayload) : IpcEvent()

    @Serializable
    @SerialName("tool_progress")
    data class ToolProgress(val payload: ToolProgressPayload) : IpcEvent()

    @Serializable
    @SerialName("tool_created")
    data class ToolCreated(val payload: ToolCreatedPayload) : IpcEvent()

    @Serializable
    @SerialName("human_input_request")
    data class HumanInputRequest(val payload: HumanInputPayload) : IpcEvent()

    @Serializable
    @SerialName("ping_result")
    data class PingResult(val payload: PingResultPayload) : IpcEvent()

    @Serializable
    @SerialName("backends_list")
    data class BackendsList(val payload: BackendsListPayload) : IpcEvent()

    @Serializable
    @SerialName("task_scheduled")
    data class TaskScheduled(val payload: TaskScheduledPayload) : IpcEvent()

    @Serializable
    @SerialName("config")
    data class Config(val payload: ConfigPayload) : IpcEvent()
}

@Serializable
data class MessageStartPayload(
    val session_id: String,
    val message_id: String,
    val protocol_version: String
)

@Serializable
data class TextDeltaPayload(
    val text: String
)

@Serializable
data class MessageEndPayload(
    val finish_reason: String = "stop",
    val usage: UsagePayload? = null
)

@Serializable
data class UsagePayload(
    val input_tokens: Int = 0,
    val output_tokens: Int = 0
)

@Serializable
data class StatusPayload(
    val state: String
)

@Serializable
data class ApprovalRequestPayload(val id: String, val tool: String, val args: JsonObject)

@Serializable
data class ErrorPayload(
    val code: String,
    val message: String
)

@Serializable
data class ToolCallPayload(
    val id: String,
    val tool: String,
    val args: JsonElement
)

@Serializable
data class ToolResultPayload(
    val id: String,
    val result: String,
    val error: String? = null
)

@Serializable
data class SessionInfo(
    val id: String,
    val backend: String = "unknown",
    val role: String = "base",
    val message_count: Int = 0,
    val tags: List<String> = emptyList(),
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class SessionsListPayload(val count: Int, val sessions: List<SessionInfo>)

@Serializable
data class SessionDataPayload(
    val session_id: String,
    val message_count: Int,
    val role: String,
    val backend: String = "unknown",
    val tags: List<String> = emptyList(),
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class ToolListPayload(val count: Int, val tools: List<JsonObject>)

@Serializable
data class LogPayload(
    val level: String, // INFO, WARN, ERROR, DEBUG
    val message: String,
    val timestamp: String,
    val source: String? = null
)

@Serializable
data class ScratchpadPayload(val content: String)

@Serializable
data class TerminalTrafficPayload(
    val direction: String, // IN, OUT
    val data: String,
    val timestamp: String
)

@Serializable
data class IndexingProgressPayload(
    val status: String, // IDLE, INDEXING, COMPLETED
    val progress: Float, // 0.0 to 1.0
    val totalFiles: Int,
    val indexedFiles: Int,
    val currentFile: String? = null
)

@Serializable
data class PluginMetadataPayload(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val isEnabled: Boolean = false
)

@Serializable
data class WorkflowStatusPayload(
    val id: String,
    val name: String,
    val status: String, // IDLE, RUNNING, COMPLETED, FAILED
    val currentStep: Int,
    val totalSteps: Int,
    val lastError: String? = null
)

@Serializable
data class VoiceTranscriptionPayload(
    val text: String,
    val isFinal: Boolean
)

@Serializable
data class VoiceStatusPayload(
    val isRecording: Boolean,
    val isPlaying: Boolean,
    val level: Float = 0f // 0.0 to 1.0 for visualization
)

@Serializable
data class CanvasUpdatePayload(
    val elements: List<CanvasElement>
)

@Serializable
data class CanvasElement(
    val id: String,
    val type: String, // RECT, CIRCLE, LINE, TEXT
    val x: Float,
    val y: Float,
    val width: Float = 0f,
    val height: Float = 0f,
    val color: String = "#000000",
    val text: String? = null,
    val strokeWidth: Float = 1f
)

@Serializable
data class AgentGroupPayload(
    val leader: AgentMetadata,
    val workers: List<AgentMetadata>
)

@Serializable
data class AgentMetadata(
    val id: String,
    val name: String,
    val role: String, // LEADER, WORKER
    val status: String, // IDLE, BUSY, OFFLINE
    val currentTask: String? = null,
    val cpuUsage: Float = 0f,
    val memoryUsage: Long = 0
)

@Serializable
data class ContextSuggestionsPayload(
    val suggestions: List<ContextItem>
)

@Serializable
data class ContextItem(
    val path: String,
    val reason: String,
    val type: String, // FILE, DOC, WEB
    val score: Float = 1.0f
)

@Serializable
data class ThoughtPayload(val text: String)

@Serializable
data class ToolProgressPayload(val call_id: String, val chunk: String)

@Serializable
data class ToolCreatedPayload(val name: String, val path: String)

@Serializable
data class HumanInputPayload(val prompt: String, val request_id: String)

@Serializable
data class PingResultPayload(val version: String, val uptime_secs: Long)

@Serializable
data class BackendInfo(
    val name: String,
    val is_available: Boolean,
    val model: String? = null
)

@Serializable
data class BackendsListPayload(val backends: List<BackendInfo>)

@Serializable
data class TaskScheduledPayload(
    val task_id: String,
    val next_fire: String,
    val kind: String
)

@Serializable
data class ConfigPayload(val config: kotlinx.serialization.json.JsonObject)
