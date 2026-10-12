package com.minetallest.mino.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §7.1 derived rules and §4.6 refusal sentences. */
class RulesTest {

    @Test
    fun autoTitle_collapsesWhitespace_and_capsAt42() {
        assertEquals("hello   world".let { "hello world" }, Rules.autoTitle("  hello \n  world "))
        val long = "a".repeat(60)
        val title = Rules.autoTitle(long)
        assertEquals(Rules.TITLE_MAX + 1, title.length)
        assertTrue(title.endsWith("…"))
        assertEquals(Rules.TITLE_MAX, title.dropLast(1).length)
    }

    @Test
    fun autoTitle_blankBecomesNewChat() = assertEquals("New chat", Rules.autoTitle("   \n  "))

    @Test
    fun displayName_isTrimmedCappedAndNeverTheRawOverflow() {
        assertEquals("Ada Lovelace", Rules.normalizeDisplayName("  Ada   Lovelace  "))
        assertEquals("Ada", Rules.normalizeDisplayName("   Ada   "))
        assertEquals(Rules.NAME_MAX, Rules.normalizeDisplayName("x".repeat(200)).length)
    }

    @Test
    fun folderName_emptyIsNull_andClashIsCaseInsensitive() {
        assertNull(Rules.normalizeFolderName("    "))
        assertEquals("Work stuff", Rules.normalizeFolderName("  Work \n stuff "))
        assertEquals(Rules.FOLDER_NAME_MAX, Rules.normalizeFolderName("y".repeat(90))!!.length)
        assertTrue(Rules.folderNameClash("Work", "  work "))
        assertFalse(Rules.folderNameClash("Work", "Play"))
    }

    @Test
    fun timeAgo_followsTheSpecLadder() {
        val now = 1_700_000_000_000L
        assertEquals("now", Rules.timeAgo(now, now - 5_000))
        assertEquals("now", Rules.timeAgo(now, now))
        assertEquals("5m", Rules.timeAgo(now, now - 5 * 60_000L))
        assertEquals("59m", Rules.timeAgo(now, now - 59 * 60_000L))
        assertEquals("3h", Rules.timeAgo(now, now - 3 * 3_600_000L))
        assertEquals("4d", Rules.timeAgo(now, now - 4 * 86_400_000L))
        // ≥7 days falls back to "MMM d"
        val older = Rules.timeAgo(now, now - 10 * 86_400_000L)
        assertTrue("expected a month-day label, got $older", older.matches(Regex("""^[A-Z][a-z]{2} \d{1,2}$""")))
    }

    @Test
    fun uid_isUniqueAndNonEmpty() {
        assertNotEquals(Rules.uid(), Rules.uid())
        assertTrue(Rules.uid().isNotBlank())
    }

    @Test
    fun rateLimitSentence_isVerbatimWithTheNamedSeconds() {
        assertEquals(
            "Too many messages at once. Please wait 12s and try again.",
            Refusals.rateLimit(12),
        )
        assertEquals(
            "Too many images at once. Please wait 7s and try again.",
            Refusals.imageRateLimit(7),
        )
        assertEquals(
            "Mino's daily limit of 40 messages has been reached on this device. It resets tomorrow.",
            Refusals.dailyChatCap(40),
        )
    }

    @Test
    fun banMessage_showsReasonAndEndDate_toTheVisitor() {
        assertEquals("You are banned from using Mino.", Refusals.banMessage("", null))
        assertTrue(Refusals.banMessage("spam", null).startsWith("You are banned: spam"))
        val dated = Refusals.banMessage("spam", 1_700_000_000_000L)
        assertTrue(dated.contains("(until "))
        assertFalse(Refusals.banMessage("x", null).contains("(until "))
    }

    @Test
    fun rateLimitCountdown_onlyStartsWhenTheServerNamedANumber() {
        assertEquals(12, Refusals.parseWaitSeconds("Please wait 12s and try again."))
        assertEquals(12, Refusals.parseWaitSeconds("Too many messages at once. Please wait 12s."))
        assertEquals(30, Refusals.parseWaitSeconds("Retry after 30 s"))
        assertNull(Refusals.parseWaitSeconds("Something went wrong."))
        assertNull(Refusals.parseWaitSeconds(null))
        assertNull(Refusals.parseWaitSeconds("   "))
        assertNull(Refusals.parseWaitSeconds("wait 0s"))
    }
}
