package com.minetallest.mino.domain

import kotlinx.serialization.Serializable

/** Wire mode ids — see spec §8.1. */
enum class ModeId(val wire: String) {
    AUTO("auto"), CODE("code"), SELF("self");

    companion object {
        fun fromWire(value: String?): ModeId = entries.firstOrNull { it.wire == value } ?: AUTO
        /** Legacy Dexie value accepted by the web build. */
        fun fromStored(value: String?): ModeId = when (value) {
            "dev" -> CODE
            else -> fromWire(value)
        }
    }
}

enum class SearchMode(val wire: String) {
    AUTO("auto"), ALWAYS("always"), OFF("off");

    companion object {
        fun fromWire(value: String?): SearchMode = entries.firstOrNull { it.wire == value } ?: AUTO
    }
}

enum class ResponseLength(val wire: String) {
    SHORT("short"), BALANCED("balanced"), DETAILED("detailed");

    companion object {
        fun fromWire(value: String?): ResponseLength =
            entries.firstOrNull { it.wire == value } ?: BALANCED
    }
}

enum class ReasoningEffort(val wire: String) {
    LOW("low"), MEDIUM("medium"), HIGH("high");

    companion object {
        fun fromWire(value: String?): ReasoningEffort = entries.firstOrNull { it.wire == value } ?: LOW
    }
}

enum class Role(val wire: String) {
    USER("user"), ASSISTANT("assistant"), SYSTEM("system");

    companion object {
        fun fromWire(value: String?): Role = entries.firstOrNull { it.wire == value } ?: USER
    }
}

@Serializable
data class ImageAttachment(
    val url: String,
    val name: String,
    val size: Long,
)

@Serializable
data class GeneratedImage(
    val url: String,
    val prompt: String,
    val mime: String,
    val model: String,
    val createdAt: Long,
)

@Serializable
data class DocumentAttachment(
    val name: String,
    val size: Long,
    val text: String,
    val truncated: Boolean = false,
)

@Serializable
data class SearchSource(
    val id: String,
    val title: String,
    val url: String,
    val snippet: String,
    val publishedDate: String? = null,
)

@Serializable
data class TokenUsage(
    val prompt: Int,
    val completion: Int,
    val total: Int,
)

/** Spec §7.1 — persisted verbatim; [variantIndex] k shows variants[k-1], 0/absent shows content. */
@Serializable
data class ChatMessage(
    val id: String,
    val chatId: String,
    val role: Role,
    val content: String = "",
    val images: List<ImageAttachment>? = null,
    val generatedImages: List<GeneratedImage>? = null,
    val documents: List<DocumentAttachment>? = null,
    val model: String? = null,
    val searchQuery: String? = null,
    val sources: List<SearchSource>? = null,
    val usage: TokenUsage? = null,
    val truncated: Boolean = false,
    val error: String? = null,
    val variants: List<String>? = null,
    val variantIndex: Int = 0,
    val createdAt: Long,
    val updatedAt: Long? = null,
    val pinned: Boolean = false,
) {
    /** The answer currently on screen (spec §6.6 variant stepper). */
    fun displayedContent(): String =
        if (variantIndex > 0) variants?.getOrNull(variantIndex - 1) ?: content else content

    fun hasVariants(): Boolean = !variants.isNullOrEmpty()
}

@Serializable
data class Chat(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false,
    val folder: String? = null,
)

@Serializable
data class ChatFolder(
    val id: String,
    val name: String,
    val createdAt: Long,
)

@Serializable
data class Memory(
    val id: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class TrashEntry(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false,
    val folder: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val deletedAt: Long,
)

/** Spec §7.1 — matches Dexie's `mino-backup-YYYY-MM-DD.json`. */
@Serializable
data class MinoBackup(
    val app: String,
    val version: Int,
    val exportedAt: String,
    val chats: List<Chat>,
    val messages: List<ChatMessage>,
)
