package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPremiumScreenBinding
import org.dslul.openboard.translator.pro.classes.Misc

class PremiumScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPremiumScreenBinding
    var inAppPosition = 1
    private lateinit var billingClient: BillingClient
    private var productDetailsList = ArrayList<ProductDetails>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        billing()

        binding.clPB.setOnClickListener {

        }

//        binding.tvYearlyPrice.text = Misc.yearlyPrice
//        binding.tvWeeklyPrice.text = Misc.weeklyPrice
//        binding.tvMonthlyPrice.text = Misc.monthlyPrice

        Handler(Looper.getMainLooper()).postDelayed({
            binding.clPB.visibility = View.GONE
        }, 1000)


        binding.clMonthly.setOnClickListener {
            inAppPosition = 0
            binding.clYearly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.clWeekly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.clMonthly.setBackgroundResource(R.drawable.bg_in_app_item_selected)


            launchPurchaseFlow(productDetailsList, inAppPosition)
        }

        binding.clWeekly.setOnClickListener {
            inAppPosition = 1
            binding.clWeekly.setBackgroundResource(R.drawable.bg_in_app_item_selected)
            binding.clYearly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.clMonthly.setBackgroundResource(R.drawable.bg_in_app_item)


            launchPurchaseFlow(productDetailsList, inAppPosition)
        }

        binding.clYearly.setOnClickListener {
            inAppPosition = 2
            binding.clYearly.setBackgroundResource(R.drawable.bg_in_app_item_selected)
            binding.clWeekly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.clMonthly.setBackgroundResource(R.drawable.bg_in_app_item)


            launchPurchaseFlow(productDetailsList, inAppPosition)
        }

        binding.btnGetPro.setOnClickListener {
            launchPurchaseFlow(productDetailsList, inAppPosition)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            binding.btnDismiss.visibility = View.VISIBLE
        }, 3000)

        binding.btnDismiss.setOnClickListener {
            if (intent.getStringExtra(Misc.logKey) == null) {
                finish()
            } else {
                startActivity(
                    Intent(
                        this,
                        FragmentsDashboardActivity::class.java
                    )
                )
                finish()
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (intent.getStringExtra(Misc.data) == null) {
            super.onBackPressed()
        }
    }

    private fun billing() {
        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult, list ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    for (purchase in list) {
                        verifySubPurchase(purchase)
                    }
                }
            }.build()

        establishConnection()
    }

    var connectionFailedCount = 0
    fun establishConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@NonNull billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    showProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                if (Misc.checkInternetConnection(this@PremiumScreenActivity)) {
                    if (connectionFailedCount < 4)
                        establishConnection()

                    connectionFailedCount++
                } else {
                    Toast.makeText(
                        this@PremiumScreenActivity,
                        resources.getString(R.string.please_check_your_internet_connection_and_try_again),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    fun showProducts() {
        val productList: ImmutableList<QueryProductDetailsParams.Product> =
            ImmutableList.of(
                //Product 1
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(Misc.weeklyKey)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),  //Product 3
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(Misc.monthlyKey)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(Misc.yearlyKey)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),  //Product 2
            )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient.queryProductDetailsAsync(
            params
        ) { billingResult: BillingResult?, prodDetailsList: List<ProductDetails?>? ->
            productDetailsList.clear()
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(Misc.logKey, "posted delayed")
                if (prodDetailsList != null) {
                    for ((i, item) in prodDetailsList.withIndex()) {
                        Log.d(Misc.logKey, "item name ${item?.name} $i")
                        item?.let { productDetailsList.add(it) }
                    }
                }
            }, 2000)
        }
    }

    private fun launchPurchaseFlow(arrProductDetails: ArrayList<ProductDetails>, position: Int) {
        try {
            val productDetails = arrProductDetails[position]
            assert(productDetails.subscriptionOfferDetails != null)
            val productDetailsParamsList = ImmutableList.of(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(productDetails.subscriptionOfferDetails!![0].offerToken)
                    .build()
            )
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
            val billingResult = billingClient.launchBillingFlow(this, billingFlowParams)
        } catch (e: Exception) {
            Toast.makeText(this, "Please check your internet and try again.", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }

    }

    private fun verifySubPurchase(purchases: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams
            .newBuilder()
            .setPurchaseToken(purchases.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(
            acknowledgePurchaseParams
        ) { billingResult: BillingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Toast.makeText(
                    this,
                    getString(R.string.subscription_activated),
                    Toast.LENGTH_SHORT
                ).show()
                Misc.setPurchasedStatus(this, true)
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult: BillingResult, list: List<Purchase> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        verifySubPurchase(purchase)
                    }
                }
            }
        }
    }

    fun scaleView(v: View, startScale: Float, endScale: Float) {
        val anim: Animation = ScaleAnimation(
            1f, 1f,  // Start and end values for the X axis scaling
            startScale, endScale,  // Start and end values for the Y axis scaling
            Animation.RELATIVE_TO_SELF, 0f,  // Pivot point of X scaling
            Animation.RELATIVE_TO_SELF, 1f
        ) // Pivot point of Y scaling
        anim.fillAfter = true // Needed to keep the result of the animation
        anim.duration = 1000
        v.startAnimation(anim)
    }

}