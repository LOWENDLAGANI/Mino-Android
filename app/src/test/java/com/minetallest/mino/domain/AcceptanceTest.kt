package com.minetallest.mino.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Acceptance invariants from spec §19 (variants, search, names, stream, schedule). */
class AcceptanceTest {

    private fun message(
        content: String = "",
        variants: List<String>? = null,
        variantIndex: Int = 0,
        role: Role = Role.ASSISTANT,
        createdAt: Long = 0,
        chatId: String = "c1",
    ) = ChatMessage(
        id = "m$createdAt$variantIndex",
        chatId = chatId,
        role = role,
        content = content,
        variants = variants,
        variantIndex = variantIndex,
        createdAt = createdAt,
    )

    // --- §19.9 variant retirement -----------------------------------------
    @Test
    fun retry_retiresTheOldAnswerBehindAKOverNStepper() {
        val retired = Variants.retire(message(content = "first answer"))
        assertEquals(listOf("first answer"), retired.variants)
        assertEquals("", retired.content)
        assertEquals(0, retired.variantIndex)
        assertTrue(Variants.canStepBack(retired))
        assertFalse(Variants.canStepForward(retired))
    }

    @Test
    fun anEmptyAnswerIsCutNotBranched() {
        val cut = Variants.retire(message(content = "   "))
        assertNull(cut.variants)
        assertEquals(0, cut.variantIndex)
    }

    @Test
    fun variantCapsAtEight_andDisplayedContentFollowsTheIndex() {
        var current = message(content = "v0")
        repeat(12) { current = Variants.retire(current.copy(content = "answer $it")) }
        assertTrue((current.variants?.size ?: 0) <= Variants.MAX_VARIANTS)

        val onScreen = message(
            content = "live",
            variants = listOf("older", "newer"),
            variantIndex = 2,
        )
        assertEquals("newer", onScreen.displayedContent())
        assertEquals("live", onScreen.copy(variantIndex = 0).displayedContent())
    }

    // --- §19.6 Mino names only --------------------------------------------
    @Test
    fun storedAndDisplayedNamesAreOnlyEverMinoNames() {
        assertEquals("Mino", Models.toMinoName("gpt-4o"))
        assertEquals("Mino", Models.toMinoName("claude-3-5-sonnet"))
        assertEquals("Mino", Models.toMinoName(null))
        assertEquals("Mino Azure", Models.toMinoName("Mino Azure"))
        assertEquals("Mino Auto", Models.toMinoName("Mino Auto"))
        assertEquals("Mino V3", Models.toMinoName("Mino V3"))
        Models.modes.forEach { mode ->
            assertTrue(
                "${mode.display} must be a Mino name",
                Regex("""^Mino.*""").containsMatchIn(Models.badgeFor(mode.id)),
            )
        }
        assertEquals("Mino Code", Models.badgeFor(ModeId.CODE))
    }

    // --- §19.34 local search ----------------------------------------------
    @Test
    fun searchIsLocal_displayedVariantNewestFirstAndCapped() {
        val chats = listOf(
            Chat(id = "c1", title = "Rocket notes", createdAt = 0, updatedAt = 100),
            Chat(id = "c2", title = "Groceries", createdAt = 0, updatedAt = 300),
            Chat(id = "c3", title = "Unrelated", createdAt = 0, updatedAt = 200),
        )
        val messages = listOf(
            message(content = "nothing here", createdAt = 10, chatId = "c3"),
            message(content = "I love rockets", createdAt = 5, chatId = "c1"),
            // Displayed variant (index 1 → variants[0]) is what must match.
            message(content = "live text", variants = listOf("hidden rockets answer"), variantIndex = 1, createdAt = 20, chatId = "c2"),
        )

        val hits = LocalSearch.search("rockets", chats, messages)
        assertEquals(2, hits.size)
        // Newest chat/message first: c2 (variant) precedes c1 (title match is prepended by title pass).
        assertTrue(hits.any { it.chat.id == "c2" && it.message != null })
        assertTrue(hits.any { it.chat.id == "c1" })
        // Each chat appears only once.
        assertEquals(hits.size, hits.map { it.chat.id }.distinct().size)
        // Snippet points at the match.
        assertTrue(hits.first { it.chat.id == "c1" }.snippet.contains("rockets"))
    }

