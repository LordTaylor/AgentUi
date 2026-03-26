package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutoAcceptDialog(
    currentAutoAccept: Boolean,
    currentBypassAll: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit
) {
    var autoAccept by remember { mutableStateOf(currentAutoAccept) }
    var bypassAll by remember { mutableStateOf(currentBypassAll) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Ustawienia Automatyzacji") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Decydujesz o poziomie kontroli nad działaniami Agenta.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoAccept, onCheckedChange = { autoAccept = it })
                    Column {
                        Text("Auto-akceptacja (approval_mode)", fontWeight = FontWeight.Bold)
                        Text("Agent nie będzie pytał o potwierdzenie narzędzi takich jak odczyt plików.", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = bypassAll, 
                        onCheckedChange = { bypassAll = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bypass ALL (UNSAFE)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Surface(color = MaterialTheme.colorScheme.error, shape = MaterialTheme.shapes.extraSmall) {
                                Text("EXPERIMENTAL", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 8.sp, color = Color.White)
                            }
                        }
                        Text("Całkowite pominięcie uprawnień systemowych. Agent może wykonywać dowolne akcje (bash, delete) bez żadnej blokady.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
                
                if (bypassAll) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⚠️ OSTRZEŻENIE: Tryb Bypass ALL pozwala agentowi na niekontrolowany dostęp do Twojego systemu. Używaj tylko w zaufanym środowisku!",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(autoAccept, bypassAll) },
                colors = if (bypassAll) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
            ) {
                Text("Zastosuj Ustawienia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
