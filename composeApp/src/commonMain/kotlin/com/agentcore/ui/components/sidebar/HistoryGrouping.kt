package com.agentcore.ui.components.sidebar

import com.agentcore.api.SessionInfo
import kotlinx.datetime.*

data class HistoryGroup(
    val title: String,
    val sessions: List<SessionInfo>
)

object HistoryGrouping {
    fun groupSessions(sessions: List<SessionInfo>): List<HistoryGroup> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        val today = mutableListOf<SessionInfo>()
        val yesterday = mutableListOf<SessionInfo>()
        val thisWeek = mutableListOf<SessionInfo>()
        val older = mutableListOf<SessionInfo>()

        sessions.sortedByDescending { it.created_at }.forEach { session ->
            try {
                // Parse ISO 8601 (e.g. 2024-03-25T12:34:56Z)
                val sessionDate = Instant.parse(session.created_at)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                
                val daysDiff = now.toEpochDays() - sessionDate.toEpochDays()
                
                when {
                    daysDiff == 0 -> today.add(session)
                    daysDiff == 1 -> yesterday.add(session)
                    daysDiff < 7 -> thisWeek.add(session)
                    else -> older.add(session)
                }
            } catch (e: Exception) {
                older.add(session) // Fallback for malformed or empty timestamps
            }
        }

        return listOf(
            HistoryGroup("Dziś", today),
            HistoryGroup("Wczoraj", yesterday),
            HistoryGroup("Ten tydzień", thisWeek),
            HistoryGroup("Wcześniej", older)
        ).filter { it.sessions.isNotEmpty() }
    }
}
