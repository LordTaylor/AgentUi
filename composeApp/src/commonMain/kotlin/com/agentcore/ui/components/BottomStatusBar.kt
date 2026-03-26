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
import com.agentcore.api.UsagePayload
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.surfaceColorAtElevation

/**
 * Bottom status bar showing model info, cumulative token usage, and IPC status.
 * SENT = sum of input_tokens across all messages; RECV = sum of output_tokens.
 */
@Composable
fun BottomStatusBar(tokenHistory: List<UsagePayload>, lastIpc: String, currentModel: String) {
    val inTokens  = tokenHistory.sumOf { it.input_tokens.toLong() }
    val outTokens = tokenHistory.sumOf { it.output_tokens.toLong() }
    val msgCount  = tokenHistory.size
    // Latest context window size (input_tokens of last message ≈ total context sent)
    val lastCtx   = tokenHistory.lastOrNull()?.input_tokens ?: 0
    // Tokens generated in the last reply
    val lastOut   = tokenHistory.lastOrNull()?.output_tokens ?: 0

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

            // Middle: Cumulative totals + per-message context/delta
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabelValue("SENT", "$inTokens", Color.Gray)
                LabelValue("RECV", "$outTokens", Color.Gray)
                if (msgCount > 0) LabelValue("MSGS", "$msgCount", Color.Gray)
                if (lastCtx > 0) LabelValue("CTX",  "$lastCtx",  Color(0xFF7CB9E8))
                if (lastOut > 0) LabelValue("+OUT",  "$lastOut",  Color(0xFF7BC67E))
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
            tokenHistory = listOf(UsagePayload(1200, 340), UsagePayload(800, 220)),
            lastIpc = "Preview IPC status",
            currentModel = "gpt-4-preview"
        )
    }
}
