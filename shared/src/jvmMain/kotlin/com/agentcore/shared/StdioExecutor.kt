package com.agentcore.shared

import com.agentcore.api.IpcCommand
import com.agentcore.api.IpcEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.PrintWriter

class StdioExecutor(private val workingDir: String = "/Users/jaroslawkrawczyk/AgentCl2.0/CoreApp") {
    private var process: Process? = null
    private var writer: PrintWriter? = null
    // replay=1 so the "ready" event is not lost if the collector starts after the process
    private val _events = MutableSharedFlow<IpcEvent>(replay = 1)
    val events: SharedFlow<IpcEvent> = _events

    // Persistent coroutine scope — NOT cancelled on stop so restart() works
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null
    private var stderrJob: Job? = null

    // Extra environment variables injected before start (e.g. LMSTUDIO_BASE_URL)
    private val extraEnv = mutableMapOf<String, String>()

    // Stderr capture — capped at 5000 lines for debug log collection
    private val stderrBuffer = ArrayDeque<String>(5000)
    fun stderrSnapshot(): List<String> = synchronized(stderrBuffer) { stderrBuffer.toList() }

    private val json = Json {
        ignoreUnknownKeys = true
        // encodeDefaults = false (default) — do NOT serialize null fields or fields equal to
        // their default values. The Rust backend uses serde defaults and cannot handle
        // null where it expects a sequence (e.g. attachments: null → PARSE_ERROR).
        encodeDefaults = false
    }

    /** Set extra environment variables (e.g. LMSTUDIO_BASE_URL) before calling start(). */
    fun setEnvVars(vars: Map<String, String>) {
        extraEnv.clear()
        extraEnv.putAll(vars)
    }

    private fun findBinary(): Pair<List<String>, File?> {
        // Delegate to CoreLauncher for consistent binary resolution
        // (checks bundled resources dir, ~/.local/bin, dev build paths)
        val binary = CoreLauncher.findBinary()
        if (binary != null) return listOf(binary, "--stdio", "--debug") to null

        // Fallback: use cargo run for dev environments.
        // cargo may not be in PATH when launched from a GUI app bundle, so try known locations.
        val home = System.getProperty("user.home") ?: ""
        val cargo = listOf(
            "$home/.cargo/bin/cargo",
            "/usr/local/bin/cargo",
            "/opt/homebrew/bin/cargo"
        ).firstOrNull { File(it).canExecute() } ?: "cargo"

        return listOf(cargo, "run", "--", "--stdio", "--debug") to File(workingDir)
    }

    fun start() {
        if (process != null) return
        try {
            val (cmd, dir) = findBinary()
            val builder = ProcessBuilder(cmd)
                .redirectErrorStream(false)
            dir?.let { builder.directory(it) }

            // Inject extra environment variables
            if (extraEnv.isNotEmpty()) {
                builder.environment().putAll(extraEnv)
            }

            process = builder.start()
            writer = PrintWriter(process!!.outputStream, true)

            // Read stdout (JSON events).
            // Capture the process handle NOW so that if restart() is called while this
            // coroutine is still draining output, waitFor() runs on THIS process
            // (not on the new one set by start()).
            val capturedProcess = process!!
            readJob = scope.launch(Dispatchers.IO) {
                val inputReader = capturedProcess.inputStream.bufferedReader()
                while (isActive) {
                    val line = inputReader.readLine() ?: break
                    try {
                        val event = json.decodeFromString<IpcEvent>(line)
                        _events.emit(event)
                    } catch (e: Exception) {
                        if (line.trim().startsWith("{")) {
                            kotlin.io.println("Failed to parse stdio event: $line — ${e.message}")
                        }
                    }
                }
                // Process exited — notify collectors so UI can show disconnection
                val exitCode = try { capturedProcess.waitFor() } catch (_: Exception) { -1 }
                _events.emit(IpcEvent.Error(payload = com.agentcore.api.ErrorPayload(
                    "STDIO_EXITED",
                    "agent-core process exited (code $exitCode)"
                )))
            }

            // Read stderr into buffer (for debug log collection)
            stderrJob = scope.launch(Dispatchers.IO) {
                val errReader = capturedProcess.errorStream.bufferedReader()
                while (isActive) {
                    val line = errReader.readLine() ?: break
                    synchronized(stderrBuffer) {
                        if (stderrBuffer.size >= 5000) stderrBuffer.removeFirst()
                        stderrBuffer.addLast(line)
                    }
                }
            }
        } catch (e: Exception) {
            scope.launch {
                _events.emit(IpcEvent.Error(payload = com.agentcore.api.ErrorPayload("STDIO_ERROR", e.message ?: "Unknown error")))
            }
        }
    }

    suspend fun sendCommand(command: IpcCommand) {
        withContext(Dispatchers.IO) {
            val jsonStr = json.encodeToString(command)
            writer?.println(jsonStr)
        }
    }

    /**
     * Stop the current agent-core process without cancelling the shared scope.
     * Call start() or restart() afterwards to reconnect.
     */
    fun stop() {
        readJob?.cancel()
        stderrJob?.cancel()
        writer?.close()
        val dying = process
        process = null
        writer = null
        // Send SIGTERM and wait briefly, then SIGKILL if still alive
        dying?.destroy()
        try { dying?.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) } catch (_: Exception) {}
        dying?.destroyForcibly()
    }

    /**
     * Stop the process, update env vars, and spawn a fresh agent-core process.
     * The existing [events] SharedFlow continues — collectors don't need to resubscribe.
     */
    fun restart(newEnvVars: Map<String, String> = emptyMap()) {
        stop()
        extraEnv.clear()
        extraEnv.putAll(newEnvVars)
        start()
    }
}
