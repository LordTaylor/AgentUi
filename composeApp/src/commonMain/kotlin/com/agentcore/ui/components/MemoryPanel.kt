// A12: Enhanced KV Store UI — panel for viewing and deleting agent memory facts.
// Opened via ToggleMemoryPanel intent; facts are loaded via list_memory IPC command.
// Delete emits delete_memory, which triggers a refreshed memory_list event.

package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MemoryPanel(
    sessionId: String?,
    facts: Map<String, String>,
    onRefresh: () -> Unit,
    onDeleteKey: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agent Memory",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    AppTooltip("Odśwież") {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    AppTooltip("Zamknij") {
                        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (sessionId == null) {
                Text("Brak aktywnej sesji", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                return@Column
            }

            if (facts.isEmpty()) {
                Text("Brak zapamiętanych faktów.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(facts.entries.toList(), key = { it.key }) { (key, value) ->
                        MemoryFactRow(key = key, value = value, onDelete = { onDeleteKey(key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryFactRow(key: String, value: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(key, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(value, fontSize = 10.sp, color = Color.Gray, maxLines = 2)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Usuń $key", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
        }
    }
    Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}
