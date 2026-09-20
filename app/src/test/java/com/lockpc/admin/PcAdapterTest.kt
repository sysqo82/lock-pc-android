package com.lockpc.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class PcAdapterTest {

    @Test
    fun formatDisplayName_usesNameWhenAvailable() {
        val item = PcItem(id = "1", name = "Office PC", ip = "10.0.0.1", status = "online", connected = true)
        assertEquals("Office PC", PcAdapter.formatDisplayName(item))
    }

    @Test
    fun formatDisplayName_fallsBackToIdWhenNameNull() {
        val item = PcItem(id = "pc-101", name = null, ip = "10.0.0.1", status = "online", connected = true)
        assertEquals("pc-101", PcAdapter.formatDisplayName(item))
    }

    @Test
    fun formatDisplayName_fallsBackToPCWhenNameAndIdNull() {
        val item = PcItem(id = null, name = null, ip = null, status = null, connected = null)
        assertEquals("PC", PcAdapter.formatDisplayName(item))
    }

    @Test
    fun formatDetails_formatsIpAndStatusCorrectly() {
        val item = PcItem(id = "1", name = "PC", ip = "192.168.1.5", status = "active", connected = true)
        assertEquals("IP: 192.168.1.5 — Status: active", PcAdapter.formatDetails(item))
    }

    @Test
    fun formatDetails_handlesNullIpAndStatus() {
        val item = PcItem(id = "1", name = "PC", ip = null, status = null, connected = false)
        assertEquals("IP: N/A — Status: Unknown", PcAdapter.formatDetails(item))
    }

    @Test
    fun formatDetails_showsPausedStatusWhenPauseInfoPresent() {
        val item = PcItem(id = "pc1", name = "PC1", ip = "192.168.1.10", status = "online", connected = true)
        val pauseInfo = PausedScheduleInfo(
            blockId = 1,
            pcId = "pc1",
            originalFrom = "10:00",
            originalTo = "12:00",
            days = emptyList(),
            pauseUntilTimeStr = "12:00",
            restoreTimeMs = System.currentTimeMillis() + 10000
        )

        val text = PcAdapter.formatDetails(item, pauseInfo = pauseInfo)
        assertEquals("IP: 192.168.1.10 — Status: Paused (until 12:00)", text)
    }

    @Test
    fun formatDetails_showsExtendedStatusWhenExtendInfoPresent() {
        val item = PcItem(id = "pc2", name = "PC2", ip = "192.168.1.20", status = "online", connected = true)
        val extendInfo = ExtendedScheduleInfo(
            blockId = 2,
            pcId = "pc2",
            originalFrom = "16:55",
            originalTo = "18:00",
            days = emptyList(),
            extendedFromTimeStr = "17:10",
            extendUntilMs = System.currentTimeMillis() + 10000
        )

        val text = PcAdapter.formatDetails(item, extendInfo = extendInfo)
        assertEquals("IP: 192.168.1.20 — Status: Extended (block starts at 17:10)", text)
    }
}
