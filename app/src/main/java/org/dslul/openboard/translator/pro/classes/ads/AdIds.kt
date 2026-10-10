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

    // App Open Ads - Resume
    var appOpenAdIdResume: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6806702755182088/2568581914"
        },
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6806702755182088/5712696562"
        },
        if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            "ca-app-pub-6806702755182088/1879369129"
        }
    )

    // Native Ads - Shared bottom placement
    var nativeAdIdSharedBottom: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/3213968225"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/8136406273"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/5211849712"
        }
    )

    // Native Ads - Exit
    var nativeAdIdAdMobExit: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/3213968225"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/8136406273"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/5211849712"
        }
    )

    // Native Ads - Translate
    var nativeAdIdAdMobTranslate: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/3213968225"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/8136406273"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/5211849712"
        }
    )

    // Native Ads - Dashboard
    var nativeAdIdDashboard: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/3383762726"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/4718621587"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/8466294905"
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

    // Native Ads - Languages
    var nativeAdIdLanguages: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/1773451556"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/1142896162"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
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
            "ca-app-pub-6806702755182088/7646441361"
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6806702755182088/3487704500"
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6806702755182088/1081033010"
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
            "ca-app-pub-6806702755182088/8821518124"
        },
        if (BuildConfig.DEBUG) {
            TEST_REWARDED_ID
        } else {
            "ca-app-pub-6806702755182088/4690701426"
        },
        if (BuildConfig.DEBUG) {
            TEST_REWARDED_ID
        } else {
            "ca-app-pub-6806702755182088/7125293075"
        }
    )

    // Native Ads - Language Refresh
    var nativeAdIdLangRefresh: Array<String> = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/6940124117"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/2889198463"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/6203651155"
        }
    )

    // Native Ads - Onboarding 1
    var nativeAdIdOB1 = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/2530105812"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/5329553189"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/2264406149"
        }
    )

    // Native Ads - Onboarding 2
    var nativeAdIdOB2 = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/8061634099"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/8535224461"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/7590860803"
        }
    )

    // Native Ads - Onboarding 3
    var nativeAdIdOB3 = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/7949953456"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/9342666412"
        },
        if (BuildConfig.DEBUG) {
            TEST_NATIVE_ID
        } else {
            "ca-app-pub-6806702755182088/9368313039"
        }
    )

    // Interstitial Ads - Onboarding 3
    var interstitialAdIdOB3 = arrayOf(
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6806702755182088/4115986356"
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6806702755182088/5101764962"
        },
        if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_ID
        } else {
            "ca-app-pub-6806702755182088/9151094727"
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
