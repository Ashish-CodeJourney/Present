package com.mindguard.accessibility

object InstagramHomeTabStrategy {

    // Priority-ordered list of known Instagram home-tab resource IDs across builds (2022–2025).
    // Earlier entries are preferred; the list is exhaustive enough to cover most builds.
    val ORDERED_IDS = listOf(
        "com.instagram.android:id/feed_tab",
        "com.instagram.android:id/tab_feed",
        "com.instagram.android:id/home_tab",
        "com.instagram.android:id/navigation_home",
        "com.instagram.android:id/tab_bar_home_button",
        "com.instagram.android:id/tab_icon_0"
    )

    fun selectId(availableIds: Set<String>): String? =
        ORDERED_IDS.firstOrNull { it in availableIds }
}

enum class SafeTabAction { TAP_HOME_TAB, GO_BACK }

// Only the Reels *tab* needs the Home tap (Back there just moves to the previous reel).
// A full-screen Reel (opened from the feed, a DM or a share) has no visible tab bar, and Back
// closes it. Tapping Home while it's already selected would refresh the feed, so that's Back too.
fun safeTabActionFor(homeTabFound: Boolean, homeTabSelected: Boolean): SafeTabAction =
    if (homeTabFound && !homeTabSelected) SafeTabAction.TAP_HOME_TAB else SafeTabAction.GO_BACK
