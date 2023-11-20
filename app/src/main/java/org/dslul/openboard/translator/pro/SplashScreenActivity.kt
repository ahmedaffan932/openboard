package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.billingclient.api.*
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.android.synthetic.main.activity_splash_screen.*
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.adaptor.LanguagesAdapter
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.startProActivity
import org.dslul.openboard.translator.pro.classes.admob.BannerAds
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import org.dslul.openboard.translator.pro.classes.admob.NativeAds
import org.dslul.openboard.translator.pro.fragments.SplashFragment
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import org.dslul.openboard.translator.pro.interfaces.LoadInterstitialCallBack

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private lateinit var billingClient: BillingClient


    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Misc.setPurchasedStatus(this, true)
            Log.d(Misc.logKey, "Ya hooo.....")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash_screen)

        Firebase.analytics.logEvent("SplashScreenStarted", null)

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
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

        splashViewPager.adapter = FragmentAdapter(this)

        splashViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                if (position == 0) {
                    clTextBottom.visibility = View.VISIBLE
                    splashTabLayout.visibility = View.INVISIBLE
                } else {
                    clTextBottom.visibility = View.INVISIBLE
                    splashTabLayout.visibility = View.VISIBLE
                }
            }
        })

        TabLayoutMediator(splashTabLayout, splashViewPager) { tab, position -> }.attach()

        btnContinue.setOnClickListener {
            if (splashViewPager.currentItem < 2) {
                splashViewPager.setCurrentItem(splashViewPager.currentItem + 1, true)
            } else {
                startActivity(Intent(this, TranslateActivity::class.java))
            }
        }


    }

    override fun onBackPressed() {
        if (!Misc.splashScreenOnBackPressDoNothing)
            super.onBackPressed()
    }


    private inner class FragmentAdapter(fa: FragmentActivity) :
        FragmentStateAdapter(fa) {
        override fun getItemCount(): Int {
            return 3
        }

        override fun createFragment(position: Int): Fragment {
            return SplashFragment.newInstance(position.toString())
        }
    }

}