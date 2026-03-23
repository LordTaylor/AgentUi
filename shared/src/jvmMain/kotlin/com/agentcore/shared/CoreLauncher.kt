package com.agentcore.shared

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.exists

object CoreLauncher {

    private val home = System.getProperty("user.home") ?: ""

    /**
     * Detects if agent-core is already running.
     * Priority: Unix socket → HTTP server → null (not running, use STDIO).
     *
     * Called from main() before showing the UI — fast, blocking, no coroutines.
     */
    fun detectRunningMode(): ConnectionMode? {
        // 1. Unix socket — fastest, most reliable for local daemon
        val sock = File("$home/.agentcore/agent.sock")
        if (sock.exists() && isSocketAlive(sock.absolutePath)) {
            println("[CoreLauncher] Detected running agent-core via Unix socket")
            return ConnectionMode.UNIX_SOCKET
        }
        // 2. HTTP server (e.g. agent-core --server)
        if (isHttpReachable("http://127.0.0.1:7700")) {
            println("[CoreLauncher] Detected running agent-core via HTTP :7700")
            return ConnectionMode.IPC
        }
        // 3. Nothing running — UI will start its own subprocess via STDIO
        println("[CoreLauncher] No running agent-core detected — will use STDIO mode")
        return null
    }

    /**
     * Returns the absolute path of the agent-core binary, checking locations in order:
     *   1. Bundled resources dir (compose.application.resources.dir) — installed app
     *   2. ~/.local/bin/agent-core — installed via install.sh
     *   3. Project release build — dev environment
     *   4. Project debug build — dev environment
     */
    fun findBinary(): String? {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        val binaryName = if (System.getProperty("os.name").contains("Windows", ignoreCase = true))
            "agent-core.exe" else "agent-core"

        val candidates = listOfNotNull(
            resourcesDir?.let { File("$it/$binaryName").takeIf { f -> f.canExecute() } },
            File("$home/.local/bin/$binaryName").takeIf { it.canExecute() },
            // Dev: look relative to the jar/class location heuristically
            findDevBinary(binaryName),
        )
        return candidates.firstOrNull()?.absolutePath?.also {
            println("[CoreLauncher] Found agent-core binary: $it")
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun isSocketAlive(socketPath: String): Boolean {
        return try {
            // Java 17+ Unix domain socket
            val addr = java.net.UnixDomainSocketAddress.of(Path.of(socketPath))
            val ch = java.nio.channels.SocketChannel.open(java.net.StandardProtocolFamily.UNIX)
            ch.use { it.connect(addr) }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isHttpReachable(url: String): Boolean {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 800
            conn.readTimeout = 800
            conn.connect()
            conn.responseCode in 200..499 // any response = server is up
        } catch (_: Exception) {
            false
        }
    }

    private fun findDevBinary(name: String): File? {
        // Walk up from working directory looking for CoreApp/target/release/agent-core
        var dir = File(System.getProperty("user.dir") ?: return null)
        repeat(5) {
            val candidate = File(dir, "CoreApp/target/release/$name")
            if (candidate.canExecute()) return candidate
            val candidate2 = File(dir, "CoreApp/target/debug/$name")
            if (candidate2.canExecute()) return candidate2
            dir = dir.parentFile ?: return null
        }
        return null
    }
}
