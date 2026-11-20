package com.mindguard.ui.components

import com.mindguard.ui.present.MINUTES_RECLAIMED_PER_INTERCEPT_TENTHS

const val DEFAULT_RECLAIM_GOAL_MINUTES = 84L

fun reclaimedRingProgress(blocksToday: Long, goalMinutes: Long = DEFAULT_RECLAIM_GOAL_MINUTES): Float {
    if (goalMinutes <= 0L) return 1f
    val minutes = blocksToday * MINUTES_RECLAIMED_PER_INTERCEPT_TENTHS / 10
    return (minutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
}

fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11  -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else      -> "Good night"
}
