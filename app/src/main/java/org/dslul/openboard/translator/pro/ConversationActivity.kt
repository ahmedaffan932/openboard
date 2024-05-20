package org.dslul.openboard.translator.pro

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import kotlinx.android.synthetic.main.activity_conversation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*

class ConversationActivity : AppCompatActivity() {
    private val speechRequestCode = 0
    private var isLanguageTo = true
    private var textToSpeechLngTo: TextToSpeech? = null
    private var textToSpeechLngFrom: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        clLanguageFrom.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)

            startActivity(intent)
        }

        clLanguageTo.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            startActivity(intent)
        }

        ivInputFrom.setOnClickListener {
            isLanguageTo = false
            displaySpeechRecognizer(false)
        }

        ivInputTo.setOnClickListener {
            isLanguageTo = true
            displaySpeechRecognizer(true)
        }

        btnCopyTextTo.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Photo Translator", tvTextTo.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        btnCopyTextFrom.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Photo Translator", tvTextFrom.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        tvTextTo.setOnClickListener {
            speakLngTo()
        }

        tvTextFrom.setOnClickListener {
            speakLngFrom()
        }

        btnShareTextTo.setOnClickListener {
            if (tvTextTo.text != "") {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, tvTextTo.text.toString())
                startActivity(Intent.createChooser(sharingIntent, "Share via"))
            }
        }

        btnShareTextFrom.setOnClickListener {
            if (tvTextFrom.text != "") {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, tvTextFrom.text.toString())
                startActivity(Intent.createChooser(sharingIntent, "Share."))
            }
        }

        tvTextTo.addTextChangedListener { text ->
            if (text.toString() != "") {
                if (btnCopyTextTo.visibility == View.GONE) {
                    Misc.zoomInView(btnCopyTextTo, this, 250)
                    Handler(Looper.getMainLooper()).postDelayed({
                        Misc.zoomInView(btnShareTextTo, this, 250)
                    }, 100)
                }
            }
        }

        tvTextFrom.addTextChangedListener { text ->
            if (text.toString() != "") {
                if (btnCopyTextFrom.visibility == View.GONE) {
                    Misc.zoomInView(btnCopyTextFrom, this, 250)
                    Handler(Looper.getMainLooper()).postDelayed({
                        Misc.zoomInView(btnShareTextFrom, this, 250)
                    }, 100)
                }
            }
        }


    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            tvLanguageFrom.text = resources.getString(R.string.detect)
        } else {
            tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
        }

        tvLanguageTo.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
    }

    private fun displaySpeechRecognizer(isLanguageTo: Boolean) {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.EXTRA_LANGUAGE)
            }

            if (isLanguageTo) {
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE, Misc.getLanguageTo(this)
                )
            } else {
                if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE, Misc.getLanguageFrom(this)
                )
            }

            startActivityForResult(intent, speechRequestCode)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                resources.getString(R.string.sorry_some_erroe_occurred_please_try_againg),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequestCode && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results?.get(0)
                }
            try {
                if (spokenText != null) {
                    llPBTranslateFrag.visibility = View.VISIBLE
                    if (isLanguageTo) {
                        tvTextFrom.text = ""
                        setText(tvTextTo, spokenText)
                    } else {
                        tvTextTo.text = ""
                        setText(tvTextFrom, spokenText)
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        jugarTranslation(spokenText)
                    }, 500)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun jugarTranslation(text: String) {
        setSelectedLng()
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                resources.getString(R.string.please_check_your_internet_connection_and_try_again),
                Toast.LENGTH_SHORT
            ).show()
            llPBTranslateFrag.visibility = View.GONE
            return
        }

        llPBTranslateFrag.visibility = View.VISIBLE
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val encoded = URLEncoder.encode(text, "utf-8")

        var fromCode = if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) ""
        else if (Misc.getLanguageFrom(this) == "zh") "zh-CN"
        else if (Misc.getLanguageFrom(this) == "he") "iw"
        else Misc.getLanguageFrom(this)

        var toCode = when (Misc.getLanguageTo(this)) {
            "zh" -> "zh-CN"
            "he" -> "iw"
            else -> Misc.getLanguageTo(this)
        }
        if (isLanguageTo) {
            val temp = toCode
            toCode = if (fromCode == "") {
                "en"
            } else {
                fromCode
            }
            fromCode = temp
        }


