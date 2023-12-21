package org.dslul.openboard.translator.pro.classes.admob

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdRequest.Builder
import org.dslul.openboard.translator.pro.classes.Misc


object AdmobBannerAds {
    private var adView: AdView? = null
    var isLoaded = false

    fun load(context: Activity, adId: String = AdIds.bannerAdIdAdOne) {
        if (!Misc.getPurchasedStatus(context)) {
            adView = AdView(context)
            adView?.adUnitId = adId
            val adSize = getAdSize(context)
            adView?.setAdSize(adSize)
            val adRequest = Builder().build()
            adView?.loadAd(adRequest)

            adView!!.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isLoaded = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(
                        Misc.logKey,
                        "onAdFailedToLoad:Adaptive Banner  ${adError.code}: ${adError.message}"
                    )
                    isLoaded = false

                }

                override fun onAdOpened() {}
                override fun onAdClicked() {}
                override fun onAdClosed() {}
            }

        } else {
            adView = null
        }

    }

    private fun getAdSize(context: Activity): AdSize {
        val display: Display = context.windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        val density = displayMetrics.density
        var adwidthpixels: Float = displayMetrics.widthPixels.toFloat()
        if (adwidthpixels == 0f) {
            adwidthpixels = displayMetrics.widthPixels.toFloat()
        }
        val adWith = (adwidthpixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWith)
    }

    fun show(view: FrameLayout) {
        if (adView != null && isLoaded) {
            view.visibility = View.VISIBLE
            if (adView?.parent != null) {
                (adView?.parent as ViewGroup).removeAllViews() // <- fix
            }
            view.removeAllViews()
            view.addView(adView)
        } else {
            view.visibility = View.GONE
        }
    }

    fun loadCollapsibleBanner(
        context: Activity,
        remoteKey: String,
        collapsibleAdView: AdView,
        callback: LoadAdCallBack? = null
    ) {
        if (Misc.getPurchasedStatus(context)) {
            return
        }
        if (remoteKey.contains("am_collapsible")) {
            val extras = Bundle()
            extras.putString("collapsible", "bottom")

            val adRequest = Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()
            collapsibleAdView.loadAd(adRequest)

            collapsibleAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isLoaded = true
                    collapsibleAdView.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(
                        Misc.logKey,
                        "Banner  ${adError.code}: ${adError.message}"
                    )
                    isLoaded = false
                    callback?.onFailed()
                }

                override fun onAdOpened() {}
                override fun onAdClicked() {}
                override fun onAdClosed() {}
            }
        }
    }
}