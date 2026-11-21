package org.dslul.openboard.translator.pro.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.FragmentCameraBinding
import org.dslul.openboard.objects.CameraMisc
import org.dslul.openboard.objects.CameraMisc.getCameraFace
import org.dslul.openboard.objects.CameraMisc.getFlash
import org.dslul.openboard.objects.CameraMisc.setFlash
import org.dslul.openboard.translator.pro.CameraPermissionActivity
import org.dslul.openboard.translator.pro.LanguageSelectorActivity
import org.dslul.openboard.translator.pro.OCRActivity
import org.dslul.openboard.translator.pro.SettingsActivity
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import java.io.File
import java.util.Locale

class CameraFragment : Fragment() {
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var imageCapture: ImageCapture
    lateinit var binding: FragmentCameraBinding

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                CameraMisc.fileUri = uri
                val intent =
                    Intent(requireContext(), OCRActivity::class.java)
                intent.putExtra(CameraMisc.uri, uri.toString())
                intent.putExtra(CameraMisc.typeGallery, true)
                startActivity(intent)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(layoutInflater, container, false)
        Misc.isItemClicked = true

//        Ads.loadAndShowInterstitial(
//            requireActivity(),
//            Ads.cameraTranslationInt,
//            AdIds.interstitialAdIdAdMobCameraTranslate,
//            object : InterstitialCallBack {
//                override fun onDismiss() {
//                    getCameraPermission()
//                }
//            }
//        )

        // Round the preview's corners with the viewfinder's (android:clipToOutline needs API 31).
        binding.flViewfinder.clipToOutline = true

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        binding.btnFlashOCR.setOnClickListener {
            requireContext().setFlash(!requireContext().getFlash())
            showFlashIcon()
            startCamera()
        }

        binding.btnGallery.setOnClickListener {
            openImagePickerForOCR()
        }

        setSelectedLng()

        binding.ivSwitchLanguages.setOnClickListener {
            if (Misc.getLanguageFrom(requireActivity()) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 150
                rotate.interpolator = LinearInterpolator()
                val image = binding.ivSwitchLanguages
                image.startAnimation(rotate)

                Misc.zoomOutView(binding.llLanguageTo, requireActivity(), 150)
                Misc.zoomOutView(binding.llLanguageFrom, requireActivity(), 150)

                Handler().postDelayed({
                    val temp = Misc.getLanguageFrom(requireActivity())
                    Misc.setLanguageFrom(requireActivity(), Misc.getLanguageTo(requireActivity()))
                    Misc.setLanguageTo(requireActivity(), temp)

                    setSelectedLng()

                    Misc.zoomInView(binding.llLanguageTo, requireActivity(), 150)
                    Misc.zoomInView(binding.llLanguageFrom, requireActivity(), 150)

                }, 150)
            } else {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.please_select_language_you_want_to_translate),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.llLanguageFrom.setOnClickListener {
            val intent = Intent(requireContext(), LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivity(intent)
        }

        binding.llLanguageTo.setOnClickListener {
            startActivity(Intent(requireContext(), LanguageSelectorActivity::class.java))
        }

        getCameraPermission()
        return binding.root
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun startCamera() {
        if (cameraProvider != null) {
            cameraProvider?.unbindAll()
        }
        val cameraFacing = if (requireContext().getCameraFace()) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        showFlashIcon()

        try {
            imageCapture = ImageCapture.Builder().build()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

            cameraProvider = cameraProviderFuture.get()

            val cameraRunnable = Runnable {
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.cameraView.surfaceProvider)
                    }

                imageCapture.flashMode = if (requireContext().getFlash()) {
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
                ContextCompat.getMainExecutor(requireContext())
            )
            cameraProvider?.unbindAll()

            binding.btnCapture.setOnClickListener {
                if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(requireContext(), CameraPermissionActivity::class.java))
                } else {
                    binding.ocrFragmentPB.visibility = View.VISIBLE
                    val file = File(
                        requireContext().externalMediaDirs.firstOrNull(),
                        ".CameraApp - ${System.currentTimeMillis()}.jpg"
                    )
                    val outPut = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        outPut,
                        ContextCompat.getMainExecutor(requireContext()),
                        object : ImageCapture.OnImageCapturedCallback(),
                            ImageCapture.OnImageSavedCallback {
                            override fun onCaptureStarted() = Unit

                            override fun onCaptureProcessProgressed(progress: Int) = Unit

                            override fun onPostviewBitmapAvailable(bitmap: Bitmap) = Unit

                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val imagePath = file.path
                                CameraMisc.fileUri = file.toUri()

                                binding.ocrFragmentPB.visibility =
                                    View.GONE
//                            if (Misc.checkInternetConnection(requireContext())) {
                                val intent =
                                    Intent(requireContext(), OCRActivity::class.java)
                                intent.putExtra(CameraMisc.uri, imagePath)
                                intent.putExtra(CameraMisc.typeGallery, false)
                                startActivity(intent)
//                            } else {
//                                Toast.makeText(
//                                    requireContext(),
//                                    resources.getString(R.string.please_check_your_internet_connection_and_try_again),
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
                            }
                        }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Zap while the flash is on, crossed-out zap while it is off. */
    private fun showFlashIcon() {
        binding.btnFlashOCR.setImageResource(
            if (requireContext().getFlash()) R.drawable.ic_ui_zap else R.drawable.ic_ui_zap_off
        )
    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(requireActivity()) == Misc.defaultLanguage) {
            binding.tvLanguageFrom.text = resources.getString(R.string.detect)
            binding.flagFrom.setImageResource(Misc.getFlag(requireActivity(), "100"))

        } else {
            binding.tvLanguageFrom.text =
                Locale(Misc.getLanguageFrom(requireActivity())).displayName
            binding.flagFrom.setImageResource(
                Misc.getFlag(
                    requireActivity(),
                    Misc.getLanguageFrom(requireActivity())
                )
            )
        }

        binding.tvLanguageTo.text = Locale(Misc.getLanguageTo(requireActivity())).displayName
        binding.flagTo.setImageResource(
            Misc.getFlag(
                requireActivity(),
                Misc.getLanguageTo(requireActivity())
            )
        )
    }


    override fun onResume() {
        super.onResume()
        setSelectedLng()
        startCamera()
        orientationListener.enable()
    }

    override fun onPause() {
        orientationListener.disable()
        super.onPause()
    }

    /**
     * The screen is locked to portrait, so CameraX assumed every photo was taken upright. This
     * keeps the capture's rotation in step with how the phone is actually held, so a landscape
     * photo is saved (and shown in the cropper) the right way up. OCRActivity also turns text
     * upright itself, which covers gallery images.
     */
    private val orientationListener by lazy {
        object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN || !::imageCapture.isInitialized) return
                imageCapture.targetRotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
    }

    private fun openImagePickerForOCR() {
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun getCameraPermission() {
        if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(requireContext(), CameraPermissionActivity::class.java))
        } else {
            startCamera()
        }
    }

}
