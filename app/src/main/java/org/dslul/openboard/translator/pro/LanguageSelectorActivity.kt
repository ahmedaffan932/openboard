package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.text.Html
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.admob.BannerAds
import org.dslul.openboard.translator.pro.classes.admob.NativeAds
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.android.synthetic.main.activity_language_selector.*
import kotlinx.android.synthetic.main.activity_language_selector.bannerFrame
import kotlinx.android.synthetic.main.activity_language_selector.nativeAdFrameLayoutInBetween
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.adaptor.LanguagesAdapter
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import org.dslul.openboard.translator.pro.interfaces.LoadInterstitialCallBack
import java.util.*

class LanguageSelectorActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private val arr = ArrayList<String>()
    lateinit var adapter: LanguagesAdapter
    var showingInterstitial = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selector)

        val frameLayout = if (Misc.isLanguageSelectorInBetweenNativeEnabled) {
            nativeAdFrameLayoutInBetween
        } else {
            nativeAdFrame
        }

        NativeAds.loadNativeAd(this, object : LoadInterstitialCallBack {
            override fun onLoaded() {
                NativeAds.manageShowNativeAd(
                    this@LanguageSelectorActivity,
                    Misc.splashNativeAm,
                    frameLayout
                )
            }
        })

        if (intent?.flags == Intent.FLAG_ACTIVITY_NEW_TASK && !Misc.getPurchasedStatus(this) && InterstitialAd.interAdmob == null) {
            val objDialog = Misc.LoadingAdDialog(this)
            objDialog.setCancelable(false)
            objDialog.show()

            objDialog.findViewById<TextView>(R.id.warning).visibility = View.VISIBLE
            Misc.anyAdLoaded.observeForever { t ->
                try {
                    if (t) {
                        if (!showingInterstitial) {
                            InterstitialAd.showInterstitial(this, Misc.getAppOpenIntAm(this))
                        }
                        showingInterstitial = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            object : CountDownTimer(1500, 3000) {
                override fun onTick(p0: Long) {}
                override fun onFinish() {
                    if (objDialog.isShowing) {
                        objDialog.dismiss()
                    }
                }
            }.start()
        }

        Firebase.analytics.logEvent("LanguageSelectorActivity", null)

        Misc.isLngTo = intent.getBooleanExtra(Misc.lngTo, true)

        setBackground()

        NativeAds.manageShowNativeAd(this, Misc.languageSelectorNativeAm, frameLayout)

        for (lng in TranslateLanguage.getAllLanguages()) {
            Log.d(Misc.logKey, lng)
            arr.add(lng)
        }

        textViewLngTo.text = Locale(Misc.getLanguageTo(this)).displayName
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            textViewLngFrom.text = "Detect"
        } else {
            textViewLngFrom.text = Locale(Misc.getLanguageFrom(this)).displayName
        }

        if (intent.getStringExtra(Misc.data) != null) {
            textViewLngFrom.visibility = View.GONE
        } else if (intent.getStringExtra(Misc.data).toString().contains("ml")) {
            textViewLngTo.visibility = View.GONE
            Misc.isLngTo = false
        }

        val arrRecent = ArrayList<String>()
        for (i in Misc.getRecentLngs(this).indices) {
            if (i > 1) {
                break
            }
            arrRecent.add(Misc.getRecentLngs(this)[i])
            textViewRecentlyUsedLng.visibility = View.VISIBLE
        }


        recyclerViewRecentLanguages.layoutManager = LinearLayoutManager(this)
        recyclerViewRecentLanguages.adapter =
            LanguagesAdapter(arrRecent, intent.getBooleanExtra(Misc.lngTo, true), this)

        recyclerViewLanguages.layoutManager = LinearLayoutManager(this)
        adapter = LanguagesAdapter(arr, intent.getBooleanExtra(Misc.lngTo, true), this)
        recyclerViewLanguages.adapter = adapter

        simpleSearchView.setQueryHint(
            Html.fromHtml(
                "<font color = #BDBDBD>" + "Search Language" + "</font>"
            )
        )

        btnBackLanguages.setOnClickListener {
            onBackPressed()
        }
        simpleSearchView.setOnQueryTextListener(this);

        textViewLngFrom.setOnClickListener {
            if (Misc.isLngTo) {
                Misc.isLngTo = !Misc.isLngTo

                zoom()
            }

        }

        textViewLngTo.setOnClickListener {
            if (!Misc.isLngTo) {
                Misc.isLngTo = !Misc.isLngTo

                zoom()
            }
        }

    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        val inputManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (currentFocus != null) {
            inputManager.hideSoftInputFromWindow(
                currentFocus!!.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        adapter.filter.filter(newText)
        recyclerViewLanguages.adapter = adapter
        return true
    }

    private fun setBackground() {
        if (Misc.isLngTo) {
            textViewLngFrom.backgroundTintList = textViewLngTo.backgroundTintList
            textViewLngTo.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.accent))
            textViewLngTo.setTextColor(resources.getColor(R.color.black))
            textViewLngFrom.setTextColor(resources.getColor(R.color.white))
        } else {
            if (!intent.getBooleanExtra("isPhrasebook", false)) {
                arr.add(Misc.defaultLanguage)
            }
            textViewLngTo.backgroundTintList = textViewLngFrom.backgroundTintList
            textViewLngFrom.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.accent))
            textViewLngFrom.setTextColor(resources.getColor(R.color.black))
            textViewLngTo.setTextColor(resources.getColor(R.color.white))
        }
    }

    private fun zoom() {
        val a: Animation =
            AnimationUtils.loadAnimation(this, R.anim.fade_out)
        a.duration = 150
        ll.startAnimation(a)
        Handler().postDelayed({
            setBackground()
            val a: Animation =
                AnimationUtils.loadAnimation(this, R.anim.fade_in)
            a.duration = 150
            ll.startAnimation(a)
        }, 150)
    }

    override fun onResume() {
        super.onResume()
        BannerAds.show(bannerFrame)
    }
}