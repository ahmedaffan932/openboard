package org.dslul.openboard.translator.pro.classes.ads

import android.content.Context
import android.util.Log
import com.facebook.ads.AdSettings
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.translator.pro.classes.Misc

/**
 * Consent + test configuration for Meta Audience Network, which is served through AdMob mediation
 * (see the `com.google.ads.mediation:facebook` adapter in build.gradle). AdMob drives the auction;
 * this object only sets the Meta-specific flags that Google UMP does not set for us.
 *
 * GDPR / EEA: no code needed here. The Audience Network SDK automatically reads the IAB TCF v2
 * consent string that the Google UMP form writes to the default SharedPreferences (the IABTCF_*
 * keys). That flow already runs in PreSplashScreenActivity, so EEA consent is covered.
 *
 * What UMP does NOT cover for Meta, and what this object handles:
 *   1. US state privacy (CCPA/CPRA) "Limited Data Use" (LDU) signal — see [ENABLE_LIMITED_DATA_USE].
 *   2. COPPA audience declaration (this app is not child-directed).
 *   3. Meta test ads during development, before the app passes Audience Network review.
 *
 * Call [configure] once at startup, right before MobileAds.initialize(), while consent is known to
 * allow ad requests. It is static configuration only — it never makes a network/ad request, so it
 * is safe to call even before the Meta App ID / placement IDs are wired.
 */
object MetaAdsConsent {

    /**
     * Meta test-device hash IDs. On the first ad request from a fresh device, the Audience Network
     * SDK prints a line to logcat like:
     *   "When testing your app with Facebook's ad units you must specify the device hashed ID ...
     *    AdSettings.addTestDevice("THE_HASH")"
     * Paste that hash here to receive Meta test ads on that device. Debug builds only.
     */
    private val TEST_DEVICE_HASHES = listOf<String>(
        // "THE_HASH_FROM_LOGCAT",
    )

    /**
     * When true, users detected in a US state with privacy laws (e.g. California) are sent Meta's
     * Limited Data Use flag. Leave false for normal processing, which is the default for most
     * ad-monetised apps. Flip to true only if the client's privacy policy / legal counsel requires
     * LDU. With country/state passed as 0, Meta geolocates the user and applies LDU only where it is
     * actually required.
     */
    private const val ENABLE_LIMITED_DATA_USE = false

    fun configure(context: Context) {
        try {
            // Not a child-directed app: declare a mixed/adult audience so Meta can serve
            // personalised ads (required by Audience Network for apps that are not for children).
            AdSettings.setMixedAudience(false)

            if (ENABLE_LIMITED_DATA_USE) {
                // country = 0, state = 0 -> let Meta geolocate; LDU applied only where required.
                AdSettings.setDataProcessingOptions(arrayOf("LDU"), 0, 0)
            } else {
                // Empty array is the explicit "process data normally" signal.
                AdSettings.setDataProcessingOptions(arrayOf())
            }

            if (BuildConfig.DEBUG) {
                AdSettings.setTestMode(true)
                TEST_DEVICE_HASHES.forEach { AdSettings.addTestDevice(it) }
            }

            Log.d(Misc.logKey, "Meta Audience Network configured (LDU=$ENABLE_LIMITED_DATA_USE).")
        } catch (t: Throwable) {
            // Defensive: a Meta-side misconfiguration must never crash app startup. If anything
            // here throws, the app simply continues running AdMob (and other adapters) as before.
            Log.w(Misc.logKey, "Meta Audience Network config skipped: ${t.message}")
        }
    }
}
