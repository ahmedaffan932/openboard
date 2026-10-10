package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Notification analytics, logged straight to Firebase Analytics. Event and parameter names
 * match the TranslatorGuru notification spec (`app_noti_show`, `app_noti_skip`,
 * `app_entry_open`), so the same dashboards read both apps.
 */
object NotiAnalytics {

    const val SOURCE_NOTIFICATION = "notification"
    const val SOURCE_PINNED_NOTI = "pinned_noti"
    const val SOURCE_SHORTCUT = "shortcut"
    const val SOURCE_WIDGET = "widget"

    const val REASON_STILL_SHOWING = "still_showing"

    fun notiShow(context: Context, notiType: String, replaced: Boolean = false) =
        log(context, "app_noti_show", Bundle().apply {
            putString("noti_type", notiType)
            putString("replaced", replaced.toString())
        })

    fun notiSkip(context: Context, notiType: String, reason: String) =
        log(context, "app_noti_skip", Bundle().apply {
            putString("noti_type", notiType)
            putString("reason", reason)
        })

    /** A notification tap that reached NotiRouterActivity, or a pinned-notification tile tap. */
    fun entryOpen(context: Context, source: String, target: String) =
        log(context, "app_entry_open", Bundle().apply {
            putString("source", source)
            putString("target", target)
        })

    private fun log(context: Context, event: String, params: Bundle) {
        try {
            FirebaseAnalytics.getInstance(context.applicationContext).logEvent(event, params)
        } catch (e: Exception) {
            // Analytics must never break a notification post (e.g. Firebase not initialised in a
            // cold alarm process on some OEM builds).
            e.printStackTrace()
        }
    }
}
