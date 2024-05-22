package org.dslul.openboard.translator.pro.classes.ads.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest.Builder
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds


object AdmobBannerAds {
    private var adView: AdView? = null
    var isLoaded = false

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
        adId: String = AdIds.collapsibleBannerAdIdAd,
        remoteKey: String,
        view: LinearLayout,
        callBack: LoadAdCallBack? = null
    ) {
        if (Misc.getPurchasedStatus(context)) {
            return
        }

        if (remoteKey.contains("am")) {

            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                    as LayoutInflater

            val shimmerView = inflater.inflate(R.layout.banner_shimmer, null)
            view.addView(shimmerView)

            val collapsibleAdView = AdView(context)
            collapsibleAdView.adUnitId = adId
            val extras = Bundle()
            extras.putString("collapsible", "bottom")

            collapsibleAdView.setAdSize(getAdSize(context))

            val adRequest = Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()

            collapsibleAdView.loadAd(adRequest)

            collapsibleAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isLoaded = true
                    view.removeAllViews()
                    view.addView(collapsibleAdView)
                    callBack?.onLoaded()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(
                        Misc.logKey,
                        "Banner  ${adError.code}: ${adError.message}"
                    )
                    isLoaded = false
                    view.removeAllViews()
                    callBack?.onFailed()
                }

                override fun onAdOpened() {}
                override fun onAdClicked() {}
                override fun onAdClosed() {}
            }
        }
    }

}