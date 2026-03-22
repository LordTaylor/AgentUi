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
        val messages = mutableListOf<Message>()
        
        IpcHandler.handleIpcEvent(
            event = IpcEvent.Status(StatusPayload("THINKING")),
            currentMessages = messages,
            onMessageAdded = { messages.add(it) },
            onLastMessageUpdated = { messages[messages.size - 1] = it },
            onStatusChange = { status = it },
            onStatsUpdate = {},
            onApprovalRequest = {},
            onLogReceived = {},
            onScratchpadUpdate = {},
            onTerminalTraffic = {},
            onIndexingProgress = {},
            onPluginsLoaded = {},
            onWorkflowsUpdate = {},
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
        val messages = mutableListOf<Message>()
        
        IpcHandler.handleIpcEvent(
            event = IpcEvent.MessageStart(MessageStartPayload("1", "m1", "1.0")),
            currentMessages = messages,
            onMessageAdded = { messages.add(it) },
            onLastMessageUpdated = { messages[messages.size - 1] = it },
            onStatusChange = { status = it },
            onStatsUpdate = {},
            onApprovalRequest = {},
            onLogReceived = {},
            onScratchpadUpdate = {},
            onTerminalTraffic = {},
            onIndexingProgress = {},
            onPluginsLoaded = {},
            onWorkflowsUpdate = {},
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
            onStatusChange = { status = it }
        )

        assertEquals(1, messages.size)
        assertEquals("Test command", messages[0].text)
        assertEquals("THINKING", status)
        
        testScheduler.advanceUntilIdle()

        coVerify { client.sendCommand(any()) }
        
        val captured = commandSlot.captured
        assertTrue(captured is IpcCommand.SendMessage)
        val payload = (captured as IpcCommand.SendMessage).payload
        assertEquals("Test command", payload.text)
        assertEquals("session-123", payload.session_id)
    }
}
