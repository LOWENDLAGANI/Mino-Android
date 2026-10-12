package com.minetallest.mino.data

import com.minetallest.mino.data.db.Mappers.toDomain
import com.minetallest.mino.data.db.Mappers.toEntity
import com.minetallest.mino.data.db.MinoDatabase
import com.minetallest.mino.domain.Backup
import com.minetallest.mino.domain.Chat
import com.minetallest.mino.domain.ChatFolder
import com.minetallest.mino.domain.ChatMessage
import com.minetallest.mino.domain.Memory
import com.minetallest.mino.domain.Rules
import com.minetallest.mino.domain.ScheduledMessage
import com.minetallest.mino.domain.TrashEntry
import com.minetallest.mino.domain.Variants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Dexie-equivalent repository (spec §7.1 behavioural rules):
 * auto-title, transactional delete → trash, folder re-filing, variant retirement,
 * backup/restore bounds, 30-day trash purge on start.
 */
class ChatRepository(private val db: MinoDatabase) {

    /** Pinned floated to top, then updatedAt desc (spec §6.4). */
    val chats: Flow<List<Chat>> = db.chatDao().observeAll()
        .map { list -> list.map { it.toDomain() } }
        .map { list ->
            list.sortedWith(compareByDescending<Chat> { it.pinned }.thenByDescending { it.updatedAt })
        }

    val folders: Flow<List<ChatFolder>> = db.folderDao().observeAll().map { it.map { f -> f.toDomain() } }
    val memories: Flow<List<Memory>> = db.memoryDao().observeAll().map { it.map { m -> m.toDomain() } }
    val scheduled: Flow<List<ScheduledMessage>> =
        db.scheduledDao().observeAll().map { it.map { s -> s.toDomain() } }
    val trash: Flow<List<TrashEntry>> = db.trashDao().observeAll().map { it.map { t -> t.toDomain() } }

    fun messagesFor(chatId: String): Flow<List<ChatMessage>> =
        db.messageDao().observeForChat(chatId).map { list -> list.map { it.toDomain() } }

    suspend fun allChats(): List<Chat> = db.chatDao().all().map { it.toDomain() }
    suspend fun allMessages(): List<ChatMessage> = db.messageDao().all().map { it.toDomain() }
    suspend fun chat(id: String): Chat? = db.chatDao().byId(id)?.toDomain()
    suspend fun messagesOnce(chatId: String): List<ChatMessage> =
        db.messageDao().forChat(chatId).map { it.toDomain() }

    /** Expired trash is purged on app start (spec §10.4). */
    suspend fun purgeExpiredTrash(now: Long) {
        db.trashDao().purgeExpired(now - Variants.TRASH_RETENTION_MS)
    }

    suspend fun createChat(now: Long): Chat {
        val chat = Chat(id = Rules.uid(), title = "New chat", createdAt = now, updatedAt = now)
        db.chatDao().upsert(chat.toEntity())
        return chat
    }

    suspend fun touch(chatId: String, now: Long) = db.chatDao().touch(chatId, now)

    suspend fun rename(chatId: String, title: String) {
        val collapsed = title.replace(Regex("\\s+"), " ").trim()
        if (collapsed.isNotEmpty()) db.chatDao().rename(chatId, collapsed)
    }

    suspend fun setPinned(chatId: String, pinned: Boolean) = db.chatDao().setPinned(chatId, pinned)

    suspend fun setFolder(chatId: String, folderId: String?) = db.chatDao().setFolder(chatId, folderId)

    /** Filing a chat does not touch updatedAt (spec §7.1). */
    suspend fun createFolder(name: String, now: Long): ChatFolder? {
        val normalized = Rules.normalizeFolderName(name) ?: return null
        val existing = db.folderDao().all()
        existing.firstOrNull { Rules.folderNameClash(it.name, normalized) }?.let { return it.toDomain() }
        val folder = ChatFolder(id = Rules.uid(), name = normalized, createdAt = now)
        db.folderDao().upsert(folder.toEntity())
        return folder
    }

    /** Deleting a folder returns its chats to unfiled and never deletes them (spec #33). */
    suspend fun deleteFolder(folderId: String) {
        db.chatDao().unfileFolder(folderId)
        db.folderDao().delete(folderId)
    }

