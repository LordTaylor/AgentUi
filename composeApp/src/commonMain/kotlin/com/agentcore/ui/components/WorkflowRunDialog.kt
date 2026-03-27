// Modal dialog for building and launching AgentGroup workflows (IPC 1.7 A10).
// Supports Sequential, Parallel, and Conditional step types.
// Dispatches ChatIntent.RunWorkflow on confirm; step UI lives in WorkflowStepComponents.kt.

package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.chat.ChatIntent
import kotlinx.coroutines.CoroutineScope

// ── Internal mutable model for the dialog ────────────────────────────────────

internal data class TaskDraft(
    val task: String = "",
    val role: String = "base",
    val inheritContext: Boolean = false
)

internal data class StepDraft(
    val type: String = "sequential",
    val tasks: List<TaskDraft> = listOf(TaskDraft()),
    val fallback: TaskDraft? = null
)

internal val STEP_TYPES = listOf("sequential", "parallel", "conditional")

// ── Main dialog ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowRunDialog(
    onIntent: (ChatIntent, CoroutineScope, ConnectionMode) -> Unit,
    scope: CoroutineScope,
    mode: ConnectionMode,
    onDismiss: () -> Unit
) {
    var steps by remember { mutableStateOf(listOf(StepDraft())) }
    var globalBackend by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 680.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hub, null, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Nowy Workflow Agentów", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = globalBackend,
                    onValueChange = { globalBackend = it },
                    label = { Text("Backend (opcjonalny override dla wszystkich kroków)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(steps) { stepIdx, step ->
                        StepCard(
                            stepNumber = stepIdx + 1,
                            step = step,
                            onChange = { updated -> steps = steps.toMutableList().also { it[stepIdx] = updated } },
                            onRemove = if (steps.size > 1) ({ steps = steps.toMutableList().also { it.removeAt(stepIdx) } }) else null
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = { steps = steps + StepDraft() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dodaj krok", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val payload = buildPayload(steps, globalBackend.takeIf { it.isNotBlank() })
                    onIntent(ChatIntent.RunWorkflow(payload), scope, mode)
                    onDismiss()
                },
                enabled = steps.all { s -> s.tasks.all { it.task.isNotBlank() } }
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Uruchom")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
