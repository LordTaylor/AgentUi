// Parses "/" commands from chat input and returns structured SlashCommand values.
// Suggestions are pre-filtered by the typed prefix for the autocomplete popup.
package com.agentcore.ui.components

sealed class SlashCommand {
    data class Role(val name: String) : SlashCommand()
    data class Backend(val name: String, val model: String = "") : SlashCommand()
    data class SystemPrompt(val prompt: String) : SlashCommand()
    data class Bash(val cmd: String) : SlashCommand()
    object Help : SlashCommand()
    object NewSession : SlashCommand()
}

data class SlashSuggestion(
    val trigger: String,       // e.g. "/role"
    val description: String,   // shown in popup
    val example: String = ""   // e.g. "/role coder"
)

object SlashCommandParser {

    private val allSuggestions = listOf(
        SlashSuggestion("/role",    "Switch agent role",          "/role coder"),
        SlashSuggestion("/backend", "Switch AI backend",          "/backend claude"),
        SlashSuggestion("/system",  "Set system prompt",          "/system You are…"),
        SlashSuggestion("/bash",    "Run shell command",          "/bash ls -la"),
        SlashSuggestion("/new",     "Start a new session",        "/new"),
        SlashSuggestion("/help",    "Show available commands",    "/help"),
    )

    /** Returns matching suggestions for the current input prefix. */
    fun suggestions(input: String): List<SlashSuggestion> {
        if (!input.startsWith("/")) return emptyList()
        val prefix = input.lowercase()
        return allSuggestions.filter { it.trigger.startsWith(prefix) }
    }

    /** Parse a complete input string into a SlashCommand, or null if not a slash command. */
    fun parse(input: String): SlashCommand? {
        if (!input.startsWith("/")) return null
        val parts = input.trim().split(Regex("\\s+"), limit = 2)
        val cmd = parts[0].lowercase()
        val arg = parts.getOrNull(1)?.trim() ?: ""
        return when (cmd) {
            "/role"    -> if (arg.isNotEmpty()) SlashCommand.Role(arg) else null
            "/backend" -> {
                val bParts = arg.split(Regex("\\s+"), limit = 2)
                val backend = bParts.getOrNull(0) ?: ""
                if (backend.isNotEmpty()) SlashCommand.Backend(backend, bParts.getOrNull(1) ?: "") else null
            }
            "/system"  -> if (arg.isNotEmpty()) SlashCommand.SystemPrompt(arg) else null
            "/bash"    -> if (arg.isNotEmpty()) SlashCommand.Bash(arg) else null
            "/new"     -> SlashCommand.NewSession
            "/help"    -> SlashCommand.Help
            else -> null
        }
    }

    fun helpText(): String = allSuggestions.joinToString("\n") { "${it.trigger.padEnd(12)} — ${it.description}  (e.g. ${it.example})" }
}
