package org.dslul.openboard.translator.pro.notifications.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.dslul.openboard.translator.pro.notifications.DailyNoti
import org.dslul.openboard.translator.pro.notifications.LockscreenNotiEngine
import org.dslul.openboard.translator.pro.notifications.NotiConfig
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG
import org.dslul.openboard.translator.pro.notifications.NotiScheduler
import org.dslul.openboard.translator.pro.notifications.WinbackNoti

/** Single alarm entry point for all scheduled notification types. */
class NotiAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(NotiScheduler.EXTRA_TYPE) ?: return
        val slotIndex = intent.getIntExtra(NotiScheduler.EXTRA_SLOT_INDEX, 0)

        // Cold alarm-launched process: pull the persisted Remote Config values first,
        // so kill switches / slot changes / Variant A are honored.
        NotiConfig.load(context)

        Log.d(TAG, "NotiAlarmReceiver: type=$type slot=$slotIndex")

        // Re-arm from CURRENT config FIRST — nothing below may break the chain.
        // Re-deriving the slots (instead of re-arming the fired hour/minute) keeps a
        // user who crossed the new-user cutoff, or an RC slot change, on the right
        // schedule even if the app is never opened again.
        NotiScheduler.rearmType(context, type)

        when (type) {
            NotiScheduler.TYPE_LOCKSCREEN -> {
                if (NotiConfig.lockscreenEnabled) LockscreenNotiEngine.onSlotFired(context)
            }
            NotiScheduler.TYPE_WINBACK -> {
                if (NotiConfig.winbackEnabled) WinbackNoti.onSlotFired(context)
            }
            NotiScheduler.TYPE_WINBACK_FOLLOW_UP -> {
                if (NotiConfig.winbackEnabled) WinbackNoti.onFollowUpFired(context)
            }
            NotiScheduler.TYPE_DAILY -> {
                if (NotiConfig.dailyEnabled) DailyNoti.onSlotFired(context)
            }
        }
    }
}
