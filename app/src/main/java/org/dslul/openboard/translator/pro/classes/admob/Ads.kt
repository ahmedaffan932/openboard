package org.dslul.openboard.translator.pro.classes.admob

import android.app.Activity
import android.util.Log
import android.widget.FrameLayout
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

object Ads {

    var translateInt: String = "am"
    var exitNative: String = "am_large_hctr"
    var splashInt: String = "am"
    var phraseNative: String = "am_small_hctr"
    var dashboardNative: String = "am"
    var languageSelectorOnBackInt: String = "am"
    var phraseInt: String = "am"
    var dashboardBanner: String = "am"
    var enableKeyboardInt: String = "am"
    var cameraTranslationInt: String = "am"
    var cameraTranslationBanner: String = "am"
    var onBoardingNative: String = "am_small_hctr"
    var splashNative: String = "am_large_hctr"

    var isSplashAppOpenEnabled: Boolean = true

    fun showBannerAd(frameLayout: FrameLayout, remoteKey: String) {
        if (remoteKey.contains("am")) {
            AdmobBannerAds.show(frameLayout)
        }
    }

    fun showNativeAd(
        activity: Activity,
        remoteKey: String,
        amLayout: FrameLayout
    ) {
        Log.d(Misc.logKey, remoteKey)
        if (remoteKey.contains("am")) {
            AdmobNativeAds.showNativeAd(activity, remoteKey, amLayout)
        }
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
}