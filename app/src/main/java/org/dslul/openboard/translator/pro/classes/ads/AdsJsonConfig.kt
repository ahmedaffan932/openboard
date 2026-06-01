package org.dslul.openboard.translator.pro.classes.ads

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.dslul.openboard.inputmethod.latin.BuildConfig
import org.dslul.openboard.translator.pro.classes.Misc
import org.json.JSONArray
import org.json.JSONObject

object AdsJsonConfig {
    const val REMOTE_CONFIG_KEY = "translator_pro_ads_config_json"
    private const val FALLBACK_REMOTE_CONFIG_KEY = "ads_config_json"

    fun remoteJson(remoteConfig: FirebaseRemoteConfig): String {
        return remoteConfig.getString(REMOTE_CONFIG_KEY)
            .ifBlank { remoteConfig.getString(FALLBACK_REMOTE_CONFIG_KEY) }
            .trim()
    }

    fun apply(rawJson: String): Boolean {
        if (rawJson.isBlank()) return false

        return try {
            applyConfig(JSONObject(rawJson))
            true
        } catch (exception: Exception) {
            Log.e(Misc.logKey, "Ads JSON config failed: ${exception.message}")
            false
        }
    }

    private fun applyConfig(root: JSONObject) {
        applyGlobal(root.optJSONObject("global"))
        applySplash(root.optJSONObject("splash_screen"))
        applyLanguageScreen(root.optJSONObject("language_screen"))
        applyNativePlacement(
            config = root.optJSONObject("onboarding_1_native"),
            placementKey = "onboarding_native",
            defaultLayout = "small_hctr_native_btn_up"
        ) { AdIds.nativeAdIdOB1 = it }
        root.optJSONObject("onboarding_2_native")?.let { config ->
            Ads.setFloorFor("onboarding_native", config.floor())
            applyAdIds(config) { AdIds.nativeAdIdOB2 = it }
        }
        root.optJSONObject("onboarding_3_native")?.let { config ->
            Ads.setFloorFor("onboarding_native", config.floor())
            applyAdIds(config) { AdIds.nativeAdIdOB3 = it }
        }
        applyInterstitialPlacement(
            config = root.optJSONObject("started_interstitial"),
            placementKey = "onboarding_interstitial",
            applyRemote = { Ads.dashboardInt = it }
        ) { AdIds.interstitialAdIdOB3 = it }
        applyNativePlacement(
            config = root.optJSONObject("home_screen"),
            placementKey = "dashboard_native",
            defaultLayout = "large_hctr",
            applyRemote = { Ads.dashboardNative = it }
        ) { AdIds.nativeAdIdDashboard = it }
        applyNativePlacement(
            config = root.optJSONObject("shared_bottom_native"),
            placementKey = "shared_bottom_native",
            defaultLayout = "large_hctr",
            applyRemote = { Ads.sharedBottomNative = it }
        ) { ids ->
            AdIds.nativeAdIdSharedBottom = ids
            AdIds.nativeAdIdAdMobExit = ids
            AdIds.nativeAdIdAdMobTranslate = ids
        }
        applyTranslateScreen(root.optJSONObject("translate_screen"))
        applyPhrasebook(root.optJSONObject("phrasebook"))
        applyNativePlacement(
            config = root.optJSONObject("exit_save_native"),
            placementKey = "exit_native",
            defaultLayout = "large_hctr",
            applyRemote = { Ads.exitNative = it }
        ) { AdIds.nativeAdIdAdMobExit = it }
        applyCameraTranslation(root.optJSONObject("camera_translation"))
        applyNativePlacement(
            config = root.optJSONObject("premium_screen")?.optJSONObject("native"),
            placementKey = "premium_native",
            defaultLayout = "large_hctr",
            applyRemote = { Ads.premiumNative = it }
        )
        applyAppInterstitial(root.optJSONObject("app_interstitial"))
        applyRewarded(root.optJSONObject("rewarded"))
        applyAppBanner(root.optJSONObject("app_banner"))
    }

    private fun applyGlobal(config: JSONObject?) {
        config ?: return
        Ads.isNativeAdPreload = config.optBoolean("native_preload_enabled", Ads.isNativeAdPreload)
        Ads.isIntPreLoad = config.optBoolean("interstitial_preload_enabled", Ads.isIntPreLoad)
        Ads.interstitialClickFrequency = config.optInt(
            "interstitial_click_frequency",
            Ads.interstitialClickFrequency
        )
    }

    private fun applySplash(config: JSONObject?) {
        config ?: return
        Ads.splashAdTimeoutMs = config.optLong("timeout", Ads.splashAdTimeoutMs)
        Ads.splashAppOpenWaitMs = config.optLong("time", Ads.splashAppOpenWaitMs)
        Misc.isProScreenEnabled = config.optBoolean("splash_pro_home", Misc.isProScreenEnabled)

        applyNativePlacement(
            config = config.optJSONObject("native"),
            placementKey = "splash_native",
            defaultLayout = "native_splash",
            applyRemote = { Ads.splashNative = it }
        ) { AdIds.nativeAdIdAdMobSplash = it }

        config.optJSONObject("app_open")?.let { appOpen ->
            Ads.isSplashAppOpenAdEnabled = appOpen.optBoolean(
                "is_enabled",
                Ads.isSplashAppOpenAdEnabled
            )
            Ads.setFloorFor("splash_app_open", appOpen.floor())
            applyAdIds(appOpen) { AdIds.appOpenAdIdSplash = it }
        }

        applyInterstitialPlacement(
            config = config.optJSONObject("interstitial"),
            placementKey = "splash_interstitial",
            applyRemote = { Ads.splashInt = it }
        ) { AdIds.interstitialAdIdAdMobSplash = it }
    }

