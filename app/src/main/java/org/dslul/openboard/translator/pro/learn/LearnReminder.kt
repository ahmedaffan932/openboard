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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.App
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.learn.engine.LearnDays
import org.dslul.openboard.translator.pro.learn.engine.Srs
import org.dslul.openboard.translator.pro.learn.engine.WeeklyStats
import org.dslul.openboard.translator.pro.notifications.NotiAnalytics
import org.dslul.openboard.translator.pro.notifications.NotiChannels
import org.dslul.openboard.translator.pro.notifications.NotiPoster
import org.dslul.openboard.translator.pro.notifications.NotiRouterActivity
import org.dslul.openboard.translator.pro.notifications.NotiTargets
import org.dslul.openboard.translator.pro.notifications.NotiTextCard
import org.dslul.openboard.translator.pro.notifications.PhraseOfTheDay
import java.util.Calendar
import kotlin.random.Random

/**
 * Learn's own notifications, all switched on in Learn onboarding/settings and so not marketing
 * (they also reach Premium users). One alarm each, re-armed every time it fires:
 * - **reminder** at the learner's time: "keep your streak", only if no lesson was done today;
 * - **word of the day** at 9:00: a word the learner has already met, to keep it fresh;
 * - **weekly summary** on Sunday at 18:00: lessons, XP and streak of the last 7 days, only after
 *   an active week.
 * None posts while the app is on screen. All tap through to the course map.
 */
object LearnReminder {
    const val NOTI_ID = 2010
    const val NOTI_ID_WORD = 2011
    const val NOTI_ID_WEEKLY = 2012

    private const val EXTRA_KIND = "learnNotiKind"
    const val KIND_REMINDER = "learn_reminder"
    const val KIND_WORD = "learn_word"
    const val KIND_WEEKLY = "learn_weekly"
    private const val WORD_AT_MINUTES = 9 * 60
    private const val WEEKLY_AT_MINUTES = 18 * 60
    private const val WINDOW_MS = 10 * 60 * 1000L

    /** (Re)arms every Learn alarm from the current settings. Safe to call repeatedly. */
    fun schedule(context: Context) {
        val store = LearnStore.get(context)
        val active = store.onboarded && store.course != null
        arm(context, KIND_REMINDER, active && store.reminderOn) { nextDaily(store.reminderMinutes) }
        arm(context, KIND_WORD, active && store.wordOfDayOn) { nextDaily(WORD_AT_MINUTES) }
        arm(context, KIND_WEEKLY, active && store.weeklyOn) { nextSunday(WEEKLY_AT_MINUTES) }
    }

    fun cancel(context: Context) {
        for (kind in listOf(KIND_REMINDER, KIND_WORD, KIND_WEEKLY)) arm(context, kind, false) { 0L }
    }

    private fun arm(context: Context, kind: String, on: Boolean, at: () -> Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, kind)
        if (!on) {
            am.cancel(pi)
            return
        }
        val time = at()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setWindow(AlarmManager.RTC_WAKEUP, time, WINDOW_MS, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            }
        } catch (e: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, time, WINDOW_MS, pi)
        }
        Log.d(Misc.logKey, "LearnReminder $kind armed for ${java.util.Date(time)}")
    }

    private fun nextDaily(minutes: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis() + 5_000) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    private fun nextSunday(minutes: Int): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        while (timeInMillis <= System.currentTimeMillis() + 5_000) add(Calendar.WEEK_OF_YEAR, 1)
    }.timeInMillis

    private fun pendingIntent(context: Context, kind: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            when (kind) {
                KIND_WORD -> 3501
                KIND_WEEKLY -> 3502
                else -> 3500
            },
            Intent(context, LearnReminderReceiver::class.java).putExtra(EXTRA_KIND, kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** From [LearnReminderReceiver]: re-arm everything, then post this kind if it applies. */
    suspend fun onFired(context: Context, kind: String) {
        schedule(context)
        val store = LearnStore.get(context)
        val course = LearnRepository.currentCourse(context) ?: return
        if (App.isAppOnScreen()) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        when (kind) {
            KIND_WORD -> postWord(context, store, course)
            KIND_WEEKLY -> postWeekly(context, store, course)
            else -> postReminder(context, store, course)
        }
    }

    private fun postReminder(context: Context, store: LearnStore, course: String) {
        if (store.didLessonToday()) return
        val res = PhraseOfTheDay.appLanguageContext(context)
        val streak = store.streak()
        val title = if (streak > 0) {
            res.resources.getQuantityString(R.plurals.learn_reminder_title_streak, streak, streak)
        } else {
            res.getString(R.string.learn_reminder_title)
        }
        post(
            context, NOTI_ID, KIND_REMINDER, title,
            res.getString(R.string.learn_reminder_desc, PhraseOfTheDay.languageName(context, course)),
            res.getString(R.string.learn_reminder_button)
        )
    }

    /** A word the learner has met: one due for review if any, else any seen word. */
    private suspend fun postWord(context: Context, store: LearnStore, course: String) {
        val content = LearnRepository.course(context, course)
        val items = store.progress(course).items.filter { it.value.seen > 0 }
        if (items.isEmpty()) return
        val today = LearnDays.today()
        val due = items.filter { Srs.isDue(it.value, today) }.keys.toList()
        val pool = (due.ifEmpty { items.keys.toList() })
            .mapNotNull { content.item(it) }
            .filter { !it.isSentence || it.target.length <= 28 }
        val item = pool.ifEmpty { return }.random(Random(today))
        val res = PhraseOfTheDay.appLanguageContext(context)
        post(
            context, NOTI_ID_WORD, KIND_WORD, res.getString(R.string.learn_word_of_day),
            res.getString(R.string.learn_word_of_day_desc, item.target, item.meaning),
            res.getString(R.string.learn_practice)
        )
    }

    private fun postWeekly(context: Context, store: LearnStore, course: String) {
        val (lessons, xp, _) = WeeklyStats.lastWeek(store.dayStats(), LearnDays.today())
        if (lessons == 0) return
        val res = PhraseOfTheDay.appLanguageContext(context)
        val r = res.resources
        val streak = store.streak()
        post(
            context, NOTI_ID_WEEKLY, KIND_WEEKLY,
            res.getString(R.string.learn_weekly_title, PhraseOfTheDay.languageName(context, course)),
            res.getString(
                R.string.learn_weekly_desc,
                r.getQuantityString(R.plurals.learn_unit_lessons, lessons, lessons),
                xp,
                r.getQuantityString(R.plurals.learn_streak_days, streak, streak)
            ),
            res.getString(R.string.learn_reminder_button)
        )
    }

    private fun post(context: Context, id: Int, kind: String, title: String, desc: String, button: String) {
        val open = NotiRouterActivity.pendingIntent(context, NotiTargets.LEARN, id, kind)
        val notification = NotiTextCard.build(
            context,
            NotiChannels.DAILY_TIPS,
            NotificationCompat.PRIORITY_DEFAULT,
            title = title,
            desc = desc,
            primary = NotiTextCard.Button(button, open),
            contentIntent = open,
        )
        if (NotiPoster.postSafely(context, id, NotiChannels.DAILY_TIPS, notification)) {
            NotiAnalytics.notiShow(context, kind)
        }
    }

    internal fun kindOf(intent: Intent): String = intent.getStringExtra(EXTRA_KIND) ?: KIND_REMINDER
}

class LearnReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LearnConfig.load(context)
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LearnReminder.onFired(app, LearnReminder.kindOf(intent))
            } catch (e: Exception) {
                Log.w(Misc.logKey, "LearnReminder failed: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }
}
