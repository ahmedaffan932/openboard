package org.dslul.openboard.translator.pro.objects.inapp

import com.google.gson.annotations.SerializedName
import org.dslul.openboard.translator.pro.objects.inapp.SubscriptionOfferDetails

data class Zza(
    @SerializedName("name") var name: String? = null,
    @SerializedName("productId") var productId: String? = null,
    @SerializedName("subscriptionOfferDetails") var subscriptionOfferDetails: ArrayList<SubscriptionOfferDetails> = arrayListOf()
)