package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.android.synthetic.main.activity_language_selector.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.adaptor.LanguagesAdapter
import org.dslul.openboard.translator.pro.classes.Misc
import java.util.*

class LanguageSelectorActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private val arr = ArrayList<String>()
    lateinit var adapter: LanguagesAdapter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selector)

        Firebase.analytics.logEvent("LanguageSelectorActivity", null)


        Misc.isLngTo = intent.getBooleanExtra(Misc.lngTo, true)

        if (Misc.isLngTo) {
            tvTitle.text = getString(R.string.language_to)
        } else {
            tvTitle.text = getString(R.string.language_from)
        }

        setBackground()

        for (lng in TranslateLanguage.getAllLanguages()) {
            arr.add(lng)
        }

        tvLanguageTo.text = Locale(Misc.getLanguageTo(this)).displayName
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            tvLanguageFrom.text = resources.getString(R.string.detect)
        } else {
            tvLanguageFrom.text = Locale(Misc.getLanguageFrom(this)).displayName
        }

        if (intent.getStringExtra(Misc.data) != null) {
            tvLanguageFrom.visibility = View.GONE
        } else if (intent.getStringExtra(Misc.data).toString().contains("ml")) {
            tvLanguageTo.visibility = View.GONE
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

        btnBack.setOnClickListener {
            onBackPressed()
        }
        simpleSearchView.setOnQueryTextListener(this);

        tvLanguageFrom.setOnClickListener {
            if (Misc.isLngTo) {
                Misc.isLngTo = !Misc.isLngTo

                zoom()
            }

        }

        tvLanguageTo.setOnClickListener {
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
            tvLanguageFrom.backgroundTintList = tvLanguageTo.backgroundTintList
            tvLanguageTo.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.accent))
        } else {
            if (!intent.getBooleanExtra("isPhrasebook", false)) {
                arr.add(Misc.defaultLanguage)
            }
            tvLanguageTo.backgroundTintList = tvLanguageFrom.backgroundTintList
            tvLanguageFrom.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.accent))
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
}