package com.lockpc.admin

import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

data class PauseState(
    val pcId: String,
    val pauseUntilMs: Long,
    val untilEndOfSession: Boolean
)

object PcPauseManager {

    private val pauseMap = ConcurrentHashMap<String, PauseState>()

    fun pauseForDuration(pcId: String, durationMinutes: Int, nowMs: Long = System.currentTimeMillis()): PauseState {
        val pauseUntilMs = nowMs + durationMinutes * 60 * 1000L
        val state = PauseState(
            pcId = pcId,
            pauseUntilMs = pauseUntilMs,
            untilEndOfSession = false
        )
        pauseMap[pcId] = state
        return state
    }

    fun pauseUntilEndOfSession(
        pcId: String,
        blockPeriods: List<BlockPeriod>,
        calendar: Calendar = Calendar.getInstance()
    ): PauseState? {
        val endMs = BlockSessionCalculator.getEndOfSessionTimestamp(blockPeriods, calendar) ?: return null
        val state = PauseState(
            pcId = pcId,
            pauseUntilMs = endMs,
            untilEndOfSession = true
        )
        pauseMap[pcId] = state
        return state
    }

    fun unpause(pcId: String) {
        pauseMap.remove(pcId)
    }

    fun isPaused(pcId: String?, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (pcId == null) return false
        val state = pauseMap[pcId] ?: return false
        if (nowMs >= state.pauseUntilMs) {
            pauseMap.remove(pcId)
            return false
        }
        return true
    }

    fun getPauseState(pcId: String?, nowMs: Long = System.currentTimeMillis()): PauseState? {
        if (pcId == null) return null
        if (!isPaused(pcId, nowMs)) return null
        return pauseMap[pcId]
    }

    fun clearAll() {
        pauseMap.clear()
    }
}
