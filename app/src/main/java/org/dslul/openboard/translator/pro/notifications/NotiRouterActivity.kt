package org.dslul.openboard.translator.pro.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.translator.pro.PreSplashScreenActivity
import org.dslul.openboard.translator.pro.TranslateActivity
import org.dslul.openboard.translator.pro.classes.App
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG

/**
 * Tap target for every notification (transparent, finishes immediately).
 *
 * A tap while the app is on screen opens the feature on the live task. Every other tap starts a
 * session through PreSplashScreenActivity - its app-open ad is the spec's "splash ad" - which
 * reads [EXTRA_TARGET] and goes straight to the feature instead of the premium / language screens.
 *
 * What is left here is the cleanup a `PendingIntent` cannot do on its own: dismissing the
 * notification that was tapped, disarming the lockscreen re-wake, and recording the entry.
 *
 * Note: this is an activity trampoline, which Android 12+ still permits (the notification-trampoline
 * restriction covers services and receivers only). Excluded from the resume app-open ad (App).
 */
class NotiRouterActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET = "notiTarget"
        const val EXTRA_NOTI_ID = "notiRouterNotiId"
        const val EXTRA_SOURCE = "notiRouterSource"

        /** Optional text for the target screen (NotiTargets.PHRASE); rides the splash passthrough. */
        const val EXTRA_TEXT = "notiRouterText"
        const val SOURCE_NOTI = "noti"

        /** A launcher shortcut reaches the splash directly, so it stamps this itself. */
        const val SOURCE_SHORTCUT = "shortcut"

        /** A home-screen widget tile (TranslateWidgetProvider). */
        const val SOURCE_WIDGET = "widget"

        private const val REQUEST_BASE = 4000
        private const val WIDGET_REQUEST_BASE = 4200

        fun pendingIntent(
            context: Context,
            target: String,
            notiId: Int,
            text: String? = null,
        ): PendingIntent {
            val intent = Intent(context, NotiRouterActivity::class.java)
                .putExtra(EXTRA_TARGET, target)
                .putExtra(EXTRA_NOTI_ID, notiId)
                .apply { if (text != null) putExtra(EXTRA_TEXT, text) }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                context,
                REQUEST_BASE + notiId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * A home-screen widget tile. No notification id (the widget stays put), and [SOURCE_WIDGET]
         * so the router and analytics tell it apart from a notification. [slot] keeps each tile's
         * PendingIntent distinct.
         */
        fun widgetPendingIntent(context: Context, target: String, slot: Int): PendingIntent {
            val intent = Intent(context, NotiRouterActivity::class.java)
                .putExtra(EXTRA_TARGET, target)
                .putExtra(EXTRA_SOURCE, SOURCE_WIDGET)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                context,
                WIDGET_REQUEST_BASE + slot,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Snapshot before super.onCreate(): this Activity's own start is what would push the
        // process observer to STARTED, so reading it afterwards would always say "on screen".
        val appOnScreen = App.isAppOnScreen()
        super.onCreate(savedInstanceState)

        val target = intent.getStringExtra(EXTRA_TARGET) ?: ""
        val notiId = intent.getIntExtra(EXTRA_NOTI_ID, -1)
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: SOURCE_NOTI
        val text = intent.getStringExtra(EXTRA_TEXT)

        // Spec: opening the app dismisses the notification, and the re-wake belongs to it.
        if (notiId != -1) {
            NotiPoster.cancel(this, notiId)
            ScreenWakeController.disarmRewake(this)
        }

        val analyticsSource = when (source) {
            SOURCE_WIDGET -> NotiAnalytics.SOURCE_WIDGET
            SOURCE_SHORTCUT -> NotiAnalytics.SOURCE_SHORTCUT
            else -> NotiAnalytics.SOURCE_NOTIFICATION
        }
        NotiAnalytics.entryOpen(this, analyticsSource, target)

        if (appOnScreen && !Misc.isFirstTime(this) && !Misc.isSplashScreen) {
            startActivity(NotiTargets.intentFor(this, target, fromEntry = true, text = text).apply {
                // The existing task already owns the Back destination.
                removeExtra(TranslateActivity.EXTRA_DASHBOARD_ON_BACK)
            })
            finish()
            return
        }

        Misc.isSplashScreen = true
        val splash = Intent(this, PreSplashScreenActivity::class.java)
            .apply {
                if (target.isNotBlank()) putExtra(EXTRA_TARGET, target)
                if (text != null) putExtra(EXTRA_TEXT, text)
            }
            .putExtra(EXTRA_SOURCE, source)
            // From the tray: clearing is what makes it genuinely start a session rather than
            // resurface the screen the user left. But the on-open reminder is posted while the
            // user is *inside* the app, and clearing there would destroy the translation or
            // conversation they are in the middle of (first open, or the splash still running),
            // so that one case narrows the flags and the splash stacks on the live task.
            .addFlags(
                if (appOnScreen) Intent.FLAG_ACTIVITY_NEW_TASK
                else Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        startActivity(splash)
        Log.d(TAG, "NotiRouter: target=$target source=$source onScreen=$appOnScreen -> splash")
        finish()
    }
}
