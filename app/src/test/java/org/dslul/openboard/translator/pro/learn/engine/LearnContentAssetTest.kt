package org.dslul.openboard.translator.pro.learn.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.random.Random

/**
 * Checks the shipped Learn content (assets/learn/) against the engine: every course covers the
 * syllabus, every canonical answer is accepted by the checker, sentences can be rebuilt from their
 * tiles, and every lesson of every course builds with four distinct options per question.
 */
class LearnContentAssetTest {

    private val dir = File("src/main/assets/learn")
    private fun read(path: String) = File(dir, path).readText(Charsets.UTF_8)

    private val manifest = ContentParser.manifest(read("manifest.json"))
    private val syllabus = ContentParser.syllabus(read("concepts.json"))

    private fun course(code: String, meaning: String): CourseContent = CourseContent(
        code, meaning, syllabus,
        ContentParser.texts(read("text/$code.json")),
        ContentParser.texts(read("text/$meaning.json")),
        ContentParser.texts(read("text/en.json")),
        ContentParser.tips(read("tips/en/$code.json")),
    )

    @Test fun syllabusShape() {
        assertEquals(12, syllabus.unitCount)
        assertEquals(360, syllabus.concepts.size)
        assertEquals(listOf("en", "es", "fr", "de", "it"), manifest.courses)
    }

    @Test fun everyLanguageCoversTheSyllabus() {
        for (lang in manifest.glossLanguages) {
            val texts = ContentParser.texts(read("text/$lang.json"))
            val missing = syllabus.concepts.keys.filter { it !in texts }
            assertTrue("$lang misses $missing", missing.isEmpty())
        }
    }

    @Test fun canonicalAnswersAndTilesAreAccepted() {
        for (code in manifest.courses) {
            val c = course(code, if (code == "en") "es" else "en")
            for (item in c.allItems) {
                for (a in item.accepted) {
                    assertTrue("$code ${item.id}: \"$a\" rejected", AnswerChecker.check(a, item.accepted).verdict == AnswerChecker.Verdict.CORRECT)
                }
                val rebuilt = item.tokens.joinToString(" ")
                assertEquals("$code ${item.id}", AnswerChecker.Verdict.CORRECT, AnswerChecker.check(rebuilt, item.accepted).verdict)
            }
        }
    }

    @Test fun everyLessonOfEveryCourseBuilds() {
        for (code in manifest.courses) {
            val c = course(code, if (code == "en") "hi" else "en")
            for ((u, l) in syllabus.allLessons()) {
                val news = c.items(syllabus.lessonItems(u, l))
                assertEquals("$code u$u l$l", 6, news.size)
                val before = syllabus.itemsBefore(u, l)
                val pool = c.items(before + syllabus.unitItems(u)).let { if (it.size < 12) c.allItems else it }
                val tip = if (l == 1) c.tipFor(u)?.let { Exercise.TipCard(it, c.items(it.examples)) } else null
                val ex = LessonBuilder(Random(u * 31 + l)).lesson(news, c.items(before.takeLast(8)), pool, tip)
                assertTrue(ex.size >= 16)
                for (e in ex) {
                    val options = when (e) {
                        is Exercise.ChooseMeaning -> e.options
                        is Exercise.ChooseTarget -> e.options
                        is Exercise.Listen -> e.options
                        else -> null
                    } ?: continue
                    assertEquals("$code u$u l$l ${e.itemIds}", 4, options.size)
                }
            }
        }
    }

    @Test fun tipsReferenceRealConcepts() {
        val tipsDir = File(dir, "tips")
        for (explain in tipsDir.listFiles()!!) for (f in explain.listFiles()!!) {
            for (tip in ContentParser.tips(f.readText())) {
                assertTrue("${f.path}", tip.unit in 1..12)
                assertTrue("${f.path}", tip.examples.all { it in syllabus.concepts })
            }
        }
    }

    @Test fun phraseDeckPairsThePhrasebook() {
        ZipFile(File("src/main/assets/translated-phrases.zip")).use { zip ->
            fun entry(lang: String) = zip.getInputStream(zip.getEntry("translated-phrases/$lang.json")).readBytes().toString(Charsets.UTF_8)
            val topics = PhraseDeck.build(entry("en"), entry("es"))
            assertEquals(9, topics.size)
            val phrases = topics.flatMap { it.phrases }
            assertTrue(phrases.size > 180)
            assertTrue(phrases.none { "_" in it.target || "_" in it.meaning })
            assertEquals(phrases.size, phrases.map { it.id }.toSet().size)
        }
    }
}
