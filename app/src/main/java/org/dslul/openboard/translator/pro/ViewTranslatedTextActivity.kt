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
import org.dslul.openboard.translator.pro.classes.Misc
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
    lateinit var binding: ActivityViewTranslatedTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        binding.btnCopyInput.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Camera Translator", binding.etText.text
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

        binding.btnClearText.setOnClickListener {
            binding.etText.setText("")
            binding.textViewTextTranslatedFrag.text = ""
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
                    this, "Please select language you want to translate from.", Toast.LENGTH_SHORT
                ).show()
            }
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

        MiscTranslate.translate(this, text, object : TranslationInterface{
            override fun onTranslate(translation: String) {
                binding.textViewTextTranslatedFrag.text = translation
                saveInHistory(text, translation)
            }

            override fun onFailed() {
                binding.llPBTranslateFrag.visibility = View.GONE
                Toast.makeText(
                    this@ViewTranslatedTextActivity,
                    getString(R.string.some_error_occurred_in_translation_please_try_again_later),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            binding.tvLanguageFrom.text = "Detect"
            binding.textLngFrom.text = "Detect"
            binding.flagFrom.setImageResource(Misc.getFlag(this, "100"))
        } else {
            binding.tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
            binding.textLngFrom.text = Misc.getLanguageFrom(this)

            binding.flagFrom.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))
        }

        binding.flagTo.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
        binding.tvLanguageTo.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
        binding.textLngTo.text = Misc.getLanguageTo(this)
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