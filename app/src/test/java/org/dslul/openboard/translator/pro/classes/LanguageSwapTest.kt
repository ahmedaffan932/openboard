package org.dslul.openboard.translator.pro.classes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageSwapTest {

    private val detect = "100"

    @Test
    fun swap_withTranslation_swapsLanguagesAndTexts() {
        val swapped = LanguageSwap.swap(
            LanguageSwap.State(from = "en", to = "es", input = "Good morning", output = "Buenos días"),
            detect,
            detectedFrom = null
        )
        assertEquals(
            LanguageSwap.State(from = "es", to = "en", input = "Buenos días", output = "Good morning"),
            swapped
        )
    }

    @Test
    fun swap_withoutTranslation_swapsLanguagesOnly() {
        val swapped = LanguageSwap.swap(
            LanguageSwap.State(from = "en", to = "fr", input = "Hello", output = ""),
            detect,
            detectedFrom = null
        )
        assertEquals(LanguageSwap.State(from = "fr", to = "en", input = "Hello", output = ""), swapped)
    }

    @Test
    fun swap_blankTranslationCountsAsNone() {
        val swapped = LanguageSwap.swap(
            LanguageSwap.State(from = "en", to = "fr", input = "Hello", output = "   "),
            detect,
            detectedFrom = null
        )
        assertEquals("Hello", swapped!!.input)
        assertEquals("", swapped.output)
    }

    @Test
    fun swap_fromDetect_usesDetectedLanguage() {
        val swapped = LanguageSwap.swap(
            LanguageSwap.State(from = detect, to = "en", input = "Bonjour", output = "Hello"),
            detect,
            detectedFrom = "fr"
        )
        assertEquals(
            LanguageSwap.State(from = "en", to = "fr", input = "Hello", output = "Bonjour"),
            swapped
        )
    }

    @Test
    fun swap_fromDetect_withoutDetection_isRefused() {
        val state = LanguageSwap.State(from = detect, to = "en", input = "Bonjour", output = "Hello")
        assertNull(LanguageSwap.swap(state, detect, detectedFrom = null))
        assertNull(LanguageSwap.swap(state, detect, detectedFrom = ""))
        // "Detect" can never become the target language.
        assertNull(LanguageSwap.swap(state, detect, detectedFrom = detect))
    }
}
