package com.mindguard.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mindguard.shared.models.BlockAction
import com.mindguard.shared.models.DetectionResult
import com.mindguard.shared.models.ScreenSnapshot
import com.mindguard.shared.policy.PolicyEvaluator
import com.mindguard.shared.policy.PresentPolicy
import com.mindguard.shared.usecases.isPausedAt
import com.mindguard.shared.rules.InstagramReelRule
import com.mindguard.shared.rules.RuleEngine
import com.mindguard.shared.rules.SnapchatSpotlightRule
import com.mindguard.shared.rules.TikTokRule
import com.mindguard.shared.rules.YouTubeShortsRule
import com.mindguard.shared.usecases.BlockCooldown
import com.mindguard.shared.usecases.DetectBlockedContentUseCase
import com.mindguard.storage.PolicyStore
import com.mindguard.storage.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class MindGuardAccessibilityService : AccessibilityService(), KoinComponent {

    private val settingsDataStore: SettingsDataStore by inject()
    private val policyStore: PolicyStore by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var converter: AccessibilityEventConverter
    private lateinit var cooldown: BlockCooldown
    private lateinit var executor: BlockActionExecutor
    private lateinit var tabCover: TabCoverOverlay
    private var lastCoverRefreshMs = 0L
    private var coverMisses = 0
    private var lastRootScanMs = 0L

    private val detector = DetectBlockedContentUseCase(
        RuleEngine(listOf(InstagramReelRule(), YouTubeShortsRule(), TikTokRule(), SnapchatSpotlightRule()))
    )
    private val policyEvaluator = PolicyEvaluator()

    // Policy and its derived package set are swapped together so the hot path never sees a mismatch.
    private class ActivePolicy(val policy: PresentPolicy, val packages: Set<String>)

    // All cached synchronously — no coroutine hop on the hot path
    @Volatile private var enabledPackages: Set<String> = SettingsDataStore.run {
        setOf(INSTAGRAM_PKG, YOUTUBE_PKG, TIKTOK_PKG, TIKTOK_PKG2, TIKTOK_PKG3, SNAPCHAT_PKG)
    }
    @Volatile private var protectionEnabled    = true
    @Volatile private var focusScheduleEnabled = false
    @Volatile private var focusStartHour       = 9
    @Volatile private var focusEndHour         = 17
    @Volatile private var pauseUntilMs         = 0L
    @Volatile private var activePolicy: ActivePolicy? = null

    // Watchdog: re-checks the current foreground window every WATCHDOG_INTERVAL_MS.
    // Suspended for POST_BLOCK_PAUSE_MS after each block to let navigation animations settle.
    private var watchdogJob: Job? = null
    private var watchdogPackage: String? = null
    private var lastBlockTimeMs: Long = 0L

    companion object {
        const val WATCHDOG_INTERVAL_MS = 300L
        const val POST_BLOCK_PAUSE_MS  = 3_000L
        const val COVER_REFRESH_MS     = 300L
    }

    override fun onCreate() {
        super.onCreate()
        converter = AccessibilityEventConverter()
        executor  = BlockActionExecutor(this)
        tabCover  = TabCoverOverlay(this)
        // 500ms cooldown: tight enough to re-block quickly if user navigates back,
        // long enough to prevent back-press feedback loops.
        cooldown  = BlockCooldown(cooldownMs = 500L)

        serviceScope.launch { settingsDataStore.resetDailyCountsIfNeeded() }

        serviceScope.launch { settingsDataStore.protectionEnabledFlow.collect { protectionEnabled = it } }
        serviceScope.launch { settingsDataStore.enabledPackagesFlow.collect { enabledPackages = it } }
        serviceScope.launch { settingsDataStore.pauseUntilFlow.collect { pauseUntilMs = it } }
        serviceScope.launch {
            policyStore.activePolicyFlow.collect { policy ->
                activePolicy = policy?.let { ActivePolicy(it, PolicyEvaluator.packagesFor(it)) }
            }
        }
        serviceScope.launch {
            combine(
                settingsDataStore.focusScheduleEnabledFlow,
                settingsDataStore.focusStartHourFlow,
                settingsDataStore.focusEndHourFlow
            ) { e, s, n -> Triple(e, s, n) }.collect { (e, s, n) ->
                focusScheduleEnabled = e; focusStartHour = s; focusEndHour = n
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        // No debounce — every event is evaluated. The cooldown (500ms) prevents rapid
        // back-press loops. Processing every event is essential because the event that
        // contains reel resource IDs is often the 2nd or 3rd in a burst.
        val pkg = event.packageName?.toString() ?: return
        manageWatchdog(pkg)
        processEventInternal(event)
        refreshTabCover(force = false)
    }

    private fun processEventInternal(event: AccessibilityEvent) {
        try {
            val pkg = event.packageName?.toString() ?: return
            if (!isMonitored(pkg)) return

            if (android.util.Log.isLoggable("MindGuard", android.util.Log.DEBUG)) {
                android.util.Log.d("MindGuard", "Event pkg=$pkg class=${event.className} type=${event.eventType}")
            }

            val snapshot = buildSnapshot(event) ?: return
            val result = detector.execute(snapshot)
            if (!result.shouldBlock) return

            maybeBlock(result, pkg)
        } catch (e: Exception) {
            android.util.Log.e("MindGuard", "processEvent error: ${e.message}")
        }
    }

    private fun maybeBlock(result: DetectionResult, packageName: String) {
        val now = System.currentTimeMillis()
        if (isPausedAt(pauseUntilMs, now)) return
        if (!isProtected(result, packageName)) return

        // Suppress watchdog re-triggers for POST_BLOCK_PAUSE_MS after each block.
        // This prevents the watchdog from firing again before the navigation animation
        // (e.g. swipe-to-home-tab) has finished, which caused the Instagram refresh loop.
        if (now - lastBlockTimeMs < POST_BLOCK_PAUSE_MS) return

        if (cooldown.canBlock(now)) {
            lastBlockTimeMs = now
            cooldown.recordBlock(now)
            executeBlockAction(result.action)
            serviceScope.launch(Dispatchers.IO) {
                settingsDataStore.recordAttempt()
                settingsDataStore.recordBlock(packageName)
            }
        }
    }

    // Watchdog: scan the full active window every 800ms while a blocked app is foreground.
    // This catches the case where the app stops emitting events during reel playback.
    private fun manageWatchdog(pkg: String) {
        if (!isMonitored(pkg)) {
            if (watchdogPackage != null) stopWatchdog()
            return
        }
        if (pkg == watchdogPackage) return  // already watching
        stopWatchdog()
        watchdogPackage = pkg
        watchdogJob = serviceScope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                scanActiveWindowFor(pkg)
                refreshTabCover(force = true)
            }
        }
    }

    private fun stopWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
        watchdogPackage = null
    }

    private fun scanActiveWindowFor(pkg: String) {
        try {
            val root = rootInActiveWindow ?: return
            val snapshot = converter.toScreenSnapshotFromRoot(root, pkg) ?: run {
                root.recycle(); return
            }
            root.recycle()
            if (!isMonitored(snapshot.packageName)) return
            val result = detector.execute(snapshot)
            if (result.shouldBlock) maybeBlock(result, snapshot.packageName)
        } catch (e: Exception) {
            android.util.Log.e("MindGuard", "watchdog error: ${e.message}")
        }
    }

    // Covers the Reels/Shorts tab in the foreground app while the rules block that surface.
    private fun refreshTabCover(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastCoverRefreshMs < COVER_REFRESH_MS) return
        lastCoverRefreshMs = now
        try {
            val root = rootInActiveWindow
            val tab = root?.packageName?.toString()?.let { HideableTab.forPackage(it) }
            if (root == null || tab == null || !shouldHideTab(tabHidingState(tab, now))) {
                root?.recycle()
                coverMisses = 0
                tabCover.hide()
                return
            }
            val screen = screenSize()
            val (homeSelected, bounds) = try {
                val home = isHomeTabSelected(root, tab, screen)
                home to (if (home) findNavTabCover(root, tab, screen) else null)
            } finally { root.recycle() }
            when (coverUpdateFor(homeSelected, bounds != null, coverMisses)) {
                CoverUpdate.SHOW -> {
                    coverMisses = 0
                    bounds?.let { tabCover.showAt(tab, it) }
                }
                CoverUpdate.KEEP -> coverMisses++
                CoverUpdate.HIDE_NOW -> {
                    coverMisses = 0
                    tabCover.hide()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MindGuard", "tab cover error: ${e.message}")
            tabCover.hide()
        }
    }

    private fun tabHidingState(tab: HideableTab, now: Long) = TabHidingState(
        tab = tab,
        policy = activePolicy?.policy,
        protectionEnabled = protectionEnabled,
        inFocusHours = isCurrentlyInFocusHours(),
        paused = isPausedAt(pauseUntilMs, now),
        enabledPackages = enabledPackages,
        minuteOfDay = currentMinuteOfDay()
    )

    private fun findNavTabCover(root: AccessibilityNodeInfo, tab: HideableTab, screen: ScreenSize): NodeBounds? {
        val candidates = if (tab.usesLabelSearch) root.findAccessibilityNodeInfosByText(tab.label)
            else tab.viewIds.flatMap { root.findAccessibilityNodeInfosByViewId(it) }
        return try {
            candidates.asSequence()
                .map { it.toTabNode() }
                .firstOrNull { isNavTab(tab, it, screen) }
                ?.let { coverBounds(it.bounds, screen) }
        } finally {
            candidates.forEach { it.recycle() }
        }
    }

    private fun isHomeTabSelected(root: AccessibilityNodeInfo, tab: HideableTab, screen: ScreenSize): Boolean {
        val candidates = if (tab.usesLabelSearch) root.findAccessibilityNodeInfosByText("Home")
            else tab.homeViewIds.flatMap { root.findAccessibilityNodeInfosByViewId(it) }
        return try {
            candidates.any { node ->
                val facts = node.toTabNode()
                node.isSelected && facts.visibleToUser && facts.bounds.top >= screen.height * 0.8
            }
        } finally {
            candidates.forEach { it.recycle() }
        }
    }

    private fun AccessibilityNodeInfo.toTabNode(): TabNode {
        val rect = Rect().also { getBoundsInScreen(it) }
        return TabNode(
            viewId = viewIdResourceName,
            label = (contentDescription ?: text)?.toString(),
            bounds = NodeBounds(rect.left, rect.top, rect.right, rect.bottom),
            visibleToUser = isVisibleToUser
        )
    }

    private fun screenSize(): ScreenSize {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            return ScreenSize(bounds.width(), bounds.height())
        }
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return ScreenSize(metrics.widthPixels, metrics.heightPixels)
    }

    private fun buildSnapshot(event: AccessibilityEvent): ScreenSnapshot? {
        val basic = converter.toScreenSnapshot(event) ?: return null
        val now = System.currentTimeMillis()
        if (!shouldScanRootOnEvent(event.eventType, now - lastRootScanMs)) return basic
        lastRootScanMs = now
        // Augment with a full window scan to find resource IDs deep in the hierarchy
        val root = rootInActiveWindow ?: return basic
        return try {
            converter.augmentWithRoot(basic, root)
        } finally {
            root.recycle()
        }
    }

    // With an active Present policy the policy decides which apps are watched and when
    // (the protection toggle stays a master off switch); otherwise the legacy toggles apply.
    private fun isMonitored(pkg: String): Boolean {
        val policy = activePolicy ?: return pkg in enabledPackages
        return pkg in policy.packages
    }

    private fun isProtected(result: DetectionResult, packageName: String): Boolean {
        val policy = activePolicy?.policy ?: return protectionEnabled || isCurrentlyInFocusHours()
        if (!protectionEnabled) return false
        val surface = result.surface ?: return false
        return policyEvaluator.shouldEnforce(policy, packageName, surface, currentMinuteOfDay())
    }

    private fun currentMinuteOfDay(): Int {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }

    private fun isCurrentlyInFocusHours(): Boolean {
        if (!focusScheduleEnabled) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (focusStartHour <= focusEndHour) {
            hour in focusStartHour..focusEndHour
        } else {
            // overnight range e.g. 22:00 → 06:00
            hour >= focusStartHour || hour <= focusEndHour
        }
    }

    private fun executeBlockAction(action: BlockAction) {
        try { executor.execute(action) } catch (e: Exception) {
            android.util.Log.e("MindGuard", "executeBlockAction error: ${e.message}")
        }
    }

    override fun onInterrupt() = Unit
    override fun onDestroy() {
        tabCover.hide()
        serviceScope.cancel()
        super.onDestroy()
    }
}
