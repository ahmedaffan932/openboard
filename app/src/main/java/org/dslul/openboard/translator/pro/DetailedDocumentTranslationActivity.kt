package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.android.synthetic.main.activity_detailed_document_translation.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.jsoup.Jsoup
import java.io.*
import java.net.URLEncoder
import java.util.*

class DetailedDocumentTranslationActivity : AppCompatActivity() {
    var textToSpeechLngTo: TextToSpeech? = null
    private val lngSelectorRequestCode = 1230
    var arrText = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_document_translation)

        Firebase.analytics.logEvent("DocumentTranslationDetailed", null)

        getTextFromFile()

//        Handler(Looper.getMainLooper()).postDelayed({
//            jugarTranslation(arrText, 0)
//        }, 100)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnCopyTranslatedText.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Camera Translator", textViewTranslatedText.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        btnSpeakTranslation.setOnClickListener {
            speakLngTo()
        }

        btnShareTranslation.setOnClickListener {
            if (textViewTranslatedText.text != "") {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, textViewTranslatedText.text)
                startActivity(Intent.createChooser(sharingIntent, "Share."))
            }
        }

        btnSwitchLngs.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 150
                rotate.interpolator = LinearInterpolator()
                val image = btnSwitchLngs
                image.startAnimation(rotate)

                Misc.zoomOutView(llLngToFrag, this, 150)
                Misc.zoomOutView(llLngFromFrag, this, 150)

                Handler().postDelayed({
                    val temp = Misc.getLanguageFrom(this)
                    Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                    Misc.setLanguageTo(this, temp)

                    setSelectedLng()

                    Misc.zoomInView(llLngToFrag, this, 150)
                    Misc.zoomInView(llLngFromFrag, this, 150)

                }, 150)
            } else {
                Toast.makeText(
                    this, "Please select language you want to translate from.", Toast.LENGTH_SHORT
                ).show()
            }
        }

        llLngFromFrag.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorRequestCode)
        }

        llLngToFrag.setOnClickListener {
            startActivityForResult(
                Intent(
                    this, LanguageSelectorActivity::class.java
                ), lngSelectorRequestCode
            )
        }

        btnPreviewDocument.setOnClickListener {
            val path = intent.getStringExtra(Misc.text).toString()
            val intent = Intent(this, PreviewDocumentActivity::class.java)
            intent.putExtra(Misc.text, path)
            startActivity(intent)
        }
    }

    private fun speakLngTo() {
        textToSpeechLngTo = TextToSpeech(applicationContext) { i ->
            if (i == TextToSpeech.ERROR) {
                Toast.makeText(
                    this, "Sorry! Your device does not support this language.", Toast.LENGTH_SHORT
                ).show()
            } else {
                textToSpeechLngTo?.language = Locale(Misc.getLanguageTo(this))

                textToSpeechLngTo?.speak(
                    textViewTranslatedText.text.toString(),
                    TextToSpeech.QUEUE_FLUSH,
                    null
                )
            }
        }
    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            textViewLngFromFrag.text = "Detect"
            textLngFrom.text = "Detect"
            flagFromFrag.setImageResource(Misc.getFlag(this, "100"))
        } else {
            textViewLngFromFrag.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
            textLngFrom.text = Misc.getLanguageFrom(this)

            flagFromFrag.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))
        }

        flagToFrag.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
        textViewLngToFrag.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
        textLngTo.text = Misc.getLanguageTo(this)
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
    }


    @SuppressLint("SetTextI18n")
    private fun jugarTranslation(arrText: ArrayList<String>, position: Int) {
        setSelectedLng()
        if (position >= arrText.size) {
            return
        }
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                "Please check your Internet connection and try again later.",
                Toast.LENGTH_SHORT
            ).show()
            llPBTranslateFrag.visibility = View.GONE
            return
        }
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            val languageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(arrText[position])
                .addOnSuccessListener { languageCode ->
                    if (languageCode == "und") {
                        Toast.makeText(
                            this, "Unable to detect Language. ", Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        textLngFrom.text = "Detected -> $languageCode"
                    }
                }.addOnFailureListener {
                    Toast.makeText(
                        this, "Unable to detect Language. ", Toast.LENGTH_SHORT
                    ).show()
                }
        }
        llPBTranslateFrag.visibility = View.VISIBLE
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val trimmedText = arrText[position].substring(0, minOf(arrText[position].length, 5000))
        val encoded = URLEncoder.encode(trimmedText, "utf-8")

        val fromCode = if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) ""
        else if (Misc.getLanguageFrom(this) == "zh") "zh-CN"
        else if (Misc.getLanguageFrom(this) == "he") "iw"
        else Misc.getLanguageFrom(this)

        val toCode = when (Misc.getLanguageTo(this)) {
            "zh" -> "zh-CN"
            "he" -> "iw"
            else -> Misc.getLanguageTo(this)
        }

        try {
            val doc =
                Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                    .get()

            val element = doc.getElementsByClass("result-container")

            if (element.text() != "" && !TextUtils.isEmpty(element.text())) {
                this.runOnUiThread {
                    try {
                        if (textViewTranslatedText.text.toString().isNotEmpty()) {
                            textViewTranslatedText.text =
                                textViewTranslatedText.text.toString() + "\n" + element.text()
                        } else {
                            textViewTranslatedText.text = element.text()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Some error occurred, Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                        e.printStackTrace()
                    }
                    llPBTranslateFrag.visibility = View.GONE
                }

                jugarTranslation(arrText, position + 1)

//                val objCustomDialog = CustomDialog(this)
//                objCustomDialog.show()
//
//                val window: Window = objCustomDialog.window!!
//                window.setLayout(
//                    WindowManager.LayoutParams.FILL_PARENT,
//                    WindowManager.LayoutParams.WRAP_CONTENT
//                )
//                window.setBackgroundDrawableResource(R.color.color_nothing)
//
//                objCustomDialog.findViewById<TextView>(R.id.tvTitle).text =
//                    "Your Document is too long, Some pages are translated od you want to translate more?"
//                objCustomDialog.findViewById<TextView>(R.id.btnYes).text = "Yes"
//                objCustomDialog.findViewById<TextView>(R.id.btnNo).text = "No"
//                objCustomDialog.setCancelable(true)
//
//                objCustomDialog.findViewById<TextView>(R.id.btnYes).setOnClickListener {
//                    jugarTranslation(arrText, position + 1)
//                    objCustomDialog.dismiss()
//                }
//
//                objCustomDialog.findViewById<TextView>(R.id.btnNo).setOnClickListener {
//                    objCustomDialog.dismiss()
//                }

            } else {
                llPBTranslateFrag.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Some error occurred in translation please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(Misc.logKey, "its empty")
                textViewTranslatedText.text = "Your file is empty"
            }
        } catch (e: Exception) {
            llPBTranslateFrag.visibility = View.GONE
            Toast.makeText(
                this,
                "Some error occurred in translation please try again later.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (textToSpeechLngTo != null) {
            if (textToSpeechLngTo?.isSpeaking == true) {
                textToSpeechLngTo?.stop()
            }
        }
    }

    private fun readTextFile(file: File) {
        val stringBuilder = StringBuilder()
        try {
            val bufferedReader = BufferedReader(FileReader(file))
            var line = bufferedReader.readLine()
            while (line != null) {
                stringBuilder.append(line).append("\n")
                line = bufferedReader.readLine()
            }
            bufferedReader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val fileContent = stringBuilder.toString()

        if (fileContent.isEmpty()) {
            splitTextIfRequired("Your file is empty")
        } else {
            splitTextIfRequired(fileContent)
        }
    }

    private fun getTextFromFile() {
        val path = intent.getStringExtra(Misc.text).toString()

        val file = File(path)
        when (file.extension) {
            "txt" -> {
                readTextFile(file)
            }
            "pdf" -> {
                try {
                    var parsedText = ""
                    val pdfInputStream = FileInputStream(file)
                    val reader = PdfReader(pdfInputStream)
                    val n: Int = reader.numberOfPages
                    for (i in 0 until n) {
                        parsedText = """$parsedText${
                            PdfTextExtractor.getTextFromPage(reader, i + 1)
                                .trim { it <= ' ' }
                        }""".trimIndent() //Extracting the content from the different pages
                    }
                    splitTextIfRequired(parsedText)
                    reader.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            "docx" -> {
                Toast.makeText(
                    this,
                    "Please convert Word file to PDF and the translate.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                Toast.makeText(
                    this,
                    "This file type can not be translated yet, We are working on it.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun splitTextIfRequired(text: String) {
        Log.d(Misc.logKey, "Parsed Text = $text")
        if (text.length > 5000) {
            val paragraphs = text.split("\n")
            var tempPara = ""
            for (para in paragraphs) {
                Log.d(Misc.logKey, "para $para")
                tempPara = if (tempPara.length < 4000) {
                    if (tempPara == "") {
                        para
                    } else {
                        tempPara + "\n" + para
                    }
                } else {
                    arrText.add(tempPara)
                    ""
                }
            }
        } else {
            arrText.add(text)
        }
        Log.d(Misc.logKey, arrText[(arrText.size / 2)] + arrText.size)
        textViewTranslatedText.text = ""
        jugarTranslation(arrText, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == lngSelectorRequestCode){
            getTextFromFile()

        }
    }
}