package org.dslul.openboard.translator.pro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityAppLanguageSelectorBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads

class AppLanguageSelectorActivity : AppCompatActivity() {
    lateinit var binding: ActivityAppLanguageSelectorBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityAppLanguageSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        Ads.loadAndShowNativeAd(
//            this,
//            AdIds.nativeAdIdAdMobSplash,
//            Ads.appLanguagesSelectorNative,
//            binding.nativeAdFrameLayout,
//            R.layout.admob_native_splash,
//            R.layout.shimmer_native_splash
//        )

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


        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            setAppLanguage(this, findViewById<View>(checkedId).tag.toString())
        }

        binding.btnSave.setOnClickListener {
            if (Misc.isFirstTime(this)) {
                startActivity(Intent(this, OnBoardingActivity::class.java))
            } else {
                startActivity(Intent(this, FragmentsDashboardActivity::class.java))
            }
            finish()
        }

    }
}