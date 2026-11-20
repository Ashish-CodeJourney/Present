package com.mindguard.network

import com.mindguard.shared.policy.CompileResponse
import com.mindguard.shared.policy.PolicyJson
import com.mindguard.shared.policy.PresentPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class InsightsRequest(
    val interceptsByApp: Map<String, Long>,
    val streakDays: Long,
    val totalIntercepts: Long
)

sealed interface InsightResult {
    data class Success(val headline: String, val insight: String, val suggestion: String) : InsightResult
    data class Failure(val message: String) : InsightResult
}

interface CompilerClient {
    suspend fun compile(rawPrompt: String, existing: PresentPolicy?): CompileResponse
    suspend fun insights(request: InsightsRequest): InsightResult
}

const val UNREACHABLE_MESSAGE = "Couldn't reach Present's compiler. Check your connection."
const val INSIGHTS_FAILED_MESSAGE = "Gemini couldn't write your insight right now. Try again in a moment."

fun encodeInsightsRequest(request: InsightsRequest): String {
    val apps = request.interceptsByApp.entries.joinToString(",") { (app, count) ->
        "\"${app.replace("\\", "\\\\").replace("\"", "\\\"")}\":$count"
    }
    return "{\"intercepts_by_app\":{$apps},\"streak_days\":${request.streakDays},\"total_intercepts\":${request.totalIntercepts}}"
}

class HttpCompilerClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val timeoutMs: Int = 15_000
) : CompilerClient {

    private data class HttpResult(val status: Int, val body: String)

    override suspend fun compile(rawPrompt: String, existing: PresentPolicy?): CompileResponse =
        try {
            val result = post("/v1/compile", PolicyJson.encodeCompileRequest(rawPrompt, existing))
            PolicyJson.decodeCompileResponse(result.status, result.body)
        } catch (e: IOException) {
            CompileResponse.Failed(UNREACHABLE_MESSAGE)
        }

    override suspend fun insights(request: InsightsRequest): InsightResult =
        try {
            val result = post("/v1/insights", encodeInsightsRequest(request))
            if (result.status != HttpURLConnection.HTTP_OK) {
                InsightResult.Failure(INSIGHTS_FAILED_MESSAGE)
            } else {
                val json = JSONObject(result.body)
                InsightResult.Success(
                    headline = json.getString("headline"),
                    insight = json.getString("insight"),
                    suggestion = json.getString("suggestion")
                )
            }
        } catch (e: IOException) {
            InsightResult.Failure(UNREACHABLE_MESSAGE)
        } catch (e: org.json.JSONException) {
            InsightResult.Failure(INSIGHTS_FAILED_MESSAGE)
        }

    private suspend fun post(path: String, body: String): HttpResult = withContext(Dispatchers.IO) {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("x-present-key", apiKey)
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            HttpResult(status, responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
