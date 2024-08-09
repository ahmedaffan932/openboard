package org.dslul.openboard.translator.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.inputmethod.latin.databinding.ActivityCameraPermissionBinding

class CameraPermissionActivity : AppCompatActivity() {
    lateinit var binding: ActivityCameraPermissionBinding
    private val cameraPermissionRequest = 100

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityCameraPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            Toast.makeText(
                this,
                "Camera permission is required for this feature",
                Toast.LENGTH_SHORT
            ).show()
            onBackPressed()
        }

        binding.btnAllow.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraPermissionRequest)
            } else {
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequest) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required for this feature", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}