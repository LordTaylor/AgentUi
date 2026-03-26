// Handles message sending, approval, plan resolution, input history, and message-display intents.
// Updates ChatUiState via shared MutableState reference injected from ChatViewModel.
// See: docs/COMMUNICATION.md for send_message, approval_response, human_input_response protocols.
package com.agentcore.ui.chat

import androidx.compose.runtime.MutableState
import com.agentcore.api.*
import com.agentcore.logic.IpcHandler
import com.agentcore.model.Message
import com.agentcore.model.MessageType
import com.agentcore.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MessageViewModel(
    private val uiState: MutableState<ChatUiState>,
    private val client: AgentClient,
    private val stdioExecutor: StdioExecutor,
    private val unixSocketExecutor: UnixSocketExecutor,
    private val cliExecutor: CliExecutor,
    private val log: (String, String, String) -> Unit,
    private val saveCache: () -> Unit,
) {
    private fun update(block: ChatUiState.() -> ChatUiState) { uiState.value = uiState.value.block() }
    private val st get() = uiState.value

    fun handle(intent: ChatIntent, scope: CoroutineScope, mode: ConnectionMode) {
        when (intent) {
            is ChatIntent.SendMessage        -> sendMessage(intent, scope, mode)
            is ChatIntent.UpdateInputText    -> update { copy(inputText = intent.text) }
            ChatIntent.ClearChat             -> update { copy(messages = emptyList()) }
            is ChatIntent.ResolveApproval    -> resolveApproval(intent, scope, mode)
            is ChatIntent.RespondHumanInput  -> respondHumanInput(intent, scope, mode)
            ChatIntent.CancelAction          -> cancelAction(scope, mode)
            ChatIntent.NavigateHistoryUp     -> navigateUp()
            ChatIntent.NavigateHistoryDown   -> navigateDown()
            is ChatIntent.UpdateSearchQuery  -> update { copy(messageSearchQuery = intent.query) }
            is ChatIntent.ResolvePlan        -> resolvePlan(intent, scope, mode)
            is ChatIntent.PasteToInput       -> update { copy(inputText = intent.text) }
            ChatIntent.ToggleSearch          -> update { copy(showSearch = !showSearch) }
            ChatIntent.ToggleToolOutput      -> update { copy(showToolOutput = !showToolOutput) }
            ChatIntent.ClearToolOutput       -> update { copy(toolOutput = emptyList()) }
            ChatIntent.ToggleTokenAnalytics  -> update { copy(showTokenAnalytics = !showTokenAnalytics) }
            ChatIntent.RetryMessage          -> {
                val last = st.messages.lastOrNull { it.isFromUser }
                if (last != null) sendMessage(ChatIntent.SendMessage(last.text, last.attachments ?: emptyList()), scope, mode)
            }
            ChatIntent.ExportSession         -> exportSession()
            else                             -> {}
        }
    }

    private fun sendMessage(intent: ChatIntent.SendMessage, scope: CoroutineScope, mode: ConnectionMode) {
        if (handleSlashCommand(intent.text, scope, mode)) return
        val history = st.messageHistory.toMutableList()
        if (intent.text.isNotBlank() && history.lastOrNull() != intent.text) history.add(intent.text)
        log("→", "send_message", "\"${intent.text.take(40)}${if (intent.text.length > 40) "…" else ""}\"")
        IpcHandler.performSendMessage(
            scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode,
            intent.text, intent.images, st.currentSessionId,
            onMessageAdded = { msg -> update { copy(messages = messages + msg) }; saveCache() },
            onClearInput = {}, onClearAttachments = {},
            onStatusChange = { update { copy(statusState = it) } },
            workingDir = st.workingDir.takeIf { it.isNotBlank() }
        )
        update { copy(messageHistory = history, historyIndex = null, draftMessage = "", inputText = "") }
    }

    private fun resolveApproval(intent: ChatIntent.ResolveApproval, scope: CoroutineScope, mode: ConnectionMode) {
        scope.launch {
            val pending = st.pendingApproval
            if (pending != null) {
                val cmd = IpcCommand.ApprovalResponse(ApprovalResponsePayload(pending.id, intent.approved))
                when (mode) {
                    ConnectionMode.IPC         -> client.sendCommand(cmd)
                    ConnectionMode.STDIO       -> stdioExecutor.sendCommand(cmd)
                    ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                    else                       -> {}
                }
            }
            update { copy(pendingApproval = null) }
        }
    }

    private fun respondHumanInput(intent: ChatIntent.RespondHumanInput, scope: CoroutineScope, mode: ConnectionMode) {
        val pending = st.pendingHumanInput
        update { copy(pendingHumanInput = null) }
        if (pending != null) {
            val cmd = IpcCommand.HumanInputResponse(HumanInputResponsePayload(pending.id, intent.answer))
            scope.launch {
                when (mode) {
                    ConnectionMode.IPC         -> client.sendCommand(cmd)
                    ConnectionMode.STDIO       -> stdioExecutor.sendCommand(cmd)
                    ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                    ConnectionMode.CLI         -> {}
                }
            }
        }
    }

    private fun cancelAction(scope: CoroutineScope, mode: ConnectionMode) {
        scope.launch {
            val sid = st.currentSessionId
            if (sid != null) {
                val cmd = IpcCommand.Cancel(CancelPayload(sid))
                when (mode) {
                    ConnectionMode.IPC         -> client.sendCommand(cmd)
                    ConnectionMode.STDIO       -> stdioExecutor.sendCommand(cmd)
                    ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                    ConnectionMode.CLI         -> {}
                }
            }
            update { copy(statusState = "IDLE") }
        }
    }

    private fun navigateUp() {
        val history = st.messageHistory
        if (history.isEmpty()) return
        val idx = st.historyIndex
        val newIdx = if (idx == null) {
            update { copy(draftMessage = inputText) }
            history.size - 1
        } else (idx - 1).coerceAtLeast(0)
        update { copy(historyIndex = newIdx, inputText = history[newIdx]) }
    }

    private fun navigateDown() {
        val history = st.messageHistory
        val idx = st.historyIndex ?: return
        if (idx >= history.size - 1) {
            update { copy(historyIndex = null, inputText = draftMessage) }
        } else {
            val newIdx = idx + 1
            update { copy(historyIndex = newIdx, inputText = history[newIdx]) }
        }
    }

    private fun resolvePlan(intent: ChatIntent.ResolvePlan, scope: CoroutineScope, mode: ConnectionMode) {
        scope.launch {
            val cmd = IpcCommand.ApprovePlan(ApprovePlanPayload(intent.planId, intent.approved))
            when (mode) {
                ConnectionMode.IPC         -> client.sendCommand(cmd)
                ConnectionMode.STDIO       -> stdioExecutor.sendCommand(cmd)
                ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                else                       -> {}
            }
            update { copy(pendingPlan = null) }
        }
    }

    private fun handleSlashCommand(text: String, scope: CoroutineScope, mode: ConnectionMode): Boolean {
        if (!text.startsWith("/")) return false
        val cmd = text.lowercase().substringBefore(" ")
        when (cmd) {
            "/clear"  -> { update { copy(messages = emptyList()) }; saveCache() }
            "/reset"  -> { update { copy(messages = emptyList(), statusState = "IDLE") }; saveCache() }
            "/help"   -> {
                val help = "**Available commands:**\n- `/clear` — Clear messages\n- `/reset` — Clear + reset status\n- `/stats` — Refresh token stats\n- `/export` — Export session to file\n- `/help` — Show this help"
                update { copy(messages = messages + Message(IpcHandler.nextId("help"), "System", help, false, type = MessageType.SYSTEM)) }
            }
            "/stats"  -> scope.launch {
                when (mode) {
                    ConnectionMode.STDIO       -> stdioExecutor.sendCommand(IpcCommand.GetStats())
                    ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(IpcCommand.GetStats())
                    ConnectionMode.IPC         -> { val s = client.getStats(); update { copy(sessionStats = s) } }
                    else                       -> {}
                }
            }
            "/bash"   -> {
                val code = text.removePrefix("/bash").trim()
                if (code.isNotEmpty()) IpcHandler.performSendMessage(scope, client, stdioExecutor, unixSocketExecutor, cliExecutor, mode, "Executing: $code", emptyList(), st.currentSessionId ?: "", {}, {}, {}, {})
            }
            "/export" -> exportSession()
            else      -> return false
        }
        return true
    }

    private fun exportSession() {
        val sid = st.currentSessionId ?: return
        val messages = st.messages
        if (messages.isEmpty()) return
        val baseDir = st.workingDir.ifEmpty { System.getProperty("user.home") ?: "." }
        val file = java.io.File(java.io.File(baseDir, "Exports"), "session_${sid.take(8)}_${System.currentTimeMillis()}.md")
        file.parentFile?.mkdirs()
        try {
            file.writeText(buildString {
                appendLine("# Chat Session Export")
                appendLine("Session ID: $sid")
                appendLine("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
                appendLine("\n---\n")
                messages.forEach { msg ->
                    appendLine("### ${msg.sender}${if (msg.isFromUser) " (User)" else ""}")
                    appendLine(msg.text)
                    if (!msg.attachments.isNullOrEmpty()) appendLine("Attached: ${msg.attachments!!.size} image(s)")
                    appendLine()
                }
            })
            update { copy(messages = messages + Message(IpcHandler.nextId("export"), "System", "Session exported to: `${file.absolutePath}`", false, type = MessageType.SYSTEM)) }
        } catch (e: Exception) { log("←", "export_failed", e.message ?: "error") }
    }
}
