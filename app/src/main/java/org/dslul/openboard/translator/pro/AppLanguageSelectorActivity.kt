package org.dslul.openboard.translator.pro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.translatorguru.ads.admob.LoadAdCallBack
import org.dslul.openboard.inputmethod.latin.databinding.ActivityAppLanguageSelectorBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobNativeAds

class AppLanguageSelectorActivity : AppCompatActivity() {
    lateinit var binding: ActivityAppLanguageSelectorBinding
    private var isLanguageSelected = false
    private var isLanguageRefreshNativeShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityAppLanguageSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLanguageNativeOnOpen()

        if (Misc.showNextButtonOnLanguageScreen || !Misc.isFirstTime(this)) {
            binding.btnSave.visibility = View.VISIBLE
            when (Misc.getAppLanguage(this)) {
                "pt" -> {
                    binding.rbPortuguese.isChecked = true
                }

                "ko" -> {
                    binding.rbKorean.isChecked = true
                }

                "it" -> {
                    binding.rbItalian.isChecked = true
                }

                "ru" -> {
                    binding.rbRussian.isChecked = true
                }

                "fr" -> {
                    binding.rbFrench.isChecked = true
                }

                "de" -> {
                    binding.rbGerman.isChecked = true
                }

                "es" -> {
                    binding.rbSpanish.isChecked = true
                }

                "hi" -> {
                    binding.rbHindi.isChecked = true
                }

                else -> {
                    binding.rbEnglish.isChecked = true
                }
            }
        }


        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == View.NO_ID) return@setOnCheckedChangeListener

            binding.btnSave.visibility = View.VISIBLE
            setAppLanguage(this, findViewById<View>(checkedId).tag.toString())
            showLanguageRefreshNativeIfNeeded()
        }

        binding.btnSave.setOnClickListener {
            if (Misc.isFirstTime(this@AppLanguageSelectorActivity)) {
                startActivity(
                    Intent(
                        this@AppLanguageSelectorActivity,
                        OnBoardingActivity::class.java
                    )
                )
            } else {
                if (Misc.isProScreenEnabled) {
                    startActivity(
                        Intent(
                            this@AppLanguageSelectorActivity,
                            PremiumScreenActivity::class.java
                        ).putExtra(
                            Misc.data,
                            Misc.data
                        )
                    )
                } else {
                    startActivity(
                        Intent(
                            this@AppLanguageSelectorActivity,
                            FragmentsDashboardActivity::class.java
                        )
                    )
                }
            }
            finish()
        }

    }

    private fun showLanguageNativeOnOpen() {
        if (!Ads.appLanguagesSelectorNative.contains("am")) {
            binding.nativeAdFrameLayout.removeAllViews()
            binding.nativeAdFrameLayout.visibility = View.GONE
            Log.d(Misc.logKey, "Language native skipped on activity open.")
            return
        }

        if (!Misc.checkInternetConnection(this) || Misc.getPurchasedStatus(this)) {
            binding.nativeAdFrameLayout.removeAllViews()
            binding.nativeAdFrameLayout.visibility = View.GONE
            Log.d(Misc.logKey, "Language native skipped on activity open.")
            return
        }

        Log.d(Misc.logKey, "Language native requested on activity open.")

        if (AdmobNativeAds.isNativeAdAvailableFor(AdIds.nativeAdIdLanguages)) {
            AdmobNativeAds.showNativeAd(
                context = this,
                remoteKey = Ads.appLanguagesSelectorNative,
                amLayout = binding.nativeAdFrameLayout,
                nextPreloadAdIds = AdIds.nativeAdIdLangRefresh
            )
            return
        }

        AdmobNativeAds.loadAdmobNative(
            context = this,
            adIds = AdIds.nativeAdIdLanguages,
            remoteKey = Ads.appLanguagesSelectorNative,
            frameLayout = binding.nativeAdFrameLayout,
            callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    Log.d(Misc.logKey, "Language native loaded on activity open.")

                    AdmobNativeAds.showNativeAd(
                        context = this@AppLanguageSelectorActivity,
                        remoteKey = Ads.appLanguagesSelectorNative,
                        amLayout = binding.nativeAdFrameLayout,
                        nextPreloadAdIds = AdIds.nativeAdIdLangRefresh
                    )
                }

                override fun onFailed() {
                    Log.d(Misc.logKey, "Language native failed on activity open.")
                }
            }
        )
    }

    private fun showLanguageRefreshNativeIfNeeded() {
        if (!isLanguageRefreshNativeShown) {
            isLanguageRefreshNativeShown = true
            Log.d(Misc.logKey, "Language refresh native requested on first language tap.")

            if (AdmobNativeAds.isNativeAdAvailableFor(AdIds.nativeAdIdLangRefresh)) {
                AdmobNativeAds.showNativeAd(
                    context = this,
                    remoteKey = Ads.appLanguagesSelectorNative,
                    amLayout = binding.nativeAdFrameLayout,
                    nextPreloadAdIds = if (Misc.isFirstTime(this)) {
                        AdIds.nativeAdIdOB1
                    } else {
                        null
                    }
                )
            } else {
                Log.d(Misc.logKey, "Language refresh native not available on first language tap.")
                preloadFirstOnboardingNativeIfNeeded()
            }
        } else {
            Log.d(Misc.logKey, "Language refresh native already shown, skipping.")
        }
    }

    private fun preloadFirstOnboardingNativeIfNeeded() {
        if (!Misc.isFirstTime(this)) return

        if (!Ads.onBoardingNative.contains("native")) {
            Log.d(Misc.logKey, "Onboarding native preload skipped from language screen.")
            return
        }

        if (!Misc.checkInternetConnection(this) || Misc.getPurchasedStatus(this)) {
            Log.d(Misc.logKey, "Onboarding native preload skipped from language screen.")
            return
        }

        Log.d(Misc.logKey, "Onboarding native preload started from language screen.")

        AdmobNativeAds.loadAdmobNative(
            context = this,
            adIds = AdIds.nativeAdIdOB1,
            callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    Log.d(Misc.logKey, "Onboarding native preloaded from language screen.")
                }

                override fun onFailed() {
                    Log.d(Misc.logKey, "Onboarding native failed to preload from language screen.")
                }
            }
        )
    }
}
