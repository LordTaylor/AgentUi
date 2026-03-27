// Defines ProviderMeta data class, the PROVIDERS list, and env-var builder utilities.
// Public buildAllEnvVars is used externally by ChatViewModel.
// Private buildEnvVars is used within the provider dialog components.
package com.agentcore.ui.components

import androidx.compose.ui.graphics.Color
import com.agentcore.api.ProviderConfig

internal data class ProviderMeta(
    val id: String,
    val label: String,
    val badge: String,
    val badgeColor: Color,
    val description: String,
    val defaultModel: String,
    val defaultUrl: String = "",
    val urlLabel: String = "",
    val needsApiKey: Boolean = false,
    val apiKeyEnv: String = "",
    val urlEnvVar: String = ""
)

internal val PROVIDERS = listOf(
    ProviderMeta(
        id = "lmstudio",
        label = "LM Studio",
        badge = "LOCAL",
        badgeColor = Color(0xFF4CAF50),
        description = "OpenAI-compatible local server",
        defaultModel = "local-model",
        defaultUrl = "http://localhost:1234",
        urlLabel = "Server URL",
        urlEnvVar = "LMSTUDIO_BASE_URL"
    ),
    ProviderMeta(
        id = "ollama",
        label = "Ollama",
        badge = "LOCAL",
        badgeColor = Color(0xFF4CAF50),
        description = "Run models locally with Ollama",
        defaultModel = "llama3",
        defaultUrl = "http://localhost:11434",
        urlLabel = "Server URL",
        urlEnvVar = "OLLAMA_BASE_URL"
    ),
    ProviderMeta(
        id = "claude",
        label = "Claude (Anthropic)",
        badge = "API",
        badgeColor = Color(0xFF9C27B0),
        description = "claude-sonnet-4-6, claude-opus-4-6…",
        defaultModel = "claude-sonnet-4-6",
        needsApiKey = true,
        apiKeyEnv = "ANTHROPIC_API_KEY"
    ),
    ProviderMeta(
        id = "openai",
        label = "OpenAI",
        badge = "API",
        badgeColor = Color(0xFF2196F3),
        description = "gpt-4o, gpt-4-turbo, o1…",
        defaultModel = "gpt-4o",
        needsApiKey = true,
        apiKeyEnv = "OPENAI_API_KEY"
    ),
    ProviderMeta(
        id = "google",
        label = "Google Gemini",
        badge = "API",
        badgeColor = Color(0xFFFF9800),
        description = "gemini-2.0-flash, gemini-pro…",
        defaultModel = "gemini-2.0-flash",
        needsApiKey = true,
        apiKeyEnv = "GOOGLE_API_KEY"
    ),
    ProviderMeta(
        id = "huggingface",
        label = "HuggingFace / TGI",
        badge = "LOCAL/API",
        badgeColor = Color(0xFF795548),
        description = "Text Generation Inference server",
        defaultModel = "",
        defaultUrl = "http://localhost:8080",
        urlLabel = "TGI Server URL",
        needsApiKey = true,
        apiKeyEnv = "HF_TOKEN",
        urlEnvVar = "HF_BASE_URL"
    ),
)

internal fun buildEnvVars(backend: String, cfg: ProviderConfig, meta: ProviderMeta): Map<String, String> {
    val vars = mutableMapOf<String, String>()
    if (meta.urlEnvVar.isNotEmpty() && cfg.baseUrl.isNotEmpty()) {
        vars[meta.urlEnvVar] = cfg.baseUrl
    }
    if (meta.apiKeyEnv.isNotEmpty() && cfg.apiKey.isNotEmpty()) {
        vars[meta.apiKeyEnv] = cfg.apiKey
    }
    return vars
}

/**
 * Utility to build all environment variables for all provider configs.
 * Used by ChatViewModel to ensure all potential backends have their required env vars set.
 */
fun buildAllEnvVars(configs: Map<String, ProviderConfig>): Map<String, String> {
    val allVars = mutableMapOf<String, String>()
    PROVIDERS.forEach { meta ->
        val cfg = configs[meta.id] ?: ProviderConfig()
        if (meta.urlEnvVar.isNotEmpty() && cfg.baseUrl.isNotEmpty()) {
            allVars[meta.urlEnvVar] = cfg.baseUrl
        }
        if (meta.apiKeyEnv.isNotEmpty() && cfg.apiKey.isNotEmpty()) {
            allVars[meta.apiKeyEnv] = cfg.apiKey
        }
    }
    return allVars
}
