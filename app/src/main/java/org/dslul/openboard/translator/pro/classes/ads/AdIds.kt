package org.dslul.openboard.translator.pro.classes.ads

import org.dslul.openboard.inputmethod.latin.BuildConfig

object AdIds {

    var mrecAdIdAd: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        "ca-app-pub-6814505709397727/9113781148"
    }
    var appOpenAdIdSplash = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/9257395921"
    } else {
        "ca-app-pub-6814505709397727/2248951904"
    }

    var nativeAdIdAdMobExit: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        "ca-app-pub-6814505709397727/4524821252"
    }
    var nativeAdIdAdMobTranslate: String = if (BuildConfig.DEBUG) {
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
    var interstitialAdIdAdMobPhrases: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-6814505709397727/1853366084"
    }
    var interstitialAdIdAdMobExit: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-6814505709397727/4929899953"
    }
    var interstitialAdIdAdMobTranslate: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-6814505709397727/4107772871"
    }
    var interstitialAdIdAdMobCameraTranslate: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        "ca-app-pub-6814505709397727/3868347146"
    }

    var collapsibleBannerAdIdAdChat: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2014213617"
    } else {
        "ca-app-pub-6814505709397727/8212784823"
    }
    var collapsibleBannerAdIdAdLanguages: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2014213617"
    } else {
        "ca-app-pub-6814505709397727/1298024121"
    }
    var collapsibleBannerAdIdAdOnboarding: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2014213617"
    } else {
        "ca-app-pub-6814505709397727/6358779115"
    }

}