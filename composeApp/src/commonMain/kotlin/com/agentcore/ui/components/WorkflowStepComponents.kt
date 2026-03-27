// Step-level composables and payload builders for the WorkflowRunDialog.
// Contains StepCard, TaskRow, buildPayload, and TaskDraft.toIpc() extension.
// Internal data types TaskDraft and StepDraft are defined in WorkflowRunDialog.kt.

package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
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

@Composable
internal fun StepCard(
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

@Composable
internal fun TaskRow(task: TaskDraft, onChange: (TaskDraft) -> Unit, onRemove: (() -> Unit)?) {
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

internal fun buildPayload(steps: List<StepDraft>, globalBackend: String?): RunWorkflowPayload =
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

internal fun TaskDraft.toIpc() = WorkflowTaskDef(
    task = task,
    role = role.ifBlank { "base" },
    inherit_context = inheritContext
)
