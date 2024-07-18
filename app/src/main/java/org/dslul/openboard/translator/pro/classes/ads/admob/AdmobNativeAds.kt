package org.dslul.openboard.translator.pro.classes.ads.admob

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads

object AdmobNativeAds {
    var amNative: NativeAd? = null

    @SuppressLint("MissingPermission")
    fun loadAdmobNative(
        context: Context,
        adId: String = AdIds.nativeAdIdAdMobSplash,
        remoteKey: String? = null,
        frameLayout: FrameLayout? = null,
        callBack: LoadAdCallBack? = null,
    ) {
        if (adId == "") {
            callBack?.onFailed()
            return
        }

        if (Misc.getPurchasedStatus(context)) {
            return
        }

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        if (remoteKey != null) {
            frameLayout?.removeAllViews()
            val shimmerView =
                if (remoteKey.contains("small"))
                    inflater.inflate(R.layout.small_native_shimmer, null)
                else
                    inflater.inflate(R.layout.large_native_shimmer, null)

            if (remoteKey.contains("large")) {
                if (frameLayout != null) {
                    changeViewHeight(frameLayout, dpToPx(context))
                }
            }
            frameLayout?.addView(shimmerView)
            frameLayout?.visibility = View.VISIBLE
        }

        if (amNative != null) {
            callBack?.onLoaded()
        } else {
            val adLoader = AdLoader.Builder(
                context, adId
            ).forNativeAd { ad: NativeAd ->
                amNative = ad
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    amNative = null
                    Log.e("logKey", "Native Ad Failed: " + adError.code + " | " + adError.message)
                    frameLayout?.removeAllViews()
                    callBack?.onFailed()

                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    callBack?.onLoaded()
                    Log.e("logKey", "Native Ad Loaded")
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    amNative = null
                }
            }).withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
                    .build()
            ).build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    fun showNativeAd(
        context: Context,
        remoteKey: String,
        amLayout: FrameLayout
    ) {
        if (amNative != null) {
            val nativeAdToShow = amNative
            if (remoteKey.contains("am")) {
                amLayout.visibility = View.VISIBLE


                val adView = chooseLayout(context, remoteKey)

                if (remoteKey.contains("large")) {
                    changeViewHeight(amLayout, dpToPx(context))
                }

                amLayout.removeAllViews()
                amLayout.addView(adView)

                if (remoteKey.contains("splash") || remoteKey.contains("small_hctr")) {
                    adView.mediaView = null
                } else {
                    adView.mediaView = adView.findViewById(R.id.ad_media)
                }
                adView.headlineView = adView.findViewById(R.id.ad_headline)
                adView.bodyView = adView.findViewById(R.id.ad_body)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                adView.iconView = adView.findViewById(R.id.ad_app_icon)

                (adView.headlineView as TextView).text = nativeAdToShow?.headline
                if (adView.mediaView != null) adView.mediaView?.mediaContent =
                    nativeAdToShow?.mediaContent

                if (nativeAdToShow?.body == null) {
                    adView.bodyView?.visibility = View.INVISIBLE
                } else {
                    (adView.bodyView as TextView).text = nativeAdToShow.body
                }

                if (nativeAdToShow?.callToAction == null) {
                    adView.callToActionView?.visibility = View.INVISIBLE
                } else {
                    (adView.callToActionView as Button).text = nativeAdToShow.callToAction
                }

                if (nativeAdToShow?.icon == null) {
                    adView.iconView?.visibility = View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(
                        nativeAdToShow.icon!!.drawable
                    )
                }

                adView.setNativeAd(nativeAdToShow!!)
                amNative = null

                if (Ads.isNativeAdPreload) loadAdmobNative(context, AdIds.nativeAdIdAdMobSplash)
            }
        } else {
            if (Ads.isNativeAdPreload) loadAdmobNative(context, AdIds.nativeAdIdAdMobSplash)

            amLayout.visibility = View.GONE
        }
    }

    private fun chooseLayout(context: Context, remoteKey: String): NativeAdView {
        val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return if (remoteKey.contains("small"))
            if (remoteKey.contains("lctr"))
                inflater.inflate(
                    R.layout.admob_small_native_ad_lctr,
                    null
                ) as NativeAdView
            else
                inflater.inflate(
                    R.layout.admob_small_native_ad_hctr,
                    null
                ) as NativeAdView
        else if (remoteKey.contains("lctr"))
            inflater.inflate(R.layout.admob_native_layout_lctr, null) as NativeAdView
        else
            inflater.inflate(R.layout.admob_native_hctr, null) as NativeAdView
    }

    private fun changeViewHeight(view: View, newHeight: Int) {
        val layoutParams = view.layoutParams
        layoutParams.height = newHeight
        view.layoutParams = layoutParams
    }


    private fun dpToPx(context: Context, dp: Float = 300F): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}