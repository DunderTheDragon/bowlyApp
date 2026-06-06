package com.cantbebetter.bowly.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClockUtilsTest {

    @Test
    fun formatToApiDate_returnsIsoDate() {
        val formatted = Clock.formatToApiDate(0L)
        assertTrue(formatted.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun uniqueId_containsTimestampPart() {
        val id = Clock.uniqueId()
        assertTrue(id.contains("_"))
    }

    @Test
    fun todayApiDate_matchesFormatOfTodayMillis() {
        assertEquals(Clock.formatToApiDate(Clock.getTodayMillis()), Clock.todayApiDate())
    }
}
