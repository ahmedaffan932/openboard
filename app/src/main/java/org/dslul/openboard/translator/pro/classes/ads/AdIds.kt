package org.dslul.openboard.translator.pro.classes.ads

import org.dslul.openboard.inputmethod.latin.BuildConfig

object AdIds {

    var bannerAdIdAdOne = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        "ca-app-pub-6814505709397727/9113781148"
    }

    var appOpenAdIdOne = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/3419835294"
    } else {
        "ca-app-pub-6814505709397727/2248951904"
    }

    var nativeAdIdAdMob: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        "ca-app-pub-6814505709397727/1291093454"
    }

    var nativeAdIdAdMobSplash: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        "ca-app-pub-6814505709397727/3725685104"
    }

    var interstitialAdIdAdMobSplash: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-6814505709397727/6566087979"
    }

    var interstitialAdIdAdMob: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-6814505709397727/1853366084"
    }

    var collapsibleBannerAdIdAd: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2014213617"
    } else {
        ""
    }

}