package org.dslul.openboard.inputmethod.latin.gesture

import java.text.Normalizer

/**
 * The words swipe typing can produce, with their dictionary probability (0..255), indexed by
 * the base letter of their first and last letter so a swipe only has to look at words that start
 * and end near where the finger went down and came up.
 *
 * Built once per language on a background thread, often right after the keyboard process starts
 * when its code is not compiled yet, so the index is built with plain arrays.
 */
class GestureWords private constructor(
    val words: Array<String>,
    val probabilities: IntArray
) {
    private val index: HashMap<Long, IntArray> = buildIndex(words)

    val size: Int get() = words.size

    /** "word<TAB>probability" lines, most frequent first; [read] turns them back into a list. */
    fun write(out: Appendable) {
        for (i in words.indices) out.append(words[i]).append('\t').append(probabilities[i].toString()).append('\n')
    }

    /** Indices of the words whose first base letter is in [firstLetters] and last in [lastLetters]. */
    fun candidates(firstLetters: Collection<Int>, lastLetters: Collection<Int>): List<Int> {
        val out = ArrayList<Int>()
        for (first in firstLetters) {
            for (last in lastLetters) {
                index[pairKey(first, last)]?.let { ids -> ids.forEach { out.add(it) } }
            }
        }
        return out
    }

    companion object {
        /** Keeps the most frequent words only: rare words cost memory and rarely win a swipe. */
        const val MAX_WORDS = 80_000

        private const val NO_KEY = Long.MIN_VALUE

        /** Lower case without accents, so "É" and "e" share a key and an index slot. */
        @JvmStatic
        fun baseLetter(codePoint: Int): Int {
            val lower = Character.toLowerCase(codePoint)
            if (lower < 0x80) return lower
            val decomposed = Normalizer.normalize(String(Character.toChars(lower)), Normalizer.Form.NFD)
            return if (decomposed.isEmpty()) lower else decomposed.codePointAt(0)
        }

        /** Apostrophes and hyphens are part of words ("don't") but have no key on the swipe path. */
        @JvmStatic
        fun isSkippedOnPath(codePoint: Int) =
            codePoint == '\''.code || codePoint == '’'.code || codePoint == '-'.code

        private fun isWordChar(codePoint: Int): Boolean {
            if (Character.isLetter(codePoint) || isSkippedOnPath(codePoint)) return true
            return when (Character.getType(codePoint)) {
                Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt() -> true
                else -> false
            }
        }

        private fun isSwipeableWord(word: String): Boolean {
            if (word.codePointCount(0, word.length) < 2) return false
            var i = 0
            while (i < word.length) {
                val cp = word.codePointAt(i)
                if (!isWordChar(cp)) return false
                i += Character.charCount(cp)
            }
            return true
        }

        private fun pairKey(first: Int, last: Int) = (first.toLong() shl 32) or (last.toLong() and 0xffffffffL)

        /**
         * Builds the list from (word, probability) pairs. Duplicates keep their highest
         * probability; words with digits, spaces or symbols, single letters and words with no
         * probability are dropped.
         */
        @JvmStatic
        fun build(entries: Iterable<Pair<String, Int>>, maxWords: Int = MAX_WORDS): GestureWords {
            val best = HashMap<String, Int>()
            for ((word, probability) in entries) {
                if (probability < 0 || !isSwipeableWord(word)) continue
                val old = best[word]
                if (old == null || old < probability) best[word] = probability
            }
            val sorted = best.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(maxWords)
            return GestureWords(Array(sorted.size) { sorted[it].key }, IntArray(sorted.size) { sorted[it].value })
        }

        /**
         * Reads lines written by [write]. They are already filtered and sorted, so only malformed
         * lines are skipped.
         */
        @JvmStatic
        fun read(lines: Sequence<String>): GestureWords {
            val words = ArrayList<String>(MAX_WORDS)
            var probabilities = IntArray(1024)
            for (line in lines) {
                val tab = line.lastIndexOf('\t')
                if (tab <= 0) continue
                val probability = line.substring(tab + 1).toIntOrNull() ?: continue
                if (words.size == probabilities.size) probabilities = probabilities.copyOf(probabilities.size * 2)
                probabilities[words.size] = probability
                words.add(line.substring(0, tab))
            }
            return GestureWords(words.toTypedArray(), probabilities.copyOf(words.size))
        }

        private fun buildIndex(words: Array<String>): HashMap<Long, IntArray> {
            // Two passes over plain arrays: count the words per (first, last) pair, then fill.
            val keys = LongArray(words.size) { indexKey(words[it]) }
            val counts = HashMap<Long, IntArray>()
            for (key in keys) {
                if (key == NO_KEY) continue
                val count = counts[key]
                if (count == null) counts[key] = intArrayOf(1) else count[0]++
            }
            val index = HashMap<Long, IntArray>(counts.size * 2)
            for ((key, count) in counts) index[key] = IntArray(count[0])
            for (count in counts.values) count[0] = 0
            for (id in keys.indices) {
                val key = keys[id]
                if (key == NO_KEY) continue
                val filled = counts.getValue(key)
                index.getValue(key)[filled[0]++] = id
            }
            return index
        }

        private fun indexKey(word: String): Long {
            var first = -1
            var last = -1
            var i = 0
            while (i < word.length) {
                val cp = word.codePointAt(i)
                if (!isSkippedOnPath(cp)) {
                    if (first == -1) first = cp
                    last = cp
                }
                i += Character.charCount(cp)
            }
            return if (first == -1) NO_KEY else pairKey(baseLetter(first), baseLetter(last))
        }
    }
}
