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
import com.agentcore.api.SkillInfo

private val EXAMPLE_SKILLS = listOf(
    SkillInfo("ex-1", "Code Review",      "Analizuje kod i sugeruje ulepszenia: wydajność, bezpieczeństwo, czytelność.",   "builtin"),
    SkillInfo("ex-2", "Git Assistant",    "Pomaga z Git: commity, PR, rozwiązywanie konfliktów, cherry-pick.",              "builtin"),
    SkillInfo("ex-3", "Test Writer",      "Generuje testy jednostkowe i integracyjne dla podanego kodu Kotlin/Rust.",       "builtin"),
    SkillInfo("ex-4", "Dokumentacja",     "Tworzy dokumentację API, README i KDoc/rustdoc do modułów.",                    "builtin"),
    SkillInfo("ex-5", "Debugger",         "Analizuje błędy i stack trace, proponuje poprawki krok po kroku.",               "builtin"),
    SkillInfo("ex-6", "Refactoring",      "Restrukturyzuje kod wg DRY/SOLID: wydziela funkcje, upraszcza logikę.",         "builtin"),
)

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
fun SkillLibrary(
    tools: List<JsonObject>,
    skills: List<SkillInfo>,
    onReload: () -> Unit,
    onCreateTool: () -> Unit = {},
    onDeleteTool: (String) -> Unit = {}
) {
    var toolToDelete by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Skills Library", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Row {
                IconButton(onClick = onCreateTool) {
                    Icon(Icons.Default.Add, contentDescription = "Create Tool")
                }
                IconButton(onClick = onReload) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload")
                }
            }
        }
        
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Tools (${tools.size})", modifier = Modifier.padding(vertical = 8.dp), fontSize = 13.sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Skills (${skills.size})", modifier = Modifier.padding(vertical = 8.dp), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (selectedTab == 0) {
                items(tools) { tool ->
                    val name = tool["name"]?.jsonPrimitive?.content ?: "Unknown"
                    val desc = tool["description"]?.jsonPrimitive?.content ?: "No description"
                    val needsApproval = tool["requires_approval"]?.jsonPrimitive?.booleanOrNull ?: false
                    
                    SkillCard(
                        name = name,
                        description = desc,
                        isTool = true,
                        needsApproval = needsApproval,
                        onDelete = { toolToDelete = name }
                    )
                }
            } else {
                if (skills.isEmpty()) {
                    item {
                        Text(
                            "Przykładowe wbudowane skille",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(EXAMPLE_SKILLS) { skill ->
                        SkillCard(
                            name = skill.name,
                            description = skill.description,
                            isTool = false,
                            isExample = true,
                            onDelete = null
                        )
                    }
                } else {
                    items(skills) { skill ->
                        SkillCard(
                            name = skill.name,
                            description = skill.description,
                            isTool = false,
                            onDelete = null
                        )
                    }
                }
            }
        }
    }

    // ... AlertDialog logic for tool deletion remains same ...
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

@Composable
private fun SkillCard(
    name: String,
    description: String,
    isTool: Boolean,
    needsApproval: Boolean = false,
    isExample: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = if (isTool) "⚙️" else "📚"
                Text(icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                if (needsApproval) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red) {
                        Text("PAUSE", fontSize = 9.sp)
                    }
                }
                if (isExample) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text("PRZYKŁAD", fontSize = 9.sp)
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
            }
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
                lineHeight = 16.sp
            )
        }
    }
}
