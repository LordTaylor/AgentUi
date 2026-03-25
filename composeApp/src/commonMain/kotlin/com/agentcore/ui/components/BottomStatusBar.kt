package com.agentcore.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Bottom status bar showing model info, token usage, and IPC status.
 */
@Composable
fun BottomStatusBar(stats: JsonObject?, lastIpc: String, currentModel: String) {
    fun extractLong(key: String, obj: JsonObject?): Long {
        if (obj == null) return 0L
        val element = obj[key] ?: return 0L
        return when (element) {
            is JsonPrimitive -> {
                if (element.isString) element.content.toLongOrNull() ?: 0L
                else element.longOrNull ?: element.doubleOrNull?.toLong() ?: 0L
            }
            else -> 0L
        }
    }

    val usage = stats?.get("usage")?.let { if (it is JsonObject) it else null }
    
    val inTokens = if (usage != null) extractLong("input_tokens", usage) else extractLong("input_tokens", stats)
    val outTokens = if (usage != null) extractLong("output_tokens", usage) else extractLong("output_tokens", stats)
    val context = if (usage != null) extractLong("context_window_tokens", usage) else extractLong("context_window_tokens", stats)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().height(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Model and Tokens
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(Color.Cyan, RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(8.dp))
                val modelDisplay = currentModel.ifEmpty { "UNKNOWN" }.uppercase()
                Text("MODEL: $modelDisplay", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            
            Text("TOKENS: $inTokens ↑ $outTokens ↓", fontSize = 9.sp, color = Color.Gray)
            Text("CONTEXT: $context", fontSize = 9.sp, color = Color.Gray)

            Spacer(modifier = Modifier.weight(1f))

            Text("IPC: ${lastIpc.take(35)}", fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.7f))
            
            VerticalDivider(modifier = Modifier.height(12.dp), color = Color.Gray.copy(alpha = 0.2f))
            
            Text("THREAD: 0x${(stats?.hashCode() ?: 0).toString(16).uppercase().take(4)}", fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Preview
@Composable
fun BottomStatusBarPreview() {
    MaterialTheme {
        BottomStatusBar(
            stats = null,
            lastIpc = "Preview IPC status",
            currentModel = "gpt-4-preview"
        )
    }
}
