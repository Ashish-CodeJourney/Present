package com.mindguard.ui.present

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TurnOffGuardTest {

    @Test
    fun `switching protection off goes through the guard`() {
        assertTrue(requiresTurnOffGuard(currentlyEnabled = true, requested = false))
    }

    @Test
    fun `switching protection on is instant`() {
        assertFalse(requiresTurnOffGuard(currentlyEnabled = false, requested = true))
    }

    @Test
    fun `re-sending the current state never shows the guard`() {
        assertFalse(requiresTurnOffGuard(currentlyEnabled = true, requested = true))
        assertFalse(requiresTurnOffGuard(currentlyEnabled = false, requested = false))
    }

    @Test
    fun `the first turn-off of the day waits 15 seconds`() {
        assertEquals(15, turnOffCountdownSeconds(turnOffsToday = 0))
    }

    @Test
    fun `each extra turn-off the same day doubles the wait`() {
        assertEquals(30, turnOffCountdownSeconds(turnOffsToday = 1))
        assertEquals(60, turnOffCountdownSeconds(turnOffsToday = 2))
    }

    @Test
    fun `the wait never exceeds a minute`() {
        assertEquals(60, turnOffCountdownSeconds(turnOffsToday = 3))
        assertEquals(60, turnOffCountdownSeconds(turnOffsToday = 40))
    }

    @Test
    fun `a corrupt negative count falls back to the first wait`() {
        assertEquals(15, turnOffCountdownSeconds(turnOffsToday = -1))
    }

    @Test
    fun `turning off needs a real reason, not a single word`() {
        assertFalse(canConfirmTurnOff(""))
        assertFalse(canConfirmTurnOff("   bored   "))
        assertTrue(canConfirmTurnOff("Need to post my event"))
    }

    @Test
    fun `quests are social or real-world and some can be started from the phone`() {
        assertTrue(SOCIAL_QUESTS.size >= 5)
        assertTrue(SOCIAL_QUESTS.any { it.action == QuestAction.DIAL })
        assertTrue(SOCIAL_QUESTS.any { it.action == QuestAction.MESSAGE })
    }

    @Test
    fun `shuffling walks through every quest before repeating`() {
        val seen = (0 until SOCIAL_QUESTS.size).map { socialQuestAt(it) }.toSet()
        assertEquals(SOCIAL_QUESTS.toSet(), seen)
        assertEquals(socialQuestAt(0), socialQuestAt(SOCIAL_QUESTS.size))
    }

    @Test
    fun `any index picks a valid quest, including negative seeds`() {
        assertEquals(SOCIAL_QUESTS.last(), socialQuestAt(-1))
        assertNotEquals(socialQuestAt(0), socialQuestAt(1))
    }

    @Test
    fun `the gentler alternative is a 15 minute pause`() {
        assertEquals(15 * 60 * 1000L, GUARD_PAUSE_DURATION_MS)
    }
}
