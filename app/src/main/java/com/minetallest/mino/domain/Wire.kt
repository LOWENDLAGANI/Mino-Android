package com.minetallest.mino.domain

import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put

/**
 * Request builder for `POST /api/chat` (spec §9.1).
 * Client-supplied `system` messages are discarded — the persona is server-side only (§1.3).
 */
object Wire {

    data class ChatRequest(
        val messages: List<ChatMessage>,
        val mode: ModeId,
        val searchMode: SearchMode,
        val reasoningEffort: ReasoningEffort,
        val responseLength: ResponseLength,
        val memories: List<Memory>,
        val timeZone: String,
    )

    fun buildChatRequest(request: ChatRequest): JsonObject = buildJsonObject {
        put("messages", buildJsonArray {
            request.messages
                .filter { it.role != Role.SYSTEM }
                .filter { it.displayedContent().isNotEmpty() || !it.images.isNullOrEmpty() }
                .forEach { message ->
                    addJsonObject {
                        put("role", message.role.wire)
                        val documents = message.documents.orEmpty()
                        val text = message.displayedContent()
                        val hasAttachments = documents.isNotEmpty() || !message.images.isNullOrEmpty()
                        if (!hasAttachments) {
                            put("content", text)
                        } else {
                            put("content", buildJsonArray {
                                if (documents.isNotEmpty() || text.isNotEmpty()) {
                                    addJsonObject {
                                        put("type", "text")
                                        put(
                                            "text",
                                            if (documents.isNotEmpty()) {
                                                val blocks = documents.joinToString("\n\n") { doc ->
                                                    "Attachment ${doc.name}:\n${doc.text}"
                                                }
                                                if (text.isEmpty()) blocks else "$text\n\n$blocks"
                                            } else text,
                                        )
                                    }
                                }
                                message.images.orEmpty().forEach { image ->
                                    addJsonObject {
                                        put("type", "image_url")
                                        put("image_url", buildJsonObject { put("url", image.url) })
                                    }
                                }
                            })
                        }
                    }
                }
        })
        put("mode", request.mode.wire)
        put("searchMode", request.searchMode.wire)
        put("reasoningEffort", request.reasoningEffort.wire)
        put("responseLength", request.responseLength.wire)
        put("memories", buildJsonArray {
            request.memories.forEach { memory ->
                addJsonObject {
                    put("id", memory.id)
                    put("text", memory.text)
                    put("createdAt", memory.createdAt)
                    put("updatedAt", memory.updatedAt)
                }
            }
        })
        put("timeZone", request.timeZone)
    }
}
