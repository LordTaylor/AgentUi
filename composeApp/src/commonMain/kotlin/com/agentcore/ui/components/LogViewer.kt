package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.LogPayload
import kotlinx.coroutines.launch

@Composable
fun LogViewer(
    logs: List<LogPayload>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLevel by remember { mutableStateOf("ALL") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var autoScroll by remember { mutableStateOf(true) }

    val filteredLogs = if (selectedLevel == "ALL") logs else logs.filter { it.level == selectedLevel }

    LaunchedEffect(filteredLogs.size) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(filteredLogs.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(8.dp)
    ) {
        // Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.List, contentDescription = null, tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("ENGINE LOGS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            
            Spacer(Modifier.weight(1f))
            
            // Level Selector
            FilterChip(
                selected = selectedLevel == "ALL",
                onClick = { selectedLevel = "ALL" },
                label = { Text("ALL") },
                colors = FilterChipDefaults.filterChipColors(containerColor = Color.DarkGray, labelColor = Color.LightGray)
            )
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = selectedLevel == "ERROR",
                onClick = { selectedLevel = "ERROR" },
                label = { Text("ERR") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD32F2F), labelColor = Color.White)
            )
            
            Spacer(Modifier.width(16.dp))
            
            IconButton(onClick = { autoScroll = !autoScroll }) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Auto Scroll",
                    tint = if (autoScroll) Color(0xFF4CAF50) else Color.Gray
                )
            }
            
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
            }
        }

        Divider(color = Color(0xFF333333))

        // Log List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(filteredLogs) { log ->
                LogItem(log)
            }
        }
    }
}

@Composable
fun LogItem(log: LogPayload) {
    val levelColor = when (log.level.uppercase()) {
        "ERROR" -> Color(0xFFD32F2F)
        "WARN" -> Color(0xFFFFA000)
        "DEBUG" -> Color(0xFF7B1FA2)
        else -> Color(0xFF43A047)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(
            text = log.timestamp.takeLast(12), // Show only time part
            color = Color.DarkGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.width(85.dp)
        )
        
        Text(
            text = log.level.padEnd(5),
            color = levelColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.width(45.dp)
        )

        log.source?.let {
            Text(
                text = "[$it]",
                color = Color(0xFF3F51B5),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Text(
            text = log.message,
            color = Color.LightGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
