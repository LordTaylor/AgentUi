// Local Model Manager: UI for browsing, downloading and monitoring Ollama/LM Studio models.
// Shows model name, size, quantization; allows pull and delete actions via shell commands.
// Uses IPC list_backends + Ollama REST API (localhost:11434) for model info.
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
import kotlinx.serialization.json.*

private data class OllamaModel(val name: String, val sizeMb: Long, val family: String)

@Composable
fun LocalModelManager(onClose: () -> Unit = {}) {
    var models by remember { mutableStateOf<List<OllamaModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var pullModelName by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            isLoading = true
            models = loadOllamaModels()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("LOCAL MODELS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("Ollama · localhost:11434", fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.6f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                IconButton(onClick = { refresh() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp), Color.Gray)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, Modifier.size(16.dp), Color.Gray)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // Pull model
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = pullModelName,
                onValueChange = { pullModelName = it },
                placeholder = { Text("llama3:8b", fontSize = 12.sp) },
                label = { Text("Pull model") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            )
            Button(
                onClick = {
                    scope.launch {
                        statusMsg = "Pulling ${pullModelName}…"
                        try {
                            Runtime.getRuntime().exec(arrayOf("ollama", "pull", pullModelName)).waitFor()
                            statusMsg = "Pull complete ✓"
                            refresh()
                        } catch (e: Exception) { statusMsg = "Error: ${e.message}" }
                    }
                },
                enabled = pullModelName.isNotBlank(),
                modifier = Modifier.height(56.dp)
            ) { Text("Pull") }
        }

        if (statusMsg.isNotEmpty()) {
            Text(statusMsg, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        Text("Installed Models (${models.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (models.isEmpty() && !isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Ollama not running or no models installed",
                            fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.6f))
                    }
                }
            }
            items(models) { m ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, null, Modifier.size(18.dp), Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(m.name, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace)
                            Text(formatModelSize(m.sizeMb), fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF7C4DFF).copy(alpha = 0.1f)
                    ) {
                        Text(m.family.ifEmpty { "llm" }, fontSize = 9.sp,
                            color = Color(0xFF7C4DFF),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
            }
        }
    }
}

private fun formatModelSize(sizeMb: Long) = when {
    sizeMb > 1024 -> "%.1f GB".format(sizeMb / 1024.0)
    else -> "$sizeMb MB"
}

private suspend fun loadOllamaModels(): List<OllamaModel> = try {
    val client = HttpClient()
    val resp = client.get("http://localhost:11434/api/tags")
    client.close()
    val json = Json { ignoreUnknownKeys = true }
    val obj = json.decodeFromString<JsonObject>(resp.bodyAsText())
    val arr = obj["models"]?.jsonArray ?: return emptyList()
    arr.mapNotNull { el ->
        val o = el.jsonObject
        OllamaModel(
            name = o["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
            sizeMb = (o["size"]?.jsonPrimitive?.longOrNull ?: 0L) / (1024 * 1024),
            family = o["details"]?.jsonObject?.get("family")?.jsonPrimitive?.contentOrNull ?: ""
        )
    }
} catch (_: Exception) { emptyList() }
