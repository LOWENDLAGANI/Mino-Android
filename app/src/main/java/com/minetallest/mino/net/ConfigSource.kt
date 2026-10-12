package com.minetallest.mino.net

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.minetallest.mino.BuildConfig
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Public runtime config (spec §4.4): `config/` is a public-read RTDB node.
 * Defaults are permissive — unreadable config never breaks the app (spec §16).
 */
data class AppConfig(
    val chatEnabled: Boolean = true,
    val imageEnabled: Boolean = true,
    val searchEnabled: Boolean = true,
    val googleToolsEnabled: Boolean = true,
    val announcement: String = "",
    val notesTitle: String = "News From Developers",
    val notes: List<DevNote> = emptyList(),
    val dailyChatCap: Int = 0,
    val dailyImageCap: Int = 0,
    val maintenanceEnabled: Boolean = false,
    val maintenanceMessage: String = "Mino is down for maintenance. Please check back soon.",
    val updatedAt: Long = 0,
) {
    companion object {
        val DEFAULT = AppConfig()

        /** Parses the `config` object served by `GET /api/admin/config` (spec §9.6). */
        fun parseStatic(obj: kotlinx.serialization.json.JsonObject): AppConfig {
            fun bool(key: String, default: Boolean) =
                obj[key]?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content?.toBoolean()
                    ?: default
            fun str(key: String, default: String) =
                (obj[key] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    ?.takeIf { it.isNotBlank() } ?: default
            val notes = (obj["notes"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { item ->
                val o = item as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                DevNote(
                    id = (o["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                        ?: return@mapNotNull null,
                    badge = (o["badge"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                    tone = (o["tone"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "neutral",
                    byline = (o["byline"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                    date = (o["date"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                    summary = (o["summary"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                    body = (o["body"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                )
            } ?: emptyList()
            return AppConfig(
                chatEnabled = bool("chatEnabled", true),
                imageEnabled = bool("imageEnabled", true),
                searchEnabled = bool("searchEnabled", true),
                googleToolsEnabled = bool("googleToolsEnabled", true),
                announcement = (obj["announcement"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    ?.take(200) ?: "",
                notesTitle = str("notesTitle", "News From Developers"),
                notes = notes,
                dailyChatCap = (obj["dailyChatCap"] as? kotlinx.serialization.json.JsonPrimitive)
                    ?.content?.toIntOrNull() ?: 0,
                dailyImageCap = (obj["dailyImageCap"] as? kotlinx.serialization.json.JsonPrimitive)
                    ?.content?.toIntOrNull() ?: 0,
                maintenanceEnabled = bool("maintenanceEnabled", false),
                maintenanceMessage = str(
                    "maintenanceMessage",
                    "Mino is down for maintenance. Please check back soon.",
                ).take(300),
                updatedAt = (obj["updatedAt"] as? kotlinx.serialization.json.JsonPrimitive)
                    ?.content?.toLongOrNull() ?: 0L,
            )
        }
    }

    /** Serialises this config for `PUT /api/admin/config` (spec §9.6). */
    fun toJson(): kotlinx.serialization.json.JsonObject =
        kotlinx.serialization.json.buildJsonObject {
            put("chatEnabled", chatEnabled)
            put("imageEnabled", imageEnabled)
            put("searchEnabled", searchEnabled)
            put("googleToolsEnabled", googleToolsEnabled)
            put("announcement", announcement)
            put("notesTitle", notesTitle)
            put("dailyChatCap", dailyChatCap)
            put("dailyImageCap", dailyImageCap)
            put("maintenanceEnabled", maintenanceEnabled)
            put("maintenanceMessage", maintenanceMessage)
            put("updatedAt", updatedAt)
            put(
                "notes",
                kotlinx.serialization.json.buildJsonArray {
                    notes.forEach { n ->
                        add(
                            kotlinx.serialization.json.buildJsonObject {
                                put("id", n.id)
                                put("badge", n.badge)
                                put("tone", n.tone)
                                put("byline", n.byline)
                                put("date", n.date)
                                put("summary", n.summary)
                                put("body", n.body)
                            },
                        )
                    }
                },
            )
        }

}

data class DevNote(
    val id: String,
    val badge: String = "",
    val tone: String = "neutral",
    val byline: String = "",
    val date: String = "",
    val summary: String = "",
    val body: String = "",
)

class ConfigSource(private val context: Context) {

    private val database: FirebaseDatabase? by lazy {
        if (BuildConfig.FIREBASE_API_KEY.isBlank()) return@lazy null
        try {
            val app = try {
                FirebaseApp.getInstance()
            } catch (e: IllegalStateException) {
                FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder()
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                        .setDatabaseUrl(BuildConfig.FIREBASE_DATABASE_URL)
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .build(),
                )
            } ?: return@lazy null
            FirebaseDatabase.getInstance(app)
        } catch (e: Throwable) {
            null
        }
    }

    /** Best-effort read; null keeps permissive defaults. */
    suspend fun read(): AppConfig? {
        val db = database ?: return null
        return try {
            val snapshot = db.getReference("config").readSnapshot()
            parse(snapshot)
        } catch (e: Throwable) {
            null
        }
    }

    private fun parse(snapshot: DataSnapshot?): AppConfig? {
        val value = snapshot?.value as? Map<*, *> ?: return null
        fun bool(key: String, default: Boolean) =
            (value[key] as? Boolean) ?: default
        fun str(key: String, default: String) =
            (value[key] as? String)?.takeIf { it.isNotBlank() } ?: default
        val notesRaw = value["notes"] as? List<*> ?: emptyList<Any?>()
        val notes = notesRaw.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            DevNote(
                id = (map["id"] as? String) ?: return@mapNotNull null,
                badge = (map["badge"] as? String) ?: "",
                tone = (map["tone"] as? String) ?: "neutral",
                byline = (map["byline"] as? String) ?: "",
                date = (map["date"] as? String) ?: "",
                summary = (map["summary"] as? String) ?: "",
                body = (map["body"] as? String) ?: "",
            )
        }
        return AppConfig(
            chatEnabled = bool("chatEnabled", true),
            imageEnabled = bool("imageEnabled", true),
            searchEnabled = bool("searchEnabled", true),
            googleToolsEnabled = bool("googleToolsEnabled", true),
            announcement = (value["announcement"] as? String)?.take(200) ?: "",
            notesTitle = str("notesTitle", "News From Developers"),
            notes = notes,
            dailyChatCap = (value["dailyChatCap"] as? Number)?.toInt() ?: 0,
            dailyImageCap = (value["dailyImageCap"] as? Number)?.toInt() ?: 0,
            maintenanceEnabled = bool("maintenanceEnabled", false),
            maintenanceMessage = str(
                "maintenanceMessage",
                "Mino is down for maintenance. Please check back soon.",
            ).take(300),
            updatedAt = (value["updatedAt"] as? Number)?.toLong() ?: 0L,
        )
    }
}

private suspend fun com.google.firebase.database.DatabaseReference.readSnapshot(): DataSnapshot? =
    suspendCancellableCoroutine { continuation ->
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                continuation.resume(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resume(null)
            }
        }
        addListenerForSingleValueEvent(listener)
        continuation.invokeOnCancellation { removeEventListener(listener) }
    }
