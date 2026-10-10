package org.dslul.openboard.translator.pro.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.dslul.openboard.inputmethod.latin.R

/**
 * Fixed notification channels per Noti.pdf / the lockscreen spec.
 * Created once from App.onCreate. The pinned notification keeps its own channel (PinnedNoti),
 * and FCM pushes keep theirs.
 */
object NotiChannels {
    const val UPDATES_NEWS = "updates_news"     // Win-Back, Clicked-Ads, Kill-App — HIGH (sound + heads-up)
    const val REMINDERS = "reminders"           // Shortcut/Reminder — LOW ("no sound, no peek")
    const val DAILY_TIPS = "daily_tips"         // Daily Noti — DEFAULT
    const val LOCKSCREEN = "lockscreen"         // Lockscreen engine — HIGH, public on lock screen
    const val ACTIVE_PROCESS = "active_process" // Foreground service — LOW

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val lockscreen = NotificationChannel(
            LOCKSCREEN,
            context.getString(R.string.channel_lockscreen),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannels(
            listOf(
                lockscreen,
                NotificationChannel(
                    UPDATES_NEWS,
                    context.getString(R.string.channel_updates_news),
                    NotificationManager.IMPORTANCE_HIGH
                ),
                // Spec table says "DEFAULT (no sound, no peek)" — on Android that
                // behavior is IMPORTANCE_LOW, so LOW is what we create.
                NotificationChannel(
                    REMINDERS,
                    context.getString(R.string.channel_reminders),
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    DAILY_TIPS,
                    context.getString(R.string.channel_daily_tips),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    ACTIVE_PROCESS,
                    context.getString(R.string.channel_active_process),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        )
    }
}
