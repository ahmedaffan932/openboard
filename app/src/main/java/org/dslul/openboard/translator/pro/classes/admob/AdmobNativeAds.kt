package org.dslul.openboard.translator.pro.classes.admob

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.imageview.ShapeableImageView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc

object AdmobNativeAds {
    var amInner: NativeAd? = null
    var isNativeAdLoading = false

    @SuppressLint("MissingPermission")
    fun loadAdmobNative(
        context: Context,
        adId: String = AdIds.nativeAdIdAdMobOne,
        callBack: LoadAdCallBack? = null
    ) {
        if (adId == "") {
            return
        }

        if (Misc.getPurchasedStatus(context)) {
            return
        }

        isNativeAdLoading = true
        val adLoader = AdLoader.Builder(
            context,
            adId
        ).forNativeAd { ad: NativeAd ->
            amInner = ad
        }.withAdListener(object : AdListener() {

            override fun onAdFailedToLoad(adError: LoadAdError) {
                isNativeAdLoading = false

                amInner = null
                Log.e("logKey", "Native Ad Failed: " + adError.code + " | " + adError.message)

                callBack?.onFailed()
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                isNativeAdLoading = false
                Log.e("logKey", "Native Ad Loaded")
                callBack?.onLoaded()
            }

            override fun onAdImpression() {
                super.onAdImpression()
                amInner = null
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun showNativeAd(
        context: Context,
        remoteKey: String,
        amLayout: FrameLayout
    ) {
        Log.e(Misc.logKey, "Looking for Ad")
        if (amInner != null) {
            Log.d(Misc.logKey, "Have Ad")
            val nativeAdToShow = amInner
            if (remoteKey.contains("am")) {
                amLayout.visibility = View.VISIBLE

                val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                        as LayoutInflater

                val adView = if (remoteKey.contains("dashboard")) {
                    inflater.inflate(R.layout.admob_native_dashboard, null) as NativeAdView
                } else if (remoteKey.contains("large")) {
                    if (remoteKey.contains("lctr")) {
                        inflater.inflate(R.layout.admob_native_layout_lctr, null) as NativeAdView
                    } else {
                        inflater.inflate(R.layout.admob_native_hctr, null) as NativeAdView
                    }
                } else {
                    if (remoteKey.contains("lctr")) {
                        inflater.inflate(
                            R.layout.admob_small_native_ad_lctr,
                            null
                        ) as NativeAdView
                    } else {
                        inflater.inflate(
                            R.layout.admob_small_native_ad_hctr,
                            null
                        ) as NativeAdView
                    }
                }

                amLayout.removeAllViews()
                amLayout.addView(adView)

                adView.mediaView = adView.findViewById(R.id.ad_media)

                adView.headlineView = adView.findViewById(R.id.ad_headline)
                adView.bodyView = adView.findViewById(R.id.ad_body)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                adView.iconView = adView.findViewById(R.id.ad_app_icon)

                (adView.headlineView as TextView).text = nativeAdToShow?.headline
                adView.mediaView?.mediaContent = nativeAdToShow?.mediaContent

                if (nativeAdToShow?.body == null) {
                    adView.bodyView?.visibility = View.INVISIBLE
                } else {
                    adView.bodyView?.visibility = View.VISIBLE
                    (adView.bodyView as TextView).text = nativeAdToShow?.body
                }


                if (nativeAdToShow?.callToAction == null) {
                    adView.callToActionView?.visibility = View.INVISIBLE
                } else {
                    adView.callToActionView?.visibility = View.VISIBLE
                    (adView.callToActionView as Button).text = nativeAdToShow?.callToAction
                }

                if (nativeAdToShow?.icon == null) {
                    adView.iconView?.visibility = View.GONE
                } else {
                    (adView.iconView as ShapeableImageView).setImageDrawable(
                        nativeAdToShow.icon!!.drawable
                    )
                }

                adView.setNativeAd(nativeAdToShow!!)

                loadAdmobNative(context)
            }
        } else {
            if (isNativeAdLoading)
                loadAdmobNative(context)
            amLayout.visibility = View.GONE
        }
    }
}