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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.surfaceColorAtElevation

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
    val contextLimit = if (usage != null) extractLong("context_window_limit", usage) else extractLong("context_window_limit", stats)

    val contextPercent = if (contextLimit > 0L) (context * 100 / contextLimit) else 0L

    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier.fillMaxWidth().height(32.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left: Model Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color.Cyan, Color(0xFF00BFA5))
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Spacer(Modifier.width(10.dp))
                val modelDisplay = currentModel.ifEmpty { "UNKNOWN" }.uppercase()
                Text(
                    text = "MODEL: $modelDisplay",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
            }
            
            VerticalDivider(modifier = Modifier.height(14.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Middle: Tokens
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabelValue("SENT", "$inTokens", Color.Gray)
                LabelValue("RECV", "$outTokens", Color.Gray)
            }

            VerticalDivider(modifier = Modifier.height(14.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Context usage with percentage
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CONTEXT: ",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$context / $contextLimit",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(${contextPercent}%)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        contextPercent > 90 -> Color.Red
                        contextPercent > 70 -> Color(0xFFFFA000)
                        else -> Color(0xFF4CAF50)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Right: IPC Status
            Text(
                text = "LOG: ${lastIpc.take(40)}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            VerticalDivider(modifier = Modifier.height(14.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            
            Text(
                text = "0x${(stats?.hashCode() ?: 0).toString(16).uppercase().take(4)}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
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
