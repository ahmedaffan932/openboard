package org.dslul.openboard.translator.pro.classes

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

/**
 * Word-game content: the pre-sorted (short/easy -> long/hard) English word list the
 * puzzle draws from.
 *
 * The words ship inside the app's existing `translated-phrases.zip` asset as the entry
 * `translated-phrases/sorted_word.json`, shaped `{ "data": [ { "word", "meaning" }, ... ] }`,
 * so the game needs no network to start. Parsing uses Gson against the existing
 * [WordList]/[Data] models — chosen over `org.json` so that [parse] stays unit-testable
 * off-device (android.jar's `org.json` is stubbed in plain JVM unit tests).
 */
object WordGameContent {
    private const val ZIP_ASSET = "translated-phrases.zip"
    private const val ENTRY_NAME = "translated-phrases/sorted_word.json"

    /** Reads and parses the bundled word list. Throws if the asset entry is missing. */
    suspend fun load(context: Context): WordList = withContext(Dispatchers.IO) {
        parse(readEntry(context))
    }

    /**
     * Pure JSON -> [WordList]. Separated out so it can be unit-tested without assets.
     *
     * Gson instantiates via unsafe allocation and ignores Kotlin's non-null types, so a
     * syntactically-valid but wrong-shape payload (`{}`, `{"data":null}`, or `null`) yields
     * a `WordList` whose `data` is null (or a null `WordList`) WITHOUT throwing. Normalize
     * that to an empty list here so callers take the "no words" path instead of an NPE.
     */
    fun parse(json: String): WordList {
        val parsed: WordList? = Gson().fromJson(json, WordList::class.java)
        return WordList(parsed?.data ?: arrayListOf())
    }

    private fun readEntry(context: Context): String {
        context.assets.open(ZIP_ASSET).use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name == ENTRY_NAME) {
                        return zip.readBytes().toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        throw IllegalStateException("Word-game asset entry not found: $ENTRY_NAME")
    }
}
