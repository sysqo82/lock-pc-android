package com.lockpc.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExtendRestoreManagerTest {

    @Before
    fun setUp() {
        ExtendRestoreManager.clearAll()
    }

    @Test
    fun saveExtension_storesOriginalAndExtendedInfo() {
        val now = 1000000000000L
        val extendMs = now + 15 * 60 * 1000L
        val info = ExtendRestoreManager.saveExtension(
            blockId = 10,
            pcId = "POOKY-PC",
            originalFrom = "16:55",
            originalTo = "18:00",
            days = listOf("thu"),
            extendedFromTimeStr = "17:10",
            extendUntilMs = extendMs
        )

        assertEquals(10, info.blockId)
        assertEquals("POOKY-PC", info.pcId)
        assertEquals("16:55", info.originalFrom)
        assertEquals("17:10", info.extendedFromTimeStr)

        val retrieved = ExtendRestoreManager.getExtensionInfo(10)
        assertNotNull(retrieved)
        assertEquals("17:10", retrieved?.extendedFromTimeStr)

        val byPc = ExtendRestoreManager.getExtensionInfoForPc("POOKY-PC")
        assertNotNull(byPc)
        assertEquals(10, byPc?.blockId)
    }

    @Test
    fun getExpiredExtensions_detectsWhenExtensionTimeIsReached() {
        val now = 1000000000000L
        ExtendRestoreManager.saveExtension(10, "PC1", "16:55", "18:00", emptyList(), "17:10", now + 1000)

        val expiredBefore = ExtendRestoreManager.getExpiredExtensions(now)
        assertTrue(expiredBefore.isEmpty())

        val expiredAfter = ExtendRestoreManager.getExpiredExtensions(now + 2000)
        assertEquals(1, expiredAfter.size)
        assertEquals(10, expiredAfter[0].blockId)

        ExtendRestoreManager.removeExtension(10)
        assertNull(ExtendRestoreManager.getExtensionInfo(10))
    }
}
