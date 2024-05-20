package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Context
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
import kotlinx.android.synthetic.main.activity_phrases.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.adaptor.PhraseBookMainAdapter
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.json.JSONObject
import java.util.*

class PhrasesActivity : AppCompatActivity() {
    private val arrTo = ArrayList<String>()
    private val arrFrom = ArrayList<String>()
    private val lngSelectorLngTo = 1230
    private val lngSelectorLngFrom = 1090

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_phrases)

        Ads.showInterstitial(this, Ads.phraseInt)

        setSelectedLng()

        btnBack.setOnClickListener {
            onBackPressed()
        }

//        btnFavorite.setOnClickListener {
//            startActivity(Intent(this@PhrasesActivity, DisplayFavoritesActivity::class.java))
//        }

        llLanguageFrom.setOnClickListener {
            val intent = Intent(this@PhrasesActivity, LanguageSelectorActivity::class.java)
            intent.putExtra("isPhrasebook", true)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorLngFrom)
        }

        llLanguageTo.setOnClickListener {
            startActivityForResult(
                Intent(this@PhrasesActivity, LanguageSelectorActivity::class.java),
                lngSelectorLngTo
            )
        }

        ivSwitchLanguages.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 200
                rotate.interpolator = LinearInterpolator()

                val image = ivSwitchLanguages
                image.startAnimation(rotate)

                Misc.zoomOutView(llLanguageTo, this, 150)
                Misc.zoomOutView(llLanguageFrom, this, 150)

                val temp = Misc.getLanguageFrom(this)
                Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                Misc.setLanguageTo(this, temp)

                Handler().postDelayed({
                    setSelectedLng()
                    getTextFrom()

                    Misc.zoomInView(llLanguageTo, this, 150)
                    Misc.zoomInView(llLanguageFrom, this, 150)

                }, 150)


            }
            getTextFrom()
        }

        getTextFrom()
    }


    private suspend fun getLanguageJson(lan: String): String? {
        return try {
            val sharedPref = this.getSharedPreferences("SavedLanguages", Context.MODE_PRIVATE)

            var valueString = sharedPref?.getString(lan, null)

            if (valueString != null) {
                Log.d("Getting Language", "Getting value from SP")
                Log.e("Getting Language", valueString)
                llPBPhrasebookFrag.visibility = View.GONE
                return valueString
            }

            val islandRef = Misc.storage.reference.child("/$lan.json")
            val fiftyKBs: Long = 1024 * 50
            Log.d("Getting Language", "Getting value from FB")
            valueString = String(islandRef.getBytes(fiftyKBs).await())
            Log.e("Getting Language", valueString)
            sharedPref?.edit()?.putString(lan, valueString)?.apply()
            llPBPhrasebookFrag.visibility = View.GONE

            valueString
        } catch (e: Exception) {
            llPBPhrasebookFrag.visibility = View.GONE
            e.printStackTrace()
            "Unable to fetch value, please check your internet."
        }
    }

    //@DelicateCoroutinesApi
    private fun getLngTo() {

        try {
            llPBPhrasebookFrag.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.Main) {
                Log.d("Button", "English")
                val srcLng = Misc.getLanguageTo(this@PhrasesActivity)

                try {
                    arrTo.clear()
                    val obj = JSONObject(getLanguageJson(srcLng))

                    for (t in obj.keys()) {
                        arrTo.add(t.toString())
                    }

                } catch (e: java.lang.Exception) {
                    Misc.canWeProceed = false
                    Toast.makeText(
                        this@PhrasesActivity,
                        "Sorry! ${Locale(srcLng).displayName} is not available in phrasebook yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    //@DelicateCoroutinesApi
    @SuppressLint("NotifyDataSetChanged")
    private fun getTextFrom() {
        try {
            llPBPhrasebookFrag.visibility = View.VISIBLE
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

                    recyclerViewPhraseBookMain.layoutManager =
                        LinearLayoutManager(this@PhrasesActivity)
                    recyclerViewPhraseBookMain.adapter =
                        PhraseBookMainAdapter(arrFrom, this@PhrasesActivity)
                    getLngTo()

                } catch (e: java.lang.Exception) {
                    if (recyclerViewPhraseBookMain.adapter != null)
                        recyclerViewPhraseBookMain.adapter!!.notifyDataSetChanged()
                    Misc.canWeProceed = false
                    Toast.makeText(
                        this@PhrasesActivity,
                        "Sorry! ${Locale(srcLng).displayName} is not available in phrasebook yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //@DelicateCoroutinesApi
    @SuppressLint("SetTextI18n")
    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            tvLanguageFrom.text = "English"
        } else {
            tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
        }
        tvLanguageTo.text = Locale(
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