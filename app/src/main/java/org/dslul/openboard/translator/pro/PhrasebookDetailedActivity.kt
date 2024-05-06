package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.dslul.openboard.translator.pro.classes.Misc

import kotlinx.android.synthetic.main.activity_phrasebook_detailed.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.guru.translate.translator.pro.translation.keyboard.translator.R
import org.dslul.openboard.translator.pro.adaptor.CustomExpandableListAdapter
import org.json.JSONObject
import java.util.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNREACHABLE_CODE")
class PhrasebookDetailedActivity : AppCompatActivity() {

    private val expandableListTitle: ArrayList<String> = ArrayList()
    private val expandableListTitleTranslation: ArrayList<String> = ArrayList()
    private lateinit var expandableListAdapter: ExpandableListAdapter
    private val expandableListDetail: HashMap<String, List<String>> = HashMap()
    private val expandableListDetailTranslation: HashMap<String, List<String>> = HashMap()
    private val lngSelectorLngFrom = 1090
    private val lngSelectorLngTo = 1230
    private val arrExpandableListTitle: ArrayList<String> = ArrayList()
    private lateinit var imagesResId: IntArray
    private val imageResId0 = intArrayOf(
        R.drawable.debit_card,
        R.drawable.stand
    )
    private val imageResId1 = intArrayOf(
        R.drawable.handshake,
        R.drawable.debit_card
    )
    private val imageResId2 = intArrayOf(
        R.drawable.fast_food,
        R.drawable.drink
    )
    private val imageResId3 = intArrayOf(
        R.drawable.cocktail_,
        R.drawable.drink,
        R.drawable.patient
    )
    private val imageResId4 = intArrayOf(
        R.drawable.salary,
        R.drawable.languages,
        R.drawable.directional_sign,
        R.drawable.happy,
        R.drawable.delivery_truck,
        R.drawable.waving_hand
    )
    private val imageResId5 = intArrayOf(
        R.drawable.patient,
        R.drawable.va
    )
    private val imageResId6 = intArrayOf(
        R.drawable.police_hat,
        R.drawable.handshake
    )
    private val imageResId7 = intArrayOf(
        R.drawable.countdown,
        R.drawable.calendar,
        R.drawable.calendar_week,
        R.drawable.clock
    )
    private val imageResId8 = intArrayOf(
        R.drawable.travelling,
        R.drawable.review,
        R.drawable.transport,
        R.drawable.public_transport,
        R.drawable.map,
        R.drawable.arrivals,
        R.drawable.ticket
    )

