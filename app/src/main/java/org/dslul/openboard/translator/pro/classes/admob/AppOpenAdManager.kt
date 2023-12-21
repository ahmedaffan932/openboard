package org.dslul.openboard.translator.pro.classes.admob

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import java.util.Date

object AppOpenAdManager {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var loadTime: Long = 0

    fun loadAd(
        context: Activity,
        adId: String = AdIds.appOpenAdIdOne,
        callBack: LoadAdCallBack? = null
    ) {
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        if (Misc.getPurchasedStatus(context)) {
            return
        }

        Log.d(Misc.logKey, "AppOpen Loading.....")


        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            adId,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    callBack?.onLoaded()
                    Log.d(Misc.logKey, "AppOpen onAdLoaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    callBack?.onFailed()
                    isLoadingAd = false
                    Log.d(Misc.logKey, "AppOpen onAdFailedToLoad: " + loadAdError.message)
                }
            }
        )
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    fun showAdIfAvailable(
        activity: Activity,
        remoteKey: Boolean = true,
        callBack: InterstitialCallBack? = null
    ) {
        if (!isAdAvailable() || !remoteKey || isShowingAd) {
            loadAd(activity,)
            return
        }

        appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                Log.d(Misc.logKey, "App Open Ad dismissed")

                callBack?.onDismiss()
                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.d(Misc.logKey, "App Open Ad Failed to Show: " + adError.message)

                callBack?.onDismiss()
                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(Misc.logKey, "App Open Ad showed.")
                callBack?.onAdDisplayed()
            }
        }
        isShowingAd = true
        appOpenAd!!.show(activity)
    }
}