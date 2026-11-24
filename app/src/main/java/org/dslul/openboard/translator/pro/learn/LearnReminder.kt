package org.dslul.openboard.translator.pro.learn

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.App
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.notifications.NotiAnalytics
import org.dslul.openboard.translator.pro.notifications.NotiChannels
import org.dslul.openboard.translator.pro.notifications.NotiPoster
import org.dslul.openboard.translator.pro.notifications.NotiRouterActivity
import org.dslul.openboard.translator.pro.notifications.NotiTargets
import org.dslul.openboard.translator.pro.notifications.NotiTextCard
import org.dslul.openboard.translator.pro.notifications.PhraseOfTheDay
import java.util.Calendar

/**
 * The daily "keep your streak" reminder the learner switched on in Learn onboarding/settings.
 * One alarm at the chosen time, re-armed for the next day each time it fires. It posts only if
 * no lesson was done today and the app is not on screen. Not a marketing notification, so it
 * also reaches Premium users.
 */
object LearnReminder {
    const val NOTI_ID = 2010
    private const val TYPE = "learn_reminder"
    private const val REQUEST = 3500
    private const val WINDOW_MS = 10 * 60 * 1000L

    fun schedule(context: Context) {
        val store = LearnStore.get(context)
        if (!store.onboarded || !store.reminderOn || store.course == null) {
            cancel(context)
            return
        }
        val minutes = store.reminderMinutes
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 5_000) add(Calendar.DAY_OF_YEAR, 1)
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setWindow(AlarmManager.RTC_WAKEUP, cal.timeInMillis, WINDOW_MS, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (e: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, cal.timeInMillis, WINDOW_MS, pi)
        }
        Log.d(Misc.logKey, "LearnReminder armed for ${cal.time}")
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        am.cancel(pi)
        pi.cancel()
    }

    fun onFired(context: Context) {
        schedule(context)
        val store = LearnStore.get(context)
        val course = store.course ?: return
        if (store.didLessonToday()) return
        if (App.isAppOnScreen()) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val res = PhraseOfTheDay.appLanguageContext(context)
        val streak = store.streak()
        val language = PhraseOfTheDay.languageName(context, course)
        val title = if (streak > 0) {
            res.resources.getQuantityString(R.plurals.learn_reminder_title_streak, streak, streak)
        } else {
            res.getString(R.string.learn_reminder_title)
        }
        val open = NotiRouterActivity.pendingIntent(context, NotiTargets.LEARN, NOTI_ID, TYPE)
        val notification = NotiTextCard.build(
            context,
            NotiChannels.DAILY_TIPS,
            NotificationCompat.PRIORITY_DEFAULT,
            title = title,
            desc = res.getString(R.string.learn_reminder_desc, language),
            primary = NotiTextCard.Button(res.getString(R.string.learn_reminder_button), open),
            contentIntent = open,
        )
        if (NotiPoster.postSafely(context, NOTI_ID, NotiChannels.DAILY_TIPS, notification)) {
            NotiAnalytics.notiShow(context, TYPE)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST,
            Intent(context, LearnReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class LearnReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LearnConfig.load(context)
        LearnReminder.onFired(context)
    }
}
