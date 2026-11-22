package com.mindguard.accessibility

import com.mindguard.shared.policy.BlockedSurface
import com.mindguard.shared.policy.PolicyEvaluator
import com.mindguard.shared.policy.PresentPolicy

private const val NAV_BAR_TOP_FRACTION = 0.8
private const val MIN_COVER_PX = 24

// Tab-id lookups are cheap, so the cover can follow the screen several times a second.
const val COVER_REFRESH_MS = 80L

enum class HideableTab(
    val packageName: String,
    val surface: BlockedSurface,
    val viewIds: List<String>,
    val label: String,
    val barViewIds: List<String>,
    private val dayBar: Int,
    private val nightBar: Int
) {
    // Bar colours measured from the apps' bottom navigation (Instagram dark = #0C1014).
    INSTAGRAM_REELS(
        packageName = "com.instagram.android",
        surface = BlockedSurface.REELS_TAB,
        viewIds = listOf("com.instagram.android:id/clips_tab"),
        label = "Reels",
        // Any of the bar's other tabs proves the bar is on screen (Home, DMs, Search, Profile).
        barViewIds = listOf(
            "com.instagram.android:id/feed_tab",
            "com.instagram.android:id/direct_tab",
            "com.instagram.android:id/search_tab",
            "com.instagram.android:id/profile_tab"
        ),
        dayBar = 0xFFFFFFFF.toInt(),
        nightBar = 0xFF0C1014.toInt()
    ),
    YOUTUBE_SHORTS(
        packageName = "com.google.android.youtube",
        surface = BlockedSurface.SHORTS_PLAYER,
        viewIds = emptyList(),
        label = "Shorts",
        barViewIds = emptyList(),
        dayBar = 0xFFFFFFFF.toInt(),
        nightBar = 0xFF0F0F0F.toInt()
    );

    fun barColor(night: Boolean): Int = if (night) nightBar else dayBar

    // The label search walks the whole tree (it took 1.7 s on an Instagram story); only apps
    // without stable view ids use it.
    val usesLabelSearch: Boolean get() = viewIds.isEmpty()

    companion object {
        fun forPackage(packageName: String): HideableTab? = entries.firstOrNull { it.packageName == packageName }
    }
}

data class ScreenSize(val width: Int, val height: Int)

data class NodeBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

data class TabNode(val viewId: String?, val label: String?, val bounds: NodeBounds, val visibleToUser: Boolean)

data class TabHidingState(
    val tab: HideableTab,
    val policy: PresentPolicy?,
    val protectionEnabled: Boolean,
    val inFocusHours: Boolean,
    val paused: Boolean,
    val enabledPackages: Set<String>,
    val minuteOfDay: Int
)

private val policyEvaluator = PolicyEvaluator()

fun shouldHideTab(state: TabHidingState): Boolean {
    if (state.paused) return false
    val policy = state.policy
        ?: return (state.protectionEnabled || state.inFocusHours) && state.tab.packageName in state.enabledPackages
    return state.protectionEnabled &&
        policyEvaluator.shouldEnforce(policy, state.tab.packageName, state.tab.surface, state.minuteOfDay)
}

private fun labelMatches(tab: HideableTab, label: String?): Boolean =
    label != null && Regex("\\b${tab.label}\\b", RegexOption.IGNORE_CASE).containsMatchIn(label)

fun isNavTab(tab: HideableTab, node: TabNode, screen: ScreenSize): Boolean {
    if (!node.visibleToUser) return false
    val b = node.bounds
    if (b.right <= b.left || b.bottom <= b.top) return false
    if (b.top < screen.height * NAV_BAR_TOP_FRACTION) return false
    return node.viewId in tab.viewIds || labelMatches(tab, node.label)
}

fun coverBounds(tab: NodeBounds, screen: ScreenSize): NodeBounds? {
    val clamped = NodeBounds(
        left = tab.left.coerceIn(0, screen.width),
        top = tab.top.coerceIn(0, screen.height),
        right = tab.right.coerceIn(0, screen.width),
        bottom = tab.bottom.coerceIn(0, screen.height)
    )
    val wideEnough = clamped.right - clamped.left >= MIN_COVER_PX
    val tallEnough = clamped.bottom - clamped.top >= MIN_COVER_PX
    return if (wideEnough && tallEnough) clamped else null
}

enum class CoverUpdate { SHOW, KEEP, HIDE_NOW }

private const val MISSES_BEFORE_HIDE = 2

// The tab stays hidden on every screen that shows the bottom bar (Home, DMs, Search, Profile).
// Where the bar disappears (a story, a DM thread, a full-screen Reel) the cover leaves at once.
// While the bar redraws, Instagram briefly drops the tab, so one miss keeps the cover.
fun coverUpdateFor(tabBarVisible: Boolean, tabFound: Boolean, misses: Int): CoverUpdate = when {
    !tabBarVisible                    -> CoverUpdate.HIDE_NOW
    tabFound                          -> CoverUpdate.SHOW
    misses + 1 >= MISSES_BEFORE_HIDE  -> CoverUpdate.HIDE_NOW
    else                              -> CoverUpdate.KEEP
}
