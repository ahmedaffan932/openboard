package org.dslul.openboard.translator.pro.classes.ads.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import java.util.Date

object AppOpenAdManager {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false

    private var loadTime: Long = 0L

    fun loadAd(
        context: Context,
        adIds: Array<String> = AdIds.appOpenAdIdSplash,
        callBack: LoadAdCallBack? = null
    ) {
        if (isLoadingAd) {
            Log.d(Misc.logKey, "AppOpen load skipped: already loading.")
            return
        }

        if (isAdAvailable()) {
            Log.d(Misc.logKey, "AppOpen load skipped: ad already available.")
            callBack?.onLoaded()
            return
        }

        if (Misc.getPurchasedStatus(context)) {
            Log.d(Misc.logKey, "AppOpen load skipped: user purchased.")
            callBack?.onFailed()
            return
        }

        if (adIds.isEmpty()) {
            Log.d(Misc.logKey, "AppOpen load failed: adIds array is empty.")
            callBack?.onFailed()
            return
        }

        Log.d(Misc.logKey, "AppOpen Loading started...")

        isLoadingAd = true

        loadAdByIndex(
            context = context.applicationContext,
            adIds = adIds,
            index = 0,
            callBack = callBack
        )
    }

    private fun loadAdByIndex(
        context: Context,
        adIds: Array<String>,
        index: Int,
        callBack: LoadAdCallBack?
    ) {
        if (index >= adIds.size) {
            isLoadingAd = false
            Log.d(Misc.logKey, "AppOpen all ad ids failed.")
            callBack?.onFailed()
            return
        }

        val currentAdId = adIds[index]

        if (currentAdId.isBlank()) {
            Log.d(Misc.logKey, "AppOpen skipped blank ad id at index: $index")

            loadAdByIndex(
                context = context,
                adIds = adIds,
                index = index + 1,
                callBack = callBack
            )
            return
        }

        Log.d(Misc.logKey, "AppOpen trying ad id index $index: $currentAdId")

        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            currentAdId,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time

                    Log.d(Misc.logKey, "AppOpen loaded successfully with index: $index")

                    callBack?.onLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(
                        Misc.logKey,
                        "AppOpen failed at index $index: ${loadAdError.message}"
                    )

                    loadAdByIndex(
                        context = context,
                        adIds = adIds,
                        index = index + 1,
                        callBack = callBack
                    )
                }
            }
        )
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour = 3600000L
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    fun showIfAvailable(
        activity: Activity,
        remoteKey: Boolean = true,
        callBack: InterstitialCallBack? = null
    ) {
        if (isShowingAd) {
            Log.d(Misc.logKey, "AppOpen show skipped: already showing.")
            return
        }

        if (!remoteKey) {
            Log.d(Misc.logKey, "AppOpen show skipped: remote key is off.")
            callBack?.onDismiss()
            return
        }

        if (Misc.getPurchasedStatus(activity)) {
            Log.d(Misc.logKey, "AppOpen show skipped: user purchased.")
            callBack?.onDismiss()
            return
        }

        val ad = appOpenAd

        if (ad == null || !isAdAvailable()) {
            Log.d(Misc.logKey, "AppOpen ad not available.")
            callBack?.onDismiss()

            loadAd(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false

                Log.d(Misc.logKey, "AppOpen dismissed.")

                callBack?.onDismiss()

                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false

                Log.d(Misc.logKey, "AppOpen failed to show: ${adError.message}")

                callBack?.onDismiss()

                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(Misc.logKey, "AppOpen showed.")
                callBack?.onAdDisplayed()
            }

            override fun onAdClicked() {
                Log.d(Misc.logKey, "AppOpen clicked.")
            }

            override fun onAdImpression() {
                Log.d(Misc.logKey, "AppOpen impression recorded.")
            }
        }

        isShowingAd = true
        ad.show(activity)
    }

    fun clearAd() {
        appOpenAd = null
        isLoadingAd = false
        isShowingAd = false
        loadTime = 0L
    }
}