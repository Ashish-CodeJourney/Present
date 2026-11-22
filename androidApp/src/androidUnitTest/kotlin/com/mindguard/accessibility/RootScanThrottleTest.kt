package com.mindguard.accessibility

import android.view.accessibility.AccessibilityEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RootScanThrottleTest {

    @Test
    fun `a screen change is always scanned so a Reel is caught the moment it opens`() {
        assertTrue(shouldScanRootOnEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, msSinceLastRootScan = 0))
    }

    @Test
    fun `a burst of content changes, like a story's progress bar, does not rescan the whole screen every time`() {
        assertFalse(shouldScanRootOnEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, msSinceLastRootScan = 40))
        assertFalse(shouldScanRootOnEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, msSinceLastRootScan = 299))
    }

    @Test
    fun `content changes are scanned again once the throttle window has passed`() {
        assertTrue(shouldScanRootOnEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, msSinceLastRootScan = 300))
    }
}
