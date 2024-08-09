package org.dslul.openboard.translator.pro.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.activity.result.ActivityResult
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
import org.dslul.openboard.translator.pro.GalleryPermissionActivity
import org.dslul.openboard.translator.pro.LanguageSelectorActivity
import org.dslul.openboard.translator.pro.OCRActivity
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
    private var isGalleryPermission = false

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { data: ActivityResult ->
            try {
                val fileUri = data!!.data!!
                CameraMisc.fileUri = fileUri.data!!
                val intent =
                    Intent(requireContext(), OCRActivity::class.java)
                intent.putExtra(CameraMisc.uri, fileUri.data?.path.toString())
                intent.putExtra(CameraMisc.typeGallery, true)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
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

        binding.btnFlashOCR.setOnClickListener {
            requireContext().setFlash(!requireContext().getFlash())
            if (requireContext().getFlash()) {
                binding.btnFlashOCR
                    .setImageDrawable(resources.getDrawable(R.drawable.flash_on))
            } else {
                binding.btnFlashOCR
                    .setImageDrawable(resources.getDrawable(R.drawable.flash_off))
            }
            startCamera()
        }

        binding.btnGallery.setOnClickListener {
            if (checkGalleryPermission()) {
                val galleryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                getContent.launch(galleryIntent)
            } else {
                isGalleryPermission = true
                startActivity(Intent(requireContext(), GalleryPermissionActivity::class.java))
            }
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

        if (requireContext().getFlash()) {
            binding.btnFlashOCR
                .setImageDrawable(resources.getDrawable(R.drawable.flash_on))
        } else {
            binding.btnFlashOCR
                .setImageDrawable(resources.getDrawable(R.drawable.flash_off))
        }

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
        if (isGalleryPermission) {
            isGalleryPermission = false
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

            getContent.launch(galleryIntent)

        } else {
            startCamera()
        }
    }

    private fun checkGalleryPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            requireContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getCameraPermission() {
        if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(requireContext(), CameraPermissionActivity::class.java))
        } else {
            startCamera()
        }
    }

}