package org.dslul.openboard.translator.pro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.inputmethod.latin.databinding.ActivityGalleryPermissionBinding

class GalleryPermissionActivity : AppCompatActivity() {
    lateinit var binding: ActivityGalleryPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnAllow.setOnClickListener {
            onBackPressed()
        }
    }
}
