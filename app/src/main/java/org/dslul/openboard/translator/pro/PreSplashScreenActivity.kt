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
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import kotlinx.android.synthetic.main.activity_pre_splash_screen.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPreSplashScreenBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isSplashScreen
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobInterstitialAd
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

@SuppressLint("CustomSplashScreen")
class PreSplashScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPreSplashScreenBinding
    private lateinit var consentInformation: ConsentInformation
    private var isIntAdLoaded = false
    private var isNativeAdLoaded = false
    private var isStartButtonVisible = false
    private lateinit var objDialog: Misc.LoadingAdDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreSplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                        Log.d(Misc.logKey, loadAndShowError.message)
                        showStartButton()
                    }

                    if (consentInformation.canRequestAds()) {
                        MobileAds.initialize(this) {}

                        loadAds()

                        object : CountDownTimer(8000, 3000) {
                            @SuppressLint("SetTextI18n")
                            override fun onTick(p0: Long) {
                            }

                            override fun onFinish() {
                                showStartButton()
                            }
                        }.start()

                    } else {
                        showStartButton()
                    }
                }
            },
            { requestConsentError ->
                Log.d(
                    Misc.logKey, "${requestConsentError.errorCode} ${requestConsentError.message}"
                )
                showStartButton()

            }
        )

        binding.btnStart.setOnClickListener {
            startNextActivity()
        }

//        binding.ratingBar.onRatingBarChangeListener =
//            RatingBar.OnRatingBarChangeListener { _, p1, _ ->
//                if (p1 > 3f) {
//                    rateUs()
//                } else {
//                    Toast.makeText(this, "Thanks for your review.", Toast.LENGTH_SHORT).show()
//                }
//            }


    }


    private fun startNextActivity() {
        Ads.showInterstitial(this, Ads.splashInt, object : InterstitialCallBack {
            override fun onDismiss() {
                if (Misc.isFirstTime(this@PreSplashScreenActivity)) {
                    startActivity(
                        Intent(
                            this@PreSplashScreenActivity,
                            SplashScreenActivity::class.java
                        )
                    )
                    finish()
                } else {
                    startActivity(Intent(this@PreSplashScreenActivity, NewDashboardActivity::class.java))
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        isSplashScreen = true
    }

    override fun onPause() {
        super.onPause()
        isSplashScreen = false
    }

    private fun showStartButton() {
        if (!isStartButtonVisible) {

            try {
                objDialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Handler(Looper.getMainLooper()).postDelayed({
                startNextActivity()
            }, 1000)
        }
        isStartButtonVisible = true
    }

    private fun loadAds() {
        AdmobInterstitialAd.loadInterAdmob(
            this,
            AdIds.interstitialAdIdAdMobSplash,
            object : LoadAdCallBack {
                override fun onLoaded() {
                    showStartButton()
                }

                override fun onFailed() {
                    showStartButton()
                }
            }
        )
    }
}