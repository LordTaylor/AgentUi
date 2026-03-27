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
    uiSettings: com.agentcore.api.UiSettings,
    onUpdateUiSettings: (com.agentcore.api.UiSettings) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var backend by remember { mutableStateOf<String>(currentBackend) }
    var role by remember { mutableStateOf<String>(currentRole) }
    var systemPrompt by remember { mutableStateOf<String>(initialSystemPrompt) }
    var chatFontSize by remember { mutableStateOf(uiSettings.chatFontSize) }

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

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.1f))

                Text("Agent Behaviour", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ReAct Mode", style = MaterialTheme.typography.bodyMedium)
                        Text("Pokaż wewnętrzne przemyślenia agenta (Thought: bloki)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = uiSettings.reactMode,
                        onCheckedChange = { onUpdateUiSettings(uiSettings.copy(reactMode = it)) }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.1f))

                Text("Chat Appearance", style = MaterialTheme.typography.labelMedium, color = Color.Gray)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Font Size: ${chatFontSize.toInt()}sp", modifier = Modifier.width(100.dp))
                    Slider(
                        value = chatFontSize,
                        onValueChange = {
                            chatFontSize = it
                            onUpdateUiSettings(uiSettings.copy(chatFontSize = it))
                        },
                        valueRange = 10f..24f,
                        modifier = Modifier.weight(1f)
                    )
                }
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
