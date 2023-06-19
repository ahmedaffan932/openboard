package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.android.synthetic.main.activity_splash_screen.*
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.adaptor.LanguagesAdapter
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.startNotification
import org.dslul.openboard.translator.pro.classes.Misc.startProActivity
import org.dslul.openboard.translator.pro.classes.admob.BannerAds
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import org.dslul.openboard.translator.pro.classes.admob.NativeAds
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import org.dslul.openboard.translator.pro.interfaces.LoadInterstitialCallBack

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private var isStartButtonVisible = false
    private var handler = Handler()
    private var loadingPercentage = 0
    private var isNativeAdDisplayed = false

    private lateinit var billingClient: BillingClient
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Misc.setPurchasedStatus(this, true)
            Log.d(Misc.logKey, "Ya h o o o.....")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash_screen)

        startNotification()

        llLogo.visibility = View.VISIBLE

        Firebase.analytics.logEvent("SplashScreenStarted", null)

        billingClient = BillingClient.newBuilder(this).setListener(purchasesUpdatedListener)
            .enablePendingPurchases().build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                billingClient.queryPurchases(BillingClient.SkuType.INAPP)
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP) { p0, p1 ->
                        Log.d(Misc.logKey, p1?.size.toString() + " size ..")
                        if (p1 != null) {
                            for (purchase in p1) {
                                Misc.setPurchasedStatus(this@SplashScreenActivity, true)
                            }
                        }
                    }
                    Log.d(Misc.logKey, "Billing Result Ok")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(Misc.logKey, "Service disconnected")
            }
        })

        handler.post(runLoadingPercentage)

        Misc.isRemoteConfigFetched.observeForever { t ->
            if (Misc.splashContinueBtnText.isNotEmpty()) {
                tvStart.text = Misc.splashContinueBtnText
            }
        }
        loadAds()

        val t: Long = if (BuildConfig.DEBUG) {
            6200
        } else {
            if (Misc.getPurchasedStatus(this)) {
                2000
            } else {
                6200
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({ showStartButton() }, t)

        val arr = ArrayList<String>()
        for (lng in TranslateLanguage.getAllLanguages()) {
            Log.d(Misc.logKey, lng)
            arr.add(lng)
        }


        recyclerViewLanguages.layoutManager = LinearLayoutManager(this)
        recyclerViewLanguages.adapter =
            LanguagesAdapter(arr, true, this, object : InterstitialCallBack {
                override fun onDismiss() {
                    Firebase.analytics.logEvent("StartedAfterSelectingLanguage", null)
                    val objDialog = Misc.LoadingAdDialog(this@SplashScreenActivity)
                    objDialog.setCancelable(false)
                    objDialog.show()
                    try {
                        Handler().postDelayed({
                            startNextActivity()
                        }, 2700)
                    } catch (e: Exception) {
                        startNextActivity()
                    }
                }
            })

        val anim = AnimationUtils.loadAnimation(this, R.anim.logo_scale_up)
        anim.duration = 0

        btnStart.setOnClickListener {
//            if (Misc.isFirstTimeShowLanguagesEnabled) {
//                if (Misc.isFirstTime(this)) {
//                    Firebase.analytics.logEvent("LanguagesDisplayedOnSplash", null)
//
//                    Misc.zoomOutView(btnStart, this, 250)
//                    clLanguages.animate().translationY(0F).duration = 500
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        btnStart.visibility = View.GONE
//                    }, 220)
//                } else {
//                    startNextActivity()
//                }
//            } else {
            startNextActivity()
//            }
        }
    }


    private fun showStartButton() {
        if (!isStartButtonVisible) {
            Misc.zoomOutView(spline, this, 250)
            Misc.zoomOutView(tvLoading, this, 250)

            Handler(Looper.getMainLooper()).postDelayed({
                spline.visibility = View.GONE
                tvLoading.visibility = View.GONE
                Misc.zoomInView(btnStart, this, 250)
            }, 250)

            val a: Animation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_logo)
            a.duration = 500

            a.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {

                }

                override fun onAnimationEnd(p0: Animation?) {
                    spline.visibility = View.INVISIBLE
                }

                override fun onAnimationRepeat(p0: Animation?) {

                }
            })
        }
        isStartButtonVisible = true
    }


    private val runLoadingPercentage: Runnable by lazy {
        return@lazy object : Runnable {
            @SuppressLint("LogNotTimber", "SetTextI18n")
            override fun run() {
                if (loadingPercentage < 100) loadingPercentage += 1
                handler.postDelayed(this, 70)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(Misc.logKey, "Splash screen onStop .")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(Misc.logKey, "Splash screen onDestroyed .")

        Misc.isIntDisplayed = false
    }

    private fun loadAds() {
        val frameLayout = if (Misc.splashNativeAm.contains("no_media")) {
            nativeAdFrameLayoutSmall
        } else {
            nativeAdFrameLayout
        }

        NativeAds.loadNativeAd(
            activity = this,
            object : LoadInterstitialCallBack {
                override fun onLoaded() {
//                    if (!isNativeAdDisplayed) {
                    NativeAds.manageShowNativeAd(
                        this@SplashScreenActivity,
                        Misc.splashNativeAm,
                        frameLayout
                    )
                    frameLayout.visibility = View.VISIBLE
                    if (Misc.splashNativeAm.contains("no_media")) {
                        frameLayout.animate().translationY(0F).duration = 500
                    }
//                    }
//                    isNativeAdDisplayed = true
                }
            }
        )

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        Log.d(Misc.logKey, "sp = ${preferences.all}")
        preferences.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            Log.d(Misc.logKey, "sp = ${sharedPreferences.all}")
        }

        BannerAds.load(this@SplashScreenActivity)
        InterstitialAd.manageLoadInterAdmob(this@SplashScreenActivity)

    }

    override fun onBackPressed() {
        if (!Misc.splashScreenOnBackPressDoNothing)
            super.onBackPressed()
    }

    private fun startNextActivity() {
        if (Misc.isDirectTranslateScreenEnabled) {
            val intent = Intent(this, TranslateActivity::class.java)
            intent.putExtra("isDirectTranslateScreenEnabled", true)
            startActivity(intent)
            finish()
            return
        }
        if (Misc.isFirstTime(this) && Misc.isKeyboardSelectionInFlow) {
            val intent = Intent(this, EnableKeyboardActivity::class.java)
            intent.putExtra(Misc.data, Misc.data)
            startActivity(intent)
            return
        }
        if (Misc.getPurchasedStatus(this)) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
            if (Misc.isProScreenEnabled) {
                Firebase.analytics.logEvent("ProInFlow", null)
                startProActivity()
                finish()
            } else {
                startActivity(
                    Intent(
                        this@SplashScreenActivity, DashboardActivity::class.java
                    )
                )
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Misc.isIntDisplayed = true
    }

    override fun onResume() {
        super.onResume()
        val frameLayout = if (Misc.splashNativeAm.contains("no_media")) {
            nativeAdFrameLayoutSmall
        } else {
            nativeAdFrameLayout
        }
        if (Misc.isNativeAdClicked) {
            NativeAds.manageShowNativeAd(
                this,
                Misc.translateNativeAm,
                frameLayout
            )
        }
    }
}