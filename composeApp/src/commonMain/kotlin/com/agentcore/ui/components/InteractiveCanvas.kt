package com.agentcore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.CanvasElement

@Composable
fun InteractiveCanvas(
    elements: List<CanvasElement>,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier.fillMaxSize().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Interactive Canvas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )
                Row {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Canvas")
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .background(Color.White, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    elements.forEach { element ->
                        val color = try {
                            Color(parseColor(element.color))
                        } catch (e: Exception) {
                            Color.Black
                        }

                        when (element.type) {
                            "RECT" -> {
                                drawRect(
                                    color = color,
                                    topLeft = Offset(element.x, element.y),
                                    size = Size(element.width, element.height),
                                    style = Stroke(width = element.strokeWidth)
                                )
                            }
                            "CIRCLE" -> {
                                drawCircle(
                                    color = color,
                                    center = Offset(element.x, element.y),
                                    radius = element.width / 2, // Use width as diameter
                                    style = Stroke(width = element.strokeWidth)
                                )
                            }
                            "LINE" -> {
                                // For lines, width and height are end coordinates relative to x,y or absolute? 
                                // Let's assume absolute for now: x,y to width,height
                                drawLine(
                                    color = color,
                                    start = Offset(element.x, element.y),
                                    end = Offset(element.width, element.height),
                                    strokeWidth = element.strokeWidth
                                )
                            }
                            "TEXT" -> {
                                element.text?.let {
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = it,
                                        topLeft = Offset(element.x, element.y),
                                        style = TextStyle(color = color, fontSize = 14.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseColor(colorString: String): Int {
    if (colorString.startsWith("#")) {
        return colorString.substring(1).toLong(16).toInt() or -0x1000000
    }
    return 0 // Default black
}
