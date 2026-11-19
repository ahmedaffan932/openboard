package org.dslul.openboard.translator.pro.classes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordGameContentTest {

    @Test
    fun parse_readsWordsAndMeaningsInOrder() {
        val json = """
            {"data":[
              {"word":"air","meaning":"the mixture of gases that surrounds the Earth"},
              {"word":"art","meaning":"the activity of making objects that are beautiful"}
            ]}
        """.trimIndent()

        val list = WordGameContent.parse(json)

        assertEquals(2, list.data.size)
        assertEquals("air", list.data[0].word)
        assertEquals("the mixture of gases that surrounds the Earth", list.data[0].meaning)
        assertEquals("art", list.data[1].word)
        assertEquals("the activity of making objects that are beautiful", list.data[1].meaning)
    }

    @Test
    fun parse_emptyDataArray_yieldsEmptyList() {
        val list = WordGameContent.parse("""{"data":[]}""")
        assertTrue(list.data.isEmpty())
    }

    @Test(expected = Exception::class)
    fun parse_malformedJson_throws() {
        WordGameContent.parse("not valid json {")
    }

    @Test
    fun parse_wrongShapeJson_yieldsEmptyListNotNpe() {
        // Gson can build a WordList with a null `data` for these; parse() must normalize
        // to an empty list so the caller shows the "error fetching words" path, not a crash.
        assertTrue(WordGameContent.parse("{}").data.isEmpty())
        assertTrue(WordGameContent.parse("""{"data":null}""").data.isEmpty())
        assertTrue(WordGameContent.parse("null").data.isEmpty())
    }

    @Test
    fun parseAlternates_readsMap() {
        val map = WordGameContent.parseAlternates("""{"art":["rat","tar"],"low":["owl"]}""")
        assertEquals(listOf("rat", "tar"), map["art"])
        assertEquals(listOf("owl"), map["low"])
    }

    @Test
    fun parseAlternates_wrongShapeYieldsEmptyMap() {
        assertTrue(WordGameContent.parseAlternates("null").isEmpty())
    }

    @Test
    fun shippedAlternatesAsset_acceptsTheReportedWords() {
        // Unit tests run with the module directory as the working directory.
        val json = java.io.File("src/main/assets/wordgame_alternates.json").readText()
        val map = WordGameContent.parseAlternates(json)
        assertTrue(map["art"]!!.containsAll(listOf("rat", "tar")))
        assertTrue(map["low"]!!.contains("owl"))
        // Every entry is lowercase a-z, the way the game compares guesses.
        for ((word, others) in map) {
            assertTrue(word, word.matches(Regex("[a-z]+")))
            for (other in others) {
                assertTrue(other, other.matches(Regex("[a-z]+")))
                assertEquals(word.toList().sorted(), other.toList().sorted())
            }
        }
    }
}
