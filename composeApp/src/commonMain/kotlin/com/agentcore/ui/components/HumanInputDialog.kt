package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.agentcore.api.HumanInputPayload

@Composable
fun HumanInputDialog(
    request: HumanInputPayload,
    onRespond: (String) -> Unit
) {
    var answer by remember { mutableStateOf("") }

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier.width(480.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "🤔 Agent pyta",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = request.prompt,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = answer,
                    onValueChange = { answer = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Wpisz odpowiedź…") },
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { if (answer.isNotBlank()) onRespond(answer) },
                        enabled = answer.isNotBlank()
                    ) {
                        Text("Wyślij odpowiedź")
                    }
                }
            }
        }
    }
}
