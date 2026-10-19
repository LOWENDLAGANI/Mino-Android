package com.minetallest.mino.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Spec §7.1 — the Dexie tables, mapped to SQLite. */
@Entity(tableName = "chats", indices = [Index("updatedAt"), Index("pinned")])
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean,
)

@Entity(tableName = "messages", indices = [Index("chatId"), Index("createdAt")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val model: String?,
    val searchQuery: String?,
    val truncated: Boolean,
    val error: String?,
    val variantIndex: Int,
    val createdAt: Long,
    val updatedAt: Long?,
    val images: String?,
    val generatedImages: String?,
    val documents: String?,
    val sources: String?,
    val usage: String?,
    val variants: String?,
    val pinned: Boolean,
)

@Entity(tableName = "memories", indices = [Index("createdAt")])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "trash", indices = [Index("deletedAt")])
data class TrashEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean,
    val messages: String,
    val deletedAt: Long,
)
