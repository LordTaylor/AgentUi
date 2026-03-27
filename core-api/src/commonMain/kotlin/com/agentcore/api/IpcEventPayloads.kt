// Payload data classes for core IpcEvent subclasses (message flow, tools, sessions).
// Covers: MessageStart/End, TextDelta, Status, ToolCall/Result, SessionsList, SessionData,
// Log, Scratchpad, TerminalTraffic, Indexing, Plugin, Workflow, Voice, Canvas, AgentGroup.
// See: IpcEvent.kt for the sealed class, CoreApp/docs/communication.md section 7.
package com.agentcore.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class MessageStartPayload(
    val session_id: String,
    val message_id: String,
    val protocol_version: String
)

@Serializable
data class TextDeltaPayload(val text: String)

@Serializable
data class MessageEndPayload(
    val finish_reason: String = "stop",
    val usage: UsagePayload? = null
)

@Serializable
data class UsagePayload(val input_tokens: Int = 0, val output_tokens: Int = 0)

@Serializable
data class StatusPayload(
    val state: String,
    val role: String? = null,
    val backend: String? = null,
    val updated_key: String? = null,
    val value: kotlinx.serialization.json.JsonElement? = null
)

@Serializable
data class ApprovalRequestPayload(val id: String, val tool: String, val args: JsonObject)

@Serializable
data class ErrorPayload(val code: String, val message: String)

@Serializable
data class ToolCallPayload(val id: String, val tool: String, val args: JsonElement)

@Serializable
data class ToolResultPayload(
    val id: String,
    val result: String,
    val error: String? = null,
    /** Sprint C: semantic error bucket — null for successes. */
    val error_category: String? = null
)

@Serializable
data class SessionsListPayload(val count: Int, val sessions: List<SessionInfo>)

@Serializable
data class SessionDataPayload(
    val session_id: String,
    val message_count: Int,
    val role: String,
    val backend: String = "unknown",
    val system_prompt: String? = null,
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
data class TerminalTrafficPayload(val direction: String, val data: String, val timestamp: String)

@Serializable
data class IndexingProgressPayload(
    val status: String, // IDLE, INDEXING, COMPLETED
    val progress: Float,
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
data class VoiceTranscriptionPayload(val text: String, val isFinal: Boolean)

@Serializable
data class VoiceStatusPayload(val isRecording: Boolean, val isPlaying: Boolean, val level: Float = 0f)

@Serializable
data class CanvasUpdatePayload(val elements: List<CanvasElement>)

@Serializable
data class AgentGroupPayload(val leader: AgentMetadata, val workers: List<AgentMetadata>)

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
data class ContextSuggestionsPayload(val suggestions: List<ContextItem>)

@Serializable
data class ContextItem(val path: String, val reason: String, val type: String, val score: Float = 1.0f)

// B5 fix: Rust ThoughtPayload has { text: String, step: usize }
@Serializable
data class ThoughtPayload(val text: String, val step: Int = 0)

// B3 fix: field names must match Rust ToolProgressPayload { tool: String, message: String }
@Serializable
data class ToolProgressPayload(val tool: String, val message: String)

@Serializable
data class ToolCreatedPayload(val name: String, val path: String)

/** B02: Payload for I01 streaming tool output (one subprocess stdout line). */
@Serializable
data class ToolOutputDeltaPayload(
    val id: String,   // tool call id (matches ToolCallPayload.id)
    val tool: String, // tool name for display
    val line: String  // one line of stdout from the subprocess
)

// B1 fix: field names must match Rust HumanInputRequestPayload { id, question, context }
@Serializable
data class HumanInputPayload(val id: String, val question: String, val context: String = "")