    private fun applyLanguageScreen(config: JSONObject?) {
        config ?: return
        applyNativePlacement(
            config = config.optJSONObject("native_before_selection"),
            placementKey = "language_native",
            defaultLayout = "large_hctr",
            applyRemote = { Ads.appLanguagesSelectorNative = it }
        ) { AdIds.nativeAdIdLanguages = it }
        applyNativePlacement(
            config = config.optJSONObject("native_after_selection"),
            placementKey = "language_refresh_native",
            defaultLayout = "large_hctr",
            applyRemote = { Ads.appLanguagesRefreshNative = it }
        ) { AdIds.nativeAdIdLangRefresh = it }
    }

    private fun applyTranslateScreen(config: JSONObject?) {
        config ?: return
        applyNativePlacement(
            config = config.optJSONObject("native"),
            placementKey = "translate_native",
            defaultLayout = "large_hctr_bottom",
            applyRemote = { Ads.translateNative = it }
        ) { AdIds.nativeAdIdAdMobTranslate = it }
        applyInterstitialPlacement(
            config = config.optJSONObject("interstitial"),
            placementKey = "translate_interstitial",
            applyRemote = { Ads.translateInt = it }
        ) { AdIds.interstitialAdIdAdMobTranslate = it }
    }

    private fun applyPhrasebook(config: JSONObject?) {
        config ?: return
        applyNativePlacement(
            config = config.optJSONObject("native"),
            placementKey = "shared_bottom_native",
            defaultLayout = "large_hctr",
            applyRemote = { Ads.sharedBottomNative = it }
        ) { AdIds.nativeAdIdSharedBottom = it }
        applyInterstitialPlacement(
            config = config.optJSONObject("interstitial"),
            placementKey = "phrase_interstitial",
            applyRemote = { Ads.phraseInt = it }
        ) { AdIds.interstitialAdIdAdMobPhrases = it }
    }

    private fun applyCameraTranslation(config: JSONObject?) {
        config ?: return
        applyInterstitialPlacement(
            config = config.optJSONObject("interstitial"),
            placementKey = "camera_translation_interstitial",
            applyRemote = { Ads.cameraTranslationInt = it }
        ) { AdIds.interstitialAdIdAdMobCameraTranslate = it }
        config.optJSONObject("rewarded")?.let { rewarded ->
            Ads.cameraTranslationRewardedAd = rewarded.remoteKey()
            Ads.setFloorFor("camera_translation_rewarded", rewarded.floor())
        }
    }

    private fun applyAppInterstitial(config: JSONObject?) {
        config ?: return
        Ads.everySixthClickInterstitial = config.remoteKey()
        Ads.setFloorFor("every_sixth_click_interstitial", config.floor())
        Ads.interstitialClickFrequency = config.optInt(
            "after_start_count",
            Ads.interstitialClickFrequency
        )
        applyAdIds(config) { AdIds.interstitialAdIdEverySixthClick = it }
    }

    private fun applyRewarded(config: JSONObject?) {
        config ?: return
        Ads.unlockPremiumRewardedAd = config.remoteKey()
        Ads.setFloorFor("unlock_premium_rewarded", config.floor())
        applyAdIds(config) { AdIds.rewardedAdIdUnlockPremium = it }
    }

    private fun applyAppBanner(config: JSONObject?) {
        config ?: return
        Ads.dashboardBanner = config.remoteKey()
        Ads.setFloorFor("dashboard_banner", config.floor())
        applySingleAdId(config) { adId ->
            AdIds.collapsibleBannerAdIdAd = adId
            AdIds.bannerAdIdAdSplash = adId
        }
    }

    private fun applyNativePlacement(
        config: JSONObject?,
        placementKey: String,
        defaultLayout: String,
        applyRemote: (String) -> Unit = {},
        applyIds: ((Array<String>) -> Unit)? = null
    ) {
        config ?: return
        applyRemote(config.nativeRemoteKey(defaultLayout))
        Ads.setFloorFor(placementKey, config.floor())
        applyIds?.let { applyAdIds(config, it) }
    }

    private fun applyInterstitialPlacement(
        config: JSONObject?,
        placementKey: String,
        applyRemote: (String) -> Unit,
        applyIds: (Array<String>) -> Unit
    ) {
        config ?: return
        applyRemote(config.remoteKey())
        Ads.setFloorFor(placementKey, config.floor())
        applyAdIds(config, applyIds)
    }

    private fun JSONObject.nativeRemoteKey(defaultLayout: String): String {
        if (!optBoolean("is_enabled", true)) return "off"
        val layout = optString("native_layout", defaultLayout).ifBlank { defaultLayout }
        return "am_$layout"
    }

    private fun JSONObject.remoteKey(): String {
        return if (optBoolean("is_enabled", true)) "am" else "off"
    }

    private fun JSONObject.floor(): Double {
        return optDouble("floor", 0.0)
    }

    private fun applyAdIds(config: JSONObject, applyIds: (Array<String>) -> Unit) {
        if (BuildConfig.DEBUG) return
        val adIds = config.adIds()
        if (adIds.isNotEmpty()) {
            applyIds(adIds)
        }
    }

    private fun applySingleAdId(config: JSONObject, applyId: (String) -> Unit) {
        if (BuildConfig.DEBUG) return
        config.adIds().firstOrNull()?.let(applyId)
    }

    private fun JSONObject.adIds(): Array<String> {
        val jsonArray = optJSONArray("ad_unit_ids") ?: JSONArray()
        return Array(jsonArray.length()) { index ->
            jsonArray.optString(index).trim()
        }.filter { it.isNotEmpty() }.toTypedArray()
    }
}
