package org.dslul.openboard.translator.pro.classes.ads.admob

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds

object AdmobMRECAds {
    private var mrecAdView: AdView? = null
    private var isMRECLoaded = false

    fun loadMREC(
        context: Activity,
        adId: String = AdIds.mrecAdIdAd
    ) {
        isMRECLoaded = false
        mrecAdView = AdView(context)
        mrecAdView?.adUnitId = adId
        mrecAdView?.setAdSize(AdSize.MEDIUM_RECTANGLE)
        val adRequest = AdRequest.Builder().build()
        mrecAdView?.loadAd(adRequest)

        mrecAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(Misc.logKey, "MREC loaded.")
                isMRECLoaded = true
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(
                    Misc.logKey,
                    "onAdFailedToLoad:Adaptive Banner  ${adError.code}: ${adError.message}"
                )
                isMRECLoaded = false
            }

            override fun onAdOpened() {}
            override fun onAdClosed() {}
            override fun onAdClicked() {}
        }
    }

    fun showMREC(context: Activity, frameLayout: FrameLayout, remoteKey: String) {
        if(!Misc.checkInternetConnection(context)){
            frameLayout.removeAllViews()
            return
        }
        if (remoteKey.contains("am")) {
            if (mrecAdView != null && isMRECLoaded) {
                frameLayout.visibility = View.VISIBLE
                frameLayout.removeAllViews()
                val adToShow = mrecAdView
                frameLayout.addView(adToShow)
            }
            mrecAdView = null
            loadMREC(context)
        } else {
            frameLayout.visibility = View.GONE
        }
    }


}