package com.minetallest.mino.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §19.31 — backup export/import round-trips; over-limit files are refused verbatim. */
class BackupTest {

    private val chats = listOf(
        Chat(id = "c1", title = "Rocket notes", createdAt = 10, updatedAt = 20, pinned = true, folder = "f1"),
        Chat(id = "c2", title = "Groceries", createdAt = 11, updatedAt = 21),
    )
    private val messages = listOf(
        ChatMessage(
            id = "m1", chatId = "c1", role = Role.USER, content = "hello",
            createdAt = 12, images = null,
        ),
        ChatMessage(
            id = "m2", chatId = "c1", role = Role.ASSISTANT, content = "hi — Mino here",
            model = "Mino Auto", variants = listOf("earlier answer"), variantIndex = 1,
            usage = TokenUsage(prompt = 10, completion = 5, total = 15), createdAt = 13,
        ),
    )

    @Test
    fun roundTrip_preservesEverythingIncludingVariantsAndUsage() {
        val exported = Backup.export(chats, messages, 1_700_000_000_000L)
        assertTrue(exported.contains("\"app\":\"mino\""))
        // Acceptance #6: no vendor model id may appear in a backup.
        assertTrue(exported.contains("Mino Auto"))

        val restored = Backup.parse(exported)
        assertTrue("restore failed: $restored", restored is Backup.RestoreResult.Ok)
        restored as Backup.RestoreResult.Ok
        assertEquals(chats, restored.chats)
        assertEquals(messages, restored.messages)
        assertEquals(listOf("earlier answer"), restored.messages[1].variants)
        assertEquals(1, restored.messages[1].variantIndex)
        assertEquals(15, restored.messages[1].usage?.total)
        assertEquals(true, restored.chats[0].pinned)
        assertEquals("f1", restored.chats[0].folder)
    }

    @Test
    fun garbageIsRefusedWithTheExactSentence() {
        assertEquals("Invalid Mino backup file", reason("not json at all"))
        assertEquals("Invalid Mino backup file", reason("""{"app":"other","chats":[],"messages":[]}"""))
        assertEquals("Invalid Mino backup file", reason(""))
    }

    @Test
    fun overLimitBackupsAreRefusedWithTheExactSentence() {
        val huge = Backup.export(
            chats = emptyList(),
            messages = (0 until (Backup.MAX_MESSAGES + 1)).map {
                ChatMessage(id = "m$it", chatId = "c", role = Role.USER, content = "x", createdAt = it.toLong())
            },
            now = 0,
        )
        assertEquals("That backup is too large to restore", reason(huge))
    }

    @Test
    fun exportFileNameIsDatedAndSortable() {
        val name = Backup.exportFileName(1_700_000_000_000L)
        assertTrue(name, name.matches(Regex("""mino-backup-\d{4}-\d{2}-\d{2}\.json""")))
    }

    private fun reason(text: String): String? =
        (Backup.parse(text) as? Backup.RestoreResult.Error)?.reason
}
