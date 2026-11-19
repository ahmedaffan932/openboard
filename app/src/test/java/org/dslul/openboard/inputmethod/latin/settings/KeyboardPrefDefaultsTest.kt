package org.dslul.openboard.inputmethod.latin.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The keyboard settings screen and the keyboard code must use the same default for each switch.
 * They disagreed for "Sound on keypress", "Emoji key" and "Language switch key": the screen showed
 * one value, the keyboard used the other, and merely opening the screen saved the screen's
 * defaults and changed the keyboard. Both now read one `@bool` resource; this test keeps it that way.
 *
 * Plain-JVM test: reads the source files (the working directory is the app module).
 */
class KeyboardPrefDefaultsTest {

    private val prefsXml = File("src/main/res/xml/prefs_screen_preferences.xml").readText()
    private val code = File("src/main/java/org/dslul/openboard/inputmethod/latin/settings/SettingsValues.java").readText() +
        File("src/main/java/org/dslul/openboard/inputmethod/latin/settings/Settings.java").readText()

    /** The android:defaultValue of the preference with [key]. */
    private fun xmlDefault(key: String): String {
        val element = Regex("<[A-Za-z.]+[^>]*android:key=\"$key\"[^>]*>", RegexOption.DOT_MATCHES_ALL)
            .find(prefsXml)?.value ?: error("no preference $key")
        return Regex("android:defaultValue=\"([^\"]+)\"").find(element)?.groupValues?.get(1)
            ?: error("$key has no default")
    }

    private fun assertSharedDefault(key: String, prefConstant: String, boolName: String) {
        assertEquals(key, "@bool/$boolName", xmlDefault(key))
        val read = Regex(
            "getBoolean\\(\\s*(Settings\\.)?$prefConstant,\\s*res\\.getBoolean\\(\\s*R\\.bool\\.$boolName\\)"
        )
        assertTrue("$prefConstant is not read with R.bool.$boolName", read.containsMatchIn(code))
    }

    @Test
    fun soundOnKeypress() =
        assertSharedDefault("sound_on", "PREF_SOUND_ON", "config_default_sound_enabled")

    @Test
    fun vibrateOnKeypress() =
        assertSharedDefault("vibrate_on", "PREF_VIBRATE_ON", "config_default_vibration_enabled")

    @Test
    fun numberRow() = assertSharedDefault(
        "pref_show_number_row", "PREF_SHOW_NUMBER_ROW", "config_default_show_number_row"
    )

    @Test
    fun emojiKey() = assertSharedDefault(
        "pref_show_emoji_key", "PREF_SHOW_EMOJI_KEY", "config_default_show_emoji_key"
    )

    @Test
    fun languageSwitchKey() = assertSharedDefault(
        "pref_show_language_switch_key",
        "PREF_SHOW_LANGUAGE_SWITCH_KEY",
        "config_default_show_language_switch_key"
    )
}
