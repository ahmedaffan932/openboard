package org.dslul.openboard.translator.pro.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Exists only to hear the app being swiped away from Recents: a started service declared with
 * android:stopWithTask="false" gets [onTaskRemoved] when that happens, which nothing else in
 * the platform reports. It does no work and holds nothing.
 *
 * Started when the app comes to the foreground (a started service may only be started then,
 * API 26+), not sticky. Android stops idle background services about a minute after the app
 * leaves the screen, so a swipe long after leaving goes unnoticed - by design of the platform,
 * and fine for a "you just closed us" message. Force-stop never reaches any callback.
 */
class KillAppWatcherService : Service() {

    companion object {
        fun start(context: Context) {
            if (!NotiConfig.killAppEnabled) return
            try {
                context.startService(Intent(context, KillAppWatcherService::class.java))
            } catch (e: Exception) {
                // IllegalStateException when the app is not in the foreground after all.
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Android also "removes" tasks the user never saw in Recents: from API 28 it trims a task
        // kept out of Recents (the Quick Access trampoline, the keyboard's language picker) once
        // it sits behind Home, and that reaches here too. Only a task the user could have swiped
        // counts. Task.setIntent copies android:excludeFromRecents into the root intent's flags.
        val excluded = rootIntent != null &&
                (rootIntent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) != 0
        val rootClass = rootIntent?.component?.className
        if (excluded || rootClass == QuickAccessActivity::class.java.name) return
        KillAppNoti.onAppKilled(applicationContext)
        stopSelf()
    }
}
