package com.lockpc.admin

import java.util.concurrent.ConcurrentHashMap

data class ExtendedScheduleInfo(
    val blockId: Int,
    val pcId: String,
    val originalFrom: String,
    val originalTo: String,
    val days: List<String>,
    val extendedFromTimeStr: String,
    val extendUntilMs: Long
)

object ExtendRestoreManager {

    private val extendMap = ConcurrentHashMap<Int, ExtendedScheduleInfo>()

    fun saveExtension(
        blockId: Int,
        pcId: String,
        originalFrom: String,
        originalTo: String,
        days: List<String>,
        extendedFromTimeStr: String,
        extendUntilMs: Long
    ): ExtendedScheduleInfo {
        val info = ExtendedScheduleInfo(
            blockId = blockId,
            pcId = pcId,
            originalFrom = originalFrom,
            originalTo = originalTo,
            days = days,
            extendedFromTimeStr = extendedFromTimeStr,
            extendUntilMs = extendUntilMs
        )
        extendMap[blockId] = info
        return info
    }

    fun getExtensionInfo(blockId: Int): ExtendedScheduleInfo? = extendMap[blockId]

    fun getExtensionInfoForPc(pcId: String): ExtendedScheduleInfo? {
        return extendMap.values.find { it.pcId == pcId }
    }

    fun getAllExtensionInfos(): List<ExtendedScheduleInfo> {
        return extendMap.values.toList()
    }

    fun removeExtension(blockId: Int): ExtendedScheduleInfo? = extendMap.remove(blockId)

    fun getExpiredExtensions(nowMs: Long = System.currentTimeMillis()): List<ExtendedScheduleInfo> {
        val expired = mutableListOf<ExtendedScheduleInfo>()
        for ((_, info) in extendMap) {
            if (nowMs >= info.extendUntilMs) {
                expired.add(info)
            }
        }
        return expired
    }

    fun clearAll() {
        extendMap.clear()
    }
}
