package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolOutputPanel(
    output: List<String>,
    isVisible: Boolean,
    onToggle: () -> Unit,
    onClear: () -> Unit
) {
    if (!isVisible) return

    val listState = rememberLazyListState()

    LaunchedEffect(output.size) {
        if (output.isNotEmpty()) {
            listState.animateScrollToItem(output.size - 1)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        color = Color(0xFF1E1E1E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Green
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "TOOL OUTPUT LIVE-STREAM",
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White
                    )
                }
                Row {
                    IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, "Clear", modifier = Modifier.size(14.dp), tint = Color.Gray)
                    }
                    IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Close", modifier = Modifier.size(14.dp), tint = Color.Gray)
                    }
                }
            }

            // Output list
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(output) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    )
                }
            }
        }
    }
}
