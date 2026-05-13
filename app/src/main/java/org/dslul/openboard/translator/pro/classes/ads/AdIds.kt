package org.dslul.openboard.translator.pro.classes.ads

import org.dslul.openboard.inputmethod.latin.BuildConfig

object AdIds {

    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/2014213617"
    private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/5224354917"

    // Banner Ads
    var collapsibleBannerAdIdAd: String = if (BuildConfig.DEBUG) {
        TEST_BANNER_ID
    } else {
        ""
    }

    var bannerAdIdAdSplash: String = if (BuildConfig.DEBUG) {
        TEST_BANNER_ID
    } else {
        ""
    }

    // App Open Ads - Splash
    var appOpenAdIdSplash: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6806702755182088/6507826922"
        },
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6806702755182088/4867442196"
        },
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6806702755182088/1334467852"
        }
    )

    // Native Ads - Exit
    var nativeAdIdAdMobExit: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            ""
        }
    )

    // Native Ads - Translate
    var nativeAdIdAdMobTranslate: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            ""
        }
    )

    // Native Ads - Splash
    var nativeAdIdAdMobSplash: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/9757859146"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/8444777472"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/1959260170"
        }
    )

    var nativeAdIdLanguages = arrayOf(
        if (BuildConfig.DEBUG) {
            "ca-app-pub-3940256099942544/2247696110"
        } else {
            "ca-app-pub-6806702755182088/1773451556"
        },
        if (BuildConfig.DEBUG) {
            "ca-app-pub-3940256099942544/2247696110"
        } else {
            "ca-app-pub-6806702755182088/1142896162"
        },
        if (BuildConfig.DEBUG) {
            "ca-app-pub-3940256099942544/2247696110"
        } else {
            "ca-app-pub-6806702755182088/3080770150"
        }
    )

    // Interstitial Ads - Splash
    var interstitialAdIdAdMobSplash: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            ""
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            ""
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            ""
        }
    )

    // Interstitial Ads - Phrases
    var interstitialAdIdAdMobPhrases: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            ""
        }
    )

    // Interstitial Ads - Translate
    var interstitialAdIdAdMobTranslate: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            ""
        }
    )

    // Rewarded Interstitial
    var rewardedInterstitialAdIdAdMob: String = if (BuildConfig.DEBUG) {
        TEST_REWARDED_INTERSTITIAL_ID
    } else {
        ""
    }
}