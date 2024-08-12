package org.dslul.openboard.translator.pro

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityAppLanguageSelectorBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AppOpenAdManager
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

class AppLanguageSelectorActivity : AppCompatActivity() {
    lateinit var binding: ActivityAppLanguageSelectorBinding
    private var isLanguageSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityAppLanguageSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Ads.loadAndShowNativeAd(
            this,
            AdIds.nativeAdIdAdMobSplash,
            Ads.appLanguagesSelectorNative,
            binding.nativeAdFrameLayout,
            R.layout.shimmer_native_splash
        )

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


        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            binding.btnSave.visibility = View.VISIBLE
            setAppLanguage(this, findViewById<View>(checkedId).tag.toString())
        }

        binding.btnSave.setOnClickListener {
            AppOpenAdManager.showIfAvailable(this, Ads.isSplashAppOpenAdEnabled, object :
                InterstitialCallBack {
                override fun onDismiss() {
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
            })
        }

    }
}