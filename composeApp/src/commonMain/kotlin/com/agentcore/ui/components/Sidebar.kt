package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.SessionInfo
import com.agentcore.ui.components.sidebar.HistoryGrouping
import com.agentcore.ui.components.sidebar.SessionItem
import com.agentcore.ui.components.sidebar.SidebarHeader

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

            val sessionsByFolder = filteredSessions.groupBy { sessionFolders[it.id] }
            
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
                                onSelect = { onSessionSelect(session.id) },
                                onDelete = { onSessionDelete(session.id) },
                                onPrune = { onSessionPrune(session.id) },
                                onTag = { onSessionTag(session.id, (session.tags ?: emptyList()) + "tag") },
                                onMove = { 
                                    // Cycle through some folder names for demo, or we can add a proper list later
                                    val current = sessionFolders[session.id]
                                    val next = when(current) {
                                        null -> "Project A"
                                        "Project A" -> "Research"
                                        "Research" -> "Archive"
                                        else -> null
                                    }
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

@Composable
private fun SessionItem(
    session: SessionInfo,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onPrune: () -> Unit,
    onTag: () -> Unit,
    onMove: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title ?: session.id.take(8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                AppTooltip("Zmień folder") {
                    IconButton(onClick = onMove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Folder, null, Modifier.size(12.dp), Color.Gray.copy(alpha = 0.4f))
                    }
                }
                AppTooltip("Wyczyść historię") {
                    IconButton(onClick = onPrune, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, null, Modifier.size(12.dp), Color.Gray.copy(alpha = 0.4f))
                    }
                }
                AppTooltip("Usuń sesję") {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
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

private fun shortenSidebarPath(path: String): String {
    return path.split("/").takeLast(2).joinToString("/")
}
