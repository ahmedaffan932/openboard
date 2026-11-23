package org.dslul.openboard.inputmethod.latin.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.function.Predicate

class OffensiveWordFilterTest {

    // Stands in for the main dictionary's flag; "darn" plays the offensive word.
    private val flagged = Predicate<String> { it.lowercase() == "darn" }

    @Test
    fun completionOfAFlaggedWordIsHidden() {
        assertTrue(OffensiveWordFilter.shouldHide("darn", "da", flagged))
        assertTrue(OffensiveWordFilter.shouldHide("darn", "drn", flagged)) // would autocorrect
    }

    @Test
    fun exactlyTypedFlaggedWordStays() {
        assertFalse(OffensiveWordFilter.shouldHide("darn", "darn", flagged))
        assertFalse(OffensiveWordFilter.shouldHide("Darn", "darn", flagged))
    }

    @Test
    fun predictionsAndSwipesHideEveryFlaggedWord() {
        assertTrue(OffensiveWordFilter.shouldHide("darn", "", flagged))
        assertTrue(OffensiveWordFilter.shouldHide("darn", null, flagged))
    }

    @Test
    fun otherWordsStay() {
        assertFalse(OffensiveWordFilter.shouldHide("dark", "da", flagged))
    }

    @Test
    fun flaggedWordsAreNotLearnedWhileBlocking() {
        assertFalse(OffensiveWordFilter.mayLearn("darn", 120, true, flagged))
        assertFalse(OffensiveWordFilter.mayLearn("heck", 0, true, flagged)) // old-style: frequency 0
        assertTrue(OffensiveWordFilter.mayLearn("dark", 120, true, flagged))
        assertTrue(OffensiveWordFilter.mayLearn("darn", 120, false, flagged))
    }
}
