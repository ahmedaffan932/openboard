package org.dslul.openboard.translator.pro.learn

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import com.example.translatorguru.ads.admob.LoadAdCallBack
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobRewardedAds
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import org.dslul.openboard.translator.pro.interfaces.RewardedAdCallBack

/**
 * Ads in the Learn module. It reuses the app's existing placements, so nothing new is needed in
 * the ad config:
 * - a small native ad at the bottom of the course map and result screens (the Fun Words slot);
 * - an interstitial after every [LearnConfig.interstitialEvery] finished lessons (the
 *   every-sixth-click unit);
 * - the unlock-premium rewarded ad for opening a locked unit.
 *
 * Never inside a lesson. Premium users see none: each [Ads] call checks that first.
 */
object LearnAds {

    fun bottomNative(activity: Activity, frame: FrameLayout) {
        Ads.loadAndShowNativeAd(
            activity = activity,
            adIds = AdIds.nativeAdIdSharedBottom,
            remoteKey = Ads.gameNative,
            frameLayout = frame,
            shimmerLayout = R.layout.small_native_shimmer
        )
    }

    /** Runs [then] after an interstitial when one is due, otherwise straight away. */
    fun afterLesson(activity: Activity, then: () -> Unit) {
        val store = LearnStore.get(activity)
        val every = LearnConfig.interstitialEvery
        store.lessonsSinceAd = store.lessonsSinceAd + 1
        if (every <= 0 || store.lessonsSinceAd < every || Misc.getPurchasedStatus(activity)) {
            then()
            return
        }
        store.lessonsSinceAd = 0
        Ads.loadAndShowInterstitial(
            activity = activity,
            remoteKey = Ads.everySixthClickInterstitial,
            adIds = AdIds.interstitialAdIdEverySixthClick,
            callBack = object : InterstitialCallBack {
                override fun onDismiss() = then()
            }
        )
    }

    /**
     * Shows the rewarded ad and calls [onUnlock] when the reward is earned. Like the dashboard's
     * feature lock, a missing ad (no fill, timeout, no internet) still unlocks: the user tapped
     * "Watch ad" and must not stay stuck. [onUnlock] gets the reason for analytics.
     */
    fun rewardedUnlock(activity: Activity, onUnlock: (method: String) -> Unit) {
        if (!Ads.unlockPremiumRewardedAd.contains("am") || !Misc.checkInternetConnection(activity)) {
            onUnlock("no_ad")
            return
        }
        val loading = Misc.LoadingAdDialog(activity)
        loading.setCancelable(false)
        if (!activity.isFinishing && !activity.isDestroyed) loading.show()

        var settled = false
        val handler = Handler(Looper.getMainLooper())
        fun dismiss() {
            if (!activity.isFinishing && !activity.isDestroyed && loading.isShowing) loading.dismiss()
        }
        val timeout = Runnable {
            if (settled) return@Runnable
            settled = true
            dismiss()
            onUnlock("ad_timeout")
        }
        handler.postDelayed(timeout, Ads.adLoadTimeoutMs)

        AdmobRewardedAds.loadRewardedAd(
            context = activity,
            adIds = AdIds.rewardedAdIdUnlockPremium,
            callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    if (settled) return
                    settled = true
                    handler.removeCallbacks(timeout)
                    dismiss()
                    AdmobRewardedAds.showRewardedAd(
                        activity = activity,
                        callBack = object : RewardedAdCallBack {
                            override fun onRewardEarned() {
                                activity.runOnUiThread { onUnlock("rewarded") }
                            }

                            override fun onFailed() {
                                Log.d(Misc.logKey, "Learn: rewarded ad failed to show, unlocking anyway")
                                activity.runOnUiThread { onUnlock("show_failed") }
                            }
                        }
                    )
                }

                override fun onFailed() {
                    if (settled) return
                    settled = true
                    handler.removeCallbacks(timeout)
                    dismiss()
                    onUnlock("ad_failed")
                }
            }
        )
    }
}
