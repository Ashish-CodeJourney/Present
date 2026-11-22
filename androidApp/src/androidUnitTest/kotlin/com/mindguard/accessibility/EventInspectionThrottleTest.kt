package com.mindguard.accessibility

import android.view.accessibility.AccessibilityEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventInspectionThrottleTest {

    @Test
    fun `a screen change is always inspected so a Reel is caught the moment it opens`() {
        assertTrue(shouldInspectEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, msSinceLastInspection = 0))
    }

    @Test
    fun `a burst of content changes, like a story's progress bar, is not inspected on every event`() {
        assertFalse(shouldInspectEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, msSinceLastInspection = 40))
        assertFalse(shouldInspectEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, msSinceLastInspection = 299))
    }

    @Test
    fun `content changes are inspected again once the throttle window has passed`() {
        assertTrue(shouldInspectEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, msSinceLastInspection = 300))
    }
}
