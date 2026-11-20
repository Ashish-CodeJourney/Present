package com.mindguard.ui.present

import com.mindguard.network.CompilerClient
import com.mindguard.network.InsightResult
import com.mindguard.network.InsightsRequest
import com.mindguard.shared.policy.AllowedSurface
import com.mindguard.shared.policy.BlockedSurface
import com.mindguard.shared.policy.CompileResponse
import com.mindguard.shared.policy.Platform
import com.mindguard.shared.policy.PolicyRule
import com.mindguard.shared.policy.PresentPolicy
import com.mindguard.shared.policy.Schedule
import com.mindguard.shared.policy.ScheduleMode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun aPolicy(summary: String = "No Reels after 9 PM, DMs stay open") = PresentPolicy(
    policyVersion = "1",
    summary = summary,
    rules = listOf(
        PolicyRule(
            platform = Platform.INSTAGRAM,
            targetSurfaces = listOf(BlockedSurface.REELS_TAB),
            allowedSurfaces = listOf(AllowedSurface.DIRECT_MESSAGES),
            schedule = Schedule(mode = ScheduleMode.TIME_WINDOW, startTime = "21:00", endTime = "06:00", dailyLimitMinutes = null),
            whitelistedEntities = emptyList()
        )
    ),
    pauseCardSettings = null
)

private class FakeCompilerClient(
    private val respond: suspend (String, PresentPolicy?) -> CompileResponse
) : CompilerClient {
    val prompts = mutableListOf<String>()
    val existingPolicies = mutableListOf<PresentPolicy?>()
    var onCall: () -> Unit = {}

    override suspend fun compile(rawPrompt: String, existing: PresentPolicy?): CompileResponse {
        prompts += rawPrompt
        existingPolicies += existing
        onCall()
        return respond(rawPrompt, existing)
    }

    override suspend fun insights(request: InsightsRequest): InsightResult =
        InsightResult.Failure("not used")
}

class IntentCompilerTest {

    @Test
    fun startsIdle() {
        val compiler = IntentCompiler(FakeCompilerClient { _, _ -> CompileResponse.Failed("x") })
        assertEquals(CompileUiState.Idle, compiler.state.value)
    }

    @Test
    fun compiledPolicyMovesToReview() = runBlocking {
        val policy = aPolicy()
        val compiler = IntentCompiler(FakeCompilerClient { _, _ -> CompileResponse.Compiled(policy) })

        compiler.compile("No Reels after 9", existing = null)

        assertEquals(CompileUiState.Review(policy), compiler.state.value)
    }

    @Test
    fun rejectedIntentShowsTheReason() = runBlocking {
        val compiler = IntentCompiler(FakeCompilerClient { _, _ -> CompileResponse.Rejected("I can't block phone calls") })

        compiler.compile("Block calls", existing = null)

        assertEquals(CompileUiState.Rejected("I can't block phone calls"), compiler.state.value)
    }

    @Test
    fun failedCompileShowsTheErrorMessage() = runBlocking {
        val compiler = IntentCompiler(FakeCompilerClient { _, _ -> CompileResponse.Failed("Couldn't reach Present's compiler.") })

        compiler.compile("No Reels", existing = null)

        assertEquals(CompileUiState.Error("Couldn't reach Present's compiler."), compiler.state.value)
    }

    @Test
    fun clientExceptionBecomesAnError() = runBlocking {
        val compiler = IntentCompiler(FakeCompilerClient { _, _ -> throw IllegalStateException("boom") })

        compiler.compile("No Reels", existing = null)

        assertEquals(CompileUiState.Error(GENERIC_COMPILE_ERROR), compiler.state.value)
    }

    @Test
    fun isLoadingWhileTheCompilerRuns() = runBlocking {
        val client = FakeCompilerClient { _, _ -> CompileResponse.Compiled(aPolicy()) }
        val compiler = IntentCompiler(client)
        var stateDuringCall: CompileUiState? = null
        client.onCall = { stateDuringCall = compiler.state.value }

        compiler.compile("No Reels", existing = null)

        assertEquals(CompileUiState.Loading, stateDuringCall)
    }

