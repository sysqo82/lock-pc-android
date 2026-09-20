package com.lockpc.admin

import java.util.concurrent.ConcurrentHashMap

data class PausedScheduleInfo(
    val blockId: Int,
    val pcId: String,
    val originalFrom: String,
    val originalTo: String,
    val days: List<String>,
    val pauseUntilTimeStr: String,
    val restoreTimeMs: Long
)

object PauseRestoreManager {

    private val pausedMap = ConcurrentHashMap<Int, PausedScheduleInfo>()

    fun savePause(
        blockId: Int,
        pcId: String,
        originalFrom: String,
        originalTo: String,
        days: List<String>,
        pauseUntilTimeStr: String,
        restoreTimeMs: Long
    ): PausedScheduleInfo {
        val info = PausedScheduleInfo(
            blockId = blockId,
            pcId = pcId,
            originalFrom = originalFrom,
            originalTo = originalTo,
            days = days,
            pauseUntilTimeStr = pauseUntilTimeStr,
            restoreTimeMs = restoreTimeMs
        )
        pausedMap[blockId] = info
        return info
    }

    fun getPausedInfo(blockId: Int): PausedScheduleInfo? = pausedMap[blockId]

    fun getPausedInfoForPc(pcId: String): PausedScheduleInfo? {
        return pausedMap.values.find { it.pcId == pcId }
    }

    fun getAllPausedInfos(): List<PausedScheduleInfo> {
        return pausedMap.values.toList()
    }

    fun removePause(blockId: Int): PausedScheduleInfo? = pausedMap.remove(blockId)

    fun removePauseForPc(pcId: String) {
        pausedMap.entries.removeIf { it.value.pcId == pcId }
    }

    fun getExpiredPauses(nowMs: Long = System.currentTimeMillis()): List<PausedScheduleInfo> {
        val expired = mutableListOf<PausedScheduleInfo>()
        for ((_, info) in pausedMap) {
            if (nowMs >= info.restoreTimeMs) {
                expired.add(info)
            }
        }
        return expired
    }

    fun clearAll() {
        pausedMap.clear()
    }
}
