package org.dslul.openboard.translator.pro.learn.engine

import kotlin.random.Random

/** One screen of a lesson. Everything is generated from [StudyItem]s; nothing is hand-written. */
sealed class Exercise {
    /** Items this exercise tests (empty for cards that are not graded). */
    abstract val itemIds: List<String>
    open val isGraded: Boolean get() = true

    /** A grammar tip card before the unit's first lesson. */
    data class TipCard(val tip: Tip, val examples: List<StudyItem>) : Exercise() {
        override val itemIds: List<String> get() = emptyList()
        override val isGraded: Boolean get() = false
    }

    /** First sight of a new item: text, audio and meaning. */
    data class Intro(val item: StudyItem) : Exercise() {
        override val itemIds: List<String> get() = emptyList()
        override val isGraded: Boolean get() = false
    }

    /** Course-language text (+ audio) shown; pick its meaning. */
    data class ChooseMeaning(val item: StudyItem, val options: List<StudyItem>) : Exercise() {
        override val itemIds: List<String> get() = listOf(item.id)
    }

    /** Meaning shown; pick the course-language text. */
    data class ChooseTarget(val item: StudyItem, val options: List<StudyItem>) : Exercise() {
        override val itemIds: List<String> get() = listOf(item.id)
    }

    /** Only audio; pick the course-language text heard. */
    data class Listen(val item: StudyItem, val options: List<StudyItem>) : Exercise() {
        override val itemIds: List<String> get() = listOf(item.id)
    }

    /** Meaning shown; build the course-language sentence from word tiles (+ distractors). */
    data class Tiles(val item: StudyItem, val tiles: List<String>) : Exercise() {
        override val itemIds: List<String> get() = listOf(item.id)
    }

    /** Meaning shown; type it in the course language. */
    data class Type(val item: StudyItem) : Exercise() {
        override val itemIds: List<String> get() = listOf(item.id)
    }

    /** Tap the matching pairs: course-language texts vs meanings. */
    data class Match(val items: List<StudyItem>) : Exercise() {
        override val itemIds: List<String> get() = items.map { it.id }
    }
}

/**
 * Builds exercise lists. [listening] off replaces audio-only questions (no voice, or the user
 * can't listen now); [typing] off replaces typing with tiles/choices (scripts our keyboard lacks).
 */
