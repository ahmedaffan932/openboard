package org.dslul.openboard.translator.pro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import kotlinx.android.synthetic.main.activity_translate_interstitial.*
import org.dslul.openboard.inputmethod.latin.R

class TranslateInterstitialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_translate_interstitial)

        InterstitialAd.showInterstitial(this, "am", callback = {
            finish()
        })

        clRoot.setOnClickListener { finish() }
    }
}