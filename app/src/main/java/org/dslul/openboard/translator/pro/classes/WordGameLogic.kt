package org.dslul.openboard.translator.pro.classes

/**
 * Pure, unit-testable progression rules for the word game, extracted from the activity
 * so they can be covered without an Android runtime.
 */
object WordGameLogic {

    /** The Next button is unlocked only once the current level has been solved. */
    fun isNextUnlocked(index: Int, latestSolved: Int): Boolean = index < latestSolved

    /** Wraps back to the first word once the end of the list is reached. */
    fun resetIndexIfAtEnd(index: Int, size: Int): Int =
        if (size > 0 && index >= size) 0 else index

    /**
     * Whether a full-length guess solves the level: the puzzle word itself, or another common
     * English word made of exactly the same letters (e.g. "rat" or "tar" for "art"). Only the
     * puzzle word used to count, so real words were marked wrong.
     *
     * @param alternates puzzle word (lowercase) -> the other accepted words (lowercase), from
     *        [WordGameContent.loadAlternates].
     */
    fun isAcceptedGuess(target: String, guess: String, alternates: Map<String, List<String>>): Boolean {
        if (target.equals(guess, ignoreCase = true)) return true
        val key = target.lowercase(java.util.Locale.ROOT)
        val normalizedGuess = guess.lowercase(java.util.Locale.ROOT)
        return alternates[key]?.contains(normalizedGuess) == true
    }
}
