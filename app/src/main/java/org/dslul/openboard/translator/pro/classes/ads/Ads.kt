package org.dslul.openboard.translator.pro.classes.ads

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.dslul.openboard.inputmethod.latin.R
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
    var splashAdTimeoutMs: Long = 3000
    var splashAppOpenWaitMs: Long = 3000

    var isIntPreLoad: Boolean = true
    var isNativeAdPreload: Boolean = true
    var isSplashAppOpenAdEnabled: Boolean = true
    var isResumeAppOpenAdEnabled: Boolean = true

    var chatBanner: String = "am"
    var languageSelectorBanner: String = "am"
    var splashNative: String = "am_native_splash"
    var appLanguagesSelectorNative: String = "am_large_hctr"
    var appLanguagesRefreshNative: String = "am_large_hctr"
    var cameraTranslationInt: String = "am"
    var exitInt: String = "am"
    var onBoardingNative: String = "am_small_hctr_native_btn_up"
    var exitNative: String = "am_large_hctr"
    var sharedBottomNative: String = "am_large_hctr"
    var phraseInt: String = "am"
    var translateNative: String = "am_large_hctr_bottom"
    var premiumNative: String = "am"
    var dashboardNative: String = "am"
    var splashInt: String = "am"
    var translateInt: String = "am"

    var isShowingInt = false

    private val remoteFloors = mutableMapOf<String, Double>()

    private data class RemotePlacement(
        val key: String,
        val isNative: Boolean = false,
        val currentValue: () -> String,
        val applyValue: (String) -> Unit
    )

    fun floorFor(placementKey: String): Double = remoteFloors[placementKey] ?: 0.0

    fun setFloorFor(placementKey: String, floor: Double) {
        remoteFloors[placementKey] = floor
    }

    fun applyRemoteConfig(remoteConfig: FirebaseRemoteConfig) {
        isIntPreLoad = remoteConfig.booleanOrDefault("ad_interstitial_preload_enabled", isIntPreLoad)
        isNativeAdPreload = remoteConfig.booleanOrDefault("ad_native_preload_enabled", isNativeAdPreload)
        isSplashAppOpenAdEnabled = remoteConfig.booleanOrDefault(
            "splash_app_open_enabled",
            isSplashAppOpenAdEnabled
        )
        isResumeAppOpenAdEnabled = remoteConfig.booleanOrDefault(
            "resume_app_open_enabled",
            isResumeAppOpenAdEnabled
        )
        interstitialClickFrequency = remoteConfig.longOrDefault(
            "interstitial_click_frequency",
            interstitialClickFrequency.toLong()
        ).toInt()

        remoteFloors["splash_app_open"] = remoteConfig.doubleOrDefault(
            "splash_app_open_floor",
            0.0
        )
        remoteFloors["resume_app_open"] = remoteConfig.doubleOrDefault(
            "resume_app_open_floor",
            0.0
        )

        listOf(
            RemotePlacement("translation_rewarded", currentValue = { translationRewardedAd }) {
                translationRewardedAd = it
            },
            RemotePlacement("camera_translation_rewarded", currentValue = { cameraTranslationRewardedAd }) {
                cameraTranslationRewardedAd = it
            },
            RemotePlacement("unlock_premium_rewarded", currentValue = { unlockPremiumRewardedAd }) {
                unlockPremiumRewardedAd = it
            },
            RemotePlacement("dashboard_fragment_interstitial", currentValue = { dashboardFragmentChangeInt }) {
                dashboardFragmentChangeInt = it
            },
            RemotePlacement("dashboard_banner", currentValue = { dashboardBanner }) {
                dashboardBanner = it
            },
            RemotePlacement("dashboard_interstitial", currentValue = { dashboardInt }) {
                dashboardInt = it
            },
            RemotePlacement("every_sixth_click_interstitial", currentValue = { everySixthClickInterstitial }) {
                everySixthClickInterstitial = it
            },
            RemotePlacement("chat_banner", currentValue = { chatBanner }) {
                chatBanner = it
            },
            RemotePlacement("language_selector_banner", currentValue = { languageSelectorBanner }) {
                languageSelectorBanner = it
            },
            RemotePlacement("splash_native", isNative = true, currentValue = { splashNative }) {
                splashNative = it
            },
            RemotePlacement("language_native", isNative = true, currentValue = { appLanguagesSelectorNative }) {
                appLanguagesSelectorNative = it
            },
            RemotePlacement("language_refresh_native", isNative = true, currentValue = { appLanguagesRefreshNative }) {
                appLanguagesRefreshNative = it
            },
            RemotePlacement("camera_translation_interstitial", currentValue = { cameraTranslationInt }) {
                cameraTranslationInt = it
            },
            RemotePlacement("exit_interstitial", currentValue = { exitInt }) {
                exitInt = it
            },
            RemotePlacement("onboarding_native", isNative = true, currentValue = { onBoardingNative }) {
                onBoardingNative = it
            },
            RemotePlacement("exit_native", isNative = true, currentValue = { exitNative }) {
                exitNative = it
            },
            RemotePlacement("shared_bottom_native", isNative = true, currentValue = { sharedBottomNative }) {
                sharedBottomNative = it
            },
            RemotePlacement("phrase_interstitial", currentValue = { phraseInt }) {
                phraseInt = it
            },
            RemotePlacement("translate_native", isNative = true, currentValue = { translateNative }) {
                translateNative = it
            },
            RemotePlacement("premium_native", isNative = true, currentValue = { premiumNative }) {
                premiumNative = it
            },
            RemotePlacement("dashboard_native", isNative = true, currentValue = { dashboardNative }) {
                dashboardNative = it
            },
            RemotePlacement("splash_interstitial", currentValue = { splashInt }) {
                splashInt = it
            },
            RemotePlacement("translate_interstitial", currentValue = { translateInt }) {
                translateInt = it
            }
        ).forEach { remoteConfig.applyPlacement(it) }
    }

    private fun FirebaseRemoteConfig.applyPlacement(placement: RemotePlacement) {
        val defaultRemoteKey = placement.currentValue()
        val isEnabled = booleanOrDefault(
            "${placement.key}_enabled",
            defaultRemoteKey.contains("am")
        )

        remoteFloors[placement.key] = doubleOrDefault("${placement.key}_floor", 0.0)

        val overrideRemoteKey = optionalString("${placement.key}_remote_key")
        val remoteKey = if (!isEnabled) {
            "off"
        } else if (!overrideRemoteKey.isNullOrBlank()) {
            overrideRemoteKey
        } else {
            val network = optionalString("${placement.key}_network")
                ?: defaultRemoteKey.substringBefore("_", "am")
            val nativeLayout = optionalString("${placement.key}_native_layout")
                ?: defaultRemoteKey.substringAfter("_", "")

            if (placement.isNative && nativeLayout.isNotBlank()) {
                "${network}_${nativeLayout}"
            } else {
                network
            }
        }

        placement.applyValue(remoteKey)
    }

    private fun FirebaseRemoteConfig.booleanOrDefault(key: String, defaultValue: Boolean): Boolean {
        return all[key]?.asBoolean() ?: defaultValue
    }

    private fun FirebaseRemoteConfig.longOrDefault(key: String, defaultValue: Long): Long {
        return all[key]?.asLong() ?: defaultValue
    }

    private fun FirebaseRemoteConfig.doubleOrDefault(key: String, defaultValue: Double): Double {
        return all[key]?.asDouble() ?: defaultValue
    }

    private fun FirebaseRemoteConfig.optionalString(key: String): String? {
        return all[key]?.asString()?.trim()?.takeIf { it.isNotEmpty() }
    }

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

    fun loadAndShowSharedBottomNativeAd(
        activity: Activity,
        frameLayout: FrameLayout
    ) {
        loadAndShowNativeAd(
            activity = activity,
            adIds = AdIds.nativeAdIdSharedBottom,
            remoteKey = sharedBottomNative,
            frameLayout = frameLayout,
            shimmerLayout = R.layout.large_native_shimmer
        )
    }

    fun loadAndShowNativeAd(
        activity: Activity,
        adIds: Array<String> = AdIds.nativeAdIdAdMobExit,
        remoteKey: String,
        frameLayout: FrameLayout,
        shimmerLayout: Int? = null,
        callBack: LoadAdCallBack? = null
    ) {
        logAdEvent("ad_native_request", remoteKey, "native")

        if (!remoteKey.contains("am")) {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            logAdEvent("ad_native_skipped", remoteKey, "native", "remote_off")
            callBack?.onFailed()
            return
        }

        if (!Misc.checkInternetConnection(activity)) {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            logAdEvent("ad_native_skipped", remoteKey, "native", "no_internet")
            callBack?.onFailed()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
            logAdEvent("ad_native_skipped", remoteKey, "native", "purchased")
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
                    logAdEvent("ad_native_loaded", remoteKey, "native")

                    AdmobNativeAds.showNativeAd(
                        context = activity,
                        remoteKey = remoteKey,
                        amLayout = frameLayout,
                        adIds = adIds
                    )

                    callBack?.onLoaded()
                }

                override fun onFailed() {
                    frameLayout.removeAllViews()
                    frameLayout.visibility = View.GONE
                    logAdEvent("ad_native_failed", remoteKey, "native")

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
            logAdEvent("ad_interstitial_skipped", remote, "interstitial", "remote_off")
            callback?.onDismiss()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            Log.d(Misc.logKey, "Interstitial skipped: user purchased.")
            logAdEvent("ad_interstitial_skipped", remote, "interstitial", "purchased")
            callback?.onDismiss()
            return
        }

        Log.d(Misc.logKey, "Interstitial show requested.")
        logAdEvent("ad_interstitial_show", remote, "interstitial")

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
            logAdEvent("ad_interstitial_skipped", remoteKey, "interstitial", "already_showing")
            return
        }

        if (!remoteKey.contains("am")) {
            Log.d(Misc.logKey, "Interstitial load/show skipped: remote off.")
            logAdEvent("ad_interstitial_skipped", remoteKey, "interstitial", "remote_off")
            callBack?.onDismiss()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            Log.d(Misc.logKey, "Interstitial load/show skipped: user purchased.")
            logAdEvent("ad_interstitial_skipped", remoteKey, "interstitial", "purchased")
            callBack?.onDismiss()
            return
        }

        if (AdmobInterstitialAd.isInterAdAvailableFor(adIds)) {
            Log.d(Misc.logKey, "Interstitial already loaded. Showing now.")
            logAdEvent("ad_interstitial_show", remoteKey, "interstitial", "preloaded")

            AdmobInterstitialAd.showInterstitial(
                activity = activity,
                callback = callBack,
                nextPreloadAdIds = adIds
            )
            return
        }

        if (!Misc.checkInternetConnection(activity)) {
            Log.d(Misc.logKey, "Interstitial failed: no internet.")
            logAdEvent("ad_interstitial_skipped", remoteKey, "interstitial", "no_internet")
            callBack?.onDismiss()
            return
        }

        val objDialog = Misc.LoadingAdDialog(activity)
        objDialog.setCancelable(false)

        if (!activity.isFinishing && !activity.isDestroyed) {
            objDialog.show()
        }

        isShowingInt = true
        logAdEvent("ad_interstitial_request", remoteKey, "interstitial")

        AdmobInterstitialAd.loadInterAdmob(
            context = activity,
            adIds = adIds,
            callBack = object : LoadAdCallBack {

                override fun onLoaded() {
                    if (!activity.isFinishing && !activity.isDestroyed && objDialog.isShowing) {
                        objDialog.dismiss()
                    }

                    isShowingInt = false
                    logAdEvent("ad_interstitial_loaded", remoteKey, "interstitial")

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
                    logAdEvent("ad_interstitial_failed", remoteKey, "interstitial")
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
            logAdEvent("ad_rewarded_skipped", remoteKey, "rewarded_interstitial", "remote_off")
            callBack?.onDismiss()
            return
        }

        if (!Misc.checkInternetConnection(activity)) {
            logAdEvent("ad_rewarded_skipped", remoteKey, "rewarded_interstitial", "no_internet")
            callBack?.onDismiss()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            logAdEvent("ad_rewarded_skipped", remoteKey, "rewarded_interstitial", "purchased")
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
                    logAdEvent("ad_rewarded_loaded", remoteKey, "rewarded_interstitial")

                    if (!activity.isFinishing && !activity.isDestroyed && objDialog.isShowing) {
                        objDialog.dismiss()
                    }

                    AdmobRewardedInterstitial.showInterstitial(activity, callBack)
                }

                override fun onFailed() {
                    logAdEvent("ad_rewarded_failed", remoteKey, "rewarded_interstitial")

                    if (!activity.isFinishing && !activity.isDestroyed && objDialog.isShowing) {
                        objDialog.dismiss()
                    }

                    callBack?.onDismiss()
                }
            }
        )
    }

    private fun logAdEvent(
        eventName: String,
        placement: String,
        format: String,
        reason: String? = null
    ) {
        val params = Bundle().apply {
            putString("ad_placement", placement.take(40))
            putString("ad_format", format)
            reason?.let { putString("reason", it.take(40)) }
        }
        Firebase.analytics.logEvent(eventName, params)
    }
}
