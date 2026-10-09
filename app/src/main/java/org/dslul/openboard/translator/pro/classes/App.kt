package org.dslul.openboard.translator.pro.classes

import android.app.*
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import org.dslul.openboard.translator.pro.OnResumeActivity
import org.dslul.openboard.translator.pro.PreSplashScreenActivity
import org.dslul.openboard.translator.pro.UninstallSurveyActivity
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AppOpenAdManager
import org.dslul.openboard.translator.pro.notifications.AdClickTracker
import org.dslul.openboard.translator.pro.notifications.AppShortcuts
import org.dslul.openboard.translator.pro.notifications.KillAppNoti
import org.dslul.openboard.translator.pro.notifications.KillAppWatcherService
import org.dslul.openboard.translator.pro.notifications.LockscreenNotiEngine
import org.dslul.openboard.translator.pro.notifications.NotiChannels
import org.dslul.openboard.translator.pro.notifications.NotiConfig
import org.dslul.openboard.translator.pro.notifications.NotiPoster
import org.dslul.openboard.translator.pro.notifications.NotiRouterActivity
import org.dslul.openboard.translator.pro.notifications.NotiScheduler
import org.dslul.openboard.translator.pro.notifications.OnOpenReminderNoti
import org.dslul.openboard.translator.pro.notifications.QuickAccessActivity
import org.dslul.openboard.translator.pro.notifications.ScreenWakeController
import org.dslul.openboard.translator.pro.notifications.WinbackNoti
import org.dslul.openboard.translator.pro.objects.inapp.InAppUtils.billing


class App : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    companion object {
        /** An app screen is started (ProcessLifecycleOwner STARTED). The keyboard does not count. */
        fun isAppOnScreen(): Boolean =
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

        private const val RESUME_SUPPRESS_WINDOW_MS = 60_000L

        @Volatile
        private var suppressResumeAdUntilMs = 0L

        /**
         * Suppresses the resume app-open ad for a short window. Set before handing off to a system
         * screen (the widget pin dialog), so returning from it does not pop an ad. Self-expiring on
         * purpose: a launcher that never backgrounds the app (pinning unsupported, or a silent pin)
         * leaves no sticky flag to later swallow an unrelated resume ad.
         */
        fun suppressNextResumeAd() {
            suppressResumeAdUntilMs = SystemClock.elapsedRealtime() + RESUME_SUPPRESS_WINDOW_MS
        }

        private fun consumeResumeAdSuppressed(): Boolean {
            if (SystemClock.elapsedRealtime() >= suppressResumeAdUntilMs) return false
            suppressResumeAdUntilMs = 0L
            return true
        }
    }

    private var currentActivity: Activity? = null
    private var hasCompletedInitialForeground = false

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(this)

        Misc.selectThemeMode(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        billing()

        // Also runs whenever the keyboard starts this process, which keeps the alarms armed for
        // users who type more than they open the app.
        KillAppNoti.reportPendingShow(this)
        NotiChannels.createAll(this)
        // Order matters: alarms armed before the config loads would ignore the kill switches.
        NotiConfig.load(this)
        NotiScheduler.scheduleAll(this)
        AppShortcuts.setup(this)
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        Misc.isAppInForeground.value = true
        Log.e(Misc.logKey, "Foreground.")

        onAppOpened(currentActivity)

        if (!hasCompletedInitialForeground) {
            hasCompletedInitialForeground = true
            return
        }

        val activity = currentActivity ?: return
        if (activity is PreSplashScreenActivity || activity is OnResumeActivity) return
        // Transparent trampolines: the router hands over to the splash or the feature, and the
        // pinned card's tiles open their tool with no ad on the way.
        if (activity is NotiRouterActivity || activity is QuickAccessActivity) return
        // A "leaving" screen reached by the Uninstall shortcut: no resume ad over it (e.g. when the
        // user backs out of the system uninstaller).
        if (activity is UninstallSurveyActivity) return
        if (consumeResumeAdSuppressed()) return
        if (Ads.isShowingInt || AppOpenAdManager.isShowingAd) return

        AppOpenAdManager.showIfAvailable(
            activity = activity,
            remoteKey = Ads.isResumeAppOpenAdEnabled,
            adIds = AdIds.appOpenAdIdResume,
            nextLoadAdIds = AdIds.appOpenAdIdResume
        )
    }

    /**
     * ON_PAUSE rather than ON_STOP: an ad tap that lands on Play's in-app sheet leaves that sheet
     * floating over the app, so the host activity is only paused — it stays STARTED and ON_STOP
     * never arrives. [AdClickTracker] holds the delay that tells a real departure from a sheet the
     * user dismisses at once.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onMoveToBackground() = AdClickTracker.onAppPaused(this)

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onReturnToForeground() = AdClickTracker.onAppResumed()

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Misc.isAppInForeground.value = false
        Log.i(Misc.logKey, "Background.")
    }

    private fun onAppOpened(activity: Activity?) {
        // Business rule: opening the app dismisses the lockscreen notification, not just a tap on it.
        NotiPoster.cancel(this, LockscreenNotiEngine.NOTI_ID)
        ScreenWakeController.disarmRewake(this)
        WinbackNoti.onAppOpened(this)
        // Must be now: a started service can only be started while the app is in the foreground.
        KillAppWatcherService.start(this)

        // Not during the splash or first open (the splash posts it itself once it hands over), and
        // not after a pinned-card tile tap: that card already is the quick way back.
        if (activity !is PreSplashScreenActivity && activity !is NotiRouterActivity &&
            !Misc.isSplashScreen && !Misc.isFirstTime(this) && !QuickAccessActivity.tappedRecently()
        ) {
            OnOpenReminderNoti.show(this)
        }
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
