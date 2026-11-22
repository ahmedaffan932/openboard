package org.dslul.openboard.inputmethodcommon

import android.content.Context
import android.graphics.drawable.Drawable
import android.preference.Preference
import android.preference.PreferenceScreen
import android.text.TextUtils
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import org.dslul.openboard.inputmethod.latin.RichInputMethodManager
import org.dslul.openboard.inputmethod.latin.settings.KeyboardLanguagesFragment
import org.dslul.openboard.inputmethod.latin.utils.SubtypeLocaleUtils

/* package private */
internal class InputMethodSettingsImpl : InputMethodSettingsInterface {
    private var mSubtypeEnablerPreference: Preference? = null
    private var mInputMethodSettingsCategoryTitleRes = 0
    private var mInputMethodSettingsCategoryTitle: CharSequence? = null
    private var mSubtypeEnablerTitleRes = 0
    private var mSubtypeEnablerTitle: CharSequence? = null
    private var mSubtypeEnablerIconRes = 0
    private var mSubtypeEnablerIcon: Drawable? = null
    private var mImm: InputMethodManager? = null
    private var mImi: InputMethodInfo? = null
    /**
     * Initialize internal states of this object.
     * @param context the context for this application.
     * @param prefScreen a PreferenceScreen of PreferenceActivity or PreferenceFragment.
     * @return true if this application is an IME and has two or more subtypes, false otherwise.
     */
    fun init(context: Context, prefScreen: PreferenceScreen): Boolean {
        mImm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mImi = getMyImi(context, mImm)
        if (mImi == null || mImi!!.subtypeCount <= 1) {
            return false
        }
        mSubtypeEnablerPreference = Preference(context)
        // Opens the keyboard's own Languages screen. It used to open Android's screen for an IME's
        // languages, which on Samsung did not let the user change anything (client feedback, row
        // 11); the settings activity starts this fragment like its other sub-screens.
        mSubtypeEnablerPreference!!.fragment = KeyboardLanguagesFragment::class.java.name
        prefScreen.addPreference(mSubtypeEnablerPreference)
        updateSubtypeEnabler()
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun setInputMethodSettingsCategoryTitle(resId: Int) {
        mInputMethodSettingsCategoryTitleRes = resId
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setInputMethodSettingsCategoryTitle(title: CharSequence?) {
        mInputMethodSettingsCategoryTitleRes = 0
        mInputMethodSettingsCategoryTitle = title
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerTitle(resId: Int) {
        mSubtypeEnablerTitleRes = resId
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerTitle(title: CharSequence?) {
        mSubtypeEnablerTitleRes = 0
        mSubtypeEnablerTitle = title
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerIcon(resId: Int) {
        mSubtypeEnablerIconRes = resId
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerIcon(drawable: Drawable?) {
        mSubtypeEnablerIconRes = 0
        mSubtypeEnablerIcon = drawable
        updateSubtypeEnabler()
    }

    fun updateSubtypeEnabler() {
        val pref = mSubtypeEnablerPreference ?: return
        val context = pref.context
        val title: CharSequence?
        title = if (mSubtypeEnablerTitleRes != 0) {
            context.getString(mSubtypeEnablerTitleRes)
        } else {
            mSubtypeEnablerTitle
        }
        pref.title = title
        val summary = getEnabledSubtypesLabel(context)
        if (!TextUtils.isEmpty(summary)) {
            pref.summary = summary
        }
        if (mSubtypeEnablerIconRes != 0) {
            pref.setIcon(mSubtypeEnablerIconRes)
        } else {
            pref.icon = mSubtypeEnablerIcon
        }
    }

    companion object {

        private fun getMyImi(context: Context, imm: InputMethodManager?): InputMethodInfo? {
            val imis = imm!!.inputMethodList
            for (i in imis.indices) {
                val imi = imis[i]
                if (imis[i].packageName == context.packageName) {
                    return imi
                }
            }
            return null
        }

        /** The keyboard's enabled languages (its own list once the user has set one), by name. */
        private fun getEnabledSubtypesLabel(context: Context): String {
            RichInputMethodManager.init(context)
            return RichInputMethodManager.getInstance().getMyEnabledInputMethodSubtypeList(true)
                .joinToString(", ") { SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(it) }
        }
    }
}