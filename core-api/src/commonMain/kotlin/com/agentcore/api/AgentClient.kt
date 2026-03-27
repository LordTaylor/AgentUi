// Core AgentClient class: HTTP client setup, sendCommand(), parseCommandResponse(),
// ping(), cancel(), sendHumanInputResponse(), deleteSession(), listSessions(), getSession().
// Extension functions for sessions, tools and streaming live in AgentClient*.kt sibling files.
package com.agentcore.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import io.ktor.http.*

/**
 * @param httpEngine Optional engine override for unit testing (e.g. MockEngine).
 *                   If null, the platform default engine is used (CIO on JVM).
 */
class AgentClient(
    private val serverUrl: String = "http://localhost:7700",
    httpEngine: HttpClientEngine? = null
) {
    internal val commandUrl = "$serverUrl/command"
    internal val sessionsUrl = "$serverUrl/sessions"
    internal val eventsUrl = "$serverUrl/events"

    internal val client: HttpClient = if (httpEngine != null) {
        HttpClient(httpEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        }
    } else {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
            }
        }
    }

    suspend fun sendCommand(command: IpcCommand): IpcEvent? {
        return try {
            val response: HttpResponse = client.post(commandUrl) {
                contentType(ContentType.Application.Json)
                setBody(command)
            }
            if (response.status == HttpStatusCode.OK) {
                parseCommandResponse(response.bodyAsText())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    internal fun parseCommandResponse(body: String): IpcEvent? {
        val j = Json { ignoreUnknownKeys = true }
        return try {
            val element = j.parseToJsonElement(body)
            if (element is JsonObject && element.containsKey("events")) {
                // Backend format: {"events": [{...}, {...}]}
                val arr = element["events"]!!.jsonArray
                arr.lastOrNull()?.let { j.decodeFromJsonElement<IpcEvent>(it) }
            } else {
                // Fallback: single event object (legacy / stdio path)
                j.decodeFromJsonElement<IpcEvent>(element)
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

    // B2 fix: send human answer back to agent (N11 ask_human flow)
    suspend fun sendHumanInputResponse(requestId: String, answer: String): Boolean =
        sendCommand(IpcCommand.HumanInputResponse(HumanInputResponsePayload(requestId, answer))) != null

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

    fun close() = client.close()
}
