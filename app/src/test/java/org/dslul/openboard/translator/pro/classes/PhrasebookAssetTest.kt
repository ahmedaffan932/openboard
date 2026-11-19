package org.dslul.openboard.translator.pro.classes

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

/**
 * Checks the shipped phrasebook data (assets/translated-phrases.zip).
 *
 * The app pairs each phrase with its translation by position, so every language file must have the
 * same categories, subcategories and number of phrases as English, and no duplicate keys (a JSON
 * parser keeps only the last of two equal keys, which silently drops a phrase and shifts all the
 * rows after it, as happened in bn.json).
 */
class PhrasebookAssetTest {

    /** category -> subcategory -> phrases, in file order, failing on any duplicate key. */
    private fun readStrict(json: String, name: String): List<List<Int>> {
        val reader = JsonReader(StringReader(json))
        val categories = ArrayList<List<Int>>()
        reader.beginObject()
        val categoryKeys = HashSet<String>()
        while (reader.hasNext()) {
            val category = reader.nextName()
            if (!categoryKeys.add(category)) fail("$name: duplicate category key \"$category\"")
            val subcategories = ArrayList<Int>()
            reader.beginObject()
            val subKeys = HashSet<String>()
            while (reader.hasNext()) {
                val sub = reader.nextName()
                if (!subKeys.add(sub)) fail("$name: duplicate subcategory key \"$sub\"")
                var phrases = 0
                reader.beginObject()
                val phraseKeys = HashSet<String>()
                while (reader.hasNext()) {
                    val key = reader.nextName()
                    if (!phraseKeys.add(key)) fail("$name: duplicate phrase key \"$key\" in \"$sub\"")
                    assertEquals(JsonToken.STRING, reader.peek())
                    reader.nextString()
                    phrases++
                }
                reader.endObject()
                subcategories.add(phrases)
            }
            reader.endObject()
            categories.add(subcategories)
        }
        reader.endObject()
        return categories
    }

    @Test
    fun everyLanguageMatchesEnglishStructure_withNoDuplicateKeys() {
        ZipFile(File("src/main/assets/translated-phrases.zip")).use { zip ->
            val languageEntries = zip.entries().toList()
                .filter { it.name.endsWith(".json") && !it.name.endsWith("sorted_word.json") }
            assertTrue("no phrasebook files found", languageEntries.size > 50)

            fun read(entryName: String) =
                zip.getInputStream(zip.getEntry(entryName)).readBytes().toString(Charsets.UTF_8)

            val english = readStrict(read("translated-phrases/en.json"), "en.json")
            for (entry in languageEntries) {
                val structure = readStrict(read(entry.name), entry.name)
                assertEquals("${entry.name}: phrase counts differ from English", english, structure)
            }
        }
    }
}
