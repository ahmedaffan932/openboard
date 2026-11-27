package org.dslul.openboard.translator.pro.learn.engine

import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

/**
 * Checks a typed (or tile-built) answer against the accepted answers of a [StudyItem].
 *
 * Case, punctuation, apostrophes and spacing never matter. A missing or wrong accent
 * ("gracias" vs "grácias", "uber" vs "über") is accepted with a note, and so is one typo in a
 * longer answer (two in a long sentence).
 */
object AnswerChecker {

    enum class Verdict { CORRECT, ACCENT, TYPO, WRONG }

    /** [closest]: the accepted answer the input matched, or the canonical one when wrong. */
    data class Result(val verdict: Verdict, val closest: String) {
        val isAccepted: Boolean get() = verdict != Verdict.WRONG
    }

    fun check(input: String, accepted: List<String>): Result {
        require(accepted.isNotEmpty())
        val given = normalize(input)
        if (given.isEmpty()) return Result(Verdict.WRONG, accepted.first())
        val givenTight = given.replace(" ", "")

        for (a in accepted) {
            val n = normalize(a)
            if (n == given || n.replace(" ", "") == givenTight) return Result(Verdict.CORRECT, a)
        }
        val givenPlain = stripAccents(givenTight)
        for (a in accepted) {
            if (stripAccents(normalize(a).replace(" ", "")) == givenPlain) return Result(Verdict.ACCENT, a)
        }
        val limit = typoLimit(givenPlain.length)
        if (limit > 0) {
            for (a in accepted) {
                val plain = stripAccents(normalize(a).replace(" ", ""))
                if (distance(plain, givenPlain, limit) <= limit) return Result(Verdict.TYPO, a)
            }
        }
        return Result(Verdict.WRONG, accepted.first())
    }

    /** Lower-case, no punctuation or apostrophes, hyphens as spaces, single spaces, NFC. */
    fun normalize(text: String): String {
        val nfc = Normalizer.normalize(text, Normalizer.Form.NFC).lowercase(Locale.ROOT)
        val sb = StringBuilder(nfc.length)
        for (ch in nfc) {
            when (Character.getType(ch).toByte()) {
                Character.DASH_PUNCTUATION -> sb.append(' ')
                Character.CONNECTOR_PUNCTUATION,
                Character.START_PUNCTUATION,
                Character.END_PUNCTUATION,
                Character.INITIAL_QUOTE_PUNCTUATION,
                Character.FINAL_QUOTE_PUNCTUATION,
                Character.OTHER_PUNCTUATION,
                Character.MODIFIER_SYMBOL -> Unit
                else -> sb.append(if (Character.isWhitespace(ch)) ' ' else ch)
            }
        }
        return sb.toString().trim().replace(Regex(" +"), " ")
    }

    /** Removes accents and folds ß/æ/œ, so only real spelling differences remain. */
    fun stripAccents(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace("ß", "ss").replace("æ", "ae").replace("œ", "oe")

    private fun typoLimit(length: Int): Int = when {
        length >= 14 -> 2
        length >= 5 -> 1
        else -> 0
    }

    /** Levenshtein distance, giving up early once it is above [max]. */
    fun distance(a: String, b: String, max: Int = Int.MAX_VALUE): Int {
        if (kotlin.math.abs(a.length - b.length) > max) return max + 1
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
                rowMin = min(rowMin, cur[j])
            }
            if (rowMin > max) return max + 1
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }
}
