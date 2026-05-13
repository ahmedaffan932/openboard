package org.dslul.openboard.translator.pro.classes.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.example.translatorguru.ads.admob.LoadAdCallBack
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobInterstitialAd
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobNativeAds
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobRewardedInterstitial
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

object Ads {

    var translationRewardedAd: String = "am"
    var cameraTranslationRewardedAd: String = "am"
    var unlockPremiumRewardedAd: String = "am"
    var dashboardFragmentChangeInt: String = "am"
    var dashboardBanner: String = "am"
    var dashboardInt: String = "am"
    var everySixthClickInterstitial: String = "am"
    var interstitialClickFrequency: Int = 6

    var isIntPreLoad: Boolean = true
    var isNativeAdPreload: Boolean = true
    var isSplashAppOpenAdEnabled: Boolean = true

    var chatBanner: String = "am"
    var languageSelectorBanner: String = "am"
    var splashNative: String = "am_native_splash"
    var appLanguagesSelectorNative: String = "am_large_hctr"
    var cameraTranslationInt: String = "am"
    var exitInt: String = "am"
    var onBoardingNative: String = "am_small_hctr_native_btn_up"
    var exitNative: String = "am_large_hctr"
    var phraseInt: String = "am"
    var translateNative: String = "am_large_hctr_bottom"
    var premiumNative: String = "am"
    var dashboardNative: String = "am"
    var splashInt: String = "am"
    var translateInt: String = "am"

    var isShowingInt = false
    var isDashboardNativeDisplayed = false

    fun showBannerAd(
        frameLayout: FrameLayout,
        remoteKey: String
    ) {
        if (remoteKey.contains("am")) {
            // AdmobBannerAds.show(frameLayout)
        } else {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
        }
    }

