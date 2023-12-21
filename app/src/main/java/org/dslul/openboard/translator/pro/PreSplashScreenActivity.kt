package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.firebase.FirebaseApp
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.admob.AdIds
import org.dslul.openboard.translator.pro.classes.admob.AdmobBannerAds
import org.dslul.openboard.translator.pro.classes.admob.AdmobInterstitialAd
import org.dslul.openboard.translator.pro.classes.admob.AdmobNativeAds
import org.dslul.openboard.translator.pro.classes.admob.Ads
import org.dslul.openboard.translator.pro.classes.admob.AppOpenAdManager
import org.dslul.openboard.translator.pro.classes.admob.LoadAdCallBack
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import java.lang.annotation.Native

@SuppressLint("CustomSplashScreen")
class PreSplashScreenActivity : AppCompatActivity() {
    private var isAlreadyStarted = false
    private var isAppOpenAdShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_splash_screen)

        FirebaseApp.initializeApp(this)

        AppOpenAdManager.loadAd(this, AdIds.appOpenAdIdSplash, object : LoadAdCallBack {
            override fun onLoaded(interstitialAd: InterstitialAd?) {
                if (!isAlreadyStarted) {
                    AppOpenAdManager.showAdIfAvailable(
                        this@PreSplashScreenActivity,
                        Ads.isSplashAppOpenEnabled,
                        object : InterstitialCallBack {
                            override fun onDismiss() {
                                startSplashActivity()
                            }

                            override fun onAdDisplayed() {
                                isAppOpenAdShowing = true
                            }
                        }
                    )
                }
            }

            override fun onFailed() {
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            AdmobNativeAds.loadAdmobNative(this, AdIds.nativeAdIdAdMobSplash)

            AdmobBannerAds.load(this)
        },1000)

        Handler(Looper.getMainLooper()).postDelayed({
            AdmobInterstitialAd.loadInterAdmob(this, AdIds.interstitialAdIdAdMobSplash)
        }, 2000)

        Handler(Looper.getMainLooper()).postDelayed({
            startSplashActivity()
        }, 6000)
    }

    private fun startSplashActivity() {
        if (!isAlreadyStarted) {
            isAlreadyStarted = true

            if (Misc.isFirstTime(this)) {
                startActivity(Intent(this, SplashScreenActivity::class.java))
            } else {
                startActivity(Intent(this, TranslateActivity::class.java))
            }
            finish()
            return
        }
    }

}