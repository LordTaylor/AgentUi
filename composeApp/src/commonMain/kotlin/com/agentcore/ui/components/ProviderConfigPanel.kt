// Renders the configuration form (Column 2) for a selected provider inside ProviderDialog.
// Handles model selection, base URL input, API key field, and LM Studio-specific params.
// Internal visibility — only consumed by ProviderDialog.
package com.agentcore.ui.components

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
import com.agentcore.api.ProviderConfig

@Composable
internal fun ProviderConfigPanel(
    selectedId: String,
    meta: ProviderMeta,
    cfg: ProviderConfig,
    availableModels: Map<String, List<String>>,
    onConfigChange: (ProviderConfig) -> Unit,
    onFetchModels: (backend: String, url: String?) -> Unit,
    onLmsLoadModel: (url: String, model: String, config: ProviderConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showApiKey by remember(selectedId) { mutableStateOf(false) }
    var showModelDropdown by remember(selectedId) { mutableStateOf(false) }
    var showLmsParams by remember(selectedId) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Provider title badge + description
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = meta.badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                Text(meta.badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = meta.badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Text(meta.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Text(meta.description, fontSize = 11.sp, color = Color.Gray)

        HorizontalDivider()

        ModelField(
            selectedId = selectedId,
            meta = meta,
            cfg = cfg,
            availableModels = availableModels,
            showModelDropdown = showModelDropdown,
            onShowDropdownChange = { showModelDropdown = it },
            onConfigChange = onConfigChange
        )

        if (meta.defaultUrl.isNotEmpty()) {
            UrlField(
                selectedId = selectedId,
                meta = meta,
                cfg = cfg,
                onConfigChange = onConfigChange,
                onFetchModels = onFetchModels
            )
        }

        if (meta.needsApiKey) {
            ApiKeyField(
                meta = meta,
                cfg = cfg,
                showApiKey = showApiKey,
                onShowApiKeyChange = { showApiKey = it },
                onConfigChange = onConfigChange
            )
        }

        if (selectedId == "lmstudio") {
            LmsSection(
                cfg = cfg,
                showLmsParams = showLmsParams,
                onShowLmsParamsChange = { showLmsParams = it },
                onConfigChange = onConfigChange,
                onLmsLoadModel = onLmsLoadModel
            )
        }
    }
}

@Composable
private fun ModelField(
    selectedId: String,
    meta: ProviderMeta,
    cfg: ProviderConfig,
    availableModels: Map<String, List<String>>,
    showModelDropdown: Boolean,
    onShowDropdownChange: (Boolean) -> Unit,
    onConfigChange: (ProviderConfig) -> Unit
) {
    val modelList = availableModels[selectedId] ?: emptyList()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = cfg.model,
            onValueChange = { onConfigChange(cfg.copy(model = it)) },
            label = { Text("Model") },
            placeholder = { Text(meta.defaultModel, color = Color.Gray.copy(alpha = 0.5f)) },
            singleLine = true,
            trailingIcon = {
                if (modelList.isNotEmpty()) {
                    IconButton(onClick = { onShowDropdownChange(true) }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = showModelDropdown,
            onDismissRequest = { onShowDropdownChange(false) },
            modifier = Modifier.width(300.dp)
        ) {
            modelList.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m, fontSize = 12.sp) },
                    onClick = {
                        onConfigChange(cfg.copy(model = m))
                        onShowDropdownChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun UrlField(
    selectedId: String,
    meta: ProviderMeta,
    cfg: ProviderConfig,
    onConfigChange: (ProviderConfig) -> Unit,
    onFetchModels: (backend: String, url: String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = cfg.baseUrl,
            onValueChange = { onConfigChange(cfg.copy(baseUrl = it)) },
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

    if (meta.id in listOf("lmstudio", "ollama")) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Szybki wybór:", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
            AssistChip(
                onClick = { onConfigChange(cfg.copy(baseUrl = meta.defaultUrl)) },
                label = { Text("localhost", fontSize = 10.sp) }
            )
            AssistChip(
                onClick = {
                    val networkUrl = if (meta.id == "lmstudio") "http://192.168.1.100:1234"
                                     else "http://192.168.1.100:11434"
                    onConfigChange(cfg.copy(baseUrl = networkUrl))
                },
                label = { Text("sieć (192.168.1.100)", fontSize = 10.sp) }
            )
        }
    }
}

@Composable
private fun ApiKeyField(
    meta: ProviderMeta,
    cfg: ProviderConfig,
    showApiKey: Boolean,
    onShowApiKeyChange: (Boolean) -> Unit,
    onConfigChange: (ProviderConfig) -> Unit
) {
    val apiKeyError = cfg.apiKey.isBlank()
    OutlinedTextField(
        value = cfg.apiKey,
        onValueChange = { onConfigChange(cfg.copy(apiKey = it)) },
        label = { Text(meta.apiKeyEnv) },
        placeholder = { Text("sk-…", color = Color.Gray.copy(alpha = 0.5f)) },
        singleLine = true,
        isError = apiKeyError,
        supportingText = if (apiKeyError) {
            { Text("Wymagany klucz API", color = MaterialTheme.colorScheme.error) }
        } else null,
        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { onShowApiKeyChange(!showApiKey) }, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showApiKey) "Ukryj klucz" else "Pokaż klucz",
                    modifier = Modifier.size(14.dp),
                    tint = Color.Gray
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LmsSection(
    cfg: ProviderConfig,
    showLmsParams: Boolean,
    onShowLmsParamsChange: (Boolean) -> Unit,
    onConfigChange: (ProviderConfig) -> Unit,
    onLmsLoadModel: (url: String, model: String, config: ProviderConfig) -> Unit
) {
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
        modifier = Modifier.fillMaxWidth().clickable { onShowLmsParamsChange(!showLmsParams) }.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Dodatkowe parametry (HTTP API)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Icon(if (showLmsParams) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(20.dp))
    }

    if (showLmsParams) {
        LmsParamsPanel(cfg = cfg, onUpdate = onConfigChange)
    }
}
