// Extension functions on AgentClient for session management operations:
// updateBackend, updateRole, pruneSession, tagSession, listSessionsByTag,
// summarizeContext, scheduleTask, cancelScheduledTask, listScheduledTasks,
// updateConfig, exportSession, importSession, getStats.
package com.agentcore.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

suspend fun AgentClient.updateBackend(backend: String, model: String? = null): Boolean =
    sendCommand(IpcCommand.SetBackend(SetBackendPayload(backend, model))) != null

suspend fun AgentClient.updateRole(role: String): Boolean =
    sendCommand(IpcCommand.SetRole(SetRolePayload(role))) != null

suspend fun AgentClient.getStats(): JsonObject? {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.GetStats())
        }
        if (response.status == HttpStatusCode.OK) {
            val json = Json { ignoreUnknownKeys = true }
            val event = json.decodeFromString<IpcEvent>(response.bodyAsText())
            if (event is IpcEvent.Stats) event.payload else null
        } else null
    } catch (e: Exception) { null }
}

suspend fun AgentClient.pruneSession(sessionId: String): Boolean =
    sendCommand(IpcCommand.PruneSession(PruneSessionPayload(sessionId))) != null

suspend fun AgentClient.tagSession(sessionId: String, tags: List<String>): Boolean =
    sendCommand(IpcCommand.TagSession(TagSessionPayload(sessionId, tags))) != null

suspend fun AgentClient.listSessionsByTag(tags: List<String>): List<SessionInfo> {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.ListSessionsByTag(ListSessionsByTagPayload(tags)))
        }
        if (response.status == HttpStatusCode.OK) {
            val json = Json { ignoreUnknownKeys = true }
            val event = json.decodeFromString<IpcEvent>(response.bodyAsText())
            if (event is IpcEvent.SessionsList) event.payload.sessions else emptyList()
        } else emptyList()
    } catch (e: Exception) { emptyList() }
}

suspend fun AgentClient.summarizeContext(sessionId: String, keepRecent: Int = 6): Boolean =
    sendCommand(IpcCommand.SummarizeContext(SummarizeContextPayload(sessionId, keepRecent))) != null

suspend fun AgentClient.scheduleTask(
    text: String,
    at: String? = null,
    cron: String? = null,
    sessionId: String? = null
): TaskScheduledPayload? {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.ScheduleTask(ScheduleTaskPayload(at, cron, sessionId, text)))
        }
        if (response.status == HttpStatusCode.OK) {
            val json = Json { ignoreUnknownKeys = true }
            val event = json.decodeFromString<IpcEvent>(response.bodyAsText())
            if (event is IpcEvent.TaskScheduled) event.payload else null
        } else null
    } catch (e: Exception) { null }
}

suspend fun AgentClient.cancelScheduledTask(taskId: String): Boolean =
    sendCommand(IpcCommand.CancelScheduledTask(CancelScheduledTaskPayload(taskId))) != null

suspend fun AgentClient.listScheduledTasks(): List<ScheduledTaskInfo> {
    return try {
        val response = client.post(commandUrl) {
            contentType(ContentType.Application.Json)
            setBody(IpcCommand.ListScheduledTasks())
        }
        if (response.status == HttpStatusCode.OK) {
            val json = Json { ignoreUnknownKeys = true }
            val event = json.decodeFromString<IpcEvent>(response.bodyAsText())
            if (event is IpcEvent.ScheduledTasksList) event.payload.tasks else emptyList()
        } else emptyList()
    } catch (e: Exception) { emptyList() }
}

suspend fun AgentClient.updateConfig(key: String, value: JsonElement): Boolean =
    sendCommand(IpcCommand.UpdateConfig(UpdateConfigPayload(key, value))) != null

suspend fun AgentClient.exportSession(sessionId: String, format: String = "json"): Boolean =
    sendCommand(IpcCommand.ExportSession(ExportSessionPayload(sessionId, format))) != null

suspend fun AgentClient.importSession(path: String): Boolean =
    sendCommand(IpcCommand.ImportSession(ImportSessionPayload(path))) != null
