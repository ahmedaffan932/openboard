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
    private fun loadInterAdmob(activity: Activity, callback: LoadInterstitialCallBack? = null) {
        Log.d("Checking Var", BuildConfig.DEBUG.toString())
        if (!Misc.getPurchasedStatus(activity) && interAdmob == null) {
            val admobRequest = AdRequest.Builder().build()
            com.google.android.gms.ads.interstitial.InterstitialAd.load(
                activity,
                Misc.interstitialAdIdAdMobOne,
                admobRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("loadAdmob?", adError.message + adError.code)
                        interAdmob = null
                        callback?.onFailed()
                    }

                    override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                        Log.d("loadAdmob?", "Ad was loaded.")
                        interAdmob = interstitialAd
                        Misc.anyAdLoaded.value = true
                        callback?.onLoaded()
                    }
                })
        } else {
            callback?.onLoaded()
        }
    }

    fun showInterstitial(activity: Activity, remote: String, callback: (() -> Unit)? = null) {
        if (Misc.getPurchasedStatus(activity)) {
            callback?.invoke()
            return
        }

        if (remote.contains("am")) {
            interAdmob?.show(activity)
            interAdmob?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("interAdmobShow", "Ad was dismissed.")
                    interAdmob = null
                    callback?.invoke()
                    Misc.isInterstitialDisplaying = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    Log.d("interAdmobShow", "Ad failed to show." + p0.message + p0.code)
                    interAdmob = null
                    callback?.invoke()
                    Misc.isInterstitialDisplaying = false
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("interAdmobShow", "Ad showed fullscreen content.")
                    interAdmob = null
                    Misc.isInterstitialDisplaying = true
                }
            }
        } else {
            Log.d("interAdmobShow", "The interstitial ad wasn't ready yet.")
            callback?.invoke()
        }
    }


}