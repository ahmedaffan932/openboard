package org.dslul.openboard.translator.pro

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import kotlinx.android.synthetic.main.activity_premium_screen.*
import kotlinx.coroutines.*
import com.guru.translate.translator.pro.translation.keyboard.translator.R
import org.dslul.openboard.translator.pro.classes.Misc

class PremiumScreenActivity : AppCompatActivity() {
    private var isBillingClientConnected = false

    @OptIn(DelicateCoroutinesApi::class)
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                Misc.setPurchasedStatus(this, true)
                GlobalScope.launch {
                    try {
                        handlePurchase(purchases[0])
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@PremiumScreenActivity,
                            "Sorry, some error occurred. Please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                        e.printStackTrace()
                    }
                }
                Log.d(Misc.logKey, "Ya hooo.....")
                Toast.makeText(this, "Restarting Application.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SplashScreenActivity::class.java))
                finish()
            }
        }

    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium_screen)

        tvLifeTimePrice.text = Misc.lifeTimePrice


        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()


        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isBillingClientConnected = true
                    Log.d(Misc.logKey, "Billing Result Ok")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(Misc.logKey, "Service disconnected")
            }
        })

        clOneTimePurchase.setOnClickListener {
            startBillingProcess()
        }

        animation.setOnClickListener {
            startBillingProcess()
        }

        btnGetPro.setOnClickListener {
            startBillingProcess()
        }

        Handler().postDelayed({
            btnDismiss.visibility = View.VISIBLE
            Misc.zoomInView(btnDismiss, this@PremiumScreenActivity, 150)
        }, Misc.proScreenDismissBtnVisibleAfter)

        btnDismiss.setOnClickListener {
            if (intent.getStringExtra(Misc.data) == null) {
                startActivity(Intent(this, TranslateActivity::class.java))
                finish()
            } else {
                finish()
            }
        }

    }

    private suspend fun querySkuDetails() {
        try {
            val skuList = ArrayList<String>()
            skuList.add(Misc.inAppKey)

            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

            val skuDetailsResult = withContext(Dispatchers.IO) {
                billingClient.querySkuDetails(params.build())
            }

            val flowParams = skuDetailsResult.skuDetailsList?.get(0)?.let {
                BillingFlowParams.newBuilder()
                    .setSkuDetails(it)
                    .build()
            }
            flowParams?.let {
                billingClient.launchBillingFlow(
                    this,
                    it
                ).responseCode
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Not available yet.", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        val consumeParams =
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        val consumeResult = withContext(Dispatchers.IO) {
            billingClient.consumePurchase(consumeParams)
        }

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                val ackPurchaseResult = withContext(Dispatchers.IO) {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
                }
            }
    }

    override fun onBackPressed() {
        if (intent.getStringExtra(Misc.data) != null) {
            finish()
        }
    }

    private fun startBillingProcess() {
        if (isBillingClientConnected) {
            GlobalScope.launch {
                try {
                    querySkuDetails()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            Toast.makeText(
                this,
                getString(R.string.please_check_your_internet_connection_and_try_again),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}