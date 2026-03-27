// Payload data classes for infrastructure/extended IpcEvent subclasses.
// Covers: Backend/Ping, Scheduling, Config, SessionFork, SubAgent, Checkpoints,
// Skills, Models, Plans, AgentQuery, Workflows, Memory.
// See: IpcEvent.kt for the sealed class, CoreApp/docs/communication.md section 7.
package com.agentcore.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class PingResultPayload(
    val backend: String,
    val latency_ms: Long? = null,
    val available: Boolean,
    val version: String? = null,
    val uptime_secs: Long? = null
)

@Serializable
data class BackendInfo(
    val name: String,
    val is_available: Boolean = true,
    val configured: Boolean = true,
    val model: String? = null,
    val description: String? = null
)

@Serializable
data class BackendsListPayload(val backends: List<BackendInfo>, val active: String? = null)

@Serializable
data class TaskScheduledPayload(val task_id: String, val next_fire: String, val kind: String)

@Serializable
data class ConfigPayload(val config: kotlinx.serialization.json.JsonObject)

@Serializable
data class ScheduledTaskInfo(val id: String, val kind: String, val next_fire: String? = null)

@Serializable
data class ScheduledTasksListPayload(val count: Int, val tasks: List<ScheduledTaskInfo>)

// B7 fix: Rust emits "source_session_id", not "original_session_id"
@Serializable
data class SessionForkedPayload(
    @SerialName("source_session_id") val originalSessionId: String,
    val new_session_id: String,
    val from_message_idx: Int = 0,
    val message_count: Int = 0
)

@Serializable
data class SubAgentDonePayload(
    val agent_id: String,
    val session_id: String,
    val summary: String,
    val success: Boolean
)

// B8 fix payloads — session_export
@Serializable
data class SessionExportPayload(val session_id: String, val format: String, val content: String)

// B9 fix payloads — tool_test_result
@Serializable
data class ToolTestResultPayload(val tool: String, val passed: Int, val failed: Int, val output: String)

// B10 fix payloads — checkpoint_restored / checkpoints_list
@Serializable
data class CheckpointRestoredPayload(val session_id: String, val checkpoint_n: Int, val message_count: Int)

@Serializable
data class CheckpointsListPayload(val session_id: String, val checkpoints: List<Int>)

// B11 fix payloads — subagent_spawned
@Serializable
data class SubAgentSpawnedPayload(val id: String)

// B12 fix payloads — subagent_result
@Serializable
data class SubAgentResultPayload(
    val id: String,
    val response: String = "",
    val success: Boolean,
    val error: String? = null
)

// B13 fix payloads — subagents_list (Rust returns ids: List<String>, not full objects)
@Serializable
data class SubAgentsListPayload(val count: Int, val ids: List<String>)

@Serializable
data class ModelsListPayload(val backend: String, val models: List<String>)

@Serializable
data class SkillListPayload(val count: Int, val skills: List<SkillInfo>)

@Serializable
data class SkillInfo(val id: String, val name: String, val description: String, val path: String)

@Serializable
data class PlanReadyPayload(val plan_id: String, val steps: List<String>)

@Serializable
data class AgentQueryPayload(val query_id: String, val question: String, val role: String)

@Serializable
data class AgentQueryResponsePayload(val query_id: String, val answer: String, val success: Boolean)

// A10: AgentGroup workflow status event payload.
@Serializable
data class AgentWorkflowStatusPayload(
    val group_id: String,
    /// "running" | "recovering" | "complete" | "failed"
    val state: String,
    val step: Int,
    val total_steps: Int,
    val active_agents: List<String> = emptyList()
)

// A12: MemoryList event payload — all facts for one session.
@Serializable
data class MemoryListPayload(
    val session_id: String,
    val facts: Map<String, String> = emptyMap()
)
