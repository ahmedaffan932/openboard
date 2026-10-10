package org.dslul.openboard.translator.pro.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG
import org.dslul.openboard.translator.pro.notifications.receivers.NotiAlarmReceiver
import java.util.Calendar

/**
 * One-shot exact alarms, one per (type, slot), each re-armed for the next day when it
 * fires (NotiAlarmReceiver). setInexactRepeating is NOT used: we need per-slot payloads
 * and exact times. On Android 12+ without the SCHEDULE_EXACT_ALARM grant (default-denied
 * on 14+) we silently degrade to a 10-minute window.
 */
object NotiScheduler {
    const val TYPE_LOCKSCREEN = "lockscreen"
    const val TYPE_WINBACK = "winback"
    const val TYPE_DAILY = "daily"
    const val TYPE_WINBACK_FOLLOW_UP = "winbackFollowUp"

    const val EXTRA_TYPE = "notiType"
    const val EXTRA_SLOT_INDEX = "notiSlotIndex"
    const val EXTRA_HOUR = "notiSlotHour"
    const val EXTRA_MINUTE = "notiSlotMinute"

    // Request-code ranges per type; existing app PendingIntents use small codes (0..),
    // these stay clear of them.
    private const val REQUEST_BASE_LOCKSCREEN = 3100
    private const val REQUEST_BASE_WINBACK = 3200
    private const val REQUEST_BASE_DAILY = 3300
    private const val REQUEST_WINBACK_FOLLOW_UP = 3400
    private const val MAX_SLOTS = 10
    private const val WINDOW_MS = 10 * 60 * 1000L
    private const val WINBACK_FOLLOW_UP_DELAY_MS = 30 * 60 * 1000L

    /** (Re)arm everything from current NotiConfig. Safe to call repeatedly. */
    fun scheduleAll(context: Context) {
        if (NotiConfig.lockscreenEnabled) {
            scheduleType(context, TYPE_LOCKSCREEN, lockscreenSlotsFor(context))
        } else {
            cancelType(context, TYPE_LOCKSCREEN)
        }
        if (NotiConfig.winbackEnabled) {
            scheduleType(context, TYPE_WINBACK, NotiConfig.parseSlots(NotiConfig.winbackSlots))
        } else {
            cancelType(context, TYPE_WINBACK)
        }
        if (NotiConfig.dailyEnabled) {
            scheduleType(context, TYPE_DAILY, NotiConfig.parseSlots(NotiConfig.dailySlots))
        } else {
            cancelType(context, TYPE_DAILY)
        }
    }

    /** Existing vs new users get separate slot configs (spec §2). */
    fun lockscreenSlotsFor(context: Context): List<Pair<Int, Int>> {
        val csv = if (NotiPrefs.isNewUser(context)) {
            NotiConfig.lockscreenSlotsNew
        } else {
            NotiConfig.lockscreenSlots
        }
        return NotiConfig.parseSlots(csv)
    }

    /** Re-arms one type entirely from current NotiConfig (used by NotiAlarmReceiver). */
    fun rearmType(context: Context, type: String) {
        when (type) {
            TYPE_LOCKSCREEN ->
                if (NotiConfig.lockscreenEnabled) {
                    scheduleType(context, type, lockscreenSlotsFor(context))
                } else {
                    cancelType(context, type)
                }
            TYPE_WINBACK ->
                if (NotiConfig.winbackEnabled) {
                    scheduleType(context, type, NotiConfig.parseSlots(NotiConfig.winbackSlots))
                } else {
                    cancelType(context, type)
                }
            TYPE_DAILY ->
                if (NotiConfig.dailyEnabled) {
                    scheduleType(context, type, NotiConfig.parseSlots(NotiConfig.dailySlots))
                } else {
                    cancelType(context, type)
                }
        }
    }

    private fun scheduleType(context: Context, type: String, slots: List<Pair<Int, Int>>) {
        slots.take(MAX_SLOTS).forEachIndexed { index, (hour, minute) ->
            scheduleNextForSlot(context, type, index, hour, minute)
        }
        // Cancel stale higher-index slots left over from a previously longer config.
        for (i in slots.size until MAX_SLOTS) {
            cancelSlot(context, type, i)
        }
    }

    /** Arms [type]/[slotIndex] for its next occurrence of hour:minute (today or tomorrow). */
    fun scheduleNextForSlot(context: Context, type: String, slotIndex: Int, hour: Int, minute: Int) {
        val triggerAt = nextOccurrence(hour, minute)
        setAlarm(context, triggerAt, alarmPendingIntent(context, type, slotIndex, hour, minute))
        Log.d(TAG, "NotiScheduler: $type slot $slotIndex armed for $hour:$minute ($triggerAt)")
    }

    /** Arms the one Win-Back re-send; a newer post replaces a pending one. */
    fun scheduleWinbackFollowUp(context: Context) {
        val triggerAt = System.currentTimeMillis() + WINBACK_FOLLOW_UP_DELAY_MS
        setAlarm(context, triggerAt, winbackFollowUpPendingIntent(context))
        Log.d(TAG, "NotiScheduler: winback follow-up armed for $triggerAt")
    }

    fun cancelWinbackFollowUp(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = winbackFollowUpPendingIntent(context)
        am.cancel(pi)
        pi.cancel()
    }

    private fun setAlarm(context: Context, triggerAt: Long, pi: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, WINDOW_MS, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            // Permission revoked between check and set — degrade to a window.
            e.printStackTrace()
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, WINDOW_MS, pi)
        }
    }

    fun cancelType(context: Context, type: String) {
        for (i in 0 until MAX_SLOTS) {
            cancelSlot(context, type, i)
        }
    }

    private fun cancelSlot(context: Context, type: String, slotIndex: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmPendingIntent(context, type, slotIndex, 0, 0)
        am.cancel(pi)
        pi.cancel()
    }

    private fun nextOccurrence(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis() + 5_000) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun requestCode(type: String, slotIndex: Int): Int {
        val base = when (type) {
            TYPE_LOCKSCREEN -> REQUEST_BASE_LOCKSCREEN
            TYPE_WINBACK -> REQUEST_BASE_WINBACK
            else -> REQUEST_BASE_DAILY
        }
        return base + slotIndex
    }

    private fun winbackFollowUpPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_WINBACK_FOLLOW_UP,
            Intent(context, NotiAlarmReceiver::class.java)
                .putExtra(EXTRA_TYPE, TYPE_WINBACK_FOLLOW_UP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun alarmPendingIntent(
        context: Context,
        type: String,
        slotIndex: Int,
        hour: Int,
        minute: Int
    ): PendingIntent {
        val intent = Intent(context, NotiAlarmReceiver::class.java)
            .putExtra(EXTRA_TYPE, type)
            .putExtra(EXTRA_SLOT_INDEX, slotIndex)
            .putExtra(EXTRA_HOUR, hour)
            .putExtra(EXTRA_MINUTE, minute)
        return PendingIntent.getBroadcast(
            context,
            requestCode(type, slotIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
