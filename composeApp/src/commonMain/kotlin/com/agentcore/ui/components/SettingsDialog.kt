import com.agentcore.api.BackendInfo

@Composable
fun SettingsDialog(
    currentBackend: String,
    currentRole: String,
    availableBackends: List<BackendInfo>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var backend by remember { mutableStateOf(currentBackend) }
    var role by remember { mutableStateOf(currentRole) }

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
            }
        },
        confirmButton = {
            Button(onClick = { onSave(backend, role) }) {
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
