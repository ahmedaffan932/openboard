package org.dslul.openboard.translator.pro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.dslul.openboard.inputmethod.latin.databinding.ActivityOnResumeBinding
import org.dslul.openboard.translator.pro.classes.ads.admob.AppOpenAdManager
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack

class OnResumeActivity : AppCompatActivity() {
    lateinit var binding: ActivityOnResumeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppOpenAdManager.showIfAvailable(this, true, object : InterstitialCallBack {
            override fun onDismiss() {
                finish()
            }
        })

        binding.root.setOnClickListener {
            finish()
        }
    }
}