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
    private var loadingInterAdIdsKey: String? = null
    private var loadedInterAdIdsKey: String? = null
    private val pendingLoadCallbacks = mutableListOf<LoadAdCallBack?>()

    fun loadInterAdmob(
        context: Context,
        adIds: Array<String> = AdIds.interstitialAdIdAdMobSplash,
        callBack: LoadAdCallBack? = null
    ) {
        val adIdsKey = adIds.interstitialIdsKey()

        if (isLoadingInterAd) {
            if (loadingInterAdIdsKey == adIdsKey) {
                pendingLoadCallbacks.add(callBack)
                Log.d(Misc.logKey, "Interstitial load queued: same ad ids already loading.")
            } else {
                Log.d(Misc.logKey, "Interstitial load skipped: different ad ids already loading.")
                callBack?.onFailed()
            }
            return
        }

        if (interAdmob != null) {
            if (loadedInterAdIdsKey == adIdsKey) {
                Log.d(Misc.logKey, "Interstitial load skipped: requested ad already available.")
                callBack?.onLoaded()
                return
            }

            Log.d(Misc.logKey, "Interstitial cached ad replaced for requested placement.")
            interAdmob = null
            loadedInterAdIdsKey = null
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
        loadingInterAdIdsKey = adIdsKey
        pendingLoadCallbacks.add(callBack)

        loadInterByIndex(
            context = context.applicationContext,
            adIds = adIds,
            index = 0,
            adIdsKey = adIdsKey
        )
    }

    private fun loadInterByIndex(
        context: Context,
        adIds: Array<String>,
        index: Int,
        adIdsKey: String
    ) {
        if (index >= adIds.size) {
            isLoadingInterAd = false
            loadingInterAdIdsKey = null
            loadedInterAdIdsKey = null
            interAdmob = null

            Log.d(Misc.logKey, "Interstitial all ad ids failed.")

            notifyPendingLoadFailed()
            return
        }

        val currentAdId = adIds[index]

        if (currentAdId.isBlank()) {
            Log.d(Misc.logKey, "Interstitial skipped blank ad id at index: $index")

            loadInterByIndex(
                context = context,
                adIds = adIds,
                index = index + 1,
                adIdsKey = adIdsKey
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
                        adIdsKey = adIdsKey
                    )
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    isLoadingInterAd = false
                    loadingInterAdIdsKey = null
                    loadedInterAdIdsKey = adIdsKey
                    interAdmob = interstitialAd

                    Log.d(Misc.logKey, "Interstitial loaded successfully with index: $index")

                    notifyPendingLoadSuccess()
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        callback: InterstitialCallBack? = null,
        nextPreloadAdIds: Array<String> = AdIds.interstitialAdIdAdMobSplash
    ) {
        val ad = interAdmob

        if (ad == null) {
            Log.d(Misc.logKey, "Interstitial show skipped: ad not available.")

            callback?.onDismiss()

            if (Ads.isIntPreLoad) {
                loadInterAdmob(activity, nextPreloadAdIds)
            }

            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            Log.d(Misc.logKey, "Interstitial show skipped: user purchased.")

            interAdmob = null
            loadedInterAdIdsKey = null
            callback?.onDismiss()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {
                Log.d(Misc.logKey, "Interstitial dismissed.")

                interAdmob = null
                loadedInterAdIdsKey = null
                Ads.isShowingInt = false

                callback?.onDismiss()

                if (Ads.isIntPreLoad) {
                    loadInterAdmob(activity, nextPreloadAdIds)
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(
                    Misc.logKey,
                    "Interstitial failed to show: ${adError.code} | ${adError.message}"
                )

                interAdmob = null
                loadedInterAdIdsKey = null
                Ads.isShowingInt = false

                callback?.onDismiss()

                if (Ads.isIntPreLoad) {
                    loadInterAdmob(activity, nextPreloadAdIds)
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

    fun isInterAdAvailableFor(adIds: Array<String>): Boolean {
        return interAdmob != null && loadedInterAdIdsKey == adIds.interstitialIdsKey()
    }

    fun clearInterAd() {
        interAdmob = null
        isLoadingInterAd = false
        loadingInterAdIdsKey = null
        loadedInterAdIdsKey = null
        pendingLoadCallbacks.clear()
        Ads.isShowingInt = false
    }

    private fun notifyPendingLoadSuccess() {
        val callbacks = pendingLoadCallbacks.toList()
        pendingLoadCallbacks.clear()
        callbacks.forEach { it?.onLoaded() }
    }

    private fun notifyPendingLoadFailed() {
        val callbacks = pendingLoadCallbacks.toList()
        pendingLoadCallbacks.clear()
        callbacks.forEach { it?.onFailed() }
    }

    private fun Array<String>.interstitialIdsKey(): String {
        return joinToString(separator = "|")
    }
}
