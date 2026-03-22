package com.agentcore

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.*
import com.agentcore.ui.*
import com.agentcore.ui.components.*
import com.agentcore.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.File
import javax.swing.JFileChooser

enum class MessageType {
    TEXT, ACTION, SYSTEM
}

data class Message(
    val id: String,
    val sender: String,
    val text: String,
    val isFromUser: Boolean,
    val type: MessageType = MessageType.TEXT,
    val attachments: List<String>? = null,
    val extraContent: String? = null // For base64 images or tool-specific data
)

@Composable
fun App() {
    var connectionMode by remember { mutableStateOf<ConnectionMode?>(null) }

    MaterialTheme(
        colorScheme = AgentColorScheme,
        typography = AgentTypography
    ) {
        AnimatedContent(targetState = connectionMode, label = "ConnectionScreenTransition") { mode ->
            if (mode == null) {
                ConnectionScreen { connectionMode = it }
            } else {
                ChatMainScreen(mode)
            }
        }
    }
}

@Composable
fun ChatMainScreen(mode: ConnectionMode) {
    val client = remember { AgentClient() }
    val cliExecutor = remember { CliExecutor() }
    val stdioExecutor = remember { StdioExecutor() }
    val unixSocketExecutor = remember { UnixSocketExecutor() }
    val scope = rememberCoroutineScope()
    
    val messages = remember { mutableStateListOf<Message>() }
    val sessions = remember { mutableStateListOf<String>() }
    val pendingAttachments = remember { mutableStateListOf<String>() }
    val availableTools = remember { mutableStateListOf<JsonObject>() }
    
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("") }
    var statusState by remember { mutableStateOf("IDLE") }
    var currentBackend by remember { mutableStateOf("ollama") }
    var currentRole by remember { mutableStateOf("base") }
    
    var showSettings by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showScratchpad by remember { mutableStateOf(false) }
    var sessionStats by remember { mutableStateOf<JsonObject?>(null) }
    var pendingApproval by remember { mutableStateOf<ApprovalRequestPayload?>(null) }
    val logs = remember { mutableStateListOf<LogPayload>() }
    var scratchpadContent by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        when (mode) {
            ConnectionMode.STDIO -> {
                stdioExecutor.start()
                stdioExecutor.events.collectLatest { event ->
                    handleIpcEvent(event, messages, { statusState = it }, { sessionStats = it }, { pendingApproval = it }, { logs.add(it) }, { scratchpadContent = it })
                }
            }
            ConnectionMode.UNIX_SOCKET -> {
                unixSocketExecutor.start(scope)
                unixSocketExecutor.events.collectLatest { event ->
                    handleIpcEvent(event, messages, { statusState = it }, { sessionStats = it }, { pendingApproval = it }, { logs.add(it) }, { scratchpadContent = it })
                }
            }
            ConnectionMode.IPC -> {
                sessions.clear()
                sessions.addAll(client.listSessions())
                
                scope.launch {
                    val tools = client.listTools()
                    availableTools.clear()
                    availableTools.addAll(tools)
                }

                client.observeEvents().collectLatest { event ->
                    handleIpcEvent(event, messages, { statusState = it }, { sessionStats = it }, { pendingApproval = it }, { logs.add(it) }, { scratchpadContent = it })
                }
            }
            else -> {}
        }

        // Mock Logs for Phase 5 Demonstration
        scope.launch {
            val mockMessages = listOf(
                "Initializing AgentCore v1.3...",
                "Connecting to local Ollama instance...",
                "Loading vector database for workspace indexing...",
                "Found 12 source files in project root.",
                "Tool 'bash' loaded with restricted permissions.",
                "Failed to reach remote registry, using local cache.",
                "High memory usage detected. Optimizing context window..."
            )
            val levels = listOf("INFO", "INFO", "DEBUG", "INFO", "WARN", "ERROR", "WARN")
            
            mockMessages.forEachIndexed { i, msg ->
                kotlinx.coroutines.delay(2000L * (i + 1))
                logs.add(LogPayload(
                    level = levels[i],
                    message = msg,
                    timestamp = (System.currentTimeMillis() / 1000).toString(),
                    source = "core-engine"
                ))
            }
        }
    }
    
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                sessions = sessions,
                onSessionSelect = { sessionId ->
                    currentSessionId = sessionId
                    messages.clear()
                    messages.add(Message("sys-${System.currentTimeMillis()}", "System", "Joined session: $sessionId", false, MessageType.SYSTEM))
                },
                modifier = Modifier.width(240.dp).background(MaterialTheme.colorScheme.surface)
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(if (statusState == "IDLE") Color.Green else Color.Yellow, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${mode.name} MODE - ${statusState.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (currentSessionId != null) {
                             Text(" | ${currentSessionId!!.take(8)}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TokenTracker(sessionStats)
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = { 
                            scope.launch { 
                                if (mode == ConnectionMode.IPC) {
                                    sessionStats = client.getStats()
                                    showStats = !showStats
                                }
                            }
                        }) { Icon(Icons.Default.Info, contentDescription = "Stats", tint = if (showStats) MaterialTheme.colorScheme.primary else Color.Gray) }
                        
                        IconButton(onClick = { showTools = !showTools }) { 
                            Icon(Icons.Default.List, contentDescription = "Tools", tint = if (showTools) MaterialTheme.colorScheme.primary else Color.Gray) 
                        }

                        IconButton(onClick = { showLogs = !showLogs }) {
                            Icon(Icons.Default.Info, contentDescription = "Logs", tint = if (showLogs) Color(0xFF4CAF50) else Color.Gray)
                        }

                        IconButton(onClick = { 
                            showScratchpad = !showScratchpad
                            if (showScratchpad && mode == ConnectionMode.IPC) {
                                scope.launch {
                                    client.sendCommand(IpcCommand.GetScratchpad())
                                }
                            }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Scratchpad", tint = if (showScratchpad) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                        
                        IconButton(onClick = { showSettings = true }) { 
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray) 
                        }
                    }
                }

                HorizontalDivider(color = Color.DarkGray)

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        itemsIndexed(messages) { _, msg ->
                            when {
                                msg.type == MessageType.ACTION && msg.text.startsWith("Consulting Agent") -> {
                                    AgentConsultationItem(
                                        agentName = msg.text.substringAfter(": ").ifEmpty { "Default" },
                                        query = "Internal Reasoning",
                                        response = if (msg.extraContent?.isNotEmpty() == true) msg.extraContent else null
                                    )
                                }
                                msg.type == MessageType.ACTION -> ActionLogItem(msg.text)
                                else -> ChatBubble(msg)
                            }
                        }
                    }
                    
                    if (showStats && sessionStats != null) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).width(300.dp).padding(16.dp)) {
                            StatsDashboard(sessionStats!!)
                        }
                    }
                }

                // Input Area
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (pendingAttachments.isNotEmpty()) {
                        LazyRow(modifier = Modifier.padding(bottom = 8.dp)) {
                            items(pendingAttachments.toList()) { path ->
                                AttachmentChip(path) { pendingAttachments.remove(path) }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("How can I help you today?") },
                        shape = RoundedCornerShape(24.dp),
                        leadingIcon = {
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val chooser = JFileChooser()
                                    chooser.isMultiSelectionEnabled = true
                                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                        withContext(Dispatchers.Main) {
                                            chooser.selectedFiles.forEach { pendingAttachments.add(it.absolutePath) }
                                        }
                                    }
                                }
                            }) { Icon(Icons.Default.Add, contentDescription = "Attach File") }
                        },
                        trailingIcon = {
                            Button(
                                onClick = {
                                    if (inputText.isNotBlank() || pendingAttachments.isNotEmpty()) {
                                        performSendMessage(scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode, inputText, pendingAttachments.toList(), currentSessionId, messages, { inputText = "" }, { pendingAttachments.clear() }, { statusState = it })
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) { Text("Send") }
                        }
                    )
                }
            }
            
            if (showTools) {
                Box(modifier = Modifier.width(300.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                    ToolExplorer(availableTools)
                }
            }

            if (showLogs) {
                Box(modifier = Modifier.width(400.dp).fillMaxHeight().background(Color(0xFF0D0D0D))) {
                    LogViewer(logs = logs, onClear = { logs.clear() })
                }
            }

            if (showScratchpad) {
                Box(modifier = Modifier.width(400.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                    Scratchpad(
                        content = scratchpadContent,
                        onSave = { newContent ->
                            scope.launch {
                                val cmd = IpcCommand.UpdateScratchpad(UpdateScratchpadPayload(newContent))
                                when (mode) {
                                    ConnectionMode.IPC -> client.sendCommand(cmd)
                                    ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                                    ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                                    else -> {}
                                }
                                scratchpadContent = newContent
                            }
                        },
                        onRefresh = {
                            scope.launch {
                                when (mode) {
                                    ConnectionMode.IPC -> client.sendCommand(IpcCommand.GetScratchpad())
                                    else -> {} // Direct modes might not support separate refresh
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (pendingApproval != null) {
        ApprovalDialog(pendingApproval!!) { approved ->
            val payload = ApprovalResponsePayload(pendingApproval!!.id, approved)
            val cmd = IpcCommand.ApprovalResponse(payload)
            scope.launch {
                when (mode) {
                    ConnectionMode.IPC -> client.sendCommand(cmd)
                    ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                    ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                    else -> {}
                }
                pendingApproval = null
            }
        }
    }

    if (showSettings) {
        SettingsDialog(currentBackend, currentRole, { showSettings = false }) { b, r ->
            currentBackend = b
            currentRole = r
            showSettings = false
            scope.launch {
                val cmd = IpcCommand.SetBackend(SetBackendPayload(b))
                val roleCmd = IpcCommand.SetRole(SetRolePayload(r))
                when (mode) {
                    ConnectionMode.IPC -> {
                        client.updateBackend(b)
                        client.updateRole(r)
                    }
                    ConnectionMode.STDIO -> {
                        stdioExecutor.sendCommand(cmd)
                        stdioExecutor.sendCommand(roleCmd)
                    }
                    ConnectionMode.UNIX_SOCKET -> {
                        unixSocketExecutor.sendCommand(cmd)
                        unixSocketExecutor.sendCommand(roleCmd)
                    }
                    else -> {}
                }
                messages.add(Message("sys-${System.currentTimeMillis()}", "System", "Updated settings: $b / $r", false, MessageType.SYSTEM))
            }
        }
    }
}

private fun performSendMessage(
    scope: CoroutineScope,
    client: AgentClient,
    stdioExecutor: StdioExecutor,
    unixSocketExecutor: UnixSocketExecutor,
    cliExecutor: CliExecutor,
    mode: ConnectionMode,
    text: String,
    attachments: List<String>,
    currentSessionId: String?,
    messages: MutableList<Message>,
    onClearInput: () -> Unit,
    onClearAttachments: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    messages.add(Message("u-${System.currentTimeMillis()}", "You", text, true, attachments = attachments))
    onClearInput()
    onClearAttachments()
    
    scope.launch {
        when (mode) {
            ConnectionMode.IPC -> {
                val response = client.sendCommand(IpcCommand.SendMessage(SendMessagePayload(
                    session_id = currentSessionId,
                    text = text,
                    attachments = attachments
                )))
                if (response == null) {
                    messages.add(Message("sys-${System.currentTimeMillis()}", "System", "Failed to send message: No connection", false, MessageType.SYSTEM))
                    onUpdateStatus("OFFLINE")
                } else if (response is IpcEvent.MessageComplete) {
                    onUpdateStatus("IDLE")
                }
            }
            ConnectionMode.STDIO -> {
                onUpdateStatus("THINKING")
                stdioExecutor.sendCommand(IpcCommand.SendMessage(SendMessagePayload(
                    session_id = currentSessionId,
                    text = text,
                    attachments = attachments
                )))
            }
            ConnectionMode.UNIX_SOCKET -> {
                onUpdateStatus("THINKING")
                unixSocketExecutor.sendCommand(IpcCommand.SendMessage(SendMessagePayload(
                    session_id = currentSessionId,
                    text = text,
                    attachments = attachments
                )))
            }
            ConnectionMode.CLI -> {
                onUpdateStatus("THINKING")
                val responseText = withContext(Dispatchers.IO) {
                    cliExecutor.executeCommand(text)
                }
                messages.add(Message("a-${System.currentTimeMillis()}", "Agent", responseText, false))
                onUpdateStatus("IDLE")
            }
        }
    }
}

private fun handleIpcEvent(
    event: IpcEvent, 
    messages: MutableList<Message>, 
    updateStatus: (String) -> Unit,
    updateStats: (JsonObject) -> Unit,
    requestApproval: (ApprovalRequestPayload?) -> Unit,
    addLog: (LogPayload) -> Unit,
    updateScratchpad: (String) -> Unit
) {
    when (event) {
        is IpcEvent.MessageStart -> messages.add(Message(event.payload.session_id, "Agent", "", false))
        is IpcEvent.TextDelta -> {
            val idx = messages.indexOfLast { it.sender == "Agent" && it.type == MessageType.TEXT }
            if (idx != -1) messages[idx] = messages[idx].copy(text = messages[idx].text + event.payload.text)
            else messages.add(Message("a-temp-${System.currentTimeMillis()}", "Agent", event.payload.text, false))
        }
        is IpcEvent.ToolCall -> {
            val toolName = event.payload.tool
            val msgText = if (toolName == "ask_agent") "Consulting Agent..." else "Calling tool: $toolName"
            messages.add(Message("tc-${event.payload.id}", "Agent", msgText, false, MessageType.ACTION))
        }
        is IpcEvent.ToolResult -> {
            val result = event.payload.result
            val isImage = result.startsWith("data:image")
            val msgText = if (isImage) "Viewed Image" else "Tool result: ${result.take(50)}..."
            messages.add(Message(
                id = "tr-${event.payload.id}", 
                sender = "Agent", 
                text = msgText, 
                isFromUser = false, 
                type = MessageType.ACTION,
                extraContent = if (isImage) result else null
            ))
        }
        is IpcEvent.Status -> updateStatus(event.payload.state)
        is IpcEvent.Stats -> updateStats(event.payload)
        is IpcEvent.ApprovalRequest -> requestApproval(event.payload)
        is IpcEvent.Error -> {
            updateStatus("OFFLINE")
            messages.add(Message("err-${System.currentTimeMillis()}", "System", "Error: ${event.payload.message}", false, MessageType.SYSTEM))
        }
        is IpcEvent.Log -> addLog(event.payload)
        is IpcEvent.Scratchpad -> updateScratchpad(event.payload.content)
        else -> {}
    }
}
