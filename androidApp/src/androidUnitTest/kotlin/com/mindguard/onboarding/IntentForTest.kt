package com.mindguard.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals

class IntentForTest {

    private fun answers(
        apps: Set<TrackedApp> = setOf(TrackedApp.INSTAGRAM, TrackedApp.YOUTUBE),
        keep: Set<KeepSurface> = setOf(KeepSurface.DMS, KeepSurface.SEARCH, KeepSurface.SUBSCRIPTIONS),
        hardestTime: HardestTime? = HardestTime.LATE_NIGHT
    ) = OnboardingAnswers(apps = apps, keep = keep, hardestTime = hardestTime)

    @Test
    fun instagramAndYoutubeLateNightMatchesTheReferenceSentence() {
        assertEquals(
            "Keep my Instagram DMs and search, and my YouTube subscriptions and search, " +
                "but block Instagram Reels and YouTube Shorts after 9 PM.",
            intentFor(answers())
        )
    }

    @Test
    fun morningBlocksBeforeTenAm() {
        assertEquals(true, intentFor(answers(hardestTime = HardestTime.MORNING)).endsWith(" before 10 AM."))
    }

    @Test
    fun workHoursBlocksFromNineToSix() {
        assertEquals(true, intentFor(answers(hardestTime = HardestTime.WORK_HOURS)).endsWith(" from 9 AM to 6 PM."))
    }

    @Test
    fun allDayBlocksAllDay() {
        assertEquals(true, intentFor(answers(hardestTime = HardestTime.ALL_DAY)).endsWith(" all day."))
    }

    @Test
    fun noHardestTimeBlocksAllDay() {
        assertEquals(true, intentFor(answers(hardestTime = null)).endsWith(" all day."))
    }

    @Test
    fun noAppsSelectedDefaultsToInstagramAndYoutube() {
        assertEquals(intentFor(answers()), intentFor(answers(apps = emptySet())))
    }

    @Test
    fun snapchatBlocksSpotlight() {
        assertEquals(
            "Keep my Snapchat DMs, but block Snapchat Spotlight all day.",
            intentFor(answers(apps = setOf(TrackedApp.SNAPCHAT), keep = setOf(KeepSurface.DMS), hardestTime = null))
        )
    }

    @Test
    fun tiktokBlocksTheFeed() {
        assertEquals(
            "Keep my TikTok DMs and search, but block the TikTok feed after 9 PM.",
            intentFor(answers(apps = setOf(TrackedApp.TIKTOK), keep = setOf(KeepSurface.DMS, KeepSurface.SEARCH)))
        )
    }

    @Test
    fun threeOrMoreBlockedSurfacesAreListedWithCommas() {
        assertEquals(
            "Keep my Instagram DMs, my Snapchat DMs, and my TikTok DMs, " +
                "but block Instagram Reels, Snapchat Spotlight and the TikTok feed after 9 PM.",
            intentFor(
                answers(
                    apps = setOf(TrackedApp.TIKTOK, TrackedApp.INSTAGRAM, TrackedApp.SNAPCHAT),
                    keep = setOf(KeepSurface.DMS)
                )
            )
        )
    }

    @Test
    fun subscriptionsOnlyApplyToYoutube() {
        assertEquals(
            "Keep my YouTube subscriptions, but block Instagram Reels and YouTube Shorts after 9 PM.",
            intentFor(answers(keep = setOf(KeepSurface.SUBSCRIPTIONS)))
        )
    }

    @Test
    fun profilesOnlyApplyToInstagram() {
        assertEquals(
            "Keep my Instagram profiles, but block Instagram Reels and YouTube Shorts after 9 PM.",
            intentFor(answers(keep = setOf(KeepSurface.PROFILES)))
        )
    }

    @Test
    fun instagramListsDmsSearchAndProfilesInThatOrder() {
        assertEquals(
            "Keep my Instagram DMs, search and profiles, but block Instagram Reels all day.",
            intentFor(
                answers(
                    apps = setOf(TrackedApp.INSTAGRAM),
                    keep = setOf(KeepSurface.PROFILES, KeepSurface.SEARCH, KeepSurface.DMS),
                    hardestTime = HardestTime.ALL_DAY
                )
            )
        )
    }

    @Test
    fun nothingKeptJustBlocks() {
        assertEquals(
            "Block Instagram Reels and YouTube Shorts after 9 PM.",
            intentFor(answers(keep = emptySet()))
        )
    }
}
