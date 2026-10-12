package com.minetallest.mino.domain

import java.util.Calendar

/** Send-later presets and chip labels (spec §10.3). */
object Schedule {

    fun inMinutes(now: Long, minutes: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis + minutes * 60_000L
    }

    fun tomorrowAt(now: Long, hour: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** Floor for the custom picker: now + 60 s (spec §6.7). */
    fun floor(now: Long): Long = now + 60_000L

    data class Preset(val label: String, val sendAt: (Long) -> Long)

    val presets: List<Preset> = listOf(
        Preset("In 5 minutes") { inMinutes(it, 5) },
        Preset("In 30 minutes") { inMinutes(it, 30) },
        Preset("In 1 hour") { inMinutes(it, 60) },
        Preset("Tomorrow 9:00") { tomorrowAt(it, 9) },
    )

    /** Chip label: `due now` / `in {n}m` / `today {h:mm}` / `MMM d, h:mm`. */
    fun chipLabel(now: Long, sendAt: Long): String {
        val delta = sendAt - now
        if (delta <= 0) return "due now"
        val minutes = delta / 60_000L
        if (minutes < 60 && isToday(sendAt)) return "in ${minutes}m"
        if (isToday(sendAt)) return "today ${Rules.clockTime(sendAt)}"
        return "${Rules.monthDay(sendAt)}, ${Rules.clockTime(sendAt)}"
    }

    fun isToday(then: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = then }
        val b = Calendar.getInstance()
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
