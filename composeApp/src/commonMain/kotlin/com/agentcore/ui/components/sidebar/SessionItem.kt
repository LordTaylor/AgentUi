// Full-featured SessionItem composable for the session history sidebar.
// Supports inline rename, pin, export, folder move, prune, and delete with confirmation.
// Used exclusively by Sidebar.kt via com.agentcore.ui.components.sidebar import.

package com.agentcore.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.SessionInfo
import com.agentcore.ui.components.AppTooltip

/** Generates a readable session name from an ISO-8601 timestamp: NS_HH:mm/dd.MM.yy */
internal fun formatSessionName(createdAt: String): String {
    return try {
        val time = createdAt.substringAfter('T').take(5)   // "14:30"
        val date = createdAt.take(10)                       // "2024-01-15"
        val parts = date.split('-')
        val dd = parts[2]
        val mm = parts[1]
        val yy = parts[0].takeLast(2)
        "NS_$time/$dd.$mm.$yy"
    } catch (_: Exception) {
        "NS_${createdAt.take(4).ifBlank { "---" }}"
    }
}

@Composable
internal fun SessionItem(
    session: SessionInfo,
    isActive: Boolean = false,
    isPinned: Boolean = false,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onPrune: () -> Unit,
    onTag: () -> Unit,
    onRename: (String) -> Unit,
    onPin: () -> Unit = {},
    onExport: () -> Unit = {},
    onMove: () -> Unit,
    onCheckpoint: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(session.id) { mutableStateOf(session.title ?: formatSessionName(session.created_at)) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Auto-focus text field when entering edit mode
    LaunchedEffect(isEditing) {
        if (isEditing) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    fun commitRename() {
        runCatching { focusManager.clearFocus() }
        val trimmed = editText.trim()
        if (trimmed.isNotEmpty()) onRename(trimmed)
        isEditing = false
    }

    fun cancelEdit() {
        runCatching { focusManager.clearFocus() }
        isEditing = false
    }

    // Confirm delete dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Usuń sesję", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Czy na pewno chcesz usunąć \"${session.title ?: session.id.take(8)}\"?\nTej operacji nie można cofnąć.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    val activeBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    val activePrimary = MaterialTheme.colorScheme.primary

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isActive) activeBackground else Color.Transparent,
                    RoundedCornerShape(6.dp)
                )
                .clickable(enabled = !isEditing) { onSelect() }
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(32.dp)
                        .background(activePrimary, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    BasicTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onKeyEvent { e ->
                                when {
                                    e.type == KeyEventType.KeyDown && e.key == Key.Enter -> {
                                        commitRename(); true
                                    }
                                    e.type == KeyEventType.KeyDown && e.key == Key.Escape -> {
                                        cancelEdit(); true
                                    }
                                    else -> false
                                }
                            }
                    )
                } else {
                    Text(
                        text = session.title ?: formatSessionName(session.created_at),
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                editText = session.title ?: formatSessionName(session.created_at)
                                isEditing = true
                            })
                        }
                    )
                }
                Text(
                    text = "${session.backend} · ${session.role} · ${session.message_count} msg",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!session.tags.isNullOrEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 2.dp)) {
                        session.tags.forEach { tag ->
                            Surface(
                                color = Color.Gray.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    tag,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Row {
                AppTooltip(if (isEditing) "Zatwierdź nazwę (Enter)" else "Zmień nazwę (dwuklik)") {
                    IconButton(
                        onClick = { if (isEditing) commitRename() else { editText = session.title ?: ""; isEditing = true } },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            null, Modifier.size(12.dp),
                            if (isEditing) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                        )
                    }
                }
                AppTooltip(if (isPinned) "Odepnij sesję" else "Przypnij sesję na górze") {
                    IconButton(onClick = onPin, modifier = Modifier.size(24.dp)) {
                        Text(
                            text = if (isPinned) "📌" else "📍",
                            fontSize = 10.sp
                        )
                    }
                }
                AppTooltip("Eksportuj sesję (.md)") {
                    IconButton(onClick = onExport, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Share, null, Modifier.size(12.dp), Color.Gray.copy(alpha = 0.4f))
                    }
                }
                AppTooltip("Zmień folder") {
                    IconButton(onClick = onMove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Folder, null, Modifier.size(12.dp), Color.Gray.copy(alpha = 0.4f))
                    }
                }
                AppTooltip("Checkpointy — przywróć do wcześniejszego stanu") {
                    IconButton(onClick = onCheckpoint, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.History, null, Modifier.size(12.dp), Color.Gray.copy(alpha = 0.4f))
                    }
                }
                AppTooltip("Wyczyść historię") {
                    IconButton(onClick = onPrune, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, null, Modifier.size(12.dp), Color.Gray.copy(alpha = 0.4f))
                    }
                }
                AppTooltip("Usuń sesję") {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(12.dp), Color.Gray.copy(alpha = 0.5f))
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
