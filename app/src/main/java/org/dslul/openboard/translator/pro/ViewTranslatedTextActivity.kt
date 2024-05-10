package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.rw.keyboardlistener.KeyboardUtils
import kotlinx.android.synthetic.main.activity_view_translated_text.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import org.dslul.openboard.translator.pro.classes.admob.Ads
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.Locale

class ViewTranslatedTextActivity : AppCompatActivity() {
    private var isNativeAdLoaded: Boolean = false
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
        setContentView(R.layout.activity_view_translated_text)

        Misc.setIsFirstTime(this, false)

        Ads.showNativeAd(this, Ads.dashboardNative, nativeAdFrameLayoutInBetween)

        initializeAnimation()

        translationsCount = Misc.showInterstitialAfter

        Misc.isActivityCreatingFirstTime = true

        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            textViewTextFrag.setText(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
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
                        textViewTextFrag.hint = hint.substring(0, count)
                        count++
                        handler.postDelayed(this, 75);
                    }
                }
            }
        }
        handler.post(runnable)

        Handler(Looper.getMainLooper()).postDelayed({
            if (intent.getStringExtra(Misc.key) == null) {
                Toast.makeText(
                    this,
                    "Sorry some error occurred, Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                textViewTextFrag.setText(intent.getStringExtra(Misc.key))
                jugarTranslation(textViewTextFrag.text.toString())
            }
        }, 100)

        textViewTextFrag.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        }


        scrollView.isSmoothScrollingEnabled = true
        KeyboardUtils.addKeyboardToggleListener(this) { isVisible ->
            if (isVisible) {
                if (!isBtnTranslateVisible) {
                    isBtnTranslateVisible = true
                }

            } else {
                textViewTextFrag.clearFocus()
                if (textViewTextFrag.text.toString() == "") {

                    isBtnTranslateVisible = false
                }
            }
        }

        textViewTextTranslatedFrag.doOnTextChanged { _, _, _, _ ->
            llPBTranslateFrag.visibility = View.GONE
        }

        btnCopyTextTranslatedFrag.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Camera Translator", textViewTextTranslatedFrag.text
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

        textViewTextFrag.doOnTextChanged { text, start, before, count ->
            isBtnTranslateVisible = if (textViewTextFrag.text.toString() == "") {
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

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnClearText.setOnClickListener {
            textViewTextFrag.setText("")
            textViewTextTranslatedFrag.text = ""
            btnClearText.visibility = View.INVISIBLE
            Misc.zoomOutView(btnClearText, this, 150)
//            Misc.zoomOutView(btnTranslate, this, 150)
            Handler().postDelayed({
                btnClearText.visibility = View.INVISIBLE
//                btnTranslate.visibility = View.INVISIBLE

            }, 150)

            textViewTextFrag.clearFocus()

            KeyboardUtils.forceCloseKeyboard(textViewTextFrag)

//            llText.setBackgroundResource(R.drawable.bg_main_less_rounded)

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
            if (textViewTextTranslatedFrag.text != "") {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, textViewTextTranslatedFrag.text)
                startActivity(Intent.createChooser(sharingIntent, "Share."))
            }
        }

        btnTranslate.setOnClickListener {
            if (textViewTextFrag.text.toString() != "") {
                llPBTranslateFrag.visibility = View.VISIBLE
                Handler().postDelayed({
                    jugarTranslation(textViewTextFrag.text.toString())
                }, 100)
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

                Misc.zoomOutView(llLngTo, this, 150)
                Misc.zoomOutView(llLngFrom, this, 150)

                Handler().postDelayed({
                    val temp = Misc.getLanguageFrom(this)
                    Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                    Misc.setLanguageTo(this, temp)

                    setSelectedLng()

                    if (textViewTextFrag.text.toString() != "") {
                        llPBTranslateFrag.visibility = View.VISIBLE
                        Handler().postDelayed({
                            jugarTranslation(textViewTextFrag.text.toString())
                        }, 150)
                    }
                    Misc.zoomInView(llLngTo, this, 150)
                    Misc.zoomInView(llLngFrom, this, 150)

                }, 150)
            } else {
                Toast.makeText(
                    this, "Please select language you want to translate from.", Toast.LENGTH_SHORT
                ).show()
            }
        }

        llLngFrom.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorRequestCode)
        }

        llLngTo.setOnClickListener {
            startActivityForResult(
                Intent(
                    this, LanguageSelectorActivity::class.java
                ), lngSelectorRequestCode
            )
        }


        textViewTextFrag.setOnEditorActionListener { _, actionId, _ ->
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
//                    translate(textViewTextFrag.text.toString())
//                } else {
                Handler().postDelayed({
                    jugarTranslation(textViewTextFrag.text.toString())
                }, 1)
//                }
                true
            } else false
        }

    }

    private fun initializeAnimation() {
        Misc.zoomOutView(btnClearText, this, 0)
        btnClearText.visibility = View.INVISIBLE

        Misc.zoomOutView(llTranslatedText, this, 0)
        llTranslatedText.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
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
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val encoded = URLEncoder.encode(text, "utf-8")

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

            GlobalScope.launch(Dispatchers.Main) {
                try {
                    if (element.text().isNotBlank()) {
                        textViewTextTranslatedFrag.text = element.text()
                        saveInHistory(text, element.text())
                    } else {
                        showToast("Translation result is empty.")
                    }
                } catch (e: Exception) {
                    showToast("Some error occurred. Please try again.")
                    e.printStackTrace()
                } finally {
                    llPBTranslateFrag.visibility = View.GONE
                }
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

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            textViewLngFrom.text = "Detect"
            textLngFrom.text = "Detect"
            flagFrom.setImageResource(Misc.getFlag(this, "100"))
        } else {
            textViewLngFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
            textLngFrom.text = Misc.getLanguageFrom(this)

            flagFrom.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))
        }

        flagTo.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
        textViewLngTo.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
        textLngTo.text = Misc.getLanguageTo(this)
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


        textViewTextFrag.clearFocus()
        KeyboardUtils.forceCloseKeyboard(textViewTextFrag)
//        manageTranslationInterstitial()
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
                    textViewTextTranslatedFrag.text.toString(),
                    TextToSpeech.QUEUE_FLUSH,
                    null
                )
            }
        }
    }

}