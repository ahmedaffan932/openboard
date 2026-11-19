package org.dslul.openboard.translator.pro.objects.inapp

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.common.collect.ImmutableList
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc

object InAppUtils {
    var connectionFailedCount = 0
    var mProductDetailsList = ArrayList<ProductDetails>()
    lateinit var billingClient: BillingClient

    /**
     * The product the purchase sheet was last opened for. A cancel or an error comes back with no
     * purchase attached, so this is what names the plan in `premium_purchase_result`.
     */
    @Volatile
    var lastLaunchedProductId: String? = null


    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            val code = billingResult.responseCode
            when {
                code == BillingClient.BillingResponseCode.OK && purchases != null -> {
                    for (purchase in purchases) {
                        Log.d(Misc.logKey, "Purchase made")
                        val status = when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> AppAnalytics.PURCHASE_SUCCESS
                            Purchase.PurchaseState.PENDING -> AppAnalytics.PURCHASE_PENDING
                            else -> AppAnalytics.PURCHASE_FAILED
                        }
                        AppAnalytics.premiumPurchaseResult(
                            purchase.products.firstOrNull() ?: lastLaunchedProductId, status
                        )
                    }
                }

                code == BillingClient.BillingResponseCode.USER_CANCELED ->
                    AppAnalytics.premiumPurchaseResult(
                        lastLaunchedProductId, AppAnalytics.PURCHASE_CANCELLED
                    )

                else -> AppAnalytics.premiumPurchaseResult(
                    lastLaunchedProductId, AppAnalytics.PURCHASE_FAILED, code
                )
            }
        }


    fun Context.establishConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@NonNull billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                }
                Log.d(Misc.logKey, "Connection is ready");
//                showProducts()
            }

            override fun onBillingServiceDisconnected() {
                // With enableAutoServiceReconnection(), Play Billing reconnects automatically.
                // Keep a limited manual retry as a fallback when the network is available.
                if (Misc.checkInternetConnection(this@establishConnection)) {
                    if (connectionFailedCount < 4)
                        establishConnection()

                    connectionFailedCount++
                } else {
                    Toast.makeText(
                        this@establishConnection,
                        resources.getString(R.string.please_check_your_internet_connection_and_try_again),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    fun Context.billing() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

        establishConnection()
    }

    fun showProducts(callBack: InAppProductsDetailsCallback? = null) {
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ImmutableList.of(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(Misc.weeklyKey)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(Misc.monthlyKey)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(Misc.yearlyKey)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()


        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult,
                                                                            productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = productDetailsResult.productDetailsList

                Log.d(Misc.logKey, "productDetailsList size ${productDetailsList.size}")
                mProductDetailsList.clear()
                for (item in productDetailsList) {
                    mProductDetailsList.add(item)
                }

                val weeklyPrice = formattedPriceFor(Misc.weeklyKey)
                val monthlyPrice = formattedPriceFor(Misc.monthlyKey)
                val yearlyPrice = formattedPriceFor(Misc.yearlyKey)

                Log.d(
                    Misc.logKey,
                    "objZzaWeekly $weeklyPrice"
                )
                Log.d(
                    Misc.logKey,
                    "objZzaMonthly $monthlyPrice"
                )
                Log.d(
                    Misc.logKey,
                    "objZzaYearly $yearlyPrice"
                )

                callBack?.onFetched(weeklyPrice, monthlyPrice, yearlyPrice)
            }
        }
    }

    private fun formattedPriceFor(productId: String): String {
        return mProductDetailsList
            .firstOrNull { it.productId == productId }
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull { it.priceAmountMicros > 0 }
            ?.formattedPrice
            .orEmpty()
    }

    /**
     * Returns the offer token to use at checkout for [productDetails], preferring an offer that
     * includes a free trial (a pricing phase priced at 0) so the advertised free trial is actually
     * applied. Falls back to the first available offer.
     */
    fun bestOfferToken(productDetails: ProductDetails): String? {
        val offers = productDetails.subscriptionOfferDetails ?: return null
        val freeTrialOffer = offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        }
        return (freeTrialOffer ?: offers.firstOrNull())?.offerToken
    }

}
