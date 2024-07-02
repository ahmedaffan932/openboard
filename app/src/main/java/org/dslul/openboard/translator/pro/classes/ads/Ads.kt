package org.dslul.openboard.translator.pro.classes.ads

import android.app.Activity
import android.util.Log
import android.widget.FrameLayout
import com.example.translatorguru.ads.admob.LoadAdCallBack
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobBannerAds
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobInterstitialAd
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobNativeAds
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

object Ads {

    var dashboardInt: String ="am"
    var isIntPreLoad: Boolean = true
    var isNativeAdPreload: Boolean = true
    var isSplashAppOpenAdEnabled: Boolean = true
    var chatBanner: String = "am"
    var languageSelectorBanner: String = "am"
    var splashNative: String = "am_native_splash"
    var appLanguagesSelectorNative: String = "am_large_hctr"
    var cameraTranslationInt: String = "am"
    var exitInt: String = "am"
    var onBoardingNative: String = "am_small_hctr_native"
    var exitNative: String = "am_large_hctr"
    var phraseInt: String = "am"
    var translateNative: String = "am_large_hctr_bottom"
    var dashboardNative: String = "am"
    var splashInt: String = "am"
    var translateInt: String = "am"

    var isShowingInt = false

    fun showBannerAd(frameLayout: FrameLayout, remoteKey: String) {
        if (remoteKey.contains("am")) {
            AdmobBannerAds.show(frameLayout)
        }
    }

    fun loadAndShowNativeAd(
        activity: Activity,
        adId: String = AdIds.nativeAdIdAdMobExit,
        remoteKey: String,
        frameLayout: FrameLayout,
        adLayout: Int,
        shimmerLayout: Int? = null,
        callBack: LoadAdCallBack? = null
    ) {
        if (remoteKey.contains("am"))
            AdmobNativeAds.loadAdmobNative(activity, adId, shimmerLayout, callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    AdmobNativeAds.showNativeAd(activity, remoteKey, frameLayout, adLayout)
                    callBack?.onLoaded()
                }

                override fun onFailed() {
                    callBack?.onFailed()
                    frameLayout.removeAllViews()
                }
            }, frameLayout)
    }

    fun showInterstitial(
        activity: Activity,
        remote: String,
        callback: InterstitialCallBack? = null
    ) {
        if (remote.contains("am")) {
            Log.d(Misc.logKey, "Int am")
            AdmobInterstitialAd.showInterstitial(activity, callback)
        } else {
            Log.d(Misc.logKey, "Int off")
            callback?.onDismiss()
        }
    }


    fun loadAndShowInterstitial(
        activity: Activity,
        remoteKey: String,
        adId: String = AdIds.interstitialAdIdAdMobPhrases,
        callBack: InterstitialCallBack? = null
    ) {
        if (remoteKey.contains("am")) {
            val objDialog = Misc.LoadingAdDialog(activity)
            objDialog.show()
            isShowingInt = true

            if (AdmobInterstitialAd.interAdmob != null) {
                android.os.Handler().postDelayed({
                    AdmobInterstitialAd.showInterstitial(activity, callBack)
                    isShowingInt = false
                    objDialog.dismiss()
                }, 100)
            } else {
                AdmobInterstitialAd.loadInterAdmob(activity, adId, object : LoadAdCallBack {
                    override fun onLoaded() {
                        AdmobInterstitialAd.showInterstitial(
                            activity,
                            object : InterstitialCallBack {
                                override fun onDismiss() {
                                    callBack?.onDismiss()
                                    objDialog.dismiss()
                                }
                            })
                        isShowingInt = false
                    }

                    override fun onFailed() {
                        objDialog.dismiss()
                        isShowingInt = false
                        callBack?.onDismiss()
                    }

                })
            }
        } else {
            callBack?.onDismiss()
        }
    }
}