package org.dslul.openboard.translator.pro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dslul.openboard.translator.pro.learn.LearnReminder

/**
 * Debug-only test hook (registered only in the debug manifest). The slots fire at fixed times,
 * so QA drives the notifications directly:
 *
 *   adb shell am broadcast -n com.translator.keyboard.translate.all.langugaes/org.dslul.openboard.translator.pro.notifications.DebugNotiReceiver -a com.translatorpro.debug.FIRE_NOTI --es type lockscreen
 *
 * Types: lockscreen, winback, winbackFollowUp, daily, clickedads, killapp, onopen, pinned,
 * reschedule, replaceOn / replaceOff, and Learn's learnReminder, learnWord, learnWeekly (posted
 * only while the app is in the background, as for real).
 *
 * The lock-screen notification wakes the screen only when it is off: lock the phone first.
 */
class DebugNotiReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type")
        Log.d(TAG, "DebugNotiReceiver: type=$type")
        when (type) {
            "lockscreen" -> LockscreenNotiEngine.onSlotFired(context)
            "winback" -> WinbackNoti.onSlotFired(context)
            "winbackFollowUp" -> WinbackNoti.onFollowUpFired(context)
            "daily" -> DailyNoti.onSlotFired(context)
            "clickedads" -> ClickedAdsNoti.show(context)
            // Posts directly; the real trigger is swiping the app from Recents within ~1 min of
            // leaving it (then subject to the 24h cooldown).
            "killapp" -> KillAppNoti.post(context)
            "onopen" -> OnOpenReminderNoti.show(context)
            "pinned" -> PinnedNoti.post(context)
            "reschedule" -> NotiScheduler.scheduleAll(context)
            "replaceOn" -> NotiConfig.lockscreenReplace = true
            "replaceOff" -> NotiConfig.lockscreenReplace = false
            "learnReminder", "learnWord", "learnWeekly" -> {
                val kind = when (type) {
                    "learnWord" -> LearnReminder.KIND_WORD
                    "learnWeekly" -> LearnReminder.KIND_WEEKLY
                    else -> LearnReminder.KIND_REMINDER
                }
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        LearnReminder.onFired(context.applicationContext, kind)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