    @Test
    fun blankPromptDoesNotCallTheCompiler() = runBlocking {
        val client = FakeCompilerClient { _, _ -> CompileResponse.Compiled(aPolicy()) }
        val compiler = IntentCompiler(client)

        compiler.compile("   ", existing = null)

        assertEquals(CompileUiState.Idle, compiler.state.value)
        assertEquals(emptyList(), client.prompts)
    }

    @Test
    fun sendsTrimmedPromptAndExistingPolicy() = runBlocking {
        val existing = aPolicy(summary = "Old rules")
        val client = FakeCompilerClient { _, _ -> CompileResponse.Compiled(aPolicy()) }
        val compiler = IntentCompiler(client)

        compiler.compile("  Also block Shorts  ", existing = existing)

        assertEquals(listOf("Also block Shorts"), client.prompts)
        assertEquals(listOf<PresentPolicy?>(existing), client.existingPolicies)
    }

    @Test
    fun noExistingPolicyIsSentAsNull() = runBlocking {
        val client = FakeCompilerClient { _, _ -> CompileResponse.Compiled(aPolicy()) }
        IntentCompiler(client).compile("No Reels", existing = null)
        assertNull(client.existingPolicies.single())
    }

    @Test
    fun resetReturnsToIdle() = runBlocking {
        val compiler = IntentCompiler(FakeCompilerClient { _, _ -> CompileResponse.Rejected("nope") })
        compiler.compile("Block calls", existing = null)

        compiler.reset()

        assertEquals(CompileUiState.Idle, compiler.state.value)
    }
}

class InsightsLoaderTest {

    private val request = InsightsRequest(
        interceptsByApp = mapOf("instagram" to 3L, "youtube" to 2L, "tiktok" to 0L, "snapchat" to 1L),
        streakDays = 4L,
        totalIntercepts = 6L
    )

    private class FakeInsightsClient(private val result: suspend () -> InsightResult) : CompilerClient {
        var onCall: () -> Unit = {}
        val requests = mutableListOf<InsightsRequest>()
        override suspend fun compile(rawPrompt: String, existing: PresentPolicy?): CompileResponse =
            CompileResponse.Failed("not used")
        override suspend fun insights(request: InsightsRequest): InsightResult {
            requests += request
            onCall()
            return result()
        }
    }

    @Test
    fun startsIdle() {
        val loader = InsightsLoader(FakeInsightsClient { InsightResult.Failure("x") })
        assertEquals(InsightsUiState.Idle, loader.state.value)
    }

    @Test
    fun successfulInsightIsReady() = runBlocking {
        val client = FakeInsightsClient { InsightResult.Success("Night owl", "Most intercepts after 10 PM", "Charge your phone outside the bedroom") }
        val loader = InsightsLoader(client)

        loader.load(request)

        assertEquals(
            InsightsUiState.Ready("Night owl", "Most intercepts after 10 PM", "Charge your phone outside the bedroom"),
            loader.state.value
        )
        assertEquals(listOf(request), client.requests)
    }

    @Test
    fun failureBecomesError() = runBlocking {
        val loader = InsightsLoader(FakeInsightsClient { InsightResult.Failure("Gemini is busy") })
        loader.load(request)
        assertEquals(InsightsUiState.Error("Gemini is busy"), loader.state.value)
    }

    @Test
    fun exceptionBecomesError() = runBlocking {
        val loader = InsightsLoader(FakeInsightsClient { throw IllegalStateException("boom") })
        loader.load(request)
        assertEquals(InsightsUiState.Error(GENERIC_INSIGHTS_ERROR), loader.state.value)
    }

    @Test
    fun isLoadingWhileFetching() = runBlocking {
        val client = FakeInsightsClient { InsightResult.Success("h", "i", "s") }
        val loader = InsightsLoader(client)
        var stateDuringCall: InsightsUiState? = null
        client.onCall = { stateDuringCall = loader.state.value }

        loader.load(request)

        assertEquals(InsightsUiState.Loading, stateDuringCall)
    }
}
