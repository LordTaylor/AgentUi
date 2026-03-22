package com.agentcore.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    onSessionSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight().padding(16.dp)) {
        Text("DASHBOARD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        Text("SESSIONS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Brak sesji",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn {
                items(sessions, key = { it.id }) { session ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionSelect(session.id) }
                            .padding(vertical = 10.dp)
                    ) {
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
                        if (session.created_at.isNotEmpty()) {
                            Text(
                                text = session.created_at.take(10),
                                fontSize = 9.sp,
                                color = Color.Gray.copy(alpha = 0.4f)
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}
