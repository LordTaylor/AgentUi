package com.agentcore.api

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
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
                
                // If it's a message_complete wrapper (new in v1.2 HTTP)
                if (element is JsonObject && element["event"]?.jsonPrimitive?.content == "message_complete") {
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

    suspend fun listSessions(): List<String> {
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
        try {
            client.prepareGet(eventsUrl).execute { response ->
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
        } catch (e: Exception) {
            println("Connection to events stream failed: ${e.message}")
            emit(IpcEvent.Error(payload = ErrorPayload("connection_failed", "Cannot connect to agent-core. Is it running?")))
        }
    }
}
