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
    private var isLoadingNativeAd = false

    @SuppressLint("MissingPermission")
    fun loadAdmobNative(
        context: Context,
        adIds: Array<String> = AdIds.nativeAdIdAdMobSplash,
        remoteKey: String? = null,
        frameLayout: FrameLayout? = null,
        callBack: LoadAdCallBack? = null,
    ) {
        if (isLoadingNativeAd) {
            Log.d(Misc.logKey, "Native Ad load skipped: already loading.")
            return
        }

        if (Misc.getPurchasedStatus(context)) {
            Log.d(Misc.logKey, "Native Ad load skipped: user purchased.")
            callBack?.onFailed()
            return
        }

        if (adIds.isEmpty()) {
            Log.d(Misc.logKey, "Native Ad load failed: adIds array is empty.")
            callBack?.onFailed()
            return
        }

        if (amNative != null) {
            Log.d(Misc.logKey, "Native Ad already available.")
            callBack?.onLoaded()
            return
        }

        showNativeShimmer(
            context = context,
            remoteKey = remoteKey,
            frameLayout = frameLayout
        )

        isLoadingNativeAd = true

        loadNativeByIndex(
            context = context.applicationContext,
            adIds = adIds,
            index = 0,
            callBack = callBack,
            frameLayout = frameLayout
        )
    }

    private fun loadNativeByIndex(
        context: Context,
        adIds: Array<String>,
        index: Int,
        callBack: LoadAdCallBack?,
        frameLayout: FrameLayout?
    ) {
        if (index >= adIds.size) {
            isLoadingNativeAd = false
            amNative = null

            Log.d(Misc.logKey, "Native Ad all ad ids failed.")

            frameLayout?.removeAllViews()
            callBack?.onFailed()
            return
        }

        val currentAdId = adIds[index]

        if (currentAdId.isBlank()) {
            Log.d(Misc.logKey, "Native Ad skipped blank ad id at index: $index")

            loadNativeByIndex(
                context = context,
                adIds = adIds,
                index = index + 1,
                callBack = callBack,
                frameLayout = frameLayout
            )
            return
        }

        Log.d(Misc.logKey, "Native Ad trying index $index: $currentAdId")

        val adLoader = AdLoader.Builder(context, currentAdId)
            .forNativeAd { ad: NativeAd ->
                amNative = ad
            }
            .withAdListener(object : AdListener() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    amNative = null

                    Log.e(
                        Misc.logKey,
                        "Native Ad failed at index $index: ${adError.code} | ${adError.message}"
                    )

                    loadNativeByIndex(
                        context = context,
                        adIds = adIds,
                        index = index + 1,
                        callBack = callBack,
                        frameLayout = frameLayout
                    )
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()

                    isLoadingNativeAd = false

                    Log.d(Misc.logKey, "Native Ad loaded successfully with index: $index")

                    callBack?.onLoaded()
                }

                override fun onAdImpression() {
                    super.onAdImpression()

                    Log.d(Misc.logKey, "Native Ad impression recorded.")

                    amNative = null
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun showNativeShimmer(
        context: Context,
        remoteKey: String?,
        frameLayout: FrameLayout?
    ) {
        if (remoteKey == null || frameLayout == null) return

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        frameLayout.removeAllViews()

        val shimmerView = if (remoteKey.contains("small")) {
            inflater.inflate(R.layout.small_native_shimmer, null)
        } else {
            inflater.inflate(R.layout.large_native_shimmer, null)
        }

        if (remoteKey.contains("large")) {
            changeViewHeight(frameLayout, dpToPx(context))
        }

        frameLayout.addView(shimmerView)
        frameLayout.visibility = View.VISIBLE
    }

    fun showNativeAd(
        context: Context,
        remoteKey: String,
        amLayout: FrameLayout
    ) {
        val nativeAdToShow = amNative

        if (nativeAdToShow == null) {
            if (Ads.isNativeAdPreload) {
                loadAdmobNative(context, AdIds.nativeAdIdAdMobSplash)
            }

            amLayout.visibility = View.GONE
            return
        }

        if (!remoteKey.contains("am")) {
            amLayout.visibility = View.GONE
            return
        }

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

        (adView.headlineView as TextView).text = nativeAdToShow.headline

        adView.mediaView?.mediaContent = nativeAdToShow.mediaContent

        if (nativeAdToShow.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAdToShow.body
        }

        if (nativeAdToShow.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAdToShow.callToAction
        }

        if (nativeAdToShow.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            adView.iconView?.visibility = View.VISIBLE
            (adView.iconView as ImageView).setImageDrawable(nativeAdToShow.icon?.drawable)
        }

        adView.setNativeAd(nativeAdToShow)

        amNative = null

        if (Ads.isNativeAdPreload) {
            loadAdmobNative(context, AdIds.nativeAdIdAdMobSplash)
        }
    }

    private fun chooseLayout(context: Context, remoteKey: String): NativeAdView {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        return if (remoteKey.contains("small")) {
            if (remoteKey.contains("lctr")) {
                inflater.inflate(
                    R.layout.admob_small_native_ad_lctr,
                    null
                ) as NativeAdView
            } else if (remoteKey.contains("btn_up")) {
                inflater.inflate(
                    R.layout.admob_small_native_ad_hctr,
                    null
                ) as NativeAdView
            } else {
                inflater.inflate(
                    R.layout.admob_small_native_ad_hctr_btn_bottom,
                    null
                ) as NativeAdView
            }
        } else if (remoteKey.contains("lctr")) {
            inflater.inflate(R.layout.admob_native_layout_lctr, null) as NativeAdView
        } else {
            inflater.inflate(R.layout.admob_native_hctr, null) as NativeAdView
        }
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

    fun clearNativeAd() {
        amNative?.destroy()
        amNative = null
        isLoadingNativeAd = false
    }
}