class LessonBuilder(
    private val random: Random,
    private val listening: Boolean = true,
    private val typing: Boolean = true,
) {

    /**
     * A lesson: each new item is introduced and recognised, a match round mixes them with earlier
     * words, then four of them are produced (sentences first) and two earlier items are reviewed.
     */
    fun lesson(
        newItems: List<StudyItem>,
        reviewPool: List<StudyItem>,
        optionPool: List<StudyItem>,
        tip: Exercise.TipCard? = null,
    ): List<Exercise> {
        val out = ArrayList<Exercise>()
        if (tip != null) out += tip
        for (item in newItems) {
            out += Exercise.Intro(item)
            out += recognition(item, optionPool)
        }
        match(newItems + reviewPool.shuffled(random))?.let { out += it }
        val produce = newItems.sortedByDescending { if (it.isSentence) 1 else 0 }.take(4).shuffled(random)
        produce.forEach { out += production(it, optionPool) }
        reviewPool.shuffled(random).take(2).forEach {
            out += if (random.nextBoolean()) production(it, optionPool) else recognition(it, optionPool)
        }
        return out
    }

    /** A practice round over already-learned items: a mix of every question type. */
    fun practice(items: List<StudyItem>, optionPool: List<StudyItem>): List<Exercise> {
        val out = ArrayList<Exercise>()
        items.forEachIndexed { i, item ->
            out += if (i % 2 == 0) production(item, optionPool) else recognition(item, optionPool)
        }
        if (items.size >= 6) match(items.shuffled(random))?.let { out.add(minOf(3, out.size), it) }
        return out
    }

    /** A test-out quiz: mostly production, no intro cards. */
    fun checkpoint(items: List<StudyItem>, optionPool: List<StudyItem>): List<Exercise> =
        items.mapIndexed { i, item ->
            if (i % 3 == 2) recognition(item, optionPool) else production(item, optionPool)
        }

    /** Recognise: read (or hear) the course text, pick the meaning (or the text heard). */
    fun recognition(item: StudyItem, pool: List<StudyItem>): Exercise =
        if (listening && random.nextInt(10) < 4) {
            Exercise.Listen(item, options(item, pool) { it.target })
        } else {
            Exercise.ChooseMeaning(item, options(item, pool) { it.meaning })
        }

    /** Produce: from the meaning, build or type the course text (or pick it). */
    fun production(item: StudyItem, pool: List<StudyItem>): Exercise {
        val tokens = item.tokens
        return when {
            tokens.size >= 3 && (!typing || random.nextInt(10) < 7) -> Exercise.Tiles(item, tiles(item, pool))
            typing && (tokens.size >= 3 || random.nextBoolean()) -> Exercise.Type(item)
            else -> Exercise.ChooseTarget(item, options(item, pool) { it.target })
        }
    }

    /** Up to 5 pairs of short items with distinct texts and meanings; null if fewer than 4. */
    fun match(candidates: List<StudyItem>): Exercise.Match? {
        val seenTargets = HashSet<String>()
        val seenMeanings = HashSet<String>()
        val picked = ArrayList<StudyItem>()
        for (c in candidates.sortedBy { if (it.isSentence) 1 else 0 }) {
            if (c.target.length > 22 || c.meaning.length > 22) continue
            val t = AnswerChecker.normalize(c.target)
            val m = AnswerChecker.normalize(c.meaning)
            // "No" = "No": the same text in both columns is a confusing pair.
            if (t == m || t in seenTargets || m in seenMeanings) continue
            seenTargets += t; seenMeanings += m; picked += c
            if (picked.size == 5) break
        }
        return if (picked.size >= 4) Exercise.Match(picked) else null
    }

    /**
     * The answer plus 3 wrong options of the same kind (word vs sentence) whose shown text and
     * meaning both differ from the answer's and from each other. Order is shuffled.
     */
    fun options(item: StudyItem, pool: List<StudyItem>, shown: (StudyItem) -> String): List<StudyItem> {
        val usedShown = hashSetOf(AnswerChecker.normalize(shown(item)))
        val usedMeaning = hashSetOf(AnswerChecker.normalize(item.meaning))
        val usedTarget = hashSetOf(AnswerChecker.normalize(item.target))
        val picks = ArrayList<StudyItem>()
        val sameKind = pool.filter { it.id != item.id && it.isSentence == item.isSentence }.shuffled(random)
        val otherKind = pool.filter { it.id != item.id && it.isSentence != item.isSentence }.shuffled(random)
        for (c in sameKind + otherKind) {
            if (picks.size == 3) break
            val s = AnswerChecker.normalize(shown(c))
            val m = AnswerChecker.normalize(c.meaning)
            val t = AnswerChecker.normalize(c.target)
            if (s in usedShown || m in usedMeaning || t in usedTarget) continue
            usedShown += s; usedMeaning += m; usedTarget += t
            picks += c
        }
        return (picks + item).shuffled(random)
    }

    /** The answer's tiles plus 2–3 words from other sentences that are not in the answer. */
    fun tiles(item: StudyItem, pool: List<StudyItem>): List<String> {
        val answer = item.tokens
        val inAnswer = answer.map { AnswerChecker.normalize(it) }.toHashSet()
        val extra = pool.asSequence()
            .filter { it.id != item.id && it.isSentence }
            .flatMap { it.tokens.asSequence() }
            .filter { AnswerChecker.normalize(it).let { n -> n.isNotEmpty() && n !in inAnswer } }
            .distinctBy { AnswerChecker.normalize(it) }
            .toList()
            .shuffled(random)
            .take(if (answer.size <= 4) 2 else 3)
        return (answer + extra).shuffled(random)
    }
}
