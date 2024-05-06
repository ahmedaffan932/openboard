package org.dslul.openboard.translator.pro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager

import kotlinx.android.synthetic.main.activity_translate_interstitial.*
import com.guru.translate.translator.pro.translation.keyboard.translator.R
import org.dslul.openboard.translator.pro.classes.admob.Ads
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

class TranslateInterstitialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_translate_interstitial)

        Ads.showInterstitial(this, Ads.translateInt, object : InterstitialCallBack{
            override fun onDismiss() {
                finish()
            }
        })

        clRoot.setOnClickListener { finish() }
    }
}