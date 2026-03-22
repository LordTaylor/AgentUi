package com.agentcore.logic

import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.api.*
import com.agentcore.shared.*
import kotlinx.coroutines.test.runTest
import androidx.compose.runtime.mutableStateListOf
import io.mockk.*
import kotlin.test.*

class IpcHandlerTest {

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun testHandleStatusEvent() {
        var status = ""
        val messages = mutableStateListOf<Message>()
        
        IpcHandler.handleIpcEvent(
            event = IpcEvent.Status(StatusPayload("THINKING")),
            messages = messages,
            onStatusChange = { status = it },
            onStatsUpdate = {},
            onApprovalRequest = {},
            onLogReceived = {},
            onScratchpadUpdate = {},
            onTerminalTraffic = {},
            onIndexingProgress = {},
            onPluginsLoaded = {},
            onWorkflowsUpdate = {},
            onInputTextChange = {},
            onVoiceUpdate = {},
            onContextSuggestions = {},
            onError = {},
            onSessionData = {},
            onHumanInputRequest = {},
            onAgentGroupUpdate = {}
        )
        
        assertEquals("THINKING", status)
    }

    @Test
    fun testHandleMessageStartEvent() {
        var status = ""
        val messages = mutableStateListOf<Message>()
        
        IpcHandler.handleIpcEvent(
            event = IpcEvent.MessageStart(MessageStartPayload("1", "ollama", "base")),
            messages = messages,
            onStatusChange = { status = it },
            onStatsUpdate = {},
            onApprovalRequest = {},
            onLogReceived = {},
            onScratchpadUpdate = {},
            onTerminalTraffic = {},
            onIndexingProgress = {},
            onPluginsLoaded = {},
            onWorkflowsUpdate = {},
            onInputTextChange = {},
            onVoiceUpdate = {},
            onContextSuggestions = {},
            onError = {},
            onSessionData = {},
            onHumanInputRequest = {},
            onAgentGroupUpdate = {}
        )
        
        assertEquals("THINKING", status)
    }

    @Test
    fun testPerformSendMessage() = runTest {
        val messages = mutableStateListOf<Message>()
        var status = ""
        
        val client = mockk<AgentClient>(relaxed = true)
        val stdio = mockk<StdioExecutor>(relaxed = true)
        val unix = mockk<UnixSocketExecutor>(relaxed = true)
        val cli = mockk<CliExecutor>(relaxed = true)

        val commandSlot = slot<IpcCommand>()
        coEvery { client.sendCommand(capture(commandSlot)) } returns null

        println("--- Starting testPerformSendMessage ---")
        IpcHandler.performSendMessage(
            scope = this,
            client = client,
            stdioExecutor = stdio,
            unixSocketExecutor = unix,
            cliExecutor = cli,
            mode = ConnectionMode.IPC,
            text = "Test command",
            attachments = emptyList(),
            sessionId = "session-123",
            messages = messages,
            onClearInput = {},
            onClearAttachments = {},
            onStatusChange = { 
                println("[testPerformSendMessage] Status changed to: $it")
                status = it 
            }
        )

        println("[testPerformSendMessage] Initial assertions...")
        assertEquals(1, messages.size)
        assertEquals("Test command", messages[0].text)
        assertEquals("THINKING", status)
        
        println("[testPerformSendMessage] Waiting for coroutines...")
        testScheduler.advanceUntilIdle()

        println("[testPerformSendMessage] Verifying command...")
        coVerify { client.sendCommand(any()) }
        
        val captured = commandSlot.captured
        println("[testPerformSendMessage] Captured command: $captured")
        assertTrue(captured is IpcCommand.SendMessage)
        val payload = (captured as IpcCommand.SendMessage).payload
        assertEquals("Test command", payload.text)
        assertEquals("session-123", payload.session_id)
        println("--- Finished testPerformSendMessage ---")
    }
}
