// Root sidebar composable: renders pinned/folder-grouped session history.
// Delegates per-session row rendering to SessionItem (sidebar sub-package).
// FolderHeader renders group labels; utility functions live in SessionItem.kt.

package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.SessionInfo
import com.agentcore.ui.components.sidebar.SessionItem

@Composable
fun Sidebar(
    sessions: List<SessionInfo>,
    activeFilters: List<String>,
    currentSessionId: String?,
    searchText: String,
    onSearchChange: (String) -> Unit,
    onSessionSelect: (String) -> Unit,
    onSessionDelete: (String) -> Unit,
    onSessionPrune: (String) -> Unit,
    onToggleFilter: (String) -> Unit,
    onCollapse: () -> Unit = {},
    onNewSession: () -> Unit = {},
    sessionFolders: Map<String, String> = emptyMap(),
    onMoveToFolder: (String, String?) -> Unit = { _, _ -> },
    onSessionTag: (String, List<String>) -> Unit = { _, _ -> },
    onSessionRename: (String, String) -> Unit = { _, _ -> },
    pinnedSessions: Set<String> = emptySet(),
    onSessionPin: (String) -> Unit = {},
    onSessionExport: (String) -> Unit = {},
    onSessionCheckpoint: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SESJE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AppTooltip("Zwiń panel boczny") {
                IconButton(onClick = onCollapse, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // ── Sessions (full remaining height) ─────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("HISTORIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(6.dp))

            val allTags = sessions.flatMap { it.tags ?: emptyList() }.distinct()
            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    allTags.forEach { tag ->
                        FilterChip(
                            selected = activeFilters.contains(tag),
                            onClick = { onToggleFilter(tag) },
                            label = { Text(tag, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFB283FF).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFFB283FF)
                            )
                        )
                    }
                }
            }

            val filteredSessions = remember(sessions, activeFilters) {
                if (activeFilters.isEmpty()) sessions
                else sessions.filter { s -> activeFilters.all { f -> s.tags?.contains(f) == true } }
            }

            // Pinned sessions always appear first in their own "Przypięte" group
            val pinnedList = filteredSessions.filter { pinnedSessions.contains(it.id) }
            val unpinnedByFolder = filteredSessions.filter { !pinnedSessions.contains(it.id) }.groupBy { sessionFolders[it.id] }
            val sessionsByFolder: Map<String?, List<SessionInfo>> = buildMap {
                if (pinnedList.isNotEmpty()) put("📌 Przypięte", pinnedList)
                putAll(unpinnedByFolder)
            }

            if (filteredSessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (sessions.isEmpty()) "Brak sesji" else "Brak pasujących",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    for ((folderName, sessionsInFolder) in sessionsByFolder) {
                        val displayFolder = folderName ?: "Ogólne"

                        item(key = "folder-$displayFolder") {
                            FolderHeader(displayFolder)
                        }

                        items(sessionsInFolder, key = { it.id }) { session ->
                            SessionItem(
                                session = session,
                                isActive = session.id == currentSessionId,
                                isPinned = pinnedSessions.contains(session.id),
                                onSelect = { onSessionSelect(session.id) },
                                onDelete = { onSessionDelete(session.id) },
                                onPrune = { onSessionPrune(session.id) },
                                onTag = { onSessionTag(session.id, (session.tags ?: emptyList()) + "tag") },
                                onRename = { newTitle -> onSessionRename(session.id, newTitle) },
                                onPin = { onSessionPin(session.id) },
                                onExport = { onSessionExport(session.id) },
                                onCheckpoint = { onSessionCheckpoint(session.id) },
                                onMove = {
                                    val availableFolders = sessionFolders.values
                                        .distinct().filterNotNull().sorted()
                                    val current = sessionFolders[session.id]
                                    val idx = availableFolders.indexOf(current)
                                    val next = if (current == null) availableFolders.firstOrNull()
                                               else availableFolders.getOrNull(idx + 1)
                                    onMoveToFolder(session.id, next)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderHeader(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            letterSpacing = 1.sp
        )
    }
}
