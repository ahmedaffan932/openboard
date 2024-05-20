package org.dslul.openboard.translator.pro.classes

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.translator.pro.OnResumeActivity
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AppOpenAdManager


class App : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

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
                        Ads.splashInt = mFRC.getString("splashInt")
                        Ads.exitNative = mFRC.getString("exitNative")
                        Ads.translateInt = mFRC.getString("translateInt")
                        Ads.phraseInt = mFRC.getString("phraseInt")
                        Ads.phraseNative = mFRC.getString("phraseNative")
                        Ads.onBoardingNative = mFRC.getString("onBoardingNative")
                        Misc.lifeTimePrice = mFRC.getString("lifeTimePrice")
                        Ads.dashboardNative = mFRC.getString("dashboardNative")

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
        if(!Ads.isShowingInt) {
            currentActivity?.let {
                Log.d(Misc.logKey, "App OnResume")
                if (Ads.isAppOpenAdEnabled && AppOpenAdManager.isAdAvailable()) {
                    val intent = Intent(this, OnResumeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
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
