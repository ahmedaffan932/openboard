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
import com.example.translatorguru.ads.admob.LoadAdCallBack
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
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.LanguageSwap
import org.dslul.openboard.translator.pro.classes.ShareUtils
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

    companion object {
        /** With a launch text (Misc.key): read the translation aloud as soon as it arrives. */
        const val EXTRA_SPEAK_RESULT = "speakResult"

        /** Start speech input as soon as the screen opens (the pinned notification's Voice tile). */
        const val EXTRA_START_VOICE = "startVoice"

        /** Back opens the dashboard when this screen is the task root (a cold notification entry). */
        const val EXTRA_DASHBOARD_ON_BACK = "createDashboardOnBack"

        /**
         * Skip the entry interstitial: a notification entry already went through the splash ad,
         * and the pinned notification's tiles open their tool with no ad on the way.
         */
        const val EXTRA_SKIP_ENTRY_AD = "skipEntryAd"
    }

    /** Set from [EXTRA_SPEAK_RESULT] for the launch text only; cleared once it has spoken. */
    private var speakLaunchTranslation = false

    private var isNativeAdLoaded: Boolean = false
    lateinit var binding: ActivityTranslateBinding
    private var isBtnTranslateVisible = false
    private var isLLTranslateVisible = false
    private var isTranslateNativeRequested = false
    private val lngSelectorRequestCode = 1230
    private val speechRequestCode = 0
    private var textToSpeechLngTo: TextToSpeech? = null
    private var translationCount = 0

    /** Bumped for every translation request and swap; a result for an older id is dropped. */
    private var translationRequestId = 0

    /** The language "Detect" resolved to for the current translation, for the swap button. */
    private var detectedSourceLanguage: String? = null

    /** The text / translation pair the result card shows, for its heart (Favorites). */
    private var lastTranslated: TranslateHistoryClass? = null

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

        if (!intent.getBooleanExtra(EXTRA_SKIP_ENTRY_AD, false)) {
            Ads.loadAndShowInterstitial(
                this,
                Ads.translateInt,
                AdIds.interstitialAdIdAdMobTranslate
            )
        }

        // removeExtra only reaches this process's copy: after process death the system restores the
        // screen with its original launch intent, and it must not start talking or listening on its
        // own then.
        speakLaunchTranslation = savedInstanceState == null &&
            intent.getBooleanExtra(EXTRA_SPEAK_RESULT, false) &&
            !intent.getStringExtra(Misc.key).isNullOrBlank()
        intent.removeExtra(EXTRA_SPEAK_RESULT)
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_START_VOICE, false)) {
            intent.removeExtra(EXTRA_START_VOICE)
            displaySpeechRecognizer()
        }

        if (intent.getStringExtra(Misc.key) != null) {
            if (intent.getStringExtra(Misc.key) != "") {
                binding.etText.setText(intent.getStringExtra(Misc.key))
                showTranslateNativeAd()
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

        binding.etText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            binding.clTranslation.setBackgroundResource(
                if (hasFocus)
                    R.drawable.ui_tr_bg_card_r22_focused else R.drawable.bg_ui_card_r22
            )
        }

        binding.btnFavoriteTranslation.setOnClickListener { toggleFavorite() }


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
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }

        binding.btnCopyInput.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "TranslatorPro", binding.etText.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
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
            lastTranslated = null
            updateFavoriteIcon()
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
            ShareUtils.shareText(this, binding.textViewTextTranslatedFrag.text)
        }

        binding.btnTranslate.setOnClickListener {
            if (binding.etText.text.toString() != "") {
                showTranslateNativeAd()
                binding.llPBTranslateFrag.visibility = View.VISIBLE
                Handler().postDelayed({
                    translateNow(binding.etText.text.toString())
                }, 100)
            }
        }

        binding.btnSwitchLngs.setOnClickListener {
            val swapped = LanguageSwap.swap(
                LanguageSwap.State(
                    from = Misc.getLanguageFrom(this),
                    to = Misc.getLanguageTo(this),
                    input = binding.etText.text.toString(),
                    output = if (isLLTranslateVisible) {
                        binding.textViewTextTranslatedFrag.text.toString()
                    } else ""
                ),
                Misc.defaultLanguage,
                detectedSourceLanguage
            )
            if (swapped == null) {
                Toast.makeText(
                    this,
                    resources.getString(R.string.please_select_language_you_want_to_translate),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val rotate = RotateAnimation(
                0F, 180F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
            )
            rotate.duration = 150
            rotate.interpolator = LinearInterpolator()
            binding.btnSwitchLngs.startAnimation(rotate)

            Misc.zoomOutView(binding.llLanguageTo, this, 150)
            Misc.zoomOutView(binding.llLanguageFrom, this, 150)

            Handler(Looper.getMainLooper()).postDelayed({
                applySwap(swapped)
                Misc.zoomInView(binding.llLanguageTo, this, 150)
                Misc.zoomInView(binding.llLanguageFrom, this, 150)
            }, 150)
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

    private fun showTranslateNativeAd() {
        if (isTranslateNativeRequested) return

        isTranslateNativeRequested = true
        Ads.loadAndShowNativeAd(
            activity = this,
            adIds = AdIds.nativeAdIdAdMobTranslate,
            remoteKey = Ads.translateNative,
            frameLayout = binding.nativeAdFrameLayoutInBetween,
            shimmerLayout = if (Ads.translateNative.contains("splash"))
                R.layout.shimmer_native_splash else
                R.layout.small_native_shimmer,
            callBack = object : LoadAdCallBack {
                override fun onFailed() {
                    isTranslateNativeRequested = false
                    binding.nativeAdFrameLayoutInBetween.removeAllViews()
                    binding.nativeAdFrameLayoutInBetween.visibility = View.GONE
                }
            }
        )
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
    /**
     * Swaps the languages and, when there is a translation, the texts too (see [LanguageSwap]):
     * the translation becomes the editable input and the original comes back as the translation,
     * with no new request.
     */
    private fun applySwap(swapped: LanguageSwap.State) {
        // A translation still on its way belongs to the old direction; drop it.
        translationRequestId++
        detectedSourceLanguage = null
        binding.llPBTranslateFrag.visibility = View.GONE
        Misc.setLanguageFrom(this, swapped.from)
        Misc.setLanguageTo(this, swapped.to)
        setSelectedLng()
        binding.etText.setText(swapped.input)
        binding.etText.setSelection(swapped.input.length)
        binding.textViewTextTranslatedFrag.text = swapped.output
        lastTranslated = if (swapped.output.isNotEmpty()) {
            TranslateHistoryClass(swapped.input, swapped.output, swapped.to, swapped.from)
        } else null
        updateFavoriteIcon()
    }

    /** The result card's heart: adds the shown translation to Favorites or takes it out again. */
    private fun toggleFavorite() {
        val entry = lastTranslated ?: return
        val favorites = Misc.getFavorites(this)
        val saved = favorites.firstOrNull { isSameEntry(it, entry) }
        if (saved != null) favorites.remove(saved) else favorites.add(entry)
        getSharedPreferences(Misc.favorites, MODE_PRIVATE).edit()
            .putString(Misc.favorites, Gson().toJson(favorites))
            .apply()
        updateFavoriteIcon()
    }

    private fun updateFavoriteIcon() {
        val entry = lastTranslated
        val isFavorite = entry != null && Misc.getFavorites(this).any { isSameEntry(it, entry) }
        binding.btnFavoriteTranslation.setImageResource(
            if (isFavorite) R.drawable.ic_home_heart_filled else R.drawable.ic_home_heart_outline
        )
    }

    /** Same match as the History list's hearts (HistoryAdapter). */
    private fun isSameEntry(a: TranslateHistoryClass, b: TranslateHistoryClass) =
        a.text == b.text && a.translation == b.translation &&
            a.translatedFrom == b.translatedFrom && a.translatedTo == b.translatedTo

    private fun setSelectedLng() {
        val fromName: String
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            fromName = resources.getString(R.string.detect)
            binding.textLngFrom.text = resources.getString(R.string.detect)
            binding.tvLanguageFrom.text = fromName
            binding.flagFrom.setImageResource(Misc.getFlag(this, "100"))
        } else {
            fromName = Locale(Misc.getLanguageFrom(this)).displayName
            binding.tvLanguageFrom.text = fromName
            binding.textLngFrom.text = Misc.getLanguageFrom(this)

            binding.flagFrom.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))
        }

        val toName = Locale(Misc.getLanguageTo(this)).displayName
        binding.flagTo.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
        binding.tvLanguageTo.text = toName
        binding.textLngTo.text = Misc.getLanguageTo(this)

        // Result card header and the "Translating…" card's language pair.
        binding.ivResultFlag.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
        binding.tvResultLanguage.text = toName
        binding.tvTranslatingPair.text = getString(R.string.ui_tr_language_pair, fromName, toName)
    }

    @SuppressLint("SetTextI18n")
    private fun translateNow(text: String) {
        setSelectedLng()

        binding.llPBTranslateFrag.visibility = View.VISIBLE

        val requestId = ++translationRequestId
        detectedSourceLanguage = null
        MiscTranslate.translate(this, text, object : TranslationInterface {
            override fun onTranslate(translation: String) {
                if (requestId != translationRequestId) return
                AppAnalytics.translateDone(
                    AppAnalytics.FEATURE_TEXT,
                    Misc.getLanguageFrom(this@TranslateActivity),
                    Misc.getLanguageTo(this@TranslateActivity)
                )
                binding.textViewTextTranslatedFrag.text = translation
                lastTranslated = TranslateHistoryClass(
                    text,
                    translation,
                    Misc.getLanguageTo(this@TranslateActivity),
                    Misc.getLanguageFrom(this@TranslateActivity)
                )
                updateFavoriteIcon()
                saveInHistory(text, translation)
                // The Daily notification's "hear it now": the launch translation is read once.
                if (speakLaunchTranslation) {
                    speakLaunchTranslation = false
                    speakLngTo()
                }
            }

            override fun onFailed() {
                if (requestId != translationRequestId) return
                AppAnalytics.translateFailed(
                    AppAnalytics.FEATURE_TEXT,
                    Misc.getLanguageFrom(this@TranslateActivity),
                    Misc.getLanguageTo(this@TranslateActivity)
                )
                speakLaunchTranslation = false
                binding.llPBTranslateFrag.visibility = View.GONE
                Toast.makeText(
                    this@TranslateActivity,
                    resources.getString(R.string.some_error_occurred_in_translation),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }) { detected ->
            if (requestId == translationRequestId) detectedSourceLanguage = detected
        }

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
        } else if (isTaskRoot && intent.getBooleanExtra(EXTRA_DASHBOARD_ON_BACK, false)) {
            // Opened straight from a notification, with nothing of the app under it.
            startActivity(Intent(this, FragmentsDashboardActivity::class.java))
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
