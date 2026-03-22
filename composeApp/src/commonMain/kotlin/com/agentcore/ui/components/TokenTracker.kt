package com.agentcore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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

    val totalTokens = stats["total_tokens"]?.jsonPrimitive?.content ?: "0"
    val cost = stats["cost_estimate"]?.jsonPrimitive?.content ?: "0.00"
    val iterations = stats["iterations"]?.jsonPrimitive?.content ?: "0"

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
            Text("$$cost", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("ITERS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(iterations, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isSummarizing) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            IconButton(
                onClick = onSummarize,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Summarize Context",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
