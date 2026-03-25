// Displays real-time progress of an AgentGroup workflow (IPC 1.7 A10).
// Receives AgentWorkflowStatusPayload updates and renders step progress,
// state badge, and active agent IDs. Disappears when no workflow is running.
// See: CoreApp/docs/communication.md §8 — AgentGroup.

package com.agentcore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.AgentWorkflowStatusPayload

private fun stateColor(state: String): Color = when (state) {
    "running"    -> Color(0xFF4CAF50)
    "recovering" -> Color(0xFFFF9800)
    "complete"   -> Color(0xFF2196F3)
    "failed"     -> Color(0xFFF44336)
    else         -> Color.Gray
}

private fun stateIcon(state: String): ImageVector = when (state) {
    "running"    -> Icons.Default.PlayArrow
    "recovering" -> Icons.Default.Refresh
    "complete"   -> Icons.Default.CheckCircle
    "failed"     -> Icons.Default.Warning
    else         -> Icons.Default.Info
}

@Composable
fun AgentGroupWorkflowPanel(
    status: AgentWorkflowStatusPayload?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = status != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (status == null) return@AnimatedVisibility

        val color = stateColor(status.state)
        val progress by animateFloatAsState(
            targetValue = if (status.total_steps > 0) status.step.toFloat() / status.total_steps else 0f,
            label = "workflow_progress"
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = stateIcon(status.state),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Workflow", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = status.state.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "krok ${status.step}/${status.total_steps}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f)
                )

                // Active agents
                if (status.active_agents.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, Modifier.size(12.dp), Color.Gray)
                        status.active_agents.take(4).forEach { agentId ->
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = agentId.take(8),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (status.active_agents.size > 4) {
                            Text("+${status.active_agents.size - 4}", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
