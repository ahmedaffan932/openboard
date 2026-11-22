package org.dslul.openboard.inputmethod.latin.settings

import android.os.Bundle
import android.preference.PreferenceCategory
import android.preference.PreferenceScreen
import android.preference.SwitchPreference
import android.view.inputmethod.InputMethodSubtype
import android.widget.Toast
import org.dslul.openboard.inputmethod.latin.KeyboardLanguages
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.RichInputMethodManager
import org.dslul.openboard.inputmethod.latin.utils.DeviceProtectedUtils
import org.dslul.openboard.inputmethod.latin.utils.SubtypeLocaleUtils
import java.util.Locale

/**
 * The keyboard's own Languages screen ("Languages" in its settings): a switch for every layout.
 * It replaces Android's screen for an IME's languages, which on Samsung did not let the user
 * change anything (client feedback, row 11). The choice is stored in [Settings.PREF_KEYBOARD_LANGUAGES]
 * and read through [RichInputMethodManager], so the language key, its long-press picker and the
 * space bar use it.
 */
class KeyboardLanguagesFragment : SubScreenFragment() {

    /** The enabled layouts' keys, in the order they were turned on. */
    private var enabledKeys: List<String> = emptyList()

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        val context = activity
        RichInputMethodManager.init(context)
        val richImm = RichInputMethodManager.getInstance()

        val screen = preferenceManager.createPreferenceScreen(context)
        screen.setTitle(R.string.select_language)
        preferenceScreen = screen

        // Start from the languages in use now: the in-app list, or Android's before first use.
        enabledKeys = richImm.getMyEnabledInputMethodSubtypeList(true)
            .map { RichInputMethodManager.getKeyboardLanguageKey(it) }
            .distinct()
            .ifEmpty {
                listOf(RichInputMethodManager.getKeyboardLanguageKey(
                    richImm.currentSubtype.rawSubtype))
            }

        val byKey = richImm.allKeyboardSubtypes.associateBy {
            RichInputMethodManager.getKeyboardLanguageKey(it)
        }
        val enabled = enabledKeys.mapNotNull { byKey[it] }
        val others = byKey.filterKeys { it !in enabledKeys }.values
            .sortedBy { displayName(it).lowercase(Locale.getDefault()) }

        addCategory(screen, R.string.keyboard_languages_on, enabled)
        addCategory(screen, R.string.keyboard_languages_all, others)
    }

    private fun addCategory(screen: PreferenceScreen, titleRes: Int, subtypes: List<InputMethodSubtype>) {
        if (subtypes.isEmpty()) return
        val context = activity
        val category = PreferenceCategory(context)
        category.setTitle(titleRes)
        screen.addPreference(category)
        for (subtype in subtypes) {
            val key = RichInputMethodManager.getKeyboardLanguageKey(subtype)
            val switch = SwitchPreference(context)
            switch.isPersistent = false
            switch.title = displayName(subtype)
            switch.isChecked = key in enabledKeys
            switch.setOnPreferenceChangeListener { _, newValue -> onToggled(key, newValue as Boolean) }
            category.addPreference(switch)
        }
    }

    /** Saves the change; refuses to turn off the last language. */
    private fun onToggled(key: String, enabled: Boolean): Boolean {
        val updated = KeyboardLanguages.withToggled(enabledKeys, key, enabled)
        if (updated == null) {
            Toast.makeText(activity, R.string.keyboard_languages_keep_one, Toast.LENGTH_SHORT).show()
            return false
        }
        enabledKeys = updated
        DeviceProtectedUtils.getSharedPreferences(activity).edit()
            .putString(Settings.PREF_KEYBOARD_LANGUAGES, KeyboardLanguages.serialize(updated))
            .apply()
        // Same process as the keyboard: it uses the new list from its next key press.
        RichInputMethodManager.getInstance().refreshSubtypeCaches()
        return true
    }

    private fun displayName(subtype: InputMethodSubtype): String =
        SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)
}
