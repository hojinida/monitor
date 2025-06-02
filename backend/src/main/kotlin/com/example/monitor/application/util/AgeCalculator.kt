package com.example.monitor.application.util

import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object AgeCalculator {

    fun computeAgeString(createdStr: String): String {
        val createdAt = OffsetDateTime.parse(createdStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return computeAgeString(createdAt)
    }

    fun computeAgeString(createdAt: OffsetDateTime): String {
        val now = OffsetDateTime.now()
        val diff = Duration.between(createdAt, now)
        val hours = diff.toHours()
        val mins = diff.toMinutes() % 60
        return "${hours}h ${mins}m"
    }
}
