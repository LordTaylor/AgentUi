package com.agentcore.logic

import com.agentcore.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlinx.serialization.json.*

class BackendIntegrationTest {

    private val client = AgentClient("http://localhost:7700")

    @Test
    fun testSseEventStreaming() = runTest {
        // This test requires the Rust backend to be running at localhost:7700
        // If it's not running, this test will skip or fail depending on environment.
        
        val events = mutableListOf<IpcEvent>()
        val job = launch {
            client.observeEvents()
                .take(3) // Wait for at least 3 events
                .collect {
                    events.add(it)
                }
        }

        // Give it a moment to connect
        delay(1000)

        // Send a ping to trigger an event
        val pingResult = client.ping()
        assertNotNull(pingResult, "Backend should be reachable and respond to ping")

        // Wait for events to be collected
        try {
            withTimeout(5000) {
                job.join()
            }
        } catch (e: Exception) {
            job.cancel()
        }

        // We expect at least the PingResult event if the backend broadcasts it
        // Note: Our current ping handler in config.rs DOES broadcast PingResult.
        assertTrue(events.any { it is IpcEvent.PingResult }, "SSE stream should have received a PingResult event")
    }

    @Test
    fun testSendMessageStreaming() = runTest {
        val events = mutableListOf<IpcEvent>()
        val job = launch {
            client.observeEvents()
                .filter { it !is IpcEvent.Status } // Ignore heartbeat/status
                .collect {
                    events.add(it)
                    if (it is IpcEvent.MessageEnd) {
                        cancel() // Stop observing after message ends
                    }
                }
        }

        delay(1000)

        val command = IpcCommand.SendMessage(
            SendMessagePayload(
                text = "Hello Integration Test",
                session_id = "test-session-${System.currentTimeMillis()}"
            )
        )
        
        val result = client.sendCommand(command)
        assertNotNull(result, "Command should be accepted")

        try {
            withTimeout(15000) {
                job.join()
            }
        } catch (e: Exception) {
            job.cancel()
        }

        // Verify we got the start, hopefully some deltas, and the end
        assertTrue(events.any { it is IpcEvent.MessageStart }, "Should receive MessageStart")
        assertTrue(events.any { it is IpcEvent.MessageEnd }, "Should receive MessageEnd")
    }
}
