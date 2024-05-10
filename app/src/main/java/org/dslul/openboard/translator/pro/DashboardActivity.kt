package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import com.blongho.country_data.World
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.dslul.openboard.inputmethod.keyboard.Keyboard
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.admob.Ads

class DashboardActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n", "RemoteViewLayout")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        showNativeAd()

        Misc.setIsFirstTime(this, false)

        Firebase.analytics.logEvent("Dashboard", null)

        World.init(applicationContext)
        Misc.selectThemeMode(this)

        Misc.isActivityCreatingFirstTime = true

        llKeyboard.setOnClickListener {
            if (isInputMethodSelected()) {
                Toast.makeText(this, "Keyboard is already enabled.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, EnableKeyboardActivity::class.java))
            }
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, DisplayHistoryActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        llVoice.setOnClickListener {
            val intent = Intent(this, TranslateActivity::class.java)
            val pairs = arrayOf<Pair<View, String>>(
//                Pair(binding.clTopBar, "anim_cl_languages"),
//                Pair(binding.btnMenu, "anim_btn_menu"),
//                Pair(binding.ivVoice, "anim_iv_voice")
            )
            intent.putExtra("isVoiceTranslation", true)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *pairs)
            startActivity(intent, options.toBundle())
        }

        findViewById<View>(R.id.llTextTranslate).setOnClickListener {
            val intent = Intent(this, TranslateActivity::class.java)
            val pairs = arrayOf<Pair<View, String>>(
//                Pair(binding.etText, "anim_et_main"),
//                Pair(binding.clTopBar, "anim_cl_languages"),
//                Pair(binding.etText, "anim_et_main")
            )
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *pairs)
            startActivity(intent, options.toBundle())
        }

        findViewById<View>(R.id.llPhrases).setOnClickListener {
            val intent = Intent(this, PhrasesActivity::class.java)
            val pairs = arrayOf<Pair<View, String>>(
//                Pair(binding.etText, "anim_et_main"),
//                Pair(binding.clTopBar, "anim_cl_languages"),
//                Pair(binding.etText, "anim_et_main")
            )
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *pairs)
            startActivity(intent, options.toBundle())
        }


        findViewById<View>(R.id.llCamera).setOnClickListener {
            val intent = Intent(this, CameraTranslationActivity::class.java)
            val pairs = arrayOf<Pair<View, String>>(
//                Pair(binding.ivCamera, "anim_camera_icon"),
//                Pair(binding.clTopBar, "anim_cl_languages")
            )
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *pairs)
            startActivity(intent, options.toBundle())
        }

        findViewById<View>(R.id.llConversation).setOnClickListener {
            val intent = Intent(this, ConversationActivity::class.java)
            val pairs = arrayOf<Pair<View, String>>(
//                Pair(binding.ivCamera, "anim_camera_icon"),
//                Pair(binding.clTopBar, "anim_cl_languages")
            )
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *pairs)
            startActivity(intent, options.toBundle())
        }

    }

    override fun onBackPressed() {
        startActivity(Intent(this@DashboardActivity, ExitActivity::class.java))
    }


    private fun showNativeAd() {
        Ads.showNativeAd(this, Ads.dashboardNative, findViewById(R.id.nativeAdFrameLayout))
    }

}