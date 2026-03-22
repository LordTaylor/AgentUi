package com.agentcore.shared

import com.agentcore.api.IpcCommand
import com.agentcore.api.IpcEvent
import com.agentcore.api.ErrorPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Path
import kotlin.io.path.exists

class UnixSocketExecutor(private val socketPath: String = System.getProperty("user.home") + "/.agentcore/agent.sock") {
    private val _events = MutableSharedFlow<IpcEvent>()
    val events = _events.asSharedFlow()

    private var channel: SocketChannel? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun start(scope: CoroutineScope) {
        val path = Path.of(socketPath)
        if (!path.exists()) {
            scope.launch {
                _events.emit(IpcEvent.Error(payload = ErrorPayload("SOCKET_NOT_FOUND", "Socket file not found at $socketPath")))
            }
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                channel = SocketChannel.open(StandardProtocolFamily.UNIX)
                channel?.connect(UnixDomainSocketAddress.of(path))
                
                val buffer = ByteBuffer.allocate(1024 * 64)
                val stringBuilder = StringBuilder()

                while (channel?.isOpen == true) {
                    buffer.clear()
                    val bytesRead = try {
                        channel?.read(buffer) ?: -1
                    } catch (e: Exception) {
                        -1
                    }
                    if (bytesRead == -1) break

                    buffer.flip()
                    val chunk = Charsets.UTF_8.decode(buffer).toString()
                    stringBuilder.append(chunk)

                    var newlineIndex = stringBuilder.indexOf('\n')
                    while (newlineIndex != -1) {
                        val line = stringBuilder.substring(0, newlineIndex).trim()
                        stringBuilder.delete(0, newlineIndex + 1)
                        if (line.isNotEmpty()) {
                            try {
                                val event = json.decodeFromString<IpcEvent>(line)
                                _events.emit(event)
                            } catch (e: Exception) {
                                println("Failed to parse socket event: $line")
                            }
                        }
                        newlineIndex = stringBuilder.indexOf('\n')
                    }
                }
            } catch (e: Exception) {
                _events.emit(IpcEvent.Error(payload = ErrorPayload("SOCKET_ERROR", "Failed to connect to unix socket: ${e.message}")))
            } finally {
                channel?.close()
                channel = null
            }
        }
    }

    suspend fun sendCommand(command: IpcCommand) {
        withContext(Dispatchers.IO) {
            try {
                val cmdJson = json.encodeToString(command) + "\n"
                val buffer = ByteBuffer.wrap(cmdJson.toByteArray(Charsets.UTF_8))
                while (buffer.hasRemaining()) {
                    channel?.write(buffer)
                }
            } catch (e: Exception) {
                _events.emit(IpcEvent.Error(payload = ErrorPayload("SEND_ERROR", "Failed to send command over socket: ${e.message}")))
            }
        }
    }

    fun stop() {
        channel?.close()
        channel = null
    }
}
