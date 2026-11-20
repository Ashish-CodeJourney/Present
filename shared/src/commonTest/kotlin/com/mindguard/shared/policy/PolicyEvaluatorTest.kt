package com.mindguard.shared.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PolicyEvaluatorTest {

    private val evaluator = PolicyEvaluator()

    private val instagram = "com.instagram.android"
    private val youtube = "com.google.android.youtube"

    private fun minute(hour: Int, min: Int = 0) = hour * 60 + min

    private fun rule(
        platform: Platform = Platform.INSTAGRAM,
        targets: List<BlockedSurface> = listOf(BlockedSurface.REELS_TAB),
        schedule: Schedule = Schedule(mode = ScheduleMode.ALWAYS)
    ) = PolicyRule(
        platform = platform,
        targetSurfaces = targets,
        allowedSurfaces = listOf(AllowedSurface.DIRECT_MESSAGES),
        schedule = schedule
    )

    private fun policy(vararg rules: PolicyRule) =
        PresentPolicy(policyVersion = "1.0", summary = "test", rules = rules.toList())

    private fun window(start: String?, end: String?) =
        Schedule(mode = ScheduleMode.TIME_WINDOW, startTime = start, endTime = end)

    @Test
    fun alwaysScheduleEnforcesAtAnyMinute() {
        val p = policy(rule())
        listOf(0, minute(9), minute(12, 30), minute(23, 59)).forEach {
            assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, it))
        }
    }

    @Test
    fun platformNotInPolicyIsNotEnforced() {
        val p = policy(rule(platform = Platform.INSTAGRAM))
        assertFalse(evaluator.shouldEnforce(p, youtube, BlockedSurface.SHORTS_PLAYER, minute(12)))
    }

    @Test
    fun unknownPackageIsNotEnforced() {
        val p = policy(rule())
        assertFalse(evaluator.shouldEnforce(p, "com.example.notes", BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun surfaceNotTargetedIsNotEnforced() {
        val p = policy(rule(targets = listOf(BlockedSurface.EXPLORE_GRID)))
        assertFalse(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun emptyPolicyEnforcesNothing() {
        assertFalse(evaluator.shouldEnforce(policy(), instagram, BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun youtubeShortsRuleMatchesYouTubePackage() {
        val p = policy(rule(platform = Platform.YOUTUBE, targets = listOf(BlockedSurface.SHORTS_PLAYER)))
        assertTrue(evaluator.shouldEnforce(p, youtube, BlockedSurface.SHORTS_PLAYER, minute(12)))
    }

    @Test
    fun allThreeTikTokPackagesMapToTikTok() {
        val p = policy(rule(platform = Platform.TIKTOK))
        listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme").forEach {
            assertTrue(evaluator.shouldEnforce(p, it, BlockedSurface.REELS_TAB, minute(12)))
        }
    }

    @Test
    fun snapchatSpotlightRuleMatchesSnapchatPackage() {
        val p = policy(rule(platform = Platform.SNAPCHAT, targets = listOf(BlockedSurface.SPOTLIGHT)))
        assertTrue(evaluator.shouldEnforce(p, "com.snapchat.android", BlockedSurface.SPOTLIGHT, minute(12)))
    }

    @Test
    fun daytimeWindowIsActiveFromStartUntilJustBeforeEnd() {
        val p = policy(rule(schedule = window("09:00", "17:00")))
        assertFalse(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(8, 59)))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(9)))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(16, 59)))
        assertFalse(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(17)))
    }

    @Test
    fun overnightWindowWrapsPastMidnight() {
        val p = policy(rule(schedule = window("21:00", "06:00")))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(21)))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(21, 5)))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(23, 59)))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, 0))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(5, 59)))
    }

    @Test
    fun overnightWindowIsInactiveDuringTheDay() {
        val p = policy(rule(schedule = window("21:00", "06:00")))
        assertFalse(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(6)))
        assertFalse(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12)))
        assertFalse(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(20, 59)))
    }

    @Test
    fun singleDigitHourIsAccepted() {
        val p = policy(rule(schedule = window("9:30", "10:00")))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(9, 30)))
        assertFalse(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(9, 29)))
    }

    @Test
    fun malformedTimeFailsSafeToAlways() {
        val p = policy(rule(schedule = window("9pm", "06:00")))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun outOfRangeTimeFailsSafeToAlways() {
        val p = policy(rule(schedule = window("21:00", "25:00")))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun missingWindowTimesFailSafeToAlways() {
        val p = policy(rule(schedule = window(null, null)))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun missingEndTimeFailsSafeToAlways() {
        val p = policy(rule(schedule = window("21:00", null)))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun zeroLengthWindowFailsSafeToAlways() {
        val p = policy(rule(schedule = window("21:00", "21:00")))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun dailyAllowanceIsEnforcedLikeAlwaysForTheMvp() {
        val p = policy(rule(schedule = Schedule(mode = ScheduleMode.DAILY_ALLOWANCE, dailyLimitMinutes = 15)))
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun anyMatchingActiveRuleIsEnough() {
        val p = policy(
            rule(schedule = window("21:00", "06:00")),
            rule(schedule = window("12:00", "13:00"))
        )
        assertTrue(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12, 15)))
        assertFalse(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(15)))
    }

    @Test
    fun ruleForAnotherPlatformDoesNotLeakItsSchedule() {
        val p = policy(
            rule(platform = Platform.YOUTUBE, targets = listOf(BlockedSurface.REELS_TAB)),
            rule(platform = Platform.INSTAGRAM, schedule = window("21:00", "06:00"))
        )
        assertFalse(evaluator.shouldEnforce(p, instagram, BlockedSurface.REELS_TAB, minute(12)))
    }

    @Test
    fun platformForMapsKnownPackages() {
        assertEquals(Platform.INSTAGRAM, PolicyEvaluator.platformFor(instagram))
        assertEquals(Platform.YOUTUBE, PolicyEvaluator.platformFor(youtube))
        assertEquals(Platform.TIKTOK, PolicyEvaluator.platformFor("com.ss.android.ugc.trill"))
        assertEquals(Platform.SNAPCHAT, PolicyEvaluator.platformFor("com.snapchat.android"))
        assertNull(PolicyEvaluator.platformFor("com.reddit.frontpage"))
    }

    @Test
    fun packagesForReturnsOnlyPackagesOfPlatformsInThePolicy() {
        val p = policy(rule(platform = Platform.TIKTOK), rule(platform = Platform.YOUTUBE), rule(platform = Platform.REDDIT))
        assertEquals(
            setOf(youtube, "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme"),
            PolicyEvaluator.packagesFor(p)
        )
    }

    @Test
    fun onlyReelsShortsAndSpotlightAreEnforcedOnAndroid() {
        assertEquals(
            setOf(BlockedSurface.REELS_TAB, BlockedSurface.SHORTS_PLAYER, BlockedSurface.SPOTLIGHT),
            BlockedSurface.entries.filter { it.isEnforcedOnAndroid }.toSet()
        )
    }
}
