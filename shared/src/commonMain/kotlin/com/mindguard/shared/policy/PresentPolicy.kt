package com.mindguard.shared.policy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the Zod schema in server/src/policy.ts. Keep the two in sync.

@Serializable
data class PresentPolicy(
    @SerialName("policy_version") val policyVersion: String,
    val summary: String,
    val rules: List<PolicyRule>,
    @SerialName("pause_card_settings") val pauseCardSettings: PauseCardSettings? = null
)

@Serializable
data class PolicyRule(
    val platform: Platform,
    @SerialName("target_surfaces") val targetSurfaces: List<BlockedSurface>,
    @SerialName("allowed_surfaces") val allowedSurfaces: List<AllowedSurface>,
    val schedule: Schedule,
    @SerialName("whitelisted_entities") val whitelistedEntities: List<String> = emptyList()
)

@Serializable
enum class Platform {
    @SerialName("instagram") INSTAGRAM,
    @SerialName("youtube") YOUTUBE,
    @SerialName("tiktok") TIKTOK,
    @SerialName("snapchat") SNAPCHAT,
    @SerialName("reddit") REDDIT,
    @SerialName("linkedin") LINKEDIN
}

@Serializable
enum class BlockedSurface(val isEnforcedOnAndroid: Boolean) {
    @SerialName("reels_tab") REELS_TAB(isEnforcedOnAndroid = true),
    @SerialName("shorts_player") SHORTS_PLAYER(isEnforcedOnAndroid = true),
    @SerialName("home_feed") HOME_FEED(isEnforcedOnAndroid = false),
    @SerialName("explore_grid") EXPLORE_GRID(isEnforcedOnAndroid = false),
    @SerialName("spotlight") SPOTLIGHT(isEnforcedOnAndroid = true)
}

@Serializable
enum class AllowedSurface {
    @SerialName("direct_messages") DIRECT_MESSAGES,
    @SerialName("search") SEARCH,
    @SerialName("subscriptions") SUBSCRIPTIONS,
    @SerialName("profile") PROFILE,
    @SerialName("creator_channel") CREATOR_CHANNEL
}

@Serializable
data class Schedule(
    val mode: ScheduleMode,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("daily_limit_minutes") val dailyLimitMinutes: Int? = null
)

@Serializable
enum class ScheduleMode {
    @SerialName("always") ALWAYS,
    @SerialName("time_window") TIME_WINDOW,
    @SerialName("daily_allowance") DAILY_ALLOWANCE
}

@Serializable
data class PauseCardSettings(
    @SerialName("delay_seconds") val delaySeconds: Int? = null,
    @SerialName("prompt_reflection") val promptReflection: Boolean? = null,
    @SerialName("side_quest_category") val sideQuestCategory: String? = null
)

sealed interface CompileResponse {
    data class Compiled(val policy: PresentPolicy) : CompileResponse
    data class Rejected(val reason: String) : CompileResponse
    data class Failed(val message: String) : CompileResponse
}
