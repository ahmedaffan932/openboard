package org.dslul.openboard.translator.pro.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import org.dslul.openboard.translator.pro.notifications.NotiConfig

/**
 * When the "Add widget" card ([WidgetPromptDialog]) is due.
 *
 * Shown once, and never on the very first dashboard visit: that visit already carries the
 * POST_NOTIFICATIONS request, so the card would stack on the system dialog. The first visit only
 * arms it; the first visit after that shows it. A persisted flag (not an intent extra) because a
 * launch that names another screen skips the dashboard, and the card then waits for a real visit.
 */
object WidgetPrompt {
    private const val PREFS = "widgetPrompt"
    private const val KEY_ARMED = "armed"
    private const val KEY_SHOWN = "shown"

    /**
     * Shows the card over [activity] if it is due. Call from the dashboard's onCreate with no saved
     * state: the card's window then takes focus before the dashboard's own ad slots load.
     */
    fun showIfDue(activity: FragmentActivity) {
        val p = prefs(activity)
        if (p.getBoolean(KEY_SHOWN, false)) return
        if (!NotiConfig.widgetPromptEnabled) return
        if (isWidgetOnHomeScreen(activity)) {
            // Nothing to ask for any more.
            p.edit { putBoolean(KEY_SHOWN, true) }
            return
        }
        if (!p.getBoolean(KEY_ARMED, false)) {
            // First dashboard visit: arm now, show on the next one (keeps clear of the
            // notification-permission dialog this visit may raise).
            p.edit { putBoolean(KEY_ARMED, true) }
            return
        }
        val fragments = activity.supportFragmentManager
        if (fragments.isStateSaved || fragments.findFragmentByTag(WidgetPromptDialog.TAG) != null) return
        // Mark shown first, so a killed process never shows it twice.
        p.edit { putBoolean(KEY_SHOWN, true) }
        WidgetPromptDialog().show(fragments, WidgetPromptDialog.TAG)
    }

    /** True once any launcher holds one of this app's widgets. */
    fun isWidgetOnHomeScreen(context: Context): Boolean = try {
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, TranslateWidgetProvider::class.java))
            .isNotEmpty()
    } catch (e: Exception) {
        false
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
