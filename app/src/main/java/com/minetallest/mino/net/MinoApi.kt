package com.minetallest.mino.net

import com.minetallest.mino.domain.Memory
import com.minetallest.mino.domain.ModeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException

/** Non-stream endpoints: availability, plan, memory suggestions, image, Dew (spec §9). */
class MinoApi(private val api: ApiClient) {

    data class Availability(
        val available: List<ModeId>,
        val searchAvailable: Boolean,
        val googleAvailable: Boolean,
        val imageAvailable: Boolean,
    )

    data class PlanInfo(
        val planId: String?,
        val expiresAt: Long,
        val paused: Boolean,
        val checked: Boolean,
        val isAdmin: Boolean,
    )

    data class DewInfo(val balance: Double, val configured: Boolean)

    suspend fun availability(): Availability? {
        val obj = api.getJsonObject("/api/chat") ?: return null
        val available = obj["available"]?.jsonArray
            ?.mapNotNull { ModeId.fromWire(it.jsonPrimitive.content) } ?: emptyList()
        return Availability(
            available = available,
            searchAvailable = obj["searchAvailable"]?.jsonPrimitive?.content?.toBoolean() ?: false,
            googleAvailable = obj["googleAvailable"]?.jsonPrimitive?.content?.toBoolean() ?: false,
            imageAvailable = obj["imageAvailable"]?.jsonPrimitive?.content?.toBoolean() ?: false,
        )
    }

    suspend fun plan(): PlanInfo? {
        val obj = api.getJsonObject("/api/plan") ?: return null
        return PlanInfo(
            planId = obj["planId"]?.jsonPrimitive?.content?.takeIf { it != "null" },
            expiresAt = obj["expiresAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            paused = obj["paused"]?.jsonPrimitive?.content?.toBoolean() ?: false,
            checked = obj["checked"]?.jsonPrimitive?.content?.toBoolean() ?: false,
            isAdmin = obj["isAdmin"]?.jsonPrimitive?.content?.toBoolean() ?: false,
        )
    }

    /** Any failure returns `[]` — memory suggestions never block the answer (spec §9.3). */
    suspend fun memorySuggestions(
        userText: String,
        answer: String,
        memories: List<Memory>,
    ): List<String> {
        if (userText.length < 12 || !api.isConfigured()) return emptyList()
        return try {
            val body = buildJsonObject {
                put("userText", userText.take(4000))
                put("answer", answer.take(4000))
                put("memories", buildJsonArray {
                    memories.forEach { m ->
                        buildJsonObject { put("id", m.id); put("text", m.text) }.let { add(it) }
                    }
                })
            }
            withContext(Dispatchers.IO) {
                val response = api.http.newCall(api.postJson("/api/memory", body)).execute()
                response.use {
                    if (!it.isSuccessful) return@use emptyList()
                    val text = it.body?.string() ?: return@use emptyList()
                    api.json.parseToJsonElement(text).jsonObject["suggestions"]?.jsonArray
                        ?.mapNotNull { s -> s.jsonPrimitive.content.takeIf { t -> t.isNotBlank() } }
                        ?: emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    sealed class ImageResult {
        data class Ok(val bytes: ByteArray, val model: String?) : ImageResult() {
            override fun equals(other: Any?): Boolean = other is Ok && other.bytes.contentEquals(bytes)
            override fun hashCode(): Int = bytes.contentHashCode()
        }
        data class Error(val status: Int, val message: String) : ImageResult()
        data object NotConfigured : ImageResult()
    }

    /** `POST /api/image` with the spec's error sentences (§9.2). */
    suspend fun generateImage(prompt: String): ImageResult = withContext(Dispatchers.IO) {
        if (!api.isConfigured()) return@withContext ImageResult.NotConfigured
        try {
            val body = buildJsonObject { put("prompt", prompt) }
            val response = api.http.newCall(api.postJson("/api/image", body)).execute()
            response.use {
                val contentType = it.header("Content-Type").orEmpty()
                if (it.isSuccessful && contentType.startsWith("image/")) {
                    val bytes = it.body?.bytes() ?: ByteArray(0)
                    return@withContext ImageResult.Ok(bytes, it.header("X-Mino-Image-Model"))
                }
                val text = it.body?.string().orEmpty()
                val message = serverImageError(text, it.code)
                ImageResult.Error(it.code, message)
            }
        } catch (e: IOException) {
            ImageResult.Error(0, SseChatClient.NETWORK_ERROR)
        }
    }

    private fun serverImageError(body: String, status: Int): String {
        val serverMessage = try {
            org.json.JSONObject(body).optString("error").ifBlank { null }
        } catch (e: Exception) {
            null
        }
        return serverMessage ?: when (status) {
            503 -> "Mino's image model is not set up on this server yet."
            402 -> com.minetallest.mino.domain.Refusals.PAUSED_PLAN
            429, 401, 403 -> "Daily generation limit reached. Please try again tomorrow."
            else -> "Mino could not generate that image (HTTP $status)."
        }
    }

    /** `GET /api/dew` — balance, best-effort (spec §11.6). */
    suspend fun dew(): DewInfo? {
        val obj = api.getJsonObject("/api/dew") ?: return null
        val balance = obj["balance"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
        return DewInfo(balance = balance, configured = true)
    }

    /** `POST /api/dew` — claims the one-time trial (server-pinned value, spec §11.1). */
    suspend fun claimDewTrial(): DewInfo? {
        return try {
            withContext(Dispatchers.IO) {
                val response = api.http.newCall(api.postJson("/api/dew", buildJsonObject {})).execute()
                response.use {
                    if (!it.isSuccessful) return@use null
                    val text = it.body?.string() ?: return@use null
                    val balance = api.json.parseToJsonElement(text).jsonObject["balance"]
                        ?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@use null
                    DewInfo(balance, true)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
