package com.mindguard.ui.present

const val GUARD_PAUSE_DURATION_MS = 15 * 60 * 1000L

private const val FIRST_TURN_OFF_WAIT_SECONDS = 15
private const val MAX_TURN_OFF_WAIT_SECONDS = 60
private const val MIN_REASON_LENGTH = 10

enum class QuestAction { NONE, DIAL, MESSAGE }

data class SocialQuest(
    val emoji: String,
    val title: String,
    val detail: String,
    val action: QuestAction = QuestAction.NONE
)

val SOCIAL_QUESTS = listOf(
    SocialQuest("📞", "Call someone you miss", "A 5-minute call beats 50 Reels. Who haven't you talked to this week?", QuestAction.DIAL),
    SocialQuest("💬", "Make a plan with a friend", "Text someone: \"Chai / coffee later?\" Real plans, real people.", QuestAction.MESSAGE),
    SocialQuest("🗣️", "Talk to someone nearby", "Walk over and ask a family member, roommate or colleague how their day is going."),
    SocialQuest("🙏", "Thank someone", "Send a thank-you to someone who helped you recently.", QuestAction.MESSAGE),
    SocialQuest("🚶", "Step outside for 10 minutes", "Leave the phone in your pocket. Notice three things around you."),
    SocialQuest("🍽️", "Phone-free meal", "Eat your next meal with someone, phone face-down on the table.")
)

fun requiresTurnOffGuard(currentlyEnabled: Boolean, requested: Boolean): Boolean =
    currentlyEnabled && !requested

fun turnOffCountdownSeconds(turnOffsToday: Long): Int {
    val doublings = turnOffsToday.coerceIn(0L, 2L).toInt()
    return (FIRST_TURN_OFF_WAIT_SECONDS shl doublings).coerceAtMost(MAX_TURN_OFF_WAIT_SECONDS)
}

fun canConfirmTurnOff(reason: String): Boolean = reason.trim().length >= MIN_REASON_LENGTH

fun socialQuestAt(index: Int): SocialQuest = SOCIAL_QUESTS[index.mod(SOCIAL_QUESTS.size)]
