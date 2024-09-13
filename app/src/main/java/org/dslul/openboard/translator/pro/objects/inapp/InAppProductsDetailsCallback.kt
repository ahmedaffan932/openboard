package org.dslul.openboard.translator.pro.objects.inapp

interface InAppProductsDetailsCallback {
    fun onFetched(weeklyPrice: String, monthlyPrice: String, yearlyPrice: String)
}