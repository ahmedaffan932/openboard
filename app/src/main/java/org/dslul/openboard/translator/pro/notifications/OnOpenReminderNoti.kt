package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.notifications.receivers.NotiDismissReceiver

/**
 * Shortcut / Reminder notification (Noti.pdf §3, Figma NotiShortcut): posted right when the user
 * opens the app so a quick way back stays in the tray. Channel Reminders ("no sound, no peek" -
 * the channel is created LOW to honor that).
 *
 * A [NotiTextCard]: standard collapsed row (title + copy, as the frame's header row shows), and
 * expanded the frame's LATER | TRY NOW row. No image: the frame has none. TRY NOW (and the row
 * itself) open the dashboard - "type, speak or point your camera".
 */
object OnOpenReminderNoti {

    const val NOTI_ID = 2004

    private const val MIN_INTERVAL_MS = 15 * 60 * 1000L

    fun show(context: Context) {
        if (!NotiConfig.onOpenEnabled) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        // Every foreground transition (ad returns, share sheets, the splash of a cold launch)
        // reaches here — throttle so rapid re-opens don't spam.
        if (!NotiPrefs.shouldPostOnOpen(context, MIN_INTERVAL_MS)) return

        val contentPi = NotiRouterActivity.pendingIntent(context, NotiTargets.HOME, NOTI_ID)
        val notification = NotiTextCard.build(
            context,
            NotiChannels.REMINDERS,
            NotificationCompat.PRIORITY_LOW,
            title = context.getString(R.string.noti_shortcut_title),
            desc = context.getString(R.string.noti_shortcut_desc),
            primary = NotiTextCard.Button(context.getString(R.string.noti_ls_card_btn_try), contentPi),
            secondary = NotiTextCard.Button(
                context.getString(R.string.noti_later),
                NotiDismissReceiver.pendingIntent(context, NOTI_ID)
            ),
            contentIntent = contentPi,
        )
        NotiPoster.postSafely(context, NOTI_ID, NotiChannels.REMINDERS, notification)
    }
}
