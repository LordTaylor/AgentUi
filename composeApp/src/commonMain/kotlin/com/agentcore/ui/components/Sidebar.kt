package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight().padding(16.dp)) {
        Text("DASHBOARD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        // Tag Filters
        val allTags = sessions.flatMap { it.tags ?: emptyList() }.distinct()
        if (allTags.isNotEmpty()) {
            Text("FILTRY", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    FilterChip(
                        selected = activeFilters.contains(tag),
                        onClick = { onToggleFilter(tag) },
                        label = { Text(tag, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFB283FF).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFB283FF)
                        )
                    )
                }
            }
        }

        Text("SESSIONS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(12.dp))

        val filteredSessions = if (activeFilters.isEmpty()) sessions 
                              else sessions.filter { s -> activeFilters.all { f -> s.tags?.contains(f) == true } }

        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (sessions.isEmpty()) "Brak sesji" else "Brak pasujących sesji",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn {
                items(filteredSessions, key = { it.id }) { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionSelect(session.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.id.take(8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${session.backend} · ${session.role}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${session.message_count} msg",
                                    fontSize = 10.sp,
                                    color = Color.Gray.copy(alpha = 0.6f)
                                )
                            }
                            
                            // Tags display
                            if (!session.tags.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    session.tags.forEach { tag ->
                                        Surface(
                                            color = Color.Gray.copy(alpha = 0.1f),
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = tag,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onSessionPrune(session.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Prune Session",
                                    tint = Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { 
                                    // Basic tagging UX: just append a "new" tag for now or toggle
                                    val currentTags = session.tags ?: emptyList()
                                    onSessionTag(session.id, currentTags + "tag") 
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tag Session",
                                    tint = Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onSessionDelete(session.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Session",
                                    tint = Color.Gray.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}
