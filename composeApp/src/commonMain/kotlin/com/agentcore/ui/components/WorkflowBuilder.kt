package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.WorkflowStatusPayload

@Composable
fun WorkflowBuilder(
    workflows: List<WorkflowStatusPayload>,
    onStartWorkflow: (String) -> Unit,
    onStopWorkflow: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text("AGENTIC WORKFLOWS", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Gray)
            }
        }

        if (workflows.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No workflows defined.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workflows) { workflow ->
                    WorkflowItem(workflow, onStartWorkflow, onStopWorkflow)
                }
            }
        }
    }
}

@Composable
fun WorkflowItem(
    workflow: WorkflowStatusPayload,
    onStartWorkflow: (String) -> Unit,
    onStopWorkflow: (String) -> Unit
) {
    val isRunning = workflow.status == "RUNNING"
    val progress = if (workflow.totalSteps > 0) workflow.currentStep.toFloat() / workflow.totalSteps else 0f

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = workflow.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "Status: ${workflow.status}",
                        fontSize = 11.sp,
                        color = when (workflow.status) {
                            "RUNNING" -> Color(0xFF2196F3)
                            "COMPLETED" -> Color(0xFF4CAF50)
                            "FAILED" -> Color(0xFFF44336)
                            else -> Color.Gray
                        }
                    )
                }
                
                if (isRunning) {
                    IconButton(onClick = { onStopWorkflow(workflow.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "Stop", tint = Color(0xFFF44336))
                    }
                } else {
                    IconButton(onClick = { onStartWorkflow(workflow.id) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color(0xFF4CAF50))
                    }
                }
            }
            
            if (isRunning || workflow.status == "COMPLETED" || workflow.status == "FAILED") {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (workflow.status == "FAILED") Color(0xFFF44336) else MaterialTheme.colorScheme.primary,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Step ${workflow.currentStep} of ${workflow.totalSteps}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            workflow.lastError?.let {
                Spacer(Modifier.height(8.dp))
                Text(text = "Error: $it", color = Color(0xFFF44336), fontSize = 11.sp)
            }
        }
    }
}
