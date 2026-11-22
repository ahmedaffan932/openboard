package org.dslul.openboard.inputmethod.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardLanguagesTest {

    @Test
    fun keyCombinesLocaleAndLayout() {
        assertEquals("en_US:qwerty", KeyboardLanguages.keyOf("en_US", "qwerty"))
    }

    @Test
    fun parseKeepsOrderAndDropsBlanksAndDuplicates() {
        assertEquals(
            listOf("bn_BD:bengali", "en_US:qwerty"),
            KeyboardLanguages.parse(" bn_BD:bengali ;;en_US:qwerty;bn_BD:bengali;")
        )
        assertEquals(emptyList<String>(), KeyboardLanguages.parse(null))
        assertEquals(emptyList<String>(), KeyboardLanguages.parse(""))
    }

    @Test
    fun serializeRoundTrips() {
        val keys = listOf("en_US:qwerty", "de:qwertz", "ar:arabic")
        assertEquals(keys, KeyboardLanguages.parse(KeyboardLanguages.serialize(keys)))
    }

    @Test
    fun turningOnAddsAtTheEndOnce() {
        val keys = listOf("en_US:qwerty")
        assertEquals(listOf("en_US:qwerty", "bn_BD:bengali"),
            KeyboardLanguages.withToggled(keys, "bn_BD:bengali", true))
        assertEquals(keys, KeyboardLanguages.withToggled(keys, "en_US:qwerty", true))
    }

    @Test
    fun turningOffRemovesButNeverTheLastOne() {
        val keys = listOf("en_US:qwerty", "bn_BD:bengali")
        assertEquals(listOf("bn_BD:bengali"),
            KeyboardLanguages.withToggled(keys, "en_US:qwerty", false))
        assertNull(KeyboardLanguages.withToggled(listOf("en_US:qwerty"), "en_US:qwerty", false))
    }

    @Test
    fun nextIndexWrapsAndStartsFromAnUnknownLanguage() {
        assertEquals(1, KeyboardLanguages.nextIndex(0, 3))
        assertEquals(0, KeyboardLanguages.nextIndex(2, 3))
        assertEquals(0, KeyboardLanguages.nextIndex(0, 1))
        assertEquals(0, KeyboardLanguages.nextIndex(-1, 2))
        assertEquals(-1, KeyboardLanguages.nextIndex(0, 0))
    }
}
