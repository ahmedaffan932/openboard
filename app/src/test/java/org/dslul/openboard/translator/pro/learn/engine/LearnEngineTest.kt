package org.dslul.openboard.translator.pro.learn.engine

import org.dslul.openboard.translator.pro.learn.engine.AnswerChecker.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AnswerCheckerTest {

    private fun verdict(input: String, vararg accepted: String) =
        AnswerChecker.check(input, accepted.toList()).verdict

    @Test fun exactAndIgnoringCasePunctuationSpaces() {
        assertEquals(Verdict.CORRECT, verdict("Muchas gracias", "Muchas gracias"))
        assertEquals(Verdict.CORRECT, verdict("  muchas   GRACIAS!! ", "Muchas gracias."))
        assertEquals(Verdict.CORRECT, verdict("¿cómo te llamas", "¿Cómo te llamas?"))
        assertEquals(Verdict.CORRECT, verdict("Wie geht's dir", "Wie geht's dir?"))
    }

    @Test fun apostrophesAndHyphensDoNotMatter() {
        assertEquals(Verdict.CORRECT, verdict("Im fine", "I'm fine."))
        assertEquals(Verdict.CORRECT, verdict("I’m fine", "I'm fine."))
        assertEquals(Verdict.CORRECT, verdict("What time is checkout?", "What time is check-out?"))
        assertEquals(Verdict.CORRECT, verdict("l'eau", "l'eau"))
        assertEquals(Verdict.CORRECT, verdict("l eau", "l'eau"))
    }

    @Test fun missingAccentIsAcceptedWithNote() {
        assertEquals(Verdict.ACCENT, verdict("Donde esta el hotel", "¿Dónde está el hotel?"))
        assertEquals(Verdict.ACCENT, verdict("uber", "über"))
        assertEquals(Verdict.ACCENT, verdict("Strasse", "Straße"))
        assertEquals(Verdict.ACCENT, verdict("manana", "mañana"))
    }

    @Test fun oneTypoInALongerAnswer() {
        assertEquals(Verdict.TYPO, verdict("Muchas grasias", "Muchas gracias"))
        assertEquals(Verdict.WRONG, verdict("si", "no"))
        // short words need to be exact (apart from accents)
        assertEquals(Verdict.WRONG, verdict("gato", "pato"))
        assertEquals(Verdict.WRONG, verdict("sol", "sal"))
    }

    @Test fun alternativesAndArticles() {
        assertEquals(Verdict.CORRECT, verdict("agua", "el agua", "agua"))
        val r = AnswerChecker.check("¿Cómo se llama usted?", listOf("¿Cómo te llamas?", "¿Cómo se llama usted?"))
        assertEquals(Verdict.CORRECT, r.verdict)
        assertEquals("¿Cómo se llama usted?", r.closest)
    }

    @Test fun wrongAndEmpty() {
        assertEquals(Verdict.WRONG, verdict("Buenas noches", "Buenos días"))
        assertEquals(Verdict.WRONG, verdict("", "Hola"))
        assertEquals("Hola", AnswerChecker.check("adios", listOf("Hola", "Buenas")).closest)
    }

    @Test fun distance() {
        assertEquals(0, AnswerChecker.distance("abc", "abc"))
        assertEquals(1, AnswerChecker.distance("abc", "abd"))
        assertEquals(3, AnswerChecker.distance("kitten", "sitting"))
        assertTrue(AnswerChecker.distance("aaaaaaaa", "b", 2) > 2)
    }
}

class SrsAndStreakTest {

    @Test fun correctAnswersClimbBoxes() {
        var p = ItemProgress()
        p = Srs.answered(p, true, 100)
        assertEquals(1, p.box); assertEquals(101, p.due)
        p = Srs.answered(p, true, 101)
        assertEquals(2, p.box); assertEquals(103, p.due)
        repeat(5) { p = Srs.answered(p, true, 200) }
        assertEquals(Srs.MAX_BOX, p.box); assertEquals(216, p.due)
    }

    @Test fun wrongAnswerResetsAndIsDueToday() {
        var p = ItemProgress(box = 4, due = 120, seen = 5, right = 5)
        p = Srs.answered(p, false, 110)
        assertEquals(1, p.box); assertEquals(110, p.due); assertEquals(1, p.wrong)
        assertTrue(Srs.isDue(p, 110))
        assertFalse(Srs.isDue(ItemProgress(), 110))
    }

    @Test fun practicePicksDueFirstThenWeakest() {
        val progress = mapOf(
            "a" to ItemProgress(box = 3, due = 50, seen = 3),
            "b" to ItemProgress(box = 1, due = 99, seen = 2, wrong = 1),
            "c" to ItemProgress(box = 2, due = 105, seen = 2),
            "d" to ItemProgress(box = 5, due = 200, seen = 9),
            "e" to ItemProgress(),
        )
        val picked = Srs.pickForPractice(progress, 100, 3, Random(1))
        assertEquals(setOf("a", "b", "c"), picked.toSet())
        assertEquals(2, Srs.dueCount(progress, 100))
        assertEquals(4, Srs.pickForPractice(progress, 100, 10, Random(1)).size)
    }

