package com.agentcore.api

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import io.ktor.http.*
import io.ktor.utils.io.*

class AgentClient(private val serverUrl: String = "http://localhost:7700") {
    private val commandUrl = "$serverUrl/command"
    private val sessionsUrl = "$serverUrl/sessions"
    private val eventsUrl = "$serverUrl/events"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun sendCommand(command: IpcCommand): IpcEvent? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(command)
            }
            if (response.status == HttpStatusCode.OK) {
                val jsonBody = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val element = json.parseToJsonElement(jsonBody)
                if (element is JsonObject && element["event"]?.jsonPrimitive?.content == "message_end") {
                    return json.decodeFromJsonElement<IpcEvent>(element)
                }
                json.decodeFromString<IpcEvent>(jsonBody)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun ping(): PingResultPayload? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.Ping())
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.PingResult) event.payload else null
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun cancel(sessionId: String): Boolean =
        sendCommand(IpcCommand.Cancel(CancelPayload(sessionId))) != null

    suspend fun deleteSession(sessionId: String): Boolean =
        sendCommand(IpcCommand.DeleteSession(DeleteSessionPayload(sessionId))) != null

    suspend fun listSessions(): List<SessionInfo> {
        return try {
            val response: HttpResponse = client.get(sessionsUrl)
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent.SessionsList>(body)
                event.payload.sessions
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSession(sessionId: String): SessionDataPayload? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.GetSession(GetSessionPayload(sessionId)))
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.SessionData) event.payload else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateBackend(backend: String, model: String? = null): Boolean {
        return sendCommand(IpcCommand.SetBackend(SetBackendPayload(backend, model))) != null
    }

    suspend fun updateRole(role: String): Boolean {
        return sendCommand(IpcCommand.SetRole(SetRolePayload(role))) != null
    }

    suspend fun getStats(): JsonObject? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.GetStats())
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.Stats) event.payload else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun listTools(): List<JsonObject> {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.ListTools())
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.ToolList) event.payload.tools else emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun listBackends(): List<BackendInfo> {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.ListBackends())
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.BackendsList) event.payload.backends else emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun pruneSession(sessionId: String): Boolean {
        return sendCommand(IpcCommand.PruneSession(PruneSessionPayload(sessionId))) != null
    }

    suspend fun tagSession(sessionId: String, tags: List<String>): Boolean {
        return sendCommand(IpcCommand.TagSession(TagSessionPayload(sessionId, tags))) != null
    }

    suspend fun listSessionsByTag(tags: List<String>): List<SessionInfo> {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.ListSessionsByTag(ListSessionsByTagPayload(tags)))
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.SessionsList) event.payload.sessions else emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun summarizeContext(sessionId: String, keepRecent: Int = 6): Boolean {
        return sendCommand(IpcCommand.SummarizeContext(SummarizeContextPayload(sessionId, keepRecent))) != null
    }

    suspend fun scheduleTask(text: String, at: String? = null, cron: String? = null, sessionId: String? = null): TaskScheduledPayload? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.ScheduleTask(ScheduleTaskPayload(at, cron, sessionId, text)))
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.TaskScheduled) event.payload else null
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun cancelScheduledTask(taskId: String): Boolean {
        return sendCommand(IpcCommand.CancelScheduledTask(CancelScheduledTaskPayload(taskId))) != null
    }

    suspend fun listScheduledTasks(): List<ScheduledTaskInfo> {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.ListScheduledTasks())
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.ScheduledTasksList) event.payload.tasks else emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun updateConfig(key: String, value: JsonElement): Boolean {
        return sendCommand(IpcCommand.UpdateConfig(UpdateConfigPayload(key, value))) != null
    }

    suspend fun getTool(toolName: String): JsonObject? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.GetTool(GetToolPayload(toolName)))
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.ToolList) event.payload.tools.firstOrNull() else null
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun exportSession(sessionId: String, format: String = "json"): Boolean {
        return sendCommand(IpcCommand.ExportSession(ExportSessionPayload(sessionId, format))) != null
    }

    suspend fun importSession(path: String): Boolean {
        return sendCommand(IpcCommand.ImportSession(ImportSessionPayload(path))) != null
    }

    suspend fun testTool(toolName: String): Boolean {
        return sendCommand(IpcCommand.TestTool(TestToolPayload(toolName))) != null
    }

    suspend fun spawnSubAgent(task: String, role: String = "base", backend: String? = null): String? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.SpawnSubAgent(SpawnSubAgentPayload(task, role, backend)))
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val element = json.parseToJsonElement(body)
                if (element is JsonObject && element["event"]?.jsonPrimitive?.content == "status") {
                    element["payload"]?.jsonObject?.get("subagent_id")?.jsonPrimitive?.content
                } else null
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun waitSubAgent(id: String, timeoutSecs: Long = 300): JsonObject? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.WaitSubAgent(WaitSubAgentPayload(id, timeoutSecs)))
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val element = json.parseToJsonElement(body)
                if (element is JsonObject && element["event"]?.jsonPrimitive?.content == "status") {
                    element["payload"]?.jsonObject
                } else null
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun cancelSubAgent(id: String): Boolean {
        return sendCommand(IpcCommand.CancelSubAgent(CancelSubAgentPayload(id))) != null
    }

    suspend fun listSubAgents(): List<JsonObject> {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.ListSubAgents())
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val element = json.parseToJsonElement(body)
                if (element is JsonObject && element["event"]?.jsonPrimitive?.content == "status") {
                    element["payload"]?.jsonObject?.get("subagents")?.jsonArray?.map { it.jsonObject } ?: emptyList()
                } else emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun listCheckpoints(sessionId: String): List<Int> {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.ListCheckpoints(ListCheckpointsPayload(sessionId)))
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val element = json.parseToJsonElement(body)
                // Assuming status event with checkpoints array
                element.jsonObject["payload"]?.jsonObject?.get("checkpoints")?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun restoreCheckpoint(sessionId: String, checkpointN: Int): Boolean {
        return sendCommand(IpcCommand.RestoreCheckpoint(RestoreCheckpointPayload(sessionId, checkpointN))) != null
    }

    suspend fun pingBackend(backend: String? = null, model: String? = null): PingResultPayload? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(IpcCommand.PingBackend(PingBackendPayload(backend, model)))
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val event = json.decodeFromString<IpcEvent>(body)
                if (event is IpcEvent.PingResult) event.payload else null
            } else null
        } catch (e: Exception) { null }
    }

    fun observeEvents(): Flow<IpcEvent> = flow {
        val maxAttempts = 3
        val backoffDelays = listOf(2_000L, 4_000L, 8_000L)
        var attempt = 0

        while (attempt <= maxAttempts) {
            if (attempt > 0) {
                val waitMs = backoffDelays.getOrElse(attempt - 1) { 8_000L }
                emit(IpcEvent.Status(StatusPayload("RECONNECTING")))
                delay(waitMs)
                emit(IpcEvent.Status(StatusPayload("CONNECTING")))
            }

            try {
                client.prepareGet(eventsUrl).execute { response ->
                    attempt = 0  // reset counter on successful connection
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val data = line.substring(6)
                            try {
                                val event = Json { ignoreUnknownKeys = true }.decodeFromString<IpcEvent>(data)
                                emit(event)
                            } catch (e: Exception) {
                                println("Failed to parse event: $data")
                            }
                        }
                    }
                }
                attempt++
            } catch (e: Exception) {
                attempt++
                println("SSE connection lost (attempt $attempt/${maxAttempts}): ${e.message}")
                if (attempt > maxAttempts) {
                    emit(
                        IpcEvent.Error(
                            payload = ErrorPayload(
                                code = "connection_failed",
                                message = "Utracono połączenie z agentem po $maxAttempts próbach reconnect."
                            )
                        )
                    )
                }
            }
        }
    }
}
