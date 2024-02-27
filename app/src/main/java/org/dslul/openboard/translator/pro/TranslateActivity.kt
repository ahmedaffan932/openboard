package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.os.*
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.rw.keyboardlistener.KeyboardUtils
import kotlinx.android.synthetic.main.activity_translate.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import org.dslul.openboard.translator.pro.classes.admob.Ads
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*

class TranslateActivity : AppCompatActivity() {
    var isBtnTranslateVisible = false
    private var isLLTranslateVisible = false
    private val lngSelectorRequestCode = 1230
    private val speechRequestCode = 0
    private var textToSpeechLngTo: TextToSpeech? = null
    private var translationsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_translate)

        Misc.setIsFirstTime(this, false)

        Ads.showNativeAd(this, Ads.dashboardNative, nativeAdFrameLayoutInBetween)

        initializeAnimation()

        translationsCount = Misc.showInterstitialAfter

        btnBack.setOnClickListener {
            onBackPressed()
        }

        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            etMain.setText(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
            btnClearText.visibility = View.VISIBLE
        }

        Misc.isActivityCreatingFirstTime = true

        val handler = Handler()
        var count = 0
        val hint = "Tap here to enter text.."
        val runnable: Runnable by lazy {
            return@lazy object : Runnable {
                override fun run() {
                    if (count < hint.length) {
                        etMain.hint = hint.substring(0, count)
                        count++
                        handler.postDelayed(this, 75);
                    }
                }
            }
        }
        handler.post(runnable)


        etMain.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        }


        scrollView.isSmoothScrollingEnabled = true
        KeyboardUtils.addKeyboardToggleListener(this) { isVisible ->
            if (isVisible) {
                Misc.zoomOutView(btnSpeakInput, this, 150)
                Handler().postDelayed({
                    btnSpeakInput.visibility = View.GONE
                }, 150)

                if (!isBtnTranslateVisible) {
                    isBtnTranslateVisible = true
                }

            } else {
                etMain.clearFocus()
                if (etMain.text.toString() == "") {

                    isBtnTranslateVisible = false
                }
                Misc.zoomInView(btnSpeakInput, this, 150)
            }
        }

        tvTextTranslated.doOnTextChanged { _, _, _, _ ->
            llPBTranslateFrag.visibility = View.GONE
        }

        btnCopyTextTranslatedFrag.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Camera Translator", tvTextTranslated.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        btnClearTranslation.setOnClickListener {
            Misc.zoomOutView(llTranslatedText, this, 150)
            Handler().postDelayed({
                llTranslatedText.visibility = View.GONE
            }, 150)

            isBtnTranslateVisible = true
            isLLTranslateVisible = false
        }

        etMain.doOnTextChanged { text, start, before, count ->
            isBtnTranslateVisible = if (etMain.text.toString() == "") {
                Misc.zoomOutView(btnClearText, this, 150)
                Handler().postDelayed({
                    btnClearText.visibility = View.INVISIBLE
                }, 150)
                false
            } else {
                if (before == 0) Misc.zoomInView(btnClearText, this, 150)
                true
            }
        }

        btnHistory.setOnClickListener {
            startActivity(
                Intent(
                    this@TranslateActivity, DisplayHistoryActivity::class.java
                )
            )
        }

        btnClearText.setOnClickListener {
            etMain.setText("")
            tvTextTranslated.text = ""
            btnClearText.visibility = View.INVISIBLE
            Misc.zoomOutView(btnClearText, this, 150)
//            Misc.zoomOutView(btnTranslate, this, 150)
            Handler().postDelayed({
                btnClearText.visibility = View.INVISIBLE
//                btnTranslate.visibility = View.INVISIBLE

            }, 150)

            etMain.clearFocus()

            KeyboardUtils.forceCloseKeyboard(etMain)

            if (isLLTranslateVisible) {
                Misc.zoomOutView(llTranslatedText, this, 150)

                Handler().postDelayed({
                    llTranslatedText.visibility = View.GONE
                }, 150)
                isLLTranslateVisible = false
            }
        }

        btnSpeakTranslation.setOnClickListener {
            speakLngTo()
        }

        btnShareTranslation.setOnClickListener {
            if (tvTextTranslated.text != "") {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, tvTextTranslated.text)
                startActivity(Intent.createChooser(sharingIntent, "Share."))
            }
        }

        btnTranslate.setOnClickListener {
            if (etMain.text.toString() != "") {
                llPBTranslateFrag.visibility = View.VISIBLE
                Handler().postDelayed({
                    jugarTranslation(etMain.text.toString())
                }, 100)
            }
        }

        ivSwitchLanguages.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 150
                rotate.interpolator = LinearInterpolator()
                val image = ivSwitchLanguages
                image.startAnimation(rotate)

                Misc.zoomOutView(llLanguageTo, this, 150)
                Misc.zoomOutView(llLanguageFrom, this, 150)

                Handler().postDelayed({
                    val temp = Misc.getLanguageFrom(this)
                    Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                    Misc.setLanguageTo(this, temp)

                    setSelectedLng()

                    if (etMain.text.toString() != "") {
                        llPBTranslateFrag.visibility = View.VISIBLE
                        Handler().postDelayed({
                            jugarTranslation(etMain.text.toString())
                        }, 150)
                    }
                    Misc.zoomInView(llLanguageTo, this, 150)
                    Misc.zoomInView(llLanguageFrom, this, 150)

                }, 150)
            } else {
                Toast.makeText(
                    this, "Please select language you want to translate from.", Toast.LENGTH_SHORT
                ).show()
            }
        }

        llLanguageFrom.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorRequestCode)
        }

        llLanguageTo.setOnClickListener {
            startActivityForResult(
                Intent(
                    this, LanguageSelectorActivity::class.java
                ), lngSelectorRequestCode
            )
        }


        etMain.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val inputManager: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                if (this.currentFocus != null) {
                    inputManager.hideSoftInputFromWindow(
                        this.currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
                    )
                }
                llPBTranslateFrag.visibility = View.VISIBLE
