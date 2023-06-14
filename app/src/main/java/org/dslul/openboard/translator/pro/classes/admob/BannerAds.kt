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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest.Builder
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import org.dslul.openboard.translator.pro.classes.Misc


object BannerAds {

    interface bannerAdsCallBack {
        fun onFailed(){}
        fun onLoaded(){}
    }

    var adView: AdView? = null

    private var isLoaded = false

    fun load(context: Activity, adId: String = Misc.bannerAdIdOne, callBack: bannerAdsCallBack? = null) {
        adView = AdView(context)
        adView?.adUnitId = adId
        val adSize = getAdSize(context)
        adView?.setAdSize(adSize)
        val adRequest = Builder().build()
        adView?.loadAd(adRequest)

        adView!!.adListener = object : AdListener() {
            override fun onAdLoaded() {
                isLoaded = true
                callBack?.onLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("TAG", "Adaptive Banner  ${adError.code}: ${adError.message}")
                isLoaded = false
                if (adId == Misc.bannerAdIdOne) {
                    load(context, Misc.bannerAdIdTwo)
                } else if (adId == Misc.bannerAdIdTwo) {
                    load(context, Misc.bannerAdIdThree)
                }
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {}
            override fun onAdClosed() {}
        }
    }

    fun loadCollapsibleBanner(remoteKey: String, collapsibleAdView: AdView, callBack: bannerAdsCallBack? = null) {
        if (remoteKey.contains("am")) {
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
                        "TAG",
                        "Banner  ${adError.code}: ${adError.message}"
                    )
                    isLoaded = false
                    callBack?.onFailed()
                }

                override fun onAdOpened() {}
                override fun onAdClicked() {}
                override fun onAdClosed() {}
            }
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

    fun show(/*remoteKey: String,*/ view: FrameLayout) {
        if (adView != null && isLoaded/* && remoteKey.contains("am")*/) {
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

}