    fun loadAndShowNativeAd(
        activity: Activity,
        adIds: Array<String> = AdIds.nativeAdIdAdMobExit,
        remoteKey: String,
        frameLayout: FrameLayout,
        shimmerLayout: Int? = null,
        callBack: LoadAdCallBack? = null
    ) {
        if (!remoteKey.contains("am")) {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            callBack?.onFailed()
            return
        }

        if (!Misc.checkInternetConnection(activity)) {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            callBack?.onFailed()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            callBack?.onFailed()
            return
        }

        AdmobNativeAds.loadAdmobNative(
            context = activity,
            adIds = adIds,
            remoteKey = remoteKey,
            frameLayout = frameLayout,
            callBack = object : LoadAdCallBack {

                override fun onLoaded() {
                    AdmobNativeAds.showNativeAd(
                        context = activity,
                        remoteKey = remoteKey,
                        amLayout = frameLayout
                    )

                    callBack?.onLoaded()
                }

                override fun onFailed() {
                    frameLayout.removeAllViews()
                    frameLayout.visibility = View.GONE

                    callBack?.onFailed()
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        remote: String,
        callback: InterstitialCallBack? = null
    ) {
        if (isShowingInt) {
            Log.d(Misc.logKey, "Interstitial skipped: already showing.")
            return
        }

        if (!remote.contains("am")) {
            Log.d(Misc.logKey, "Interstitial off.")
            callback?.onDismiss()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            Log.d(Misc.logKey, "Interstitial skipped: user purchased.")
            callback?.onDismiss()
            return
        }

        Log.d(Misc.logKey, "Interstitial show requested.")

        AdmobInterstitialAd.showInterstitial(activity, callback)
    }

    fun runWithEverySixthClickInterstitial(
        activity: Activity,
        action: () -> Unit
    ) {
        if (!everySixthClickInterstitial.contains("am")) {
            Log.d(Misc.logKey, "Every sixth click interstitial skipped: remote off.")
            action()
            return
        }

        if (interstitialClickFrequency <= 0) {
            Log.d(Misc.logKey, "Every sixth click interstitial skipped: invalid frequency.")
            action()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            Log.d(Misc.logKey, "Every sixth click interstitial skipped: user purchased.")
            action()
            return
        }

        if (isShowingInt) {
            Log.d(Misc.logKey, "Every sixth click interstitial skipped: already showing.")
            action()
            return
        }

        Misc.interstitialClickCtrCount++
        Log.d(Misc.logKey, "Every sixth click count: ${Misc.interstitialClickCtrCount}.")

        if (Misc.interstitialClickCtrCount % interstitialClickFrequency != 0) {
            action()
            return
        }

        Log.d(Misc.logKey, "Every sixth click interstitial requested.")

        loadAndShowInterstitial(
            activity = activity,
            remoteKey = everySixthClickInterstitial,
            adIds = AdIds.interstitialAdIdEverySixthClick,
            callBack = object : InterstitialCallBack {
                override fun onDismiss() {
                    action()
                }
            }
        )
    }

    fun loadAndShowInterstitial(
        activity: Activity,
        remoteKey: String,
        adIds: Array<String> = AdIds.interstitialAdIdAdMobPhrases,
        callBack: InterstitialCallBack? = null
    ) {
        if (isShowingInt) {
            Log.d(Misc.logKey, "Interstitial load/show skipped: already showing.")
            return
        }

        if (!remoteKey.contains("am")) {
            Log.d(Misc.logKey, "Interstitial load/show skipped: remote off.")
            callBack?.onDismiss()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            Log.d(Misc.logKey, "Interstitial load/show skipped: user purchased.")
            callBack?.onDismiss()
            return
        }

        if (AdmobInterstitialAd.isInterAdAvailableFor(adIds)) {
            Log.d(Misc.logKey, "Interstitial already loaded. Showing now.")

            AdmobInterstitialAd.showInterstitial(
                activity = activity,
                callback = callBack,
                nextPreloadAdIds = adIds
            )
            return
        }

        if (!Misc.checkInternetConnection(activity)) {
            Log.d(Misc.logKey, "Interstitial failed: no internet.")
            callBack?.onDismiss()
            return
        }

        val objDialog = Misc.LoadingAdDialog(activity)
        objDialog.setCancelable(false)

        if (!activity.isFinishing && !activity.isDestroyed) {
            objDialog.show()
        }

        isShowingInt = true

        AdmobInterstitialAd.loadInterAdmob(
            context = activity,
            adIds = adIds,
            callBack = object : LoadAdCallBack {

                override fun onLoaded() {
                    if (!activity.isFinishing && !activity.isDestroyed && objDialog.isShowing) {
                        objDialog.dismiss()
                    }

                    isShowingInt = false

                    AdmobInterstitialAd.showInterstitial(
                        activity = activity,
                        callback = object : InterstitialCallBack {

                            override fun onAdDisplayed() {
                                callBack?.onAdDisplayed()
                            }

                            override fun onDismiss() {
                                callBack?.onDismiss()
                            }
                        },
                        nextPreloadAdIds = adIds
                    )
                }

                override fun onFailed() {
                    if (!activity.isFinishing && !activity.isDestroyed && objDialog.isShowing) {
                        objDialog.dismiss()
                    }

                    isShowingInt = false
                    callBack?.onDismiss()
                }
            }
        )
    }

    fun loadAndShowRewardedInterstitial(
        activity: Activity,
        remoteKey: String,
        callBack: InterstitialCallBack? = null
    ) {
        if (!remoteKey.contains("am")) {
            callBack?.onDismiss()
            return
        }

        if (!Misc.checkInternetConnection(activity)) {
            callBack?.onDismiss()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            callBack?.onDismiss()
            return
        }

        val objDialog = Misc.LoadingAdDialog(activity)
        objDialog.setCancelable(false)

        if (!activity.isFinishing && !activity.isDestroyed) {
            objDialog.show()
        }

        AdmobRewardedInterstitial.loadRewardedInterAdmob(
            activity,
            AdIds.rewardedInterstitialAdIdAdMob,
            object : LoadAdCallBack {

                override fun onLoaded() {
                    if (!activity.isFinishing && !activity.isDestroyed && objDialog.isShowing) {
                        objDialog.dismiss()
                    }

                    AdmobRewardedInterstitial.showInterstitial(activity, callBack)
                }

                override fun onFailed() {
                    if (!activity.isFinishing && !activity.isDestroyed && objDialog.isShowing) {
                        objDialog.dismiss()
                    }

                    callBack?.onDismiss()
                }
            }
        )
    }
}
