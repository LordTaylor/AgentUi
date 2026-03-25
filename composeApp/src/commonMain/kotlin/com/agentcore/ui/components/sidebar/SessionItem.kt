package com.agentcore.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.SessionInfo
import com.agentcore.ui.components.AppTooltip

@Composable
fun SessionItem(
    session: SessionInfo,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onPrune: () -> Unit,
    onMove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    val roleIcon = when (session.role.lowercase()) {
        "coder", "coding", "developer" -> Icons.Default.Code
        "researcher", "research" -> Icons.Default.Search
        "writer" -> Icons.Default.EditNote
        "designer" -> Icons.Default.Brush
        else -> Icons.Default.ChatBubbleOutline
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onSelect() }
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        if (isActive) {
            // Active indicator bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-8).dp)
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(end = 2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = roleIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title ?: session.id.take(8),
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${session.backend} · ${session.message_count} msg",
                        fontSize = 10.sp,
                        color = Color.Gray.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Actions Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray.copy(alpha = 0.4f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Zmień folder", fontSize = 12.sp) },
                        onClick = { showMenu = false; onMove() },
                        leadingIcon = { Icon(Icons.Default.Folder, null, Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Wyczyść historię", fontSize = 12.sp) },
                        onClick = { showMenu = false; onPrune() },
                        leadingIcon = { Icon(Icons.Default.ClearAll, null, Modifier.size(16.dp)) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = { Text("Usuń sesję", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
