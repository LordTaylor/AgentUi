// Sealed class IpcEvent and all its subclasses (backend → client events).
// Each subclass maps to an "event" discriminator value per the IPC protocol v1.6.
// Payload data classes for events live in IpcEventPayloads.kt.
package com.agentcore.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("event")
sealed class IpcEvent {
    @Serializable @SerialName("message_start")
    data class MessageStart(val payload: MessageStartPayload) : IpcEvent()

    @Serializable @SerialName("text_delta")
    data class TextDelta(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: TextDeltaPayload
    ) : IpcEvent()

    @Serializable @SerialName("message_end")
    data class MessageEnd(val payload: MessageEndPayload) : IpcEvent()

    @Serializable @SerialName("stats")
    data class Stats(val payload: JsonObject) : IpcEvent()

    @Serializable @SerialName("status")
    data class Status(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: StatusPayload
    ) : IpcEvent()

    @Serializable @SerialName("approval_request")
    data class ApprovalRequest(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: ApprovalRequestPayload
    ) : IpcEvent()

    @Serializable @SerialName("error")
    data class Error(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: ErrorPayload
    ) : IpcEvent()

    @Serializable @SerialName("tool_call")
    data class ToolCall(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: ToolCallPayload
    ) : IpcEvent()

    @Serializable @SerialName("tool_result")
    data class ToolResult(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: ToolResultPayload
    ) : IpcEvent()

    @Serializable @SerialName("sessions_list")
    data class SessionsList(val payload: SessionsListPayload) : IpcEvent()

    @Serializable @SerialName("session_data")
    data class SessionData(val payload: SessionDataPayload) : IpcEvent()

    @Serializable @SerialName("tools_list")
    data class ToolList(val payload: ToolListPayload) : IpcEvent()

    @Serializable @SerialName("log")
    data class Log(val payload: LogPayload) : IpcEvent()

    @Serializable @SerialName("scratchpad_data")
    data class Scratchpad(val payload: ScratchpadPayload) : IpcEvent()

    @Serializable @SerialName("terminal_traffic")
    data class TerminalTraffic(val payload: TerminalTrafficPayload) : IpcEvent()

    @Serializable @SerialName("indexing_progress")
    data class IndexingProgress(val payload: IndexingProgressPayload) : IpcEvent()

    @Serializable @SerialName("plugin_metadata")
    data class PluginMetadata(val payload: List<PluginMetadataPayload>) : IpcEvent()

    @Serializable @SerialName("workflow_status")
    data class WorkflowStatus(val payload: List<WorkflowStatusPayload>) : IpcEvent()

    @Serializable @SerialName("voice_transcription")
    data class VoiceTranscription(val payload: VoiceTranscriptionPayload) : IpcEvent()

    @Serializable @SerialName("voice_status")
    data class VoiceStatus(val payload: VoiceStatusPayload) : IpcEvent()

    @Serializable @SerialName("canvas_update")
    data class CanvasUpdate(val payload: CanvasUpdatePayload) : IpcEvent()

    @Serializable @SerialName("agent_group_update")
    data class AgentGroupUpdate(val payload: AgentGroupPayload) : IpcEvent()

    @Serializable @SerialName("context_suggestions")
    data class ContextSuggestions(val payload: ContextSuggestionsPayload) : IpcEvent()

    @Serializable @SerialName("thought")
    data class Thought(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: ThoughtPayload
    ) : IpcEvent()

    @Serializable @SerialName("tool_progress")
    data class ToolProgress(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: ToolProgressPayload
    ) : IpcEvent()

    @Serializable @SerialName("tool_created")
    data class ToolCreated(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: ToolCreatedPayload
    ) : IpcEvent()

    @Serializable @SerialName("human_input_request")
    data class HumanInputRequest(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: HumanInputPayload
    ) : IpcEvent()

    @Serializable @SerialName("ping_result")
    data class PingResult(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: PingResultPayload
    ) : IpcEvent()

