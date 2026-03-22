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
    val attachments: List<String>? = null
)

@Serializable
data class UpdateScratchpadPayload(val content: String)

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
    @SerialName("message_complete")
    data class MessageComplete(val payload: MessageCompletePayload) : IpcEvent()

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
data class MessageCompletePayload(val events: List<IpcEvent>)

@Serializable
data class SessionsListPayload(val count: Int, val sessions: List<String>)

@Serializable
data class SessionDataPayload(
    val session_id: String,
    val message_count: Int,
    val role: String
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
data class UpdateScratchpadPayload_Obsolete(val content: String) // Duplicate removed, payload already defined above
