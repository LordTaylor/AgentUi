// Handles session CRUD, history search, memory panel, and folder management intents.
// Owns cachedData (offline session cache) and syncSessions/saveSessionCache helpers.
// See: docs/COMMUNICATION.md §sessions for session lifecycle protocol.
package com.agentcore.ui.chat

import androidx.compose.runtime.MutableState
import com.agentcore.api.*
import com.agentcore.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SessionViewModel(
    private val uiState: MutableState<ChatUiState>,
    private val client: AgentClient,
    private val stdioExecutor: StdioExecutor,
    private val unixSocketExecutor: UnixSocketExecutor,
    private val sessionCache: SettingsManager,
    private val log: (String, String, String) -> Unit,
) {
    var scope: CoroutineScope? = null
    var mode: ConnectionMode = ConnectionMode.STDIO

    var cachedData = SessionCache()
    private var saveJob: Job? = null

    private fun update(block: ChatUiState.() -> ChatUiState) { uiState.value = uiState.value.block() }
    private val st get() = uiState.value

    fun handle(intent: ChatIntent, scope: CoroutineScope, mode: ConnectionMode) {
        when (intent) {
            is ChatIntent.SelectSession        -> selectSession(intent, scope, mode)
            ChatIntent.NewSession              -> { update { copy(currentSessionId = null, messages = emptyList()) }; log("→", "new_session", "cleared") }
            is ChatIntent.DeleteSession        -> deleteSession(intent, scope, mode)
            is ChatIntent.PruneSession         -> pruneSession(intent, scope, mode)
            is ChatIntent.ForkSession          -> scope.launch { sendCmd(IpcCommand.ForkSession(ForkSessionPayload(intent.sessionId, intent.messageIdx)), mode) }
            is ChatIntent.TagSession           -> tagSession(intent, scope, mode)
            is ChatIntent.ToggleFilter         -> {
                val f = st.activeFilters.toMutableList()
                if (f.contains(intent.tag)) f.remove(intent.tag) else f.add(intent.tag)
                update { copy(activeFilters = f) }
            }
            is ChatIntent.SummarizeContext     -> summarizeContext(intent, scope, mode)
            is ChatIntent.UpdateHistorySearch  -> update { copy(historySearchText = intent.query) }
            is ChatIntent.MoveSessionToFolder  -> moveToFolder(intent)
            is ChatIntent.LoadMemory           -> scope.launch { sendCmd(IpcCommand.ListMemory(ListMemoryPayload(intent.sessionId)), mode) }
            is ChatIntent.DeleteMemoryKey      -> scope.launch { sendCmd(IpcCommand.DeleteMemory(DeleteMemoryPayload(intent.sessionId, intent.key)), mode) }
            ChatIntent.ToggleMemoryPanel       -> toggleMemory(scope, mode)
            else                               -> {}
        }
    }

    private fun selectSession(intent: ChatIntent.SelectSession, scope: CoroutineScope, mode: ConnectionMode) {
        log("→", "get_session", intent.id.take(8))
        val cachedMsgs = cachedData.sessionMessages[intent.id] ?: emptyList()
        update { copy(currentSessionId = intent.id, messages = cachedMsgs) }
        scope.launch {
            if (mode == ConnectionMode.IPC) client.sendCommand(IpcCommand.GetSession(GetSessionPayload(intent.id)))
        }
    }

    private fun deleteSession(intent: ChatIntent.DeleteSession, scope: CoroutineScope, mode: ConnectionMode) {
        val oldSessions = st.sessions; val oldId = st.currentSessionId; val oldMsgs = st.messages
        update {
            copy(
                sessions = sessions.filter { it.id != intent.id },
                currentSessionId = if (currentSessionId == intent.id) null else currentSessionId,
                messages = if (currentSessionId == intent.id) emptyList() else messages
            )
        }
        saveSessionCache()
        scope.launch {
            try {
                if (mode == ConnectionMode.IPC) {
                    client.deleteSession(intent.id)
                    val sessions = client.listSessions()
                    update { copy(sessions = sessions) }
                    saveSessionCache()
                }
            } catch (e: Exception) {
                update { copy(sessions = oldSessions, currentSessionId = oldId, messages = oldMsgs) }
                saveSessionCache()
                log("←", "delete_failed", e.message ?: "network error")
            }
        }
    }

    private fun pruneSession(intent: ChatIntent.PruneSession, scope: CoroutineScope, mode: ConnectionMode) {
        scope.launch {
            sendCmd(IpcCommand.PruneSession(PruneSessionPayload(intent.id, 6)), mode)
            if (mode == ConnectionMode.IPC) {
                client.pruneSession(intent.id)
                if (st.currentSessionId == intent.id) {
                    val sessions = client.listSessions()
                    update { copy(sessions = sessions) }
                }
            }
        }
    }

    private fun tagSession(intent: ChatIntent.TagSession, scope: CoroutineScope, mode: ConnectionMode) {
        scope.launch {
            sendCmd(IpcCommand.TagSession(TagSessionPayload(intent.id, intent.tags)), mode)
            if (mode == ConnectionMode.IPC) {
                client.tagSession(intent.id, intent.tags)
                val sessions = client.listSessions()
                update { copy(sessions = sessions) }
            }
        }
    }

    private fun summarizeContext(intent: ChatIntent.SummarizeContext, scope: CoroutineScope, mode: ConnectionMode) {
        update { copy(isSummarizing = true) }
        scope.launch {
            if (mode == ConnectionMode.IPC) client.summarizeContext(intent.sessionId)
            update { copy(isSummarizing = false) }
        }
    }

    private fun moveToFolder(intent: ChatIntent.MoveSessionToFolder) {
        val folders = st.sessionFolders.toMutableMap()
        if (intent.folderName == null) folders.remove(intent.sessionId) else folders[intent.sessionId] = intent.folderName
        update { copy(sessionFolders = folders) }
        saveSessionCache()
    }

    private fun toggleMemory(scope: CoroutineScope, mode: ConnectionMode) {
        val opening = !st.showMemoryPanel
        update { copy(showMemoryPanel = opening) }
        if (opening) {
            st.currentSessionId?.let { sid ->
                scope.launch { sendCmd(IpcCommand.ListMemory(ListMemoryPayload(sid)), mode) }
            }
        }
    }

    private suspend fun sendCmd(cmd: IpcCommand, mode: ConnectionMode) = when (mode) {
        ConnectionMode.IPC         -> client.sendCommand(cmd)
        ConnectionMode.STDIO       -> stdioExecutor.sendCommand(cmd)
        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
        else                       -> {}
    }

    /** Refresh session list from backend — called after IDLE status or session lifecycle events. */
    fun syncSessions() {
        val currentScope = scope ?: return
        currentScope.launch(Dispatchers.IO) {
            delay(500)
            when (mode) {
                ConnectionMode.IPC -> try {
                    val sessions = client.listSessions()
                    update { copy(sessions = sessions) }
                    saveSessionCache()
                } catch (e: Exception) { log("←", "sync_sessions_err", e.message ?: "unknown") }
                ConnectionMode.STDIO -> stdioExecutor.sendCommand(IpcCommand.ListSessions())
                ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(IpcCommand.ListSessions())
                else -> {}
            }
        }
    }

    /** Debounced write of session cache to disk — safe to call on every event. */
    fun saveSessionCache() {
        val sid = st.currentSessionId ?: return
        val snapshot = cachedData.copy(
            sessions = st.sessions,
            sessionMessages = cachedData.sessionMessages.toMutableMap().also { it[sid] = st.messages }
        )
        cachedData = snapshot
        saveJob?.cancel()
        saveJob = scope?.launch(Dispatchers.IO) {
            delay(500)
            try { sessionCache.save(snapshot, SessionCache.serializer()) } catch (_: Exception) {}
        }
    }

    /** Load cached sessions from disk on startup. */
    fun loadCache() {
        sessionCache.load(SessionCache.serializer())?.let { cache ->
            cachedData = cache
            update { copy(sessions = cache.sessions, sessionFolders = cache.sessionFolders) }
        }
    }
}
