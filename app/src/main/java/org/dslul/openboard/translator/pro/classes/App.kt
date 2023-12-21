package org.dslul.openboard.translator.pro.classes

import android.app.*
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.translator.pro.classes.admob.Ads
import org.dslul.openboard.translator.pro.classes.admob.AppOpenAdManager


class App : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this) {}

        registerActivityLifecycleCallbacks(this)

        Misc.selectThemeMode(this)

        getRemoteConfigValues()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun getRemoteConfigValues(): Boolean {
        return try {
            val mFRC = FirebaseRemoteConfig.getInstance()
            mFRC.ensureInitialized()
            mFRC.fetchAndActivate().addOnCompleteListener { p0 ->
                if (p0.isSuccessful) {
                    if (!BuildConfig.DEBUG) {
                        Ads.phraseInt = mFRC.getString("phraseInt")
                        Ads.phraseNative = mFRC.getString("phraseNative")
                        Ads.splashNative = mFRC.getString("splashNative")
                        Misc.lifeTimePrice = mFRC.getString("lifeTimePrice")
                        Ads.dashboardNative = mFRC.getString("dashboardNative")
                        Ads.dashboardBanner = mFRC.getString("dashboardBanner")
                        Ads.enableKeyboardInt = mFRC.getString("enableKeyboardInt")
                        Ads.cameraTranslationInt = mFRC.getString("cameraTranslationInt")
                        Ads.cameraTranslationBanner = mFRC.getString("cameraTranslationBanner")
                        Ads.languageSelectorOnBackInt = mFRC.getString("languageSelectorOnBackInt")

                        Ads.isSplashAppOpenEnabled = mFRC.getBoolean("isSplashAppOpenEnabled")

                        try {
                            Misc.showInterstitialAfter =
                                mFRC.getString("showInterstitialAfter").toInt()
                        } catch (e: Exception) {
                            Misc.showInterstitialAfter = 1
                        }
                    }
                    mFRC.reset()
                    Misc.isRemoteConfigFetched.value = true

                    Log.d(Misc.logKey, p0.exception.toString())
                } else {
                    Misc.isRemoteConfigFetched.value = true
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Misc.isRemoteConfigFetched.value = true
            false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        currentActivity?.let {
            if (!Misc.isInterstitialDisplaying)
                AppOpenAdManager.showAdIfAvailable(it, Ads.isSplashAppOpenEnabled)
        }
    }

    /** Show the ad if one isn't already showing. */

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Updating the currentActivity only when an ad is not showing.
        if (!AppOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
