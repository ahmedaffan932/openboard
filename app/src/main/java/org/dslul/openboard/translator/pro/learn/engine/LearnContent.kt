package org.dslul.openboard.translator.pro.learn.engine

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * The Learn module's bundled course data (assets/learn/, see tools/learn-content/README.md), as
 * plain Kotlin. Parsed with Gson so the engine runs in JVM unit tests (org.json is stubbed there).
 */

/** One syllabus entry: an id, its English source text, and whether it is a phrase/sentence. */
data class Concept(val id: String, val en: String, val isSentence: Boolean)

/** A unit: 5 lessons of concept ids, in teaching order. [number] starts at 1. */
data class SyllabusUnit(val number: Int, val lessons: List<List<String>>)

/** The shared A1 syllabus used by every course (assets/learn/concepts.json). */
data class Syllabus(val version: Int, val units: List<SyllabusUnit>, val concepts: Map<String, Concept>) {

    val unitCount: Int get() = units.size

    fun unit(number: Int): SyllabusUnit = units.first { it.number == number }

    fun lessonCount(unit: Int): Int = unit(unit).lessons.size

    fun lessonItems(unit: Int, lesson: Int): List<String> = unit(unit).lessons[lesson - 1]

    /** Every concept id taught in [unit] (all lessons). */
    fun unitItems(unit: Int): List<String> = unit(unit).lessons.flatten()

    /** Ids taught before (unit, lesson), in course order. */
    fun itemsBefore(unit: Int, lesson: Int): List<String> {
        val out = ArrayList<String>()
        for (u in units) {
            for ((i, items) in u.lessons.withIndex()) {
                if (u.number > unit || (u.number == unit && i + 1 >= lesson)) return out
                out += items
            }
        }
        return out
    }

    /** All lessons in order as (unit, lesson) pairs, 1-based. */
    fun allLessons(): List<Pair<Int, Int>> =
        units.flatMap { u -> u.lessons.indices.map { u.number to it + 1 } }

    companion object {
        fun lessonKey(unit: Int, lesson: Int) = "u${unit}l$lesson"
    }
}

/** A concept's text in one language: [t] the canonical answer, [alt] other correct answers. */
data class TextEntry(val t: String, val alt: List<String>)

/** A grammar tip shown before a unit's first lesson. [examples] are concept ids. */
data class Tip(val unit: Int, val title: String, val body: String, val examples: List<String>)

/** assets/learn/manifest.json */
data class LearnManifest(
    val version: Int,
    val courses: List<String>,
    val glossLanguages: List<String>,
)

/**
 * One thing to learn, ready for an exercise: [target] (and [alt]) in the course language,
 * [meaning] in the learner's language.
 */
data class StudyItem(
    val id: String,
    val target: String,
    val alt: List<String>,
    val meaning: String,
    val isSentence: Boolean,
) {
    /** Every answer that counts as correct, canonical first. */
    val accepted: List<String> get() = listOf(target) + alt

    /** The canonical answer split into word tiles (punctuation stays on its word). */
    val tokens: List<String> get() = target.split(' ').filter { it.isNotBlank() }
}

object ContentParser {

    fun syllabus(json: String): Syllabus {
        val root = JsonParser.parseString(json).asJsonObject
        val concepts = LinkedHashMap<String, Concept>()
        for ((id, el) in root.getAsJsonObject("concepts").entrySet()) {
            val o = el.asJsonObject
            concepts[id] = Concept(id, o.get("en").asString, o.get("k").asString == "s")
        }
        val units = root.getAsJsonArray("units").map { u ->
            val uo = u.asJsonObject
            SyllabusUnit(
                uo.get("id").asInt,
                uo.getAsJsonArray("lessons").map { l -> l.asJsonArray.map { it.asString } }
            )
        }
        return Syllabus(root.get("version")?.asInt ?: 1, units, concepts)
    }

    fun texts(json: String): Map<String, TextEntry> {
        val root = JsonParser.parseString(json).asJsonObject
        val out = LinkedHashMap<String, TextEntry>()
        for ((id, el) in root.entrySet()) {
            val o = el.asJsonObject
            val t = o.get("t")?.asString?.trim().orEmpty()
            if (t.isEmpty()) continue
            out[id] = TextEntry(t, o.stringList("alt"))
        }
        return out
    }

    fun tips(json: String): List<Tip> =
        JsonParser.parseString(json).asJsonArray.map { el ->
            val o = el.asJsonObject
            Tip(
                o.get("unit").asInt,
                o.get("title").asString,
                o.get("body").asString,
                o.stringList("examples")
            )
        }

    fun manifest(json: String): LearnManifest {
        val o = JsonParser.parseString(json).asJsonObject
        return LearnManifest(
            o.get("version")?.asInt ?: 1,
            o.stringList("courses"),
            o.stringList("glossLanguages"),
        )
    }

    private fun JsonObject.stringList(name: String): List<String> =
        getAsJsonArray(name)?.mapNotNull { e -> e.asString.trim().takeIf { it.isNotEmpty() } }
            ?: emptyList()
}

/**
 * A course ready to teach: syllabus + the course language's texts + the learner's meanings.
 * Meanings fall back to English, then to the concept's English source.
 */
class CourseContent(
    val course: String,
    val meaningLanguage: String,
    val syllabus: Syllabus,
    private val targetTexts: Map<String, TextEntry>,
    private val meaningTexts: Map<String, TextEntry>,
    private val fallbackMeanings: Map<String, TextEntry>,
    val tips: List<Tip>,
) {
    private val cache = HashMap<String, StudyItem>()

    fun item(id: String): StudyItem? = cache[id] ?: run {
        val concept = syllabus.concepts[id] ?: return null
        val target = targetTexts[id] ?: return null
        val meaning = meaningTexts[id]?.t ?: fallbackMeanings[id]?.t ?: concept.en
        StudyItem(id, target.t, target.alt, meaning, concept.isSentence).also { cache[id] = it }
    }

    fun items(ids: List<String>): List<StudyItem> = ids.mapNotNull { item(it) }

    fun tipFor(unit: Int): Tip? = tips.firstOrNull { it.unit == unit }

    /** Every item of the course, for picking wrong options. */
    val allItems: List<StudyItem> by lazy { items(syllabus.concepts.keys.toList()) }
}
