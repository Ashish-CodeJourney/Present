package com.mindguard.ui.present

import com.mindguard.shared.policy.AllowedSurface
import com.mindguard.shared.policy.BlockedSurface
import com.mindguard.shared.policy.PauseCardSettings
import com.mindguard.shared.policy.Platform
import com.mindguard.shared.policy.Schedule
import com.mindguard.shared.policy.ScheduleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyFormattingTest {

    private fun schedule(
        mode: ScheduleMode,
        startTime: String? = null,
        endTime: String? = null,
        dailyLimitMinutes: Int? = null
    ) = Schedule(mode = mode, startTime = startTime, endTime = endTime, dailyLimitMinutes = dailyLimitMinutes)

    @Test
    fun alwaysScheduleReadsAlways() {
        assertEquals("Always", scheduleLabel(schedule(ScheduleMode.ALWAYS)))
    }

    @Test
    fun timeWindowShowsStartAndEndWithArrow() {
        assertEquals(
            "Daily 21:00 → 06:00",
            scheduleLabel(schedule(ScheduleMode.TIME_WINDOW, startTime = "21:00", endTime = "06:00"))
        )
    }

    @Test
    fun timeWindowPadsSingleDigitHours() {
        assertEquals(
            "Daily 09:00 → 17:30",
            scheduleLabel(schedule(ScheduleMode.TIME_WINDOW, startTime = "9:00", endTime = "17:30"))
        )
    }

    @Test
    fun timeWindowWithMissingTimesFallsBackToAllDay() {
        assertEquals(
            "Daily 00:00 → 23:59",
            scheduleLabel(schedule(ScheduleMode.TIME_WINDOW))
        )
    }

    @Test
    fun dailyAllowanceShowsMinutesPerDay() {
        assertEquals(
            "30 min a day, then blocked",
            scheduleLabel(schedule(ScheduleMode.DAILY_ALLOWANCE, dailyLimitMinutes = 30))
        )
    }

    @Test
    fun dailyAllowanceWithoutLimitMeansNoAllowance() {
        assertEquals(
            "0 min a day, then blocked",
            scheduleLabel(schedule(ScheduleMode.DAILY_ALLOWANCE))
        )
    }

    @Test
    fun blockedSurfacesHaveHumanLabels() {
        assertEquals("Reels", blockedSurfaceLabel(BlockedSurface.REELS_TAB))
        assertEquals("Shorts", blockedSurfaceLabel(BlockedSurface.SHORTS_PLAYER))
        assertEquals("Home feed", blockedSurfaceLabel(BlockedSurface.HOME_FEED))
        assertEquals("Explore", blockedSurfaceLabel(BlockedSurface.EXPLORE_GRID))
        assertEquals("Spotlight", blockedSurfaceLabel(BlockedSurface.SPOTLIGHT))
    }

    @Test
    fun allowedSurfacesHaveHumanLabels() {
        assertEquals("DMs", allowedSurfaceLabel(AllowedSurface.DIRECT_MESSAGES))
        assertEquals("Search", allowedSurfaceLabel(AllowedSurface.SEARCH))
        assertEquals("Subscriptions", allowedSurfaceLabel(AllowedSurface.SUBSCRIPTIONS))
        assertEquals("Profile", allowedSurfaceLabel(AllowedSurface.PROFILE))
        assertEquals("Creator channels", allowedSurfaceLabel(AllowedSurface.CREATOR_CHANNEL))
    }

    @Test
    fun everyPlatformHasANameAndEmoji() {
        assertEquals("Instagram", platformName(Platform.INSTAGRAM))
        assertEquals("YouTube", platformName(Platform.YOUTUBE))
        assertEquals("TikTok", platformName(Platform.TIKTOK))
        assertEquals("Snapchat", platformName(Platform.SNAPCHAT))
        assertEquals("Reddit", platformName(Platform.REDDIT))
        assertEquals("LinkedIn", platformName(Platform.LINKEDIN))
        Platform.values().forEach { assertTrue(platformEmoji(it).isNotBlank()) }
    }

    @Test
    fun timeReclaimedUnderAnHourIsMinutesOnly() {
        assertEquals("42m", formatTimeReclaimed(10))
    }

    @Test
    fun timeReclaimedOverAnHourShowsHoursAndMinutes() {
        assertEquals("3h 17m", formatTimeReclaimed(47))
    }

    @Test
    fun timeReclaimedOnExactHourOmitsMinutes() {
        assertEquals("7h", formatTimeReclaimed(100))
    }

    @Test
    fun noInterceptsMeansNoTimeReclaimed() {
        assertEquals("0m", formatTimeReclaimed(0))
    }

    @Test
    fun pauseCountdownDefaultsToTenSeconds() {
        assertEquals(10, pauseCountdownSeconds(null))
        assertEquals(10, pauseCountdownSeconds(PauseCardSettings(delaySeconds = null, promptReflection = null, sideQuestCategory = null)))
    }

    @Test
    fun pauseCountdownUsesPolicyDelay() {
        assertEquals(20, pauseCountdownSeconds(PauseCardSettings(delaySeconds = 20, promptReflection = true, sideQuestCategory = null)))
    }

    @Test
    fun pauseCountdownIsAtLeastOneSecond() {
        assertEquals(1, pauseCountdownSeconds(PauseCardSettings(delaySeconds = 0, promptReflection = null, sideQuestCategory = null)))
    }

    @Test
    fun unlockRequiresANonBlankReason() {
        assertFalse(canUnlock(""))
        assertFalse(canUnlock("   \n"))
        assertTrue(canUnlock("Replying to my sister"))
    }
}
