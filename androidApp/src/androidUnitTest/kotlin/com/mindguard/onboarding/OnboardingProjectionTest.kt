package com.mindguard.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingProjectionTest {

    private fun answers(
        apps: Set<TrackedApp> = setOf(TrackedApp.INSTAGRAM, TrackedApp.YOUTUBE),
        dailyMinutes: Int = 120,
        measured: Map<TrackedApp, Int> = emptyMap(),
        share: ShortFormShare = ShortFormShare.ABOUT_HALF,
        motivations: Set<Motivation> = emptySet(),
        ageBand: AgeBand? = null
    ) = OnboardingAnswers(
        apps = apps,
        dailyMinutes = dailyMinutes,
        measuredMinutesByApp = measured,
        shortFormShare = share,
        motivations = motivations,
        ageBand = ageBand
    )

    @Test
    fun designDocExampleTwoHoursAboutHalfIsAnHourOfShortFormAndThreeAndAHalfYears() {
        val p = projectionFor(answers())
        assertEquals(60, p.dailyShortFormMinutes)
        assertEquals(365, p.yearlyHours)
        assertEquals(15, p.yearlyDays)
        assertEquals(3.5, p.lifetimeYears)
        assertEquals(3.5, p.reclaimedWeeklyHours)
    }

    @Test
    fun selfReportedMinutesAreUsedWhenNothingWasMeasured() {
        val a = answers(dailyMinutes = 200, share = ShortFormShare.ABOUT_HALF)
        assertEquals(false, a.isMeasured)
        assertEquals(100, projectionFor(a).dailyShortFormMinutes)
    }

    @Test
    fun measuredMinutesForSelectedAppsReplaceTheSelfReportedTotal() {
        val a = answers(
            apps = setOf(TrackedApp.INSTAGRAM, TrackedApp.YOUTUBE),
            dailyMinutes = 500,
            measured = mapOf(TrackedApp.INSTAGRAM to 70, TrackedApp.YOUTUBE to 50, TrackedApp.TIKTOK to 400)
        )
        assertEquals(true, a.isMeasured)
        assertEquals(60, projectionFor(a).dailyShortFormMinutes)
    }

    @Test
    fun eachShortFormShareScalesTheDailyTotal() {
        assertEquals(80, projectionFor(answers(dailyMinutes = 100, share = ShortFormShare.ALMOST_ALL)).dailyShortFormMinutes)
        assertEquals(50, projectionFor(answers(dailyMinutes = 100, share = ShortFormShare.ABOUT_HALF)).dailyShortFormMinutes)
        assertEquals(25, projectionFor(answers(dailyMinutes = 100, share = ShortFormShare.A_LITTLE)).dailyShortFormMinutes)
        assertEquals(60, projectionFor(answers(dailyMinutes = 100, share = ShortFormShare.NOT_SURE)).dailyShortFormMinutes)
    }

    @Test
    fun shortFormMinutesRoundToTheNearestMinute() {
        assertEquals(23, projectionFor(answers(dailyMinutes = 90, share = ShortFormShare.A_LITTLE)).dailyShortFormMinutes)
        assertEquals(22, projectionFor(answers(dailyMinutes = 89, share = ShortFormShare.A_LITTLE)).dailyShortFormMinutes)
    }

    @Test
    fun yearlyHoursAndDaysAreRounded() {
        val p = projectionFor(answers(dailyMinutes = 180, share = ShortFormShare.A_LITTLE))
        assertEquals(45, p.dailyShortFormMinutes)
        assertEquals(274, p.yearlyHours)
        assertEquals(11, p.yearlyDays)
        val mirror = projectionFor(answers(dailyMinutes = 135, share = ShortFormShare.ALMOST_ALL))
        assertEquals(108, mirror.dailyShortFormMinutes)
        assertEquals(657, mirror.yearlyHours)
        assertEquals(27, mirror.yearlyDays)
    }

    @Test
    fun ageDefaultsToTwentyTwoWhenNoBandIsGiven() {
        assertEquals(22, DEFAULT_AGE)
        assertEquals(3.5, projectionFor(answers(ageBand = null)).lifetimeYears)
    }

    @Test
    fun eachAgeBandUsesItsMidAge() {
        assertEquals(3.9, projectionFor(answers(ageBand = AgeBand.UNDER_18)).lifetimeYears)
        assertEquals(3.6, projectionFor(answers(ageBand = AgeBand.AGE_18_24)).lifetimeYears)
        assertEquals(3.0, projectionFor(answers(ageBand = AgeBand.AGE_25_34)).lifetimeYears)
        assertEquals(2.3, projectionFor(answers(ageBand = AgeBand.AGE_35_PLUS)).lifetimeYears)
    }

    @Test
    fun lifetimeYearsAndWeeklyHoursRoundToOneDecimal() {
        val p = projectionFor(answers(dailyMinutes = 135, share = ShortFormShare.ALMOST_ALL))
        assertEquals(6.3, p.lifetimeYears)
        assertEquals(6.3, p.reclaimedWeeklyHours)
    }

    @Test
    fun lifetimeYearsAreNeverNegativeAboveSeventyEight() {
        assertEquals(0.0, lifetimeYearsFor(shortFormMinutes = 60, age = 90))
        assertEquals(0.0, lifetimeYearsFor(shortFormMinutes = 60, age = 78))
    }

    @Test
    fun reclaimFactorIsAConservativeHalf() {
        assertEquals(0.5, RECLAIM_FACTOR)
    }

    @Test
    fun noMotivationsFillsWithFriendsThenSleep() {
        val p = projectionFor(answers())
        assertEquals(
            listOf(
                Equivalent("🫂", "91 evenings with friends a year"),
                Equivalent("😴", "15 extra hours of sleep a month")
            ),
            p.equivalents
        )
    }

    @Test
    fun oneMotivationIsFollowedByTheFirstUnusedFiller() {
        val sleepOnly = projectionFor(answers(motivations = setOf(Motivation.SLEEP))).equivalents
        assertEquals(listOf("😴", "🫂"), sleepOnly.map { it.emoji })
        val friendsOnly = projectionFor(answers(motivations = setOf(Motivation.FRIENDS_FAMILY))).equivalents
        assertEquals(listOf("🫂", "😴"), friendsOnly.map { it.emoji })
        val gymOnly = projectionFor(answers(motivations = setOf(Motivation.GYM))).equivalents
        assertEquals(listOf("🏋️", "🫂"), gymOnly.map { it.emoji })
    }

    @Test
    fun moreThanTwoMotivationsKeepTheFirstTwoInEnumOrder() {
        val p = projectionFor(
            answers(motivations = setOf(Motivation.SIDE_PROJECT, Motivation.READING, Motivation.GYM))
        )
        assertEquals(
            listOf(
                Equivalent("🏋️", "15 gym sessions a month"),
                Equivalent("📖", "30 books a year")
            ),
            p.equivalents
        )
    }

    @Test
    fun studyAndSideProjectUseTheirOwnUnits() {
        val p = projectionFor(answers(motivations = setOf(Motivation.STUDY, Motivation.SIDE_PROJECT)))
        assertEquals(
            listOf(
                Equivalent("📚", "10 focused study sessions a month"),
                Equivalent("🚀", "5 build sessions a month")
            ),
            p.equivalents
        )
    }

    @Test
    fun equivalentsAreNeverLessThanOne() {
        val p = projectionFor(answers(dailyMinutes = 4, share = ShortFormShare.A_LITTLE, motivations = setOf(Motivation.READING)))
        assertEquals(Equivalent("📖", "1 book a year"), p.equivalents.first())
        assertEquals(2, p.equivalents.size)
    }

    @Test
    fun formatMinutesUsesHoursAndMinutes() {
        assertEquals("0 m", formatMinutes(0))
        assertEquals("45 m", formatMinutes(45))
        assertEquals("1 h 48 m", formatMinutes(108))
        assertEquals("2 h", formatMinutes(120))
        assertEquals("1 h 5 m", formatMinutes(65))
    }
}
