package com.agentcore.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import com.agentcore.api.SavedProviderConfig

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
    savedConfigs: Map<String, List<SavedProviderConfig>>,
    onDismiss: () -> Unit,
    onActivate: (backend: String, model: String) -> Unit,
    onActivateAndRestart: (backend: String, envVars: Map<String, String>, updatedConfigs: Map<String, ProviderConfig>) -> Unit,
    onSaveConfigs: (Map<String, ProviderConfig>) -> Unit,
    onSaveNamedConfig: (backend: String, name: String, config: ProviderConfig) -> Unit,
    onDeleteNamedConfig: (backend: String, name: String) -> Unit,
    onLoadNamedConfig: (backend: String, name: String) -> Unit,
    onLmsLoadModel: (url: String, model: String, config: ProviderConfig) -> Unit,
    onFetchModels: (backend: String, url: String?) -> Unit
) {
    var selectedId by remember { mutableStateOf(activeBackend.ifEmpty { "lmstudio" }) }
    var showSavedConfigs by remember { mutableStateOf(false) }
    var showLmsParams by remember { mutableStateOf(false) }
    var newConfigName by remember { mutableStateOf("") }

    // Local editable copy of all provider configs
    var configs by remember(providerConfigs) {
        mutableStateOf(
            PROVIDERS.associate { meta ->
                val existing = providerConfigs[meta.id] ?: ProviderConfig()
                meta.id to ProviderConfig(
                    model = existing.model.ifEmpty { meta.defaultModel },
                    baseUrl = existing.baseUrl.ifEmpty { meta.defaultUrl },
                    apiKey = existing.apiKey,
                    contextLength = existing.contextLength,
                    evalBatchSize = existing.evalBatchSize,
                    flashAttention = existing.flashAttention,
                    numExperts = existing.numExperts,
                    offloadKvCacheToGpu = existing.offloadKvCacheToGpu
                )
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(if (showSavedConfigs) 900.dp else 640.dp),
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

                // Three-column body
                Row(modifier = Modifier.height(500.dp)) {

                    // Column 0: Saved Configs (Collapsible)
                    AnimatedVisibility(
                        visible = showSavedConfigs,
                        enter = expandHorizontally(),
                        exit = shrinkHorizontally()
                    ) {
                        SavedConfigsPanel(
                            backend = selectedId,
                            configs = savedConfigs[selectedId] ?: emptyList(),
                            currentConfig = configs[selectedId] ?: ProviderConfig(),
                            onLoad = { name -> onLoadNamedConfig(selectedId, name) },
                            onDelete = { name -> onDeleteNamedConfig(selectedId, name) },
                            onSaveAs = { name -> onSaveNamedConfig(selectedId, name, configs[selectedId] ?: ProviderConfig()) }
                        )
                    }

                    if (showSavedConfigs) VerticalDivider()

                    // Column 1: provider list
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        // Toggle for saved configs
                        ListItem(
                            headlineContent = { Text("Zapisane konfigi", fontSize = 12.sp) },
                            leadingContent = { Icon(Icons.Default.Bookmark, null, Modifier.size(18.dp)) },
                            trailingContent = { 
                                Icon(
                                    if (showSavedConfigs) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight, 
                                    null, Modifier.size(16.dp)
                                ) 
                            },
                            modifier = Modifier.clickable { showSavedConfigs = !showSavedConfigs }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
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
                    }

                    VerticalDivider()

                    // Column 2: config form for selected provider
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
                        }

                        // LM Studio Specific Section
                        if (selectedId == "lmstudio") {
                            HorizontalDivider()
                            
                            Button(
                                onClick = { onLmsLoadModel(cfg.baseUrl, cfg.model, cfg) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Załaduj model do LM Studio", fontSize = 12.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showLmsParams = !showLmsParams }.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Dodatkowe parametry (HTTP API)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Icon(if (showLmsParams) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(20.dp))
                            }

                            if (showLmsParams) {
                                LmsParamsPanel(
                                    cfg = cfg,
                                    onUpdate = { newCfg ->
                                        configs = configs + (selectedId to newCfg)
                                    }
                                )
                            }
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

// ── Saved Configurations Panel ────────────────────────────────────────────────

@Composable
private fun SavedConfigsPanel(
    backend: String,
    configs: List<SavedProviderConfig>,
    currentConfig: ProviderConfig,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSaveAs: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Zapisane konfiguracje", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        
        // Save Current Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nazwa nowej konfiguracji", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            Button(
                onClick = { if (newName.isNotBlank()) { onSaveAs(newName); newName = "" } },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Save, null, Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Zapisz bieżącą", fontSize = 11.sp)
            }
        }

        HorizontalDivider()

        // List
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (configs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak zapisanych", fontSize = 11.sp, color = Color.Gray)
                }
            }
            configs.forEach { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDelete(item.name) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Text("Model: ${item.config.model}", fontSize = 10.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { onLoad(item.name) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Input, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Wczytaj", fontSize = 10.sp)
                            }
                            TextButton(
                                onClick = { onSaveAs(item.name) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Update, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Aktualizuj", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── LM Studio Parameters Panel ────────────────────────────────────────────────

@Composable
private fun LmsParamsPanel(
    cfg: ProviderConfig,
    onUpdate: (ProviderConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = cfg.contextLength?.toString() ?: "",
                onValueChange = { onUpdate(cfg.copy(contextLength = it.toIntOrNull())) },
                label = { Text("Context Length") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = cfg.evalBatchSize?.toString() ?: "",
                onValueChange = { onUpdate(cfg.copy(evalBatchSize = it.toIntOrNull())) },
                label = { Text("Batch Size") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = cfg.numExperts?.toString() ?: "",
                onValueChange = { onUpdate(cfg.copy(numExperts = it.toIntOrNull())) },
                label = { Text("Num Experts") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = cfg.flashAttention ?: false,
                    onCheckedChange = { onUpdate(cfg.copy(flashAttention = it)) }
                )
                Text("Flash Attention", fontSize = 12.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = cfg.offloadKvCacheToGpu ?: true,
                onCheckedChange = { onUpdate(cfg.copy(offloadKvCacheToGpu = it)) }
            )
            Text("Offload KV Cache to GPU", fontSize = 12.sp)
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
