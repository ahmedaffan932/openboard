package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.Color
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityTranslateBinding
import org.dslul.openboard.translator.pro.classes.MiscTranslate
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import org.dslul.openboard.translator.pro.interfaces.TranslationInterface
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*

class TranslateActivity : AppCompatActivity() {
    private var isNativeAdLoaded: Boolean = false
    lateinit var binding: ActivityTranslateBinding
    private var isBtnTranslateVisible = false
    private var isLLTranslateVisible = false
    private val lngSelectorRequestCode = 1230
    private val speechRequestCode = 0
    private var textToSpeechLngTo: TextToSpeech? = null
    private var translationCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityTranslateBinding.inflate(layoutInflater)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(binding.root)

        Misc.isItemClicked = true

        Ads.loadAndShowInterstitial(
            this,
            Ads.translateInt,
            AdIds.interstitialAdIdAdMobTranslate
        )

        if (intent.getStringExtra(Misc.key) != null) {
            if (intent.getStringExtra(Misc.key) != "") {
                binding.etText.setText(intent.getStringExtra(Misc.key))
                binding.llPBTranslateFrag.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    translateNow(binding.etText.text.toString())
                }, 500)
            }
        }

        initializeAnimation()

        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            binding.etText.setText(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
            binding.btnTranslate.visibility = View.VISIBLE
            binding.btnClearText.visibility = View.VISIBLE
        }

        Ads.loadAndShowNativeAd(
            this,
            AdIds.nativeAdIdAdMobTranslate,
            Ads.translateNative,
            binding.nativeAdFrameLayoutInBetween,
            if (Ads.translateNative.contains("splash"))
                R.layout.shimmer_native_splash else
                R.layout.small_native_shimmer,
        )



        if (!Misc.isNightModeOn(this)) {
            binding.etText.setTextColor(Color.BLACK)
        }

        binding.etText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            binding.clTranslation.setBackgroundResource(
                if (hasFocus)
                    R.drawable.bg_accent_bordered_less_rounded else R.drawable.bg_translate_dashboard
            )
        }


        binding.scrollView.isSmoothScrollingEnabled = true
        KeyboardUtils.addKeyboardToggleListener(this) { isVisible ->
            if (isVisible) {
                Misc.zoomOutView(binding.btnSpeakInput, this, 150)
                Misc.zoomOutView(binding.btnCopyInput, this, 150)
                Handler().postDelayed({
                    binding.btnSpeakInput.visibility = View.GONE
                    binding.btnCopyInput.visibility = View.GONE
                }, 150)

                if (!isBtnTranslateVisible) {
                    binding.btnTranslate.visibility = View.VISIBLE
                    Misc.zoomInView(binding.btnTranslate, this, 150)
                    isBtnTranslateVisible = true
                }

            } else {
                binding.etText.clearFocus()
                if (binding.etText.text.toString() == "") {
                    Misc.zoomOutView(binding.btnTranslate, this, 150)
                    Handler().postDelayed({
                        binding.btnTranslate.visibility = View.INVISIBLE
                    }, 150)

                    isBtnTranslateVisible = false
                }
                Misc.zoomInView(binding.btnSpeakInput, this, 150)
                Misc.zoomInView(binding.btnCopyInput, this, 150)
            }
        }

        binding.textViewTextTranslatedFrag.doOnTextChanged { _, _, _, _ ->
            binding.llPBTranslateFrag.visibility = View.GONE
        }

        binding.btnCopyTextTranslatedFrag.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "TranslatorPro", binding.textViewTextTranslatedFrag.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        binding.btnCopyInput.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "TranslatorPro", binding.etText.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        binding.etText.doOnTextChanged { text, start, before, count ->
            isBtnTranslateVisible = if (binding.etText.text.toString() == "") {
                Misc.zoomOutView(binding.btnClearText, this, 150)
                Handler().postDelayed({
                    binding.btnClearText.visibility = View.INVISIBLE
                }, 150)
                false
            } else {
                if (before == 0) Misc.zoomInView(binding.btnClearText, this, 150)
                true
            }
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(
                Intent(
                    this@TranslateActivity, SettingsActivity::class.java
                )
            )
        }

        binding.btnClearText.setOnClickListener {
            binding.etText.setText("")
            binding.textViewTextTranslatedFrag.text = ""
            binding.btnClearText.visibility = View.INVISIBLE
            Misc.zoomOutView(binding.btnClearText, this, 150)
            Misc.zoomOutView(binding.btnTranslate, this, 150)
            Handler().postDelayed({
                binding.btnClearText.visibility = View.INVISIBLE
                binding.btnTranslate.visibility = View.INVISIBLE

            }, 150)

            binding.etText.clearFocus()
            KeyboardUtils.forceCloseKeyboard(binding.etText)

//            binding.llText.setBackgroundResource(R.drawable.bg_main_less_rounded)

            if (isLLTranslateVisible) {
                Misc.zoomOutView(binding.clTranslatedText, this, 150)

                Handler().postDelayed({
                    binding.clTranslatedText.visibility = View.GONE
                }, 150)
                isLLTranslateVisible = false
            }
        }

        binding.btnSpeakTranslation.setOnClickListener {
            speakLngTo()
        }

        binding.btnShareTranslation.setOnClickListener {
            if (binding.textViewTextTranslatedFrag.text != "") {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, binding.textViewTextTranslatedFrag.text)
                startActivity(Intent.createChooser(sharingIntent, "Share."))
            }
        }

        binding.btnTranslate.setOnClickListener {
            if (binding.etText.text.toString() != "") {
                binding.llPBTranslateFrag.visibility = View.VISIBLE
                Handler().postDelayed({
                    translateNow(binding.etText.text.toString())
                }, 100)
            }
        }

        binding.btnSwitchLngs.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 150
                rotate.interpolator = LinearInterpolator()
                val image = binding.btnSwitchLngs
                image.startAnimation(rotate)

                Misc.zoomOutView(binding.llLanguageTo, this, 150)
                Misc.zoomOutView(binding.llLanguageFrom, this, 150)

                Handler().postDelayed({
                    val temp = Misc.getLanguageFrom(this)
                    Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                    Misc.setLanguageTo(this, temp)

                    setSelectedLng()

                    if (binding.etText.text.toString() != "") {
                        binding.llPBTranslateFrag.visibility = View.VISIBLE
                        Handler().postDelayed({
                            translateNow(binding.etText.text.toString())
                        }, 150)
                    }
                    Misc.zoomInView(binding.llLanguageTo, this, 150)
                    Misc.zoomInView(binding.llLanguageFrom, this, 150)

                }, 150)
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.please_select_language_you_want_to_translate),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.llLanguageFrom.setOnClickListener {
            val intent = Intent(this@TranslateActivity, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorRequestCode)
        }

        binding.llLanguageTo.setOnClickListener {
            startActivityForResult(
                Intent(
                    this@TranslateActivity, LanguageSelectorActivity::class.java
                ), lngSelectorRequestCode
            )
        }


        binding.etText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val inputManager: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                if (this.currentFocus != null) {
                    inputManager.hideSoftInputFromWindow(
                        this.currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
                    )
                }
                binding.llPBTranslateFrag.visibility = View.VISIBLE
