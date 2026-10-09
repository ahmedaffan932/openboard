package org.dslul.openboard.translator.pro.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/** Single safe entry point for posting/inspecting notifications. */
object NotiPoster {

    /** Log tag shared by the notification package. */
    const val TAG = "TranslatorProNoti"

    /**
     * Posts only when notifications are enabled (POST_NOTIFICATIONS on 33+) AND the
     * target channel is not user-blocked — notify() to a blocked channel silently
     * no-ops, which would otherwise let callers wake the screen for nothing.
     */
    fun postSafely(context: Context, id: Int, channelId: String, notification: Notification): Boolean {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled — skipping post of id=$id")
            return false
        }
        // Channels only exist on API 26+; NotificationManagerCompat returns null below
        // that, so the SDK_INT check is what lets lint see getImportance() is guarded.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = nm.getNotificationChannel(channelId)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                Log.d(TAG, "Channel $channelId blocked — skipping post of id=$id")
                return false
            }
        }
        return try {
            nm.notify(id, notification)
            true
        } catch (e: SecurityException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * True when a notification with [id] is currently showing. Source of truth for the
     * lockscreen skip/replace logic — survives process death and never resets at midnight.
     */
    fun isActive(context: Context, id: Int): Boolean {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.activeNotifications.any { it.id == id }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun cancel(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
