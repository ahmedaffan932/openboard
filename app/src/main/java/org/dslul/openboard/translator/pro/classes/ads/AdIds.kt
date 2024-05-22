package org.dslul.openboard.translator.pro.classes.ads

import org.dslul.openboard.inputmethod.latin.BuildConfig

object AdIds {

    var mrecAdIdAd: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        ""
    }
    var translateNativeAdId: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        ""
    }

    var translateIntAdId: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        ""
    }

    var bannerAdIdAdOne = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        ""
    }

    var appOpenAdIdOne = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/3419835294"
    } else {
        ""
    }

    var nativeAdIdAdMob: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        ""
    }

    var nativeAdIdAdMobSplash: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        ""
    }

    var interstitialAdIdAdMobSplash: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        ""
    }

    var interstitialAdIdAdMob: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        ""
    }

    var collapsibleBannerAdIdAd: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2014213617"
    } else {
        ""
    }

}