package org.dslul.openboard.translator.pro.classes.admob

import android.app.Activity
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdRequest.Builder
import org.dslul.openboard.translator.pro.classes.Misc

object BannerAds {
    private var adView: AdView? = null
    private var isLoaded = false


    fun load(context: Activity) {

        if (Misc.banner_ads == "am" && !Misc.getPurchasedStatus(context)) {
            adView = AdView(context)
            adView?.adUnitId = Misc.banner_id
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
                        "TAG",
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

}