package org.dslul.openboard.translator.pro.classes

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.common.collect.ImmutableList
import org.dslul.openboard.inputmethod.latin.R

object InAppUtils {
    var connectionFailedCount = 0
    var mProductDetailsList = ArrayList<ProductDetails>()
    lateinit var billingClient: BillingClient


    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    Log.d(Misc.logKey, "Purchase made")

                }
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
            .enablePendingPurchases()
            .setListener(purchasesUpdatedListener).build()

        establishConnection()
    }

    fun showProducts() {
//        val productList: ImmutableList<QueryProductDetailsParams.Product> =
//            ImmutableList.of(
//                //Product 1
//                QueryProductDetailsParams.Product.newBuilder()
//                    .setProductId(Misc.weeklyKey)
//                    .setProductType(BillingClient.ProductType.SUBS)
//                    .build(),  //Product 3
//                QueryProductDetailsParams.Product.newBuilder()
//                    .setProductId(Misc.monthlyKey)
//                    .setProductType(BillingClient.ProductType.SUBS)
//                    .build(),
//                QueryProductDetailsParams.Product.newBuilder()
//                    .setProductId(Misc.yearlyKey)
//                    .setProductType(BillingClient.ProductType.SUBS)
//                    .build(),  //Product 2
//            )

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
                                                                            productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(Misc.logKey, "productDetailsList size ${productDetailsList.size}")
                for (item in productDetailsList) {
                    Log.d(Misc.logKey, "item name ${item.name}")
                    item?.let { mProductDetailsList.add(it) }
                }
            }
        }
    }

}