package com.minetallest.mino.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * SSE parsing for `POST /api/chat` (spec §9.1): `data: {json}` lines terminated by
 * `data: [DONE]`. Chunk-safe: feed raw bytes/strings as they arrive.
 */
sealed class ChatEvent {
    data class Search(val used: Boolean, val query: String, val sources: List<SearchSource>) : ChatEvent()
    data class Google(val connected: Boolean) : ChatEvent()
    data class Model(val mode: String?, val provider: String?, val model: String) : ChatEvent()
    data class Content(val content: String) : ChatEvent()
    data class Usage(val prompt: Int, val completion: Int, val total: Int) : ChatEvent()
    data object Truncated : ChatEvent()
    data class Error(val message: String) : ChatEvent()
    data object Done : ChatEvent()
}

object SseEvents {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Parse one `data:` payload (without the `data: ` prefix). */
    fun parsePayload(payload: String): ChatEvent? {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed == "[DONE]") return ChatEvent.Done
        val obj = try {
            json.parseToJsonElement(trimmed).jsonObject
        } catch (e: Exception) {
            return null
        }
        return when {
            obj.containsKey("error") ->
                ChatEvent.Error(obj["error"]?.jsonPrimitive?.content ?: "Something went wrong.")
            obj.containsKey("content") ->
                ChatEvent.Content(obj["content"]?.jsonPrimitive?.content ?: "")
            obj.containsKey("truncated") ->
                ChatEvent.Truncated
            obj.containsKey("mode") || (obj.containsKey("model") && !obj.containsKey("search")) ->
                ChatEvent.Model(
                    mode = obj["mode"]?.jsonPrimitive?.content,
                    provider = obj["provider"]?.jsonPrimitive?.content,
                    model = obj["model"]?.jsonPrimitive?.content ?: "Mino",
                )
            obj.containsKey("usage") -> {
                val usage = obj["usage"]!!.jsonObject
                ChatEvent.Usage(
                    prompt = usage["prompt"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    completion = usage["completion"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    total = usage["total"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                )
            }
            obj.containsKey("search") -> {
                val search = obj["search"]!!.jsonObject
                val sources = search["sources"]?.jsonArray?.mapNotNull { element ->
                    val s = element.jsonObject
                    SearchSource(
                        id = s["id"]?.jsonPrimitive?.content ?: "",
                        title = s["title"]?.jsonPrimitive?.content ?: "",
                        url = s["url"]?.jsonPrimitive?.content ?: "",
                        snippet = s["snippet"]?.jsonPrimitive?.content ?: "",
                        publishedDate = s["publishedDate"]?.jsonPrimitive?.content,
                    )
                } ?: emptyList()
                ChatEvent.Search(
                    used = search["used"]?.jsonPrimitive?.booleanOrNull ?: sources.isNotEmpty(),
                    query = search["query"]?.jsonPrimitive?.content ?: "",
                    sources = sources,
                )
            }
            obj.containsKey("google") ->
                ChatEvent.Google(
                    obj["google"]!!.jsonObject["connected"]?.jsonPrimitive?.booleanOrNull ?: false,
                )
            else -> null
        }
    }
}
