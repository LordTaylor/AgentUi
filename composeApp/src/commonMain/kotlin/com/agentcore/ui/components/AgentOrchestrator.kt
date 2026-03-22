package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.AgentGroupPayload
import com.agentcore.api.AgentMetadata

@Composable
fun AgentOrchestrator(
    group: AgentGroupPayload?,
    onAssignTask: (String, String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Agent Orchestrator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (group == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active agent groups", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Team Leader", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    AgentCard(group.leader, isLeader = true, onAssignTask)
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Workers (${group.workers.size})", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                }

                items(group.workers) { worker ->
                    AgentCard(worker, isLeader = false, onAssignTask)
                }
            }
        }
    }
}

@Composable
fun AgentCard(agent: AgentMetadata, isLeader: Boolean, onAssignTask: (String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLeader) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(
                        when (agent.status) {
                            "IDLE" -> Color.Green
                            "BUSY" -> Color.Yellow
                            else -> Color.Gray
                        }
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(agent.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(agent.id, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(Modifier.height(8.dp))
            
            Text(
                text = agent.currentTask ?: "No active task",
                style = MaterialTheme.typography.bodyMedium,
                color = if (agent.currentTask != null) MaterialTheme.colorScheme.primary else Color.Gray,
                maxLines = 1
            )

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CPU: ${(agent.cpuUsage * 100).toInt()}%", fontSize = 10.sp, color = Color.Gray)
                    Text("MEM: ${agent.memoryUsage / 1024 / 1024} MB", fontSize = 10.sp, color = Color.Gray)
                }
                
                if (!isLeader) {
                    Button(
                        onClick = { onAssignTask(agent.id, "New Task") },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Assign", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
