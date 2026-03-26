// Dialog for granular Dev Mode display options.
// Allows toggling which message types are visible in the chat (tool calls, thoughts, sub-agents).
package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentcore.api.DevModeOptions

@Composable
fun ChatDisplaySettingsDialog(
    options: DevModeOptions,
    onDismiss: () -> Unit,
    onApply: (DevModeOptions) -> Unit
) {
    var showToolCalls by remember { mutableStateOf(options.showToolCalls) }
    var showToolOutput by remember { mutableStateOf(options.showToolOutput) }
    var showThoughts by remember { mutableStateOf(options.showThoughts) }
    var showSubAgentMessages by remember { mutableStateOf(options.showSubAgentMessages) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opcje trybu DEV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Wybierz, co jest widoczne w trybie deweloperskim:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                DevOptionRow("Wywołania narzędzi (Tool calls)", showToolCalls) { showToolCalls = it }
                DevOptionRow("Strumieniowy output narzędzi", showToolOutput) { showToolOutput = it }
                DevOptionRow("Myśli modelu (Thoughts)", showThoughts) { showThoughts = it }
                DevOptionRow("Wiadomości sub-agentów", showSubAgentMessages) { showSubAgentMessages = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(DevModeOptions(showToolCalls, showToolOutput, showThoughts, showSubAgentMessages))
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}

@Composable
private fun DevOptionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
