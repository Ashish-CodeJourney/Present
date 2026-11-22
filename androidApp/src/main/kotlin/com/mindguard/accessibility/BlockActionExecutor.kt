package com.mindguard.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import com.mindguard.shared.models.BlockAction

class BlockActionExecutor(private val service: AccessibilityService) {

    fun execute(action: BlockAction): Boolean {
        return when (action) {
            BlockAction.GO_BACK -> goBack()
            BlockAction.GO_HOME_AND_REOPEN_APP -> goHomeAndReopenApp()
            BlockAction.CLICK_SAFE_TAB -> clickSafeTab()
            BlockAction.NONE -> false
        }
    }

    private fun goBack(): Boolean {
        return try {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            false
        }
    }

    private fun goHomeAndReopenApp(): Boolean {
        return try {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        } catch (e: Exception) {
            false
        }
    }

    // Leaves the Reels player without looping or refreshing the feed:
    // from the Reels tab, tap Home; if Home is already selected (a Reel opened over the feed),
    // press Back, because tapping Home again refreshes the feed. Back inside the Reels *tab*
    // only moves to the previous reel, so it is never used there.
    private fun clickSafeTab(): Boolean {
        val root = try { service.rootInActiveWindow } catch (e: Exception) { null }
            ?: return goBack()
        val homeTab = try { findHomeTab(root) } finally { root.recycle() }
        return try {
            when (safeTabActionFor(homeTab != null, homeTab?.isSelected == true)) {
                SafeTabAction.TAP_HOME_TAB -> homeTab?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true || goBack()
                SafeTabAction.GO_BACK -> goBack()
            }
        } finally {
            homeTab?.recycle()
        }
    }

    // Known resource IDs first, then the version-agnostic "Home" label.
    private fun findHomeTab(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (id in InstagramHomeTabStrategy.ORDERED_IDS) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            val tab = nodes.firstOrNull { it.isVisibleToUser }
            nodes.filter { it !== tab }.forEach { it.recycle() }
            if (tab != null) return tab
        }
        val homeNodes = root.findAccessibilityNodeInfosByText("Home")
        val homeTab = homeNodes.firstOrNull { it.isClickable && it.isVisibleToUser }
        homeNodes.filter { it !== homeTab }.forEach { it.recycle() }
        return homeTab
    }
}
