package org.dslul.openboard.translator.pro.classes.ads.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.interfaces.RewardedAdCallBack

object AdmobRewardedAds {
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewardedAd = false
    private val pendingRewardedLoadCallbacks = mutableListOf<LoadAdCallBack>()

    fun loadRewardedAd(
        context: Context,
        adIds: Array<String>,
        callBack: LoadAdCallBack? = null
    ) {
        if (isLoadingRewardedAd) {
            Log.d(Misc.logKey, "Rewarded Ad load skipped: already loading.")
            callBack?.let { pendingRewardedLoadCallbacks.add(it) }
            return
        }

        if (Misc.getPurchasedStatus(context)) {
            Log.d(Misc.logKey, "Rewarded Ad load skipped: user purchased.")
            callBack?.onFailed()
            return
        }

        if (adIds.isEmpty()) {
            Log.d(Misc.logKey, "Rewarded Ad load failed: adIds array is empty.")
            callBack?.onFailed()
            return
        }

        if (rewardedAd != null) {
            Log.d(Misc.logKey, "Rewarded Ad already available.")
            callBack?.onLoaded()
            return
        }

        isLoadingRewardedAd = true
        loadRewardedByIndex(
            context = context.applicationContext,
            adIds = adIds,
            index = 0,
            callBack = callBack
        )
    }

    private fun loadRewardedByIndex(
        context: Context,
        adIds: Array<String>,
        index: Int,
        callBack: LoadAdCallBack?
    ) {
        if (index >= adIds.size) {
            isLoadingRewardedAd = false
            rewardedAd = null
            Log.d(Misc.logKey, "Rewarded Ad all ad ids failed.")
            callBack?.onFailed()
            notifyPendingRewardedLoadFailed()
            return
        }

        val currentAdId = adIds[index]

        if (currentAdId.isBlank()) {
            Log.d(Misc.logKey, "Rewarded Ad skipped blank ad id at index: $index")
            loadRewardedByIndex(
                context = context,
                adIds = adIds,
                index = index + 1,
                callBack = callBack
            )
            return
        }

        Log.d(Misc.logKey, "Rewarded Ad trying index $index: $currentAdId")

        RewardedAd.load(
            context,
            currentAdId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    Log.e(
                        Misc.logKey,
                        "Rewarded Ad failed at index $index: ${adError.code} | ${adError.message}"
                    )

                    loadRewardedByIndex(
                        context = context,
                        adIds = adIds,
                        index = index + 1,
                        callBack = callBack
                    )
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    isLoadingRewardedAd = false
                    rewardedAd = ad
                    Log.d(Misc.logKey, "Rewarded Ad loaded successfully with index: $index")
                    callBack?.onLoaded()
                    notifyPendingRewardedLoadLoaded()
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        callBack: RewardedAdCallBack
    ) {
        val adToShow = rewardedAd

        if (adToShow == null) {
            Log.d(Misc.logKey, "Rewarded Ad show failed: ad not available.")
            callBack.onFailed()
            return
        }

        var rewardEarned = false

        adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(Misc.logKey, "Rewarded Ad showed fullscreen content.")
                callBack.onAdDisplayed()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(Misc.logKey, "Rewarded Ad dismissed.")
                rewardedAd = null

                if (rewardEarned) {
                    callBack.onRewardEarned()
                }

                callBack.onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(
                    Misc.logKey,
                    "Rewarded Ad failed to show: ${adError.code} | ${adError.message}"
                )
                rewardedAd = null
                callBack.onFailed()
            }
        }

        adToShow.show(activity) {
            rewardEarned = true
            Log.d(Misc.logKey, "Rewarded Ad reward earned.")
        }
    }

    fun clearRewardedAd() {
        rewardedAd = null
        isLoadingRewardedAd = false
        pendingRewardedLoadCallbacks.clear()
    }

    private fun notifyPendingRewardedLoadLoaded() {
        val callbacks = pendingRewardedLoadCallbacks.toList()
        pendingRewardedLoadCallbacks.clear()
        callbacks.forEach { it.onLoaded() }
    }

    private fun notifyPendingRewardedLoadFailed() {
        val callbacks = pendingRewardedLoadCallbacks.toList()
        pendingRewardedLoadCallbacks.clear()
        callbacks.forEach { it.onFailed() }
    }
}
