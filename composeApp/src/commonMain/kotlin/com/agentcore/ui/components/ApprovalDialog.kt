package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agentcore.api.ApprovalRequestPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Composable
fun ApprovalDialog(
    request: ApprovalRequestPayload,
    onRespond: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onRespond(false) },
        title = { Text("Tool Approval Required") },
        text = {
            Column {
                Text("The agent wants to execute the following tool:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tool: ${request.tool}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Arguments:", style = MaterialTheme.typography.labelSmall)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    val prettyJson = try {
                        val json = Json { prettyPrint = true }
                        json.encodeToString(JsonObject.serializer(), request.args)
                    } catch (e: Exception) {
                        request.args.toString()
                    }
                    Text(
                        text = prettyJson,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRespond(true) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Approve")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onRespond(false) }) {
                Text("Deny")
            }
        }
    )
}
