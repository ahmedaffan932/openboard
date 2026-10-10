package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.dslul.openboard.translator.pro.classes.Misc

/**
 * Remote-Config-backed notification configuration.
 *
 * Defaults live here. Remote Config is only fetched by PreSplashScreenActivity, which hands it to
 * [applyFrom]; the values are then persisted so a cold process started by an alarm or by the
 * keyboard (same process) runs on the last fetched values without touching Remote Config.
 */
object NotiConfig {

    // Lock-screen notification engine (260817 - MO - Lockscreen Noti Logic Spec)
    var lockscreenEnabled = true
    var lockscreenSlots = "11:30,17:00,20:00"      // existing users
    var lockscreenSlotsNew = "11:30,17:00,20:00"   // new users (separate config per spec)
    var lockscreenReplace = false                   // false = Default (skip), true = Variant A (replace)
    var lockscreenWakeSeconds = 20
    var lockscreenContentJson = ""                  // "" = use bundled localized defaults

    var newUserDays = 2          // "new user" = installed less than this many days ago
    var forPremiumUsers = false  // marketing notis for purchased users

    // Noti.pdf templates
    var winbackEnabled = true
    var winbackSlots = "09:00,11:00,18:00"
    var dailyEnabled = true
    var dailySlots = "08:00,19:00"
    var clickedAdsEnabled = true
    var killAppEnabled = true
    var killAppCooldownHours = 24   // at most one kill-app notification per this many hours
    var onOpenEnabled = true
    var shortcutsEnabled = true
    // The red "Uninstall" long-press shortcut (opens a retention survey, not the uninstaller). Its
    // own kill switch on top of shortcutsEnabled, so it can be pulled without losing camera/voice.
    var uninstallShortcutEnabled = true
    var widgetPromptEnabled = true  // the first-run "Add widget" card shown on the dashboard

    private const val PREFS = "noti_config"

    @Volatile
    private var loaded = false

