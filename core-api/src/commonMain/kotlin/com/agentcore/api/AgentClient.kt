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
