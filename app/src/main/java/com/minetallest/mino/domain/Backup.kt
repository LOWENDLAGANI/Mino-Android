package com.minetallest.mino.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Backup export/import with the Dexie bounds and exact failure sentences (spec §7.1, §10.4, #31). */
object Backup {

    const val MAX_CHATS = 2000
    const val MAX_MESSAGES = 50_000

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun export(chats: List<Chat>, messages: List<ChatMessage>, now: Long): String {
        val iso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(now))
        return json.encodeToString(
            MinoBackup.serializer(),
            MinoBackup(app = "mino", version = 1, exportedAt = iso, chats = chats, messages = messages),
        )
    }

    sealed class RestoreResult {
        data class Ok(val chats: List<Chat>, val messages: List<ChatMessage>) : RestoreResult()
        data class Error(val reason: String) : RestoreResult()
    }

    fun parse(text: String): RestoreResult {
        val backup = try {
            json.decodeFromString(MinoBackup.serializer(), text)
        } catch (e: SerializationException) {
            return RestoreResult.Error("Invalid Mino backup file")
        } catch (e: IllegalArgumentException) {
            return RestoreResult.Error("Invalid Mino backup file")
        }
        if (backup.app != "mino") return RestoreResult.Error("Invalid Mino backup file")
        if (backup.chats.size > MAX_CHATS || backup.messages.size > MAX_MESSAGES) {
            return RestoreResult.Error("That backup is too large to restore")
        }
        return RestoreResult.Ok(backup.chats, backup.messages)
    }

    fun exportFileName(now: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return "mino-backup-%04d-%02d-%02d.json".format(y, m, d)
    }
}
