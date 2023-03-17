package org.dslul.openboard.translator.pro

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.dslul.openboard.translator.pro.classes.Misc
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.android.synthetic.main.activity_multiple_language_selector.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.adaptor.MultipleLanguageSelectorAdaptor
import org.dslul.openboard.translator.pro.adaptor.SelectedLanguagesAdaptor
import org.dslul.openboard.translator.pro.interfaces.OnItemClick


class MultipleLanguageSelectorActivity : AppCompatActivity() {
    private val arr = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_multiple_language_selector)

        Firebase.analytics.logEvent("LanguageSelectorActivity", null)
        Misc.isLngTo = intent.getBooleanExtra(Misc.lngTo, true)

        for (lng in TranslateLanguage.getAllLanguages()) {
            Log.d(Misc.logKey, lng)
            arr.add(lng)
        }

        rvSelectedLanguages.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        var adapterSelectedLanguages: SelectedLanguagesAdaptor? = null

        adapterSelectedLanguages = SelectedLanguagesAdaptor(this, object : OnItemClick {
            override fun onClick() {
                val adapter = MultipleLanguageSelectorAdaptor(
                    this@MultipleLanguageSelectorActivity,
                    arr,
                    Misc.getMultipleSelectedLanguages(this@MultipleLanguageSelectorActivity),
                    object : OnItemClick {
                        override fun onClick() {
                            val arrSelectedLanguages =
                                Misc.getMultipleSelectedLanguages(this@MultipleLanguageSelectorActivity)
                            rvSelectedLanguages.adapter = adapterSelectedLanguages
                            adapterSelectedLanguages?.setData(arrSelectedLanguages)
                            rvSelectedLanguages.smoothScrollToPosition(arrSelectedLanguages.size - 1)
                            setNumberOfSelectedLanguages(arrSelectedLanguages.size.toString())
                        }
                    })
                setNumberOfSelectedLanguages(Misc.getMultipleSelectedLanguages(this@MultipleLanguageSelectorActivity).size.toString())
                recyclerViewLanguages.adapter = adapter
            }
        })

        rvSelectedLanguages.adapter = adapterSelectedLanguages
        val arrSelectedLanguages = Misc.getMultipleSelectedLanguages(this)
        adapterSelectedLanguages.setData(arrSelectedLanguages)
        setNumberOfSelectedLanguages(arrSelectedLanguages.size.toString())

        recyclerViewLanguages.layoutManager = LinearLayoutManager(this)
        val adapter = MultipleLanguageSelectorAdaptor(
            this,
            arr,
            Misc.getMultipleSelectedLanguages(this),
            object : OnItemClick {
                override fun onClick() {
                    val arrSelectedLanguages =
                        Misc.getMultipleSelectedLanguages(this@MultipleLanguageSelectorActivity)
                    rvSelectedLanguages.adapter = adapterSelectedLanguages
                    adapterSelectedLanguages.setData(arrSelectedLanguages)
                    rvSelectedLanguages.smoothScrollToPosition(arrSelectedLanguages.size - 1)
                    setNumberOfSelectedLanguages(arrSelectedLanguages.size.toString())
                }
            })
        recyclerViewLanguages.adapter = adapter

        btnBackLanguages.setOnClickListener {
            onBackPressed()
        }
    }

    fun setNumberOfSelectedLanguages(number: String) {
        tvNumberOfSelectedLanguages.text = number
    }
}