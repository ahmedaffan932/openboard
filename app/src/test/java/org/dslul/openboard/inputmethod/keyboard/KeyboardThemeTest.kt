package org.dslul.openboard.inputmethod.keyboard

import android.content.SharedPreferences
import org.dslul.openboard.inputmethod.latin.R
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardThemeTest {

    /** Empty preferences: no theme was ever saved. */
    private object NoPrefs : SharedPreferences {
        override fun getAll(): Map<String, *> = emptyMap<String, Any>()
        override fun getString(key: String?, defValue: String?): String? = defValue
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = defValues
        override fun getInt(key: String?, defValue: Int) = defValue
        override fun getLong(key: String?, defValue: Long) = defValue
        override fun getFloat(key: String?, defValue: Float) = defValue
        override fun getBoolean(key: String?, defValue: Boolean) = defValue
        override fun contains(key: String?) = false
        override fun edit(): SharedPreferences.Editor = throw UnsupportedOperationException()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
    }

    private fun styleOf(themeId: Int) =
        KeyboardTheme.searchKeyboardThemeById(themeId, KeyboardTheme.KEYBOARD_THEMES).mStyleId

    @Test
    fun darkThemesUseDarkStyles() {
        // They pointed at the Light styles, so choosing "Dark" never gave a dark keyboard.
        assertEquals(R.style.KeyboardTheme_LXX_Dark, styleOf(KeyboardTheme.THEME_ID_LXX_DARK))
        assertEquals(
            R.style.KeyboardTheme_LXX_Dark_Amoled,
            styleOf(KeyboardTheme.THEME_ID_LXX_DARK_AMOLED)
        )
        assertEquals(
            R.style.KeyboardTheme_LXX_Dark_Border,
            styleOf(KeyboardTheme.THEME_ID_LXX_DARK_BORDER)
        )
        assertEquals(R.style.KeyboardTheme_LXX_Light, styleOf(KeyboardTheme.THEME_ID_LXX_LIGHT))
    }

    @Test
    fun unsetThemeKeepsTheLookUsersAlreadyHad() {
        // Users who never picked a theme got Dark-with-borders, which was drawn with the
        // Light-with-borders style. They must keep seeing light with borders.
        for (sdk in listOf(27, 31, 36)) {
            val theme = KeyboardTheme.getDefaultKeyboardTheme(NoPrefs, sdk, KeyboardTheme.KEYBOARD_THEMES)
            assertEquals(KeyboardTheme.THEME_ID_LXX_LIGHT_BORDER, theme.mThemeId)
            assertEquals(R.style.KeyboardTheme_LXX_Light_Border, theme.mStyleId)
        }
    }
}