//        try {
//            val doc =
//                Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
//                    .get()
//
//            val element = doc.getElementsByClass("result-container")
//
//            if (element.text() != "" && !TextUtils.isEmpty(element.text())) {
//                this.runOnUiThread {
//                    Log.d(Misc.logKey, "Element: ${element.text()}")
//                    setText(
//                        if (isLanguageTo) {
//                            tvTextFrom
//                        } else {
//                            tvTextTo
//                        }, element.text()
//                    )
//                    llPBTranslateFrag.visibility = View.GONE
////                    saveInHistory(text, element.text())
//                }
//            } else {
//                llPBTranslateFrag.visibility = View.GONE
//                Toast.makeText(
//                    this,
//                    resources.getString(R.string.sorry_some_erroe_occurred_please_try_againg),
//                    Toast.LENGTH_SHORT
//                ).show()
//                Log.e(Misc.logKey, "its empty")
//            }
//        } catch (e: Exception) {
//            llPBTranslateFrag.visibility = View.GONE
//            Toast.makeText(
//                this,
//                resources.getString(R.string.sorry_some_erroe_occurred_please_try_againg),
//                Toast.LENGTH_SHORT
//            ).show()
//        }

        translateAsync(fromCode, toCode, encoded, isLanguageTo)
    }

    private fun translateAsync(
        fromCode: String,
        toCode: String,
        encoded: String,
        isLanguageTo: Boolean
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                // Use a background thread for network operations
                val result = withContext(Dispatchers.IO) {
                    Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                        .get()
                }

                val element = result.getElementsByClass("result-container")

                val translatedText = element.text()
                if (translatedText.isNotBlank()) {
                    Log.d(Misc.logKey, "Element: $translatedText")
                    setText(
                        if (isLanguageTo) {
                            speakLngFrom(translatedText)
                            tvTextFrom
                        } else {
                            speakLngTo(translatedText)
                            tvTextTo
                        }, translatedText
                    )
                    llPBTranslateFrag.visibility = View.GONE
                } else {
                    handleTranslationError()
                }
            } catch (e: Exception) {
                handleTranslationError()
            }
        }
    }

    // Function to handle translation errors
    private fun handleTranslationError() {
        llPBTranslateFrag.visibility = View.GONE
        Toast.makeText(
            this,
            resources.getString(R.string.sorry_some_erroe_occurred_please_try_againg),
            Toast.LENGTH_SHORT
        ).show()
        Log.e(Misc.logKey, "An error occurred during translation")
    }

    private fun setText(view: TextView, text: String) {
        val handler = Handler()
        var count = 0
        val runnable: Runnable by lazy {
            return@lazy object : Runnable {
                override fun run() {
                    if (count < text.length + 1) {
                        view.text = text.substring(0, count)
                        count++
                        handler.postDelayed(this, 25);
                    }
                }
            }
        }
        handler.post(runnable)
    }

    override fun onBackPressed() {
        Ads.showInterstitial(this, Ads.phraseInt, object : InterstitialCallBack {
            override fun onDismiss() {
                finish()
            }
        })
    }

    private fun speakLngTo(text: String = "") {
        textToSpeechLngTo = TextToSpeech(applicationContext) { i ->
            if (i == TextToSpeech.ERROR) {
                Toast.makeText(
                    this, "Sorry, Language not supported.", Toast.LENGTH_SHORT
                ).show()
            } else {
                textToSpeechLngTo?.language = Locale(Misc.getLanguageTo(this))

                textToSpeechLngTo?.speak(
                    if (text == "")
                        tvTextTo.text.toString()
                    else text, TextToSpeech.QUEUE_FLUSH, null
                )
            }
        }


    }

    private fun speakLngFrom(text: String = "") {
        textToSpeechLngFrom = TextToSpeech(applicationContext) { i ->
            if (i == TextToSpeech.ERROR) {
                Toast.makeText(
                    this, "Sorry, Language not supported.", Toast.LENGTH_SHORT
                ).show()
            } else {
                textToSpeechLngFrom?.language = Locale(Misc.getLanguageFrom(this))

                textToSpeechLngFrom?.speak(
                    if (text == "")
                        tvTextFrom.text.toString()
                    else text, TextToSpeech.QUEUE_FLUSH, null
                )
            }
        }


    }

}