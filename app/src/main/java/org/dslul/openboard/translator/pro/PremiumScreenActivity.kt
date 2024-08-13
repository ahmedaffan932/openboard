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
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPremiumScreenBinding
import org.dslul.openboard.translator.pro.classes.InAppUtils
import org.dslul.openboard.translator.pro.classes.Misc

class PremiumScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPremiumScreenBinding

    // In-App position is not according to that in which you enter items it's arrangement
    // is according to product id in ascending order.
    var inAppPosition = 1
    private lateinit var inAppDetailsBottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var subscriptionBottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        InAppUtils.showProducts()

        inAppDetailsBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetInAppDetails))
        inAppDetailsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        subscriptionBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetSubscriptionDetails))
        subscriptionBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

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
            inAppPosition = 1
            binding.clWeekly.setBackgroundResource(R.drawable.bg_in_app_item_selected)
            binding.clYearly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.clMonthly.setBackgroundResource(R.drawable.bg_in_app_item)


            launchPurchaseFlow(InAppUtils.mProductDetailsList, inAppPosition)
        }

        binding.clMonthly.setOnClickListener {
            Log.d(Misc.logKey, Misc.monthlyKey)
            inAppPosition = 0
            binding.clYearly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.clWeekly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.clMonthly.setBackgroundResource(R.drawable.bg_in_app_item_selected)


            launchPurchaseFlow(InAppUtils.mProductDetailsList, inAppPosition)
        }

        binding.clYearly.setOnClickListener {
            inAppPosition = 2
            binding.clYearly.setBackgroundResource(R.drawable.bg_in_app_item_selected)
            binding.clWeekly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.clMonthly.setBackgroundResource(R.drawable.bg_in_app_item)


            launchPurchaseFlow(InAppUtils.mProductDetailsList, inAppPosition)
        }

        binding.btnGetPro.setOnClickListener {
            for (i in InAppUtils.mProductDetailsList) {
                Log.d(Misc.logKey, i.productId)
            }
            launchPurchaseFlow(InAppUtils.mProductDetailsList, inAppPosition)
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