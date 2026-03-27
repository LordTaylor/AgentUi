package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.PluginMetadataPayload

@Composable
fun PluginManager(
    plugins: List<PluginMetadataPayload>,
    onTogglePlugin: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text("PLUGIN MANAGER", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Gray)
            }
        }

        if (plugins.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No plugins found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(plugins) { plugin ->
                    PluginItem(plugin, onTogglePlugin)
                }
            }
        }
    }
}

@Composable
fun PluginItem(
    plugin: PluginMetadataPayload,
    onTogglePlugin: (String, Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = plugin.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "v${plugin.version} by ${plugin.author}", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Text(text = plugin.description, fontSize = 12.sp, style = MaterialTheme.typography.bodySmall)
            }
            
            Switch(
                checked = plugin.isEnabled,
                onCheckedChange = { onTogglePlugin(plugin.name, it) },
                thumbContent = if (plugin.isEnabled) {
                    { Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}
