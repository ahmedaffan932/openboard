package org.dslul.openboard.translator.pro.classes.ads.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack


object AdmobInterstitialAd {
    var interAdmob: InterstitialAd? = null

    fun loadInterAdmob(
        context: Context,
        adId: String = AdIds.interstitialAdIdAdMob,
        callBack: LoadAdCallBack? = null
    ) {
        if (adId == "") {
            callBack?.onFailed()
            return
        }

        if (Misc.getPurchasedStatus(context)) {
            callBack?.onFailed()
            return
        }
        val admobRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adId,
            admobRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("loadAdmob?", adError.message + adError.code)
                    callBack?.onFailed()
                    interAdmob = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("loadAdmob?", "Ad was loaded.")
                    interAdmob = interstitialAd
                    callBack?.onLoaded()
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, callback: InterstitialCallBack?) {
        if (interAdmob == null) {
            callback?.onDismiss()
//            loadInterAdmob(activity)
            return
        }

        interAdmob?.show(activity)
        interAdmob?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                callback?.onDismiss()
                Log.d("interAdmobShow", "Ad was dismissed.")
                Ads.isShowingInt = false
                interAdmob = null
                if (Ads.isIntPreLoad)
                    loadInterAdmob(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d("interAdmobShow", "Ad failed to show." + adError.message + adError.code)
                interAdmob = null
                Ads.isShowingInt = false
                callback?.onDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("interAdmobShow", "Ad showed fullscreen content.")
                Ads.isShowingInt = true
                callback?.onAdDisplayed()
            }
        }
    }
}