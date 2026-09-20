package com.lockpc.admin

import java.util.Calendar

object BlockSessionCalculator {

    fun parseTimeMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size < 2) return 0
        val hours = parts[0].trim().toIntOrNull() ?: 0
        val minutes = parts[1].trim().toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    fun formatTimeMinutes(totalMinutes: Int): String {
        val normalized = (totalMinutes % (24 * 60) + (24 * 60)) % (24 * 60)
        val hours = normalized / 60
        val mins = normalized % 60
        return String.format("%02d:%02d", hours, mins)
    }

    fun isDayActive(days: List<String>?, dayOfWeek: String): Boolean {
        if (days.isNullOrEmpty()) return true
        val normalizedDay = dayOfWeek.lowercase()
        val normalizedDays = days.map { it.lowercase() }
        if (normalizedDays.contains("everyday") || normalizedDays.contains("all")) return true
        if (normalizedDays.contains("weekdays") && normalizedDay in setOf("mon", "tue", "wed", "thu", "fri")) return true
        if (normalizedDays.contains("weekends") && normalizedDay in setOf("sat", "sun")) return true
        return normalizedDays.contains(normalizedDay)
    }

    fun getDayOfWeekString(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "mon"
            Calendar.TUESDAY -> "tue"
            Calendar.WEDNESDAY -> "wed"
            Calendar.THURSDAY -> "thu"
            Calendar.FRIDAY -> "fri"
            Calendar.SATURDAY -> "sat"
            else -> "sun"
        }
    }

    fun getActiveBlockPeriod(blockPeriods: List<BlockPeriod>, calendar: Calendar): BlockPeriod? {
        val currentDay = getDayOfWeekString(calendar)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        for (bp in blockPeriods) {
            if (!isDayActive(bp.days, currentDay)) continue

            val fromMin = parseTimeMinutes(bp.from)
            val toMin = parseTimeMinutes(bp.to)

            if (fromMin < toMin) {
                if (currentMinutes in fromMin until toMin) return bp
            } else if (fromMin > toMin) {
                if (currentMinutes >= fromMin || currentMinutes < toMin) return bp
            }
        }
        return null
    }

    fun getUpcomingOrActiveBlockPeriod(blockPeriods: List<BlockPeriod>, calendar: Calendar): BlockPeriod? {
        val active = getActiveBlockPeriod(blockPeriods, calendar)
        if (active != null) return active

        val currentDay = getDayOfWeekString(calendar)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        var closestBp: BlockPeriod? = null
        var minDiff = Int.MAX_VALUE

        for (bp in blockPeriods) {
            if (!isDayActive(bp.days, currentDay)) continue
            val fromMin = parseTimeMinutes(bp.from)
            if (fromMin > currentMinutes) {
                val diff = fromMin - currentMinutes
                if (diff < minDiff) {
                    minDiff = diff
                    closestBp = bp
                }
            }
        }
        return closestBp ?: blockPeriods.firstOrNull()
    }

    fun calculatePauseFromTime(activeBlock: BlockPeriod, pauseMinutes: Int, untilEnd: Boolean, calendar: Calendar): String {
        if (untilEnd) {
            return activeBlock.to
        }
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val newFromMinutes = currentMinutes + pauseMinutes
        return formatTimeMinutes(newFromMinutes)
    }

    fun getEndOfSessionTimestamp(blockPeriods: List<BlockPeriod>, calendar: Calendar): Long? {
        val currentDay = getDayOfWeekString(calendar)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        for (bp in blockPeriods) {
            if (!isDayActive(bp.days, currentDay)) continue

            val fromMin = parseTimeMinutes(bp.from)
            val toMin = parseTimeMinutes(bp.to)

            if (fromMin < toMin) {
                if (currentMinutes in fromMin until toMin) {
                    val endCal = calendar.clone() as Calendar
                    endCal.set(Calendar.HOUR_OF_DAY, toMin / 60)
                    endCal.set(Calendar.MINUTE, toMin % 60)
                    endCal.set(Calendar.SECOND, 0)
                    endCal.set(Calendar.MILLISECOND, 0)
                    return endCal.timeInMillis
                }
            } else if (fromMin > toMin) {
                if (currentMinutes >= fromMin) {
                    val endCal = calendar.clone() as Calendar
                    endCal.add(Calendar.DAY_OF_YEAR, 1)
                    endCal.set(Calendar.HOUR_OF_DAY, toMin / 60)
                    endCal.set(Calendar.MINUTE, toMin % 60)
                    endCal.set(Calendar.SECOND, 0)
                    endCal.set(Calendar.MILLISECOND, 0)
                    return endCal.timeInMillis
                } else if (currentMinutes < toMin) {
                    val endCal = calendar.clone() as Calendar
                    endCal.set(Calendar.HOUR_OF_DAY, toMin / 60)
                    endCal.set(Calendar.MINUTE, toMin % 60)
                    endCal.set(Calendar.SECOND, 0)
                    endCal.set(Calendar.MILLISECOND, 0)
                    return endCal.timeInMillis
                }
            }
        }
        return null
    }

    fun getRemainingSessionMillis(blockPeriods: List<BlockPeriod>, calendar: Calendar): Long {
        val endMs = getEndOfSessionTimestamp(blockPeriods, calendar) ?: return 0L
        val diff = endMs - calendar.timeInMillis
        return if (diff > 0) diff else 0L
    }

    fun getNextResetTimestamp(calendar: Calendar): Long {
        val resetCal = calendar.clone() as Calendar
        resetCal.add(Calendar.DAY_OF_YEAR, 1)
        resetCal.set(Calendar.HOUR_OF_DAY, 0)
        resetCal.set(Calendar.MINUTE, 0)
        resetCal.set(Calendar.SECOND, 5)
        resetCal.set(Calendar.MILLISECOND, 0)
        return resetCal.timeInMillis
    }
}
