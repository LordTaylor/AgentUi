// UI support components for the ProviderDialog: SavedConfigsPanel, LmsParamsPanel, and ProviderRow.
// All composables are package-private (internal) — only ProviderDialog and ProviderConfigPanel use them.
package com.agentcore.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.ProviderConfig
import com.agentcore.api.SavedProviderConfig

@Composable
internal fun SavedConfigsPanel(
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
                SavedConfigItem(item = item, onLoad = onLoad, onDelete = onDelete, onSaveAs = onSaveAs)
            }
        }
    }
}

@Composable
private fun SavedConfigItem(
    item: SavedProviderConfig,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSaveAs: (String) -> Unit
) {
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

@Composable
internal fun LmsParamsPanel(
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = cfg.contextLength?.toString() ?: "",
                    onValueChange = { onUpdate(cfg.copy(contextLength = it.toIntOrNull()?.coerceIn(1024, 128000))) },
                    label = { Text("Context Length") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Slider(
                    value = (cfg.contextLength ?: 4096).toFloat().coerceIn(1024f, 128000f),
                    onValueChange = { onUpdate(cfg.copy(contextLength = it.toInt())) },
                    valueRange = 1024f..128000f,
                    steps = 124,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                Checkbox(checked = cfg.flashAttention ?: false, onCheckedChange = { onUpdate(cfg.copy(flashAttention = it)) })
                Text("Flash Attention", fontSize = 12.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = cfg.offloadKvCacheToGpu ?: true, onCheckedChange = { onUpdate(cfg.copy(offloadKvCacheToGpu = it)) })
            Text("Offload KV Cache to GPU", fontSize = 12.sp)
        }
    }
}

@Composable
internal fun ProviderRow(
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
