package org.dslul.openboard.translator.pro.classes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordGameLogicTest {

    // --- isNextUnlocked: Next is allowed only once the current level has been solved ---

    @Test
    fun isNextUnlocked_lockedUntilCurrentSolved() {
        // Fresh game: index 0, nothing solved yet -> locked.
        assertFalse(WordGameLogic.isNextUnlocked(0, 0))
        // Still on the word just unlocked -> locked (must solve it first).
        assertFalse(WordGameLogic.isNextUnlocked(1, 1))
        assertFalse(WordGameLogic.isNextUnlocked(5, 5))
    }

    @Test
    fun isNextUnlocked_unlockedWhenBehindLatestSolved() {
        // Solved word 0 (latest = 1), sitting on index 0 -> can advance.
        assertTrue(WordGameLogic.isNextUnlocked(0, 1))
        // Revisiting an earlier, already-solved level -> can advance.
        assertTrue(WordGameLogic.isNextUnlocked(2, 5))
    }

    // --- resetIndexIfAtEnd: wrap back to the first word at the end of the list ---

    @Test
    fun resetIndexIfAtEnd_wrapsAtOrPastEnd() {
        assertEquals(0, WordGameLogic.resetIndexIfAtEnd(5, 5)) // exactly at end
        assertEquals(0, WordGameLogic.resetIndexIfAtEnd(6, 5)) // past end (stale index)
    }

    @Test
    fun resetIndexIfAtEnd_keepsInRangeIndex() {
        assertEquals(0, WordGameLogic.resetIndexIfAtEnd(0, 5))
        assertEquals(4, WordGameLogic.resetIndexIfAtEnd(4, 5))
    }

    @Test
    fun resetIndexIfAtEnd_emptyListLeavesIndexUntouched() {
        // Guard against division-by-zero style wraps when size is 0.
        assertEquals(3, WordGameLogic.resetIndexIfAtEnd(3, 0))
    }
}