//                if (Misc.getPurchasedStatus(this)) {
//                    translate(binding.etText.text.toString())
//                } else {
                Handler().postDelayed({
                    translateNow(binding.etText.text.toString())
                }, 1)
//                }
                true
            } else false
        }

        binding.btnSpeakInput.setOnClickListener {
            Firebase.analytics.logEvent("BtnSpeakInput", null)
            displaySpeechRecognizer()
        }
    }

    private fun initializeAnimation() {
        Misc.zoomOutView(binding.btnTranslate, this, 0)
        binding.btnTranslate.visibility = View.INVISIBLE
        Misc.zoomOutView(binding.btnClearText, this, 0)
        binding.btnClearText.visibility = View.INVISIBLE

        Misc.zoomOutView(binding.clTranslatedText, this, 0)
        binding.clTranslatedText.visibility = View.GONE
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
                    binding.textViewTextTranslatedFrag.text.toString(),
                    TextToSpeech.QUEUE_FLUSH,
                    null
                )
            }
        }
    }

    private fun saveInHistory(text: String, translation: String) {
        translationCount++
        Log.d(Misc.logKey, "$translationCount Translation Count")
        if (text.length > 1000) {
            return
        }
        val historyList = Misc.getHistory(this)
        val objHistory = TranslateHistoryClass(
            text, translation, Misc.getLanguageTo(this), Misc.getLanguageFrom(this)
        )
        historyList.add(objHistory)
        val sharedPreferences = getSharedPreferences(
            Misc.history, AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(historyList)
        editor.putString(Misc.history, json)
        editor.apply()
        if (isBtnTranslateVisible) {
            Misc.zoomOutView(binding.btnTranslate, this, 150)
            Handler().postDelayed({
                binding.btnTranslate.visibility = View.INVISIBLE
            }, 150)
            isBtnTranslateVisible = false
        }
        if (!isLLTranslateVisible) {
            Handler().postDelayed({
//                binding.llText.setBackgroundResource(R.drawable.bg_half_up_rounded)
            }, 140)

            Handler().postDelayed({
                binding.clTranslatedText.visibility = View.VISIBLE
                Misc.zoomInView(binding.clTranslatedText, this, 150)
            }, 150)


            isLLTranslateVisible = true
        }
        Handler().postDelayed({
            binding.scrollView.smoothScrollTo(0, binding.scrollView.height)
        }, 10)


        binding.etText.clearFocus()
        KeyboardUtils.forceCloseKeyboard(binding.etText)
    }

    @SuppressLint("SetTextI18n")
    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            binding.textLngFrom.text = resources.getString(R.string.detect)
            binding.tvLanguageFrom.text = resources.getString(R.string.detect)
            binding.flagFrom.setImageResource(Misc.getFlag(this, "100"))
        } else {
            binding.tvLanguageFrom.text = Locale(Misc.getLanguageFrom(this)).displayName
            binding.textLngFrom.text = Misc.getLanguageFrom(this)

            binding.flagFrom.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))
        }

        binding.flagTo.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
        binding.tvLanguageTo.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
        binding.textLngTo.text = Misc.getLanguageTo(this)
    }

    @SuppressLint("SetTextI18n")
    private fun translateNow(text: String) {
        setSelectedLng()

        binding.llPBTranslateFrag.visibility = View.VISIBLE

        MiscTranslate.translate(this, text, object : TranslationInterface {
            override fun onTranslate(translation: String) {
                binding.textViewTextTranslatedFrag.text = translation
                saveInHistory(text, translation)
            }

            override fun onFailed() {
                binding.llPBTranslateFrag.visibility = View.GONE
                Toast.makeText(
                    this@TranslateActivity,
                    resources.getString(R.string.some_error_occurred_in_translation),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

//        if (translationCount > 3){
//            Ads.loadAndShowRewardedInterstitial(this, Ads.translationRewardedAd, object :InterstitialCallBack{
//                override fun onDismiss() {
//                    translationCount = 0
//                }
//            })
//        }
    }

    private fun onlineTranslation(fromCode: String, toCode: String, text: String) {
        val encoded = URLEncoder.encode(text, "utf-8")
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val translation = withContext(Dispatchers.IO) {
                    val doc =
                        Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                            .timeout(4000)
                            .get()
                    val element = doc.getElementsByClass("result-container")
                    element.text()
                }

                if (translation.isNotEmpty()) {
                    binding.textViewTextTranslatedFrag.text = translation

                    saveInHistory(text, translation)
                } else {
                    binding.llPBTranslateFrag.visibility = View.GONE
                    Toast.makeText(
                        this@TranslateActivity,
                        resources.getString(R.string.some_error_occurred_in_translation),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(Misc.logKey, "it's empty")
                }
            } catch (e: Exception) {
                binding.llPBTranslateFrag.visibility = View.GONE
                Toast.makeText(
                    this@TranslateActivity,
                    resources.getString(R.string.some_error_occurred_in_translation),
                    Toast.LENGTH_SHORT
                ).show()
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
                    binding.etText.setText(spokenText)
                    binding.llPBTranslateFrag.visibility = View.VISIBLE
                    Handler().postDelayed({
                        translateNow(binding.etText.text.toString())
                    }, 1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (requestCode == lngSelectorRequestCode) {
            if (binding.etText.text.toString() != "") {
                binding.llPBTranslateFrag.visibility = View.VISIBLE
                Handler().postDelayed({
                    translateNow(binding.etText.text.toString())
                }, 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
    }

    override fun onBackPressed() {
        if (intent.getStringExtra(Misc.data) != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
