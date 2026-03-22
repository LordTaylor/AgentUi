package com.agentcore.shared

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object CoreLauncher {
    private const val CORE_PATH = "/Users/jaroslawkrawczyk/AgentCl2.0/CoreApp"
    private const val SERVER_URL = "http://localhost:3000/ipc"

    fun ensureRunning() {
        if (isCoreRunning()) {
            println("agent-core is already running.")
            return
        }

        println("Starting agent-core...")
        val pb = ProcessBuilder("cargo", "run", "--", "--server")
            .directory(File(CORE_PATH))
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)

        thread(start = true, isDaemon = true) {
            try {
                val process = pb.start()
                process.waitFor()
                println("agent-core process exited.")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Wait for server to be ready
        var attempts = 0
        while (attempts < 10) {
            if (isCoreRunning()) {
                println("agent-core is now ready.")
                return
            }
            Thread.sleep(1000)
            attempts++
        }
        println("Warning: agent-core did not respond within 10 seconds.")
    }

    private fun isCoreRunning(): Boolean {
        return try {
            val connection = URL(SERVER_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 500
            connection.readTimeout = 500
            connection.doOutput = true
            // Just a ping-like check, don't worry about the body for now
            connection.outputStream.write("{}".toByteArray())
            connection.responseCode != -1
        } catch (e: Exception) {
            false
        }
    }
}
