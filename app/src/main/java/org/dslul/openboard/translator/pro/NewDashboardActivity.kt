package org.dslul.openboard.translator.pro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.rw.keyboardlistener.KeyboardUtils
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityNewDashboardBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.ads.Ads
import java.util.Locale

class NewDashboardActivity : AppCompatActivity() {
    lateinit var binding: ActivityNewDashboardBinding
    private var isBtnTranslateVisible = false
    private val speechRequestCode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        Misc.setIsFirstTime(this, false)

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }


        Firebase.analytics.logEvent("Dashboard", null)
        Misc.selectThemeMode(this)

        binding.btnCamera.setOnClickListener {
            startActivity(Intent(this, CameraTranslationActivity::class.java))
        }

        showAds()

        setUpClickListeners()

        translationViewImplementation()

        binding.btnKeyboardTranslate.setOnClickListener {
            if (isInputMethodSelected()) {
                Toast.makeText(this, "Keyboard is already enabled.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, EnableKeyboardActivity::class.java))
            }
        }

        binding.etText.doOnTextChanged { text, start, before, count ->
            isBtnTranslateVisible = if (binding.etText.text.toString() == "") {
                Misc.zoomOutView(binding.btnTranslate, this, 150)
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.btnTranslate.visibility = View.INVISIBLE
                }, 150)
                false
            } else {
                if (!isBtnTranslateVisible)
                    Misc.zoomInView(binding.btnTranslate, this, 150)
                true
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
                    startTranslateActivity(binding.etText.text.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startTranslateActivity(text: String) {
        val intent = Intent(this, TranslateActivity::class.java)
        intent.putExtra(Misc.key, text)
        startActivity(intent)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
//        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//        } else {
//            binding.quitBottomSheet.nativeAdFrameLayoutQuit.visibility = View.GONE
//            Log.d(Misc.logKey, "Bottom sheet clicked.")
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//        }

        startActivity(Intent(this, ExitActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
        Log.d(Misc.logKey, "onResume")
    }

    private fun translationViewImplementation() {
        initializeAnimation()

        if (!Misc.isNightModeOn(this)) {
            binding.etText.setTextColor(Color.BLACK)
        }

        KeyboardUtils.addKeyboardToggleListener(this) { isVisible ->
            if (isVisible) {
                Misc.zoomOutView(binding.btnSpeakInput, this, 150)
                Handler().postDelayed({
                    binding.btnSpeakInput.visibility = View.GONE
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
            }
        }

        setSelectedLng()
        binding.btnTranslate.setOnClickListener {
            startTranslateActivity(binding.etText.text.toString())
        }

        binding.btnSpeakInput.setOnClickListener {
            Firebase.analytics.logEvent("BtnSpeakInput", null)
            displaySpeechRecognizer()
        }
    }

    private fun initializeAnimation() {
        Misc.zoomOutView(binding.btnTranslate, this, 0)
        binding.btnTranslate.visibility = View.INVISIBLE
    }

    private fun showAds() {
        val handler = Handler()
        var count = 0
        val hint = getString(R.string.enter_some_text_to_translate)
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
    }


    private fun setUpClickListeners() {
        binding.btnSettings.setOnClickListener {
            Firebase.analytics.logEvent("Settings", null)
            startActivity(Intent(this@NewDashboardActivity, SettingsActivity::class.java))
        }

        binding.btnHistory.setOnClickListener {
            Firebase.analytics.logEvent("History", null)
            startActivity(
                Intent(
                    this@NewDashboardActivity,
                    DisplayHistoryActivity::class.java
                )
            )
        }

        binding.btnChat.setOnClickListener {
            Firebase.analytics.logEvent("Chat", null)
            startActivity(Intent(this@NewDashboardActivity, ConversationActivity::class.java))
        }

        binding.btnPhrasebook.setOnClickListener {
            Firebase.analytics.logEvent("Phrasebook", null)
            startActivity(Intent(this@NewDashboardActivity, PhrasesActivity::class.java))
        }

    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            binding.textViewLngFromFrag.text = resources.getString(R.string.detect)
            binding.flagFromFrag.setImageResource(Misc.getFlag(this, "100"))
        } else {
            binding.textViewLngFromFrag.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName

            binding.flagFromFrag.setImageResource(Misc.getFlag(this, Misc.getLanguageFrom(this)))
        }

        binding.flagToFrag.setImageResource(Misc.getFlag(this, Misc.getLanguageTo(this)))
        binding.textViewLngToFrag.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName


        binding.llLngFromFrag.setOnClickListener {
            val intent = Intent(this, LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            startActivity(intent)
        }

        binding.llLngToFrag.setOnClickListener {
            startActivity(Intent(this, LanguageSelectorActivity::class.java))
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

                Misc.zoomOutView(binding.llLngToFrag, this, 150)
                Misc.zoomOutView(binding.llLngFromFrag, this, 150)

                Handler().postDelayed({
                    val temp = Misc.getLanguageFrom(this)
                    Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                    Misc.setLanguageTo(this, temp)

                    setSelectedLng()

                    Misc.zoomInView(binding.llLngToFrag, this, 150)
                    Misc.zoomInView(binding.llLngFromFrag, this, 150)

                }, 150)
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.please_select_language_you_want_to_translate),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }


}