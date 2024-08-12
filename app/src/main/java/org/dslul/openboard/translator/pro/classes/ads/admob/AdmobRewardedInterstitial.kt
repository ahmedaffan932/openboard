package org.dslul.openboard.translator.pro.classes.ads.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

object AdmobRewardedInterstitial {
    var interAdmob: RewardedInterstitialAd? = null

    fun loadRewardedInterAdmob(
        context: Context,
        adId: String = AdIds.rewardedInterstitialAdIdAdMob,
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
        RewardedInterstitialAd.load(
            context,
            adId,
            admobRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("RewardedInterstitialLog", adError.message + adError.code)
                    callBack?.onFailed()
                    interAdmob = null
                }

                override fun onAdLoaded(interstitialAd: RewardedInterstitialAd) {
                    Log.d("RewardedInterstitialLog", "Ad was loaded.")
                    interAdmob = interstitialAd
                    callBack?.onLoaded()
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, callback: InterstitialCallBack?) {
        if (interAdmob == null) {
            callback?.onDismiss()
            return
        }

        interAdmob?.show(activity) { }

        interAdmob?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                callback?.onDismiss()
                Log.d("interAdmobShow", "Ad was dismissed.")
                Ads.isShowingInt = false
                interAdmob = null
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