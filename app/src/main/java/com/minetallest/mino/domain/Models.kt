package com.minetallest.mino.domain

/** Client-safe mode catalog — port of `lib/models.ts` (spec §8.1). */
data class ModeInfo(
    val id: ModeId,
    val name: String,
    val display: String,
    val tagline: String,
    val blurb: String,
    val bullets: List<String>,
    val separateSession: Boolean,
    /** ARGB badge/dot colour. */
    val color: Long,
    /** Roadmap/plans blurb used by the paywall copy. */
    val plan: String? = null,
)

object Models {
    const val DEFAULT_MODE_ID: String = "auto"
    const val IMAGE_ENGINE: String = "mino-canvas"

    /** Spec §8.2 — the only names a client may ever show or store. */
    private val MINO_NAME = Regex("""^Mino(?: Auto| Self| Azure| Canvas| V\d+)?$""")

    val modes: List<ModeInfo> = listOf(
        ModeInfo(
            id = ModeId.AUTO,
            name = "Auto",
            display = "Mino Auto",
            tagline = "Best for everyday tasks",
            blurb = "Best available model",
            bullets = listOf(
                "Smart model selection",
                "Balanced speed & quality",
                "Great for general questions",
            ),
            separateSession = false,
            color = 0xFF6EC294,
        ),
        ModeInfo(
            id = ModeId.CODE,
            name = "Code",
            display = "Code",
            tagline = "Built for developers",
            blurb = "Mino V3 · V2 · V1 only",
            bullets = listOf(
                "File-aware code blocks",
                "Technical precision",
                "Fresh session per task",
            ),
            separateSession = true,
            color = 0xFFE08E6B,
        ),
        ModeInfo(
            id = ModeId.SELF,
            name = "Azure",
            display = "Mino Azure",
            tagline = "Mino's own model",
            blurb = "Mino's own model",
            bullets = listOf(
                "Independent AI assistant",
                "Created by Minetallest",
                "Exclusive to Mino",
            ),
            separateSession = true,
            color = 0xFF5B9BD5,
        ),
    )

    fun info(id: ModeId): ModeInfo = modes.firstOrNull { it.id == id } ?: modes.first()

    /** In-message badge text: Mino Auto / Mino Code / Mino Azure (spec §6.6). */
    fun badgeFor(id: ModeId): String = when (id) {
        ModeId.AUTO -> "Mino Auto"
        ModeId.CODE -> "Mino Code"
        ModeId.SELF -> "Mino Azure"
    }

    /** Bridge for stored/model values — anything unrecognised becomes plain `Mino` (spec §8.2). */
    fun toMinoName(raw: String?): String =
        if (raw != null && MINO_NAME.matches(raw)) raw else "Mino"

    /** What actually renders / lands in backups. */
    fun getModelDisplayName(stored: String?): String = toMinoName(stored)
}
