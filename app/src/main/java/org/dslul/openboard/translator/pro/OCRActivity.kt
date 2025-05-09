package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.android.synthetic.main.activity_ocractivity.*
import kotlinx.android.synthetic.main.bottom_sheet_ocr_translation_result.rvOCRResult
import kotlinx.android.synthetic.main.bottom_sheet_ocr_translation_result.tvViewAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.objects.CameraMisc
import org.dslul.openboard.translator.pro.adaptor.OCRResultAdapter
import org.dslul.openboard.translator.pro.classes.Misc
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList

class OCRActivity : AppCompatActivity() {
    private var arrText: ArrayList<String> = java.util.ArrayList<String>()
    private var arrTranslation: ArrayList<String> = java.util.ArrayList<String>()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private var bitmap: Bitmap? = null
    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, result.uriContent)
            imageViewOcr.setImageBitmap(resizeBitmap(bitmap!!, 1024))
            ocr(bitmap!!)

        } else {
            Toast.makeText(this, "Unable to crop", Toast.LENGTH_SHORT).show()
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
        setContentView(R.layout.activity_ocractivity)

        val policy: StrictMode.ThreadPolicy =
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetResult))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        btnBack.setOnClickListener {
            onBackPressed()
        }

        progressBar.setOnClickListener { }

        imageViewOcr.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }

        init()

        btnSwitchLngs.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 100
                rotate.interpolator = LinearInterpolator()

                val image = btnSwitchLngs
                image.startAnimation(rotate)

                val temp = Misc.getLanguageFrom(this)
                Misc.setLanguageFrom(
                    this,
                    Misc.getLanguageTo(this)
                )
                Misc.setLanguageTo(this, temp)

                if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
                    textViewLngFrom.text = resources.getString(R.string.detect)
                    flagFrom.setImageResource(Misc.getFlag(this, "100"))
                } else {
                    textViewLngFrom.text =
                        Locale(
                            Misc.getLanguageFrom(this)
                        ).displayName
                    flagFrom.setImageResource(
                        Misc.getFlag(
                            this,
                            Misc.getLanguageFrom(this)
                        )
                    )
                }
                textViewLngTo.text =
                    Locale(
                        Misc.getLanguageTo(this)
                    ).displayName
                flagTo.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
            }
        }

        llLngFrom.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivity(intent)
        }

        llLngTo.setOnClickListener {
            startActivity(Intent(this, LanguageSelectorActivity::class.java))
        }
    }


    private fun init() {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, CameraMisc.fileUri!!)

            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = CameraMisc.fileUri,
                    cropImageOptions = CropImageOptions(
                    )
                )
            )

            imageViewOcr.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Sorry, some error occurred, Please try again.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun ocr(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = if (Misc.getLanguageFrom(this) == TranslateLanguage.CHINESE) {
            TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        } else if (Misc.getLanguageFrom(this) == TranslateLanguage.KOREAN) {
            TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        } else if (Misc.getLanguageFrom(this) == TranslateLanguage.JAPANESE) {
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        } else {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }

        recognizer.process(image).addOnSuccessListener { visionText ->
            drawBoundaries(bitmap, visionText)
        }.addOnFailureListener { _ ->
            val textRecognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            textRecognizer.process(image).addOnSuccessListener { visionText ->
                drawBoundaries(bitmap, visionText)
            }.addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.sorry_no_text_found), Toast.LENGTH_SHORT)
                    .show()
                progressBar.visibility = View.GONE
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun drawBoundaries(bitmap: Bitmap, visionText: Text) {
        val rectPaint = Paint()
        rectPaint.color = Color.BLUE
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = 2.0f

        val tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(tempBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, rectPaint)

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                if (line.text.isNotEmpty()) {
                    val rect = RectF(line.boundingBox)
                    rectPaint.color = Color.BLUE

                    canvas.drawRect(rect, rectPaint)
                    imageViewOcr.setImageDrawable(BitmapDrawable(resources, tempBitmap))
                    arrText.add(line.text)
                }
            }
        }

        tvViewAll.setOnClickListener {
            val intent = Intent(this, ViewTranslatedTextActivity::class.java)
            intent.putExtra(Misc.key, visionText.text)
            startActivity(intent)
        }

        pbText.text = resources.getString(R.string.translating)

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(Misc.logKey, "Size: ${arrText.size}")
            jugarTranslation(arrText, 0)
        }, 100)
    }

    private fun jugarTranslation(arrText: ArrayList<String>, position: Int) {
        setSelectedLng()
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                resources.getString(R.string.please_check_your_internet_connection_and_try_again),
                Toast.LENGTH_SHORT
            ).show()
            progressBar.visibility = View.GONE
            return
        }
        progressBar.visibility = View.VISIBLE
        try {
            val fromCode =
                if (Misc.getLanguageFrom(this) == Misc.defaultLanguage)
                    ""
                else if (Misc.getLanguageFrom(this) == "zh")
                    "zh-CN"
                else if (Misc.getLanguageFrom(this) == "he")
                    "iw"
                else
                    Misc.getLanguageFrom(this)

            val toCode = when (Misc.getLanguageTo(this)) {
                "zh" -> "zh-CN"
                "he" -> "iw"
                else -> Misc.getLanguageTo(this)
            }

            if (position < arrText.size) {
                val encoded = URLEncoder.encode(arrText[position], "utf-8")
                GlobalScope.launch(Dispatchers.Main) {
                    val result = withContext(Dispatchers.IO) {
                        // Perform network request on IO thread
                        val doc =
                            Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                                .get()
                        val element = doc.getElementsByClass("result-container")
                        element.text()
                    }

                    if (result.isNotBlank()) {
                        arrTranslation.add(result)
                        Log.d(Misc.logKey, "Text: ${arrText[position]} OCR: $result")
                        jugarTranslation(arrText, position + 1)
                    } else {
                        progressBar.visibility =
                            View.GONE
                        Toast.makeText(
                            this@OCRActivity,
                            resources.getString(R.string.sorry_some_erroe_occurred_please_try_againg),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(Misc.logKey, "its empty")
                    }

                }
            } else {
                Log.d(Misc.logKey, "Here.")
                progressBar.visibility = View.GONE

                tvViewAll.visibility = View.VISIBLE

                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                rvOCRResult.layoutManager = LinearLayoutManager(this)
                rvOCRResult.adapter =
                    OCRResultAdapter(this, arrText, arrTranslation)
            }
        } catch (e: Exception) {
            progressBar.visibility =
                View.GONE
            Toast.makeText(
                this,
                resources.getString(R.string.sorry_some_erroe_occurred_please_try_againg),
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            textViewLngFrom.text =
                resources.getString(R.string.detect)
            flagFrom.setImageResource(Misc.getFlag(this, "100"))

        } else {
            textViewLngFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
            flagFrom.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))

        }

        textViewLngTo.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
        flagTo.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))

    }

}