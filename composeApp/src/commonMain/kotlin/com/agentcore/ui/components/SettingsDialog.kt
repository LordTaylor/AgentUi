package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agentcore.api.BackendInfo

@Composable
fun SettingsDialog(
    currentBackend: String,
    currentRole: String,
    initialSystemPrompt: String = "",
    availableBackends: List<BackendInfo>,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var backend by remember { mutableStateOf<String>(currentBackend) }
    var role by remember { mutableStateOf<String>(currentRole) }
    var systemPrompt by remember { mutableStateOf<String>(initialSystemPrompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agent Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select Backend", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (availableBackends.isEmpty()) {
                        OutlinedTextField(
                            value = backend,
                            onValueChange = { backend = it },
                            label = { Text("Backend (ollama/claude/gpt)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        availableBackends.forEach { info ->
                            FilterChip(
                                selected = backend == info.name,
                                onClick = { backend = info.name },
                                label = { Text(info.name.uppercase()) },
                                enabled = info.is_available,
                                leadingIcon = if (backend == info.name) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role (base/expert/coder)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt (Session specific)") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(backend, role, systemPrompt) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
