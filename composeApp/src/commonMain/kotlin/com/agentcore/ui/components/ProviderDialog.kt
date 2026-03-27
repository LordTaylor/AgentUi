// Main dialog for selecting and configuring an AI provider/backend.
// Orchestrates the three-column layout: SavedConfigsPanel, provider list, ProviderConfigPanel.
// Delegates all sub-UI to ProviderConfigPanel and ProviderSupportComponents.
package com.agentcore.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.agentcore.api.ProviderConfig
import com.agentcore.api.SavedProviderConfig

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
                DialogHeader(onDismiss = onDismiss)

                HorizontalDivider()

                Row(modifier = Modifier.height(500.dp)) {
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

                    ProviderListColumn(
                        selectedId = selectedId,
                        activeBackend = activeBackend,
                        showSavedConfigs = showSavedConfigs,
                        onToggleSavedConfigs = { showSavedConfigs = !showSavedConfigs },
                        onSelectProvider = { selectedId = it }
                    )

                    VerticalDivider()

                    val meta = PROVIDERS.first { it.id == selectedId }
                    val cfg = configs[selectedId] ?: ProviderConfig()

                    ProviderConfigPanel(
                        selectedId = selectedId,
                        meta = meta,
                        cfg = cfg,
                        availableModels = availableModels,
                        onConfigChange = { newCfg -> configs = configs + (selectedId to newCfg) },
                        onFetchModels = onFetchModels,
                        onLmsLoadModel = onLmsLoadModel,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                DialogButtons(
                    selectedId = selectedId,
                    configs = configs,
                    onDismiss = onDismiss,
                    onActivate = onActivate,
                    onActivateAndRestart = onActivateAndRestart,
                    onSaveConfigs = onSaveConfigs
                )
            }
        }
    }
}

@Composable
private fun DialogHeader(onDismiss: () -> Unit) {
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
}

@Composable
private fun ProviderListColumn(
    selectedId: String,
    activeBackend: String,
    showSavedConfigs: Boolean,
    onToggleSavedConfigs: () -> Unit,
    onSelectProvider: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        ListItem(
            headlineContent = { Text("Zapisane konfigi", fontSize = 12.sp) },
            leadingContent = { Icon(Icons.Default.Bookmark, null, Modifier.size(18.dp)) },
            trailingContent = {
                Icon(
                    if (showSavedConfigs) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
                    null, Modifier.size(16.dp)
                )
            },
            modifier = Modifier.clickable { onToggleSavedConfigs() }
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
                    onClick = { onSelectProvider(meta.id) }
                )
            }
        }
    }
}

@Composable
private fun DialogButtons(
    selectedId: String,
    configs: Map<String, ProviderConfig>,
    onDismiss: () -> Unit,
    onActivate: (backend: String, model: String) -> Unit,
    onActivateAndRestart: (backend: String, envVars: Map<String, String>, updatedConfigs: Map<String, ProviderConfig>) -> Unit,
    onSaveConfigs: (Map<String, ProviderConfig>) -> Unit
) {
    val meta = PROVIDERS.first { it.id == selectedId }
    val cfg = configs[selectedId] ?: ProviderConfig()
    val modelToSend = cfg.model.ifEmpty { meta.defaultModel }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) { Text("Anuluj") }

        if (meta.defaultUrl.isNotEmpty()) {
            OutlinedButton(onClick = {
                onSaveConfigs(configs)
                onActivateAndRestart(selectedId, buildEnvVars(selectedId, cfg, meta), configs)
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