    private var i: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_phrasebook_detailed)

        Misc.isActivityCreatingFirstTime = true

        i = intent.getIntExtra(Misc.key, 0)
        Log.d(Misc.logKey, intent.getIntExtra(Misc.key, 0).toString())

        setSelectedLng()

        imagesResId = when (i) {
            0 -> {
                imageResId0
            }
            1 -> {
                imageResId1
            }
            2 -> {
                imageResId2
            }
            3 -> {
                imageResId3
            }
            4 -> {
                imageResId4
            }
            5 -> {
                imageResId5
            }
            6 -> {
                imageResId6
            }
            7 -> {
                imageResId7
            }
            else -> {
                imageResId8
            }
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }

        llLanguageFrom.setOnClickListener {
            val intent = Intent(
                this@PhrasebookDetailedActivity,
                LanguageSelectorActivity::class.java
            )
            intent.putExtra("isPhrasebook", true)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorLngFrom)
        }

        llLanguageTo.setOnClickListener {
            startActivityForResult(
                Intent(
                    this@PhrasebookDetailedActivity,
                    LanguageSelectorActivity::class.java
                ),
                lngSelectorLngTo
            )
        }

        ivSwitchLanguages.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 100
                rotate.interpolator = LinearInterpolator()

                val image = ivSwitchLanguages
                image.startAnimation(rotate)

                val temp = Misc.getLanguageFrom(this)
                Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                Misc.setLanguageTo(this, temp)

                Misc.zoomOutView(llLanguageFrom, this, 150)
                Misc.zoomOutView(llLanguageTo, this, 150)

                Handler().postDelayed({
                    setSelectedLng()
                    Misc.zoomInView(llLanguageFrom, this, 150)
                    Misc.zoomInView(llLanguageTo, this, 150)
                }, 150)


                expandableListTitleTranslation.clear()
                expandableListTitle.clear()
                getAllText()

            }
        }

        getAllText()
    }

    private suspend fun getLanguageJson(lan: String): String? {
        return try {
            val sharedPref = getSharedPreferences("SavedLanguages", Context.MODE_PRIVATE)

            var valueString = sharedPref?.getString(lan, null)

            if (valueString != null) {
                Log.d("Getting Language", "Getting value from SP")
                return valueString
            }

            val islandRef = Misc.storage.reference.child("/$lan.json")
            val fiftyKBs: Long = 1024 * 50
            Log.d("Getting Language", "Getting value from FB")
            valueString = String(islandRef.getBytes(fiftyKBs).await())
            sharedPref?.edit()?.putString(lan, valueString)?.apply()

            return valueString
        } catch (e: Exception) {
            e.printStackTrace()
            return "Unable to fetch value, please check your internet."
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            tvLanguageFrom.text =
                "English"
        } else {
            tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
        }
        tvLanguageTo.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
    }

    //@DelicateCoroutinesApi
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == lngSelectorLngFrom) {
            expandableListTitleTranslation.clear()
            expandableListTitle.clear()
            getAllText()
        }
        if (requestCode == lngSelectorLngTo) {
            expandableListTitleTranslation.clear()
            expandableListTitle.clear()
            getAllText()
        }
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
    }


    //@DelicateCoroutinesApi
    private fun getAllText() {
        expandableListDetail.clear()
        expandableListDetailTranslation.clear()
        GlobalScope.launch(Dispatchers.Main) {
            val srcLng =
                if (Misc.getLanguageFrom(this@PhrasebookDetailedActivity) == Misc.defaultLanguage) {
                    "en"
                } else {
                    Misc.getLanguageFrom(this@PhrasebookDetailedActivity)
                }

            try {
                val obj = JSONObject(getLanguageJson(srcLng))

                var i = 0
                for (t in obj.keys()) {
                    if (Misc.phrasebookPosition == i) {
                        for (item in obj.getJSONObject(t).keys()) {
                            arrExpandableListTitle.add(item.toString())
                            val temp: MutableList<String> = ArrayList()
                            temp.clear()
                            for (key in obj.getJSONObject(t)
                                .getJSONObject(item).keys()) {
                                temp.add(obj.getJSONObject(t).getJSONObject(item).getString(key))
                            }
                            expandableListDetail[item] = temp
                            expandableListTitle.add(item)
                        }
                    }
                    i += 1
                }

                val lngTo = Misc.getLanguageTo(this@PhrasebookDetailedActivity)
                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        val objTo = JSONObject(getLanguageJson(lngTo))
                        var i = 0

                        for (t in objTo.keys()) {
                            if (Misc.phrasebookPosition == i) {
                                Log.d(Misc.logKey, t)
                                for (item in objTo.getJSONObject(t).keys()) {
                                    val temp2: MutableList<String> = ArrayList()
                                    temp2.clear()
                                    for (key in objTo.getJSONObject(t).getJSONObject(item).keys()) {
                                        temp2.add(
                                            objTo.getJSONObject(t).getJSONObject(item)
                                                .getString(key)
                                        )
                                    }
                                    expandableListDetailTranslation[item] = temp2
                                    expandableListTitleTranslation.add(item)
                                }
                            }
                            i += 1
                        }

                        expandableListAdapter = CustomExpandableListAdapter(
                            this@PhrasebookDetailedActivity,
                            expandableListTitle,
                            expandableListTitleTranslation,
                            expandableListDetailTranslation,
                            expandableListDetail,
                            imagesResId
                        )
                        expandableListView!!.setAdapter(expandableListAdapter)
                        expandableListView!!.setGroupIndicator(null)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@PhrasebookDetailedActivity,
                            "Sorry! ${Locale(lngTo).displayName} is not available in phrasebook yet.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: java.lang.Exception) {
                Toast.makeText(
                    this@PhrasebookDetailedActivity,
                    "Sorry! ${Locale(srcLng).displayName} is not available in phrasebook yet.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}