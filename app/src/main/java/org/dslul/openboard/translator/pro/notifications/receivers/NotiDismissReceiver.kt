package org.dslul.openboard.translator.pro.notifications.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.dslul.openboard.translator.pro.notifications.NotiPoster
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG
import org.dslul.openboard.translator.pro.notifications.NotiPrefs

/**
 * Handles the custom "X" button on notifications and swipe deleteIntents:
 * cancels the notification and records the dismissal. Once dismissed, the
 * lockscreen skip-check (activeNotifications) allows the next slot to send.
 */
class NotiDismissReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_NOTI_ID = "notiId"
        private const val REQUEST_BASE = 3900

        fun pendingIntent(context: Context, notiId: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_BASE + notiId,
                Intent(context, NotiDismissReceiver::class.java).putExtra(EXTRA_NOTI_ID, notiId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_NOTI_ID, -1)
        if (id == -1) return
        NotiPoster.cancel(context, id)
        NotiPrefs.recordDismissal(context, id)
        Log.d(TAG, "NotiDismissReceiver: notification $id dismissed")
    }
}
