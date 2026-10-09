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
}
