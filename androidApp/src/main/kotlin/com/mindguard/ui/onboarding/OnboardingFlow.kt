package com.mindguard.ui.onboarding

import com.mindguard.onboarding.Motivation
import com.mindguard.onboarding.OnboardingAnswers
import com.mindguard.onboarding.Projection
import com.mindguard.ui.present.SOCIAL_QUESTS
import com.mindguard.ui.present.SocialQuest
import java.util.Locale

enum class OnboardingStep {
    SPLASH,
    WELCOME,
    APPS,
    DAILY_TIME,
    USAGE_ACCESS,
    SHORT_FORM,
    HARDEST_TIME,
    STRUGGLES,
    MOTIVATIONS,
    KEEP,
    AGE,
    BUILDING,
    REVEAL_MIRROR,
    REVEAL_UPSIDE,
    PLAN,
    COMMIT,
    PERMISSION,
    DONE
}

private val QUESTION_STEPS = listOf(
    OnboardingStep.APPS,
    OnboardingStep.DAILY_TIME,
    OnboardingStep.SHORT_FORM,
    OnboardingStep.HARDEST_TIME,
    OnboardingStep.STRUGGLES,
    OnboardingStep.MOTIVATIONS,
    OnboardingStep.KEEP,
    OnboardingStep.AGE
)

private val MAIN_PATH: List<OnboardingStep> = OnboardingStep.entries.filter { it != OnboardingStep.USAGE_ACCESS }

@Suppress("UNUSED_PARAMETER")
fun nextStep(step: OnboardingStep, answers: OnboardingAnswers, measured: Boolean): OnboardingStep = when (step) {
    OnboardingStep.USAGE_ACCESS -> if (measured) OnboardingStep.SHORT_FORM else OnboardingStep.DAILY_TIME
    else -> MAIN_PATH.getOrElse(MAIN_PATH.indexOf(step) + 1) { step }
}

@Suppress("UNUSED_PARAMETER")
fun previousStep(step: OnboardingStep, answers: OnboardingAnswers, measured: Boolean): OnboardingStep = when (step) {
    OnboardingStep.SPLASH, OnboardingStep.WELCOME -> step
    OnboardingStep.USAGE_ACCESS -> OnboardingStep.DAILY_TIME
    OnboardingStep.SHORT_FORM -> if (measured) OnboardingStep.USAGE_ACCESS else OnboardingStep.DAILY_TIME
    OnboardingStep.REVEAL_MIRROR -> OnboardingStep.AGE
    else -> MAIN_PATH[MAIN_PATH.indexOf(step) - 1]
}

fun canContinue(step: OnboardingStep, answers: OnboardingAnswers): Boolean = when (step) {
    OnboardingStep.APPS -> answers.apps.isNotEmpty()
    OnboardingStep.HARDEST_TIME -> answers.hardestTime != null
    else -> true
}

private fun questionIndex(step: OnboardingStep): Int =
    QUESTION_STEPS.indexOf(if (step == OnboardingStep.USAGE_ACCESS) OnboardingStep.DAILY_TIME else step)

fun showsProgress(step: OnboardingStep): Boolean = questionIndex(step) >= 0

fun progressFor(step: OnboardingStep): Float {
    val index = questionIndex(step)
    if (index >= 0) return (index + 1).toFloat() / QUESTION_STEPS.size
    return if (step.ordinal < OnboardingStep.APPS.ordinal) 0f else 1f
}

fun isSkippable(step: OnboardingStep): Boolean =
    step == OnboardingStep.AGE || step == OnboardingStep.USAGE_ACCESS

fun isForward(from: OnboardingStep, to: OnboardingStep): Boolean = to.ordinal >= from.ordinal

private val QUEST_PRIORITY: List<Pair<Motivation, SocialQuest>> = listOf(
    Motivation.FRIENDS_FAMILY to SOCIAL_QUESTS[0],
    Motivation.GYM to SOCIAL_QUESTS[4],
    Motivation.SLEEP to SocialQuest("🌙", "Phone outside the bedroom tonight", "Charge it in another room. Your first Reels-free night starts now."),
    Motivation.STUDY to SocialQuest("📚", "One 25-minute focus block", "Pick one topic, set a timer, phone face-down until it rings."),
    Motivation.READING to SocialQuest("📖", "Read 10 pages", "The book you keep meaning to finish. Ten pages, right now."),
    Motivation.SIDE_PROJECT to SocialQuest("🚀", "Ship one tiny thing", "Spend 20 minutes on your side project. Small and done beats big and planned.")
)

fun firstQuestFor(motivations: Set<Motivation>): SocialQuest =
    QUEST_PRIORITY.firstOrNull { (motivation, _) -> motivation in motivations }?.second ?: SOCIAL_QUESTS[4]

fun shareTextFor(projection: Projection): String =
    "Reels were set to take ${String.format(Locale.US, "%.1f", projection.lifetimeYears)} years of my waking life. " +
        "I'm choosing to be present. #PresentApp"
