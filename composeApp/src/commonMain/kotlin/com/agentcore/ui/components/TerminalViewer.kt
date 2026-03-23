package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.TerminalTrafficPayload
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit

private fun copyText(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}

@Composable
fun TerminalViewer(
    traffic: List<TerminalTrafficPayload>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(traffic.size) {
        if (traffic.isNotEmpty()) {
            listState.animateScrollToItem(traffic.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("TERMINAL TRAFFIC", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.weight(1f))
            // Copy all button
            AppTooltip("Kopiuj wszystko") {
                IconButton(onClick = {
                    val all = traffic.joinToString("\n") { item ->
                        val prefix = if (item.direction == "IN") "<<<" else ">>>"
                        "[${item.timestamp}] $prefix ${item.data}"
                    }
                    copyText(all)
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopiuj wszystko", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(traffic) { item ->
                TrafficItem(item, onCopy = { copyText("${item.data}") })
            }
        }
    }
}

@Composable
fun TrafficItem(item: TerminalTrafficPayload, onCopy: () -> Unit = {}) {
    val color = if (item.direction == "IN") Color(0xFF4CAF50) else Color(0xFF2196F3)
    val prefix = if (item.direction == "IN") "<<<" else ">>>"

    Column(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clickable { onCopy() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[${item.timestamp}]",
                color = Color.DarkGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = prefix,
                color = color,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = item.data,
            color = Color.LightGray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 4.dp)
        )
        HorizontalProvider(Modifier.height(1.dp).fillMaxWidth().background(Color(0xFF1A1A1A)))
    }
}

@Composable
private fun HorizontalProvider(modifier: Modifier) {
    Box(modifier = modifier)
}
