package com.lockpc.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockPeriodAdapterTest {

    @Test
    fun formatDayText_nullOrEmpty_returnsEveryday() {
        assertEquals("Everyday", BlockPeriodAdapter.formatDayText(null))
        assertEquals("Everyday", BlockPeriodAdapter.formatDayText(emptyList()))
    }

    @Test
    fun formatDayText_allDays_returnsEveryday() {
        val all = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
        assertEquals("Everyday", BlockPeriodAdapter.formatDayText(all))
    }

    @Test
    fun formatDayText_weekdays_returnsWeekdays() {
        val weekdays = listOf("mon", "tue", "wed", "thu", "fri")
        assertEquals("Weekdays", BlockPeriodAdapter.formatDayText(weekdays))
    }

    @Test
    fun formatDayText_weekends_returnsWeekends() {
        val weekends = listOf("sat", "sun")
        assertEquals("Weekends", BlockPeriodAdapter.formatDayText(weekends))
    }

    @Test
    fun formatDayText_customDays_returnsCapitalizedCommaSeparatedList() {
        val custom = listOf("mon", "wed", "fri")
        assertEquals("Mon, Wed, Fri", BlockPeriodAdapter.formatDayText(custom))
    }
}
