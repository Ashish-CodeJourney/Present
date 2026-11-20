package com.mindguard.shared.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PolicyJsonTest {

    private val serverPolicyJson = """
        {
          "policy_version": "1.0",
          "summary": "Instagram DMs and YouTube subscriptions stay open. Reels and Shorts are blocked from 9 PM to 6 AM.",
          "rules": [
            {
              "platform": "instagram",
              "target_surfaces": ["reels_tab", "explore_grid"],
              "allowed_surfaces": ["direct_messages", "profile"],
              "schedule": { "mode": "time_window", "start_time": "21:00", "end_time": "06:00" }
            },
            {
              "platform": "youtube",
              "target_surfaces": ["shorts_player", "home_feed"],
              "allowed_surfaces": ["subscriptions", "search", "creator_channel"],
              "schedule": { "mode": "daily_allowance", "daily_limit_minutes": 20 },
              "whitelisted_entities": ["Veritasium"]
            },
            {
              "platform": "snapchat",
              "target_surfaces": ["spotlight"],
              "allowed_surfaces": [],
              "schedule": { "mode": "always" }
            }
          ],
          "pause_card_settings": {
            "delay_seconds": 10,
            "prompt_reflection": true,
            "side_quest_category": "hydration"
          }
        }
    """.trimIndent()

    private val expectedPolicy = PresentPolicy(
        policyVersion = "1.0",
        summary = "Instagram DMs and YouTube subscriptions stay open. Reels and Shorts are blocked from 9 PM to 6 AM.",
        rules = listOf(
            PolicyRule(
                platform = Platform.INSTAGRAM,
                targetSurfaces = listOf(BlockedSurface.REELS_TAB, BlockedSurface.EXPLORE_GRID),
                allowedSurfaces = listOf(AllowedSurface.DIRECT_MESSAGES, AllowedSurface.PROFILE),
                schedule = Schedule(mode = ScheduleMode.TIME_WINDOW, startTime = "21:00", endTime = "06:00")
            ),
            PolicyRule(
                platform = Platform.YOUTUBE,
                targetSurfaces = listOf(BlockedSurface.SHORTS_PLAYER, BlockedSurface.HOME_FEED),
                allowedSurfaces = listOf(AllowedSurface.SUBSCRIPTIONS, AllowedSurface.SEARCH, AllowedSurface.CREATOR_CHANNEL),
                schedule = Schedule(mode = ScheduleMode.DAILY_ALLOWANCE, dailyLimitMinutes = 20),
                whitelistedEntities = listOf("Veritasium")
            ),
            PolicyRule(
                platform = Platform.SNAPCHAT,
                targetSurfaces = listOf(BlockedSurface.SPOTLIGHT),
                allowedSurfaces = emptyList(),
                schedule = Schedule(mode = ScheduleMode.ALWAYS)
            )
        ),
        pauseCardSettings = PauseCardSettings(delaySeconds = 10, promptReflection = true, sideQuestCategory = "hydration")
    )

    private val minimalPolicy = PresentPolicy(
        policyVersion = "1.0",
        summary = "No Reels.",
        rules = listOf(
            PolicyRule(
                platform = Platform.INSTAGRAM,
                targetSurfaces = listOf(BlockedSurface.REELS_TAB),
                allowedSurfaces = listOf(AllowedSurface.DIRECT_MESSAGES),
                schedule = Schedule(mode = ScheduleMode.ALWAYS)
            )
        )
    )

    @Test
    fun decodesServerSnakeCaseJson() {
        assertEquals(expectedPolicy, PolicyJson.decode(serverPolicyJson))
    }

    @Test
    fun roundTripsThroughEncodeAndDecode() {
        assertEquals(expectedPolicy, PolicyJson.decode(PolicyJson.encode(expectedPolicy)))
        assertEquals(minimalPolicy, PolicyJson.decode(PolicyJson.encode(minimalPolicy)))
    }

    @Test
    fun encodesSnakeCaseKeysAndLowercaseEnumValues() {
        val json = PolicyJson.encode(expectedPolicy)
        listOf(
            "\"policy_version\"", "\"target_surfaces\"", "\"allowed_surfaces\"", "\"start_time\"",
            "\"end_time\"", "\"daily_limit_minutes\"", "\"whitelisted_entities\"", "\"pause_card_settings\"",
            "\"delay_seconds\"", "\"prompt_reflection\"", "\"side_quest_category\"",
            "\"instagram\"", "\"reels_tab\"", "\"direct_messages\"", "\"time_window\"", "\"daily_allowance\""
        ).forEach { assertTrue(json.contains(it), "expected $it in $json") }
    }

    @Test
    fun omitsAbsentOptionalFieldsInsteadOfWritingNull() {
        val json = PolicyJson.encode(minimalPolicy)
        assertFalse(json.contains("null"), json)
        assertFalse(json.contains("pause_card_settings"), json)
        assertFalse(json.contains("start_time"), json)
    }

    @Test
    fun ignoresUnknownKeys() {
        val json = """{"policy_version":"1.0","summary":"s","rules":[],"extra":42}"""
        assertEquals(PresentPolicy(policyVersion = "1.0", summary = "s", rules = emptyList()), PolicyJson.decode(json))
    }

    @Test
    fun missingWhitelistDefaultsToEmpty() {
        val rule = PolicyJson.decode(serverPolicyJson)!!.rules.first()
        assertEquals(emptyList(), rule.whitelistedEntities)
    }

    @Test
    fun decodeReturnsNullOnMalformedInput() {
        assertNull(PolicyJson.decode("not json"))
        assertNull(PolicyJson.decode(""))
        assertNull(PolicyJson.decode("""{"summary":"missing fields"}"""))
        assertNull(PolicyJson.decode("""{"policy_version":"1","summary":"s","rules":[{"platform":"myspace"}]}"""))
    }

    @Test
    fun compileRequestCarriesRawPromptOnlyWhenNoExistingPolicy() {
        val json = PolicyJson.encodeCompileRequest("No Reels after 9pm", null)
        assertEquals("""{"raw_prompt":"No Reels after 9pm"}""", json)
    }

    @Test
    fun compileRequestEmbedsExistingPolicy() {
        val json = PolicyJson.encodeCompileRequest("Also block Shorts", minimalPolicy)
        assertTrue(json.contains("\"raw_prompt\":\"Also block Shorts\""), json)
        assertTrue(json.contains("\"existing_policy\":{"), json)
        assertTrue(json.contains("\"policy_version\":\"1.0\""), json)
    }

    @Test
    fun compileRequestEscapesSpecialCharacters() {
        val json = PolicyJson.encodeCompileRequest("say \"no\"\nto reels", null)
        assertEquals("""{"raw_prompt":"say \"no\"\nto reels"}""", json)
    }

    @Test
    fun compiledResponseYieldsPolicy() {
        val body = """{"status":"compiled","policy":$serverPolicyJson}"""
        val response = PolicyJson.decodeCompileResponse(200, body)
        assertEquals(CompileResponse.Compiled(expectedPolicy), response)
    }

    @Test
    fun rejectedResponseYieldsReason() {
        val body = """{"status":"rejected","reason":"Present can't read your messages."}"""
        assertEquals(CompileResponse.Rejected("Present can't read your messages."), PolicyJson.decodeCompileResponse(422, body))
    }

    @Test
    fun errorResponseYieldsServerMessage() {
        val body = """{"status":"error","message":"Gemini timed out"}"""
        assertEquals(CompileResponse.Failed("Gemini timed out"), PolicyJson.decodeCompileResponse(502, body))
    }

    @Test
    fun unauthorizedWithMessageYieldsServerMessage() {
        val body = """{"status":"error","message":"invalid api key"}"""
        assertEquals(CompileResponse.Failed("invalid api key"), PolicyJson.decodeCompileResponse(401, body))
    }

    @Test
    fun unparseableBodyYieldsGenericFailure() {
        val response = PolicyJson.decodeCompileResponse(500, "<html>Internal Server Error</html>")
        val failed = assertIs<CompileResponse.Failed>(response)
        assertTrue(failed.message.isNotBlank())
    }

    @Test
    fun okStatusWithMalformedPolicyFails() {
        val body = """{"status":"compiled","policy":{"summary":"incomplete"}}"""
        assertIs<CompileResponse.Failed>(PolicyJson.decodeCompileResponse(200, body))
    }

    @Test
    fun okStatusWithoutCompiledMarkerFails() {
        val body = """{"status":"rejected","reason":"nope"}"""
        assertIs<CompileResponse.Failed>(PolicyJson.decodeCompileResponse(200, body))
    }

    @Test
    fun unprocessableWithoutReasonFails() {
        assertIs<CompileResponse.Failed>(PolicyJson.decodeCompileResponse(422, """{"status":"rejected"}"""))
    }

    @Test
    fun emptyBodyFails() {
        val failed = assertIs<CompileResponse.Failed>(PolicyJson.decodeCompileResponse(503, ""))
        assertNotNull(failed.message)
    }
}
