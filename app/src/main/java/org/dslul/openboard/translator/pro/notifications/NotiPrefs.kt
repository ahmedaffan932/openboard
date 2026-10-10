package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.content.SharedPreferences

/**
 * Notification state. Deliberately a single prefs file ("noti_state") with multiple
 * keys, to keep engine state together.
 */
object NotiPrefs {
    private const val FILE = "noti_state"

    private const val DAY_MS = 24L * 60 * 60 * 1000

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * "New user" = installed less than [NotiConfig.newUserDays] days ago (default 2 = first 48h).
     * Uses PackageManager.firstInstallTime so app updates do not make existing users "new".
     */
    fun isNewUser(context: Context): Boolean {
        val installedAt = try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        val days = (System.currentTimeMillis() - installedAt) / DAY_MS
        return days < NotiConfig.newUserDays
    }

    /** Last shown content id per notification type — rotation must never repeat consecutively. */
    fun getLastContentId(context: Context, type: String): Int =
        prefs(context).getInt("lastContent_$type", -1)

    fun setLastContentId(context: Context, type: String, id: Int) {
        prefs(context).edit().putInt("lastContent_$type", id).apply()
    }

    /** Stamp of the lockscreen post we already re-woke the screen for (max 1 re-wake per slot). */
    fun markRewakeDone(context: Context, postStamp: Long) {
        prefs(context).edit().putLong("rewakeDoneFor", postStamp).apply()
    }

    fun wasRewakeDone(context: Context, postStamp: Long): Boolean =
        prefs(context).getLong("rewakeDoneFor", -1L) == postStamp

    /** Stamp (epoch ms) of the currently posted lockscreen notification. */
    fun setLockscreenPostStamp(context: Context, stamp: Long) {
        prefs(context).edit().putLong("lockscreenPostStamp", stamp).apply()
    }

    fun getLockscreenPostStamp(context: Context): Long =
        prefs(context).getLong("lockscreenPostStamp", -1L)

    /** Throttle for the on-open reminder: true at most once per [minIntervalMs]. */
    fun shouldPostOnOpen(context: Context, minIntervalMs: Long): Boolean {
        val p = prefs(context)
        val last = p.getLong("lastOnOpenPostAt", 0L)
        val now = System.currentTimeMillis()
        if (now - last < minIntervalMs) return false
        p.edit().putLong("lastOnOpenPostAt", now).apply()
        return true
    }

    /** Throttle for the kill-app notification: true at most once per [minIntervalMs]. */
    fun shouldPostKillApp(context: Context, minIntervalMs: Long): Boolean {
        val p = prefs(context)
        val last = p.getLong("lastKillAppPostAt", 0L)
        val now = System.currentTimeMillis()
        if (now - last in 0 until minIntervalMs) return false
        p.edit().putLong("lastKillAppPostAt", now).apply()
        return true
    }

    /** commit(), not apply(): the process is killed right after (see KillAppNoti). */
    fun markKillAppShowPending(context: Context) {
        prefs(context).edit().putBoolean("killAppShowPending", true).commit()
    }

    fun consumeKillAppShowPending(context: Context): Boolean {
        val p = prefs(context)
        if (!p.getBoolean("killAppShowPending", false)) return false
        p.edit().putBoolean("killAppShowPending", false).apply()
        return true
    }

    fun recordAppOpen(context: Context) {
        val p = prefs(context)
        p.edit().putLong("appOpens", p.getLong("appOpens", 0L) + 1).apply()
    }

    fun appOpenCount(context: Context): Long = prefs(context).getLong("appOpens", 0L)

    /** App-open count at the last Win-Back post; -1 once its follow-up has been handled. */
    fun setWinbackOpensAtPost(context: Context, count: Long) {
        prefs(context).edit().putLong("winbackOpensAtPost", count).apply()
    }

    fun getWinbackOpensAtPost(context: Context): Long =
        prefs(context).getLong("winbackOpensAtPost", -1L)

    /** Dismissal log (analytics / debugging). */
    fun recordDismissal(context: Context, notiId: Int) {
        prefs(context).edit()
            .putLong("lastDismissedAt_$notiId", System.currentTimeMillis())
            .apply()
    }
}