//                if (Misc.getPurchasedStatus(this)) {
//                    translate(etMain.text.toString())
//                } else {
                Handler().postDelayed({
                    jugarTranslation(etMain.text.toString())
                }, 1)
//                }
                true
            } else false
        }

        btnSpeakInput.setOnClickListener {
            Firebase.analytics.logEvent("BtnSpeakInput", null)
            displaySpeechRecognizer()
        }
    }

    private fun initializeAnimation() {
        Misc.zoomOutView(btnClearText, this, 0)
        btnClearText.visibility = View.INVISIBLE

        Misc.zoomOutView(llTranslatedText, this, 0)
        llTranslatedText.visibility = View.GONE
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
                    tvTextTranslated.text.toString(),
                    TextToSpeech.QUEUE_FLUSH,
                    null
                )
            }
        }
    }

    private fun saveInHistory(text: String, translation: String) {
        if (text.length > 1000) {
            return
        }
        val historyList = Misc.getHistory(this)
        val objHistory = TranslateHistoryClass(
            text, translation, Misc.getLanguageTo(this), Misc.getLanguageFrom(this)
        )
        historyList.add(objHistory)
        val sharedPreferences = getSharedPreferences(
            Misc.history, MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(historyList)
        editor.putString(Misc.history, json)
        editor.apply()
        if (isBtnTranslateVisible) {
            isBtnTranslateVisible = false
        }
        if (!isLLTranslateVisible) {
            Handler().postDelayed({
                llTranslatedText.visibility = View.VISIBLE
                Misc.zoomInView(llTranslatedText, this, 150)
            }, 150)


            isLLTranslateVisible = true
        }
        Handler().postDelayed({
            scrollView.smoothScrollTo(0, scrollView.height)
        }, 10)


        etMain.clearFocus()
        KeyboardUtils.forceCloseKeyboard(etMain)
//        manageTranslationInterstitial()
    }

    @SuppressLint("SetTextI18n")
    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            textLngFrom.text = "Detect"
            tvLanguageFrom.text = "Detect"
        } else {
            tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
            textLngFrom.text = Misc.getLanguageFrom(this)

        }

        textLngTo.text = Misc.getLanguageTo(this)
        tvLanguageTo.text = Locale(Misc.getLanguageTo(this)).displayName
    }


    private fun jugarTranslation(text: String) {
        setSelectedLng()
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
            textLngFrom.text = "Detected"
        }

        llPBTranslateFrag.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(text, "utf-8")
                val fromCode =
                    if (Misc.getLanguageFrom(this@TranslateActivity) == Misc.defaultLanguage) ""
                    else if (Misc.getLanguageFrom(this@TranslateActivity) == "zh") "zh-CN"
                    else if (Misc.getLanguageFrom(this@TranslateActivity) == "he") "iw"
                    else Misc.getLanguageFrom(this@TranslateActivity)

                val toCode = when (Misc.getLanguageTo(this@TranslateActivity)) {
                    "zh" -> "zh-CN"
                    "he" -> "iw"
                    else -> Misc.getLanguageTo(this@TranslateActivity)
                }

                val doc =
                    Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                        .get()

                val element = doc.getElementsByClass("result-container")

                withContext(Dispatchers.Main) {
                    if (element.text().isNotEmpty()) {
                        tvTextTranslated.text = element.text()
                        saveInHistory(text, element.text())
                        showInterstitialIfRequired()
                    } else {
                        llPBTranslateFrag.visibility = View.GONE
                        Toast.makeText(
                            this@TranslateActivity,
                            "Some error occurred in translation, please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(Misc.logKey, "Translation result is empty")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    llPBTranslateFrag.visibility = View.GONE
                    Toast.makeText(
                        this@TranslateActivity,
                        "Some error occurred in translation, please try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun displaySpeechRecognizer() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.EXTRA_LANGUAGE)
            }

            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE, Misc.getLanguageFrom(this)
            )
            startActivityForResult(intent, speechRequestCode)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this, "Sorry, Some Error Occurred. Please try again.", Toast.LENGTH_LONG
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
                    etMain.setText(spokenText)
                    llPBTranslateFrag.visibility = View.VISIBLE
                    Handler().postDelayed({
                        jugarTranslation(etMain.text.toString())
                    }, 1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (requestCode == lngSelectorRequestCode) {
            if (etMain.text.toString() != "") {
                llPBTranslateFrag.visibility = View.VISIBLE
                Handler().postDelayed({
                    jugarTranslation(etMain.text.toString())
                }, 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
    }


    private fun showInterstitialIfRequired() {
        if (translationsCount >= Misc.showInterstitialAfter) {
            translationsCount = 0
            startActivity(Intent(this, TranslateInterstitialActivity::class.java))
        } else {
            translationsCount++
        }
    }

}