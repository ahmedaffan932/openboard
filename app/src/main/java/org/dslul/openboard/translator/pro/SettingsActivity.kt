package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.*
import org.dslul.openboard.translator.pro.classes.Misc.startProActivity
import org.dslul.openboard.translator.pro.classes.admob.BannerAds
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import org.dslul.openboard.translator.pro.classes.admob.NativeAds
import org.dslul.openboard.translator.pro.interfaces.LoadInterstitialCallBack

class SettingsActivity : AppCompatActivity() {
    var showingInterstitial = false

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
                Misc.setPurchasedStatus(this, true)
                Log.d(Misc.logKey, "Ya hooo.....")
                Toast.makeText(this, "Restarting Application.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SplashScreenActivity::class.java))
                finish()
            }
        }

    private lateinit var billingClient: BillingClient

    @SuppressLint("QueryPermissionsNeeded")
    //@DelicateCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_settings)

        if (intent.getStringExtra(Misc.data) != null && !Misc.getPurchasedStatus(this) && InterstitialAd.interAdmob == null) {
            val objDialog = Misc.LoadingAdDialog(this)
            objDialog.setCancelable(false)
            objDialog.show()

            objDialog.findViewById<TextView>(R.id.warning).visibility = View.VISIBLE
            Misc.anyAdLoaded.observeForever { t ->
                if (t) {
                    if(!showingInterstitial) {
                        InterstitialAd.showInterstitial(this, Misc.getAppOpenIntAm(this))
                    }
                    showingInterstitial = true
                }
            }
            object : CountDownTimer(1500, 3000) {
                override fun onTick(p0: Long) {}
                override fun onFinish() {
                    if (objDialog.isShowing) {
                        objDialog.dismiss()
                    }
                }
            }.start()

        }

        Misc.isRemoteConfigFetched.observeForever { t ->
            if (t == true) {
                BannerAds.load(this)
                object : CountDownTimer(1500, 1000) {
                    override fun onTick(p0: Long) {}
                    override fun onFinish() {
                        NativeAds.loadNativeAd(this@SettingsActivity, object : LoadInterstitialCallBack{
                            override fun onLoaded() {
                                NativeAds.manageShowNativeAd(
                                    this@SettingsActivity,
                                    Misc.splashNativeAm,
                                    nativeAdFrameLayout
                                )
                            }
                        })
                    }
                }.start()
            }
        }

        NativeAds.manageShowNativeAd(
            this,
            Misc.settingsNativeAm,
            nativeAdFrameLayout
        )

        InterstitialAd.showInterstitial(this, Misc.settingsIntAm)

        if (Misc.isNightModeOn(this)) {
            tvUpgradeToPremium.setBackgroundResource(R.drawable.ic_bg_pro_yellow)
        }

        Misc.isActivityCreatingFirstTime = true

        billingClient = newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d(Misc.logKey, "Billing Result Ok")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(Misc.logKey, "Service disconnected")
            }
        })

        tvUpgradeToPremium.setOnClickListener {
            Firebase.analytics.logEvent("ProScreenFromSettings", null)
            startProActivity(Misc.data)
        }

        if (Misc.selectThemeMode(this)) {
            btnSwitchDarkTheme.isChecked = true
        }

        llShareApp.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Have a look to this interesting application:- \n \n${Misc.appUrl}"
            )
            startActivity(Intent.createChooser(sharingIntent, "Share via"))
        }

        llPrivacyPolicy.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://sites.google.com/view/elite-translator/translatorpro")
            )
            startActivity(intent)
        }

        llTermsAndConditions.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://sites.google.com/view/elite-translator/translatorpro-term-conditions")
            )
            startActivity(intent)
        }

        btnSwitchDarkTheme.setOnCheckedChangeListener { _, _ ->
            if (Misc.isNightModeOn(this)) {
                Misc.setNightMode(this, false)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                Misc.setNightMode(this, true)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        btnBackSettings.setOnClickListener {
            onBackPressed()
        }

        llFeedback.setOnClickListener {
            val objEmailUsDialog = EmailUsDialogBox(this)
            objEmailUsDialog.show()
            val window: Window = objEmailUsDialog.window!!
            window.setLayout(
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(R.color.color_nothing)
            objEmailUsDialog.findViewById<ConstraintLayout>(R.id.btnPublishFeedback)
            objEmailUsDialog.findViewById<ConstraintLayout>(R.id.btnPublishFeedback)
                .setOnClickListener {
                    val sub = objEmailUsDialog.findViewById<TextView>(R.id.etTopic).text.toString()
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "message/rfc822"
                    i.putExtra(Intent.EXTRA_EMAIL, arrayOf("elitetranslatorapps@gmail.com"))
                    i.putExtra(
                        Intent.EXTRA_TEXT,
                        objEmailUsDialog.findViewById<EditText>(R.id.etFeedbackBody).text
                    )
                    i.putExtra(Intent.EXTRA_SUBJECT, sub)
                    i.setPackage("com.google.android.gm")

                    try {
                        startActivity(Intent.createChooser(i, "Send mail..."))
                        objEmailUsDialog.dismiss()
                    } catch (ex: ActivityNotFoundException) {
                        Toast.makeText(
                            this,
                            "Some error occurred in sending email.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            objEmailUsDialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
                objEmailUsDialog.dismiss()
            }
        }

        llRateUs.setOnClickListener {
            val objRateUsDialog = RateUsDialog(this)
            objRateUsDialog.show()
            val window: Window = objRateUsDialog.window!!
            window.setLayout(
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(R.color.color_nothing)

            objRateUsDialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
                objRateUsDialog.dismiss()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateA).setOnClickListener {
                objRateUsDialog.dismiss()
                Toast.makeText(this, "Thanks.", Toast.LENGTH_SHORT).show()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateB).setOnClickListener {
                objRateUsDialog.dismiss()
                Toast.makeText(this, "Thanks.", Toast.LENGTH_SHORT).show()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateC).setOnClickListener {
                objRateUsDialog.dismiss()
                Toast.makeText(this, "Thanks.", Toast.LENGTH_SHORT).show()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateD).setOnClickListener {
                objRateUsDialog.dismiss()
                rateUs()
            }
            objRateUsDialog.findViewById<ImageView>(R.id.btnRateE).setOnClickListener {
                objRateUsDialog.dismiss()
                rateUs()
            }
            objRateUsDialog.findViewById<ConstraintLayout>(R.id.btnRateUs).setOnClickListener {
                objRateUsDialog.dismiss()
                rateUs()
            }
        }
    }


    private suspend fun querySkuDetails() {
        try {
            val skuList = ArrayList<String>()
            skuList.add(Misc.inAppKey)

            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(SkuType.INAPP)

            // leverage querySkuDetails Kotlin extension function
            val skuDetailsResult = withContext(Dispatchers.IO) {
                billingClient.querySkuDetails(params.build())
            }

            val flowParams = skuDetailsResult.skuDetailsList?.get(0)?.let {
                BillingFlowParams.newBuilder()
                    .setSkuDetails(it)
                    .build()
            }
            val responseCode = flowParams?.let {
                billingClient.launchBillingFlow(
                    this,
                    it
                ).responseCode
            }

            // Process the result.
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rateUs() {
        val p = "com.guru.translate.translator.translation.learn.language"
        val uri: Uri = Uri.parse("market://details?id=$p")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)

        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$p")
                )
            )
        }
    }

    override fun onBackPressed() {
        if (intent.getStringExtra(Misc.data) != null) {
            startActivity(Intent(this, TranslateActivity::class.java))
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Misc.isNativeAdClicked) {
            NativeAds.manageShowNativeAd(
                this,
                Misc.settingsNativeAm,
                nativeAdFrameLayout
            )
        }
    }
}