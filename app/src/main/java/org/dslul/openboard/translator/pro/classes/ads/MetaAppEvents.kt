package org.dslul.openboard.translator.pro.classes.ads

import android.app.Application
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import org.dslul.openboard.translator.pro.classes.Misc

/**
 * Meta (Facebook) app-event logging + install attribution.
 *
 * This is a DIFFERENT Meta product from Audience Network (see [MetaAdsConsent], which only
 * configures ad *serving* via AdMob mediation). The Facebook core SDK logs the app-activation and
 * automatic app events that Meta's Events Manager receives, and that Meta uses to attribute app
 * installs to its ad campaigns. It is paired with the Google Play Install Referrer library
 * (declared in build.gradle); the core SDK reads the referrer from it automatically — no extra
 * code needed for that.
 *
 * Consent: the manifest declares
 *   com.facebook.sdk.AutoInitEnabled / AutoLogAppEventsEnabled / AdvertiserIDCollectionEnabled
 * all = false, so nothing is collected before the user passes the Google UMP consent flow. This is
 * Meta's documented GDPR pattern (delay collection, then enable after consent). [activate] is
 * called from PreSplashScreenActivity inside the canRequestAds() gate — only then does it turn
 * logging on, initialise the SDK and fire the activation event.
 */
object MetaAppEvents {

    @Volatile
    private var activated = false

    /**
     * Enable Meta app-event logging and log the app-activation event. Safe to call repeatedly: it
     * no-ops after the first successful run in this process. Never throws — a Meta-side problem must
     * not break app startup (mirrors [MetaAdsConsent]'s defensive handling).
     */
    fun activate(application: Application) {
        if (activated) return
        try {
            // Consent is known by the time we reach here, so switch on what the manifest left off.
            FacebookSdk.setAutoInitEnabled(true)
            FacebookSdk.setAutoLogAppEventsEnabled(true)
            FacebookSdk.setAdvertiserIDCollectionEnabled(true)
            FacebookSdk.fullyInitialize()

            // Tells Meta the app launched; this is the event install-campaign attribution needs.
            AppEventsLogger.activateApp(application)

            activated = true
            Log.d(Misc.logKey, "Meta app events activated.")
        } catch (t: Throwable) {
            Log.w(Misc.logKey, "Meta app events activation skipped: ${t.message}")
        }
    }
}
