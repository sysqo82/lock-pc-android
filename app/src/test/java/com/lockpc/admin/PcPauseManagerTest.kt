package com.lockpc.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class PcPauseManagerTest {

    @Before
    fun setUp() {
        PcPauseManager.clearAll()
    }

    @Test
    fun pauseForDuration_setsPauseUntilTimestamp() {
        val now = 1000000000000L
        val state = PcPauseManager.pauseForDuration("pc-1", 30, now)

        assertEquals("pc-1", state.pcId)
        assertEquals(now + 30 * 60 * 1000L, state.pauseUntilMs)
        assertFalse(state.untilEndOfSession)

        assertTrue(PcPauseManager.isPaused("pc-1", now + 1000))
        assertFalse(PcPauseManager.isPaused("pc-1", now + 31 * 60 * 1000L))
    }

    @Test
    fun pauseUntilEndOfSession_usesScheduleEndTimeWithoutModifyingSchedule() {
        val block = BlockPeriod(id = 1, from = "08:00", to = "17:00", days = listOf("mon"))
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val state = PcPauseManager.pauseUntilEndOfSession("pc-2", listOf(block), cal)
        assertNotNull(state)
        assertTrue(state!!.untilEndOfSession)

        val expectedEndCal = cal.clone() as Calendar
        expectedEndCal.set(Calendar.HOUR_OF_DAY, 17)
        expectedEndCal.set(Calendar.MINUTE, 0)

        assertEquals(expectedEndCal.timeInMillis, state.pauseUntilMs)

        // BlockPeriod remains unchanged
        assertEquals("08:00", block.from)
        assertEquals("17:00", block.to)
    }

    @Test
    fun pauseUntilEndOfSession_returnsNullIfNoActiveBlock() {
        val block = BlockPeriod(id = 1, from = "08:00", to = "12:00", days = listOf("mon"))
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }

        val state = PcPauseManager.pauseUntilEndOfSession("pc-3", listOf(block), cal)
        assertNull(state)
        assertFalse(PcPauseManager.isPaused("pc-3", cal.timeInMillis))
    }

    @Test
    fun unpause_removesPauseState() {
        val now = 1000000000000L
        PcPauseManager.pauseForDuration("pc-4", 60, now)
        assertTrue(PcPauseManager.isPaused("pc-4", now))

        PcPauseManager.unpause("pc-4")
        assertFalse(PcPauseManager.isPaused("pc-4", now))
    }
}
