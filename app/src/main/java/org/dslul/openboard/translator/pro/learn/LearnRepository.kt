package org.dslul.openboard.translator.pro.learn

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.PhrasesAssetReader
import org.dslul.openboard.translator.pro.learn.engine.ContentParser
import org.dslul.openboard.translator.pro.learn.engine.CourseContent
import org.dslul.openboard.translator.pro.learn.engine.LearnManifest
import org.dslul.openboard.translator.pro.learn.engine.PhraseDeck
import org.dslul.openboard.translator.pro.learn.engine.PhraseTopic
import org.dslul.openboard.translator.pro.learn.engine.Syllabus
import org.dslul.openboard.translator.pro.learn.engine.TextEntry
import org.dslul.openboard.translator.pro.learn.engine.Tip
import java.io.IOException

/**
 * Loads and caches the bundled course data (assets/learn/) and Phrase Practice decks. All reads
 * happen on [Dispatchers.IO]; the files are small (a few hundred KB in total).
 */
object LearnRepository {
    private const val DIR = "learn"

    private var manifest: LearnManifest? = null
    private var syllabus: Syllabus? = null
    private val texts = HashMap<String, Map<String, TextEntry>>()
    private val courses = HashMap<String, CourseContent>()

    /** The app's UI language without region ("pt" for "pt-BR"); the learner's own language. */
    fun appLanguage(context: Context): String =
        Misc.getAppLanguage(context).substringBefore('-').substringBefore('_').lowercase().ifEmpty { "en" }

    suspend fun manifest(context: Context): LearnManifest = withContext(Dispatchers.IO) {
        synchronized(this@LearnRepository) {
            manifest ?: ContentParser.manifest(read(context, "manifest.json")!!).also { manifest = it }
        }
    }

    /** Courses the learner can take: never the one in their own app language. */
    suspend fun availableCourses(context: Context): List<String> {
        val own = appLanguage(context)
        return manifest(context).courses.filter { it != own }
    }

    /**
     * The learner's course, made valid: if they have since switched the app to the language they
     * were learning, move to another course (one they started, else the first available).
     */
    suspend fun currentCourse(context: Context): String? {
        val store = LearnStore.get(context)
        val course = store.course ?: return null
        val available = availableCourses(context)
        if (course in available) return course
        val pick = available.firstOrNull { store.progress(it).isStarted } ?: available.firstOrNull()
        store.course = pick
        return pick
    }

    /** The language meanings are shown in: the app language when there is a file for it, else English. */
    suspend fun meaningLanguage(context: Context, course: String): String {
        val own = appLanguage(context)
        val glosses = manifest(context).glossLanguages
        return if (own in glosses && own != course) own else "en"
    }

    suspend fun course(context: Context, course: String): CourseContent = withContext(Dispatchers.IO) {
        val meaning = meaningLanguage(context, course)
        val key = "$course>$meaning"
        synchronized(this@LearnRepository) {
            courses[key]?.let { return@withContext it }
            val syl = syllabus ?: ContentParser.syllabus(read(context, "concepts.json")!!).also { syllabus = it }
            val content = CourseContent(
                course = course,
                meaningLanguage = meaning,
                syllabus = syl,
                targetTexts = text(context, course),
                meaningTexts = text(context, meaning),
                fallbackMeanings = text(context, "en"),
                tips = tips(context, course),
            )
            courses[key] = content
            content
        }
    }

    private fun text(context: Context, lang: String): Map<String, TextEntry> =
        texts.getOrPut(lang) { read(context, "text/$lang.json")?.let { ContentParser.texts(it) } ?: emptyMap() }

    /** Tips explained in the app language when written, else in English. */
    private fun tips(context: Context, course: String): List<Tip> {
        val own = appLanguage(context)
        val json = read(context, "tips/$own/$course.json") ?: read(context, "tips/en/$course.json")
        return json?.let {
            try {
                ContentParser.tips(it)
            } catch (e: Exception) {
                Log.w(Misc.logKey, "Learn tips for $course unreadable: ${e.message}")
                null
            }
        } ?: emptyList()
    }

    /** Phrase Practice topics: the phrasebook in [source] paired with [target] (58 languages). */
    suspend fun phraseTopics(context: Context, source: String, target: String): List<PhraseTopic> =
        withContext(Dispatchers.IO) {
            PhraseDeck.build(
                PhrasesAssetReader.getLanguageJson(context, source),
                PhrasesAssetReader.getLanguageJson(context, target)
            )
        }

    private fun read(context: Context, path: String): String? = try {
        context.assets.open("$DIR/$path").bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (e: IOException) {
        null
    }
}
