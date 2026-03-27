// Extension function on AgentClient for SSE event streaming with reconnect logic.
// observeEvents() emits IpcEvents from the /events endpoint; on disconnect it retries
// up to 3 times with exponential backoff before emitting a terminal Error event.
package com.agentcore.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

fun AgentClient.observeEvents(): Flow<IpcEvent> = flow {
    val maxAttempts = 3
    val backoffDelays = listOf(2_000L, 4_000L, 8_000L)
    var attempt = 0

    while (attempt <= maxAttempts) {
        if (attempt > 0) {
            val waitMs = backoffDelays.getOrElse(attempt - 1) { 8_000L }
            emit(IpcEvent.Status(payload = StatusPayload("RECONNECTING")))
            delay(waitMs)
            emit(IpcEvent.Status(payload = StatusPayload("CONNECTING")))
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
