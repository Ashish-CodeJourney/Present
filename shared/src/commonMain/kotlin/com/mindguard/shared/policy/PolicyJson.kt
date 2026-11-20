package com.mindguard.shared.policy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

object PolicyJson {

    private const val HTTP_OK = 200
    private const val HTTP_UNPROCESSABLE = 422

    // encodeDefaults stays false (the default): the server's Zod schema uses .optional(),
    // which rejects null, so every nullable field defaults to null and is omitted on encode.
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class CompileRequest(
        @SerialName("raw_prompt") val rawPrompt: String,
        @SerialName("existing_policy") val existingPolicy: PresentPolicy? = null
    )

    fun encode(policy: PresentPolicy): String = json.encodeToString(PresentPolicy.serializer(), policy)

    fun decode(json: String): PresentPolicy? = try {
        this.json.decodeFromString(PresentPolicy.serializer(), json)
    } catch (e: Exception) {
        null
    }

    fun encodeCompileRequest(rawPrompt: String, existingPolicy: PresentPolicy?): String =
        json.encodeToString(CompileRequest.serializer(), CompileRequest(rawPrompt, existingPolicy))

    fun decodeCompileResponse(httpStatus: Int, body: String): CompileResponse {
        val root = parseObject(body) ?: return CompileResponse.Failed(genericFailure(httpStatus))
        val status = root.string("status")

        if (httpStatus == HTTP_OK && status == "compiled") {
            val policy = root["policy"]?.let { decodePolicy(it) }
            return policy?.let { CompileResponse.Compiled(it) }
                ?: CompileResponse.Failed("The compiler returned a policy this app can't read.")
        }

        if (httpStatus == HTTP_UNPROCESSABLE && status == "rejected") {
            val reason = root.string("reason")
            if (!reason.isNullOrBlank()) return CompileResponse.Rejected(reason)
        }

        val message = root.string("message")
        return CompileResponse.Failed(if (message.isNullOrBlank()) genericFailure(httpStatus) else message)
    }

    private fun parseObject(body: String): JsonObject? = try {
        json.parseToJsonElement(body).jsonObject
    } catch (e: Exception) {
        null
    }

    private fun decodePolicy(element: JsonElement): PresentPolicy? = try {
        json.decodeFromJsonElement(PresentPolicy.serializer(), element)
    } catch (e: Exception) {
        null
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun genericFailure(httpStatus: Int) =
        "Couldn't reach the policy compiler (HTTP $httpStatus). Please try again."
}
