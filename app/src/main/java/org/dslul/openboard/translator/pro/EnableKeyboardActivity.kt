package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_enable_keyboard.*
import org.dslul.openboard.inputmethod.latin.LatinIME
import org.dslul.openboard.inputmethod.latin.R
import java.util.*
import java.lang.Runnable as Runnable1

class EnableKeyboardActivity : AppCompatActivity() {
    private val t = Timer()
    private val handler: Handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enable_keyboard)

        clEnableKeyboard.setOnClickListener {
            startActivityForResult(Intent("android.settings.INPUT_METHOD_SETTINGS"), 0)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            checkKeyboardActivation()
        }
    }


    fun isInputMethodSelected(): Boolean {
        val id: String = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        val defaultInputMethod = ComponentName.unflattenFromString(id)
        val myInputMethod = ComponentName(this, LatinIME::class.java)
        return myInputMethod == defaultInputMethod
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runMainBanner)
    }

    private val runMainBanner: Runnable1 by lazy {
        return@lazy object : Runnable1 {
            override fun run() {
                if (isInputMethodSelected()) {
                    finish()
//                    clSelectKeyboard.background =
//                        resources.getDrawable(R.drawable.bg_main_less_rounded)
//                    textSelect.setTextColor(Color.WHITE)
//                    startActivity(Intent(this@EnableKeyboardActivity, DashboardActivity::class.java))
//                    finish()
                } else {
                    clSelectKeyboard.background =
                        resources.getDrawable(R.drawable.bg_accent_bordered_less_rounded)
                }
                handler.postDelayed(this, 1000)
            }
        }
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
            clEnableKeyboard.setOnClickListener { }
            clEnableKeyboard.background =
                resources.getDrawable(R.drawable.bg_main_less_rounded)
            clMain.background = resources.getDrawable(R.drawable.bg_select)

            clSelectKeyboard.setOnClickListener {
                (getSystemService(
                    getString(R.string.input_method)
                ) as InputMethodManager).showInputMethodPicker()
            }
        }
    }
}

