package org.dslul.openboard.translator.pro.learn.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Review state of one item. Days are local epoch days ([LearnDays]). */
data class ItemProgress(
    val box: Int = 0,
    val due: Long = 0,
    val seen: Int = 0,
    val right: Int = 0,
    val wrong: Int = 0,
)

/**
 * Leitner spaced repetition with 5 boxes. A correct answer moves an item up one box and schedules
 * it 1, 2, 4, 8 or 16 days ahead. A wrong answer sends it back to box 1, due again today.
 */
object Srs {
    val INTERVAL_DAYS = intArrayOf(1, 2, 4, 8, 16)
    const val MAX_BOX = 5

    fun answered(p: ItemProgress, correct: Boolean, today: Long): ItemProgress =
        if (correct) {
            val box = min(p.box + 1, MAX_BOX)
            p.copy(box = box, due = today + INTERVAL_DAYS[box - 1], seen = p.seen + 1, right = p.right + 1)
        } else {
            p.copy(box = 1, due = today, seen = p.seen + 1, wrong = p.wrong + 1)
        }

    fun isDue(p: ItemProgress, today: Long): Boolean = p.seen > 0 && p.due <= today

    fun dueCount(progress: Map<String, ItemProgress>, today: Long): Int =
        progress.values.count { isDue(it, today) }

    /**
     * Ids for a practice round: due items first (most overdue, lowest box first), topped up with
     * the weakest seen items so practice is never empty once something has been learned.
     */
    fun pickForPractice(
        progress: Map<String, ItemProgress>,
        today: Long,
        count: Int,
        random: Random,
    ): List<String> {
        val seen = progress.filter { it.value.seen > 0 }
        val due = seen.filter { isDue(it.value, today) }.entries
            .sortedWith(compareBy({ it.value.due }, { it.value.box }))
            .map { it.key }
        if (due.size >= count) return due.take(count).shuffled(random)
        val weak = seen.entries
            .filter { it.key !in due }
            .sortedWith(compareBy({ it.value.box }, { -it.value.wrong }, { random.nextInt() }))
            .map { it.key }
        return (due + weak).take(count).shuffled(random)
    }
}

/** Daily streak rules. A streak survives a day without lessons only until that day ends. */
object StreakRules {

    /** Streak after finishing a lesson on [today], given the previous count and active day. */
    fun afterLesson(count: Int, lastDay: Long, today: Long): Int = when {
        lastDay == today -> max(count, 1)
        lastDay == today - 1 -> count + 1
        else -> 1
    }

    /** The streak to show on [today]: alive while the last active day is today or yesterday. */
    fun current(count: Int, lastDay: Long, today: Long): Int =
        if (lastDay >= 0 && lastDay >= today - 1) count else 0
}

/** Local calendar days as epoch-day numbers, so streaks follow the user's wall clock. */
object LearnDays {
    fun today(nowMillis: Long = System.currentTimeMillis(), tz: java.util.TimeZone = java.util.TimeZone.getDefault()): Long =
        Math.floorDiv(nowMillis + tz.getOffset(nowMillis), 86_400_000L)
}
