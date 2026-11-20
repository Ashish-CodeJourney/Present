package com.mindguard.onboarding

import kotlin.math.floor
import kotlin.math.roundToInt

enum class TrackedApp(val label: String, val emoji: String, val packageNames: List<String>) {
    INSTAGRAM("Instagram", "📸", listOf("com.instagram.android")),
    YOUTUBE("YouTube", "▶️", listOf("com.google.android.youtube")),
    SNAPCHAT("Snapchat", "👻", listOf("com.snapchat.android")),
    TIKTOK("TikTok", "🎵", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme"))
}

enum class ShortFormShare(val label: String, val fraction: Double) {
    ALMOST_ALL("Almost all of it", 0.8),
    ABOUT_HALF("About half", 0.5),
    A_LITTLE("A little", 0.25),
    NOT_SURE("Not sure", 0.6)
}

enum class HardestTime(val label: String, val emoji: String) {
    MORNING("Morning, in bed", "🌅"),
    WORK_HOURS("During study / work", "💻"),
    LATE_NIGHT("Late at night", "🌙"),
    ALL_DAY("All day, honestly", "🔁")
}

enum class Struggle(val label: String) {
    LOST_IN_DM("I open it for a DM and lose 30 minutes"),
    SLEEP_LATE("I sleep later than I want"),
    PROCRASTINATE("I procrastinate on study or work"),
    FEEL_WORSE("I feel worse afterwards")
}

enum class Motivation(val label: String, val emoji: String) {
    FRIENDS_FAMILY("Friends & family", "🫂"),
    SLEEP("Sleep", "😴"),
    GYM("Gym / sport", "🏋️"),
    STUDY("Study / exam prep", "📚"),
    READING("Reading", "📖"),
    SIDE_PROJECT("A side project", "🚀")
}

enum class KeepSurface(val label: String) {
    DMS("DMs"),
    SEARCH("Search"),
    SUBSCRIPTIONS("YouTube subscriptions"),
    PROFILES("Profiles")
}

enum class AgeBand(val label: String, val midAge: Int) {
    UNDER_18("Under 18", 16),
    AGE_18_24("18–24", 21),
    AGE_25_34("25–34", 30),
    AGE_35_PLUS("35+", 42)
}

data class OnboardingAnswers(
    val apps: Set<TrackedApp> = emptySet(),
    val dailyMinutes: Int = 90,
    val measuredMinutesByApp: Map<TrackedApp, Int> = emptyMap(),
    val shortFormShare: ShortFormShare = ShortFormShare.NOT_SURE,
    val hardestTime: HardestTime? = null,
    val struggles: Set<Struggle> = emptySet(),
    val motivations: Set<Motivation> = emptySet(),
    val keep: Set<KeepSurface> = setOf(KeepSurface.DMS, KeepSurface.SEARCH),
    val ageBand: AgeBand? = null
) {
    val isMeasured: Boolean get() = measuredMinutesByApp.isNotEmpty()
}

data class Equivalent(val emoji: String, val text: String)

data class Projection(
    val dailyShortFormMinutes: Int,
    val yearlyHours: Int,
    val yearlyDays: Int,
    val lifetimeYears: Double,
    val reclaimedWeeklyHours: Double,
    val equivalents: List<Equivalent>
)

const val DEFAULT_AGE = 22
const val RECLAIM_FACTOR = 0.5

private const val LIFE_EXPECTANCY = 78
private const val WAKING_HOURS = 16.0
private const val WEEKS_PER_YEAR = 52.0
private const val MONTHS_PER_YEAR = 12.0

private fun roundOneDecimal(value: Double): Double = (value * 10).roundToInt() / 10.0

private fun dailyTotalMinutes(answers: OnboardingAnswers): Int =
    if (answers.isMeasured) {
        answers.measuredMinutesByApp.filterKeys { it in answers.apps }.values.sum()
    } else {
        answers.dailyMinutes
    }

internal fun lifetimeYearsFor(shortFormMinutes: Int, age: Int): Double {
    val remainingYears = (LIFE_EXPECTANCY - age).coerceAtLeast(0)
    return roundOneDecimal(shortFormMinutes / 60.0 * remainingYears / WAKING_HOURS)
}

private enum class Period { MONTH, YEAR }

private data class EquivalentUnit(val hours: Double, val period: Period, val phrase: String, val singular: String)

private fun unitFor(motivation: Motivation): EquivalentUnit = when (motivation) {
    Motivation.FRIENDS_FAMILY -> EquivalentUnit(2.0, Period.YEAR, "evenings with friends a year", "evening with friends a year")
    Motivation.SLEEP -> EquivalentUnit(1.0, Period.MONTH, "extra hours of sleep a month", "extra hour of sleep a month")
    Motivation.GYM -> EquivalentUnit(1.0, Period.MONTH, "gym sessions a month", "gym session a month")
    Motivation.STUDY -> EquivalentUnit(1.5, Period.MONTH, "focused study sessions a month", "focused study session a month")
    Motivation.READING -> EquivalentUnit(6.0, Period.YEAR, "books a year", "book a year")
    Motivation.SIDE_PROJECT -> EquivalentUnit(3.0, Period.MONTH, "build sessions a month", "build session a month")
}

private val EQUIVALENT_FILLERS = listOf(Motivation.FRIENDS_FAMILY, Motivation.SLEEP)

private fun equivalentMotivations(chosen: Set<Motivation>): List<Motivation> =
    (Motivation.entries.filter { it in chosen }.take(2) + EQUIVALENT_FILLERS).distinct().take(2)

private fun equivalentFor(motivation: Motivation, weeklyHours: Double): Equivalent {
    val unit = unitFor(motivation)
    val hoursInPeriod = when (unit.period) {
        Period.MONTH -> weeklyHours * WEEKS_PER_YEAR / MONTHS_PER_YEAR
        Period.YEAR -> weeklyHours * WEEKS_PER_YEAR
    }
    val count = floor(hoursInPeriod / unit.hours).toInt().coerceAtLeast(1)
    return Equivalent(motivation.emoji, "$count ${if (count == 1) unit.singular else unit.phrase}")
}

fun projectionFor(answers: OnboardingAnswers): Projection {
    val shortForm = (dailyTotalMinutes(answers) * answers.shortFormShare.fraction).roundToInt()
    val yearlyHoursExact = shortForm * 365 / 60.0
    val weeklyHours = shortForm * 7 * RECLAIM_FACTOR / 60.0
    return Projection(
        dailyShortFormMinutes = shortForm,
        yearlyHours = yearlyHoursExact.roundToInt(),
        yearlyDays = (yearlyHoursExact / 24).roundToInt(),
        lifetimeYears = lifetimeYearsFor(shortForm, answers.ageBand?.midAge ?: DEFAULT_AGE),
        reclaimedWeeklyHours = roundOneDecimal(weeklyHours),
        equivalents = equivalentMotivations(answers.motivations).map { equivalentFor(it, weeklyHours) }
    )
}

private val DEFAULT_INTENT_APPS = setOf(TrackedApp.INSTAGRAM, TrackedApp.YOUTUBE)

private fun keepableSurfaces(app: TrackedApp): List<KeepSurface> = when (app) {
    TrackedApp.INSTAGRAM -> listOf(KeepSurface.DMS, KeepSurface.SEARCH, KeepSurface.PROFILES)
    TrackedApp.YOUTUBE -> listOf(KeepSurface.SUBSCRIPTIONS, KeepSurface.SEARCH)
    TrackedApp.SNAPCHAT -> listOf(KeepSurface.DMS, KeepSurface.SEARCH)
    TrackedApp.TIKTOK -> listOf(KeepSurface.DMS, KeepSurface.SEARCH)
}

private fun surfacePhrase(surface: KeepSurface): String = when (surface) {
    KeepSurface.DMS -> "DMs"
    KeepSurface.SEARCH -> "search"
    KeepSurface.SUBSCRIPTIONS -> "subscriptions"
    KeepSurface.PROFILES -> "profiles"
}

private fun blockedPhrase(app: TrackedApp): String = when (app) {
    TrackedApp.INSTAGRAM -> "Instagram Reels"
    TrackedApp.YOUTUBE -> "YouTube Shorts"
    TrackedApp.SNAPCHAT -> "Snapchat Spotlight"
    TrackedApp.TIKTOK -> "the TikTok feed"
}

private fun schedulePhrase(time: HardestTime?): String = when (time) {
    HardestTime.LATE_NIGHT -> "after 9 PM"
    HardestTime.MORNING -> "before 10 AM"
    HardestTime.WORK_HOURS -> "from 9 AM to 6 PM"
    HardestTime.ALL_DAY, null -> "all day"
}

private fun naturalList(items: List<String>): String =
    if (items.size <= 1) items.joinToString("") else items.dropLast(1).joinToString(", ") + " and " + items.last()

private fun clauseList(items: List<String>): String =
    if (items.size <= 1) items.joinToString("") else items.dropLast(1).joinToString(", ") + ", and " + items.last()

private fun keepClause(app: TrackedApp, keep: Set<KeepSurface>): String? {
    val kept = keepableSurfaces(app).filter { it in keep }.map(::surfacePhrase)
    return if (kept.isEmpty()) null else "my ${app.label} ${naturalList(kept)}"
}

fun intentFor(answers: OnboardingAnswers): String {
    val apps = TrackedApp.entries.filter { it in answers.apps.ifEmpty { DEFAULT_INTENT_APPS } }
    val keepClauses = apps.mapNotNull { keepClause(it, answers.keep) }
    val block = "${naturalList(apps.map(::blockedPhrase))} ${schedulePhrase(answers.hardestTime)}."
    return if (keepClauses.isEmpty()) "Block $block" else "Keep ${clauseList(keepClauses)}, but block $block"
}

fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest m"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest m"
    }
}