    @Serializable @SerialName("backends_list")
    data class BackendsList(val payload: BackendsListPayload) : IpcEvent()

    @Serializable @SerialName("task_scheduled")
    data class TaskScheduled(val payload: TaskScheduledPayload) : IpcEvent()

    @Serializable @SerialName("config")
    data class Config(val payload: ConfigPayload) : IpcEvent()

    @Serializable @SerialName("scheduled_tasks_list")
    data class ScheduledTasksList(val payload: ScheduledTasksListPayload) : IpcEvent()

    @Serializable @SerialName("session_forked")
    data class SessionForked(val payload: SessionForkedPayload) : IpcEvent()

    // B8 fix: session_export event was missing — exportSession() always returned false
    @Serializable @SerialName("session_export")
    data class SessionExport(val payload: SessionExportPayload) : IpcEvent()

    // B9 fix: tool_test_result event was missing — testTool() always returned false
    @Serializable @SerialName("tool_test_result")
    data class ToolTestResult(val payload: ToolTestResultPayload) : IpcEvent()

    // B10 fix: checkpoint_restored event was missing — restoreCheckpoint() always returned false
    @Serializable @SerialName("checkpoint_restored")
    data class CheckpointRestored(val payload: CheckpointRestoredPayload) : IpcEvent()

    // B10 (list): checkpoints_list event for listCheckpoints()
    @Serializable @SerialName("checkpoints_list")
    data class CheckpointsList(val payload: CheckpointsListPayload) : IpcEvent()

    // B11 fix: subagent_spawned event was missing — spawnSubAgent() always returned null
    @Serializable @SerialName("subagent_spawned")
    data class SubAgentSpawned(val payload: SubAgentSpawnedPayload) : IpcEvent()

    // B12 fix: subagent_result event was missing — waitSubAgent() always returned null
    @Serializable @SerialName("subagent_result")
    data class SubAgentResult(val payload: SubAgentResultPayload) : IpcEvent()

    // B13 fix: subagents_list event was missing — listSubAgents() always returned empty
    @Serializable @SerialName("subagents_list")
    data class SubAgentsList(val payload: SubAgentsListPayload) : IpcEvent()

    @Serializable @SerialName("ready")
    data class Ready(
        @SerialName("protocol_version") val protocolVersion: String,
        val transport: String
    ) : IpcEvent()

    // NOTE: approve_plan is an IpcCommand (client→backend), NOT an IpcEvent.
    // Removed dead IpcEvent.ApprovePlan variant — it was never emitted by backend.

    @Serializable @SerialName("models_list")
    data class ModelsList(val payload: ModelsListPayload) : IpcEvent()

    /** B02: I01 streaming subprocess output — one line per event while tool runs. */
    @Serializable @SerialName("tool_output_delta")
    data class ToolOutputDelta(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: ToolOutputDeltaPayload
    ) : IpcEvent()

    @Serializable @SerialName("sub_agent_done")
    data class SubAgentDone(val payload: SubAgentDonePayload) : IpcEvent()

    @Serializable @SerialName("plan_ready")
    data class PlanReady(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: PlanReadyPayload
    ) : IpcEvent()

    @Serializable @SerialName("agent_query")
    data class AgentQuery(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: AgentQueryPayload
    ) : IpcEvent()

    @Serializable @SerialName("agent_query_response")
    data class AgentQueryResponse(
        @SerialName("agent_id") val agentId: String? = null,
        val payload: AgentQueryResponsePayload
    ) : IpcEvent()

    @Serializable @SerialName("skills_list")
    data class SkillsList(val payload: SkillListPayload) : IpcEvent()

    // A10: AgentGroup workflow progress event.
    @Serializable @SerialName("agent_workflow_status")
    data class AgentWorkflowStatus(val payload: AgentWorkflowStatusPayload) : IpcEvent()

    // A12: Response to list_memory / delete_memory — full facts map for a session.
    @Serializable @SerialName("memory_list")
    data class MemoryList(val payload: MemoryListPayload) : IpcEvent()
}
