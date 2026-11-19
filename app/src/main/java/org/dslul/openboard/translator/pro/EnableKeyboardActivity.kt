package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityEnableKeyboardBinding
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import java.util.*
import java.lang.Runnable as Runnable1

class EnableKeyboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEnableKeyboardBinding
    private val t = Timer()
    private val handler: Handler = Handler()
    private var isSettingUpKeyboardWillingly = false
    private var wasEnabledOnOpen = false
    private var wasSelectedOnOpen = false
    private var isEnabledLogged = false
    private var isSelectedLogged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityEnableKeyboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Only a change made on this screen counts as enabling / selecting: a keyboard that was
        // already on when the screen opened logs nothing.
        wasEnabledOnOpen = checkKeyboardActivation()
        wasSelectedOnOpen = isInputMethodSelected()

        Firebase.analytics.logEvent("EnableKeyboard", null)

        binding.tvSkip.setOnClickListener {
            finish()
        }

        if (intent.getStringExtra(Misc.data) != null) {
            binding.tvSkip.visibility = View.VISIBLE
        }

        binding.clEnableKeyboard.setOnClickListener {
            startActivityForResult(Intent("android.settings.INPUT_METHOD_SETTINGS"), 0)
        }

        binding.clSelectKeyboard.setOnClickListener {
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
            if (checkKeyboardActivation() && !wasEnabledOnOpen && !isEnabledLogged) {
                isEnabledLogged = true
                AppAnalytics.keyboardEnabled()
            }
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
                    if (!wasSelectedOnOpen && !isSelectedLogged) {
                        isSelectedLogged = true
                        AppAnalytics.keyboardSelected()
                    }
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


        Handler().postDelayed({
            handler.post(runMainBanner)
        }, 999)
    }

    /** Updates the steps for the keyboard's current state; returns whether it is enabled. */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun checkKeyboardActivation(): Boolean {
        val packageLocal = packageName

        val mInputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val lists: String = mInputMethodManager.enabledInputMethodList.toString()
        val mActKeyboard = lists.contains(packageLocal)

        if (mActKeyboard) {
            binding.clEnableKeyboard.setOnClickListener {
                Toast.makeText(this, R.string.step_1_completed, Toast.LENGTH_SHORT).show()
            }

            binding.clEnableKeyboard.background =
                resources.getDrawable(R.drawable.bg_main_rounded)

            binding.tvHintMeaning.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.gray_400);
            binding.clEnableKeyboard.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.disable_color);

            binding.textEnable.setTextColor(resources.getColor(R.color.gray_700))
            binding.tvHintMeaning.setTextColor(resources.getColor(R.color.gray_700))

            binding.clSelectKeyboard.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                (getSystemService(getString(R.string.input_method)) as InputMethodManager).showInputMethodPicker()
            }, 100)
            isSettingUpKeyboardWillingly = true

            binding.clSelectKeyboard.setOnClickListener {
                (getSystemService(
                    getString(R.string.input_method)
                ) as InputMethodManager).showInputMethodPicker()
                isSettingUpKeyboardWillingly = true
            }
        }
        binding.clEnableKeyboard.setPadding(dpToPx(8).toInt())
        return mActKeyboard
    }


}

