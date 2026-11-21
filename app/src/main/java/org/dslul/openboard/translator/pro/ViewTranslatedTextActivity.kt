package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
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
import com.google.gson.Gson
import com.rw.keyboardlistener.KeyboardUtils
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityViewTranslatedTextBinding
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.LanguageSwap
import org.dslul.openboard.translator.pro.classes.ShareUtils
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.MiscTranslate
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import org.dslul.openboard.translator.pro.interfaces.TranslationInterface
import java.util.Locale

class ViewTranslatedTextActivity : AppCompatActivity() {
    private var isNativeAdLoaded: Boolean = false
    var isBtnTranslateVisible = false
    private var isLLTranslateVisible = false
    private val lngSelectorRequestCode = 1230
    private val speechRequestCode = 0
    private var textToSpeechLngTo: TextToSpeech? = null
    private var translationsCount = 0

    /** Bumped for every translation request and swap; a result for an older id is dropped. */
    private var translationRequestId = 0

    /** The language "Detect" resolved to for the current translation, for the swap button. */
    private var detectedSourceLanguage: String? = null

    /** The text / translation pair the result card shows, for its heart (Favorites). */
    private var lastTranslated: TranslateHistoryClass? = null
    lateinit var binding: ActivityViewTranslatedTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityViewTranslatedTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Misc.setIsFirstTime(this, false)

        initializeAnimation()

        Misc.isActivityCreatingFirstTime = true

        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            binding.etText.setText(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
            binding.btnClearText.visibility = View.VISIBLE
        }

        Misc.isActivityCreatingFirstTime = true

        val handler = Handler()
        var count = 0
        val hint = "Tap here to enter text.."
        val runnable: Runnable by lazy {
            return@lazy object : Runnable {
                override fun run() {
                    if (count < hint.length) {
                        binding.etText.hint = hint.substring(0, count)
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
                binding.etText.setText(intent.getStringExtra(Misc.key))
                translateNow(binding.etText.text.toString())
            }
        }, 100)

        binding.etText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        }

        binding.btnFavoriteTranslation.setOnClickListener { toggleFavorite() }


        binding.scrollView.isSmoothScrollingEnabled = true
        KeyboardUtils.addKeyboardToggleListener(this) { isVisible ->
            if (isVisible) {
                if (!isBtnTranslateVisible) {
                    isBtnTranslateVisible = true
                }

            } else {
                binding.etText.clearFocus()
                if (binding.etText.text.toString() == "") {

                    isBtnTranslateVisible = false
                }
            }
        }

        binding.textViewTextTranslatedFrag.doOnTextChanged { _, _, _, _ ->
            binding.llPBTranslateFrag.visibility = View.GONE
        }

        binding.btnCopyTextTranslatedFrag.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Camera Translator", binding.textViewTextTranslatedFrag.text
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }

        binding.btnCopyInput.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Camera Translator", binding.etText.text
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

        binding.btnClearText.setOnClickListener {
            binding.etText.setText("")
            binding.textViewTextTranslatedFrag.text = ""
            lastTranslated = null
            updateFavoriteIcon()
            binding.btnClearText.visibility = View.INVISIBLE
            Misc.zoomOutView(binding.btnClearText, this, 150)
//            Misc.zoomOutView(btnTranslate, this, 150)
            Handler().postDelayed({
                binding.btnClearText.visibility = View.INVISIBLE
//                btnTranslate.visibility = View.INVISIBLE

            }, 150)

            binding.etText.clearFocus()

            KeyboardUtils.forceCloseKeyboard(binding.etText)

//            llText.setBackgroundResource(R.drawable.bg_main_less_rounded)

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
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorRequestCode)
        }

        binding.llLanguageTo.setOnClickListener {
            startActivityForResult(
                Intent(
                    this, LanguageSelectorActivity::class.java
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
//                    translate(etText.text.toString())
//                } else {
                Handler().postDelayed({
                    translateNow(binding.etText.text.toString())
                }, 1)
//                }
                true
            } else false
        }

    }

    private fun initializeAnimation() {
        Misc.zoomOutView(binding.btnClearText, this, 0)
        binding.btnClearText.visibility = View.INVISIBLE

        Misc.zoomOutView(binding.clTranslatedText, this, 0)
        binding.clTranslatedText.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun translateNow(text: String) {
        setSelectedLng()
        binding.llPBTranslateFrag.visibility = View.VISIBLE

        val requestId = ++translationRequestId
        detectedSourceLanguage = null
        MiscTranslate.translate(this, text, object : TranslationInterface{
            override fun onTranslate(translation: String) {
                if (requestId != translationRequestId) return
                AppAnalytics.translateDone(
                    AppAnalytics.FEATURE_TEXT,
                    Misc.getLanguageFrom(this@ViewTranslatedTextActivity),
                    Misc.getLanguageTo(this@ViewTranslatedTextActivity)
                )
                binding.textViewTextTranslatedFrag.text = translation
                lastTranslated = TranslateHistoryClass(
                    text,
                    translation,
                    Misc.getLanguageTo(this@ViewTranslatedTextActivity),
                    Misc.getLanguageFrom(this@ViewTranslatedTextActivity)
                )
                updateFavoriteIcon()
                saveInHistory(text, translation)
            }

            override fun onFailed() {
                if (requestId != translationRequestId) return
                AppAnalytics.translateFailed(
                    AppAnalytics.FEATURE_TEXT,
                    Misc.getLanguageFrom(this@ViewTranslatedTextActivity),
                    Misc.getLanguageTo(this@ViewTranslatedTextActivity)
                )
                binding.llPBTranslateFrag.visibility = View.GONE
                Toast.makeText(
                    this@ViewTranslatedTextActivity,
                    getString(R.string.some_error_occurred_in_translation_please_try_again_later),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }) { detected ->
            if (requestId == translationRequestId) detectedSourceLanguage = detected
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

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
            fromName = getString(R.string.detect)
            binding.tvLanguageFrom.text = fromName
            binding.textLngFrom.text = getString(R.string.detect)
            binding.flagFrom.setImageResource(Misc.getFlag(this, "100"))
        } else {
            fromName = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
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
                    binding.textViewTextTranslatedFrag.text.toString(),
                    TextToSpeech.QUEUE_FLUSH,
                    null
                )
            }
        }
    }

}
