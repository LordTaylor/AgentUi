// Handles UI settings, scratchpad, IPC log, working directory, config, and debug log dump intents.
// Owns dumpDebugLog() and testHttpUrl() debug helpers.
// See: docs/COMMUNICATION.md §update_config for config update protocol.
package com.agentcore.ui.chat

import androidx.compose.runtime.MutableState
import com.agentcore.api.*
import com.agentcore.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val uiState: MutableState<ChatUiState>,
    private val settingsManager: SettingsManager,
    private val stdioExecutor: StdioExecutor,
    private val log: (String, String, String) -> Unit,
    private val getMode: () -> ConnectionMode,
    private val incomingEventCounts: Map<String, Int>,
    private val autoAcceptUseCase: com.agentcore.logic.AutoAcceptUseCase,
    private val client: AgentClient,
    private val unixSocketExecutor: UnixSocketExecutor,
) {
    private fun update(block: ChatUiState.() -> ChatUiState) { uiState.value = uiState.value.block() }
    private val st get() = uiState.value

    fun handle(intent: ChatIntent, scope: CoroutineScope, mode: ConnectionMode) {
        when (intent) {
            is ChatIntent.UpdateScratchpad  -> update { copy(scratchpadContent = intent.content) }
            is ChatIntent.UpdateUiSettings  -> updateUiSettings(intent, scope, mode)
            ChatIntent.ToggleSettings       -> update { copy(showSettings = !showSettings) }
            ChatIntent.ToggleIpcLog         -> update { copy(ipcLogExpanded = !ipcLogExpanded) }
            is ChatIntent.SetWorkingDir     -> setWorkingDir(intent)
            ChatIntent.DumpDebugLog         -> scope.launch(Dispatchers.IO) { dumpDebugLog() }
            is ChatIntent.ScheduleTask      -> scope.launch { if (mode == ConnectionMode.IPC) client.scheduleTask(intent.text, intent.at, intent.cron, st.currentSessionId) }
            is ChatIntent.SetSystemPrompt   -> setSystemPrompt(intent, scope, mode)
            is ChatIntent.UpdateConfig      -> updateConfig(intent, scope, mode)
            ChatIntent.ToggleSidebar        -> {
                val newSettings = st.uiSettings.copy(sidebarVisible = !st.uiSettings.sidebarVisible)
                update { copy(uiSettings = newSettings) }
                settingsManager.save(newSettings, UiSettings.serializer())
            }
            else -> {}
        }
    }

    private fun updateUiSettings(intent: ChatIntent.UpdateUiSettings, scope: CoroutineScope, mode: ConnectionMode) {
        val oldAutoAccept = st.uiSettings.autoAccept
        val oldBypass = st.uiSettings.bypassAllPermissions
        update { copy(uiSettings = intent.settings) }
        settingsManager.save(intent.settings, UiSettings.serializer())
        if (oldAutoAccept != intent.settings.autoAccept || oldBypass != intent.settings.bypassAllPermissions) {
            autoAcceptUseCase.sync(scope, mode, intent.settings.autoAccept, intent.settings.bypassAllPermissions)
        }
    }

    private fun setWorkingDir(intent: ChatIntent.SetWorkingDir) {
        val newSettings = st.uiSettings.copy(workingDir = intent.path)
        update { copy(workingDir = intent.path, uiSettings = newSettings) }
        settingsManager.save(newSettings, UiSettings.serializer())
    }

    private fun setSystemPrompt(intent: ChatIntent.SetSystemPrompt, scope: CoroutineScope, mode: ConnectionMode) {
        update { copy(currentSystemPrompt = intent.prompt) }
        scope.launch {
            val sid = st.currentSessionId
            if (mode == ConnectionMode.IPC && sid != null) {
                client.sendCommand(IpcCommand.SetSystemPrompt(SetSystemPromptPayload(sid, intent.prompt)))
            }
        }
    }

    private fun updateConfig(intent: ChatIntent.UpdateConfig, scope: CoroutineScope, mode: ConnectionMode) {
        scope.launch {
            val cmd = IpcCommand.UpdateConfig(UpdateConfigPayload(intent.key, intent.value))
            when (mode) {
                ConnectionMode.IPC         -> client.updateConfig(intent.key, intent.value)
                ConnectionMode.STDIO       -> stdioExecutor.sendCommand(cmd)
                ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                else                       -> {}
            }
        }
    }

    private fun dumpDebugLog() {
        val state = st
        val mode = getMode()
        val baseDir = state.workingDir.ifEmpty { System.getProperty("user.home") ?: "." }
        val debugDir = java.io.File(baseDir, "DebugLog")
        try {
            if (debugDir.exists()) debugDir.deleteRecursively()
            debugDir.mkdirs()
            val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            val outCount = state.ipcLogs.count { "  →  " in it }
            val inCount = state.ipcLogs.count { "  ←  " in it }
            val errorLines = state.ipcLogs.filter { it.contains("error", true) || it.contains("STDIO_EXITED") || it.contains("DISCONNECTED") || it.contains("BACKEND_ERROR") }
            val stderrLines = stdioExecutor.stderrSnapshot()
            val providerCfg = state.uiSettings.providerConfigs[state.currentBackend]
            val binaryPath = com.agentcore.shared.CoreLauncher.findBinary() ?: "(not found)"
            val binaryFile = java.io.File(binaryPath)
            val binaryTs = if (binaryFile.exists()) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(binaryFile.lastModified())) else "(not found)"
            val envVars = com.agentcore.ui.components.buildAllEnvVars(state.uiSettings.providerConfigs)
            val displayUrl = providerCfg?.baseUrl?.ifEmpty { "(default)" } ?: "(none)"

            java.io.File(debugDir, "ipc_traffic.txt").writeText(buildString {
                appendLine("# IPC Traffic Log — $ts\n# Outgoing (→): $outCount   Incoming (←): $inCount\n")
                if (state.ipcLogs.isEmpty()) appendLine("(no IPC traffic recorded)") else state.ipcLogs.forEach { appendLine(it) }
            })
            java.io.File(debugDir, "connection_errors.txt").writeText(buildString {
                appendLine("# Connection & Error Lines — $ts\n")
                if (errorLines.isEmpty()) appendLine("(no errors recorded)") else errorLines.forEach { appendLine(it) }
            })
            java.io.File(debugDir, "system_info.txt").writeText(buildString {
                appendLine("# System & Configuration — $ts\n")
                appendLine("Mode: $mode  |  Active backend: ${state.currentBackend}  |  Model: ${providerCfg?.model ?: "(none)"}  |  URL: $displayUrl\n")
                appendLine("Binary: $binaryPath  ($binaryTs, ${if (binaryFile.exists()) "${binaryFile.length() / 1024}KB" else "N/A"})\n")
                appendLine("── Env vars ──"); if (envVars.isEmpty()) appendLine("(none)") else envVars.forEach { (k, v) -> appendLine("$k = ${if (k.endsWith("_KEY") || k.endsWith("_TOKEN")) v.take(8) + "***" else v}") }
                appendLine("\n── Provider configs ──"); state.uiSettings.providerConfigs.forEach { (id, cfg) -> appendLine("[$id] model=${cfg.model.ifEmpty{"(default)"}} url=${cfg.baseUrl.ifEmpty{"(default)"}}" ) }
                appendLine("\n── JVM ──"); appendLine("Java ${System.getProperty("java.version")} / ${System.getProperty("os.name")} ${System.getProperty("os.version")} / home=${System.getProperty("user.home")}")
            })
            java.io.File(debugDir, "connectivity_test.txt").writeText(buildString {
                appendLine("# Connectivity Test — $ts\n")
                val lmUrl = state.uiSettings.providerConfigs["lmstudio"]?.baseUrl?.ifEmpty { "http://localhost:1234" } ?: "http://localhost:1234"
                val ollamaUrl = state.uiSettings.providerConfigs["ollama"]?.baseUrl?.ifEmpty { "http://localhost:11434" } ?: "http://localhost:11434"
                listOf("LM Studio" to "$lmUrl/v1/models", "Ollama" to "$ollamaUrl/api/tags", "agent-core" to "http://localhost:7700").forEach { (label, url) ->
                    val (result, body) = testHttpUrl(url)
                    appendLine("$label\n  URL: $url\n  Result: $result${if (body.isNotEmpty()) "\n  Body: ${body.take(200)}" else ""}\n")
                }
            })
            java.io.File(debugDir, "app_events.txt").writeText(buildString {
                appendLine("# App Events Log — $ts\n")
                if (state.logs.isEmpty()) appendLine("(no events)") else state.logs.forEach { appendLine("[${it.timestamp}] [${it.level}] ${it.source?.let { s -> "($s) " } ?: ""}${it.message}") }
            })
            java.io.File(debugDir, "agent_stderr.txt").writeText(buildString {
                appendLine("# Agent-Core Stderr — $ts\n")
                if (stderrLines.isEmpty()) appendLine("(none)") else stderrLines.forEach { appendLine(it) }
            })
            java.io.File(debugDir, "summary.txt").writeText(buildString {
                appendLine("# Debug Summary — $ts\n")
                appendLine("Session: ${state.currentSessionId ?: "(none)"}  Status: ${state.statusState}  Messages: ${state.messages.size}")
                appendLine("Mode: $mode  Backend: ${state.currentBackend}  Model: ${providerCfg?.model ?: "(none)"}  URL: $displayUrl")
                appendLine("IPC: total=${state.ipcLogs.size} out=$outCount in=$inCount errors=${errorLines.size}")
                appendLine("Stderr lines: ${stderrLines.size}  App events: ${state.logs.size}")
                if (incomingEventCounts.isNotEmpty()) {
                    appendLine("── Incoming by type ──")
                    incomingEventCounts.entries.sortedByDescending { it.value }.forEach { (name, count) -> appendLine("  ← ${name.padEnd(28)} $count") }
                }
                appendLine("Files written to: ${debugDir.absolutePath}")
            })
            log("→", "dump_debug_log", "OK → ${debugDir.absolutePath} (7 files)")
        } catch (e: Exception) { log("→", "dump_debug_log", "FAIL: ${e.message?.take(60)}") }
    }

    private fun testHttpUrl(url: String): Pair<String, String> = try {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 3000; conn.readTimeout = 3000; conn.requestMethod = "GET"; conn.connect()
        val code = conn.responseCode
        val body = try { conn.inputStream.bufferedReader().readText().take(500) } catch (_: Exception) { "" }
        conn.disconnect(); "HTTP $code OK" to body
    } catch (e: java.net.ConnectException) { "UNREACHABLE (connection refused)" to "" }
     catch (e: java.net.SocketTimeoutException) { "TIMEOUT (>3s)" to "" }
     catch (e: Exception) { "ERROR: ${e.message?.take(80)}" to "" }
}
