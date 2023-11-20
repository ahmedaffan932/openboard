package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.Color
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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.rw.keyboardlistener.KeyboardUtils
import kotlinx.android.synthetic.main.activity_translate.*
import kotlinx.android.synthetic.main.activity_translate.view.quitBottomSheet
import kotlinx.android.synthetic.main.bottom_sheet_quit.nativeAdFrameLayoutQuit
import kotlinx.android.synthetic.main.dailog_custom.view.btnNo
import kotlinx.android.synthetic.main.dailog_custom.view.btnYes
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import org.dslul.openboard.translator.pro.classes.admob.BannerAds
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import org.dslul.openboard.translator.pro.classes.admob.NativeAds
import org.dslul.openboard.translator.pro.interfaces.LoadInterstitialCallBack
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*

class TranslateActivity : AppCompatActivity() {
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

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

        Handler(Looper.getMainLooper()).postDelayed({
            if(!isInputMethodSelected()){
                startActivity(Intent(this, EnableKeyboardActivity::class.java))
            }
        }, 1000)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.quitBottomSheet))
        quitBottomSheet.quitBottomSheet.setOnClickListener { }
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    blockView.visibility = View.VISIBLE
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    blockView.visibility = View.GONE
                }
            }
        })

        quitBottomSheet.btnYes.setOnClickListener {
            finishAffinity()
        }

        quitBottomSheet.btnNo.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }


        blockView.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            blockView.visibility = View.GONE
        }

        Misc.isActivityCreatingFirstTime = true

        Misc.isRemoteConfigFetched.observeForever { t ->
            try {
                if (t == true) {
                    BannerAds.load(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.btnTranslate -> {

                }

                R.id.btnCamera -> {
                    startActivity(Intent(this, CameraTranslationActivity::class.java))
                }

                R.id.btnKeyboard -> {
                    if (isInputMethodSelected()) {
                        Toast.makeText(
                            this,
                            "Translator Pro Keyboard is already enabled, Enjoy!",
                            Toast.LENGTH_SHORT
                        ).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            bottomNavigationView.selectedItemId = R.id.btnTranslate
                        },500)
                    } else {
                        startActivity(Intent(this, EnableKeyboardActivity::class.java))
                    }
                }

                R.id.btnPhrasebook -> {
                    startActivity(Intent(this, PhrasesActivity::class.java))
                }

                R.id.btnFavorite -> {
                    startActivity(Intent(this, DisplayFavoritesActivity::class.java))
                }
            }
            true
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            textViewTextFrag.setText(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
//            btnTranslate.visibility = View.VISIBLE
            btnClearText.visibility = View.VISIBLE
        }

        if ((intent.getStringExtra(Misc.data) != null || intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain")
            && !Misc.getPurchasedStatus(this) && InterstitialAd.interAdmob == null
        ) {
            val objDialog = Misc.LoadingAdDialog(this)
            objDialog.setCancelable(false)
            objDialog.show()

            objDialog.findViewById<TextView>(R.id.warning).visibility = View.VISIBLE
            Misc.anyAdLoaded.observeForever { t ->
                try {
                    if (t) {
                        if (!showingInterstitial) {
                            InterstitialAd.showInterstitial(this, Misc.getAppOpenIntAm(this))
                        }
                        showingInterstitial = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
        InterstitialAd.showInterstitial(this, Misc.translateNativeAm, null)
        showNativeAd()

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
//                    btnTranslate.visibility = View.VISIBLE
//                    Misc.zoomInView(btnTranslate, this, 150)
                    isBtnTranslateVisible = true
                }
                nativeAdFrameLayout.visibility = View.GONE

            } else {
                textViewTextFrag.clearFocus()
                if (textViewTextFrag.text.toString() == "") {
//                    Misc.zoomOutView(btnTranslate, this, 150)
//                    Handler().postDelayed({
//                        btnTranslate.visibility = View.INVISIBLE
//                    }, 150)

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
//            Misc.zoomInView(btnTranslate, this, 150)
//            btnTranslate.visibility = View.VISIBLE
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
                    textViewTextTranslatedFrag.text.toString(),
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
                    try {
                        textViewTextTranslatedFrag.text = ""
                        textViewTextTranslatedFrag.text = element.text()

                        saveInHistory(text, element.text())
                        showInterstitialIfRequired()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Some error occurred, Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                        e.printStackTrace()
                    }
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
        bottomNavigationView.selectedItemId = R.id.btnTranslate
        Log.d(Misc.logKey, "TranslatorOnResume.")
        if (Misc.isNativeAdClicked) {
            Log.d(Misc.logKey, "TranslatorNativeAdAfterOnClick")
            showNativeAd()
        }
    }

    private fun showNativeAd() {
        if (Misc.isTranslationInBetweenNativeEnabled) {
            NativeAds.manageShowNativeAd(
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

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            NativeAds.manageShowNativeAd(this, Misc.quitNativeAm, nativeAdFrameLayoutQuit)
            Log.d(Misc.logKey, "Bottom sheet clicked.")
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

}