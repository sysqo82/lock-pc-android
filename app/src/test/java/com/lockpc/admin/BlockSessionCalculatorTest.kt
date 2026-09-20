package com.lockpc.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BlockSessionCalculatorTest {

    @Test
    fun parseTimeMinutes_parsesHoursAndMinutes() {
        assertEquals(0, BlockSessionCalculator.parseTimeMinutes("00:00"))
        assertEquals(510, BlockSessionCalculator.parseTimeMinutes("08:30"))
        assertEquals(1020, BlockSessionCalculator.parseTimeMinutes("17:00"))
        assertEquals(0, BlockSessionCalculator.parseTimeMinutes("invalid"))
    }

    @Test
    fun formatTimeMinutes_formatsMinutesToTimeString() {
        assertEquals("08:30", BlockSessionCalculator.formatTimeMinutes(510))
        assertEquals("17:00", BlockSessionCalculator.formatTimeMinutes(1020))
        assertEquals("00:15", BlockSessionCalculator.formatTimeMinutes(15))
    }

    @Test
    fun isDayActive_matchesDayOfWeekCaseInsensitivelyAndRecognizesSynonyms() {
        assertTrue(BlockSessionCalculator.isDayActive(null, "mon"))
        assertTrue(BlockSessionCalculator.isDayActive(emptyList(), "mon"))
        assertTrue(BlockSessionCalculator.isDayActive(listOf("Mon", "Wed"), "mon"))
        assertTrue(BlockSessionCalculator.isDayActive(listOf("Everyday"), "thu"))
        assertTrue(BlockSessionCalculator.isDayActive(listOf("Weekdays"), "fri"))
        assertTrue(BlockSessionCalculator.isDayActive(listOf("Weekends"), "sat"))
        assertTrue(!BlockSessionCalculator.isDayActive(listOf("Tue", "Wed"), "mon"))
    }

    @Test
    fun calculatePauseFromTime_forSpecificDuration_shiftsFromTime() {
        val block = BlockPeriod(id = 1, from = "08:00", to = "17:00", days = listOf("mon"))
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 21)
        }
        val newFrom = BlockSessionCalculator.calculatePauseFromTime(block, 30, untilEnd = false, cal)
        assertEquals("08:51", newFrom)
    }

    @Test
    fun calculatePauseFromTime_untilEnd_returnsBlockToTime() {
        val block = BlockPeriod(id = 1, from = "08:00", to = "17:00", days = listOf("mon"))
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 21)
        }
        val newFrom = BlockSessionCalculator.calculatePauseFromTime(block, 0, untilEnd = true, cal)
        assertEquals("17:00", newFrom)
    }

    @Test
    fun getActiveBlockPeriod_findsActiveBlockWithEveryday() {
        val block = BlockPeriod(id = 1, from = "23:30", to = "07:45", days = listOf("Everyday"))
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
        }
        val active = BlockSessionCalculator.getActiveBlockPeriod(listOf(block), cal)
        assertNotNull(active)
        assertEquals(1, active?.id)
    }

    @Test
    fun getUpcomingOrActiveBlockPeriod_findsClosestUpcomingBlock() {
        val b1 = BlockPeriod(id = 1, from = "08:00", to = "12:00", days = listOf("mon"))
        val b2 = BlockPeriod(id = 2, from = "16:55", to = "18:00", days = listOf("mon"))
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }

        val closest = BlockSessionCalculator.getUpcomingOrActiveBlockPeriod(listOf(b1, b2), cal)
        assertNotNull(closest)
        assertEquals(2, closest?.id)
    }

    @Test
    fun getEndOfSessionTimestamp_returnsCorrectEndTimeForActiveSameDayBlock() {
        val block = BlockPeriod(id = 1, from = "08:00", to = "17:00", days = listOf("mon"))
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endMs = BlockSessionCalculator.getEndOfSessionTimestamp(listOf(block), cal)
        assertNotNull(endMs)

        val expectedEndCal = cal.clone() as Calendar
        expectedEndCal.set(Calendar.HOUR_OF_DAY, 17)
        expectedEndCal.set(Calendar.MINUTE, 0)

        assertEquals(expectedEndCal.timeInMillis, endMs)

        val remainingMs = BlockSessionCalculator.getRemainingSessionMillis(listOf(block), cal)
        assertEquals(6 * 3600 * 1000L + 30 * 60 * 1000L, remainingMs)
    }

    @Test
    fun getNextResetTimestamp_returnsMidnightNextDay() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val resetMs = BlockSessionCalculator.getNextResetTimestamp(cal)

        val resetCal = cal.clone() as Calendar
        resetCal.add(Calendar.DAY_OF_YEAR, 1)
        resetCal.set(Calendar.HOUR_OF_DAY, 0)
        resetCal.set(Calendar.MINUTE, 0)
        resetCal.set(Calendar.SECOND, 5)

        assertEquals(resetCal.timeInMillis, resetMs)
    }
}
