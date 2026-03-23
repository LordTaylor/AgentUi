package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.SessionInfo

@Composable
fun Sidebar(
    sessions: List<SessionInfo>,
    activeFilters: List<String>,
    onSessionSelect: (String) -> Unit,
    onSessionDelete: (String) -> Unit,
    onSessionPrune: (String) -> Unit,
    onToggleFilter: (String) -> Unit,
    onSessionTag: (String, List<String>) -> Unit,
    workingDir: String = "",
    onFileSelected: (String) -> Unit = {},
    selectedFilePath: String? = null,
    onCollapse: () -> Unit = {},
    onNewSession: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {

        // ── Header with collapse button ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PANEL",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
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

        // ── Sessions (30% of remaining height) ───────────────────────────────
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SESSIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                AppTooltip("Nowa sesja") {
                    IconButton(onClick = onNewSession, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    }
                }
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

            val filteredSessions = if (activeFilters.isEmpty()) sessions
            else sessions.filter { s -> activeFilters.all { f -> s.tags?.contains(f) == true } }

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
                    items(filteredSessions, key = { it.id }) { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSessionSelect(session.id) }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.id.take(8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${session.backend} · ${session.role} · ${session.message_count} msg",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!session.tags.isNullOrEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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
                            IconButton(onClick = { onSessionPrune(session.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Clear, null, Modifier.size(13.dp), Color.Gray.copy(alpha = 0.4f))
                            }
                            IconButton(onClick = { onSessionTag(session.id, (session.tags ?: emptyList()) + "tag") }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Add, null, Modifier.size(13.dp), Color.Gray.copy(alpha = 0.4f))
                            }
                            IconButton(onClick = { onSessionDelete(session.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Delete, null, Modifier.size(13.dp), Color.Gray.copy(alpha = 0.55f))
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // ── Working directory label ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Default.Folder, null, Modifier.size(12.dp), Color(0xFFFFB74D))
            Text(
                text = if (workingDir.isEmpty()) "nie ustawiono" else shortenPath(workingDir),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── File tree (remaining ~70% of height) ──────────────────────────────
        Box(modifier = Modifier.weight(0.7f).fillMaxWidth()) {
            FileTree(
                rootPath = workingDir.ifEmpty { System.getProperty("user.home") ?: "" },
                selectedFilePath = selectedFilePath,
                onFileSelected = onFileSelected
            )
        }
    }
}
