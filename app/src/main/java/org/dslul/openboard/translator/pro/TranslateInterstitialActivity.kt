package org.dslul.openboard.translator.pro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityTranslateInterstitialBinding
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

class TranslateInterstitialActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTranslateInterstitialBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityTranslateInterstitialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Ads.showInterstitial(this, Ads.translateInt, object : InterstitialCallBack{
            override fun onDismiss() {
                finish()
            }
        })

        binding.clRoot.setOnClickListener { finish() }
    }
}