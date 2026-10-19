package com.minetallest.mino.domain

import java.util.Locale
import kotlin.math.abs

/** Spec §7.1 derived rules: auto-title, relative times, refusal sentences. */
object Rules {

    const val NAME_MAX = 40
    const val TITLE_MAX = 42

    /** `uid()` — crypto.randomUUID with timestamp+random fallback. */
    fun uid(): String =
        try {
            java.util.UUID.randomUUID().toString()
        } catch (e: Throwable) {
            val t = System.currentTimeMillis().toString(36)
            val r = (0 until 10).joinToString("") { ("abcdefghijklmnopqrstuvwxyz0123456789").random().toString() }
            "$t-$r"
        }

    /** Auto-title: first user message, whitespace-collapsed, 42 chars + ellipsis (spec §7.1). */
    fun autoTitle(firstUserText: String): String {
        val collapsed = firstUserText.trim().replace(Regex("\\s+"), " ")
        if (collapsed.isEmpty()) return "New chat"
        return if (collapsed.length <= TITLE_MAX) collapsed else collapsed.take(TITLE_MAX) + "…"
    }

    /** Display name: trimmed, collapsed, max 40, never empty. */
    fun normalizeDisplayName(raw: String): String {
        val collapsed = raw.replace(Regex("\\s+"), " ").trim()
        return if (collapsed.length <= NAME_MAX) collapsed else collapsed.take(NAME_MAX)
    }

    /** Sidebar relative time: now · 5m · 3h · 4d · MMM d (spec §6.4). */
    fun timeAgo(now: Long, then: Long): String {
        val delta = now - then
        if (delta < 60_000L) return "now"
        val minutes = delta / 60_000L
        if (minutes < 60) return "${minutes}m"
        val hours = delta / 3_600_000L
        if (hours < 24) return "${hours}h"
        val days = delta / 86_400_000L
        if (days < 7) return "${days}d"
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = then }
        val month = cal.get(java.util.Calendar.MONTH)
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return "${MONTHS[month]} $day"
    }

    private val MONTHS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    fun monthDay(then: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = then }
        return "${MONTHS[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
    }

    fun clockTime(then: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = then }
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        return String.format(Locale.US, "%d:%02d", h, m)
    }
}

/** Spec §4.6 / §9.1 — refusal sentences, verbatim. */
object Refusals {
    fun rateLimit(seconds: Int): String =
        "Too many messages at once. Please wait ${seconds}s and try again."

    fun imageRateLimit(seconds: Int): String =
        "Too many images at once. Please wait ${seconds}s and try again."

    fun dailyChatCap(n: Int): String =
        "Mino's daily limit of $n messages has been reached on this device. It resets tomorrow."

    const val KILL_SWITCH = "Mino is paused right now. Please try again shortly."

    const val PAUSED_PLAN = "Plan on hold"

    const val EMPTY_RESPONSE = "Mino returned an empty response. Try again."

    const val MODEL_CHANGED = "Model switched"

    const val TRUNCATED_NOTICE = "Answer shortened"

    const val SEARCH_NO_SOURCE = "No sources found"

    const val VOICE_UNSUPPORTED = "Voice input is unavailable"
    const val VOICE_FAILED = "Voice input failed"

    const val MEMORY_TEXT_LIMIT = 200

    fun banMessage(reason: String, expiresAt: Long?): String {
        val base = if (reason.isBlank()) "You are banned from using Mino." else "You are banned: $reason"
        return if (expiresAt == null) base
        else "$base (until ${Rules.monthDay(expiresAt)} ${Rules.clockTime(expiresAt)})"
    }

    /**
     * Spec §4.6 rate-limit contract: parse the integer the server named → live countdown.
     * A message with no number starts no countdown.
     */
    fun parseWaitSeconds(message: String?): Int? {
        if (message.isNullOrBlank()) return null
        val match = Regex("""wait\s+(\d+)s""", RegexOption.IGNORE_CASE).find(message)
            ?: Regex("""(\d+)\s*s\b""").find(message)
            ?: return null
        return match.groupValues[1].toIntOrNull()?.takeIf { it > 0 }
    }
}
