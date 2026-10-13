package org.dslul.openboard.translator.pro.classes.ads

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.json.JSONArray

object AdIds {

    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/2014213617"
    private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
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
            "ca-app-pub-6065249128764297/9648491319"
        },
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6065249128764297/3662412328"
        },
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6065249128764297/9130361424"
        }
    )

    // App Open Ads - Resume
    var appOpenAdIdResume: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6065249128764297/8723167312"
        },
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6065249128764297/7410085644"
        },
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6065249128764297/9601134710"
        }
    )

    // Native Ads - Shared bottom placement
    var nativeAdIdSharedBottom: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/3885009263"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/9933658271"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/7128540697"
        }
    )

    // Native Ads - Exit
    var nativeAdIdAdMobExit: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/3885009263"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/9933658271"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/7128540697"
        }
    )

    // Native Ads - Translate
    var nativeAdIdAdMobTranslate: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/3885009263"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/9933658271"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/7128540697"
        }
    )

    // Native Ads - Dashboard
    var nativeAdIdDashboard: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/4762976666"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/7824254278"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/8856555333"
        }
    )

    // Native Ads - Splash
    var nativeAdIdAdMobSplash: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/9988147527"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/4261178126"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/6088520255"
        }
    )

    // Native Ads - Languages
    var nativeAdIdLanguages: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/1714905835"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/3470840638"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/6775660825"
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

    // Interstitial Ads - Camera Translate
    var interstitialAdIdAdMobCameraTranslate: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            ""
        }
    )

    // Interstitial Ads - Exit
    var interstitialAdIdAdMobExit: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            ""
        }
    )

    // Interstitial Ads - Every 6th Click
    var interstitialAdIdEverySixthClick: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6065249128764297/3501865886"
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6065249128764297/5096264563"
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6065249128764297/3783182899"
        }
    )

    // Rewarded Interstitial
    var rewardedInterstitialAdIdAdMob: String = if (BuildConfig.DEBUG) {
        TEST_REWARDED_INTERSTITIAL_ID
    } else {
        ""
    }

    // Rewarded Ads - Unlock Premium Features
    var rewardedAdIdUnlockPremium: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_REWARDED_ID
        } else {
            "ca-app-pub-6065249128764297/5994413265"
        },
        if (BuildConfig.DEBUG) {
            TEST_REWARDED_ID
        } else {
            "ca-app-pub-6065249128764297/1067274236"
        },
        if (BuildConfig.DEBUG) {
            TEST_REWARDED_ID
        } else {
            "ca-app-pub-6065249128764297/5908537371"
        }
    )

    // Native Ads - Language Refresh
    var nativeAdIdLangRefresh: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/5518419478"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/4212156791"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/1523334148"
        }
    )

    // Native Ads - Onboarding 1
    var nativeAdIdOB1 = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/2627612383"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/1314530715"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/5661889707"
        }
    )

    // Native Ads - Onboarding 2
    var nativeAdIdOB2 = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/1774615587"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/8467271588"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/9207909127"
        }
    )

    // Native Ads - Onboarding 3
    var nativeAdIdOB3 = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/1015303344"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/1039354076"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6065249128764297/2080973009"
        }
    )

    // Interstitial Ads - Onboarding 3
    var interstitialAdIdOB3 = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6065249128764297/6100109068"
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6065249128764297/3955582449"
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6065249128764297/5389662627"
        }
    )

    fun applyRemoteConfig(remoteConfig: FirebaseRemoteConfig) {
        if (BuildConfig.DEBUG) return

        remoteConfig.applyAdIds("splash_app_open_ad_ids") {
            appOpenAdIdSplash = it
        }
        remoteConfig.applyAdIds("resume_app_open_ad_ids") {
            appOpenAdIdResume = it
        }
        remoteConfig.applyAdIds("splash_interstitial_ad_ids") {
            interstitialAdIdAdMobSplash = it
        }
        remoteConfig.applyAdIds("splash_native_ad_ids") {
            nativeAdIdAdMobSplash = it
        }
        remoteConfig.applyAdIds("language_native_ad_ids") {
            nativeAdIdLanguages = it
        }
        remoteConfig.applyAdIds("language_refresh_native_ad_ids") {
            nativeAdIdLangRefresh = it
        }
        remoteConfig.applyAdIds("onboarding_native_1_ad_ids") {
            nativeAdIdOB1 = it
        }
        remoteConfig.applyAdIds("onboarding_native_2_ad_ids") {
            nativeAdIdOB2 = it
        }
        remoteConfig.applyAdIds("onboarding_native_3_ad_ids") {
            nativeAdIdOB3 = it
        }
        remoteConfig.applyAdIds("onboarding_interstitial_ad_ids") {
            interstitialAdIdOB3 = it
        }
        remoteConfig.applyAdIds("shared_bottom_native_ad_ids") {
            nativeAdIdSharedBottom = it
            nativeAdIdAdMobExit = it
            nativeAdIdAdMobTranslate = it
        }
        remoteConfig.applyAdIds("dashboard_native_ad_ids") {
            nativeAdIdDashboard = it
        }
        remoteConfig.applyAdIds("translate_native_ad_ids") {
            nativeAdIdAdMobTranslate = it
        }
        remoteConfig.applyAdIds("translate_interstitial_ad_ids") {
            interstitialAdIdAdMobTranslate = it
        }
        remoteConfig.applyAdIds("every_sixth_click_interstitial_ad_ids") {
            interstitialAdIdEverySixthClick = it
        }
        remoteConfig.applyAdIds("phrase_interstitial_ad_ids") {
            interstitialAdIdAdMobPhrases = it
        }
        remoteConfig.applyAdIds("camera_translation_interstitial_ad_ids") {
            interstitialAdIdAdMobCameraTranslate = it
        }
        remoteConfig.applyAdIds("exit_native_ad_ids") {
            nativeAdIdAdMobExit = it
        }
        remoteConfig.applyAdIds("exit_interstitial_ad_ids") {
            interstitialAdIdAdMobExit = it
        }
        remoteConfig.applyAdIds("unlock_premium_rewarded_ad_ids") {
            rewardedAdIdUnlockPremium = it
        }
        remoteConfig.applySingleAdId("rewarded_interstitial_ad_id") {
            rewardedInterstitialAdIdAdMob = it
        }
        remoteConfig.applySingleAdId("splash_banner_ad_id") {
            bannerAdIdAdSplash = it
        }
        remoteConfig.applySingleAdId("collapsible_banner_ad_id") {
            collapsibleBannerAdIdAd = it
        }
    }

    private fun FirebaseRemoteConfig.applyAdIds(key: String, applyIds: (Array<String>) -> Unit) {
        val parsedIds = parseAdIds(all[key]?.asString().orEmpty())
        if (parsedIds.isNotEmpty()) {
            applyIds(parsedIds)
        }
    }

    private fun FirebaseRemoteConfig.applySingleAdId(key: String, applyId: (String) -> Unit) {
        val parsedIds = parseAdIds(all[key]?.asString().orEmpty())
        parsedIds.firstOrNull()?.let(applyId)
    }

    private fun parseAdIds(rawValue: String): Array<String> {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return emptyArray()

        return if (trimmed.startsWith("[")) {
            val array = JSONArray(trimmed)
            Array(array.length()) { index -> array.optString(index).trim() }
        } else {
            trimmed.split(",").map { it.trim() }.toTypedArray()
        }.filter { it.isNotEmpty() }.toTypedArray()
    }
}
