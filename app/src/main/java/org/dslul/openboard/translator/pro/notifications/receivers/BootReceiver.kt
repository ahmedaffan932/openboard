package org.dslul.openboard.translator.pro.notifications.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.dslul.openboard.translator.pro.notifications.NotiConfig
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG
import org.dslul.openboard.translator.pro.notifications.NotiScheduler

/**
 * Alarms do not survive reboot. Re-arms all scheduled notifications after boot,
 * and after time / timezone changes so slots stay on local wall-clock time.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d(TAG, "BootReceiver: ${intent.action} — rescheduling notifications")
                NotiConfig.load(context)
                NotiScheduler.scheduleAll(context)
            }
        }
    }
}
