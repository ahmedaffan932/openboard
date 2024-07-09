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
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AppOpenAdManager


class App : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(this)

        Misc.selectThemeMode(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        Misc.isAppInForeground.value = true
        Log.e(Misc.logKey, "Foreground.")
        // Show the ad (if available) when the app moves to foreground.
        if (!Ads.isShowingInt) {
            currentActivity?.let {
                Log.d(Misc.logKey, "App OnResume")
                if (Ads.isSplashAppOpenAdEnabled && AppOpenAdManager.isAdAvailable()) {
                    val intent = Intent(this, OnResumeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Misc.isAppInForeground.value = false
        Log.i(Misc.logKey, "Background.")
    }

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
