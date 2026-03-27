// Displays real-time token usage, cost, iteration count and context-window fill %.
// When context_tokens / context_limit are present in stats, shows a LinearProgressIndicator
// that turns orange at >60% and red at >80%; "Compress" button becomes more prominent at >80%.
package com.agentcore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.*

@Composable
fun TokenTracker(
    stats: JsonObject?,
    isSummarizing: Boolean = false,
    onSummarize: () -> Unit = {}
) {
    if (stats == null) return

    val totalTokens  = stats["total_tokens"]?.jsonPrimitive?.content  ?: "0"
    val cost         = stats["cost_estimate"]?.jsonPrimitive?.content ?: "0.00"
    val iterations   = stats["iterations"]?.jsonPrimitive?.content    ?: "0"

    // Context window fill — optional fields emitted by backend when available.
    val ctxTokens = stats["context_tokens"]?.jsonPrimitive?.longOrNull
    val ctxLimit  = stats["context_limit"]?.jsonPrimitive?.longOrNull
    val ctxFraction: Float? = if (ctxTokens != null && ctxLimit != null && ctxLimit > 0)
        (ctxTokens.toFloat() / ctxLimit.toFloat()).coerceIn(0f, 1f)
    else null

    val ctxColor by animateColorAsState(
        targetValue = when {
            ctxFraction == null      -> Color.Gray
            ctxFraction >= 0.80f    -> MaterialTheme.colorScheme.error
            ctxFraction >= 0.60f    -> Color(0xFFFFA000) // amber
            else                    -> MaterialTheme.colorScheme.primary
        },
        label = "ctxColor"
    )
    val isNearLimit = ctxFraction != null && ctxFraction >= 0.80f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text("TOKENS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(totalTokens, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("COST", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text("$$cost", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("ITERS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(iterations, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        // Context window indicator — shown only when backend provides ctx stats.
        if (ctxFraction != null) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(64.dp)
            ) {
                Text(
                    text = "CTX ${(ctxFraction * 100).toInt()}%",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ctxColor
                )
                LinearProgressIndicator(
                    progress = { ctxFraction },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = ctxColor,
                    trackColor = ctxColor.copy(alpha = 0.15f)
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        if (isSummarizing) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            AppTooltip(if (isNearLimit) "Kontekst prawie pełny — skompresuj!" else "Skompresuj kontekst") {
                IconButton(
                    onClick = onSummarize,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compress,
                        contentDescription = "Compress Context",
                        tint = if (isNearLimit) MaterialTheme.colorScheme.error
                               else Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
