// Single row composable for the file/directory tree: icon, name, hover copy button, right-click menu.
// Also exports copyToClipboard (private) and shortenPath (internal) utilities.
// Used by DirectoryNode in FileTree.kt.

package com.agentcore.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileTreeRow(
    name: String,
    isDirectory: Boolean,
    isExpanded: Boolean,
    isSelected: Boolean,
    depth: Int,
    copyPath: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .onClick(
                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                onClick = { showMenu = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        isHovered  -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        else       -> Color.Transparent
                    }
                )
                .clickable(onClick = onClick)
                .padding(start = (depth * 12 + 8).dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand arrow / spacer
            if (isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.Gray.copy(alpha = 0.6f)
                )
            } else {
                Spacer(modifier = Modifier.size(12.dp))
            }
            Spacer(modifier = Modifier.width(3.dp))
            // File / folder icon
            Icon(
                imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (isDirectory) Color(0xFFFFB74D)
                       else if (isSelected) MaterialTheme.colorScheme.primary
                       else Color.Gray.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.width(5.dp))
            // Name
            Text(
                text = name,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.let {
                            if (isDirectory) it else it.copy(alpha = 0.75f)
                        }
            )
            // Copy icon — visible on hover
            if (isHovered) {
                AppTooltip("Kopiuj ścieżkę: $copyPath") {
                    IconButton(
                        onClick = { copyToClipboard(copyPath) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Kopiuj ścieżkę",
                            modifier = Modifier.size(11.dp),
                            tint = Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }
        }

        // Right-click context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Text("Kopiuj ścieżkę", fontSize = 12.sp)
                    }
                },
                onClick = {
                    copyToClipboard(copyPath)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Text("Kopiuj nazwę", fontSize = 12.sp)
                    }
                },
                onClick = {
                    copyToClipboard(File(copyPath).name)
                    showMenu = false
                }
            )
        }
    }
}

private fun copyToClipboard(text: String) {
    try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    } catch (_: Exception) {}
}

internal fun shortenPath(path: String): String {
    val home = System.getProperty("user.home") ?: ""
    return if (path.startsWith(home)) "~${path.removePrefix(home)}" else path
}
