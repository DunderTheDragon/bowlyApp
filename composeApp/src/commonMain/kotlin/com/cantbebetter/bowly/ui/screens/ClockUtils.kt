package com.cantbebetter.bowly.ui.screens

import kotlin.random.Random
import kotlinx.datetime.Clock as DateTimeClock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object Clock {
    fun now(): Long = DateTimeClock.System.now().toEpochMilliseconds()

    fun uniqueId(): String = "${now()}_${Random.nextInt(1000, 9999)}"

    fun formatToApiDate(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val year = dateTime.year
        val month = dateTime.monthNumber.toString().padStart(2, '0')
        val day = dateTime.dayOfMonth.toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    fun getTodayMillis(): Long {
        val instant = DateTimeClock.System.now()
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val todayStart = LocalDateTime(dateTime.year, dateTime.monthNumber, dateTime.dayOfMonth, 0, 0, 0, 0)
        return todayStart.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    fun todayApiDate(): String = formatToApiDate(getTodayMillis())
}
