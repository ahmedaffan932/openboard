package org.dslul.openboard.translator.pro.classes

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrParagraphsTest {

    @Test
    fun joinLines_joinsWrappedSentenceWithSpaces() {
        assertEquals(
            "The museum opens at nine and closes at five.",
            OcrParagraphs.joinLines(listOf("The museum opens at", "nine and closes", "at five."))
        )
    }

    @Test
    fun joinLines_rejoinsHyphenatedWord() {
        assertEquals(
            "Please read the translation carefully.",
            OcrParagraphs.joinLines(listOf("Please read the transla-", "tion carefully."))
        )
    }

    @Test
    fun joinLines_keepsRealHyphenBeforeCapitalOrNumber() {
        // Not a split word: the next line starts with a capital.
        assertEquals("North- South", OcrParagraphs.joinLines(listOf("North-", "South")))
        assertEquals("pages 10- 12", OcrParagraphs.joinLines(listOf("pages 10-", "12")))
    }

    @Test
    fun joinLines_noSeparatorForChineseJapanese() {
        assertEquals("今日は良い天気です", OcrParagraphs.joinLines(listOf("今日は良い", "天気です"), ""))
    }

    @Test
    fun joinLines_skipsBlankLinesAndTrims() {
        assertEquals("a b", OcrParagraphs.joinLines(listOf("  a ", "", "   ", " b")))
    }

    @Test
    fun build_oneParagraphPerBlock_skippingEmptyBlocks() {
        val paragraphs = OcrParagraphs.build(
            listOf(listOf("First block", "continues"), listOf(" "), listOf("Second"))
        )
        assertEquals(listOf("First block continues", "Second"), paragraphs)
    }

    @Test
    fun dominantRotation_snapsToQuarterTurns() {
        assertEquals(0, OcrParagraphs.dominantRotation(listOf(2f, -3f, 1f), listOf(20, 20, 20)))
        assertEquals(90, OcrParagraphs.dominantRotation(listOf(88f, 91f, 93f), listOf(20, 20, 20)))
        assertEquals(270, OcrParagraphs.dominantRotation(listOf(-89f, -92f), listOf(20, 20)))
        assertEquals(180, OcrParagraphs.dominantRotation(listOf(179f, -178f), listOf(20, 20)))
    }

    @Test
    fun dominantRotation_longLinesOutvoteShortOnes() {
        // Two short upright labels against one long sideways paragraph line.
        assertEquals(90, OcrParagraphs.dominantRotation(listOf(0f, 0f, 90f), listOf(3, 4, 60)))
    }

    @Test
    fun dominantRotation_noLines_isZero() {
        assertEquals(0, OcrParagraphs.dominantRotation(emptyList(), emptyList()))
    }
}
