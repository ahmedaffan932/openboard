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
}
