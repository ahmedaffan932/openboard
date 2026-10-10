package org.dslul.openboard.translator.pro.objects.inapp


import com.google.gson.annotations.SerializedName
import org.dslul.openboard.translator.pro.objects.inapp.PricingPhases


data class SubscriptionOfferDetails (

//    @SerializedName("offerIdToken"  ) var offerIdToken  : String?                  = null,
//    @SerializedName("basePlanId"    ) var basePlanId    : String?                  = null,
    @SerializedName("pricingPhases" ) var pricingPhases : ArrayList<PricingPhases> = arrayListOf(),
//    @SerializedName("offerTags"     ) var offerTags     : ArrayList<String>        = arrayListOf()

)