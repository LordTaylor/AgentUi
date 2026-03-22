package com.agentcore.shared

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    private val dayMonthFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")
        .withZone(ZoneId.systemDefault())

    fun formatTime(timestamp: Long): String {
        return try {
            timeFormatter.format(Instant.ofEpochMilli(timestamp))
        } catch (e: Exception) {
            "--:--"
        }
    }

    fun formatRelativeTime(timestamp: Long): String {
        return try {
            val now = System.currentTimeMillis()
            val diffMs = now - timestamp
            val diffSec = diffMs / 1000
            val diffMin = diffSec / 60

            when {
                diffSec < 60 -> "teraz"
                diffMin < 60 -> "${diffMin}m temu"
                else -> {
                    val zone = ZoneId.systemDefault()
                    val msgDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                    val today = LocalDate.now(zone)
                    val yesterday = today.minusDays(1)
                    when (msgDate) {
                        today -> timeFormatter.format(Instant.ofEpochMilli(timestamp))
                        yesterday -> "wczoraj " + timeFormatter.format(Instant.ofEpochMilli(timestamp))
                        else -> dayMonthFormatter.format(Instant.ofEpochMilli(timestamp))
                    }
                }
            }
        } catch (e: Exception) {
            "--:--"
        }
    }
}
