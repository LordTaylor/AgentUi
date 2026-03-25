package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown

@Composable
fun agentMarkdownComponents(
    onRunCode: (String) -> Unit = {},
    onRunInTerminal: (String) -> Unit = {}
): MarkdownComponents {
    return markdownComponents(
        codeBlock = { model ->
            CodeBlockWithActions(model.content, onRunCode, onRunInTerminal)
        },
        codeFence = { model ->
            CodeBlockWithActions(model.content, onRunCode, onRunInTerminal)
        }
    )
}

@Composable
private fun CodeBlockWithActions(
    code: String,
    onRunCode: (String) -> Unit,
    onRunInTerminal: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
    ) {
        Column {
            // Header with buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(code)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(14.dp), tint = Color.Gray)
                }
                IconButton(
                    onClick = { onRunCode(code) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "Run", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = { onRunInTerminal(code) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = "Run in Terminal", modifier = Modifier.size(14.dp), tint = Color.Green)
                }
            }
            
            // Code content or Mermaid Placeholder
            if (code.startsWith("graph") || code.startsWith("sequenceDiagram") || code.startsWith("classDiagram") || code.startsWith("mermaid")) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Cyan)
                        Spacer(Modifier.height(8.dp))
                        Text("MERMAID DIAGRAM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
                        Text(code.take(100) + "...", fontSize = 9.sp, color = Color.Gray, maxLines = 2)
                    }
                }
            } else {
                Box(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
