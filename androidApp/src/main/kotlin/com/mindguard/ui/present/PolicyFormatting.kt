package com.mindguard.ui.present

import com.mindguard.shared.policy.AllowedSurface
import com.mindguard.shared.policy.BlockedSurface
import com.mindguard.shared.policy.PauseCardSettings
import com.mindguard.shared.policy.Platform
import com.mindguard.shared.policy.Schedule
import com.mindguard.shared.policy.ScheduleMode

const val DEFAULT_PAUSE_COUNTDOWN_SECONDS = 10
const val MINUTES_RECLAIMED_PER_INTERCEPT_TENTHS = 42L

private fun normaliseClock(time: String?, fallback: String): String {
    val parts = time?.split(":")
    if (parts == null || parts.size != 2) return fallback
    val hour = parts[0].toIntOrNull() ?: return fallback
    val minute = parts[1].toIntOrNull() ?: return fallback
    return "%02d:%02d".format(hour, minute)
}

fun scheduleLabel(schedule: Schedule): String = when (schedule.mode) {
    ScheduleMode.ALWAYS -> "Always"
    ScheduleMode.TIME_WINDOW ->
        "Daily ${normaliseClock(schedule.startTime, "00:00")} → ${normaliseClock(schedule.endTime, "23:59")}"
    ScheduleMode.DAILY_ALLOWANCE -> "${schedule.dailyLimitMinutes ?: 0} min a day, then blocked"
}

fun blockedSurfaceLabel(surface: BlockedSurface): String = when (surface) {
    BlockedSurface.REELS_TAB     -> "Reels"
    BlockedSurface.SHORTS_PLAYER -> "Shorts"
    BlockedSurface.HOME_FEED     -> "Home feed"
    BlockedSurface.EXPLORE_GRID  -> "Explore"
    BlockedSurface.SPOTLIGHT     -> "Spotlight"
}

fun allowedSurfaceLabel(surface: AllowedSurface): String = when (surface) {
    AllowedSurface.DIRECT_MESSAGES -> "DMs"
    AllowedSurface.SEARCH          -> "Search"
    AllowedSurface.SUBSCRIPTIONS   -> "Subscriptions"
    AllowedSurface.PROFILE         -> "Profile"
    AllowedSurface.CREATOR_CHANNEL -> "Creator channels"
}

fun platformName(platform: Platform): String = when (platform) {
    Platform.INSTAGRAM -> "Instagram"
    Platform.YOUTUBE   -> "YouTube"
    Platform.TIKTOK    -> "TikTok"
    Platform.SNAPCHAT  -> "Snapchat"
    Platform.REDDIT    -> "Reddit"
    Platform.LINKEDIN  -> "LinkedIn"
}

fun platformEmoji(platform: Platform): String = when (platform) {
    Platform.INSTAGRAM -> "📸"
    Platform.YOUTUBE   -> "▶️"
    Platform.TIKTOK    -> "🎵"
    Platform.SNAPCHAT  -> "👻"
    Platform.REDDIT    -> "👽"
    Platform.LINKEDIN  -> "💼"
}

fun formatTimeReclaimed(intercepts: Long): String {
    val minutes = intercepts * MINUTES_RECLAIMED_PER_INTERCEPT_TENTHS / 10
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0L -> "${rest}m"
        rest == 0L  -> "${hours}h"
        else        -> "${hours}h ${rest}m"
    }
}

fun pauseCountdownSeconds(settings: PauseCardSettings?): Int =
    (settings?.delaySeconds ?: DEFAULT_PAUSE_COUNTDOWN_SECONDS).coerceAtLeast(1)

fun canUnlock(reason: String): Boolean = reason.isNotBlank()
