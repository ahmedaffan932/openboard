package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityOcrBinding
import org.dslul.openboard.objects.CameraMisc
import org.dslul.openboard.translator.pro.adaptor.OCRResultAdapter
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.MiscTranslate
import org.dslul.openboard.translator.pro.classes.OcrParagraphs
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import org.dslul.openboard.translator.pro.interfaces.TranslationInterface
import java.util.*
import kotlin.collections.ArrayList

class OCRActivity : AppCompatActivity() {
    /** One entry per recognised paragraph (OCR text block), in reading order. */
    private var arrText: ArrayList<String> = java.util.ArrayList<String>()
    private var arrTranslation: ArrayList<String> = java.util.ArrayList<String>()

    /** Bumped by every new translation run; callbacks of an older run are ignored. */
    private var translationRun = 0
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var binding: ActivityOcrBinding
    private var isRewardedAdShown = false
    private var isLanguageSelection = false
    private var bitmap: Bitmap? = null
    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                handleSelectedImageForOCR(uri)
            } ?: run {
                Toast.makeText(this, R.string.unable_to_crop, Toast.LENGTH_SHORT).show()
                finish()
            }

        } else {
            Toast.makeText(this, R.string.unable_to_crop, Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width < maxSize) {
            return bitmap
            Log.d("resizedBipmapCheck", "resizeBitmap: ")
        }
        // Determine the scaling factor
        val scaleFactor = maxOf(width.toFloat() / maxSize, height.toFloat() / maxSize)

        return if (scaleFactor > 1) {
            val newWidth = (width / scaleFactor).toInt()
            val newHeight = (height / scaleFactor).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val policy: StrictMode.ThreadPolicy =
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetResult))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // The photo stays inside the viewfinder card's rounded corners.
        binding.cvViewfinder.clipToOutline = true

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.progressBar.setOnClickListener { }

        binding.imageViewOcr.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }

        init()

        binding.ivSwitchLanguages.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 100
                rotate.interpolator = LinearInterpolator()

                val image = binding.ivSwitchLanguages
                image.startAnimation(rotate)

                val temp = Misc.getLanguageFrom(this)
                Misc.setLanguageFrom(
                    this,
                    Misc.getLanguageTo(this)
                )
                Misc.setLanguageTo(this, temp)

                if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
                    binding.tvLanguageFrom.text = resources.getString(R.string.detect)
                    binding.flagFrom.setImageResource(Misc.getFlag(this, "100"))
                } else {
                    binding.tvLanguageFrom.text =
                        Locale(
                            Misc.getLanguageFrom(this)
                        ).displayName
                    binding.flagFrom.setImageResource(
                        Misc.getFlag(
                            this,
                            Misc.getLanguageFrom(this)
                        )
                    )
                }
                binding.tvLanguageTo.text =
                    Locale(
                        Misc.getLanguageTo(this)
                    ).displayName
                binding.flagTo.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
            }
        }

        binding.llLanguageFrom.setOnClickListener {
            isLanguageSelection = true
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivity(intent)
        }

        binding.llLanguageTo.setOnClickListener {
            isLanguageSelection = true
            startActivity(Intent(this, LanguageSelectorActivity::class.java))
        }
    }


    private fun init() {
        try {
            // The pinned notification's File tile also sends the image as the intent's data, which
            // still holds after the process was killed and this screen restored.
            val selectedUri = (CameraMisc.fileUri ?: intent.data)!!

            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = selectedUri,
                    cropImageOptions = CropImageOptions(
                    ).apply {
                        activityMenuIconColor = Color.BLACK
                        activityMenuTextColor = Color.BLACK
                        toolbarTintColor = Color.BLACK
                        toolbarBackButtonColor = Color.BLACK
                        toolbarTitleColor = Color.BLACK
                        cropMenuCropButtonTitle = getString(R.string.ocr_crop_done)
                    }
                )
            )

            bitmap = loadBitmapFromUri(selectedUri)
            binding.imageViewOcr.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Sorry, some error occurred, Please try again.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun handleSelectedImageForOCR(uri: Uri) {
        bitmap = loadBitmapFromUri(uri)
        binding.imageViewOcr.setImageBitmap(resizeBitmap(bitmap!!, 1024))
        ocr(bitmap!!)
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
            }
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    /** The recogniser for the From language (Latin also covers "Detect"). */
    private fun recognizer() = when (Misc.getLanguageFrom(this)) {
        TranslateLanguage.CHINESE ->
            TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        TranslateLanguage.KOREAN ->
            TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        TranslateLanguage.JAPANESE ->
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private fun recognize(bitmap: Bitmap, onResult: (Text) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer().process(image).addOnSuccessListener(onResult).addOnFailureListener {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener(onResult)
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    Toast.makeText(this, getString(R.string.sorry_no_text_found), Toast.LENGTH_SHORT)
                        .show()
                    binding.progressBar.visibility = View.GONE
                }
        }
    }

    /**
     * Recognises the text. A photo taken sideways (landscape) or upside down is turned upright
     * first: the recogniser still reads sideways lines, but orders them by position in the
     * sideways image, which made a landscape page come out from its last line to its first.
     */
    private fun ocr(bitmap: Bitmap) {
        recognize(bitmap) { visionText ->
            val rotation = textRotation(visionText)
            if (rotation == 0) {
                drawBoundaries(bitmap, visionText)
                return@recognize
            }
            Log.d(Misc.logKey, "OCR: text is rotated $rotation degrees, turning it upright")
            val upright = rotated(bitmap, -rotation)
            recognize(upright) { uprightText ->
                if (textRotation(uprightText) == 0) {
                    this.bitmap = upright
                    drawBoundaries(upright, uprightText)
                } else {
                    // Still sideways: the angle was measured the other way round.
                    val other = rotated(bitmap, rotation)
                    this.bitmap = other
                    recognize(other) { otherText -> drawBoundaries(other, otherText) }
                }
            }
        }
    }

    /** The rotation of most of the recognised text: 0, 90, 180 or 270 degrees. */
    private fun textRotation(visionText: Text): Int {
        val lines = visionText.textBlocks.flatMap { it.lines }
        return OcrParagraphs.dominantRotation(lines.map { it.angle }, lines.map { it.text.length })
    }

    private fun rotated(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    @SuppressLint("SetTextI18n")
    private fun drawBoundaries(bitmap: Bitmap, visionText: Text) {
        // Accent boxes, thick enough to see once the photo is scaled down to the card.
        val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        rectPaint.color = ContextCompat.getColor(this, R.color.home_accent)
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = maxOf(2f, maxOf(bitmap.width, bitmap.height) / 300f)
        val boxCorner = rectPaint.strokeWidth * 3

        val tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(tempBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, rectPaint)

        // One box and one row per paragraph. Each line used to be its own row and its own
        // translation request, so sentences that wrap were translated in pieces.
        val blocks = visionText.textBlocks.filter { block -> block.lines.any { it.text.isNotBlank() } }
        for (block in blocks) {
            block.boundingBox?.let { canvas.drawRoundRect(RectF(it), boxCorner, boxCorner, rectPaint) }
        }
        binding.imageViewOcr.setImageDrawable(BitmapDrawable(resources, tempBitmap))

        // Chinese and Japanese do not put spaces between words.
        val separator = when (Misc.getLanguageFrom(this)) {
            TranslateLanguage.CHINESE, TranslateLanguage.JAPANESE -> ""
            else -> " "
        }
        arrText.clear()
        arrText.addAll(OcrParagraphs.build(blocks.map { block -> block.lines.map { it.text } }, separator))

        if (arrText.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, getString(R.string.sorry_no_text_found), Toast.LENGTH_SHORT).show()
            return
        }

        binding.bottomSheetResult.tvViewAll.setOnClickListener {
            val intent = Intent(this, ViewTranslatedTextActivity::class.java)
            intent.putExtra(Misc.key, arrText.joinToString("\n\n"))
            startActivity(intent)
        }

        binding.pbText.text = resources.getString(R.string.ui_tr_translating)
        translateAll()
    }

    /**
     * Translates every paragraph from one source language. With "Detect" the language is detected
     * once, on the whole page: per-line detection got short lines wrong and mixed languages and
     * engines within one page. A paragraph that fails no longer throws away all the others.
     */
    private fun translateAll() {
        setSelectedLng()
        val run = ++translationRun
        binding.progressBar.visibility = View.VISIBLE
        arrTranslation.clear()
        val to = Misc.getLanguageTo(this)
        val from = Misc.getLanguageFrom(this)
        if (from == Misc.defaultLanguage) {
            MiscTranslate.detectLanguage(arrText.joinToString("\n")) { detected ->
                if (run == translationRun) translateParagraph(run, 0, detected ?: "auto", to, 0)
            }
        } else {
            translateParagraph(run, 0, from, to, 0)
        }
    }

    private fun translateParagraph(run: Int, position: Int, from: String, to: String, failures: Int) {
        if (run != translationRun) return
        if (position >= arrText.size) {
            onAllTranslated(failures)
            return
        }
        MiscTranslate.translate(this, arrText[position], from, to, object : TranslationInterface {
            override fun onTranslate(translation: String) {
                if (run != translationRun) return
                arrTranslation.add(translation)
                translateParagraph(run, position + 1, from, to, failures)
            }

            override fun onFailed() {
                if (run != translationRun) return
                arrTranslation.add(getString(R.string.ocr_paragraph_translation_failed))
                translateParagraph(run, position + 1, from, to, failures + 1)
            }
        })
    }

    private fun onAllTranslated(failures: Int) {
        binding.progressBar.visibility = View.GONE
        if (failures == arrText.size) {
            AppAnalytics.translateFailed(
                AppAnalytics.FEATURE_CAMERA,
                Misc.getLanguageFrom(this),
                Misc.getLanguageTo(this)
            )
            Toast.makeText(
                this,
                resources.getString(R.string.sorry_some_erroe_occurred_please_try_againg),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        AppAnalytics.translateDone(
            AppAnalytics.FEATURE_CAMERA,
            Misc.getLanguageFrom(this),
            Misc.getLanguageTo(this)
        )
        if (failures > 0) {
            Toast.makeText(this, R.string.ocr_some_paragraphs_failed, Toast.LENGTH_SHORT).show()
        }
        binding.bottomSheetResult.tvViewAll.visibility = View.VISIBLE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        binding.bottomSheetResult.rvOCRResult.layoutManager = LinearLayoutManager(this)
        binding.bottomSheetResult.rvOCRResult.adapter =
            OCRResultAdapter(this, arrText, arrTranslation)
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
        if (isLanguageSelection) {
            isLanguageSelection = false
            if (arrText.isNotEmpty()) translateAll()
        }
    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            binding.tvLanguageFrom.text =
                resources.getString(R.string.detect)
            binding.flagFrom.setImageResource(Misc.getFlag(this, "100"))

        } else {
            binding.tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
            binding.flagFrom.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))

        }

        binding.tvLanguageTo.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
        binding.flagTo.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))

    }

}
