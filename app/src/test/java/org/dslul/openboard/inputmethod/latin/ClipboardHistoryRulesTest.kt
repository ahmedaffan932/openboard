package org.dslul.openboard.inputmethod.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardHistoryRulesTest {

    private fun texts(entries: List<ClipboardHistoryEntry>) = entries.map { it.content.toString() }

    @Test
    fun add_keepsNewestFirst() {
        val entries = mutableListOf<ClipboardHistoryEntry>()
        ClipboardHistoryRules.add(entries, "first", 1)
        ClipboardHistoryRules.add(entries, "second", 2)
        assertEquals(listOf("second", "first"), texts(entries))
    }

    @Test
    fun add_keepsOnlyLastTenUnpinned() {
        val entries = mutableListOf<ClipboardHistoryEntry>()
        for (i in 1..12) ClipboardHistoryRules.add(entries, "clip $i", i.toLong())
        assertEquals(10, entries.size)
        assertEquals("clip 12", texts(entries).first())
        assertEquals("clip 3", texts(entries).last())
    }

    @Test
    fun add_pinnedClipsDoNotCountTowardsTheTen() {
        val entries = mutableListOf(ClipboardHistoryEntry(1, "pinned", isPinned = true))
        for (i in 2..15) ClipboardHistoryRules.add(entries, "clip $i", i.toLong())
        assertEquals(11, entries.size)
        assertEquals("pinned", texts(entries).first())
    }

    @Test
    fun add_sameTextMovesToTopInsteadOfDuplicating() {
        val entries = mutableListOf<ClipboardHistoryEntry>()
        ClipboardHistoryRules.add(entries, "hello", 1)
        ClipboardHistoryRules.add(entries, "world", 2)
        assertTrue(ClipboardHistoryRules.add(entries, "hello", 3))
        assertEquals(listOf("hello", "world"), texts(entries))
    }

    @Test
    fun add_sameClipReportedTwiceIsIgnored() {
        val entries = mutableListOf<ClipboardHistoryEntry>()
        ClipboardHistoryRules.add(entries, "hello", 5)
        assertFalse(ClipboardHistoryRules.add(entries, "hello", 5))
        assertEquals(1, entries.size)
    }

    @Test
    fun add_textAlreadyPinnedStaysPinnedAndSingle() {
        val entries = mutableListOf(ClipboardHistoryEntry(1, "pinned", isPinned = true))
        assertFalse(ClipboardHistoryRules.add(entries, "pinned", 2))
        assertEquals(1, entries.size)
        assertTrue(entries[0].isPinned)
    }

    @Test
    fun add_emptyTextIgnored() {
        val entries = mutableListOf<ClipboardHistoryEntry>()
        assertFalse(ClipboardHistoryRules.add(entries, "", 1))
        assertTrue(entries.isEmpty())
    }

    @Test
    fun merge_skipsDuplicatesFromDisk() {
        val entries = mutableListOf(ClipboardHistoryEntry(10, "current"))
        val saved = listOf(
            ClipboardHistoryEntry(10, "current"),
            ClipboardHistoryEntry(5, "current"),
            ClipboardHistoryEntry(4, "older"),
            ClipboardHistoryEntry(3, "pinned", isPinned = true),
        )
        ClipboardHistoryRules.merge(entries, saved)
        assertEquals(listOf("pinned", "current", "older"), texts(entries))
    }
}
