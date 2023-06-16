package org.dslul.openboard.translator.pro.classes.admob

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.interfaces.LoadInterstitialCallBack


object InterstitialAd {

    var interAdmob: com.google.android.gms.ads.interstitial.InterstitialAd? = null

    //load Admob Interstitial
    private fun loadInterAdmob(
        activity: Activity,
        adId: String = Misc.interstitialAdIdAdMobOne/*, callback: LoadInterstitialCallBack? = null*/
    ) {
        if (!Misc.getPurchasedStatus(activity)) {
            val admobRequest = AdRequest.Builder().build()
            com.google.android.gms.ads.interstitial.InterstitialAd.load(
                activity,
                adId,
                admobRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("logKeyInt", adError.message + adError.code)
                        interAdmob = null

                        if (adId == Misc.interstitialAdIdAdMobOne) {
                            loadInterAdmob(activity, Misc.interstitialAdIdAdMobTwo)
                        } else if (adId == Misc.interstitialAdIdAdMobTwo) {
                            loadInterAdmob(activity, Misc.interstitialAdIdAdMobThree)
                        }
                    }

                    override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                        Log.d("logKeyInt", "Ad was loaded.")
                        interAdmob = interstitialAd
                        Misc.anyAdLoaded.value = true
                    }
                }
            )
        }
    }


    //load Admob Interstitial
    fun manageLoadInterAdmob(activity: Activity) {
        loadInterAdmob(activity)
    }


    fun show(activity: Activity, remote: String, callback: (() -> Unit)? = null) {
        if (Misc.getPurchasedStatus(activity)) {
            callback?.invoke()
            return
        }
        if (remote.contains("am")) {
            showInterstitial(activity, callback)
        }
    }

    private fun showInterstitial(activity: Activity, callback: (() -> Unit)? = null) {
        interAdmob?.show(activity)
        interAdmob?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("logKeyInt", "Ad was dismissed.")
                callback?.invoke()
                Misc.isInterstitialDisplaying = false
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Log.d("logKeyInt", "Ad failed to show." + p0.message + p0.code)
                interAdmob = null
                callback?.invoke()
                Misc.isInterstitialDisplaying = false
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("logKeyInt", "Ad showed fullscreen content.")
                manageLoadInterAdmob(activity)
                Misc.isInterstitialDisplaying = true
            }
        }
    }
}