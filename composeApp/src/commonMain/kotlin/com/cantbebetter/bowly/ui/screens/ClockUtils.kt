package com.cantbebetter.bowly.ui.screens

import kotlin.random.Random
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

// Wspólny mock zegara dla KMP
object Clock {
    private var lastNow: Long = 1715424000000L

    fun now(): Long {
        lastNow += 1
        return lastNow
    }

    // Dla ID wymagających większej unikalności w krótkim czasie
    fun uniqueId(): String {
        return "${now()}_${Random.nextInt(1000, 9999)}"
    }

    fun formatToApiDate(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val year = dateTime.year
        val month = dateTime.monthNumber.toString().padStart(2, '0')
        val day = dateTime.dayOfMonth.toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    fun getTodayMillis(): Long {
        val now = now()
        val instant = Instant.fromEpochMilliseconds(now)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val todayStart = LocalDateTime(dateTime.year, dateTime.monthNumber, dateTime.dayOfMonth, 0, 0, 0, 0)
        return todayStart.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
    
    object System {
        fun now(): kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now()
    }
}
