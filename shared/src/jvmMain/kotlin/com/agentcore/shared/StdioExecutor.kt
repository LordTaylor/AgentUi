package com.agentcore.shared

import com.agentcore.api.IpcCommand
import com.agentcore.api.IpcEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

class StdioExecutor(private val workingDir: String = "/Users/jaroslawkrawczyk/AgentCl2.0/CoreApp") {
    private var process: Process? = null
    private var reader: InputStreamReader? = null
    private var writer: PrintWriter? = null
    private val _events = MutableSharedFlow<IpcEvent>()
    val events: SharedFlow<IpcEvent> = _events
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    fun start() {
        if (process != null) return
        
        try {
            val builder = ProcessBuilder("cargo", "run", "--", "--stdio")
                .directory(File(workingDir))
                .redirectError(ProcessBuilder.Redirect.INHERIT)
            
            process = builder.start()
            reader = InputStreamReader(process!!.inputStream)
            writer = PrintWriter(process!!.outputStream, true)

            scope.launch {
                val inputReader = process!!.inputStream.bufferedReader()
                while (isActive) {
                    val line = inputReader.readLine() ?: break
                    try {
                        val event = json.decodeFromString<IpcEvent>(line)
                        _events.emit(event)
                    } catch (e: Exception) {
                        // Skip lines that aren't valid JSON (logs etc)
                        if (line.trim().startsWith("{")) {
                           kotlin.io.println("Failed to parse event: $line, error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            scope.launch {
                _events.emit(IpcEvent.Error(com.agentcore.api.ErrorPayload("STDIO_ERROR", e.message ?: "Unknown error")))
            }
        }
    }

    fun sendCommand(command: IpcCommand) {
        val jsonStr = json.encodeToString(command)
        writer?.println(jsonStr)
    }

    fun stop() {
        process?.destroy()
        process = null
        scope.cancel()
    }
}
