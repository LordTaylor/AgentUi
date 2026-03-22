package com.agentcore.shared

import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class CliExecutor(private val workingDir: String = "/Users/jaroslawkrawczyk/AgentCl2.0/CoreApp") {
    
    fun executeCommand(message: String): String {
        return try {
            val process = ProcessBuilder("cargo", "run", "--", message)
                .directory(File(workingDir))
                .redirectErrorStream(true)
                .start()

            val reader = InputStreamReader(process.inputStream)
            val output = reader.readText()
            
            process.waitFor(1, TimeUnit.MINUTES)
            
            // Basic filtering of logs (lines starting with date/time stamps like 2026-03-22T...)
            output.lines()
                .filter { it.isNotBlank() && !it.matches(Regex("^\\d{4}-\\d{2}-\\d{2}T.*")) }
                .filter { !it.contains("Finished `dev` profile") && !it.contains("Running `target/debug/agent-core") }
                .joinToString("\n")
                .trim()
        } catch (e: Exception) {
            "Error executing CLI: ${e.message}"
        }
    }
}
