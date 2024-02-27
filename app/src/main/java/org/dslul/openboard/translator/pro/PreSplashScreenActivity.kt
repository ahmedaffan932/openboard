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
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import kotlinx.android.synthetic.main.activity_pre_splash_screen.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.admob.AdIds
import org.dslul.openboard.translator.pro.classes.admob.AdmobBannerAds
import org.dslul.openboard.translator.pro.classes.admob.AdmobInterstitialAd
import org.dslul.openboard.translator.pro.classes.admob.AdmobNativeAds
import org.dslul.openboard.translator.pro.classes.admob.Ads
import org.dslul.openboard.translator.pro.classes.admob.LoadAdCallBack
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

@SuppressLint("CustomSplashScreen")
class PreSplashScreenActivity : AppCompatActivity() {
    private lateinit var consentInformation: ConsentInformation
    private var isIntAdLoaded = false
    private var isNativeAdLoaded = false
    private var isStartButtonVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_splash_screen)

        FirebaseApp.initializeApp(this)

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
                        // Consent gathering failed.
                        Log.d(Misc.logKey, String.format("%s: %s", loadAndShowError.message))
                    }

                    if (consentInformation.canRequestAds()) {
                        MobileAds.initialize(this) {}

                        loadAds()
                    }
                    object : CountDownTimer(12000, 3000) {
                        @SuppressLint("SetTextI18n")
                        override fun onTick(p0: Long) {
                            if (isIntAdLoaded && isNativeAdLoaded) {
                                showStartButton()
                            }
                        }

                        override fun onFinish() {
                            showStartButton()
                        }
                    }.start()

                }
            },
            { requestConsentError ->
                Log.w(
                    Misc.logKey, String.format(
                        "%s: %s",
                        requestConsentError.errorCode,
                        requestConsentError.message
                    )
                )
                object : CountDownTimer(12000, 3000) {
                    @SuppressLint("SetTextI18n")
                    override fun onTick(p0: Long) {
                        if (isIntAdLoaded && isNativeAdLoaded) {
                            showStartButton()
                        }
                    }

                    override fun onFinish() {
                        showStartButton()
                    }
                }.start()

            }
        )

        btnStart.setOnClickListener {
            startSplashActivity()
        }

    }

    private fun startSplashActivity() {
        if (Misc.isFirstTime(this)) {
            Ads.showInterstitial(this, Ads.splashInt, object : InterstitialCallBack{
                override fun onDismiss() {
                    startActivity(Intent(this@PreSplashScreenActivity, SplashScreenActivity::class.java))
                }
            })
        } else {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        finish()
        return
    }

    private fun showStartButton() {
        if (!isStartButtonVisible) {
            Handler(Looper.getMainLooper()).postDelayed({
                Misc.zoomInView(btnStart, this, 250)
            }, 250)

            spline.visibility = View.INVISIBLE

            val a: Animation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_logo)
            a.duration = 500

            a.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {

                }

                override fun onAnimationEnd(p0: Animation?) {
                }

                override fun onAnimationRepeat(p0: Animation?) {

                }
            })
        }
        isStartButtonVisible = true
    }


    private fun loadAds(){
        AdmobNativeAds.loadAdmobNative(this, AdIds.nativeAdIdAdMobSplash, object : LoadAdCallBack {
            override fun onLoaded() {
                isNativeAdLoaded = true
                AdmobNativeAds.showNativeAd(
                    this@PreSplashScreenActivity,
                    Ads.splashNative,
                    nativeAdFrameLayout
                )
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            AdmobBannerAds.load(this)
        }, 2000)

        Handler(Looper.getMainLooper()).postDelayed({
            AdmobInterstitialAd.loadInterAdmob(
                this,
                AdIds.interstitialAdIdAdMobSplash,
                object : LoadAdCallBack {
                    override fun onLoaded() {
                        isIntAdLoaded = true
                    }
                })
        }, 1000)

    }

}