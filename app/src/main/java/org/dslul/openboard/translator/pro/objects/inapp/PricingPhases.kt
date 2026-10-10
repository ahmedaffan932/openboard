package org.dslul.openboard.translator.pro.objects.inapp


import com.google.gson.annotations.SerializedName


data class PricingPhases (

//    @SerializedName("priceAmountMicros" ) var priceAmountMicros : Int?    = null,
//    @SerializedName("priceCurrencyCode" ) var priceCurrencyCode : String? = null,
    @SerializedName("formattedPrice"    ) var formattedPrice    : String? = null,
//    @SerializedName("billingPeriod"     ) var billingPeriod     : String? = null,
//    @SerializedName("recurrenceMode"    ) var recurrenceMode    : Int?    = null

)