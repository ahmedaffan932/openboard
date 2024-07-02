package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.postDelayed
import androidx.core.util.Pair
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.android.synthetic.main.activity_pre_splash_screen.*
import kotlinx.android.synthetic.main.fragment_home.mrecFrameLayout
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPreSplashScreenBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isSplashScreen
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobInterstitialAd
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobMRECAds
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobNativeAds
import org.dslul.openboard.translator.pro.classes.ads.admob.AppOpenAdManager
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

@SuppressLint("CustomSplashScreen")
class PreSplashScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPreSplashScreenBinding
    private lateinit var consentInformation: ConsentInformation
    private var isShowingAppOpen = false
    private var isRemoteConfigFetched = false
    private var isNextActivityStarted = false
    private var isAdRequestSent = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityPreSplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        Handler(Looper.getMainLooper()).postDelayed({
            binding.animBackground.playAnimation()
        }, 1000)
        getRemoteConfigValues()

        val debugSettings = ConsentDebugSettings.Builder(this)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("324810A5D07FF47ED2E42E54FD1A1556")
            .build()

        val params = ConsentRequestParameters
            .Builder()
            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this
                ) { loadAndShowError ->

                    if (loadAndShowError != null) {
                        Log.d(Misc.logKey, loadAndShowError.message)
                        startNextActivity()
                    }

                    if (consentInformation.canRequestAds()) {
                        MobileAds.initialize(this) {}
                        Log.d(Misc.logKey, "Initialized")
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (Ads.isIntPreLoad) {
                                AdmobInterstitialAd.loadInterAdmob(
                                    this,
                                    AdIds.interstitialAdIdAdMobSplash
                                )
                            }

                            if (Ads.isNativeAdPreload) {
                                AdmobNativeAds.loadAdmobNative(
                                    this,
                                    AdIds.nativeAdIdAdMobSplash
                                )
                            }
                        }, 500)

                        AppOpenAdManager.loadAd(
                            this@PreSplashScreenActivity,
                            AdIds.appOpenAdIdSplash,
                            object : LoadAdCallBack {
                                override fun onLoaded() {
                                    if (!isNextActivityStarted)
                                        AppOpenAdManager.showIfAvailable(
                                            this@PreSplashScreenActivity,
                                            Ads.isSplashAppOpenAdEnabled,
                                            object : InterstitialCallBack {
                                                override fun onDismiss() {
                                                    startNextActivity()
                                                }

                                                override fun onAdDisplayed() {
                                                    isShowingAppOpen = true
                                                }
                                            }
                                        )
                                }

                                override fun onFailed() {
                                    startNextActivity()
                                }
                            }
                        )


                        object : CountDownTimer(7000, 50) {
                            override fun onTick(millisUntilFinished: Long) {
                                if (isRemoteConfigFetched) {
                                    if (!isAdRequestSent) {
                                        isAdRequestSent = true
                                        AdmobMRECAds.loadMREC(
                                            this@PreSplashScreenActivity,
                                            AdIds.mrecAdIdAd
                                        )
                                    }
                                }
                            }

                            override fun onFinish() {
                                Log.e(Misc.logKey, "finished")
                                if (!isShowingAppOpen)
                                    startNextActivity()
                            }
                        }.start()


                    }
                }
            },
            { requestConsentError ->
                Log.d(
                    Misc.logKey, "${requestConsentError.errorCode} ${requestConsentError.message}"
                )

                startNextActivity()
            }
        )


    }

    private fun getRemoteConfigValues() {
        val mFRC = FirebaseRemoteConfig.getInstance()
        mFRC.ensureInitialized()
        mFRC.fetchAndActivate().addOnCompleteListener { p0 ->
            if (p0.isSuccessful) {
                if (!BuildConfig.DEBUG) {
                    Ads.exitInt = mFRC.getString("exitInt")
                    Ads.phraseInt = mFRC.getString("phraseInt")
                    Ads.splashInt = mFRC.getString("splashInt")
                    Ads.exitNative = mFRC.getString("exitNative")
                    Ads.chatBanner = mFRC.getString("chatBanner")
                    Ads.dashboardInt = mFRC.getString("dashboardInt")
                    Ads.splashNative = mFRC.getString("splashNative")
                    Ads.translateInt = mFRC.getString("translateInt")
                    Ads.translateNative = mFRC.getString("translateNative")
                    Ads.dashboardNative = mFRC.getString("dashboardNative")
                    Ads.onBoardingNative = mFRC.getString("onBoardingNative")
                    Ads.cameraTranslationInt = mFRC.getString("cameraTranslationInt")
                    Ads.languageSelectorBanner = mFRC.getString("languageSelectorBanner")

                    Ads.isIntPreLoad = mFRC.getBoolean("isIntPreLoad")
                    Ads.isNativeAdPreload = mFRC.getBoolean("isNativeAdPreload")
                    Ads.isSplashAppOpenAdEnabled = mFRC.getBoolean("isSplashAppOpenAdEnabled")

                    AdIds.mrecAdIdAd = mFRC.getString("mrecAdIdAd")
                    AdIds.appOpenAdIdSplash = mFRC.getString("appOpenAdIdSplash")
                    AdIds.nativeAdIdAdMobExit = mFRC.getString("nativeAdIdAdMobExit")
                    AdIds.nativeAdIdAdMobTranslate = mFRC.getString("nativeAdIdAdMobTranslate")
                    AdIds.nativeAdIdAdMobSplash = mFRC.getString("nativeAdIdAdMobSplash")

                    AdIds.interstitialAdIdAdMobSplash =
                        mFRC.getString("interstitialAdIdAdMobSplash")
                    AdIds.interstitialAdIdAdMobPhrases =
                        mFRC.getString("interstitialAdIdAdMobPhrases")
                    AdIds.interstitialAdIdAdMobExit = mFRC.getString("interstitialAdIdAdMobExit")
                    AdIds.interstitialAdIdAdMobTranslate =
                        mFRC.getString("interstitialAdIdAdMobTranslate")
                    AdIds.interstitialAdIdAdMobCameraTranslate =
                        mFRC.getString("interstitialAdIdAdMobCameraTranslate")
                    AdIds.collapsibleBannerAdIdAdChat =
                        mFRC.getString("collapsibleBannerAdIdAdChat")
                    AdIds.collapsibleBannerAdIdAdLanguages =
                        mFRC.getString("collapsibleBannerAdIdAdLanguages")
                    AdIds.collapsibleBannerAdIdAdOnboarding =
                        mFRC.getString("collapsibleBannerAdIdAdOnboarding")


                    Misc.showNextButtonOnLanguageScreen =
                        mFRC.getBoolean("showNextButtonOnLanguageScreen")
                }

                isRemoteConfigFetched = true
                mFRC.reset()
            }
        }
    }

    fun startNextActivity() {
        if (!isNextActivityStarted) {
//            if (Ads.isSplashAppOpenAdEnabled) {
            if (Misc.isFirstTime(this)) {
                startActivity(Intent(this, AppLanguageSelectorActivity::class.java))
            } else {
                startActivity(Intent(this, FragmentsDashboardActivity::class.java))
            }
//            } else {
//                val intent = Intent(
//                    this,
//                    SplashScreenActivity::class.java
//                )
//                val pairs = arrayOf<Pair<View, String>>(
//                    Pair(binding.logo, "logo_splash"),
//                    Pair(binding.spline, "anim_splash")
//                )
//                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *pairs)
//                startActivity(intent, options.toBundle())
//            }
            isNextActivityStarted = true
        }
    }
}
