package org.dslul.openboard.translator.pro.learn.engine

import kotlin.random.Random

/**
 * A running lesson: the exercise queue, the answers so far, and the result. A wrong single-item
 * answer is asked once more at the end (with reshuffled options) when [retryWrong] is on.
 */
class LessonSession(
    val kind: Kind,
    exercises: List<Exercise>,
    private val retryWrong: Boolean = kind != Kind.CHECKPOINT,
    private val random: Random = Random.Default,
) {
    enum class Kind { LESSON, PRACTICE, CHECKPOINT, PHRASES }

    private val queue = ArrayList(exercises)
    private val retried = HashSet<String>()

    /** Item id → answered right every time it was asked in this session. */
    val itemResults = LinkedHashMap<String, Boolean>()

    var index = 0
        private set
    var gradedCount = 0
        private set
    var correctCount = 0
        private set
    var mistakes = 0
        private set
    val startedAt: Long = System.currentTimeMillis()

    val exercises: List<Exercise> get() = queue
    val total: Int get() = queue.size
    val current: Exercise get() = queue[index]
    val isFinished: Boolean get() = index >= queue.size

    /** 0..1 for the progress bar. */
    val progress: Float get() = if (queue.isEmpty()) 1f else index.toFloat() / queue.size

    /**
     * Records the answer to the current exercise. For a match round, [wrongIds] are the items
     * that were mismatched at least once.
     */
    fun answer(correct: Boolean, wrongIds: Collection<String> = emptyList()) {
        val ex = current
        if (!ex.isGraded) return
        gradedCount++
        if (correct) correctCount++ else mistakes++
        if (ex is Exercise.Match) {
            for (id in ex.itemIds) itemResults[id] = (itemResults[id] ?: true) && id !in wrongIds
        } else {
            for (id in ex.itemIds) itemResults[id] = (itemResults[id] ?: true) && correct
        }
        if (!correct && retryWrong && ex !is Exercise.Match) {
            val id = ex.itemIds.firstOrNull()
            if (id != null && retried.add(id)) queue.add(reshuffled(ex))
        }
    }

    /** Moves on; returns false when the lesson is over. */
    fun next(): Boolean {
        index++
        return !isFinished
    }

    /** Percent of graded answers right on the first go (retries count as answers too). */
    val accuracy: Int get() = if (gradedCount == 0) 100 else correctCount * 100 / gradedCount

    val isPerfect: Boolean get() = gradedCount > 0 && mistakes == 0

    /** Passed a checkpoint: at least 80% right. */
    val passed: Boolean get() = accuracy >= 80

    val xp: Int
        get() = when (kind) {
            Kind.LESSON -> 10 + if (isPerfect) 5 else 0
            Kind.PRACTICE -> 5 + if (isPerfect) 5 else 0
            Kind.CHECKPOINT -> if (passed) 20 else 5
            Kind.PHRASES -> 5 + if (isPerfect) 5 else 0
        }

    /** Items introduced in this session (their Intro cards). */
    val newItems: List<StudyItem> get() = queue.filterIsInstance<Exercise.Intro>().map { it.item }

    private fun reshuffled(ex: Exercise): Exercise = when (ex) {
        is Exercise.ChooseMeaning -> ex.copy(options = ex.options.shuffled(random))
        is Exercise.ChooseTarget -> ex.copy(options = ex.options.shuffled(random))
        is Exercise.Listen -> ex.copy(options = ex.options.shuffled(random))
        is Exercise.Tiles -> ex.copy(tiles = ex.tiles.shuffled(random))
        else -> ex
    }
}
