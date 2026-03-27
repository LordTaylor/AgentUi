// Dialog listing available session checkpoints and allowing restore to a prior state.
// Checkpoints are integer indices returned by list_checkpoints IPC command.
// Triggered via LoadCheckpoints intent → shown when checkpoints list is non-empty.
package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CheckpointRestoreDialog(
    sessionId: String,
    checkpoints: List<Int>,
    onRestore: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingRestore by remember { mutableStateOf<Int?>(null) }

    if (pendingRestore != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            title = { Text("Przywrócić checkpoint ${pendingRestore}?") },
            text = { Text("Historia sesji zostanie cofnięta do punktu #${pendingRestore}. Bieżące wiadomości zostaną usunięte. Tej operacji nie można cofnąć.") },
            confirmButton = {
                Button(
                    onClick = { pendingRestore?.let { onRestore(it) }; pendingRestore = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Przywróć") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Anuluj") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.History, contentDescription = null) },
        title = { Text("Checkpointy sesji") },
        text = {
            Column {
                Text(
                    "Sesja: ${sessionId.take(8)}… • ${checkpoints.size} punkt(ów)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    itemsIndexed(checkpoints) { index, n ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "⏱ Checkpoint #$n",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (index == checkpoints.lastIndex) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) { Text("najnowszy", fontSize = 9.sp) }
                                    Spacer(Modifier.width(8.dp))
                                }
                                OutlinedButton(
                                    onClick = { pendingRestore = n },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) { Text("Przywróć", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zamknij") }
        }
    )
}
