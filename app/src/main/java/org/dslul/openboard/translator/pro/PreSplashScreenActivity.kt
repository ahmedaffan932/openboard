package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPreSplashScreenBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobInterstitialAd
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
    private var isAppOpenLoaded = false

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

                        AppOpenAdManager.loadAd(
                            this@PreSplashScreenActivity,
                            AdIds.appOpenAdIdSplash,
                            object : LoadAdCallBack {
                                override fun onLoaded() {
                                    isAppOpenLoaded = true
                                }

                                override fun onFailed() {
                                }
                            }
                        )

                        Thread { // Initialize the Google Mobile Ads SDK on a background thread.
                            MobileAds.initialize(
                                this
                            ) { initializationStatus: InitializationStatus ->
                                val statusMap =
                                    initializationStatus.adapterStatusMap
                                for (adapterClass in statusMap.keys) {
                                    val status =
                                        statusMap[adapterClass]
                                    Log.d(
                                        "MyApp",
                                        String.format(
                                            "Adapter name: %s, Description: %s, Latency: %d",
                                            adapterClass,
                                            status!!.description,
                                            status.latency
                                        )
                                    )
                                }
                            }
                        }.start()

                        Log.d(Misc.logKey, "Initialized")


                        object : CountDownTimer(8000, 50) {
                            override fun onTick(millisUntilFinished: Long) {
                                if (isRemoteConfigFetched) {
                                    if (!isAdRequestSent) {
                                        isAdRequestSent = true
                                        if (Ads.isIntPreLoad) {
                                            AdmobInterstitialAd.loadInterAdmob(
                                                this@PreSplashScreenActivity,
                                                AdIds.interstitialAdIdAdMobSplash
                                            )
                                        }

                                        if (Ads.isNativeAdPreload) {
                                            AdmobNativeAds.loadAdmobNative(
                                                context = this@PreSplashScreenActivity,
                                                adIds = AdIds.nativeAdIdAdMobSplash,
                                                remoteKey = Ads.splashNative,
                                                frameLayout = binding.bannerFrameLayout,
                                                callBack = object : LoadAdCallBack {
                                                    override fun onLoaded() {
                                                        AdmobNativeAds.showNativeAd(
                                                            context = this@PreSplashScreenActivity,
                                                            remoteKey = Ads.splashNative,
                                                            amLayout = binding.bannerFrameLayout,
                                                            adIds = AdIds.nativeAdIdAdMobSplash,
                                                            nextPreloadAdIds = AdIds.nativeAdIdLanguages
                                                        )
                                                    }

                                                    override fun onFailed() {
                                                        binding.bannerFrameLayout.removeAllViews()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            override fun onFinish() {
                                Log.e(Misc.logKey, "finished")
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
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()

        mFRC.setConfigSettingsAsync(configSettings)
        mFRC.setDefaultsAsync(R.xml.remote_config_defaults)
        mFRC.ensureInitialized()
        mFRC.fetchAndActivate().addOnCompleteListener { task ->
            Ads.applyRemoteConfig(mFRC)
            AdIds.applyRemoteConfig(mFRC)

            isRemoteConfigFetched = true
            logRemoteConfigEvent(task.isSuccessful)
        }
    }

    private fun logRemoteConfigEvent(isSuccessful: Boolean) {
        val params = Bundle().apply {
            putString("status", if (isSuccessful) "success" else "failed")
        }
        Firebase.analytics.logEvent("remote_config_fetch", params)
    }

    fun startNextActivity() {
        object : CountDownTimer(3000, 100) {
            override fun onTick(p0: Long) {
                if (isAppOpenLoaded) {
                    if (!isShowingAppOpen) {
                        AppOpenAdManager.showIfAvailable(
                            this@PreSplashScreenActivity,
                            Ads.isSplashAppOpenAdEnabled,
                            AdIds.appOpenAdIdSplash,
                            AdIds.appOpenAdIdResume,
                            object : InterstitialCallBack {
                                override fun onAdDisplayed() {
                                    isShowingAppOpen = true
                                }

                                override fun onDismiss() {
                                    openNextScreen()
                                }
                            }
                        )
                    }
                }
            }

            override fun onFinish() {
                if (!isShowingAppOpen)
                    AppOpenAdManager.showIfAvailable(
                        this@PreSplashScreenActivity,
                        Ads.isSplashAppOpenAdEnabled,
                        AdIds.appOpenAdIdSplash,
                        AdIds.appOpenAdIdResume,
                        object : InterstitialCallBack {
                            override fun onDismiss() {
                                openNextScreen()
                            }

                            override fun onAdDisplayed() {
                                isShowingAppOpen = true
                            }
                        }
                    )
            }
        }.start()
    }

    private fun openNextScreen() {
        if (isNextActivityStarted) return

        isNextActivityStarted = true
        val nextIntent = if (Misc.isProScreenEnabled) {
            Intent(this, PremiumScreenActivity::class.java)
                .putExtra(Misc.premiumShownBeforeLanguage, true)
        } else {
            Intent(this, AppLanguageSelectorActivity::class.java)
        }
        startActivity(nextIntent)
        AppOpenAdManager.loadAd(this, AdIds.appOpenAdIdResume)
        finish()
    }
}
