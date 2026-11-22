package com.mindguard.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mindguard.shared.models.ScreenSnapshot

// Only a screen change names the screen. Content-change and scroll events carry the class of
// whichever view changed (e.g. a "ShortsShelf" on YouTube's home feed), which must not be
// mistaken for being inside the Shorts or Reels player.
fun windowClassFor(eventType: Int, className: CharSequence?): String? =
    className?.toString()
        ?.takeIf { eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && it.isNotBlank() }

data class NodeSignals(val viewId: String?, val text: String?, val description: String?, val visibleToUser: Boolean)

data class CollectedSignals(val resourceIds: List<String>, val texts: List<String>, val descriptions: List<String>)

// Only views the user can see count. Instagram keeps the Reels player loaded but hidden behind
// the Home screen; counting its views made Present "escape" Reels while the user was on Home.
fun <N> collectVisibleSignals(
    root: N,
    signalsOf: (N) -> NodeSignals,
    childrenOf: (N) -> List<N>,
    release: (N) -> Unit,
    maxDepth: Int
): CollectedSignals {
    val ids = LinkedHashSet<String>()
    val texts = LinkedHashSet<String>()
    val descriptions = LinkedHashSet<String>()
    fun visit(node: N, depth: Int) {
        if (depth > maxDepth) return
        val signals = signalsOf(node)
        if (!signals.visibleToUser) return
        signals.viewId?.takeIf { it.isNotBlank() }?.let { ids += it }
        signals.text?.takeIf { it.isNotBlank() }?.let { texts += it }
        signals.description?.takeIf { it.isNotBlank() }?.let { descriptions += it }
        childrenOf(node).forEach { child ->
            visit(child, depth + 1)
            release(child)
        }
    }
    visit(root, 0)
    return CollectedSignals(ids.toList(), texts.toList(), descriptions.toList())
}

const val EVENT_INSPECTION_THROTTLE_MS = 300L

// A story's progress bar fires content-change events continuously; inspecting the screen for
// each one (90-200 ms) starved the service and delayed the tab cover. Screen changes are always
// inspected; bursts of content/scroll events at most once per throttle window (the watchdog
// covers the gaps).
fun shouldInspectEvent(eventType: Int, msSinceLastInspection: Long): Boolean =
    eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || msSinceLastInspection >= EVENT_INSPECTION_THROTTLE_MS

class AccessibilityEventConverter {

    fun toScreenSnapshot(event: AccessibilityEvent): ScreenSnapshot? {
        val packageName = event.packageName?.toString() ?: return null

        val text = mutableListOf<String>()
        val descriptions = mutableListOf<String>()
        val resourceIds = mutableListOf<String>()

        // Text directly in the event
        event.text?.forEach { if (it?.isNotBlank() == true) text.add(it.toString()) }
        if (event.contentDescription?.isNotBlank() == true) {
            descriptions.add(event.contentDescription.toString())
        }

        // Traverse the source node and its hierarchy (depth 15)
        val source = event.source
        if (source != null) {
            traverseNode(source, text, descriptions, resourceIds)
            source.recycle()
        }

        return ScreenSnapshot(
            packageName = packageName,
            screenText = text.distinct(),
            contentDescriptions = descriptions.distinct(),
            resourceIds = resourceIds.distinct(),
            timestampMillis = System.currentTimeMillis(),
            windowClassName = windowClassFor(event.eventType, event.className)
        )
    }

    /** Build a snapshot from a root AccessibilityNodeInfo (for watchdog scans). */
    fun toScreenSnapshotFromRoot(root: AccessibilityNodeInfo, packageName: String): ScreenSnapshot? {
        val text = mutableListOf<String>()
        val descriptions = mutableListOf<String>()
        val resourceIds = mutableListOf<String>()
        traverseNode(root, text, descriptions, resourceIds)
        if (resourceIds.isEmpty() && text.isEmpty()) return null
        return ScreenSnapshot(
            packageName = packageName,
            screenText = text.distinct(),
            contentDescriptions = descriptions.distinct(),
            resourceIds = resourceIds.distinct(),
            timestampMillis = System.currentTimeMillis()
        )
    }

    /** Merge a root-scan's findings into an existing snapshot. */
    fun augmentWithRoot(snapshot: ScreenSnapshot, root: AccessibilityNodeInfo): ScreenSnapshot {
        val text = snapshot.screenText.toMutableList()
        val descriptions = snapshot.contentDescriptions.toMutableList()
        val resourceIds = snapshot.resourceIds.toMutableList()
        traverseNode(root, text, descriptions, resourceIds)
        return snapshot.copy(
            screenText = text.distinct(),
            contentDescriptions = descriptions.distinct(),
            resourceIds = resourceIds.distinct()
        )
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo,
        text: MutableList<String>,
        descriptions: MutableList<String>,
        resourceIds: MutableList<String>
    ) {
        val signals = collectVisibleSignals(
            root = node,
            signalsOf = { NodeSignals(it.viewIdResourceName, it.text?.toString(), it.contentDescription?.toString(), it.isVisibleToUser) },
            childrenOf = { n -> (0 until n.childCount).mapNotNull { n.getChild(it) } },
            release = { it.recycle() },
            maxDepth = MAX_DEPTH
        )
        signals.resourceIds.filterNot { it in resourceIds }.forEach { resourceIds += it }
        signals.texts.filterNot { it in text }.forEach { text += it }
        signals.descriptions.filterNot { it in descriptions }.forEach { descriptions += it }
    }

    companion object {
        // Deep enough to reach reel containers in Instagram/YouTube view hierarchies
        private const val MAX_DEPTH = 15
    }
}
