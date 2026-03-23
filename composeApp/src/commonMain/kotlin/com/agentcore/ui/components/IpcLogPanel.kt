package com.agentcore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit

@Composable
fun IpcLogPanel(
    logs: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // ── Toggle header ──
        AppTooltip(if (expanded) "Zwiń logi IPC" else "Rozwiń logi IPC — pełna komunikacja z backendem") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable { onToggle() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "IPC LOG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${logs.size}",
                    fontSize = 10.sp,
                    color = Color.Gray.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                AppTooltip("Kopiuj logi IPC") {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable {
                                val text = logs.joinToString("\n")
                                Toolkit.getDefaultToolkit().systemClipboard
                                    .setContents(StringSelection(text), null)
                            },
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }
        }
        } // end AppTooltip

        // ── Log content ──
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                items(logs) { entry ->
                    val color = when {
                        entry.contains("  →  ") -> Color(0xFF81C784).copy(alpha = 0.95f)
                        entry.contains("  ←  ") -> Color(0xFF64B5F6).copy(alpha = 0.95f)
                        else -> Color.Gray
                    }
                    Text(
                        text = entry,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = color,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .padding(vertical = 1.dp)
                            .clickable {
                                Toolkit.getDefaultToolkit().systemClipboard
                                    .setContents(StringSelection(entry), null)
                            }
                    )
                }
            }
        }
    }
}
