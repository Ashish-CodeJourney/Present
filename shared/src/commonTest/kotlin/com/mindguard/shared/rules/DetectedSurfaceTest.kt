package com.mindguard.shared.rules

import com.mindguard.shared.models.ScreenSnapshot
import com.mindguard.shared.policy.BlockedSurface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetectedSurfaceTest {

    private fun snapshot(
        packageName: String,
        screenText: List<String> = emptyList(),
        resourceIds: List<String> = emptyList(),
        windowClassName: String? = null
    ) = ScreenSnapshot(
        packageName = packageName,
        screenText = screenText,
        contentDescriptions = emptyList(),
        resourceIds = resourceIds,
        timestampMillis = 1L,
        windowClassName = windowClassName
    )

    @Test
    fun instagramReelPlayerReportsReelsTab() {
        val result = InstagramReelRule().evaluate(snapshot("com.instagram.android", resourceIds = listOf("clips_viewer_view_pager")))
        assertTrue(result.shouldBlock)
        assertEquals(BlockedSurface.REELS_TAB, result.surface)
    }

    @Test
    fun instagramReelClassNameReportsReelsTab() {
        val result = InstagramReelRule().evaluate(snapshot("com.instagram.android", windowClassName = "ClipsViewerActivity"))
        assertEquals(BlockedSurface.REELS_TAB, result.surface)
    }

    @Test
    fun youtubeShortsPlayerReportsShortsPlayer() {
        val result = YouTubeShortsRule().evaluate(snapshot("com.google.android.youtube", resourceIds = listOf("reel_watch_fragment_root")))
        assertTrue(result.shouldBlock)
        assertEquals(BlockedSurface.SHORTS_PLAYER, result.surface)
    }

    @Test
    fun youtubeShortsClassNameReportsShortsPlayer() {
        val result = YouTubeShortsRule().evaluate(snapshot("com.google.android.youtube", windowClassName = "ShortsActivity"))
        assertEquals(BlockedSurface.SHORTS_PLAYER, result.surface)
    }

    @Test
    fun snapchatSpotlightReportsSpotlight() {
        val result = SnapchatSpotlightRule().evaluate(snapshot("com.snapchat.android", resourceIds = listOf("spotlight_feed")))
        assertTrue(result.shouldBlock)
        assertEquals(BlockedSurface.SPOTLIGHT, result.surface)
    }

    @Test
    fun tiktokFeedCountsAsReelsTab() {
        val result = TikTokRule().evaluate(snapshot("com.zhiliaoapp.musically", resourceIds = listOf("feed_video_container")))
        assertTrue(result.shouldBlock)
        assertEquals(BlockedSurface.REELS_TAB, result.surface)
    }

    @Test
    fun nonBlockingResultHasNoSurface() {
        val result = InstagramReelRule().evaluate(snapshot("com.instagram.android", resourceIds = listOf("feed_pager")))
        assertNull(result.surface)
    }

    @Test
    fun ruleEnginePropagatesSurfaceOfMatchingRule() {
        val engine = RuleEngine(listOf(InstagramReelRule(), YouTubeShortsRule(), TikTokRule(), SnapchatSpotlightRule()))
        val result = engine.evaluate(snapshot("com.google.android.youtube", resourceIds = listOf("reel_progress_bar")))
        assertEquals(BlockedSurface.SHORTS_PLAYER, result.surface)
    }
}