    @Test
    fun searchNeedsTwoCharacters_andNeverReturnsSystemMessages() {
        val chats = listOf(Chat(id = "c1", title = "Hello", createdAt = 0, updatedAt = 0))
        assertTrue(LocalSearch.search("x", chats, emptyList()).isEmpty())
        assertTrue(LocalSearch.search("", chats, emptyList()).isEmpty())
        val system = listOf(message(role = Role.SYSTEM, content = "secret system prompt about bananas", createdAt = 1))
        assertTrue(LocalSearch.search("bananas", chats, system).isEmpty())
    }

    @Test
    fun searchCapsAtThirtyResults() {
        val chats = (1..40).map { Chat(id = "c$it", title = "potions $it", createdAt = 0, updatedAt = it.toLong()) }
        val hits = LocalSearch.search("potions", chats, emptyList())
        assertEquals(LocalSearch.LIMIT, hits.size)
        // Newest first: the highest updatedAt comes back first.
        assertEquals("c40", hits.first().chat.id)
    }

    // --- §9.1 SSE ----------------------------------------------------------
    @Test
    fun streamParsesContentModelUsageSearchAndDone() {
        assertEquals(ChatEvent.Content("Hel"), SseEvents.parsePayload("""{"content":"Hel"}"""))
        assertEquals(ChatEvent.Done, SseEvents.parsePayload("[DONE]"))
        assertTrue(SseEvents.parsePayload("""{"truncated":true}""") is ChatEvent.Truncated)

        val model = SseEvents.parsePayload("""{"mode":"auto","provider":"x","model":"Mino Auto"}""")
        assertEquals("Mino Auto", (model as ChatEvent.Model).model)

        val usage = SseEvents.parsePayload("""{"usage":{"prompt":10,"completion":5,"total":15}}""")
        assertEquals(10, (usage as ChatEvent.Usage).prompt)
        assertEquals(5, usage.completion)
        assertEquals(15, usage.total)

        val error = SseEvents.parsePayload("""{"error":"Too many messages at once. Please wait 12s."}""")
        assertEquals("Too many messages at once. Please wait 12s.", (error as ChatEvent.Error).message)

        val search = SseEvents.parsePayload(
            """{"search":{"used":true,"query":"comet","sources":[{"id":"1","title":"t","url":"u","snippet":"s"}]}}""",
        )
        assertTrue(search is ChatEvent.Search)
        assertEquals("comet", (search as ChatEvent.Search).query)
        assertEquals(1, search.sources.size)
        assertNull(SseEvents.parsePayload("not json"))
    }

    @Test
    fun assemblerIsChunkSafe_splitPayloadsStillArriveInOrder() {
        val assembler = SseAssembler()
        assertTrue(assembler.feed("data: {\"conte").isEmpty())
        val first = assembler.feed("nt\":\"a\"}\ndata: {\"content\":\"b\"}\n")
        assertEquals(listOf("""{"content":"a"}""", """{"content":"b"}"""), first)
        assertEquals(listOf("[DONE]"), assembler.feed("data: [DONE]\r\n"))
    }

    // --- §10.3 schedule ----------------------------------------------------
    @Test
    fun schedulePresetsAndChipLabelsMatchTheSpec() {
        val now = Schedule.inMinutes(System.currentTimeMillis(), 0) + 60_000L
        assertEquals(listOf("In 5 minutes", "In 30 minutes", "In 1 hour", "Tomorrow 9:00"), Schedule.presets.map { it.label })

        val five = Schedule.presets[0].sendAt(now)
        assertEquals("in 5m", Schedule.chipLabel(now, five))
        assertEquals("due now", Schedule.chipLabel(now + 10_000, now))
        assertEquals("due now", Schedule.chipLabel(now, now - 10_000))

        val tomorrow = Schedule.presets[3].sendAt(now)
        assertFalse(Schedule.isToday(tomorrow))
        assertTrue(Schedule.chipLabel(now, tomorrow).contains(","))

        assertEquals(60_000L, Schedule.floor(now) - now)
        assertTrue(Schedule.inMinutes(now, 30) > now)
    }

    // --- headlines ---------------------------------------------------------
    @Test
    fun headlinesRotateWithoutSticking() {
        val (nextIndex, first) = Headlines.next(0)
        assertTrue(nextIndex in Headlines.all.indices)
        assertFalse(first.isBlank())
        // The cursor is persisted *after* display, so walk it forward one step at a time.
        val seen = mutableListOf<String>()
        var cursor = 0
        repeat(Headlines.all.size) {
            val (i, text) = Headlines.next(cursor)
            seen.add(text)
            cursor = i + 1
        }
        assertEquals(Headlines.all.size, seen.distinct().size)
        assertEquals(Headlines.all, seen)
    }
}