    @Test fun streak() {
        assertEquals(1, StreakRules.afterLesson(0, -1, 10))
        assertEquals(3, StreakRules.afterLesson(2, 9, 10))
        assertEquals(2, StreakRules.afterLesson(2, 10, 10))
        assertEquals(1, StreakRules.afterLesson(7, 5, 10))
        assertEquals(4, StreakRules.current(4, 9, 10))
        assertEquals(0, StreakRules.current(4, 8, 10))
        assertEquals(0, StreakRules.current(4, -1, 10))
    }

    @Test fun streakFreezes() {
        // missed yesterday, one freeze: used, streak continues from yesterday
        val one = StreakRules.settle(count = 5, lastDay = 8, today = 10, freezes = 1)
        assertEquals(StreakRules.Settled(9, 1, listOf(9L)), one)
        assertEquals(5, StreakRules.current(5, one.lastDay, 10))
        // missed two days, only one freeze: nothing used, streak lost
        assertEquals(StreakRules.Settled(7, 0, emptyList()), StreakRules.settle(5, 7, 10, 1))
        assertEquals(StreakRules.Settled(9, 2, listOf(8L, 9L)), StreakRules.settle(5, 7, 10, 2))
        // nothing missed, no streak, or never active: untouched
        assertEquals(0, StreakRules.settle(5, 9, 10, 2).freezesUsed)
        assertEquals(0, StreakRules.settle(0, 5, 10, 2).freezesUsed)
        assertEquals(0, StreakRules.settle(3, -1, 10, 2).freezesUsed)
        // weeks start on Monday: epoch day 4 (1970-01-05) is a Monday
        assertEquals(StreakRules.week(4), StreakRules.week(10))
        assertTrue(StreakRules.week(11) == StreakRules.week(4) + 1)
    }

    @Test fun weeklyStats() {
        var s = WeeklyStats.add(emptyList(), 100, 15)
        s = WeeklyStats.add(s, 100, 10)
        s = WeeklyStats.add(s, 103, 5)
        s = WeeklyStats.add(s, 85, 5).let { WeeklyStats.add(it, 104, 10) } // 85 is too old by 104
        assertEquals(listOf(100L, 103L, 104L), s.map { it.day })
        assertEquals(WeeklyStats.parse(WeeklyStats.format(s)), s)
        assertEquals(Triple(4, 40, 3), WeeklyStats.lastWeek(s, 104))
        assertEquals(Triple(1, 10, 1), WeeklyStats.lastWeek(s, 110))
        assertEquals(emptyList<DayStat>(), WeeklyStats.parse("x,1:2,3:a:4"))
    }

    @Test fun localDays() {
        val utc = java.util.TimeZone.getTimeZone("UTC")
        assertEquals(0L, LearnDays.today(0, utc))
        assertEquals(1L, LearnDays.today(86_400_000L, utc))
        val plus5 = java.util.TimeZone.getTimeZone("GMT+05:00")
        assertEquals(1L, LearnDays.today(86_400_000L - 3_600_000L, plus5))
    }
}

class LessonBuilderTest {

    private fun item(i: Int, sentence: Boolean) = if (sentence) {
        StudyItem("s$i", "Frase numero $i aqui.", listOf("Frase $i"), "Sentence number $i here.", true)
    } else {
        StudyItem("w$i", "palabra$i", emptyList(), "word$i", false)
    }

    private val pool = (1..20).map { item(it, false) } + (1..20).map { item(it, true) }

    @Test fun lessonIntroducesEveryNewItemAndOptionsContainTheAnswer() {
        val news = listOf(item(1, false), item(2, false), item(3, false), item(1, true), item(2, true), item(3, true))
        val review = listOf(item(10, false), item(11, false), item(10, true))
        val ex = LessonBuilder(Random(7)).lesson(news, review, pool)
        assertEquals(6, ex.count { it is Exercise.Intro })
        for (e in ex) {
            when (e) {
                is Exercise.ChooseMeaning -> checkOptions(e.item, e.options)
                is Exercise.ChooseTarget -> checkOptions(e.item, e.options)
                is Exercise.Listen -> checkOptions(e.item, e.options)
                is Exercise.Tiles -> assertTrue(e.tiles.containsAll(e.item.tokens))
                is Exercise.Match -> assertTrue(e.items.size in 4..5)
                else -> Unit
            }
        }
        // the intro of each item comes before any graded question about it
        for (n in news) {
            val intro = ex.indexOfFirst { it is Exercise.Intro && it.item.id == n.id }
            val firstGraded = ex.indexOfFirst { it.isGraded && n.id in it.itemIds }
            assertTrue(intro < firstGraded)
        }
        assertNotNull(ex.firstOrNull { it is Exercise.Match })
    }

