package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.translator.pro.classes.Misc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPhrasesBinding
import org.dslul.openboard.translator.pro.adaptor.PhraseBookMainAdapter
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.PhrasesAssetReader
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.json.JSONObject
import java.util.*

class PhrasesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhrasesBinding
    private val arrTo = ArrayList<String>()
    private val arrFrom = ArrayList<String>()
    private val lngSelectorLngTo = 1230
    private val lngSelectorLngFrom = 1090

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityPhrasesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSelectedLng()

        binding.llLanguageFrom.setOnClickListener {
            val intent = Intent(this@PhrasesActivity, LanguageSelectorActivity::class.java)
            intent.putExtra("isPhrasebook", true)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorLngFrom)
        }

        binding.llLanguageTo.setOnClickListener {
            startActivityForResult(
                Intent(this@PhrasesActivity, LanguageSelectorActivity::class.java),
                lngSelectorLngTo
            )
        }

        binding.ivSwitchLanguages.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 200
                rotate.interpolator = LinearInterpolator()

                val image = binding.ivSwitchLanguages
                image.startAnimation(rotate)

                Misc.zoomOutView(binding.llLanguageTo, this, 150)
                Misc.zoomOutView(binding.llLanguageFrom, this, 150)

                val temp = Misc.getLanguageFrom(this)
                Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                Misc.setLanguageTo(this, temp)

                Handler().postDelayed({
                    setSelectedLng()
                    getTextFrom()

                    Misc.zoomInView(binding.llLanguageTo, this, 150)
                    Misc.zoomInView(binding.llLanguageFrom, this, 150)

                }, 150)


            }
            getTextFrom()
        }

        getTextFrom()
    }


    private suspend fun getLanguageJson(lan: String): String {
        return PhrasesAssetReader.getLanguageJson(this, lan)
    }

    //@DelicateCoroutinesApi
    private fun getLngTo() {

        try {
            binding.llPBPhrasebookFrag.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.Main) {
                Log.d("Button", "English")
                val srcLng = Misc.getLanguageTo(this@PhrasesActivity)

                try {
                    arrTo.clear()
                    val obj = JSONObject(getLanguageJson(srcLng))

                    for (t in obj.keys()) {
                        arrTo.add(t.toString())
                    }
                    binding.llPBPhrasebookFrag.visibility = View.GONE

                } catch (e: java.lang.Exception) {
                    binding.llPBPhrasebookFrag.visibility = View.GONE
                    Misc.canWeProceed = false
                    Toast.makeText(
                        this@PhrasesActivity,
                        "Sorry! ${Locale(srcLng).displayName} is not available in phrasebook yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            binding.llPBPhrasebookFrag.visibility = View.GONE
            e.printStackTrace()
        }

    }

    //@DelicateCoroutinesApi
    @SuppressLint("NotifyDataSetChanged")
    private fun getTextFrom() {
        try {
            binding.llPBPhrasebookFrag.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.Main) {
                Log.d("Button", "English")
                val srcLng =
                    if (Misc.getLanguageFrom(this@PhrasesActivity) == Misc.defaultLanguage) {
                        "en"
                    } else {
                        Misc.getLanguageFrom(this@PhrasesActivity)
                    }

                try {
                    arrFrom.clear()
                    val obj = JSONObject(getLanguageJson(srcLng))

                    for (t in obj.keys()) {
                        arrFrom.add(t.toString())
                    }

                    binding.recyclerViewPhraseBookMain.layoutManager =
                        LinearLayoutManager(this@PhrasesActivity)
                    binding.recyclerViewPhraseBookMain.adapter =
                        PhraseBookMainAdapter(arrFrom, this@PhrasesActivity)
                    getLngTo()

                } catch (e: java.lang.Exception) {
                    binding.llPBPhrasebookFrag.visibility = View.GONE
                    if (binding.recyclerViewPhraseBookMain.adapter != null)
                        binding.recyclerViewPhraseBookMain.adapter!!.notifyDataSetChanged()
                    Misc.canWeProceed = false
                    Toast.makeText(
                        this@PhrasesActivity,
                        "Sorry! ${Locale(srcLng).displayName} is not available in phrasebook yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        } catch (e: Exception) {
            binding.llPBPhrasebookFrag.visibility = View.GONE
            e.printStackTrace()
        }
    }

    //@DelicateCoroutinesApi
    @SuppressLint("SetTextI18n")
    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            binding.tvLanguageFrom.text = "English"
        } else {
            binding.tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
        }
        binding.tvLanguageTo.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
        Misc.canWeProceed = true
        getTextFrom()
    }

    //@DelicateCoroutinesApi
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == lngSelectorLngFrom) {
            Misc.canWeProceed = true
            setSelectedLng()
            getTextFrom()
        }

        if (requestCode == lngSelectorLngTo) {
            Misc.canWeProceed = true
            setSelectedLng()
            getTextFrom()
        }
    }

    override fun onResume() {
        super.onResume()
        Misc.canWeProceed = true
    }
}