    /** Insert or replace a message row verbatim (streaming updates, variant moves). */
    suspend fun putMessage(message: ChatMessage) = db.messageDao().upsert(message.toEntity())

    suspend fun deleteMessage(id: String) = db.messageDao().delete(id)

    /** Auto-title from the first user message, only while the title is still "New chat". */
    suspend fun appendUserMessage(chatId: String, message: ChatMessage, now: Long) {
        db.messageDao().upsert(message.toEntity())
        val chat = db.chatDao().byId(chatId) ?: return
        if (chat.title == "New chat" && message.role.wire == "user") {
            db.chatDao().rename(chatId, Rules.autoTitle(message.displayedContent()))
        }
        db.chatDao().touch(chatId, now)
    }

    /**
     * Delete → copy into Trash (30 days, device-only), then remove chat + messages (spec §10.4).
     */
    suspend fun deleteChat(chatId: String, now: Long) {
        val chat = db.chatDao().byId(chatId) ?: return
        val messages = db.messageDao().forChat(chatId).map { it.toDomain() }
        db.trashDao().upsert(
            TrashEntry(
                id = chat.id,
                title = chat.title,
                createdAt = chat.createdAt,
                updatedAt = chat.updatedAt,
                pinned = chat.pinned,
                folder = chat.folder,
                messages = messages,
                deletedAt = now,
            ).toEntity(),
        )
        db.messageDao().deleteForChat(chatId)
        db.chatDao().delete(chatId)
    }

    /** Restore puts the original id back (spec #30). */
    suspend fun restoreFromTrash(chatId: String): Chat? {
        val stored = db.trashDao().byId(chatId) ?: return null
        val entry = stored.toDomain()
        db.chatDao().upsert(
            Chat(entry.id, entry.title, entry.createdAt, entry.updatedAt, entry.pinned, entry.folder).toEntity(),
        )
        entry.messages.forEach { db.messageDao().upsert(it.toEntity()) }
        db.trashDao().delete(chatId)
        return db.chatDao().byId(chatId)?.toDomain()
    }

    suspend fun forgetTrash(chatId: String) = db.trashDao().delete(chatId)

    suspend fun putMemory(memory: Memory) = db.memoryDao().upsert(memory.toEntity())
    suspend fun deleteMemory(id: String) = db.memoryDao().delete(id)
    suspend fun allMemories(): List<Memory> = db.memoryDao().all().map { it.toDomain() }
    suspend fun clearMemories() = db.memoryDao().clear()

    suspend fun schedule(item: ScheduledMessage) = db.scheduledDao().upsert(item.toEntity())
    suspend fun unschedule(id: String) = db.scheduledDao().delete(id)
    suspend fun allScheduled(): List<ScheduledMessage> = db.scheduledDao().all().map { it.toDomain() }

    /** Backup export/import (spec §6.5 footer, #31). */
    suspend fun exportBackup(now: Long): String =
        Backup.export(allChats(), allMessages(), now)

    sealed class RestoreOutcome {
        data class Ok(val chats: Int, val messages: Int) : RestoreOutcome()
        data class Error(val reason: String) : RestoreOutcome()
    }

    /** Restore is a whole-file replace via bulkPut semantics (spec §7.1). */
    suspend fun restoreBackup(text: String): RestoreOutcome {
        return when (val result = Backup.parse(text)) {
            is Backup.RestoreResult.Error -> RestoreOutcome.Error(result.reason)
            is Backup.RestoreResult.Ok -> {
                result.chats.forEach { db.chatDao().upsert(it.toEntity()) }
                result.messages.forEach { db.messageDao().upsert(it.toEntity()) }
                RestoreOutcome.Ok(result.chats.size, result.messages.size)
            }
        }
    }

    /** Settings → Your data: wipe local tables (spec §6.9, #32 keeps the plan). */
    suspend fun wipeAll() {
        db.messageDao().clear()
        db.chatDao().clear()
        db.folderDao().clear()
        db.scheduledDao().clear()
        db.trashDao().clear()
        db.memoryDao().clear()
    }

    suspend fun searchMessages(query: String): List<ChatMessage> =
        db.messageDao().search(query).map { it.toDomain() }
}
