package com.mindguard.accessibility

import android.view.accessibility.AccessibilityEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowClassTest {

    @Test
    fun `a screen change reports the screen's class`() {
        assertEquals(
            "com.instagram.android.clips.fragment.ClipsViewerFragment",
            windowClassFor(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "com.instagram.android.clips.fragment.ClipsViewerFragment")
        )
    }

    @Test
    fun `content changes and scrolls report a view's class, not the screen, so they are ignored`() {
        assertNull(windowClassFor(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, "com.instagram.clips.ClipsShelfView"))
        assertNull(windowClassFor(AccessibilityEvent.TYPE_VIEW_SCROLLED, "com.google.android.apps.youtube.ShortsShelfRecyclerView"))
    }

    @Test
    fun `a blank class is no class`() {
        assertNull(windowClassFor(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "  "))
        assertNull(windowClassFor(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, null))
    }
}
