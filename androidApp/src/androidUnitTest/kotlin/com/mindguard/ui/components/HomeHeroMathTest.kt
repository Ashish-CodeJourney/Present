package com.mindguard.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeHeroMathTest {

    @Test
    fun noBlocksMeansAnEmptyRing() {
        assertEquals(0f, reclaimedRingProgress(blocksToday = 0L))
    }

    @Test
    fun tenBlocksFillsHalfOfTheDefaultEightyFourMinuteGoal() {
        assertEquals(0.5f, reclaimedRingProgress(blocksToday = 10L))
    }

    @Test
    fun ringNeverOverflowsPastFull() {
        assertEquals(1f, reclaimedRingProgress(blocksToday = 500L))
    }

    @Test
    fun customGoalScalesProgress() {
        assertEquals(0.7f, reclaimedRingProgress(blocksToday = 10L, goalMinutes = 60L))
    }

    @Test
    fun nonPositiveGoalShowsAFullRingRatherThanDividingByZero() {
        assertEquals(1f, reclaimedRingProgress(blocksToday = 3L, goalMinutes = 0L))
    }

    @Test
    fun greetingFollowsTheTimeOfDay() {
        assertEquals("Good night", greetingForHour(3))
        assertEquals("Good morning", greetingForHour(5))
        assertEquals("Good morning", greetingForHour(11))
        assertEquals("Good afternoon", greetingForHour(12))
        assertEquals("Good afternoon", greetingForHour(16))
        assertEquals("Good evening", greetingForHour(17))
        assertEquals("Good evening", greetingForHour(21))
        assertEquals("Good night", greetingForHour(22))
    }
}
