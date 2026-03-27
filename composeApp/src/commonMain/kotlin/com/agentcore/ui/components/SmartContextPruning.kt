// Smart Context Pruning: interactive tool to identify and remove "context bloat".
// Shows messages sorted by estimated token cost, lets user mark messages for removal.
// Confirmed removals are communicated via onPruneMessages callback.
package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.model.Message
import com.agentcore.model.MessageType

private fun estimateTokens(text: String) = (text.length / 4).coerceAtLeast(1)

@Composable
fun SmartContextPruning(
    messages: List<Message> = emptyList(),
    onPruneMessages: (List<Int>) -> Unit = {},
    onClose: () -> Unit = {}
) {
    var markedForRemoval by remember { mutableStateOf(setOf<Int>()) }
    val sortedByTokens = remember(messages) {
        messages.mapIndexed { idx, msg -> Pair(idx, msg) }
            .sortedByDescending { estimateTokens(it.second.text) }
    }
    val totalTokens = remember(messages) { messages.sumOf { estimateTokens(it.text) } }
    val savingsTokens = markedForRemoval.sumOf { idx -> messages.getOrNull(idx)?.text?.let { estimateTokens(it) } ?: 0 }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("SMART CONTEXT PRUNING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("~$totalTokens tokens total", fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.6f))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Close", Modifier.size(16.dp), Color.Gray)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // Savings bar
        if (markedForRemoval.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Savings: ~$savingsTokens tokens (${markedForRemoval.size} msgs)",
                        fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { onPruneMessages(markedForRemoval.sorted()); markedForRemoval = emptySet() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Prune Selected") }
                }
            }
        }

        Text(
            "Messages sorted by token cost — select bloat to remove:",
            fontSize = 11.sp, color = Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(sortedByTokens) { _, (origIdx, msg) ->
                val tokens = estimateTokens(msg.text)
                val pct = tokens.toFloat() / totalTokens
                val isMarked = origIdx in markedForRemoval
                val barColor = when {
                    pct > 0.15f -> Color(0xFFEF5350)
                    pct > 0.08f -> Color(0xFFFFB300)
                    else -> Color(0xFF4CAF50)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isMarked,
                        onCheckedChange = { checked ->
                            markedForRemoval = if (checked) markedForRemoval + origIdx
                                               else markedForRemoval - origIdx
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(msg.sender, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = if (msg.type == MessageType.ACTION) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.onSurface)
                            Surface(shape = RoundedCornerShape(4.dp), color = barColor.copy(alpha = 0.12f)) {
                                Text("~$tokens tok", fontSize = 9.sp, color = barColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                            }
                        }
                        Text(msg.text.take(80), fontSize = 11.sp, color = Color.Gray,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // Token usage bar
                        Box(modifier = Modifier.fillMaxWidth().height(3.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(pct.coerceAtMost(1f)).height(3.dp)
                                .background(barColor.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
            }
        }
    }
}
