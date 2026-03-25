// Modal dialog for building and launching AgentGroup workflows (IPC 1.7 A10).
// Supports Sequential, Parallel, and Conditional step types.
// Dispatches ChatIntent.RunWorkflow on confirm.
// See: CoreApp/docs/communication.md §8 — RunWorkflow command.

package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.*
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.chat.ChatIntent
import kotlinx.coroutines.CoroutineScope

// ── Internal mutable model for the dialog ────────────────────────────────────

private data class TaskDraft(
    val task: String = "",
    val role: String = "base",
    val inheritContext: Boolean = false
)

private data class StepDraft(
    val type: String = "sequential",
    val tasks: List<TaskDraft> = listOf(TaskDraft()),
    val fallback: TaskDraft? = null
)

private val STEP_TYPES = listOf("sequential", "parallel", "conditional")

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

// ── Step card ─────────────────────────────────────────────────────────────────

@Composable
private fun StepCard(
    stepNumber: Int,
    step: StepDraft,
    onChange: (StepDraft) -> Unit,
    onRemove: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Krok $stepNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    STEP_TYPES.forEach { type ->
                        FilterChip(
                            selected = step.type == type,
                            onClick = {
                                onChange(step.copy(
                                    type = type,
                                    fallback = if (type == "conditional") TaskDraft() else null
                                ))
                            },
                            label = { Text(type, fontSize = 9.sp) },
                            modifier = Modifier.height(26.dp).padding(end = 3.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    if (onRemove != null) {
                        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, Modifier.size(14.dp), Color.Gray)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                if (step.type == "conditional") "Primary task" else "Zadania",
                fontSize = 10.sp, color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            step.tasks.forEachIndexed { taskIdx, task ->
                TaskRow(
                    task = task,
                    onChange = { updated -> onChange(step.copy(tasks = step.tasks.toMutableList().also { it[taskIdx] = updated })) },
                    onRemove = if (step.type != "conditional" && step.tasks.size > 1)
                        ({ onChange(step.copy(tasks = step.tasks.toMutableList().also { it.removeAt(taskIdx) })) })
                    else null
                )
                Spacer(Modifier.height(6.dp))
            }

            if (step.type != "conditional") {
                TextButton(
                    onClick = { onChange(step.copy(tasks = step.tasks + TaskDraft())) },
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("+ zadanie", fontSize = 11.sp)
                }
            }

            if (step.type == "conditional" && step.fallback != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fallback (uruchomi się gdy primary padnie)",
                    fontSize = 10.sp, color = Color(0xFFFF9800),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                TaskRow(task = step.fallback, onChange = { onChange(step.copy(fallback = it)) }, onRemove = null)
            }
        }
    }
}

// ── Task row ──────────────────────────────────────────────────────────────────

@Composable
private fun TaskRow(task: TaskDraft, onChange: (TaskDraft) -> Unit, onRemove: (() -> Unit)?) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = task.task,
                onValueChange = { onChange(task.copy(task = it)) },
                label = { Text("Zadanie", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
            OutlinedTextField(
                value = task.role,
                onValueChange = { onChange(task.copy(role = it)) },
                label = { Text("Rola", fontSize = 10.sp) },
                modifier = Modifier.width(90.dp),
                singleLine = true
            )
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp).padding(top = 8.dp)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(14.dp), Color.Gray)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.inheritContext,
                onCheckedChange = { onChange(task.copy(inheritContext = it)) },
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Przekaż output poprzedniego kroku jako kontekst",
                fontSize = 10.sp, color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

// ── Build IPC payload ─────────────────────────────────────────────────────────

private fun buildPayload(steps: List<StepDraft>, globalBackend: String?): RunWorkflowPayload =
    RunWorkflowPayload(
        backend = globalBackend,
        steps = steps.map { s ->
            when (s.type) {
                "parallel" -> WorkflowStepDef.Parallel(s.tasks.map { it.toIpc() })
                "conditional" -> WorkflowStepDef.Conditional(
                    primary = s.tasks.first().toIpc(),
                    fallback = s.fallback?.toIpc()
                )
                else -> WorkflowStepDef.Sequential(s.tasks.map { it.toIpc() })
            }
        }
    )

private fun TaskDraft.toIpc() = WorkflowTaskDef(
    task = task,
    role = role.ifBlank { "base" },
    inherit_context = inheritContext
)
