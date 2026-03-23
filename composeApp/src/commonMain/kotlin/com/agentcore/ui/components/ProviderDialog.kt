package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.agentcore.api.ProviderConfig

// ── Provider metadata ──────────────────────────────────────────────────────────

private data class ProviderMeta(
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

private val PROVIDERS = listOf(
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

// ── Dialog ─────────────────────────────────────────────────────────────────────

@Composable
fun ProviderDialog(
    activeBackend: String,
    providerConfigs: Map<String, ProviderConfig>,
    availableModels: Map<String, List<String>>,
    onDismiss: () -> Unit,
    onActivate: (backend: String, model: String) -> Unit,
    onActivateAndRestart: (backend: String, envVars: Map<String, String>, updatedConfigs: Map<String, ProviderConfig>) -> Unit,
    onSaveConfigs: (Map<String, ProviderConfig>) -> Unit,
    onFetchModels: (backend: String, url: String?) -> Unit
) {
    var selectedId by remember { mutableStateOf(activeBackend.ifEmpty { "lmstudio" }) }
    // Local editable copy of all provider configs
    var configs by remember(providerConfigs) {
        mutableStateOf(
            PROVIDERS.associate { meta ->
                val existing = providerConfigs[meta.id] ?: ProviderConfig()
                meta.id to ProviderConfig(
                    model = existing.model.ifEmpty { meta.defaultModel },
                    baseUrl = existing.baseUrl.ifEmpty { meta.defaultUrl },
                    apiKey = existing.apiKey
                )
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(640.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(0.dp)) {

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Provider / Backend", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }

                HorizontalDivider()

                // Two-column body
                Row(modifier = Modifier.height(400.dp)) {

                    // Left: provider list
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        PROVIDERS.forEach { meta ->
                            ProviderRow(
                                meta = meta,
                                isSelected = meta.id == selectedId,
                                isActive = meta.id == activeBackend,
                                onClick = { selectedId = meta.id }
                            )
                        }
                    }

                    VerticalDivider()

                    // Right: config form for selected provider
                    val meta = PROVIDERS.first { it.id == selectedId }
                    val cfg = configs[selectedId] ?: ProviderConfig()
                    var showApiKey by remember(selectedId) { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Provider title in right pane
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = meta.badgeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    meta.badge, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    color = meta.badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(meta.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Text(meta.description, fontSize = 11.sp, color = Color.Gray)

                        HorizontalDivider()

                        // Model
                        val modelList = availableModels[selectedId] ?: emptyList()
                        var showModelDropdown by remember(selectedId) { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = cfg.model,
                                onValueChange = { v ->
                                    configs = configs + (selectedId to cfg.copy(model = v))
                                },
                                label = { Text("Model") },
                                placeholder = { Text(meta.defaultModel, color = Color.Gray.copy(alpha = 0.5f)) },
                                singleLine = true,
                                trailingIcon = {
                                    if (modelList.isNotEmpty()) {
                                        IconButton(onClick = { showModelDropdown = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = showModelDropdown,
                                onDismissRequest = { showModelDropdown = false },
                                modifier = Modifier.width(300.dp)
                            ) {
                                modelList.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m, fontSize = 12.sp) },
                                        onClick = {
                                            configs = configs + (selectedId to cfg.copy(model = m))
                                            showModelDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // URL (local providers)
                        if (meta.defaultUrl.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cfg.baseUrl,
                                    onValueChange = { v ->
                                        configs = configs + (selectedId to cfg.copy(baseUrl = v))
                                    },
                                    label = { Text(meta.urlLabel) },
                                    placeholder = { Text(meta.defaultUrl, color = Color.Gray.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { onFetchModels(selectedId, cfg.baseUrl) },
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(56.dp).padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Pobierz", fontSize = 11.sp)
                                }
                            }
                            // Preset buttons for LM Studio / Ollama
                            if (meta.id in listOf("lmstudio", "ollama")) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Szybki wybór:", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                                    AssistChip(
                                        onClick = { configs = configs + (selectedId to cfg.copy(baseUrl = meta.defaultUrl)) },
                                        label = { Text("localhost", fontSize = 10.sp) }
                                    )
                                    AssistChip(
                                        onClick = {
                                            val networkUrl = if (meta.id == "lmstudio")
                                                "http://192.168.1.100:1234"
                                            else
                                                "http://192.168.1.100:11434"
                                            configs = configs + (selectedId to cfg.copy(baseUrl = networkUrl))
                                        },
                                        label = { Text("sieć (192.168.1.100)", fontSize = 10.sp) }
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                    Text(
                                        "Zmiana URL wymaga restartu — użyj przycisku \"Aktywuj i restartuj\"",
                                        fontSize = 10.sp, color = Color.Gray
                                    )
                                }
                            }
                        }

                        // API key (cloud providers)
                        if (meta.needsApiKey) {
                            OutlinedTextField(
                                value = cfg.apiKey,
                                onValueChange = { v ->
                                    configs = configs + (selectedId to cfg.copy(apiKey = v))
                                },
                                label = { Text(meta.apiKeyEnv) },
                                placeholder = { Text("sk-…", color = Color.Gray.copy(alpha = 0.5f)) },
                                singleLine = true,
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }, modifier = Modifier.size(20.dp)) {
                                        Icon(
                                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Klucz jest przechowywany lokalnie i przekazywany jako zmienna środowiskowa ${meta.apiKeyEnv}",
                                fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }

                    val meta = PROVIDERS.first { it.id == selectedId }
                    val cfg = configs[selectedId] ?: ProviderConfig()
                    val modelToSend = cfg.model.ifEmpty { meta.defaultModel }

                    if (meta.defaultUrl.isNotEmpty()) {
                        // Providers with URL — show restart button
                        AppTooltip("Aktywuj backend i zrestartuj agent-core z nowym URL") {
                            OutlinedButton(onClick = {
                                onSaveConfigs(configs)
                                onActivateAndRestart(
                                    selectedId,
                                    buildEnvVars(selectedId, cfg, meta),
                                    configs
                                )
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aktywuj i restartuj", fontSize = 12.sp)
                            }
                        }
                    }

                    AppTooltip("Zmień backend/model bez restartu (URL ignorowany)") {
                        Button(onClick = {
                            onSaveConfigs(configs)
                            onActivate(selectedId, modelToSend)
                            onDismiss()
                        }) {
                            Text("Aktywuj")
                        }
                    }
                }
            }
        }
    }
}

// ── Provider row in the left list ──────────────────────────────────────────────

@Composable
private fun ProviderRow(
    meta: ProviderMeta,
    isSelected: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active indicator
        Box(
            modifier = Modifier.size(7.dp).background(
                if (isActive) Color(0xFF4CAF50) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            ).border(
                width = 1.dp,
                color = if (isActive) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                meta.label, fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = meta.badgeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text(
                    meta.badge, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    color = meta.badgeColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        if (isSelected) {
            Icon(
                Icons.Default.KeyboardArrowRight, contentDescription = null,
                modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Build env vars map from a provider config ──────────────────────────────────

private fun buildEnvVars(backend: String, cfg: ProviderConfig, meta: ProviderMeta): Map<String, String> {
    val vars = mutableMapOf<String, String>()
    if (meta.urlEnvVar.isNotEmpty() && cfg.baseUrl.isNotEmpty()) {
        vars[meta.urlEnvVar] = cfg.baseUrl
    }
    if (meta.apiKeyEnv.isNotEmpty() && cfg.apiKey.isNotEmpty()) {
        vars[meta.apiKeyEnv] = cfg.apiKey
    }
    // Model is handled via set_backend IPC command, not env vars
    return vars
}

/** Build combined env vars map for all providers (used at StdioExecutor start). */
fun buildAllEnvVars(providerConfigs: Map<String, com.agentcore.api.ProviderConfig>): Map<String, String> {
    val vars = mutableMapOf<String, String>()
    PROVIDERS.forEach { meta ->
        val cfg = providerConfigs[meta.id] ?: return@forEach
        if (meta.urlEnvVar.isNotEmpty() && cfg.baseUrl.isNotEmpty()) {
            vars[meta.urlEnvVar] = cfg.baseUrl
        }
        if (meta.apiKeyEnv.isNotEmpty() && cfg.apiKey.isNotEmpty()) {
            vars[meta.apiKeyEnv] = cfg.apiKey
        }
    }
    return vars
}
