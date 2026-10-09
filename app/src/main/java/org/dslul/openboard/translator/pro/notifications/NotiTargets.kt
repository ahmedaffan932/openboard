package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.content.Intent
import org.dslul.openboard.translator.pro.FragmentsDashboardActivity
import org.dslul.openboard.translator.pro.TranslateActivity
import org.dslul.openboard.translator.pro.UninstallSurveyActivity
import org.dslul.openboard.translator.pro.classes.Misc

/**
 * Feature screens a notification can deep-link to (spec: "Deep-link → feature screen").
 *
 * Camera, conversation and phrasebook open as the dashboard's own tabs rather than as their
 * standalone activities: camera and conversation are premium features there (upgrade or watch a
 * rewarded ad), and a notification must not be a way around that. OCRActivity is absent - it
 * reads a static CameraMisc.fileUri and is not cold-start safe.
 */
object NotiTargets {
    const val TRANSLATE = "translate"
    const val CAMERA = "camera"
    const val CONVERSATION = "conversation"
    const val PHRASEBOOK = "phrasebook"

    /** The "Uninstall" long-press shortcut's retention survey (UninstallSurveyActivity). */
    const val UNINSTALL = "uninstall"

    /** The dashboard, where typing, voice and camera sit side by side. */
    const val HOME = "home"

    /**
     * The Translate screen with a given text (NotiRouterActivity.EXTRA_TEXT), translated and read
     * aloud as soon as it opens - the Daily notification's "hear it now".
     */
    const val PHRASE = "phrase"

    /**
     * [fromEntry]: the screen is reached through a notification entry - the splash already showed
     * its ad, or the user tapped while inside the app - so the Translate screen skips its own
     * entry interstitial.
     */
    fun intentFor(
        context: Context,
        target: String?,
        fromEntry: Boolean = false,
        text: String? = null,
    ): Intent =
        when (target) {
            PHRASE -> Intent(context, TranslateActivity::class.java)
                .putExtra(TranslateActivity.EXTRA_DASHBOARD_ON_BACK, true)
                .apply {
                    if (!text.isNullOrBlank()) {
                        putExtra(Misc.key, text)
                        putExtra(TranslateActivity.EXTRA_SPEAK_RESULT, true)
                    }
                }
            TRANSLATE -> Intent(context, TranslateActivity::class.java)
                .putExtra(TranslateActivity.EXTRA_DASHBOARD_ON_BACK, true)
            CAMERA -> dashboard(context, FragmentsDashboardActivity.TAB_CAMERA)
            CONVERSATION -> dashboard(context, FragmentsDashboardActivity.TAB_CHAT)
            PHRASEBOOK -> dashboard(context, FragmentsDashboardActivity.TAB_PHRASEBOOK)
            UNINSTALL -> Intent(context, UninstallSurveyActivity::class.java)
            else -> dashboard(context, null)
        }.putExtra(TranslateActivity.EXTRA_SKIP_ENTRY_AD, fromEntry)

    /**
     * Reuses a live dashboard instead of stacking a second one on the app's task (these taps can
     * land while the app is on screen: 2004 is posted from inside it).
     */
    fun dashboard(context: Context, tab: String?): Intent =
        Intent(context, FragmentsDashboardActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { if (tab != null) putExtra(FragmentsDashboardActivity.EXTRA_OPEN_TAB, tab) }
}
