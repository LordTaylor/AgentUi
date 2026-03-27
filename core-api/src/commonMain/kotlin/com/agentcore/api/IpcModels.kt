// Shared payload and data classes used by both IpcCommand and IpcEvent (or standalone).
// Contains: SessionInfo (referenced by SessionsListPayload), CanvasElement (used by both
// UpdateCanvas command and CanvasUpdatePayload event), WorkflowStepDef, WorkflowTaskDef.
// See: IpcCommand.kt for command sealed class, IpcEvent.kt for event sealed class.
package com.agentcore.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SessionInfo(
    val id: String,
    val backend: String = "unknown",
    val role: String = "base",
    val message_count: Int = 0,
    val tags: List<String> = emptyList(),
    val title: String? = null,
    val created_at: String = "",
    val updated_at: String = ""
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
sealed class WorkflowStepDef {
    @Serializable @SerialName("sequential")
    data class Sequential(val tasks: List<WorkflowTaskDef>) : WorkflowStepDef()
    @Serializable @SerialName("parallel")
    data class Parallel(val tasks: List<WorkflowTaskDef>) : WorkflowStepDef()
    @Serializable @SerialName("conditional")
    data class Conditional(val primary: WorkflowTaskDef, val fallback: WorkflowTaskDef? = null) : WorkflowStepDef()
}

@Serializable
data class WorkflowTaskDef(
    val task: String,
    val role: String = "base",
    val backend: String? = null,
    val inherit_context: Boolean = false,
    /** Sprint B: optional max input-token budget for this sub-agent. */
    val token_budget: Int? = null
)
