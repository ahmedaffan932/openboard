package org.dslul.openboard.translator.pro.learn.engine

/**
 * Where a learner is in a course, from the set of completed lesson keys ("u2l3"). Lessons are
 * taken in order; a unit after the free ones needs Premium or a still-valid rewarded unlock.
 */
object CoursePlan {

    enum class UnitState { DONE, CURRENT, UPCOMING }

    /** The first lesson not completed yet, or null when the course is finished. */
    fun nextLesson(syllabus: Syllabus, completed: Set<String>): Pair<Int, Int>? =
        syllabus.allLessons().firstOrNull { (u, l) -> Syllabus.lessonKey(u, l) !in completed }

    fun completedInUnit(syllabus: Syllabus, unit: Int, completed: Set<String>): Int =
        (1..syllabus.lessonCount(unit)).count { Syllabus.lessonKey(unit, it) in completed }

    fun unitState(syllabus: Syllabus, unit: Int, completed: Set<String>): UnitState {
        if (completedInUnit(syllabus, unit, completed) == syllabus.lessonCount(unit)) return UnitState.DONE
        val next = nextLesson(syllabus, completed) ?: return UnitState.DONE
        return if (next.first == unit) UnitState.CURRENT else UnitState.UPCOMING
    }

    /**
     * Whether new lessons of [unit] are locked: never for Premium or the free units, otherwise
     * only while no rewarded unlock is running ([unlockUntil] epoch millis, 0 = none).
     */
    fun isLocked(unit: Int, freeUnits: Int, premium: Boolean, unlockUntil: Long, now: Long): Boolean =
        !premium && unit > freeUnits && unlockUntil <= now

    /** Lesson keys of units 1..[upToUnit], marked done by a passed test-out. */
    fun testOutKeys(syllabus: Syllabus, upToUnit: Int): Set<String> =
        syllabus.allLessons().filter { it.first <= upToUnit }.map { Syllabus.lessonKey(it.first, it.second) }.toSet()

    /** Course progress in percent (completed lessons of all lessons). */
    fun percent(syllabus: Syllabus, completed: Set<String>): Int {
        val all = syllabus.allLessons()
        return if (all.isEmpty()) 0 else all.count { Syllabus.lessonKey(it.first, it.second) in completed } * 100 / all.size
    }
}
