package org.dslul.openboard.translator.pro.classes.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import org.dslul.openboard.translator.pro.interfaces.LoadInterstitialCallBack
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc

object NativeAds {
    var mNativeAd: NativeAd? = null


    fun loadNativeAd(
        activity: Activity,
        callBack: LoadInterstitialCallBack? = null
    ) {
        if (!Misc.getPurchasedStatus(activity)) {
            mNativeAd = null
            if (Misc.getPurchasedStatus(activity)) return

            val adLoader: AdLoader =
                AdLoader.Builder(activity, Misc.nativeAdIdAdMobOne).forNativeAd { nativeAd ->
                    Log.d(Misc.logKey, "Native Ad Loaded")
                    mNativeAd = nativeAd
                    callBack?.onLoaded()

                }.withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        callBack?.onFailed()
                        mNativeAd = null
                        Log.e(Misc.logKey, adError.message)
                    }
                }).build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    private fun showNativeAd(
        activity: Activity,
        amLayout: FrameLayout,
        call: ((Boolean) -> Unit)? = null
    ) {
        if (!Misc.getPurchasedStatus(activity)) {

            val nativeAdToShow = mNativeAd

            call?.invoke(true)
            val inflater =
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            val adView = inflater.inflate(R.layout.admob_native_layout, null) as NativeAdView

            amLayout.removeAllViews()
            amLayout.addView(adView)
            amLayout.visibility = View.VISIBLE

            adView.mediaView = adView.findViewById(R.id.ad_media)
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            adView.iconView = adView.findViewById(R.id.ad_app_icon)

            (adView.headlineView as TextView).text = nativeAdToShow?.headline
            nativeAdToShow?.mediaContent?.let { adView.mediaView?.setMediaContent(it) }

            if (nativeAdToShow?.body == null) {
                adView.bodyView?.visibility = View.INVISIBLE
            } else {
                adView.bodyView?.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAdToShow.body
            }

            if (nativeAdToShow?.callToAction == null) {
                adView.callToActionView?.visibility = View.INVISIBLE
            } else {
                adView.callToActionView?.visibility = View.VISIBLE
                (adView.callToActionView as Button).text = nativeAdToShow.callToAction
            }

            if (nativeAdToShow?.icon == null) {
                adView.iconView?.visibility = View.GONE
            } else {
                (adView.iconView as ImageView).setImageDrawable(
                    nativeAdToShow.icon!!.drawable
                )
                adView.iconView?.visibility = View.VISIBLE
            }

            adView.setNativeAd(nativeAdToShow ?: return)
            Misc.isNativeAdClicked = false

            loadNativeAd(activity)
        } else {
            call?.invoke(false)
            amLayout.visibility = View.GONE
            loadNativeAd(activity)
        }

    }

    fun manageShowNativeAd(
        activity: Activity,
        remoteKey: String,
        amLayout: FrameLayout,
        call: ((Boolean) -> Unit)? = null
    ) {
        if (remoteKey.contains("no_media")) {
            manageShowNativeAd(activity, remoteKey, amLayout, call)
        } else {
            showNativeAd(activity, amLayout, call)
        }
    }

}