    private fun checkOptions(answer: StudyItem, options: List<StudyItem>) {
        assertEquals(4, options.size)
        assertTrue(options.any { it.id == answer.id })
        assertEquals(4, options.map { it.id }.toSet().size)
        assertEquals(4, options.map { it.meaning }.toSet().size)
    }

    @Test fun noListeningOrTypingWhenOff() {
        val b = LessonBuilder(Random(3), listening = false, typing = false)
        repeat(50) {
            val e = b.recognition(item(it % 20 + 1, it % 2 == 0), pool)
            assertFalse(e is Exercise.Listen)
            val p = b.production(item(it % 20 + 1, it % 2 == 0), pool)
            assertFalse(p is Exercise.Type)
        }
    }

    @Test fun matchNeedsFourDistinctShortItems() {
        val b = LessonBuilder(Random(1))
        assertNull(b.match(listOf(item(1, false), item(2, false), item(3, false))))
        val dupes = listOf(item(1, false), item(1, false).copy(id = "x"), item(2, false), item(3, false))
        assertNull(b.match(dupes))
        assertEquals(5, b.match((1..8).map { item(it, false) })!!.items.size)
    }
}

class LessonSessionTest {

    private val a = StudyItem("a", "uno", emptyList(), "one", false)
    private val b = StudyItem("b", "dos", emptyList(), "two", false)

    @Test fun wrongAnswerIsRetriedOnceAndResultsTracked() {
        val s = LessonSession(
            LessonSession.Kind.LESSON,
            listOf(Exercise.Intro(a), Exercise.Type(a), Exercise.Type(b)),
            random = Random(1)
        )
        s.answer(true) // intro: ignored
        s.next()
        s.answer(false) // a wrong -> retried at the end
        s.next()
        s.answer(true)
        s.next()
        assertEquals(4, s.total)
        assertTrue(s.current is Exercise.Type)
        s.answer(false) // wrong again: not retried twice
        assertFalse(s.next())
        assertEquals(4, s.total)
        assertEquals(mapOf("a" to false, "b" to true), s.itemResults)
        assertEquals(33, s.accuracy)
        assertEquals(10, s.xp)
        assertFalse(s.isPerfect)
    }

    @Test fun perfectLessonBonusAndCheckpointPass() {
        val s = LessonSession(LessonSession.Kind.LESSON, listOf(Exercise.Type(a)))
        s.answer(true)
        assertEquals(15, s.xp)
        val c = LessonSession(LessonSession.Kind.CHECKPOINT, (1..5).map { Exercise.Type(a) })
        repeat(4) { c.answer(true); c.next() }
        c.answer(false)
        assertEquals(5, c.total) // no retries in a checkpoint
        assertTrue(c.passed)
        assertEquals(20, c.xp)
    }

    @Test fun matchRecordsMismatchedItems() {
        val m = LessonSession(LessonSession.Kind.PRACTICE, listOf(Exercise.Match(listOf(a, b))))
        m.answer(false, listOf("b"))
        assertEquals(mapOf("a" to true, "b" to false), m.itemResults)
        assertEquals(1, m.total)
    }
}

class CoursePlanTest {

    private val syllabus = Syllabus(
        1,
        listOf(
            SyllabusUnit(1, listOf(listOf("a"), listOf("b"))),
            SyllabusUnit(2, listOf(listOf("c"), listOf("d"))),
        ),
        mapOf(
            "a" to Concept("a", "A", false), "b" to Concept("b", "B", false),
            "c" to Concept("c", "C", false), "d" to Concept("d", "D", false)
        )
    )

    @Test fun nextLessonAndStates() {
        assertEquals(1 to 1, CoursePlan.nextLesson(syllabus, emptySet()))
        val done = setOf("u1l1", "u1l2")
        assertEquals(2 to 1, CoursePlan.nextLesson(syllabus, done))
        assertEquals(CoursePlan.UnitState.DONE, CoursePlan.unitState(syllabus, 1, done))
        assertEquals(CoursePlan.UnitState.CURRENT, CoursePlan.unitState(syllabus, 2, done))
        assertEquals(CoursePlan.UnitState.UPCOMING, CoursePlan.unitState(syllabus, 2, emptySet()))
        assertNull(CoursePlan.nextLesson(syllabus, done + setOf("u2l1", "u2l2")))
        assertEquals(50, CoursePlan.percent(syllabus, done))
        assertEquals(listOf("a", "b"), syllabus.itemsBefore(2, 1))
        assertEquals(listOf("a", "b", "c"), syllabus.itemsBefore(2, 2))
        assertEquals(setOf("u1l1", "u1l2"), CoursePlan.testOutKeys(syllabus, 1))
    }

    @Test fun locks() {
        assertFalse(CoursePlan.isLocked(3, 3, false, 0, 1000))
        assertTrue(CoursePlan.isLocked(4, 3, false, 0, 1000))
        assertFalse(CoursePlan.isLocked(4, 3, true, 0, 1000))
        assertFalse(CoursePlan.isLocked(4, 3, false, 2000, 1000))
        assertTrue(CoursePlan.isLocked(4, 3, false, 900, 1000))
    }
}
