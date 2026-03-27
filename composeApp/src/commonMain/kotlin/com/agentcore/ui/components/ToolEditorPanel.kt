// Inline tool editor: lists Python tool files from ~/.agentcore/tools/ and provides
// a basic text editor to create or modify them directly from the UI.
// Syntax: monospace BasicTextField with save-to-disk action.
package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private data class ToolFile(val file: File, val name: String)

private val NEW_TOOL_TEMPLATE = """# New tool — implement your logic here
# The function name must match the tool file name.

def run(args: dict) -> str:
    return "Hello from custom tool!"
"""

@Composable
fun ToolEditorPanel(onClose: () -> Unit) {
    val toolsDir = remember { File(System.getProperty("user.home"), ".agentcore/tools") }
    var toolFiles by remember { mutableStateOf(loadToolFiles(toolsDir)) }
    var selected by remember { mutableStateOf<ToolFile?>(null) }
    var editorContent by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var showNewDialog by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf("") }

    LaunchedEffect(selected) {
        editorContent = selected?.file?.readText() ?: ""
        saveStatus = ""
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TOOL EDITOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showNewDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, "New tool", Modifier.size(16.dp), Color.Gray)
                }
                IconButton(onClick = { toolFiles = loadToolFiles(toolsDir) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, "Refresh", Modifier.size(16.dp), Color.Gray)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Close", Modifier.size(16.dp), Color.Gray)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // File list
            LazyColumn(modifier = Modifier.width(160.dp).fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                items(toolFiles) { tf ->
                    val isSelected = selected?.name == tf.name
                    Text(
                        text = tf.name,
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().clickable { selected = tf }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                if (toolFiles.isEmpty()) {
                    item {
                        Text("No tools found", fontSize = 11.sp, color = Color.Gray,
                            modifier = Modifier.padding(12.dp))
                    }
                }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            // Editor
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (selected != null) {
                    // Save bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selected?.name ?: "", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (saveStatus.isNotEmpty()) Text(saveStatus, fontSize = 10.sp, color = Color(0xFF4CAF50))
                            Button(onClick = {
                                selected?.file?.writeText(editorContent)
                                saveStatus = "Saved ✓"
                            }, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                Text("Save", fontSize = 11.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    BasicTextField(
                        value = editorContent,
                        onValueChange = { editorContent = it; saveStatus = "" },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Code, null, Modifier.size(40.dp), Color.Gray.copy(alpha = 0.4f))
                            Spacer(Modifier.height(8.dp))
                            Text("Select a tool to edit", fontSize = 13.sp, color = Color.Gray.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        AlertDialog(
            onDismissRequest = { showNewDialog = false; newFileName = "" },
            title = { Text("New Tool File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("Tool name (no .py)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFileName.isNotBlank()) {
                        toolsDir.mkdirs()
                        val f = File(toolsDir, "$newFileName.py").also { it.writeText(NEW_TOOL_TEMPLATE) }
                        toolFiles = loadToolFiles(toolsDir)
                        selected = ToolFile(f, f.name)
                    }
                    showNewDialog = false; newFileName = ""
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewDialog = false; newFileName = "" }) { Text("Cancel") } }
        )
    }
}

private fun loadToolFiles(dir: File): List<ToolFile> =
    dir.listFiles { f -> f.extension == "py" }
        ?.sortedBy { it.name }
        ?.map { ToolFile(it, it.name) }
        ?: emptyList()
