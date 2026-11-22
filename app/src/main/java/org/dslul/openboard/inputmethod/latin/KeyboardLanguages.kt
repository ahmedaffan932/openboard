package org.dslul.openboard.inputmethod.latin

/**
 * The keyboard's own list of enabled languages (layouts), chosen in its in-app "Languages" screen.
 *
 * Android's system screen for an IME's languages could not be used on every phone (on Samsung it
 * would not let the user change anything), so the keyboard keeps its own list in its settings.
 * Each entry is a layout key, "locale:layout" (e.g. "en_US:qwerty"), in the order the user sees
 * them. While the setting has never been written, the system's list is used as before.
 *
 * Plain string logic only, so it is unit tested without Android.
 */
object KeyboardLanguages {
    private const val SEPARATOR = ";"

    /** The key identifying one layout: its locale and keyboard layout set. */
    @JvmStatic
    fun keyOf(locale: String, keyboardLayoutSet: String): String = "$locale:$keyboardLayoutSet"

    /** The stored keys, in order, without blanks or duplicates. */
    @JvmStatic
    fun parse(value: String?): List<String> =
        value.orEmpty().split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    @JvmStatic
    fun serialize(keys: Collection<String>): String = keys.distinct().joinToString(SEPARATOR)

    /**
     * [keys] with [key] turned on (added at the end) or off. Null when that would leave no
     * language at all: the keyboard always needs one.
     */
    @JvmStatic
    fun withToggled(keys: List<String>, key: String, enabled: Boolean): List<String>? {
        val result = if (enabled) (keys + key).distinct() else keys.filter { it != key }
        return result.ifEmpty { null }
    }

    /**
     * The position after [currentIndex] in a list of [size], wrapping around. A current language
     * that is not in the list ([currentIndex] < 0) moves to the first one.
     */
    @JvmStatic
    fun nextIndex(currentIndex: Int, size: Int): Int {
        if (size <= 0) return -1
        if (currentIndex < 0) return 0
        return (currentIndex + 1) % size
    }
}
