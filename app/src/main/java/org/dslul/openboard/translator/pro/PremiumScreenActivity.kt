package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
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

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.getStringExtra(Misc.data) != null) {
            Firebase.analytics.logEvent("v_onboarding_PremiumScreen", null)
        } else {
            Firebase.analytics.logEvent("v_onboarding_PremiumScreenInflow", null)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            Misc.zoomInView(binding.btnDismiss, this, 250)
        }, 3000)

        InAppUtils.showProducts(object : InAppProductsDetailsCallback {
            override fun onFetched(weeklyPrice: String, monthlyPrice: String, yearlyPrice: String) {
                runOnUiThread {
                    if (weeklyPrice.isNotBlank()) {
                        binding.tvWeeklyPrice.text = weeklyPrice
                    }
                    if (monthlyPrice.isNotBlank()) {
                        binding.tvMonthlyPrice.text = monthlyPrice
                    }
                }
            }
        })

        binding.clPB.setOnClickListener {

        }

        binding.tvTerms.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://zyroapps.com/terms-and-conditions.php")
                )
            )
        }

        binding.tvPrivacyPolicy.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://zyroapps.com/privacy-policy.php")
                )
            )
        }

        binding.btnGetPro.setOnClickListener {
            launchPurchaseFlow(selectedProductId)
        }

        binding.clWeekly.setOnClickListener {
            selectedProductId = Misc.weeklyKey
            binding.clWeekly.setBackgroundResource(R.drawable.bg_in_app_item_selected)
            binding.clMonthly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.tvMonthly.setBackgroundResource(R.drawable.bg_top_in_app_item_un_selected)
            launchPurchaseFlow(selectedProductId)
        }

        binding.clMonthly.setOnClickListener {
            selectedProductId = Misc.monthlyKey
            binding.clWeekly.setBackgroundResource(R.drawable.bg_in_app_item)
            binding.clMonthly.setBackgroundResource(R.drawable.bg_in_app_item_selected)
            binding.tvMonthly.setBackgroundResource(R.drawable.bg_top_in_app_item_selected)
            launchPurchaseFlow(selectedProductId)
        }

        binding.btnDismiss.setOnClickListener {
            closePremiumScreen()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (intent.getStringExtra(Misc.data) != null || shouldOpenLanguageAfterPremium()) {
            closePremiumScreen()
        }
    }

    private fun closePremiumScreen() {
        when {
            shouldOpenLanguageAfterPremium() -> {
                startActivity(
                    Intent(this, AppLanguageSelectorActivity::class.java)
                        .putExtra(Misc.premiumShownBeforeLanguage, true)
                )
                finish()
            }

            intent.getStringExtra(Misc.data) == null -> {
                finish()
            }

            else -> {
                startActivity(Intent(this, FragmentsDashboardActivity::class.java))
                finish()
            }
        }
    }

    private fun shouldOpenLanguageAfterPremium(): Boolean {
        return intent.getBooleanExtra(Misc.premiumShownBeforeLanguage, false)
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
            1f, 1f,
            startScale, endScale,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 1f
        )
        anim.fillAfter = true
        anim.duration = 1000
        v.startAnimation(anim)
    }

    private fun launchPurchaseFlow(productId: String) {
        try {
            val productDetails = InAppUtils.mProductDetailsList.firstOrNull { it.productId == productId }
            val offerToken = productDetails?.let { InAppUtils.bestOfferToken(it) }
            if (productDetails == null || offerToken == null) {
                Toast.makeText(
                    this,
                    getString(R.string.please_check_your_internet_connection_and_try_again),
                    Toast.LENGTH_SHORT
                ).show()
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
            InAppUtils.billingClient.launchBillingFlow(this, billingFlowParams)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.please_cehck_your_internet, Toast.LENGTH_SHORT)
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
                if (shouldOpenLanguageAfterPremium()) {
                    startActivity(
                        Intent(this, AppLanguageSelectorActivity::class.java)
                            .putExtra(Misc.premiumShownBeforeLanguage, true)
                    )
                } else {
                    startActivity(Intent(this, FragmentsDashboardActivity::class.java))
                }
                finish()
            }
        }
    }
}
