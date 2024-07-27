package org.dslul.openboard.translator.pro.classes.ads.admob

import android.util.Log
import android.view.View
import android.app.Activity
import android.view.Display
import android.view.ViewGroup
import android.widget.FrameLayout
import android.util.DisplayMetrics
import com.google.android.gms.ads.*
import android.annotation.SuppressLint
import com.google.android.gms.ads.AdRequest.Builder
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds


@SuppressLint("StaticFieldLeak")
object AdmobBannerAd {
    private var adView: AdView? = null
    private var isLoaded = false

    fun loadAdmobAdaptiveBanner(context: Activity,  layout: FrameLayout, remoteKey: String ) {
        if (Misc.adsCtrCount > 10) {
            return
        }

        if(Misc.getPurchasedStatus(context)){
            return
        }

        if (remoteKey.contains("am") ){
            adView = AdView(context)
            adView?.adUnitId = AdIds.bannerAdIdAdSplash
            layout.removeAllViews()
            layout.addView(adView)
            adView?.setAdSize(getAdSize(context, layout))
            val adRequest = Builder().build()
            adView?.loadAd(adRequest)

            adView!!.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    layout.visibility = View.VISIBLE
                    isLoaded = true
                    showAdaptiveBanner(layout)
                }

                override fun onAdFailedToLoad(adError : LoadAdError) {
                    Log.e(
                        "TAG",
                        "onAdFailedToLoad:Adaptive Banner  ${adError.code}: ${adError.message}"
                    )
                    layout.visibility = View.GONE
                    isLoaded = false
                }
                override fun onAdOpened() {}
                override fun onAdClicked() {
                    Misc.adsCtrCount++
                }
                override fun onAdClosed() {}
            }

        }else{
            layout.visibility = View.GONE
        }

    }

    private fun getAdSize(context: Activity, adContainer: FrameLayout): AdSize {
        val display: Display = context.windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        val density = displayMetrics.density
        var adwidthpixels: Float = adContainer.width.toFloat()
        if (adwidthpixels == 0f) {
            adwidthpixels = displayMetrics.widthPixels.toFloat()
        }
        val adWith = (adwidthpixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWith)
    }

    private fun showAdaptiveBanner(view: FrameLayout){
        if (adView != null && isLoaded){
            view.visibility = View.VISIBLE
            if (adView?.parent != null) {
                (adView?.parent as ViewGroup).removeView(adView) // <- fix
            }
            view.removeAllViews()
            view.addView(adView)
        }
        else{
            view.visibility = View.GONE
        }

    }

}