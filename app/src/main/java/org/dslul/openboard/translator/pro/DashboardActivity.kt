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
import com.google.android.gms.ads.AdView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityDashboardBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.ads.Ads

class DashboardActivity : AppCompatActivity() {
    lateinit var binding: ActivityDashboardBinding

    @SuppressLint("SetTextI18n", "RemoteViewLayout")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showNativeAd()

        Misc.setIsFirstTime(this, false)

        Firebase.analytics.logEvent("Dashboard", null)

        World.init(applicationContext)
        Misc.selectThemeMode(this)

        Misc.isActivityCreatingFirstTime = true

        binding.llKeyboard.setOnClickListener {
            if (isInputMethodSelected()) {
                Toast.makeText(this, "Keyboard is already enabled.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, EnableKeyboardActivity::class.java))
            }
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, DisplayHistoryActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.llVoice.setOnClickListener {
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
    }

}