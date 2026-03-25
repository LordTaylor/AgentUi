package com.agentcore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.UsagePayload

@Composable
fun TokenAnalyticsDialog(
    history: List<UsagePayload>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Token Usage Analytics") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available yet", color = Color.Gray)
                    }
                } else {
                    Text("Historical Consumption", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TokenChart(history)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val totalIn = history.sumOf { it.input_tokens }
                    val totalOut = history.sumOf { it.output_tokens }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Input", fontSize = 10.sp, color = Color.Gray)
                            Text("$totalIn", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Total Output", fontSize = 10.sp, color = Color.Gray)
                            Text("$totalOut", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Sessions", fontSize = 10.sp, color = Color.Gray)
                            Text("${history.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun TokenChart(history: List<UsagePayload>) {
    val inputColor = MaterialTheme.colorScheme.primary
    val outputColor = MaterialTheme.colorScheme.secondary

    val maxTokens = (history.maxOfOrNull { it.input_tokens + it.output_tokens } ?: 100).coerceAtLeast(100)

    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))) {
        val width = size.width
        val height = size.height
        val spacing = width / (history.size.coerceAtLeast(2) - 1).coerceAtLeast(1)

        val inputPath = Path()
        val outputPath = Path()

        history.forEachIndexed { index, usage ->
            val x = index * spacing
            val inputY = height - (usage.input_tokens.toFloat() / maxTokens * height)
            val outputY = height - (usage.output_tokens.toFloat() / maxTokens * height)

            if (index == 0) {
                inputPath.moveTo(x, inputY)
                outputPath.moveTo(x, outputY)
            } else {
                inputPath.lineTo(x, inputY)
                outputPath.lineTo(x, outputY)
            }
            
            // Draw points
            drawCircle(inputColor, radius = 3.dp.toPx(), center = Offset(x, inputY))
            drawCircle(outputColor, radius = 3.dp.toPx(), center = Offset(x, outputY))
        }

        drawPath(inputPath, inputColor, style = Stroke(width = 2.dp.toPx()))
        drawPath(outputPath, outputColor, style = Stroke(width = 2.dp.toPx()))
    }
    
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(inputColor, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(4.dp))
            Text("Input", fontSize = 10.sp)
        }
        Spacer(Modifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(outputColor, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(4.dp))
            Text("Output", fontSize = 10.sp)
        }
    }
}
