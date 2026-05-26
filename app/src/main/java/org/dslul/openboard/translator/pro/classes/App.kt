package org.dslul.openboard.translator.pro.classes

import android.app.*
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import org.dslul.openboard.translator.pro.OnResumeActivity
import org.dslul.openboard.translator.pro.PreSplashScreenActivity
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AppOpenAdManager
import org.dslul.openboard.translator.pro.objects.inapp.InAppUtils.billing


class App : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private var currentActivity: Activity? = null
    private var hasCompletedInitialForeground = false

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(this)

        Misc.selectThemeMode(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        billing()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        Misc.isAppInForeground.value = true
        Log.e(Misc.logKey, "Foreground.")

        if (!hasCompletedInitialForeground) {
            hasCompletedInitialForeground = true
            return
        }

        val activity = currentActivity ?: return
        if (activity is PreSplashScreenActivity || activity is OnResumeActivity) return
        if (Ads.isShowingInt || AppOpenAdManager.isShowingAd) return

        AppOpenAdManager.showIfAvailable(
            activity = activity,
            remoteKey = Ads.isResumeAppOpenAdEnabled,
            adIds = AdIds.appOpenAdIdResume,
            nextLoadAdIds = AdIds.appOpenAdIdResume
        )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Misc.isAppInForeground.value = false
        Log.i(Misc.logKey, "Background.")
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (!AppOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!AppOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
