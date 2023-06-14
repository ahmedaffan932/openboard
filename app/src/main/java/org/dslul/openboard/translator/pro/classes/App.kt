package org.dslul.openboard.translator.pro.classes

import android.app.*
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import org.dslul.openboard.translator.pro.fcm.services.FcmFireBaseID
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.dslul.openboard.inputmethod.latin.BuildConfig

class App : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(this) {}

        FirebaseApp.initializeApp(this)
        FcmFireBaseID.subscribeToTopic()

        Misc.selectThemeMode(this)

        getRemoteConfigValues()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()

        if (!Misc.getPurchasedStatus(this)) {
            AppOpenAdManager().loadAd(this)
        }
    }

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    private fun getRemoteConfigValues(): Boolean {
        return try {
            val mFRC = FirebaseRemoteConfig.getInstance()
            mFRC.ensureInitialized()
            mFRC.fetchAndActivate().addOnCompleteListener { p0 ->
                if (p0.isSuccessful) {
                    if (!BuildConfig.DEBUG) {
                        if (BuildConfig.VERSION_NAME == mFRC.getString("versionNameToBlockAds")) {
                            Misc.interstitialAdIdAdMobOne = ""
                            Misc.interstitialAdIdAdMobTwo = ""
                            Misc.interstitialAdIdAdMobThree = ""

                            Misc.nativeAdIdAdMobOne = ""
                            Misc.nativeAdIdAdMobTwo = ""
                            Misc.nativeAdIdAdMobThree = ""

                            Misc.bannerAdIdOne = ""
                            Misc.bannerAdIdTwo = ""
                            Misc.bannerAdIdThree = ""

                            Misc.chatIntAm = "off"
                            Misc.gameIntAm = "off"
                            Misc.proScreen = "off"
                            Misc.quitNativeAm = "off"
                            Misc.gameNativeAm = "off"
                            Misc.chatNativeAm = "off"
                            Misc.historyIntAm = "off"
                            Misc.lifeTimePrice = "off"
                            Misc.settingsIntAm = "off"
                            Misc.dashboardIntAm = "off"
                            Misc.favoritesIntAm = "off"
                            Misc.splashNativeAm = "off"
                            Misc.proScreenIntAm = "off"
                            Misc.phrasebookIntAm = "off"
                            Misc.settingsNativeAm = "off"
                            Misc.dashboardNativeAm = "off"
                            Misc.translateNativeAm = "off"
                            Misc.dashboardNativeAm = "off"
                            Misc.enableKeyboardIntAm = "off"
                            Misc.splashContinueBtnText = "off"
                            Misc.multiTranslateNativeAm = "off"
                            Misc.languageSelectorNativeAm = "off"
                            Misc.dashboardCollapsingBannerAm = "off"
                            Misc.translateCollapsingBannerAm = "off"
                            Misc.enableKeyboardCollapsingBannerAm = "off"

                            Misc.isProScreenEnabled = mFRC.getBoolean("isProScreenEnabled")
                            Misc.isKeyboardSelectionInFlow = mFRC.getBoolean("isKeyboardSelectionInFlow")
                        } else {

                            Misc.chatIntAm = mFRC.getString("chatIntAm")
                            Misc.gameIntAm = mFRC.getString("gameIntAm")
                            Misc.proScreen = mFRC.getString("proScreen")
                            Misc.quitNativeAm = mFRC.getString("quitNativeAm")
                            Misc.gameNativeAm = mFRC.getString("gameNativeAm")
                            Misc.chatNativeAm = mFRC.getString("chatNativeAm")
                            Misc.historyIntAm = mFRC.getString("historyIntAm")
                            Misc.lifeTimePrice = mFRC.getString("lifeTimePrice")
                            Misc.settingsIntAm = mFRC.getString("settingsIntAm")
                            Misc.dashboardIntAm = mFRC.getString("dashboardIntAm")
                            Misc.favoritesIntAm = mFRC.getString("favoritesIntAm")
                            Misc.splashNativeAm = mFRC.getString("splashNativeAm")
                            Misc.proScreenIntAm = mFRC.getString("proScreenIntAm")
                            Misc.phrasebookIntAm = mFRC.getString("phrasebookIntAm")
                            Misc.settingsNativeAm = mFRC.getString("settingsNativeAm")
                            Misc.gameNextLimit = mFRC.getLong("gameNextLimit").toInt()
                            Misc.dashboardNativeAm = mFRC.getString("dashboardNativeAm")
                            Misc.translateNativeAm = mFRC.getString("translateNativeAm")
                            Misc.dashboardNativeAm = mFRC.getString("dashboardNativeAm")
                            Misc.setGoogleApi(mFRC.getString("googleApiKey"), this)
                            Misc.isProScreenEnabled = mFRC.getBoolean("isProScreenEnabled")
                            Misc.enableKeyboardIntAm = mFRC.getString("enableKeyboardIntAm")
                            Misc.splashContinueBtnText = mFRC.getString("splashContinueBtnText")
                            Misc.multiTranslateNativeAm = mFRC.getString("multiTranslateNativeAm")
                            Misc.languageSelectorNativeAm = mFRC.getString("languageSelectorNativeAm")
                            Misc.isKeyboardSelectionInFlow = mFRC.getBoolean("isKeyboardSelectionInFlow")
                            Misc.dashboardCollapsingBannerAm = mFRC.getString("dashboardCollapsingBannerAm")
                            Misc.translateCollapsingBannerAm = mFRC.getString("translateCollapsingBannerAm")
                            Misc.isCameraTranslatorAdEnabled = mFRC.getBoolean("isCameraTranslatorAdEnabled")
                            Misc.isChatInBetweenNativeEnabled = mFRC.getBoolean("isChatInBetweenNativeEnabled")
                            Misc.proScreenDismissBtnVisibleAfter = mFRC.getLong("proScreenDismissBtnVisibleAfter")
                            Misc.isMultiTranslatorPremiumModule = mFRC.getBoolean("isMultiTranslatorPremiumModule")
                            Misc.isDirectTranslateScreenEnabled = mFRC.getBoolean("isDirectTranslateScreenEnabled")
                            Misc.enableKeyboardCollapsingBannerAm = mFRC.getString("enableKeyboardCollapsingBannerAm")
                            Misc.splashScreenOnBackPressDoNothing = mFRC.getBoolean("splashScreenOnBackPressDoNothing")
                            Misc.isLanguageSelectorInBetweenNativeEnabled = mFRC.getBoolean("isLanguageSelectorInBetweenNativeEnabled")
                        }
                        try {
                            Misc.showInterstitialAfter =
                                mFRC.getString("showInterstitialAfter").toInt()
                        } catch (e: Exception) {
                            Misc.showInterstitialAfter = 1
                        }
                    }
                    mFRC.reset()
                    Misc.isRemoteConfigFetched.value = true

                    Log.d(Misc.logKey, p0.exception.toString())
                } else {
                    Misc.isRemoteConfigFetched.value = true
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Misc.isRemoteConfigFetched.value = true
            false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        currentActivity?.let {
            if (!Misc.isInterstitialDisplaying)
                appOpenAdManager.showAdIfAvailable(it, null)
        }
    }

    /** Show the ad if one isn't already showing. */

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        /** Request an ad. */
        fun loadAd(context: Context, adId: String = Misc.appOpenAdIdOne) {
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context, adId, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {

                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        Log.d(Misc.logKey, "Ad loaded.")

                        appOpenAd = ad
                        isLoadingAd = false
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                        Log.d(Misc.logKey, loadAdError.message)

                        isLoadingAd = false
                        if (adId == Misc.appOpenAdIdOne) {
                            loadAd(context, Misc.appOpenAdIdTwo)
                        } else if (adId == Misc.appOpenAdIdTwo) {
                            loadAd(context, Misc.appOpenAdIdThree)
                        }
                    }
                })
        }

        /** Check if ad exists and can be shown. */
        private fun isAdAvailable(): Boolean {
            return appOpenAd != null
        }

        /** Shows the ad if one isn't already showing. */
        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener?
        ) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.d(Misc.logKey, "The app open ad is already showing.")
                return
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                Log.d(Misc.logKey, "The app open ad is not ready yet.")
                onShowAdCompleteListener?.onShowAdComplete()
                loadAd(activity)
                return
            }

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    // Called when full screen content is dismissed.
                    // Set the reference to null so isAdAvailable() returns false.
                    Log.d(Misc.logKey, "Ad dismissed fullscreen content.")
                    appOpenAd = null
                    isShowingAd = false

                    onShowAdCompleteListener?.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when fullscreen content failed to show.
                    // Set the reference to null so isAdAvailable() returns false.
                    Log.d(Misc.logKey, adError.message)
                    appOpenAd = null
                    isShowingAd = false

                    onShowAdCompleteListener?.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    Log.d(Misc.logKey, "Ad showed fullscreen content.")
                }
            }
            isShowingAd = true
            if (!Misc.isDocumentTranslationActivity) {
                appOpenAd?.show(activity)
            }
        }
    }

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Updating the currentActivity only when an ad is not showing.
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
