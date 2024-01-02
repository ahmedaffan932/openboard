package org.dslul.openboard.translator.pro.classes.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack


object AdmobInterstitialAd {
    var interAdmob: InterstitialAd? = null
    var isIntLoading = false

    fun loadInterAdmob(
        context: Context,
        adId: String = AdIds.interstitialAdIdAdMobOne,
        callback: LoadAdCallBack? = null
    ) {
        if (adId == "") {
            return
        }

        if (Misc.getPurchasedStatus(context)) {
            return
        }

        isIntLoading = true

        val admobRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adId,
            admobRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("loadAdmob?", adError.message + adError.code)
                    interAdmob = null
                    isIntLoading = false
                    callback?.onFailed()

                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("loadAdmob?", "Ad was loaded.")
                    interAdmob = interstitialAd
                    isIntLoading = false
                    callback?.onLoaded()
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, callback: InterstitialCallBack?) {
        if (interAdmob == null) {
            callback?.onDismiss()
            return
        }

        interAdmob?.show(activity)
        interAdmob?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("interAdmobShow", "Ad was dismissed.")
                interAdmob = null
                loadInterAdmob(activity)
                callback?.onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d("interAdmobShow", "Ad failed to show." + adError.message + adError.code)
                interAdmob = null
                callback?.onDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("interAdmobShow", "Ad showed fullscreen content.")
            }
        }

    }

}