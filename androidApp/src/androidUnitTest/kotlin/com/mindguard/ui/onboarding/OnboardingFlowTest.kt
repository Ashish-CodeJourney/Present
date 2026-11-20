package com.mindguard.ui.onboarding

import com.mindguard.onboarding.HardestTime
import com.mindguard.onboarding.Motivation
import com.mindguard.onboarding.OnboardingAnswers
import com.mindguard.onboarding.Projection
import com.mindguard.onboarding.TrackedApp
import com.mindguard.ui.present.SOCIAL_QUESTS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingFlowTest {

    private fun answers(
        apps: Set<TrackedApp> = setOf(TrackedApp.INSTAGRAM),
        hardestTime: HardestTime? = HardestTime.LATE_NIGHT
    ) = OnboardingAnswers(apps = apps, hardestTime = hardestTime)

    private fun walkForward(measured: Boolean): List<OnboardingStep> =
        generateSequence(OnboardingStep.SPLASH) { step ->
            nextStep(step, answers(), measured).takeIf { it != step }
        }.toList()

    @Test
    fun `the self-reported path visits every screen of the design in order`() {
        assertEquals(
            listOf(
                OnboardingStep.SPLASH,
                OnboardingStep.WELCOME,
                OnboardingStep.APPS,
                OnboardingStep.DAILY_TIME,
                OnboardingStep.SHORT_FORM,
                OnboardingStep.HARDEST_TIME,
                OnboardingStep.STRUGGLES,
                OnboardingStep.MOTIVATIONS,
                OnboardingStep.KEEP,
                OnboardingStep.AGE,
                OnboardingStep.BUILDING,
                OnboardingStep.REVEAL_MIRROR,
                OnboardingStep.REVEAL_UPSIDE,
                OnboardingStep.PLAN,
                OnboardingStep.COMMIT,
                OnboardingStep.PERMISSION,
                OnboardingStep.DONE
            ),
            walkForward(measured = false)
        )
    }

    @Test
    fun `usage access is a detour that is never on the default path`() {
        assertFalse(OnboardingStep.USAGE_ACCESS in walkForward(measured = true))
    }

    @Test
    fun `once measured, usage access continues to the short-form question`() {
        assertEquals(OnboardingStep.SHORT_FORM, nextStep(OnboardingStep.USAGE_ACCESS, answers(), measured = true))
    }

    @Test
    fun `leaving usage access without a measurement returns to the slider`() {
        assertEquals(OnboardingStep.DAILY_TIME, nextStep(OnboardingStep.USAGE_ACCESS, answers(), measured = false))
    }

    @Test
    fun `done is the last step`() {
        assertEquals(OnboardingStep.DONE, nextStep(OnboardingStep.DONE, answers(), measured = false))
    }

    @Test
    fun `back from the short-form question returns to how the time was given`() {
        assertEquals(OnboardingStep.DAILY_TIME, previousStep(OnboardingStep.SHORT_FORM, answers(), measured = false))
        assertEquals(OnboardingStep.USAGE_ACCESS, previousStep(OnboardingStep.SHORT_FORM, answers(), measured = true))
    }

    @Test
    fun `back from usage access returns to the slider`() {
        assertEquals(OnboardingStep.DAILY_TIME, previousStep(OnboardingStep.USAGE_ACCESS, answers(), measured = true))
    }

    @Test
    fun `back from the mirror skips the plan-building animation`() {
        assertEquals(OnboardingStep.AGE, previousStep(OnboardingStep.REVEAL_MIRROR, answers(), measured = false))
    }

    @Test
    fun `back walks the questions in reverse`() {
        assertEquals(OnboardingStep.APPS, previousStep(OnboardingStep.DAILY_TIME, answers(), measured = false))
        assertEquals(OnboardingStep.KEEP, previousStep(OnboardingStep.AGE, answers(), measured = false))
        assertEquals(OnboardingStep.REVEAL_UPSIDE, previousStep(OnboardingStep.PLAN, answers(), measured = false))
        assertEquals(OnboardingStep.PERMISSION, previousStep(OnboardingStep.DONE, answers(), measured = false))
    }

    @Test
    fun `there is nothing before the welcome screen to go back to`() {
        assertEquals(OnboardingStep.WELCOME, previousStep(OnboardingStep.WELCOME, answers(), measured = false))
        assertEquals(OnboardingStep.SPLASH, previousStep(OnboardingStep.SPLASH, answers(), measured = false))
    }

    @Test
    fun `the apps question needs at least one app`() {
        assertFalse(canContinue(OnboardingStep.APPS, answers(apps = emptySet())))
        assertTrue(canContinue(OnboardingStep.APPS, answers(apps = setOf(TrackedApp.YOUTUBE))))
    }

    @Test
    fun `the other multi-selects can be left empty`() {
        val empty = OnboardingAnswers(apps = setOf(TrackedApp.INSTAGRAM), struggles = emptySet(), motivations = emptySet(), keep = emptySet())
        assertTrue(canContinue(OnboardingStep.STRUGGLES, empty))
        assertTrue(canContinue(OnboardingStep.MOTIVATIONS, empty))
        assertTrue(canContinue(OnboardingStep.KEEP, empty))
    }

    @Test
    fun `the hardest-time question needs a pick`() {
        assertFalse(canContinue(OnboardingStep.HARDEST_TIME, answers(hardestTime = null)))
        assertTrue(canContinue(OnboardingStep.HARDEST_TIME, answers(hardestTime = HardestTime.MORNING)))
    }

    @Test
    fun `the age band is optional`() {
        assertTrue(canContinue(OnboardingStep.AGE, answers()))
    }

    @Test
    fun `progress grows through the questions and is full on the last one`() {
        val questions = listOf(
            OnboardingStep.APPS, OnboardingStep.DAILY_TIME, OnboardingStep.SHORT_FORM, OnboardingStep.HARDEST_TIME,
            OnboardingStep.STRUGGLES, OnboardingStep.MOTIVATIONS, OnboardingStep.KEEP, OnboardingStep.AGE
        )
        val progress = questions.map(::progressFor)
        assertTrue(progress.zipWithNext().all { (a, b) -> b > a })
        assertTrue(progress.first() > 0f)
        assertEquals(1f, progress.last())
    }

    @Test
    fun `usage access sits at the same progress as the time question`() {
        assertEquals(progressFor(OnboardingStep.DAILY_TIME), progressFor(OnboardingStep.USAGE_ACCESS))
    }

    @Test
    fun `only question steps show the progress bar`() {
        assertTrue(showsProgress(OnboardingStep.APPS))
        assertTrue(showsProgress(OnboardingStep.USAGE_ACCESS))
        assertFalse(showsProgress(OnboardingStep.WELCOME))
        assertFalse(showsProgress(OnboardingStep.REVEAL_MIRROR))
    }

    @Test
    fun `only the optional steps can be skipped`() {
        assertTrue(isSkippable(OnboardingStep.AGE))
        assertTrue(isSkippable(OnboardingStep.USAGE_ACCESS))
        assertFalse(isSkippable(OnboardingStep.APPS))
    }

    @Test
    fun `people who want time for friends get a social quest first`() {
        assertEquals(SOCIAL_QUESTS[0], firstQuestFor(setOf(Motivation.STUDY, Motivation.FRIENDS_FAMILY)))
    }

    @Test
    fun `each motivation leads to a matching real-world quest`() {
        val quests = Motivation.entries.map { firstQuestFor(setOf(it)) }
        assertEquals(Motivation.entries.size, quests.toSet().size)
    }

    @Test
    fun `with no motivation picked there is still a quest`() {
        assertTrue(firstQuestFor(emptySet()) in SOCIAL_QUESTS)
    }

    @Test
    fun `the share text quotes the lifetime years to one decimal`() {
        val projection = Projection(
            dailyShortFormMinutes = 108, yearlyHours = 657, yearlyDays = 27,
            lifetimeYears = 6.2849, reclaimedWeeklyHours = 6.3, equivalents = emptyList()
        )
        assertEquals(
            "Reels were set to take 6.3 years of my waking life. I'm choosing to be present. #PresentApp",
            shareTextFor(projection)
        )
    }
}
