package com.mindguard.accessibility

import com.mindguard.shared.policy.AllowedSurface
import com.mindguard.shared.policy.BlockedSurface
import com.mindguard.shared.policy.Platform
import com.mindguard.shared.policy.PolicyRule
import com.mindguard.shared.policy.PresentPolicy
import com.mindguard.shared.policy.Schedule
import com.mindguard.shared.policy.ScheduleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavTabHidingTest {

    private val screen = ScreenSize(width = 1080, height = 2400)

    private fun policy(
        platform: Platform = Platform.INSTAGRAM,
        surfaces: List<BlockedSurface> = listOf(BlockedSurface.REELS_TAB),
        schedule: Schedule = Schedule(ScheduleMode.ALWAYS)
    ) = PresentPolicy(
        policyVersion = "1",
        summary = "test",
        rules = listOf(PolicyRule(platform, surfaces, listOf(AllowedSurface.DIRECT_MESSAGES), schedule))
    )

    private fun state(
        tab: HideableTab = HideableTab.INSTAGRAM_REELS,
        policy: PresentPolicy? = policy(),
        protectionEnabled: Boolean = true,
        inFocusHours: Boolean = false,
        paused: Boolean = false,
        enabledPackages: Set<String> = setOf("com.instagram.android", "com.google.android.youtube"),
        minuteOfDay: Int = 22 * 60
    ) = TabHidingState(tab, policy, protectionEnabled, inFocusHours, paused, enabledPackages, minuteOfDay)

    private fun node(
        viewId: String? = "com.instagram.android:id/clips_tab",
        label: String? = "Reels",
        top: Int = 2250,
        bottom: Int = 2380,
        left: Int = 432,
        right: Int = 648,
        visible: Boolean = true
    ) = TabNode(viewId, label, NodeBounds(left, top, right, bottom), visible)

    @Test
    fun `Instagram and YouTube each have a tab Present can hide`() {
        assertEquals(HideableTab.INSTAGRAM_REELS, HideableTab.forPackage("com.instagram.android"))
        assertEquals(HideableTab.YOUTUBE_SHORTS, HideableTab.forPackage("com.google.android.youtube"))
        assertNull(HideableTab.forPackage("com.snapchat.android"))
    }

    @Test
    fun `hides the Reels tab while the active policy blocks Reels`() {
        assertTrue(shouldHideTab(state()))
    }

    @Test
    fun `shows the tab again outside the policy's time window`() {
        val nightOnly = policy(schedule = Schedule(ScheduleMode.TIME_WINDOW, startTime = "21:00", endTime = "06:00"))
        assertTrue(shouldHideTab(state(policy = nightOnly, minuteOfDay = 23 * 60)))
        assertFalse(shouldHideTab(state(policy = nightOnly, minuteOfDay = 14 * 60)))
    }

    @Test
    fun `keeps the tab when the policy does not block that surface`() {
        val shortsOnly = policy(platform = Platform.YOUTUBE, surfaces = listOf(BlockedSurface.SHORTS_PLAYER))
        assertFalse(shouldHideTab(state(policy = shortsOnly)))
        assertTrue(shouldHideTab(state(tab = HideableTab.YOUTUBE_SHORTS, policy = shortsOnly)))
    }

    @Test
    fun `never hides while protection is off or paused`() {
        assertFalse(shouldHideTab(state(protectionEnabled = false)))
        assertFalse(shouldHideTab(state(paused = true)))
    }

    @Test
    fun `without a policy it follows the app toggles and protection switch`() {
        assertTrue(shouldHideTab(state(policy = null)))
        assertFalse(shouldHideTab(state(policy = null, enabledPackages = setOf("com.google.android.youtube"))))
        assertFalse(shouldHideTab(state(policy = null, protectionEnabled = false)))
        assertTrue(shouldHideTab(state(policy = null, protectionEnabled = false, inFocusHours = true)))
    }

    @Test
    fun `recognises the Instagram Reels tab by its view id or label in the bottom bar`() {
        assertTrue(isNavTab(HideableTab.INSTAGRAM_REELS, node(), screen))
        assertTrue(isNavTab(HideableTab.INSTAGRAM_REELS, node(viewId = null, label = "Reels"), screen))
        assertTrue(isNavTab(HideableTab.INSTAGRAM_REELS, node(viewId = "com.instagram.android:id/clips_tab", label = null), screen))
    }

    @Test
    fun `recognises the YouTube Shorts tab by its label in the bottom bar`() {
        val shorts = node(viewId = "com.google.android.youtube:id/pivot_bar_item", label = "Shorts")
        assertTrue(isNavTab(HideableTab.YOUTUBE_SHORTS, shorts, screen))
        assertFalse(isNavTab(HideableTab.YOUTUBE_SHORTS, shorts.copy(label = "Subscriptions"), screen))
    }

    @Test
    fun `ignores Reels labels outside the bottom navigation bar`() {
        val profileReelsTab = node(viewId = null, label = "Reels", top = 900, bottom = 1000)
        assertFalse(isNavTab(HideableTab.INSTAGRAM_REELS, profileReelsTab, screen))
    }

    @Test
    fun `ignores invisible or empty nodes`() {
        assertFalse(isNavTab(HideableTab.INSTAGRAM_REELS, node(visible = false), screen))
        assertFalse(isNavTab(HideableTab.INSTAGRAM_REELS, node(left = 500, right = 500), screen))
    }

    @Test
    fun `label matching ignores case and surrounding words like a tab count`() {
        assertTrue(isNavTab(HideableTab.INSTAGRAM_REELS, node(viewId = null, label = "reels, tab 3 of 5"), screen))
        assertFalse(isNavTab(HideableTab.INSTAGRAM_REELS, node(viewId = null, label = "Reel drafts"), screen))
    }

    @Test
    fun `Instagram's cover uses the exact colour of its tab bar so the button looks absent`() {
        assertEquals(0xFF0C1014.toInt(), HideableTab.INSTAGRAM_REELS.barColor(night = true))
        assertEquals(0xFFFFFFFF.toInt(), HideableTab.INSTAGRAM_REELS.barColor(night = false))
        assertEquals(0xFF0F0F0F.toInt(), HideableTab.YOUTUBE_SHORTS.barColor(night = true))
    }

    @Test
    fun `Instagram's tabs are found by id only, never by the slow whole-screen label search`() {
        assertFalse(HideableTab.INSTAGRAM_REELS.usesLabelSearch)
        assertTrue(HideableTab.YOUTUBE_SHORTS.usesLabelSearch)
    }

    @Test
    fun `leaving the Home tab removes the cover at once, a briefly missing tab on Home waits`() {
        assertEquals(CoverUpdate.HIDE_NOW, coverUpdateFor(homeTabSelected = false, tabFound = false, misses = 0))
        assertEquals(CoverUpdate.HIDE_NOW, coverUpdateFor(homeTabSelected = false, tabFound = true, misses = 0))
        assertEquals(CoverUpdate.SHOW, coverUpdateFor(homeTabSelected = true, tabFound = true, misses = 3))
        assertEquals(CoverUpdate.KEEP, coverUpdateFor(homeTabSelected = true, tabFound = false, misses = 0))
        assertEquals(CoverUpdate.HIDE_NOW, coverUpdateFor(homeTabSelected = true, tabFound = false, misses = 1))
    }

    @Test
    fun `the cover matches the tab and stays on screen`() {
        assertEquals(NodeBounds(432, 2250, 648, 2380), coverBounds(NodeBounds(432, 2250, 648, 2380), screen))
        assertEquals(NodeBounds(0, 2250, 1080, 2400), coverBounds(NodeBounds(-20, 2250, 1200, 2450), screen))
    }

    @Test
    fun `no cover for a degenerate rectangle`() {
        assertNull(coverBounds(NodeBounds(500, 2250, 510, 2380), screen))
        assertNull(coverBounds(NodeBounds(432, 2400, 648, 2500), screen))
    }
}
