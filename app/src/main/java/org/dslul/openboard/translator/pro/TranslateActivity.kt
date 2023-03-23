package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.speech.RecognizerIntent
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.rw.keyboardlistener.KeyboardUtils
import kotlinx.android.synthetic.main.activity_translate.*
import kotlinx.android.synthetic.main.activity_translate.btnHistory
import kotlinx.android.synthetic.main.activity_translate.nativeAdFrameLayout
import kotlinx.android.synthetic.main.activity_translate.nativeAdFrameLayoutInBetween
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.admob.BannerAds
import org.dslul.openboard.translator.pro.classes.admob.NativeAds
import org.dslul.openboard.translator.pro.interfaces.LoadInterstitialCallBack
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*

class TranslateActivity : AppCompatActivity() {
    private var isNativeAdLoaded: Boolean = false
    var isBtnTranslateVisible = false
    var isLLTranslateVisible = false
    private val lngSelectorRequestCode = 1230
    private val speechRequestCode = 0
    var textToSpeechLngTo: TextToSpeech? = null
    var translationsCount = 0
    var showingInterstitial = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_translate)

        initializeAnimation()

        translationsCount = Misc.showInterstitialAfter

        if(isInputMethodSelected()){
            btnKeyboard.visibility = View.GONE
        }else{
            btnKeyboard.visibility= View.VISIBLE
        }

        btnKeyboard.setOnClickListener {
            startActivity(Intent(this, EnableKeyboardActivity::class.java))
        }

        Misc.isRemoteConfigFetched.observeForever { t ->
            if (t == true) {
                BannerAds.load(this)
            }
        }

        NativeAds.loadNativeAd(this@TranslateActivity, object : LoadInterstitialCallBack {
            override fun onLoaded() {
                NativeAds.manageShowNativeAd(
                    this@TranslateActivity,
                    Misc.splashNativeAm,
                    nativeAdFrameLayout
                )
            }
        })

        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            textViewTextFrag.setText(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
            btnTranslate.visibility = View.VISIBLE
            btnClearText.visibility = View.VISIBLE
        }

        if ((intent.getStringExtra(Misc.data) != null || intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain")
            && !Misc.getPurchasedStatus(this) && InterstitialAd.interAdmob == null
        ) {
            val objDialog = Misc.LoadingAdDialog(this)
            objDialog.setCancelable(false)
            objDialog.show()

            objDialog.findViewById<TextView>(R.id.warning).visibility = View.VISIBLE
            InterstitialAd.manageLoadInterAdmob(this)
            Misc.anyAdLoaded.observeForever { t ->
                if (t) {
                    if (!showingInterstitial) {
                        InterstitialAd.show(this, Misc.getAppOpenIntAm(this))
                    }
                    showingInterstitial = true
                }
            }

            object : CountDownTimer(1500, 3000) {
                override fun onTick(p0: Long) {}
                override fun onFinish() {
                    if (objDialog.isShowing) {
                        objDialog.dismiss()
                    }
                }
            }.start()
        }
        InterstitialAd.show(this, Misc.translateNativeAm, null)
        showNativeAd()

        if (Misc.isCameraTranslatorAdEnabled) {
            btnCamera.visibility = View.VISIBLE
        } else {
            btnCamera.visibility = View.GONE
        }

        btnCamera.setOnClickListener {
            Firebase.analytics.logEvent("BtnCameraTranslate", null)
            val p = "com.object.translate.all.languages.free.translation.elite.translator"
            val uri: Uri = Uri.parse("market://details?id=$p")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)

            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=$p")
                    )
                )
            }
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
                    btnSpeakInput.visibility = View.GONE
                }, 150)

                if (!isBtnTranslateVisible) {
                    btnTranslate.visibility = View.VISIBLE
                    Misc.zoomInView(btnTranslate, this, 150)
                    isBtnTranslateVisible = true
                }
                nativeAdFrameLayout.visibility = View.GONE

            } else {
                textViewTextFrag.clearFocus()
                if (textViewTextFrag.text.toString() == "") {
                    Misc.zoomOutView(btnTranslate, this, 150)
                    Handler().postDelayed({
                        btnTranslate.visibility = View.INVISIBLE
                    }, 150)

                    isBtnTranslateVisible = false
                }
                Misc.zoomInView(btnSpeakInput, this, 150)
                if (!isLLTranslateVisible) {
                    if (isNativeAdLoaded) {
                        nativeAdFrameLayout.visibility = View.VISIBLE
                    }
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
//            llText.setBackgroundResource(R.drawable.bg_main_less_rounded)

            Misc.zoomOutView(llTranslatedText, this, 150)
            Handler().postDelayed({
                llTranslatedText.visibility = View.GONE
            }, 150)

            Handler().postDelayed({
                if (isNativeAdLoaded) nativeAdFrameLayout.visibility = View.VISIBLE
            }, 150)
            Misc.zoomInView(btnTranslate, this, 150)
            btnTranslate.visibility = View.VISIBLE
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

        btnHistory.setOnClickListener {
            startActivity(
                Intent(
                    this@TranslateActivity, DisplayHistoryActivity::class.java
                )
            )
        }

        btnClearText.setOnClickListener {
            textViewTextFrag.setText("")
            textViewTextTranslatedFrag.text = ""
            btnClearText.visibility = View.INVISIBLE
            Misc.zoomOutView(btnClearText, this, 150)
            Misc.zoomOutView(btnTranslate, this, 150)
            Handler().postDelayed({
                btnClearText.visibility = View.INVISIBLE
                btnTranslate.visibility = View.INVISIBLE

            }, 150)

            textViewTextFrag.clearFocus()

            KeyboardUtils.forceCloseKeyboard(textViewTextFrag)

//            llText.setBackgroundResource(R.drawable.bg_main_less_rounded)

            if (isLLTranslateVisible) {
                Misc.zoomOutView(llTranslatedText, this, 150)

                Handler().postDelayed({
                    llTranslatedText.visibility = View.GONE
                }, 150)
                Handler().postDelayed({
                    if (isNativeAdLoaded) nativeAdFrameLayout.visibility = View.VISIBLE
                }, 160)
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

                Misc.zoomOutView(llLngToFrag, this, 150)
                Misc.zoomOutView(llLngFromFrag, this, 150)

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
            val intent = Intent(this@TranslateActivity, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorRequestCode)
        }

        llLngToFrag.setOnClickListener {
            startActivityForResult(
                Intent(
                    this@TranslateActivity, LanguageSelectorActivity::class.java
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

        btnSpeakInput.setOnClickListener {
            Firebase.analytics.logEvent("BtnSpeakInput", null)
            displaySpeechRecognizer()
        }
    }

    private fun initializeAnimation() {
        Misc.zoomOutView(btnTranslate, this, 0)
        btnTranslate.visibility = View.INVISIBLE
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
                    textViewTextTranslatedFrag.text.toString(),
                    TextToSpeech.QUEUE_FLUSH,
                    null
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
//    private fun translate(text: String) {
//        setSelectedLng()
//        if (!Misc.checkInternetConnection(this)) {
//            Toast.makeText(
//                this,
//                "Please check your Internet connection and try again later.",
//                Toast.LENGTH_SHORT
//            ).show()
//            llPBTranslateFrag.visibility = View.GONE
//            return
//        }
//
//        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
//            val languageIdentifier = LanguageIdentification.getClient()
//            languageIdentifier.identifyLanguage(text).addOnSuccessListener { languageCode ->
//                if (languageCode == "und") {
//                    Toast.makeText(
//                        this, "Unable to detect Language. ", Toast.LENGTH_SHORT
//                    ).show()
//                } else {
//                    textLngFrom.text = "Detected -> $languageCode"
//                }
//            }.addOnFailureListener {
//                Toast.makeText(
//                    this, "Unable to detect Language. ", Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//        Handler().postDelayed({
//            val query = URLEncoder.encode(text, "utf-8")
//            val stringRequest: StringRequest = @SuppressLint("CutPasteId") object : StringRequest(
//                Method.POST,
//                "${Misc.translationUrl}q=$query",
//                Response.Listener { response ->
//                    val tempArrayList = JSONObject(response).getJSONObject(Misc.data)
//                        .getJSONArray(Misc.translations)
//                    textViewTextTranslatedFrag.text = ""
//
//                    var str = ""
//                    for (i in 0 until tempArrayList.length()) {
//                        str += tempArrayList.getJSONObject(i).getString(Misc.translatedText)
//                    }
//
//                    textViewTextTranslatedFrag.text = str
//                    saveInHistory(text, str)
//                },
//                Response.ErrorListener { error ->
//                    llPBTranslateFrag.visibility = View.GONE
//                    Log.d(Misc.logKey, Misc.getGoogleApi(this))
//                    Log.d(Misc.logKey, "Translation error $error")
//                }) {
//                override fun getParams(): Map<String, String> {
//                    val params: MutableMap<String, String> = HashMap()
//                    params[Misc.key] = Misc.getGoogleApi(this@TranslateActivity)
//                    params[Misc.target] = Misc.getLanguageTo(this@TranslateActivity)
//                    return params
//                }
//
//                override fun getHeaders(): Map<String, String> {
//                    val headers: MutableMap<String, String> = HashMap()
//                    headers["User-agent"] = Misc.userAgent
//                    return headers
//                }
//            }
//
//            val requestQueue = Volley.newRequestQueue(this)
//            requestQueue.add(stringRequest)
//        }, 1)
//    }

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
            Misc.zoomOutView(btnTranslate, this, 150)
            Handler().postDelayed({
                btnTranslate.visibility = View.INVISIBLE
            }, 150)
            isBtnTranslateVisible = false
        }
        if (!isLLTranslateVisible) {
            Handler().postDelayed({
//                llText.setBackgroundResource(R.drawable.bg_half_up_rounded)
            }, 140)

            nativeAdFrameLayout.visibility = View.GONE

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

    @SuppressLint("SetTextI18n")
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
            val languageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(text).addOnSuccessListener { languageCode ->
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

            if (element.text() != "" && !TextUtils.isEmpty(element.text())) {

                this.runOnUiThread {
                    textViewTextTranslatedFrag.text = ""
                    textViewTextTranslatedFrag.text = element.text()

                    saveInHistory(text, element.text())
                    showInterstitialIfRequired()
                }
            } else {
                llPBTranslateFrag.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Some error occurred in translation please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
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
                    llPBTranslateFrag.visibility = View.VISIBLE
                    Handler().postDelayed({
                        jugarTranslation(textViewTextFrag.text.toString())
                    }, 1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (requestCode == lngSelectorRequestCode) {
            if (textViewTextFrag.text.toString() != "") {
                llPBTranslateFrag.visibility = View.VISIBLE
                Handler().postDelayed({
                    jugarTranslation(textViewTextFrag.text.toString())
                }, 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
        Log.d(Misc.logKey, "TranslatorOnResume.")
        if (Misc.isNativeAdClicked) {
            Log.d(Misc.logKey, "TranslatorNativeAdAfterOnClick")
            showNativeAd()
        }
    }

    override fun onBackPressed() {
        if (intent.getStringExtra(Misc.data) != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private fun showNativeAd() {
        if (Misc.isTranslationInBetweenNativeEnabled) {
            NativeAds.showSmallNativeAd(
                this,
                Misc.translateNativeAm,
                nativeAdFrameLayoutInBetween
            )
        } else {
            NativeAds.manageShowNativeAd(
                this,
                Misc.translateNativeAm,
                nativeAdFrameLayout
            ) { isLoaded ->
                isNativeAdLoaded = isLoaded
            }
        }
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