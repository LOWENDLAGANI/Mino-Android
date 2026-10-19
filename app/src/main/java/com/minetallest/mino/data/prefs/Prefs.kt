package com.minetallest.mino.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.minetallest.mino.domain.ModeId
import com.minetallest.mino.domain.ReasoningEffort
import com.minetallest.mino.domain.ResponseLength
import com.minetallest.mino.domain.SearchMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mino-prefs")

/**
 * Port of `localStorage` (spec §7.2) — the exact key names are kept.
 * Android-only addition: `mino:api-base` (the web build used its own origin).
 */
class Prefs(private val context: Context) {

    object Keys {
        const val SELECTED_MODE = "mino:selected-mode"
        const val DISPLAY_NAME = "mino:display-name"
        const val RESPONSE_LENGTH = "mino:response-length"
        const val REASONING_EFFORT = "mino:reasoning-effort"
        const val MEMORY_SUGGEST = "mino:memory-suggest"
        const val MEMORY_SUGGEST_OFF = "mino:memory-suggest-off"
        const val SUBSCRIPTION_ACK = "mino:subscription-ack"
        const val DELETED_CHATS = "mino:deleted-chats"
        const val FIRST_SEEN = "mino:first-seen"
        const val NOTES_SEEN = "mino:notes-seen"
        const val SEARCH_MODE = "mino:search-mode"
        const val API_BASE = "mino:api-base"
        const val DEW_TRIAL = "mino:dew-trial"
        const val DARK_THEME = "mino:dark-theme"
    }

    private val ds = context.dataStore

    val displayName: Flow<String?> = ds.data.map { it[stringPreferencesKey(Keys.DISPLAY_NAME)] }
    val selectedMode: Flow<ModeId> = ds.data.map { ModeId.fromStored(it[stringPreferencesKey(Keys.SELECTED_MODE)]) }
    val responseLength: Flow<ResponseLength> =
        ds.data.map { ResponseLength.fromWire(it[stringPreferencesKey(Keys.RESPONSE_LENGTH)]) }
    val reasoningEffort: Flow<ReasoningEffort> =
        ds.data.map { ReasoningEffort.fromWire(it[stringPreferencesKey(Keys.REASONING_EFFORT)]) }
    val searchMode: Flow<SearchMode> =
        ds.data.map { SearchMode.fromWire(it[stringPreferencesKey(Keys.SEARCH_MODE)]) }
    val memorySuggest: Flow<Boolean> =
        ds.data.map { it[booleanPreferencesKey(Keys.MEMORY_SUGGEST)] ?: true }
    val subscriptionAck: Flow<Long> = ds.data.map { it[longPreferencesKey(Keys.SUBSCRIPTION_ACK)] ?: 0L }
    val firstSeen: Flow<Long?> = ds.data.map { it[longPreferencesKey(Keys.FIRST_SEEN)] }
    val notesSeen: Flow<List<String>> = ds.data.map { raw ->
        raw[stringPreferencesKey(Keys.NOTES_SEEN)]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
    val apiBase: Flow<String> = ds.data.map { it[stringPreferencesKey(Keys.API_BASE)] ?: BuildConfigDefaults.API_BASE_URL }

    suspend fun setDisplayName(name: String) {
        ds.edit { it[stringPreferencesKey(Keys.DISPLAY_NAME)] = name }
    }

    suspend fun setSelectedMode(mode: ModeId) {
        ds.edit { it[stringPreferencesKey(Keys.SELECTED_MODE)] = mode.wire }
    }

    suspend fun setResponseLength(value: ResponseLength) {
        ds.edit { it[stringPreferencesKey(Keys.RESPONSE_LENGTH)] = value.wire }
    }

    suspend fun setReasoningEffort(value: ReasoningEffort) {
        ds.edit { it[stringPreferencesKey(Keys.REASONING_EFFORT)] = value.wire }
    }

    suspend fun setSearchMode(value: SearchMode) {
        ds.edit { it[stringPreferencesKey(Keys.SEARCH_MODE)] = value.wire }
    }

    suspend fun setMemorySuggest(enabled: Boolean) {
        ds.edit { it[booleanPreferencesKey(Keys.MEMORY_SUGGEST)] = enabled }
    }

    suspend fun setMemorySuggestOff(flag: Boolean) {
        ds.edit { if (flag) it[booleanPreferencesKey(Keys.MEMORY_SUGGEST_OFF)] = true
                  else it.remove(booleanPreferencesKey(Keys.MEMORY_SUGGEST_OFF)) }
    }

    suspend fun setSubscriptionAck(id: Long) {
        ds.edit { it[longPreferencesKey(Keys.SUBSCRIPTION_ACK)] = id }
    }

    suspend fun setFirstSeen(value: Long) {
        ds.edit { it[longPreferencesKey(Keys.FIRST_SEEN)] = value }
    }

    suspend fun markNotesSeen(ids: List<String>) {
        ds.edit { it[stringPreferencesKey(Keys.NOTES_SEEN)] = ids.joinToString(",") }
    }

    val dewTrialClaimed: Flow<Boolean> = ds.data.map { it[booleanPreferencesKey(Keys.DEW_TRIAL)] ?: false }

    suspend fun setDewTrialClaimed() {
        ds.edit { it[booleanPreferencesKey(Keys.DEW_TRIAL)] = true }
    }

    val darkTheme: Flow<Boolean> = ds.data.map { it[booleanPreferencesKey(Keys.DARK_THEME)] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        ds.edit { it[booleanPreferencesKey(Keys.DARK_THEME)] = enabled }
    }

    suspend fun setApiBase(value: String) {
        ds.edit {
            if (value.isBlank()) it.remove(stringPreferencesKey(Keys.API_BASE))
            else it[stringPreferencesKey(Keys.API_BASE)] = value.trim().trimEnd('/')
        }
    }

    suspend fun currentApiBase(): String = apiBase.first()
}

/** BuildConfig passthrough so the domain-adjacent layer stays free of Android imports in tests. */
object BuildConfigDefaults {
    var API_BASE_URL: String = com.minetallest.mino.BuildConfig.API_BASE_URL
}
