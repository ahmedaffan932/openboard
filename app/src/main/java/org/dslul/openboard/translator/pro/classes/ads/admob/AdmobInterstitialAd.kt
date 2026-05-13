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
    private var isLoadingInterAd = false

    fun loadInterAdmob(
        context: Context,
        adIds: Array<String> = AdIds.interstitialAdIdAdMobSplash,
        callBack: LoadAdCallBack? = null
    ) {
        if (isLoadingInterAd) {
            Log.d(Misc.logKey, "Interstitial load skipped: already loading.")
            return
        }

        if (interAdmob != null) {
            Log.d(Misc.logKey, "Interstitial load skipped: ad already available.")
            callBack?.onLoaded()
            return
        }

        if (Misc.getPurchasedStatus(context)) {
            Log.d(Misc.logKey, "Interstitial load skipped: user purchased.")
            callBack?.onFailed()
            return
        }

        if (adIds.isEmpty()) {
            Log.d(Misc.logKey, "Interstitial load failed: adIds array is empty.")
            callBack?.onFailed()
            return
        }

        isLoadingInterAd = true

        loadInterByIndex(
            context = context.applicationContext,
            adIds = adIds,
            index = 0,
            callBack = callBack
        )
    }

    private fun loadInterByIndex(
        context: Context,
        adIds: Array<String>,
        index: Int,
        callBack: LoadAdCallBack?
    ) {
        if (index >= adIds.size) {
            isLoadingInterAd = false
            interAdmob = null

            Log.d(Misc.logKey, "Interstitial all ad ids failed.")

            callBack?.onFailed()
            return
        }

        val currentAdId = adIds[index]

        if (currentAdId.isBlank()) {
            Log.d(Misc.logKey, "Interstitial skipped blank ad id at index: $index")

            loadInterByIndex(
                context = context,
                adIds = adIds,
                index = index + 1,
                callBack = callBack
            )
            return
        }

        Log.d(Misc.logKey, "Interstitial trying ad id index $index: $currentAdId")

        val admobRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            currentAdId,
            admobRequest,
            object : InterstitialAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interAdmob = null

                    Log.d(
                        Misc.logKey,
                        "Interstitial failed at index $index: ${adError.code} | ${adError.message}"
                    )

                    loadInterByIndex(
                        context = context,
                        adIds = adIds,
                        index = index + 1,
                        callBack = callBack
                    )
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    isLoadingInterAd = false
                    interAdmob = interstitialAd

                    Log.d(Misc.logKey, "Interstitial loaded successfully with index: $index")

                    callBack?.onLoaded()
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        callback: InterstitialCallBack? = null
    ) {
        val ad = interAdmob

        if (ad == null) {
            Log.d(Misc.logKey, "Interstitial show skipped: ad not available.")

            callback?.onDismiss()

            if (Ads.isIntPreLoad) {
                loadInterAdmob(activity)
            }

            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            Log.d(Misc.logKey, "Interstitial show skipped: user purchased.")

            interAdmob = null
            callback?.onDismiss()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {
                Log.d(Misc.logKey, "Interstitial dismissed.")

                interAdmob = null
                Ads.isShowingInt = false

                callback?.onDismiss()

                if (Ads.isIntPreLoad) {
                    loadInterAdmob(activity)
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(
                    Misc.logKey,
                    "Interstitial failed to show: ${adError.code} | ${adError.message}"
                )

                interAdmob = null
                Ads.isShowingInt = false

                callback?.onDismiss()

                if (Ads.isIntPreLoad) {
                    loadInterAdmob(activity)
                }
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(Misc.logKey, "Interstitial showed fullscreen content.")

                Ads.isShowingInt = true

                callback?.onAdDisplayed()
            }

            override fun onAdClicked() {
                Log.d(Misc.logKey, "Interstitial clicked.")
            }

            override fun onAdImpression() {
                Log.d(Misc.logKey, "Interstitial impression recorded.")
            }
        }

        ad.show(activity)
    }

    fun clearInterAd() {
        interAdmob = null
        isLoadingInterAd = false
        Ads.isShowingInt = false
    }
}