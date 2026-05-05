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
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.collect.ImmutableList
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPremiumScreenBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.objects.inapp.InAppProductsDetailsCallback
import org.dslul.openboard.translator.pro.objects.inapp.InAppUtils

class PremiumScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPremiumScreenBinding

    private var selectedProductId = Misc.weeklyKey
    private var weeklyTerms = ""
    private var monthlyTerms = ""
    private var yearlyTerms = ""
    private lateinit var inAppDetailsBottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var subscriptionBottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inAppDetailsBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetInAppDetails))
        inAppDetailsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        subscriptionBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetSubscriptionDetails))
        subscriptionBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        weeklyTerms = getLoadingOfferTerms(getString(R.string.weekly), getString(R.string.subscription_billing_weekly))
        monthlyTerms = getLoadingOfferTerms(getString(R.string.monthly), getString(R.string.subscription_billing_monthly))
        yearlyTerms = getLoadingOfferTerms(getString(R.string.yearly), getString(R.string.subscription_billing_yearly))
        selectPlan(Misc.weeklyKey)

        InAppUtils.showProducts(object : InAppProductsDetailsCallback {
            override fun onFetched(weeklyPrice: String, monthlyPrice: String, yearlyPrice: String) {
                runOnUiThread {
                    if (weeklyPrice.isNotBlank()) {
                        binding.tvWeeklyPer.text = getString(R.string.subscription_price_per_week, weeklyPrice)
                        weeklyTerms = getOfferTerms(
                            getString(R.string.weekly),
                            weeklyPrice,
                            getString(R.string.subscription_billing_weekly)
                        )
                    }
                    if (monthlyPrice.isNotBlank()) {
                        binding.tvMonthlyPer.text = getString(R.string.subscription_price_per_month, monthlyPrice)
                        monthlyTerms = getOfferTerms(
                            getString(R.string.monthly),
                            monthlyPrice,
                            getString(R.string.subscription_billing_monthly)
                        )
                    }
                    if (yearlyPrice.isNotBlank()) {
                        binding.tvYearlyPer.text = getString(R.string.subscription_price_per_year, yearlyPrice)
                        yearlyTerms = getOfferTerms(
                            getString(R.string.yearly),
                            yearlyPrice,
                            getString(R.string.subscription_billing_yearly)
                        )
                    }
                    updateSelectedPlanTerms()
                }
            }
        })


        binding.clMain.setOnClickListener {
            inAppDetailsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            subscriptionBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.tvShowDetails.setOnClickListener {
            inAppDetailsBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.tvSubscriptionTerms.setOnClickListener {
            subscriptionBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.bottomSheetInAppDetails.bottomSheetMain.setOnClickListener {

        }
        binding.bottomSheetSubscriptionDetails.bottomSheetMain.setOnClickListener {

        }


        binding.clWeekly.setOnClickListener {
            selectPlan(Misc.weeklyKey)
        }

        binding.clMonthly.setOnClickListener {
            Log.d(Misc.logKey, Misc.monthlyKey)
            selectPlan(Misc.monthlyKey)
        }

        binding.clYearly.setOnClickListener {
            selectPlan(Misc.yearlyKey)
        }

        binding.btnGetPro.setOnClickListener {
            for (i in InAppUtils.mProductDetailsList) {
                Log.d(Misc.logKey, i.productId)
            }
            launchPurchaseFlow(selectedProductId)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            binding.btnDismiss.visibility = View.VISIBLE
        }, 3000)

        binding.btnDismiss.setOnClickListener {
            if (intent.getStringExtra(Misc.data) == null) {
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


    private fun selectPlan(productId: String) {
        selectedProductId = productId
        binding.clWeekly.setBackgroundResource(
            if (productId == Misc.weeklyKey) R.drawable.bg_in_app_item_selected else R.drawable.bg_in_app_item
        )
        binding.clMonthly.setBackgroundResource(
            if (productId == Misc.monthlyKey) R.drawable.bg_in_app_item_selected else R.drawable.bg_in_app_item
        )
        binding.clYearly.setBackgroundResource(
            if (productId == Misc.yearlyKey) R.drawable.bg_in_app_item_selected else R.drawable.bg_in_app_item
        )
        updateSelectedPlanTerms()
    }

    private fun updateSelectedPlanTerms() {
        binding.tvCancelSubscription.text = when (selectedProductId) {
            Misc.monthlyKey -> monthlyTerms
            Misc.yearlyKey -> yearlyTerms
            else -> weeklyTerms
        }
    }

    private fun getOfferTerms(planName: String, price: String, billingFrequency: String): String {
        return getString(R.string.subscription_offer_terms, planName, price, billingFrequency)
    }

    private fun getLoadingOfferTerms(planName: String, billingFrequency: String): String {
        return getString(R.string.subscription_offer_terms_loading, planName, billingFrequency)
    }

    private fun launchPurchaseFlow(productId: String) {
        try {
            if (intent.getStringExtra(Misc.data) != null) {
                Firebase.analytics.logEvent("mBtnProContinue", null)
            }

            val productDetails = InAppUtils.mProductDetailsList.firstOrNull { it.productId == productId }
            val offerToken = productDetails?.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (productDetails == null || offerToken == null) {
                Toast.makeText(this, getString(R.string.please_check_your_internet_connection_and_try_again), Toast.LENGTH_SHORT)
                    .show()
                return
            }

            val productDetailsParamsList = ImmutableList.of(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
            val billingResult = InAppUtils.billingClient.launchBillingFlow(this, billingFlowParams)
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
        InAppUtils.billingClient.acknowledgePurchase(
            acknowledgePurchaseParams
        ) { billingResult: BillingResult ->
            Log.d(Misc.logKey, "verifySubPurchase: " + billingResult.responseCode)
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
        InAppUtils.billingClient.queryPurchasesAsync(
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
}
