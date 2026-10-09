package org.dslul.openboard.translator.pro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG

/**
 * Wakes the screen for N seconds (spec: 20s default, device may be locked, no user
 * action). SCREEN_BRIGHT_WAKE_LOCK + ACQUIRE_CAUSES_WAKEUP are deprecated but remain
 * the only way to light the screen from the background without a full-screen intent, which
 * this app does not use (it would need the Play Console full-screen-intent declaration).
 * After the timed release, the screen turns off on the device's own timeout — the
 * notification stays on the lock screen.
 */
object ScreenWakeController {

    /**
     * How long our own wake keeps going after the configured seconds, before the screen-off
     * it ends in: the keyguard's ~10s timeout once the wake lock lets go, with room to spare.
     */
    private const val OWN_WAKE_TAIL_S = 20

    private var screenOffReceiver: BroadcastReceiver? = null

    @Suppress("DEPRECATION")
    fun wake(context: Context, seconds: Int) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isInteractive) {
                // Screen is already on - the heads-up/lockscreen banner is visible,
                // nothing to wake (and no full-bright lock while the user is active).
                Log.d(TAG, "ScreenWakeController: screen already interactive, no wake")
                return
            }
            val wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        or PowerManager.ACQUIRE_CAUSES_WAKEUP
                        or PowerManager.ON_AFTER_RELEASE,
                "translatorpro:noti_wake"
            )
            wakeLock.acquire(seconds * 1000L)
            Log.d(TAG, "ScreenWakeController: screen woken for ${seconds}s")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Spec edge case: if the user ignores the notification and the screen turns off,
     * wake the screen once more. Capped at ONE re-wake per posted notification
     * ([postStamp], epoch ms) — an uncapped implementation would loop wake → timeout →
     * screen-off → wake forever. Screen-offs within wakeSeconds+[OWN_WAKE_TAIL_S]s of the
     * post are ignored (that's our own wake expiring). Best-effort: the receiver is
     * in-process and is lost if the process dies.
     */
    fun armOneRewake(context: Context, notiId: Int, postStamp: Long) {
        val appContext = context.applicationContext
        disarmRewake(appContext)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.action != Intent.ACTION_SCREEN_OFF) return
                // Ignore the screen-off caused by our own wake timing out (or the
                // user locking the phone right after the post) - the spec's re-wake
                // targets a LATER screen-off after the user saw and ignored it.
                val age = System.currentTimeMillis() - postStamp
                if (age < (NotiConfig.lockscreenWakeSeconds + OWN_WAKE_TAIL_S) * 1000L) return
                if (NotiPrefs.wasRewakeDone(c, postStamp) || !NotiPoster.isActive(c, notiId)) {
                    disarmRewake(appContext)
                    return
                }
                NotiPrefs.markRewakeDone(c, postStamp)
                wake(c, NotiConfig.lockscreenWakeSeconds)
                disarmRewake(appContext)
            }
        }
        screenOffReceiver = receiver
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun disarmRewake(context: Context) {
        screenOffReceiver?.let {
            try {
                context.applicationContext.unregisterReceiver(it)
            } catch (e: Exception) {
                // already unregistered
            }
        }
        screenOffReceiver = null
    }
}
