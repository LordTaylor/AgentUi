package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HelpItem(
    val title: String,
    val description: String,
    val example: String? = null
)

@Composable
fun HelpSystem(
    onClose: () -> Unit,
    onTryExample: (String) -> Unit
) {
    val helpItems = listOf(
        HelpItem("Chat & Tools", "Interact with the agent using natural language. The agent can use tools to perform actions.", "Help me analyze this directory."),
        HelpItem("Log Viewer", "Real-time inspection of internal engine logs with level filtering (Info, Error, etc.).", null),
        HelpItem("Terminal Inspector", "Monitor raw IPC traffic between the UI and the agent core for debugging.", null),
        HelpItem("Scratchpad", "A persistent place for notes and code snippets. Changes are saved automatically.", "Write a python script for Fibonacci sequence in the scratchpad."),
        HelpItem("Workspace Indexing", "Monitor Local RAG progress. Indexing allows the agent to search your files semantically.", "Rescan the workspace for new files."),
        HelpItem("Plugin Manager", "Enable or disable custom tool extensions and UI themes.", null),
        HelpItem("Agentic Workflows", "Visual orchestrator for multi-step tasks and automated loops.", "Start the 'Security Audit' workflow."),
        HelpItem("Voice Interaction", "Use the microphone to dictate messages and enable auto-TTS to hear responses.", null),
        HelpItem("Interactive Canvas", "A shared space for drawing prototypes and architectural diagrams.", "Draw a system architecture diagram on the canvas.")
    )

    Surface(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Agent UI Help & Examples",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(helpItems) { item ->
                    HelpCard(item, onTryExample)
                }
                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun HelpCard(item: HelpItem, onTryExample: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            item.example?.let { example ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\"$example\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onTryExample(example) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Try it", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
