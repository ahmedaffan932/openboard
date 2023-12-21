package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_enable_keyboard.*
import kotlinx.android.synthetic.main.phrase_book_main_item.*
import org.dslul.openboard.inputmethod.latin.LatinIME
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.admob.Ads
import java.util.*
import java.lang.Runnable as Runnable1

class EnableKeyboardActivity : AppCompatActivity() {
    private val t = Timer()
    private val handler: Handler = Handler()
    private var isSettingUpKeyboardWillingly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enable_keyboard)

        Ads.showInterstitial(this, Ads.enableKeyboardInt)

        Firebase.analytics.logEvent("EnableKeyboard", null)

        val sharedPreferences = getSharedPreferences("settingsIsFirstTime", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("settingsIsFirstTime", true)) {
            val editor = sharedPreferences.edit()
            editor.putBoolean("settingsIsFirstTime", false)
            editor.apply()

            val intent = Intent(
                this,
                org.dslul.openboard.inputmethod.latin.settings.SettingsActivity::class.java
            )
            intent.putExtra(Misc.data, Misc.data)
            startActivity(intent)
        }

        tvSkip.setOnClickListener {
            finish()
        }

        if (intent.getStringExtra(Misc.data) != null) {
            tvSkip.visibility = View.VISIBLE
        }

        clEnableKeyboard.setOnClickListener {
            startActivityForResult(Intent("android.settings.INPUT_METHOD_SETTINGS"), 0)
        }

        clSelectKeyboard.setOnClickListener {
            Toast.makeText(
                this,
                "Please Enable Keyboard first and then switch it.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            checkKeyboardActivation()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runMainBanner)
    }

    private val runMainBanner: Runnable1 by lazy {
        return@lazy object : Runnable1 {
            @SuppressLint("UseCompatLoadingForDrawables")
            override fun run() {
                if (isInputMethodSelected()) {
                    finish()
                }
                handler.postDelayed(this, 500)
            }
        }
    }

    private fun vibrateView(view: View, vibrateDistance: Float = 20f, vibrateDuration: Long = 250) {
        val animation = TranslateAnimation(0f, vibrateDistance, 0f, 0f).apply {
            duration = vibrateDuration
            interpolator = CycleInterpolator(10f)

        }
        view.startAnimation(animation)
    }


    fun dpToPx(dp: Int): Float {
        val scale: Float = resources.displayMetrics.density
        return dp * scale + 0.5f
    }

    override fun onResume() {
        super.onResume()
        checkKeyboardActivation()

        Handler().postDelayed({
            handler.post(runMainBanner)
        }, 999)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun checkKeyboardActivation() {
        val packageLocal = packageName

        val mInputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val lists: String = mInputMethodManager.enabledInputMethodList.toString()
        val mActKeyboard = lists.contains(packageLocal)

        if (mActKeyboard) {
            clEnableKeyboard.setOnClickListener {
                Toast.makeText(this, "Step 1 is completed.", Toast.LENGTH_SHORT).show()
            }

            clEnableKeyboard.background =
                resources.getDrawable(R.drawable.bg_main_rounded)

            tvHintMeaning.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray_400);
            clEnableKeyboard.backgroundTintList = ContextCompat.getColorStateList(this, R.color.disable_color);

            textEnable.setTextColor(resources.getColor(R.color.gray_700))
            tvHintMeaning.setTextColor(resources.getColor(R.color.gray_700))

            clSelectKeyboard.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                (getSystemService(getString(R.string.input_method)) as InputMethodManager).showInputMethodPicker()
            }, 100)
            isSettingUpKeyboardWillingly = true

            clSelectKeyboard.setOnClickListener {
                (getSystemService(
                    getString(R.string.input_method)
                ) as InputMethodManager).showInputMethodPicker()
                isSettingUpKeyboardWillingly = true
            }
        }
        clEnableKeyboard.setPadding(dpToPx(8).toInt())
    }


}

