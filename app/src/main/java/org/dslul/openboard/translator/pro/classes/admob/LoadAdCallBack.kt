package org.dslul.openboard.translator.pro.classes.admob

import com.google.android.gms.ads.interstitial.InterstitialAd

interface LoadAdCallBack {
    fun onLoaded(){}
    fun onFailed(){}
}