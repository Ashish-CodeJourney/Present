package com.mindguard.accessibility

import kotlin.test.Test
import kotlin.test.assertEquals

class VisibleSignalsTest {

    private data class FakeNode(
        val id: String?,
        val visible: Boolean = true,
        val text: String? = null,
        val description: String? = null,
        val children: List<FakeNode> = emptyList()
    )

    private fun collect(root: FakeNode, maxDepth: Int = 15) = collectVisibleSignals(
        root = root,
        signalsOf = { NodeSignals(it.id, it.text, it.description, it.visible) },
        childrenOf = { it.children },
        release = {},
        maxDepth = maxDepth
    )

    @Test
    fun `collects ids, text and descriptions from visible views`() {
        val tree = FakeNode("root", children = listOf(
            FakeNode("feed", text = "Home", description = "Home feed"),
            FakeNode("tab_bar", children = listOf(FakeNode("clips_tab", description = "Reels")))
        ))
        val signals = collect(tree)
        assertEquals(listOf("root", "feed", "tab_bar", "clips_tab"), signals.resourceIds)
        assertEquals(listOf("Home"), signals.texts)
        assertEquals(listOf("Home feed", "Reels"), signals.descriptions)
    }

    @Test
    fun `ignores the Reels player Instagram keeps loaded but hidden behind the Home screen`() {
        val tree = FakeNode("root", children = listOf(
            FakeNode("feed"),
            FakeNode("clips_viewer_view_pager", visible = false, children = listOf(
                FakeNode("clips_video_container", visible = false)
            ))
        ))
        assertEquals(listOf("root", "feed"), collect(tree).resourceIds)
    }

    @Test
    fun `skips the whole subtree under a hidden view`() {
        val tree = FakeNode("root", children = listOf(
            FakeNode("hidden_container", visible = false, children = listOf(FakeNode("clips_media_component", visible = true)))
        ))
        assertEquals(listOf("root"), collect(tree).resourceIds)
    }

    @Test
    fun `stops at the depth limit and drops blanks and duplicates`() {
        val tree = FakeNode("a", children = listOf(
            FakeNode("b", text = " ", children = listOf(FakeNode("c"))),
            FakeNode("a")
        ))
        assertEquals(listOf("a", "b"), collect(tree, maxDepth = 1).resourceIds)
        assertEquals(emptyList(), collect(tree, maxDepth = 1).texts)
    }
}
