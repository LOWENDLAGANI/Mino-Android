package com.minetallest.mino.domain

/** Answer-variant retirement (spec §6.6 / §7.1): retry/edit retire the live answer into variants. */
object Variants {
    const val MAX_VARIANTS = 8
    const val TRASH_RETENTION_MS: Long = 30L * 86_400_000L

    /**
     * Retire the current live answer so the row can be reused.
     * Only text moves — sources, usage, truncation and error stay with the row.
     * An empty answer is cut, not branched.
     */
    fun retire(message: ChatMessage): ChatMessage {
        val live = message.content
        if (live.isBlank()) {
            return message.copy(variants = null, variantIndex = 0)
        }
        val next = listOf(live) + (message.variants ?: emptyList())
        return message.copy(
            variants = next.take(MAX_VARIANTS),
            variantIndex = 0,
            content = "",
        )
    }

    fun canStepBack(message: ChatMessage): Boolean =
        message.variantIndex < (message.variants?.size ?: 0)

    fun canStepForward(message: ChatMessage): Boolean = message.variantIndex > 0
}
