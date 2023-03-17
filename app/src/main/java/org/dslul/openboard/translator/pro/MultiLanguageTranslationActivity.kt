package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import java.util.*
import org.jsoup.Jsoup
import android.util.Log
import android.view.View
import android.os.Bundle
import android.os.Handler
import java.net.URLEncoder
import android.app.Activity
import android.widget.Toast
import android.os.StrictMode
import android.text.TextUtils
import android.content.Intent
import android.graphics.Color
import android.content.Context
import android.view.WindowManager
import android.speech.RecognizerIntent
import android.view.Window
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import com.rw.keyboardlistener.KeyboardUtils
import org.dslul.openboard.translator.pro.classes.Misc
import androidx.appcompat.app.AppCompatActivity
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.translator.pro.classes.TranslatedItem
import org.dslul.openboard.translator.pro.classes.CustomDialog
import org.dslul.openboard.translator.pro.classes.Misc.startProActivity
import org.dslul.openboard.translator.pro.classes.admob.NativeAds
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_multi_language_translation.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.adaptor.MultiLanguageTranslationAdaptor
import org.dslul.openboard.translator.pro.adaptor.ShowMultipleSelectedLanguages
import org.dslul.openboard.translator.pro.interfaces.OnItemClick

class MultiLanguageTranslationActivity : AppCompatActivity() {
    var isBtnTranslateVisible = false
    var isLLTranslateVisible = false
    private val speechRequestCode = 0
    private val arrTranslation = ArrayList<TranslatedItem>()
    var translatedItemAdaptor: MultiLanguageTranslationAdaptor? = null
    var translated = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_multi_language_translation)
        Firebase.analytics.logEvent("MultipleLanguageTranslation", null)

        NativeAds.manageShowNativeAd(this, Misc.multiTranslateNativeAm, nativeAdFrameLayout, null)

        translatedItemAdaptor = MultiLanguageTranslationAdaptor(this)
        rvTranslatedText.layoutManager = LinearLayoutManager(this)

        rvTranslatedText.adapter = translatedItemAdaptor

        btnBack.setOnClickListener {
            onBackPressed()
        }

        llLngFromFrag.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.data, "ml")
            intent.putExtra(Misc.lngTo, false)
            startActivity(intent)
        }

        btnAddLanguages.setOnClickListener {
            startActivity(Intent(this, MultipleLanguageSelectorActivity::class.java))
        }

        val handler = Handler()
        var count = 0
        val hint = "Tap here to enter text.."
        val runnable: Runnable by lazy {
            return@lazy object : Runnable {
                override fun run() {
                    if (count < hint.length) {
                        textViewTextFrag.hint = hint.substring(0, count)
                        count++
                        handler.postDelayed(this, 75)
                    }
                }
            }
        }
        handler.post(runnable)

        initializeAnimation()

        if (!Misc.isNightModeOn(this)) {
            textViewTextFrag.setTextColor(Color.BLACK)
        }

        textViewTextFrag.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        }

        scrollView.isSmoothScrollingEnabled = true
        KeyboardUtils.addKeyboardToggleListener(this) { isVisible ->
            if (isVisible) {
                Misc.zoomOutView(btnSpeakInput, this, 150)
                Handler().postDelayed({
                    btnSpeakInput.visibility = View.INVISIBLE
                }, 150)
                if (!isBtnTranslateVisible) {
                    btnTranslate.visibility = View.VISIBLE
                    Misc.zoomInView(btnTranslate, this, 150)
                    isBtnTranslateVisible = true
                }

            } else {
                textViewTextFrag.clearFocus()
                if (textViewTextFrag.text.toString() == "") {
                    Misc.zoomOutView(btnTranslate, this, 150)
                    btnTranslate.visibility = View.INVISIBLE

                    isBtnTranslateVisible = false
                }
                Misc.zoomInView(btnSpeakInput, this, 150)
            }
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

        btnClearText.setOnClickListener {
            NativeAds.manageShowNativeAd(
                this,
                Misc.multiTranslateNativeAm,
                nativeAdFrameLayout,
                null
            )

            textViewTextFrag.setText("")
            Misc.zoomOutView(btnClearText, this, 150)
            Misc.zoomOutView(btnTranslate, this, 150)
            Handler().postDelayed({
                btnClearText.visibility = View.INVISIBLE
                btnTranslate.visibility = View.INVISIBLE
            }, 150)

            translated = 0
            arrTranslation.clear()
            translatedItemAdaptor?.setData(arrTranslation)

            textViewTextFrag.clearFocus()

            KeyboardUtils.forceCloseKeyboard(textViewTextFrag)

            if (isLLTranslateVisible) {
                Misc.zoomOutView(rvTranslatedText, this, 150)
                llText.setBackgroundResource(R.drawable.bg_main_less_rounded)

                Handler().postDelayed({
                    rvTranslatedText.visibility = View.GONE
                }, 150)
                isLLTranslateVisible = false
            }
        }

        btnTranslate.setOnClickListener {
            if (textViewTextFrag.text.toString() != "") {
                translated = 0
                arrTranslation.clear()
                translatedItemAdaptor?.setData(arrTranslation)
                Handler().postDelayed({
                    for (lng in Misc.getMultipleSelectedLanguages(this)) jugarTranslation(
                        textViewTextFrag.text.toString(),
                        lng
                    )
                }, 100)
            }
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
                translated = 0
                arrTranslation.clear()
                translatedItemAdaptor?.setData(arrTranslation)
                Handler().postDelayed({
                    for (lng in Misc.getMultipleSelectedLanguages(this)) jugarTranslation(
                        textViewTextFrag.text.toString(),
                        lng
                    )
                }, 1)
                true
            } else false
        }

        btnSpeakInput.setOnClickListener {
            displaySpeechRecognizer()
        }
    }

    override fun onResume() {
        if (Misc.isNativeAdClicked) {
            NativeAds.manageShowNativeAd(
                this,
                Misc.translateNativeAm,
                nativeAdFrameLayout
            )
        }

        setSelectedLng()
        val arr = ArrayList<String>()
        for (lng in Misc.getMultipleSelectedLanguages(this)) {
            arr.add(lng)
        }

        rvSelectedLanguages.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvSelectedLanguages.adapter =
            ShowMultipleSelectedLanguages(this, arr, object : OnItemClick {
                override fun onClick() {
                    startActivity(
                        Intent(
                            this@MultiLanguageTranslationActivity,
                            MultipleLanguageSelectorActivity::class.java
                        )
                    )
                }
            })
        super.onResume()
    }

    @SuppressLint("SetTextI18n")
    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            textViewLngFromFrag.text = "Detect"
            textViewLngFromFrag.text = "Detect"
            flagFromFrag.setImageResource(Misc.getFlag(this, "100"))
        } else {
            textViewLngFromFrag.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
            flagFromFrag.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))
        }
    }

    private fun initializeAnimation() {
        Misc.zoomOutView(btnTranslate, this, 0)
        btnTranslate.visibility = View.INVISIBLE
        Misc.zoomOutView(btnClearText, this, 0)
        btnClearText.visibility = View.INVISIBLE

        btnTranslate.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun jugarTranslation(text: String, lngTo: String) {
        if (Misc.isMultiTranslatorPremiumModule) if (!Misc.getPurchasedStatus(this)) {
            val objCustomDialog = CustomDialog(this)
            objCustomDialog.show()

            val window: Window = objCustomDialog.window!!
            window.setLayout(
                WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(R.color.color_nothing)

            objCustomDialog.findViewById<TextView>(R.id.tvTitle).text =
                "You need to upgrade to translate Multiple Languages.."
            objCustomDialog.findViewById<TextView>(R.id.btnYes).text = "Upgrade"
            objCustomDialog.findViewById<TextView>(R.id.btnNo).text = "May be later."
            objCustomDialog.setCancelable(true)

            objCustomDialog.findViewById<TextView>(R.id.btnYes).setOnClickListener {
                startProActivity(Misc.data)
                objCustomDialog.dismiss()
            }

            objCustomDialog.findViewById<TextView>(R.id.btnNo).setOnClickListener {
                objCustomDialog.dismiss()
            }
            return
        }

        val inputManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (this.currentFocus != null) {
            inputManager.hideSoftInputFromWindow(
                this.currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
            )
        }

        setSelectedLng()
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                "Please check your Internet connection and try again later.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        llPBTranslateFrag.visibility = View.VISIBLE

        Handler().postDelayed({
            val policy: StrictMode.ThreadPolicy =
                StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            val encoded = URLEncoder.encode(text, "utf-8")

            val fromCode = if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) ""
            else if (Misc.getLanguageFrom(this) == "zh") "zh-CN"
            else if (Misc.getLanguageFrom(this) == "he") "iw"
            else Misc.getLanguageFrom(this)

            val toCode = when (lngTo) {
                "zh" -> "zh-CN"
                "he" -> "iw"
                else -> lngTo
            }

            try {
                val doc =
                    Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                        .get()

                val element = doc.getElementsByClass("result-container")

                if (element.text() != "" && !TextUtils.isEmpty(element.text())) {
                    this.runOnUiThread {
                        nativeAdFrameLayout.visibility = View.GONE
                        translated++
                        if (translated > Misc.getMultipleSelectedLanguages(this).size - 2) llPBTranslateFrag.visibility =
                            View.GONE
                        Log.d(Misc.logKey, element.text())

                        arrTranslation.add(
                            TranslatedItem(
                                Misc.getLanguageFrom(this), lngTo, text, element.text()
                            )
                        )
                        translatedItemAdaptor?.setData(arrTranslation)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Some error occurred in translation please try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                    llPBTranslateFrag.visibility = View.GONE
                    Log.e(Misc.logKey, "its empty")
                }
            } catch (e: Exception) {
                llPBTranslateFrag.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Some error occurred in translation please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, 100)
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
                    textViewTextFrag.setText(spokenText)
                    translated = 0
                    arrTranslation.clear()
                    translatedItemAdaptor?.setData(arrTranslation)
                    Handler().postDelayed({
                        for (lng in Misc.getMultipleSelectedLanguages(this)) jugarTranslation(
                            textViewTextFrag.text.toString(),
                            lng
                        )
                    }, 1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}