package com.lockpc.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PauseRestoreManagerTest {

    @Before
    fun setUp() {
        PauseRestoreManager.clearAll()
    }

    @Test
    fun savePause_storesOriginalScheduleInfo() {
        val now = 1000000000000L
        val restoreTime = now + 30 * 60 * 1000L
        val info = PauseRestoreManager.savePause(
            blockId = 45,
            pcId = "POOKY-PC",
            originalFrom = "10:00",
            originalTo = "12:00",
            days = listOf("mon"),
            pauseUntilTimeStr = "12:00",
            restoreTimeMs = restoreTime
        )

        assertEquals(45, info.blockId)
        assertEquals("POOKY-PC", info.pcId)
        assertEquals("10:00", info.originalFrom)
        assertEquals("12:00", info.originalTo)
        assertEquals("12:00", info.pauseUntilTimeStr)

        val retrieved = PauseRestoreManager.getPausedInfo(45)
        assertNotNull(retrieved)
        assertEquals("10:00", retrieved?.originalFrom)

        val byPc = PauseRestoreManager.getPausedInfoForPc("POOKY-PC")
        assertNotNull(byPc)
        assertEquals(45, byPc?.blockId)
    }

    @Test
    fun getExpiredPauses_detectsWhenPauseTimeIsReached() {
        val now = 1000000000000L
        PauseRestoreManager.savePause(45, "PC1", "10:00", "12:00", emptyList(), "12:00", now + 1000)
        PauseRestoreManager.savePause(46, "PC2", "14:00", "16:00", emptyList(), "16:00", now + 5000)

        val expiredBefore = PauseRestoreManager.getExpiredPauses(now)
        assertTrue(expiredBefore.isEmpty())

        val expiredAfter = PauseRestoreManager.getExpiredPauses(now + 2000)
        assertEquals(1, expiredAfter.size)
        assertEquals(45, expiredAfter[0].blockId)

        PauseRestoreManager.removePause(45)
        assertNull(PauseRestoreManager.getPausedInfo(45))
    }
}
