package com.minetallest.mino.domain

/** Palette search over the *displayed* variant, newest-first, ≤30, radius 46 (spec §10.6 / §7.1). */
object LocalSearch {

    const val MIN_QUERY = 2
    const val LIMIT = 30
    const val SNIPPET_RADIUS = 46

    data class Hit(
        val chat: Chat,
        val message: ChatMessage?,
        val snippet: String,
    )

    fun snippetAround(text: String, query: String): String {
        val idx = text.indexOf(query, ignoreCase = true)
        if (idx < 0) return text.take(SNIPPET_RADIUS * 2) + if (text.length > SNIPPET_RADIUS * 2) "…" else ""
        val start = maxOf(0, idx - SNIPPET_RADIUS)
        val end = minOf(text.length, idx + query.length + SNIPPET_RADIUS)
        return buildString {
            if (start > 0) append('…')
            append(text.substring(start, end))
            if (end < text.length) append('…')
        }
    }

    /**
     * @param chatsByChatId chat lookup for titles/order
     * @param messages all messages of this device (scanned in memory, deliberately local)
     */
    fun search(
        query: String,
        chats: List<Chat>,
        messages: List<ChatMessage>,
    ): List<Hit> {
        val q = query.trim()
        if (q.length < MIN_QUERY) return emptyList()
        val chatsById = chats.associateBy { it.id }
        val hits = ArrayList<Hit>()

        // Titles first, newest chat first.
        chats.sortedWith(compareByDescending<Chat> { it.pinned }.thenByDescending { it.updatedAt })
            .forEach { chat ->
                if (chat.title.contains(q, ignoreCase = true)) {
                    hits.add(Hit(chat, null, chat.title))
                }
            }

        val byNewest = messages.sortedByDescending { it.createdAt }
        val seenChats = hits.map { it.chat.id }.toMutableSet()
        for (message in byNewest) {
            if (hits.size >= LIMIT) break
            if (message.role == Role.SYSTEM) continue
            val displayed = message.displayedContent()
            if (!displayed.contains(q, ignoreCase = true)) continue
            val chat = chatsById[message.chatId] ?: continue
            if (chat.id in seenChats) continue
            seenChats.add(chat.id)
            hits.add(Hit(chat, message, snippetAround(displayed, q)))
        }
        return hits.take(LIMIT)
    }
}
