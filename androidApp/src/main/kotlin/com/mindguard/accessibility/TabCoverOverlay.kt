package com.mindguard.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast

// Draws a patch in the nav bar's colour over another app's Reels/Shorts tab so it looks gone
// and can't be tapped. Uses TYPE_ACCESSIBILITY_OVERLAY, so no extra permission is needed.
class TabCoverOverlay(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
    private var cover: View? = null
    private var shown: Pair<HideableTab, NodeBounds>? = null

    fun showAt(tab: HideableTab, bounds: NodeBounds) {
        if (shown == tab to bounds) return
        val params = layoutParams(bounds)
        val view = cover
        if (view == null) {
            val created = createCover(tab)
            windowManager.addView(created, params)
            cover = created
        } else {
            view.setBackgroundColor(coverColor(tab))
            view.setOnClickListener { toast(tab) }
            windowManager.updateViewLayout(view, params)
        }
        shown = tab to bounds
    }

    fun hide() {
        val view = cover ?: return
        try {
            windowManager.removeView(view)
        } catch (e: IllegalArgumentException) {
            android.util.Log.w("Present", "Tab cover was already removed")
        }
        cover = null
        shown = null
    }

    private fun createCover(tab: HideableTab): View =
        FrameLayout(service).apply {
            setBackgroundColor(coverColor(tab))
            isClickable = true
            setOnClickListener { toast(tab) }
        }

    private fun layoutParams(bounds: NodeBounds) = WindowManager.LayoutParams(
        bounds.right - bounds.left,
        bounds.bottom - bounds.top,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.OPAQUE
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = bounds.left
        y = bounds.top
    }

    private fun isNightMode(): Boolean =
        (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private fun coverColor(tab: HideableTab): Int = tab.barColor(night = isNightMode())

    private fun toast(tab: HideableTab) {
        val what = when (tab) {
            HideableTab.INSTAGRAM_REELS -> "Reels are"
            HideableTab.YOUTUBE_SHORTS  -> "Shorts are"
        }
        Toast.makeText(service, "$what hidden by Present 🌅", Toast.LENGTH_SHORT).show()
    }
}
