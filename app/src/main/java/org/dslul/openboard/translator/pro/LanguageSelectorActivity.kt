package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityLanguageSelectorBinding
import org.dslul.openboard.translator.pro.adaptor.LanguagesAdapter
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import java.util.*

class LanguageSelectorActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private val arr = ArrayList<String>()
    lateinit var adapter: LanguagesAdapter
    private lateinit var binding: ActivityLanguageSelectorBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        binding = ActivityLanguageSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Firebase.analytics.logEvent("LanguageSelectorActivity", null)

//        AdmobBannerAds.loadCollapsibleBanner(
//            this,
//            AdIds.collapsibleBannerAdIdAdLanguages,
//            Ads.languageSelectorBanner,
//            binding.llCollapsibleBanner
//        )

        Misc.isLngTo = intent.getBooleanExtra(Misc.lngTo, true)

        if (Misc.isLngTo) {
            binding.tvTitle.text = getString(R.string.language_to)
        } else {
            binding.tvTitle.text = getString(R.string.language_from)
        }

        setBackground()

        for (lng in TranslateLanguage.getAllLanguages()) {
            arr.add(lng)
        }

        binding.tvLanguageTo.text = Locale(Misc.getLanguageTo(this)).displayName
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            binding.tvLanguageFrom.text = resources.getString(R.string.detect)
        } else {
            binding.tvLanguageFrom.text = Locale(Misc.getLanguageFrom(this)).displayName
        }

        if (intent.getStringExtra(Misc.data) != null) {
            binding.tvLanguageFrom.visibility = View.GONE
        } else if (intent.getStringExtra(Misc.data).toString().contains("ml")) {
            binding.tvLanguageTo.visibility = View.GONE
            Misc.isLngTo = false
        }

        val arrRecent = ArrayList<String>()
        for (i in Misc.getRecentLngs(this).indices) {
            if (i > 1) {
                break
            }
            arrRecent.add(Misc.getRecentLngs(this)[i])
            binding.textViewRecentlyUsedLng.visibility = View.VISIBLE
        }

        binding.recyclerViewRecentLanguages.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLanguages.layoutManager = LinearLayoutManager(this)

        val modelManager = RemoteModelManager.getInstance()
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val downloadedLanguages = models.map { it.language }

                binding.recyclerViewRecentLanguages.adapter =
                    LanguagesAdapter(
                        arrRecent,
                        intent.getBooleanExtra(Misc.lngTo, true),
                        this,
                        downloadedLanguages
                    )

                binding.recyclerViewLanguages.adapter =
                    LanguagesAdapter(
                        arr,
                        intent.getBooleanExtra(Misc.lngTo, true),
                        this,
                        downloadedLanguages
                    )

            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()

                binding.recyclerViewRecentLanguages.adapter =
                    LanguagesAdapter(
                        arrRecent,
                        intent.getBooleanExtra(Misc.lngTo, true),
                        this
                    )

                binding.recyclerViewLanguages.adapter =
                    LanguagesAdapter(
                        arr,
                        intent.getBooleanExtra(Misc.lngTo, true),
                        this
                    )
            }



        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        binding.simpleSearchView.setOnQueryTextListener(this);

        binding.tvLanguageFrom.setOnClickListener {
            if (Misc.isLngTo) {
                Misc.isLngTo = !Misc.isLngTo

                zoom()
            }

        }

        binding.tvLanguageTo.setOnClickListener {
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
        binding.recyclerViewLanguages.adapter = adapter
        return true
    }

    private fun setBackground() {
        if (Misc.isLngTo) {
            binding.tvLanguageFrom.backgroundTintList = binding.tvLanguageTo.backgroundTintList
            binding.tvLanguageTo.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.accent))
        } else {
            if (!intent.getBooleanExtra("isPhrasebook", false)) {
//                arr.add(Misc.defaultLanguage)
            }
            binding.tvLanguageTo.backgroundTintList = binding.tvLanguageFrom.backgroundTintList
            binding.tvLanguageFrom.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.accent))
        }
    }

    private fun zoom() {
        val a: Animation =
            AnimationUtils.loadAnimation(this, R.anim.fade_out)
        a.duration = 150
        binding.ll.startAnimation(a)
        Handler().postDelayed({
            setBackground()
            val a: Animation =
                AnimationUtils.loadAnimation(this, R.anim.fade_in)
            a.duration = 150
            binding.ll.startAnimation(a)
        }, 150)
    }
}