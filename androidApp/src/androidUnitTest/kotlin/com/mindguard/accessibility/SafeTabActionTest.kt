package com.mindguard.accessibility

import kotlin.test.Test
import kotlin.test.assertEquals

class SafeTabActionTest {

    @Test
    fun `from the Reels tab, tap Home`() {
        assertEquals(SafeTabAction.TAP_HOME_TAB, safeTabActionFor(homeTabFound = true, homeTabSelected = false))
    }

    @Test
    fun `when Home is already selected, go back instead, because tapping Home again refreshes the feed`() {
        assertEquals(SafeTabAction.GO_BACK, safeTabActionFor(homeTabFound = true, homeTabSelected = true))
    }

    @Test
    fun `in a full-screen Reel with no visible tab bar, go back to close the viewer instead of leaving Instagram`() {
        assertEquals(SafeTabAction.GO_BACK, safeTabActionFor(homeTabFound = false, homeTabSelected = false))
    }
}
