// Metrics Panel: fetches and renders Prometheus-style metrics from GET /metrics endpoint.
// Shows key counters: total tokens, messages, sessions, tool calls, uptime.
// Polls on open; data is raw text parsed into labelled rows.
package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch

private data class MetricLine(val name: String, val value: String, val isComment: Boolean = false)

@Composable
fun MetricsPanel(
    serverUrl: String = "http://localhost:7700",
    onClose: () -> Unit = {}
) {
    var rawMetrics by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun loadMetrics() {
        scope.launch {
            isLoading = true; error = ""
            try {
                val client = HttpClient()
                val resp = client.get("$serverUrl/metrics")
                rawMetrics = resp.bodyAsText()
                client.close()
            } catch (e: Exception) {
                error = "Failed to fetch metrics: ${e.message}"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadMetrics() }

    val parsedLines = remember(rawMetrics) { parseMetrics(rawMetrics) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PROMETHEUS METRICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                IconButton(onClick = { loadMetrics() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, "Refresh", Modifier.size(16.dp), Color.Gray)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Close", Modifier.size(16.dp), Color.Gray)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        if (error.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF5252).copy(alpha = 0.08f)
            ) {
                Text(error, fontSize = 11.sp, color = Color(0xFFFF5252),
                    modifier = Modifier.padding(12.dp))
            }
        }

        if (rawMetrics.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(parsedLines) { line ->
                    if (line.isComment) {
                        Text(
                            text = line.name,
                            fontSize = 10.sp,
                            color = Color.Gray.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = line.name,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = line.value,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    }
                }
            }
        } else if (!isLoading && error.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No metrics available", fontSize = 13.sp, color = Color.Gray.copy(alpha = 0.6f))
            }
        }
    }
}

private fun parseMetrics(raw: String): List<MetricLine> {
    return raw.lines().mapNotNull { line ->
        when {
            line.isBlank() -> null
            line.startsWith("#") -> MetricLine(line, "", isComment = true)
            else -> {
                val parts = line.split(" ")
                if (parts.size >= 2) MetricLine(parts[0], parts[1])
                else MetricLine(line, "")
            }
        }
    }
}
