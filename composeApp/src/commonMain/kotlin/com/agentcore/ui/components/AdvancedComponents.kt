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
fun ToolExplorer(tools: List<JsonObject>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Available Tools", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        }
                        Text(desc, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
