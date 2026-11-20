package com.mindguard.network

import kotlin.test.Test
import kotlin.test.assertEquals

class InsightsRequestTest {

    @Test
    fun encodesTheServerContractShape() {
        val request = InsightsRequest(
            interceptsByApp = mapOf("instagram" to 12L, "youtube" to 7L, "tiktok" to 0L, "snapchat" to 3L),
            streakDays = 5L,
            totalIntercepts = 22L
        )

        assertEquals(
            """{"intercepts_by_app":{"instagram":12,"youtube":7,"tiktok":0,"snapchat":3},"streak_days":5,"total_intercepts":22}""",
            encodeInsightsRequest(request)
        )
    }

    @Test
    fun encodesAnEmptyAppMap() {
        val request = InsightsRequest(interceptsByApp = emptyMap(), streakDays = 0L, totalIntercepts = 0L)

        assertEquals(
            """{"intercepts_by_app":{},"streak_days":0,"total_intercepts":0}""",
            encodeInsightsRequest(request)
        )
    }
}
