package org.dslul.openboard.translator.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.inputmethod.latin.databinding.ActivityGalleryPermissionBinding

class GalleryPermissionActivity : AppCompatActivity() {
    lateinit var binding: ActivityGalleryPermissionBinding
    private val storageReadPermissionRequest = 101
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            Toast.makeText(this, "Gallery permission is required to use this feature", Toast.LENGTH_SHORT).show()
            onBackPressed()
        }

        binding.btnAllow.setOnClickListener {
            if (getStorageReadPermission()) {
                onBackPressed()
            }
        }
    }

    private fun getStorageReadPermission(): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        storageReadPermissionRequest
                    )
                    return false
                }
            } else {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        storageReadPermissionRequest
                    )
                    return false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == storageReadPermissionRequest){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                onBackPressed()
            }else{
                Toast.makeText(this, "Gallery permission is required to use this feature", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
}