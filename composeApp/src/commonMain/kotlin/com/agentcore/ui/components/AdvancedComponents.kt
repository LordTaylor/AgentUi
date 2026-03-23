package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import kotlinx.serialization.json.*

@Composable
fun StatsDashboard(stats: JsonObject) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Session Metrics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            StatRow("Tokens (In/Out)", "${stats["input_tokens"]?.jsonPrimitive?.content ?: "0"} / ${stats["output_tokens"]?.jsonPrimitive?.content ?: "0"}")
            StatRow("Iterations", stats["agent_iterations"]?.jsonPrimitive?.content ?: "0")
            StatRow("Tool Calls", stats["tool_calls_count"]?.jsonPrimitive?.content ?: "0")
            
            val duration = stats["total_duration_ms"]?.jsonPrimitive?.longOrNull ?: 0L
            StatRow("Duration", "${duration / 1000}s")
            
            val cost = stats["cost_estimate_usd"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            StatRow("Est. Cost", if (cost > 0) "$${"%.4f".format(cost)}" else "FREE (Local)")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}



@Composable
fun ToolExplorer(
    tools: List<JsonObject>,
    onReloadTools: () -> Unit,
    onCreateTool: () -> Unit = {},
    onDeleteTool: (String) -> Unit = {}
) {
    var toolToDelete by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Available Tools", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row {
                IconButton(onClick = onCreateTool) {
                    Icon(Icons.Default.Add, contentDescription = "Create Tool")
                }
                IconButton(onClick = onReloadTools) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload Tools")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tools) { tool ->
                val name = tool["name"]?.jsonPrimitive?.content ?: "Unknown"
                val desc = tool["description"]?.jsonPrimitive?.content ?: "No description"
                val needsApproval = tool["requires_approval"]?.jsonPrimitive?.booleanOrNull ?: false
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (needsApproval) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red) {
                                    Text("PAUSE", fontSize = 9.sp)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { toolToDelete = name }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = Color.Gray)
                            }
                        }
                        Text(desc, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }

    if (toolToDelete != null) {
        AlertDialog(
            onDismissRequest = { toolToDelete = null },
            title = { Text("Delete Tool?") },
            text = { Text("Are you sure you want to delete tool '${toolToDelete}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTool(toolToDelete!!)
                        toolToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { toolToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
