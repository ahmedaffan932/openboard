package org.dslul.openboard.translator.pro


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivitySplashScreenBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobInterstitialAd
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobMRECAds
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashScreenBinding
    private var isIntAdLoaded = false
    private var isNativeAdLoaded = false
    private var isStartButtonVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadAds()

        object : CountDownTimer(8000, 1000) {
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


        binding.btnStart.setOnClickListener {
            startNextActivity()
        }

    }


    private fun startNextActivity() {
//        Ads.showInterstitial(this, Ads.splashInt, object : InterstitialCallBack {
//            override fun onDismiss() {
                if (Misc.isFirstTime(this@SplashScreenActivity)) {
                    startActivity(
                        Intent(
                            this@SplashScreenActivity,
                            AppLanguageSelectorActivity::class.java
                        )
                    )
                    finish()
                } else {
                    startActivity(
                        Intent(
                            this@SplashScreenActivity,
                            FragmentsDashboardActivity::class.java
                        )
                    )
                    finish()
                }
//            }
//        })
    }

    override fun onResume() {
        super.onResume()
        Misc.isSplashScreen = true
    }

    override fun onPause() {
        super.onPause()
        Misc.isSplashScreen = false
    }

    private fun showStartButton() {
        if (!isStartButtonVisible) {
            Misc.zoomInView(binding.btnStart, this, 250)
            binding.spline.visibility = View.INVISIBLE
        }
        isStartButtonVisible = true
    }

    private fun loadAds() {
        Handler(Looper.getMainLooper()).postDelayed({
            AdmobInterstitialAd.loadInterAdmob(
                this,
                AdIds.interstitialAdIdAdMobSplash,
                object : LoadAdCallBack {
                    override fun onLoaded() {
                        isIntAdLoaded = true
                    }

                    override fun onFailed() {
                        isIntAdLoaded = true
                    }
                }
            )
        }, 1000)

        Ads.loadAndShowNativeAd(
            this,
            AdIds.nativeAdIdAdMobSplash,
            Ads.splashNative,
            binding.nativeAdFrameLayout,
            R.layout.admob_native_splash,
            R.layout.shimmer_native_splash,
            object : LoadAdCallBack {
                override fun onFailed() {
                    isNativeAdLoaded = true
                }

                override fun onLoaded() {
                    isNativeAdLoaded = true
                }
            }
        )
        AdmobMRECAds.loadMREC(this)
    }
}