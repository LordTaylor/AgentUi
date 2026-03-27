// Payload data classes for IpcCommand subclasses (client → backend).
// Each payload corresponds to the "payload" field of a specific IpcCommand subclass.
// See: IpcCommand.kt for the sealed class, CoreApp/docs/communication.md section 6.
package com.agentcore.api

import kotlinx.serialization.Serializable

@Serializable
data class ApprovePlanPayload(val plan_id: String, val approved: Boolean)

// B2 fix: payload for human_input_response command (N11 ask_human)
@Serializable
data class HumanInputResponsePayload(val id: String, val answer: String)

@Serializable
data class ListModelsPayload(val backend: String, val base_url: String? = null)

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
    val images: List<String>? = null,
    /** B06: Working directory for tool execution. Null = use agent default. */
    val working_dir: String? = null,
    /** Whether the backend streams tokens incrementally (true) or returns the
     *  full response as a single block (false). Null = backend default (true). */
    val stream: Boolean? = null
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

@Serializable
data class RenameSessionPayload(val session_id: String, val title: String)

@Serializable
data class TagSessionPayload(val session_id: String, val tags: List<String>)

@Serializable
data class ListSessionsByTagPayload(val tags: List<String>)

@Serializable
data class GetToolPayload(val tool_name: String)

@Serializable
data class UpdateConfigPayload(val key: String, val value: kotlinx.serialization.json.JsonElement)

@Serializable
data class SummarizeContextPayload(val session_id: String, val keep_recent: Int = 6)

@Serializable
data class ScheduleTaskPayload(
    val at: String? = null,
    val cron: String? = null,
    val session_id: String? = null,
    val text: String
)

@Serializable
data class CancelScheduledTaskPayload(val task_id: String)

@Serializable
data class PingBackendPayload(val backend: String? = null, val model: String? = null)

@Serializable
data class ExportSessionPayload(val session_id: String, val format: String = "json")

@Serializable
data class ImportSessionPayload(val path: String)

@Serializable
data class TestToolPayload(val tool_name: String)

@Serializable
data class ToolTogglePayload(val name: String)

@Serializable
data class SpawnSubAgentPayload(val task: String, val role: String = "base", val backend: String? = null)

@Serializable
data class WaitSubAgentPayload(val id: String, val timeout_secs: Long = 300)

@Serializable
data class CancelSubAgentPayload(val id: String)

@Serializable
data class ListCheckpointsPayload(val session_id: String)

@Serializable
data class RestoreCheckpointPayload(val session_id: String, val checkpoint_n: Int)

@Serializable
data class CreateToolPayload(val name: String, val template: String = "python")

@Serializable
data class DeleteToolPayload(val tool_name: String)

@Serializable
data class UpdateMemoryPayload(
    val session_id: String,
    val key: String,
    val value: String
)

@Serializable
data class RestartProviderPayload(
    val provider: String,
    val model: String? = null
)

// A10: RunWorkflow command payload.
@Serializable
data class RunWorkflowPayload(
    val steps: List<WorkflowStepDef>,
    val backend: String? = null
)

// A12: KV Store command payloads.
@Serializable
data class ListMemoryPayload(val session_id: String)

@Serializable
data class DeleteMemoryPayload(val session_id: String, val key: String)
