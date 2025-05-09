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

    var enableKeyboardInt: String = "am"
    var cameraTranslationBanner: String = "am"
    var cameraTranslationInt: String = "am"
    var exitInt: String = "am"
    var onBoardingNative: String = "am_lctr"
    var dashboardInt: String = "am"
    var showIntAfterBreak: Boolean = false
    var isIntPreLoad: Boolean = false
    var isNativeAdPreload: Boolean = true
    var documentTranslateAdEnable: Boolean = true
    var isCameraAdEnabled: Boolean = false
    var showAppLanguageSelectorLoading: Boolean = true
    var cameraInt: String = "am"
    var isAppOpenAdEnabled: Boolean = true
    var exitNative: String = "am_large_hctr"
    var gameNative: String = "am_large_hctr"
    var ocrInt: String = "am"
    var gameInt: String = "am"
    var chatInt: String = "am"
    var phraseInt: String = "am"

    var viewDailyQuoteAdEnabled: Boolean = true
    var phraseNative: String = "am_small_lctr"
    var chatNative: String = "am_small_hctr"
    var languageSelectorInt: String = "am"
    var translateNative: String = "am_large_hctr_bottom"
    var dashboardNative: String = "am_small_lctr_collapsible_native"
    var splashInt: String = "am"
    var appLanguageSelectorNative: String = "am_small_hctr_native_collapsible"
    var translateInt: String = "am_fb"

    private var lastAdLoadStartTime: Long = 0
    private val MIN_AD_LOAD_TIME: Long = 30000


    var isShowingInt = false

    fun showBannerAd(frameLayout: FrameLayout, remoteKey: String) {
        if (remoteKey.contains("am")) {
            AdmobBannerAds.show(frameLayout)
        }
    }

    fun loadAndShowNativeAd(
        activity: Activity,
        adId: String,
        remoteKey: String,
        frameLayout: FrameLayout,
        adLayout: Int,
        isSmall: Boolean = true
    ) {
        if (remoteKey.contains("am"))
            AdmobNativeAds.loadAdmobNative(activity, adId, object : LoadAdCallBack {
                override fun onLoaded() {
                    AdmobNativeAds.showNativeAd(activity, remoteKey, frameLayout, adLayout)
                }

                override fun onFailed() {
                    frameLayout.removeAllViews()
                }
            }, frameLayout, isSmall)
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
        adId: String = AdIds.interstitialAdIdAdMob,
        callBack: InterstitialCallBack? = null
    ) {
        if (remoteKey.contains("am")) {
            if(showIntAfterBreak) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAdLoadStartTime < MIN_AD_LOAD_TIME) {
                    callBack?.onDismiss()
                    return
                }
                lastAdLoadStartTime = currentTime
            }
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