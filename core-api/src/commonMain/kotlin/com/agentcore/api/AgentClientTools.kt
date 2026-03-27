// Extension functions on AgentClient for tool management, backend listing,
// sub-agent lifecycle (spawn/wait/cancel/list), and checkpoint operations.
// See: CoreApp/docs/communication.md for IPC command/event details.
package com.agentcore.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

suspend fun AgentClient.listTools(): List<JsonObject> {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.ListTools())
        }
        if (response.status == HttpStatusCode.OK) {
            val json = Json { ignoreUnknownKeys = true }
            val event = json.decodeFromString<IpcEvent>(response.bodyAsText())
            if (event is IpcEvent.ToolList) event.payload.tools else emptyList()
        } else emptyList()
    } catch (e: Exception) { emptyList() }
}

suspend fun AgentClient.listBackends(): List<BackendInfo> {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.ListBackends())
        }
        if (response.status == HttpStatusCode.OK) {
            val json = Json { ignoreUnknownKeys = true }
            val event = json.decodeFromString<IpcEvent>(response.bodyAsText())
            if (event is IpcEvent.BackendsList) event.payload.backends else emptyList()
        } else emptyList()
    } catch (e: Exception) { emptyList() }
}

suspend fun AgentClient.getTool(toolName: String): JsonObject? {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.GetTool(GetToolPayload(toolName)))
        }
        if (response.status == HttpStatusCode.OK) {
            val json = Json { ignoreUnknownKeys = true }
            val event = json.decodeFromString<IpcEvent>(response.bodyAsText())
            if (event is IpcEvent.ToolList) event.payload.tools.firstOrNull() else null
        } else null
    } catch (e: Exception) { null }
}

suspend fun AgentClient.testTool(toolName: String): Boolean =
    sendCommand(IpcCommand.TestTool(TestToolPayload(toolName))) != null

suspend fun AgentClient.createTool(name: String, template: String = "python"): Boolean =
    sendCommand(IpcCommand.CreateTool(CreateToolPayload(name, template))) != null

suspend fun AgentClient.deleteTool(toolName: String): Boolean =
    sendCommand(IpcCommand.DeleteTool(DeleteToolPayload(toolName))) != null

suspend fun AgentClient.enableTool(name: String): Boolean =
    sendCommand(IpcCommand.EnableTool(ToolTogglePayload(name))) != null

suspend fun AgentClient.disableTool(name: String): Boolean =
    sendCommand(IpcCommand.DisableTool(ToolTogglePayload(name))) != null

suspend fun AgentClient.spawnSubAgent(
    task: String,
    role: String = "base",
    backend: String? = null
): String? {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.SpawnSubAgent(SpawnSubAgentPayload(task, role, backend)))
        }
        if (response.status == HttpStatusCode.OK) {
            // B11 fix: Rust emits {"event":"subagent_spawned","payload":{"id":...}}
            val event = parseCommandResponse(response.bodyAsText())
            if (event is IpcEvent.SubAgentSpawned) event.payload.id else null
        } else null
    } catch (e: Exception) { null }
}

suspend fun AgentClient.waitSubAgent(id: String, timeoutSecs: Long = 300): JsonObject? {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.WaitSubAgent(WaitSubAgentPayload(id, timeoutSecs)))
        }
        if (response.status == HttpStatusCode.OK) {
            // B12 fix: Rust emits {"event":"subagent_result","payload":{"id","response","success","error"}}
            val event = parseCommandResponse(response.bodyAsText())
            if (event is IpcEvent.SubAgentResult) {
                Json.encodeToJsonElement(SubAgentResultPayload.serializer(), event.payload).jsonObject
            } else null
        } else null
    } catch (e: Exception) { null }
}

suspend fun AgentClient.cancelSubAgent(id: String): Boolean =
    sendCommand(IpcCommand.CancelSubAgent(CancelSubAgentPayload(id))) != null

suspend fun AgentClient.listSubAgents(): List<JsonObject> {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.ListSubAgents())
        }
        if (response.status == HttpStatusCode.OK) {
            // B13 fix: Rust emits {"event":"subagents_list","payload":{"count":...,"ids":[...]}}
            val event = parseCommandResponse(response.bodyAsText())
            if (event is IpcEvent.SubAgentsList) {
                event.payload.ids.map { id -> buildJsonObject { put("id", id) } }
            } else emptyList()
        } else emptyList()
    } catch (e: Exception) { emptyList() }
}

suspend fun AgentClient.listCheckpoints(sessionId: String): List<Int> {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.ListCheckpoints(ListCheckpointsPayload(sessionId)))
        }
        if (response.status == HttpStatusCode.OK) {
            // Fix: response is wrapped in {"events":[...]} by HTTP server
            val event = parseCommandResponse(response.bodyAsText())
            if (event is IpcEvent.CheckpointsList) event.payload.checkpoints else emptyList()
        } else emptyList()
    } catch (e: Exception) { emptyList() }
}

suspend fun AgentClient.restoreCheckpoint(sessionId: String, checkpointN: Int): Boolean =
    sendCommand(IpcCommand.RestoreCheckpoint(RestoreCheckpointPayload(sessionId, checkpointN))) != null

suspend fun AgentClient.pingBackend(backend: String? = null, model: String? = null): PingResultPayload? {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.PingBackend(PingBackendPayload(backend, model)))
        }
        if (response.status == HttpStatusCode.OK) {
            val json = Json { ignoreUnknownKeys = true }
            val event = json.decodeFromString<IpcEvent>(response.bodyAsText())
            if (event is IpcEvent.PingResult) event.payload else null
        } else null
    } catch (e: Exception) { null }
}
