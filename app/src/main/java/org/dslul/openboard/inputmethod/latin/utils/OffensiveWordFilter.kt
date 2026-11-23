package org.dslul.openboard.inputmethod.latin.utils

import java.util.function.Predicate

/**
 * "Block offensive words": the main dictionary flags offensive words and never suggests them, but
 * words learned from typing (the typing history) carry no flag, so once typed they came back as
 * completions and autocorrections. These rules apply the main dictionary's flag to every
 * suggestion, like the main dictionary does for its own words.
 */
object OffensiveWordFilter {

    /**
     * Whether [suggestion] must be hidden: a flagged word, unless it is exactly what the user
     * typed (the main dictionary also lets an exact match through: it's what they wrote).
     * [typedWord] is empty or null for predictions and swipes, which hide every flagged word.
     */
    @JvmStatic
    fun shouldHide(suggestion: String, typedWord: String?, isOffensive: Predicate<String>): Boolean {
        if (!typedWord.isNullOrEmpty() && suggestion.equals(typedWord, ignoreCase = true)) return false
        return isOffensive.test(suggestion)
    }

    /** Whether a typed word may be learned into the typing history. */
    @JvmStatic
    fun mayLearn(word: String, frequency: Int, blockOffensive: Boolean, isOffensive: Predicate<String>): Boolean {
        if (!blockOffensive) return true
        // Older dictionaries mark offensive words with frequency 0 instead of a flag.
        return frequency != 0 && !isOffensive.test(word)
    }
}
