package com.mindguard.shared.policy

class PolicyEvaluator {

    companion object {
        private val PACKAGES_BY_PLATFORM: Map<Platform, Set<String>> = mapOf(
            Platform.INSTAGRAM to setOf("com.instagram.android"),
            Platform.YOUTUBE   to setOf("com.google.android.youtube"),
            Platform.TIKTOK    to setOf(
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
                "com.ss.android.ugc.aweme"
            ),
            Platform.SNAPCHAT  to setOf("com.snapchat.android")
        )

        private val CLOCK_TIME = Regex("""^([01]?[0-9]|2[0-3]):([0-5][0-9])$""")

        fun platformFor(packageName: String): Platform? =
            PACKAGES_BY_PLATFORM.entries.firstOrNull { packageName in it.value }?.key

        fun packagesFor(policy: PresentPolicy): Set<String> =
            policy.rules.flatMap { PACKAGES_BY_PLATFORM[it.platform].orEmpty() }.toSet()
    }

    fun shouldEnforce(policy: PresentPolicy, packageName: String, surface: BlockedSurface, minuteOfDay: Int): Boolean {
        val platform = platformFor(packageName) ?: return false
        return policy.rules.any { rule ->
            rule.platform == platform &&
                surface in rule.targetSurfaces &&
                isActive(rule.schedule, minuteOfDay)
        }
    }

    // DAILY_ALLOWANCE is enforced like ALWAYS for the MVP: there is no usage tracking yet.
    // Malformed or missing window times also fall back to ALWAYS, so a bad policy protects the user.
    private fun isActive(schedule: Schedule, minuteOfDay: Int): Boolean = when (schedule.mode) {
        ScheduleMode.ALWAYS, ScheduleMode.DAILY_ALLOWANCE -> true
        ScheduleMode.TIME_WINDOW -> isInWindow(schedule.startTime, schedule.endTime, minuteOfDay)
    }

    private fun isInWindow(startTime: String?, endTime: String?, minuteOfDay: Int): Boolean {
        val start = parseMinuteOfDay(startTime) ?: return true
        val end = parseMinuteOfDay(endTime) ?: return true
        return when {
            start < end -> minuteOfDay in start until end
            start > end -> minuteOfDay >= start || minuteOfDay < end
            else -> true
        }
    }

    private fun parseMinuteOfDay(time: String?): Int? {
        val match = time?.let { CLOCK_TIME.matchEntire(it.trim()) } ?: return null
        val (hours, minutes) = match.destructured
        return hours.toInt() * 60 + minutes.toInt()
    }
}