    /**
     * Loads the last-applied Remote Config values. MUST run before any scheduling or slot handling
     * in a cold process (App.onCreate, BootReceiver, NotiAlarmReceiver): Remote Config is only
     * fetched behind the splash, so alarm- and keyboard-started processes would otherwise run on
     * compile-time defaults and ignore remote kill switches / slot changes.
     */
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean("hasValues", false)) return
        lockscreenEnabled = p.getBoolean("lockscreenEnabled", lockscreenEnabled)
        lockscreenSlots = p.getString("lockscreenSlots", lockscreenSlots) ?: lockscreenSlots
        lockscreenSlotsNew = p.getString("lockscreenSlotsNew", lockscreenSlotsNew) ?: lockscreenSlotsNew
        lockscreenReplace = p.getBoolean("lockscreenReplace", lockscreenReplace)
        lockscreenWakeSeconds = p.getInt("lockscreenWakeSeconds", lockscreenWakeSeconds)
        lockscreenContentJson = p.getString("lockscreenContentJson", lockscreenContentJson) ?: lockscreenContentJson
        newUserDays = p.getInt("newUserDays", newUserDays)
        forPremiumUsers = p.getBoolean("forPremiumUsers", forPremiumUsers)
        winbackEnabled = p.getBoolean("winbackEnabled", winbackEnabled)
        winbackSlots = p.getString("winbackSlots", winbackSlots) ?: winbackSlots
        dailyEnabled = p.getBoolean("dailyEnabled", dailyEnabled)
        dailySlots = p.getString("dailySlots", dailySlots) ?: dailySlots
        clickedAdsEnabled = p.getBoolean("clickedAdsEnabled", clickedAdsEnabled)
        killAppEnabled = p.getBoolean("killAppEnabled", killAppEnabled)
        killAppCooldownHours = p.getInt("killAppCooldownHours", killAppCooldownHours)
        onOpenEnabled = p.getBoolean("onOpenEnabled", onOpenEnabled)
        shortcutsEnabled = p.getBoolean("shortcutsEnabled", shortcutsEnabled)
        uninstallShortcutEnabled = p.getBoolean("uninstallShortcutEnabled", uninstallShortcutEnabled)
        widgetPromptEnabled = p.getBoolean("widgetPromptEnabled", widgetPromptEnabled)
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("hasValues", true)
            .putBoolean("lockscreenEnabled", lockscreenEnabled)
            .putString("lockscreenSlots", lockscreenSlots)
            .putString("lockscreenSlotsNew", lockscreenSlotsNew)
            .putBoolean("lockscreenReplace", lockscreenReplace)
            .putInt("lockscreenWakeSeconds", lockscreenWakeSeconds)
            .putString("lockscreenContentJson", lockscreenContentJson)
            .putInt("newUserDays", newUserDays)
            .putBoolean("forPremiumUsers", forPremiumUsers)
            .putBoolean("winbackEnabled", winbackEnabled)
            .putString("winbackSlots", winbackSlots)
            .putBoolean("dailyEnabled", dailyEnabled)
            .putString("dailySlots", dailySlots)
            .putBoolean("clickedAdsEnabled", clickedAdsEnabled)
            .putBoolean("killAppEnabled", killAppEnabled)
            .putInt("killAppCooldownHours", killAppCooldownHours)
            .putBoolean("onOpenEnabled", onOpenEnabled)
            .putBoolean("shortcutsEnabled", shortcutsEnabled)
            .putBoolean("uninstallShortcutEnabled", uninstallShortcutEnabled)
            .putBoolean("widgetPromptEnabled", widgetPromptEnabled)
            .apply()
    }

    /**
     * Apply values fetched from Remote Config, then persist them for cold-process loads. A key is
     * only applied when it actually exists on the remote: getString() on a missing key returns ""
     * and getBoolean() returns false, which must not clobber the defaults above.
     */
    fun applyFrom(context: Context, mFRC: FirebaseRemoteConfig) {
        remoteString(mFRC, "noti_lockscreen_slots") { lockscreenSlots = it }
        remoteString(mFRC, "noti_lockscreen_slots_new") { lockscreenSlotsNew = it }
        remoteBoolean(mFRC, "notiLockscreenReplace") { lockscreenReplace = it }
        remoteInt(mFRC, "noti_lockscreen_wake_seconds") { lockscreenWakeSeconds = it }
        remoteString(mFRC, "noti_lockscreen_content") { lockscreenContentJson = it }

        remoteInt(mFRC, "noti_new_user_days") { newUserDays = it }
        remoteBoolean(mFRC, "notiForPremiumUsers") { forPremiumUsers = it }

        remoteBoolean(mFRC, "notiLockscreenEnabled") { lockscreenEnabled = it }
        remoteBoolean(mFRC, "notiWinbackEnabled") { winbackEnabled = it }
        remoteString(mFRC, "noti_winback_slots") { winbackSlots = it }
        remoteBoolean(mFRC, "notiDailyEnabled") { dailyEnabled = it }
        remoteString(mFRC, "noti_daily_slots") { dailySlots = it }
        remoteBoolean(mFRC, "notiClickedAdsEnabled") { clickedAdsEnabled = it }
        remoteBoolean(mFRC, "notiKillAppEnabled") { killAppEnabled = it }
        remoteInt(mFRC, "noti_kill_app_cooldown_hours") { killAppCooldownHours = it }
        remoteBoolean(mFRC, "notiOnOpenEnabled") { onOpenEnabled = it }
        remoteBoolean(mFRC, "notiShortcutsEnabled") { shortcutsEnabled = it }
        remoteBoolean(mFRC, "notiUninstallShortcutEnabled") { uninstallShortcutEnabled = it }
        remoteBoolean(mFRC, "notiWidgetPromptEnabled") { widgetPromptEnabled = it }

        persist(context)
    }

    /**
     * Marketing notifications (lock-screen, Win-Back, Daily, Clicked-Ads, Kill-App) skip purchased
     * users unless Remote Config `notiForPremiumUsers` opts them in. The pinned Quick Access card
     * and the on-open reminder are utilities and are not gated.
     */
    fun marketingAllowed(context: Context): Boolean =
        forPremiumUsers || !Misc.getPurchasedStatus(context)

    /** "11:30,17:00,20:00" -> [(11,30), (17,0), (20,0)]. Invalid entries are skipped. */
    fun parseSlots(csv: String): List<Pair<Int, Int>> =
        csv.split(",").mapNotNull { raw ->
            val parts = raw.trim().split(":")
            val hour = parts.getOrNull(0)?.trim()?.toIntOrNull()
            val minute = parts.getOrNull(1)?.trim()?.toIntOrNull()
            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                hour to minute
            } else {
                null
            }
        }

    private inline fun remoteString(mFRC: FirebaseRemoteConfig, key: String, apply: (String) -> Unit) {
        val value = mFRC.getValue(key)
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            val s = value.asString()
            if (s.isNotBlank()) apply(s)
        }
    }

    private inline fun remoteBoolean(mFRC: FirebaseRemoteConfig, key: String, apply: (Boolean) -> Unit) {
        val value = mFRC.getValue(key)
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            apply(value.asString().equals("true", ignoreCase = true))
        }
    }

    private inline fun remoteInt(mFRC: FirebaseRemoteConfig, key: String, apply: (Int) -> Unit) {
        val value = mFRC.getValue(key)
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            value.asString().trim().toIntOrNull()?.let(apply)
        }
    }
}
