package org.dslul.openboard.translator.pro.learn

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

/**
 * Remote Config knobs for the Learn module, persisted so the values hold in processes that never
 * fetch (an alarm-started reminder). Only keys present on the server are applied.
 *
 * learn_enabled (kill switch for the entry points), learn_free_units, learn_interstitial_every
 * (lessons between interstitials; 0 = never), learn_unlock_hours (rewarded unit unlock length),
 * learn_phrase_practice_enabled.
 */
object LearnConfig {
    private const val PREFS = "learn_config"

    var enabled = true
        private set
    var freeUnits = 3
        private set
    var interstitialEvery = 2
        private set
    var unlockHours = 24
        private set
    var phrasePracticeEnabled = true
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabled = p.getBoolean("enabled", enabled)
        freeUnits = p.getInt("freeUnits", freeUnits)
        interstitialEvery = p.getInt("interstitialEvery", interstitialEvery)
        unlockHours = p.getInt("unlockHours", unlockHours)
        phrasePracticeEnabled = p.getBoolean("phrasePractice", phrasePracticeEnabled)
        loaded = true
    }

    fun applyFrom(context: Context, rc: FirebaseRemoteConfig) {
        load(context)
        fun remote(key: String) = rc.getValue(key).takeIf { it.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE }
        remote("learn_enabled")?.let { enabled = it.asBoolean() }
        remote("learn_free_units")?.let { freeUnits = it.asLong().toInt().coerceIn(1, 12) }
        remote("learn_interstitial_every")?.let { interstitialEvery = it.asLong().toInt().coerceIn(0, 20) }
        remote("learn_unlock_hours")?.let { unlockHours = it.asLong().toInt().coerceIn(1, 24 * 7) }
        remote("learn_phrase_practice_enabled")?.let { phrasePracticeEnabled = it.asBoolean() }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", enabled)
            .putInt("freeUnits", freeUnits)
            .putInt("interstitialEvery", interstitialEvery)
            .putInt("unlockHours", unlockHours)
            .putBoolean("phrasePractice", phrasePracticeEnabled)
            .apply()
    }
}
