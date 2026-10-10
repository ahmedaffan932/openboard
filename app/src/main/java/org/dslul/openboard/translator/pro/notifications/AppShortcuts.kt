package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.PreSplashScreenActivity

/**
 * App-icon long-press shortcuts (Noti.pdf §5): camera, conversation and the red "Uninstall" entry,
 * each opening its tool through the splash. The whole set is behind NotiConfig.shortcutsEnabled
 * (RC `notiShortcutsEnabled`); the Uninstall entry has its own switch on top
 * (NotiConfig.uninstallShortcutEnabled / RC `notiUninstallShortcutEnabled`).
 *
 * ⚠ Play-policy note (flagged in NOTIFICATIONS.md): a shortcut labelled "Uninstall" that opens the
 * app (a retention survey, UninstallSurveyActivity) rather than uninstalling is a deceptive
 * pattern; implemented per client spec, with its own remote kill switch.
 *
 * Launcher-started shortcut intents must target an exported activity, so these route through
 * PreSplashScreenActivity (LAUNCHER, exported) with the standard notiTarget extra — the same
 * splash → ad → feature-screen flow as notification taps.
 */
object AppShortcuts {

    fun setup(context: Context) {
        try {
            if (!NotiConfig.shortcutsEnabled) {
                ShortcutManagerCompat.removeAllDynamicShortcuts(context)
                return
            }
            val shortcuts = mutableListOf(
                shortcut(
                    context, "camera", R.string.shortcut_camera,
                    R.drawable.ic_baseline_camera_alt_24, NotiTargets.CAMERA
                ),
                shortcut(
                    context, "conversation", R.string.shortcut_conversation,
                    R.drawable.ic_baseline_mic_selected_24, NotiTargets.CONVERSATION
                )
            )
            if (NotiConfig.uninstallShortcutEnabled) {
                shortcuts += shortcut(
                    context, "uninstall", R.string.shortcut_uninstall,
                    R.drawable.ic_shortcut_uninstall, NotiTargets.UNINSTALL
                )
            }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (e: Exception) {
            // Never let launcher-shortcut plumbing break app start (e.g. the keyboard starting the
            // process before the user unlocked the phone, when ShortcutManager refuses calls).
            e.printStackTrace()
        }
    }

    private fun shortcut(
        context: Context,
        id: String,
        labelRes: Int,
        iconRes: Int,
        target: String,
    ): ShortcutInfoCompat {
        // CLEAR_TASK as well as NEW_TASK: the launcher supplies NEW_TASK on its own, but without
        // CLEAR_TASK a task left over from an earlier session is simply brought forward and the
        // splash — with it the ad and the routing that reads EXTRA_TARGET — never runs.
        // The action is only there because a shortcut refuses an intent without one.
        val intent = Intent(context, PreSplashScreenActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(NotiRouterActivity.EXTRA_TARGET, target)
            .putExtra(NotiRouterActivity.EXTRA_SOURCE, NotiRouterActivity.SOURCE_SHORTCUT)
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(context.getString(labelRes))
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setIntent(intent)
            .build()
    }
}
