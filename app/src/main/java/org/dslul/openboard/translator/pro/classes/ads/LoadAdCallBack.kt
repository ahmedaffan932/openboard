package com.example.translatorguru.ads.admob

import com.google.android.gms.ads.interstitial.InterstitialAd

interface LoadAdCallBack {
    fun onLoaded(){}
    fun onFailed(){}
}