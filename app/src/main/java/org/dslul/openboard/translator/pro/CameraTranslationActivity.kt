package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import java.util.*
import java.io.File
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.os.Handler
import android.widget.Toast
import android.content.Intent
import androidx.core.net.toUri
import android.provider.MediaStore
import androidx.camera.core.Preview
import android.view.animation.Animation
import android.content.pm.PackageManager
import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import android.view.animation.RotateAnimation
import org.dslul.openboard.objects.CameraMisc
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.inputmethod.latin.R
import android.view.animation.LinearInterpolator
import androidx.camera.lifecycle.ProcessCameraProvider
import org.dslul.openboard.objects.CameraMisc.getFlash
import org.dslul.openboard.objects.CameraMisc.setFlash
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.objects.CameraMisc.getCameraFace
import androidx.activity.result.contract.ActivityResultContracts
import org.dslul.openboard.inputmethod.latin.databinding.ActivityCameraTranslationBinding

class CameraTranslationActivity : AppCompatActivity() {
    lateinit var binding: ActivityCameraTranslationBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var imageCapture: ImageCapture
    private val cameraPermissionRequest = 100
    private val storageReadPermissionRequest = 101

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { data: ActivityResult ->
            try {
                val fileUri = data!!.data!!
                CameraMisc.fileUri = fileUri.data!!
//                if (Misc.checkInternetConnection(this)) {
                    val intent =
                        Intent(this, OCRActivity::class.java)
                    intent.putExtra(CameraMisc.uri, fileUri.data?.path.toString())
                    intent.putExtra(CameraMisc.typeGallery, true)
                    startActivity(intent)
//                } else {
//                    Toast.makeText(
//                        this,
//                        resources.getString(R.string.please_check_your_internet_connection_and_try_again),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraTranslationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnFlashOCR.setOnClickListener {
            setFlash(!getFlash())
            if (getFlash()) {
                binding.btnFlashOCR
                    .setImageDrawable(resources.getDrawable(R.drawable.flash_on))
            } else {
                binding.btnFlashOCR
                    .setImageDrawable(resources.getDrawable(R.drawable.flash_off))
            }
            startCamera()
        }

        binding.btnGallery.setOnClickListener {
            if (getStorageReadPermission()) {
                val galleryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                getContent.launch(galleryIntent)
            }
        }

        setSelectedLng()

        binding.btnSwitchLngs.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 150
                rotate.interpolator = LinearInterpolator()
                val image = binding.btnSwitchLngs
                image.startAnimation(rotate)

                Misc.zoomOutView(binding.llLngTo, this, 150)
                Misc.zoomOutView(binding.llLngFrom, this, 150)

                Handler().postDelayed({
                    val temp = Misc.getLanguageFrom(this)
                    Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                    Misc.setLanguageTo(this, temp)

                    setSelectedLng()

                    Misc.zoomInView(binding.llLngTo, this, 150)
                    Misc.zoomInView(binding.llLngFrom, this, 150)

                }, 150)
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.please_select_language_you_want_to_translate),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.llLngFrom.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivity(intent)
        }

        binding.llLngTo.setOnClickListener {
            startActivity(Intent(this, LanguageSelectorActivity::class.java))
        }

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun startCamera() {
        if (cameraProvider != null) {
            cameraProvider?.unbindAll()
        }
        val cameraFacing = if (getCameraFace()) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        if (getFlash()) {
            binding.btnFlashOCR
                .setImageDrawable(resources.getDrawable(R.drawable.flash_on))
        } else {
            binding.btnFlashOCR
                .setImageDrawable(resources.getDrawable(R.drawable.flash_off))
        }

        try {
            imageCapture = ImageCapture.Builder().build()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProvider = cameraProviderFuture.get()

            val cameraRunnable = Runnable {
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.cameraView.surfaceProvider)
                    }

                imageCapture.flashMode = if (getFlash()) {
                    1
                } else {
                    0
                }

                cameraProvider?.bindToLifecycle(
                    this,
                    cameraFacing,
                    preview,
                    imageCapture
                )

            }
            cameraProviderFuture.addListener(
                cameraRunnable,
                ContextCompat.getMainExecutor(this)
            )
            cameraProvider?.unbindAll()

            binding.btnCapture.setOnClickListener {
                binding.ocrFragmentPB.visibility = View.VISIBLE
                val file = File(
                    this.externalMediaDirs.firstOrNull(),
                    ".CameraApp - ${System.currentTimeMillis()}.jpg"
                )
                val outPut = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    outPut,
                    ContextCompat.getMainExecutor(this),
                    object : ImageCapture.OnImageCapturedCallback(),
                        ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val imagePath = file.path
                            CameraMisc.fileUri = file.toUri()

                            binding.ocrFragmentPB.visibility =
                                View.GONE
//                            if (Misc.checkInternetConnection(this@CameraTranslationActivity)) {
                                val intent =
                                    Intent(this@CameraTranslationActivity, OCRActivity::class.java)
                                intent.putExtra(CameraMisc.uri, imagePath)
                                intent.putExtra(CameraMisc.typeGallery, false)
                                startActivity(intent)
//                            } else {
//                                Toast.makeText(
//                                    this@CameraTranslationActivity,
//                                    resources.getString(R.string.please_check_your_internet_connection_and_try_again),
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            binding.textViewLngFrom.text = resources.getString(R.string.detect)
            binding.flagFrom.setImageResource(Misc.getFlag(this, "100"))

        } else {
            binding.textViewLngFrom.text = Locale(Misc.getLanguageFrom(this)).displayName
            binding.flagFrom.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))
        }

        binding.textViewLngTo.text = Locale(Misc.getLanguageTo(this)).displayName
        binding.flagTo.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
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

    private fun getCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraPermissionRequest)
        } else {
            startCamera()
            getStorageReadPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequest) {
            try {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                    getStorageReadPermission()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.you_can_not_start_this_module),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    getString(R.string.you_can_not_start_this_module),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}