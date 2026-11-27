package org.dslul.openboard.translator.pro.learn

import android.content.Context

/**
 * Phrase Practice memory, per target language: phrases the learner knows and phrases they are
 * still learning (shown again in "Mixed review"). Ids are PhraseDeck's "c<cat>s<sub>p<n>".
 */
object PhraseProgress {
    private const val PREFS = "phrase_practice"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun known(context: Context, lang: String): Set<String> =
        prefs(context).getStringSet("known_$lang", emptySet())!!.toSet()

    fun learning(context: Context, lang: String): Set<String> =
        prefs(context).getStringSet("learning_$lang", emptySet())!!.toSet()

    fun mark(context: Context, lang: String, id: String, knowIt: Boolean) {
        val known = known(context, lang).toMutableSet()
        val learning = learning(context, lang).toMutableSet()
        if (knowIt) {
            known += id; learning -= id
        } else {
            learning += id; known -= id
        }
        prefs(context).edit()
            .putStringSet("known_$lang", known)
            .putStringSet("learning_$lang", learning)
            .apply()
    }

    /** A quiz: right answers count as known, mistakes go to "still learning". */
    fun recordQuiz(context: Context, lang: String, results: Map<String, Boolean>) {
        val known = known(context, lang).toMutableSet()
        val learning = learning(context, lang).toMutableSet()
        for ((id, ok) in results) {
            if (ok) {
                known += id; learning -= id
            } else {
                learning += id; known -= id
            }
        }
        prefs(context).edit()
            .putStringSet("known_$lang", known)
            .putStringSet("learning_$lang", learning)
            .apply()
    }

    /** Last chosen mode: true = flashcards, false = quiz. */
    fun flashcards(context: Context): Boolean = prefs(context).getBoolean("flashcards", true)

    fun setFlashcards(context: Context, on: Boolean) = prefs(context).edit().putBoolean("flashcards", on).apply()
}
