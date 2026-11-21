package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
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
import androidx.lifecycle.lifecycleScope
import org.dslul.openboard.translator.pro.classes.Misc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityPhrasebookDetailedBinding
import org.dslul.openboard.translator.pro.adaptor.CustomExpandableListAdapter
import org.dslul.openboard.translator.pro.adaptor.PhraseBookMainAdapter
import org.dslul.openboard.translator.pro.adaptor.PhraseGroupStyle
import org.dslul.openboard.translator.pro.classes.Misc.setAppLanguage
import org.dslul.openboard.translator.pro.classes.PhrasesAssetReader
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.json.JSONObject
import java.util.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNREACHABLE_CODE")
class PhrasebookDetailedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhrasebookDetailedBinding
    private val expandableListTitle: ArrayList<String> = ArrayList()
    private val expandableListTitleTranslation: ArrayList<String> = ArrayList()
    private lateinit var expandableListAdapter: ExpandableListAdapter
    private val expandableListDetail: HashMap<String, List<String>> = HashMap()
    private val expandableListDetailTranslation: HashMap<String, List<String>> = HashMap()
    private val lngSelectorLngFrom = 1090
    private val lngSelectorLngTo = 1230
    /** This category's group icons and tones (Figma "Phrasebook detail"), set in onCreate. */
    private lateinit var groupStyles: List<PhraseGroupStyle>

    private var i: Int = 0

    /** The From/To pair the list was last loaded for; null until the first load. */
    private var loadedLanguages: Pair<String, String>? = null
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppLanguage()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityPhrasebookDetailedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Ads.loadAndShowSharedBottomNativeAd(this, binding.nativeAdFrameLayout)
        Ads.loadAndShowInterstitial(this, Ads.phraseInt)

        Misc.isActivityCreatingFirstTime = true

        i = intent.getIntExtra(Misc.key, 0)
        Log.d(Misc.logKey, intent.getIntExtra(Misc.key, 0).toString())

        setSelectedLng()

        groupStyles = groupStylesFor(i)

        // The category the user opened, e.g. "At the Restaurant" (already in the From language).
        intent.getStringExtra(Misc.data)?.takeIf { it.isNotBlank() }?.let { binding.tvTitle.text = it }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.llLanguageFrom.setOnClickListener {
            val intent = Intent(
                this@PhrasebookDetailedActivity,
                LanguageSelectorActivity::class.java
            )
            intent.putExtra("isPhrasebook", true)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorLngFrom)
        }

        binding.llLanguageTo.setOnClickListener {
            startActivityForResult(
                Intent(
                    this@PhrasebookDetailedActivity,
                    LanguageSelectorActivity::class.java
                ),
                lngSelectorLngTo
            )
        }

        binding.ivSwitchLanguages.setOnClickListener {
            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 100
                rotate.interpolator = LinearInterpolator()

                val image = binding.ivSwitchLanguages
                image.startAnimation(rotate)

                val temp = Misc.getLanguageFrom(this)
                Misc.setLanguageFrom(this, Misc.getLanguageTo(this))
                Misc.setLanguageTo(this, temp)

                Misc.zoomOutView(binding.llLanguageFrom, this, 150)
                Misc.zoomOutView(binding.llLanguageTo, this, 150)

                Handler().postDelayed({
                    setSelectedLng()
                    Misc.zoomInView(binding.llLanguageFrom, this, 150)
                    Misc.zoomInView(binding.llLanguageTo, this, 150)
                }, 150)

                getAllText()
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.please_select_language_you_want_to_translate),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        getAllText()
    }

    private suspend fun getLanguageJson(lan: String): String {
        return PhrasesAssetReader.getLanguageJson(this, lan)
    }

    /** Updates the From/To header (labels and flags). Listeners are set once, in onCreate. */
    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            binding.tvLanguageFrom.text = resources.getString(R.string.detect)
            binding.flagFrom.setImageResource(Misc.getFlag(this, "100"))
        } else {
            binding.tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName

            binding.flagFrom.setImageResource(
                Misc.getFlag(
                    this,
                    Misc.getLanguageFrom(this)
                )
            )
        }

        binding.flagTo.setImageResource(
            Misc.getFlag(
                this,
                Misc.getLanguageTo(this)
            )
        )
        binding.tvLanguageTo.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
    }

    //@DelicateCoroutinesApi
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == lngSelectorLngFrom || requestCode == lngSelectorLngTo) {
            reloadIfLanguagesChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
        // Covers every way the languages can change while this screen is in the background.
        reloadIfLanguagesChanged()
    }

    private fun currentLanguages() = Misc.getLanguageFrom(this) to Misc.getLanguageTo(this)

    private fun reloadIfLanguagesChanged() {
        if (loadedLanguages != null && loadedLanguages != currentLanguages()) getAllText()
    }


    /**
     * Loads this category's phrases in the From language and their translations in the To
     * language. A new call cancels a load still in progress (two loads used to run at once and
     * append to the same lists). The category comes from the launch intent, which survives process
     * death; the static Misc.phrasebookPosition it used to read resets to 0 and showed category 0.
     */
    private fun getAllText() {
        val (from, to) = currentLanguages()
        loadedLanguages = from to to
        val srcLng = if (from == Misc.defaultLanguage) "en" else from
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val source = readCategory(srcLng) ?: return@launch
            val target = readCategory(to) ?: return@launch

            expandableListTitle.clear()
            expandableListTitle.addAll(source.first)
            expandableListDetail.clear()
            expandableListDetail.putAll(source.second)
            expandableListTitleTranslation.clear()
            expandableListTitleTranslation.addAll(target.first)
            expandableListDetailTranslation.clear()
            expandableListDetailTranslation.putAll(target.second)

            expandableListAdapter = CustomExpandableListAdapter(
                this@PhrasebookDetailedActivity,
                expandableListTitle,
                expandableListTitleTranslation,
                expandableListDetailTranslation,
                expandableListDetail,
                groupStyles,
                PhraseBookMainAdapter.categoryStyle(i)
            )
            binding.expandableListView.setAdapter(expandableListAdapter)
            binding.expandableListView.setGroupIndicator(null)
        }
    }

    /**
     * The subcategory titles and their phrases for this screen's category in [language], in file
     * order (phrases are paired with their translations by position), or null with a toast when
     * the language has no phrasebook.
     */
    private suspend fun readCategory(
        language: String
    ): Pair<List<String>, Map<String, List<String>>>? {
        return try {
            val obj = JSONObject(getLanguageJson(language))
            val titles = ArrayList<String>()
            val details = HashMap<String, List<String>>()
            val categoryKey = obj.keys().asSequence().elementAtOrNull(i) ?: return Pair(titles, details)
            val category = obj.getJSONObject(categoryKey)
            for (subcategoryKey in category.keys()) {
                val subcategory = category.getJSONObject(subcategoryKey)
                val phrases = ArrayList<String>()
                for (phraseKey in subcategory.keys()) {
                    phrases.add(subcategory.getString(phraseKey))
                }
                titles.add(subcategoryKey)
                details[subcategoryKey] = phrases
            }
            Pair(titles, details)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Sorry! ${Locale(language).displayName} is not available in phrasebook yet.",
                Toast.LENGTH_SHORT
            ).show()
            null
        }
    }

    private companion object {
        /**
         * Icon and tone of each phrase group, by category (the launch intent's position) and group position. The order is the
         * phrase files' (the same in every language), e.g. At the Store = Payment, Products and
         * Sections. A group without an entry gets the category's own icon.
         */
        fun groupStylesFor(category: Int): List<PhraseGroupStyle> = when (category) {
            0 -> listOf( // At the Store
                PhraseGroupStyle.teal(R.drawable.ic_ui_cart),           // Payment
                PhraseGroupStyle.orange(R.drawable.ic_ui_grid),         // Products and Sections
            )
            1 -> listOf( // At the Hotel
                PhraseGroupStyle.accent(R.drawable.ic_ui_hotel),        // Check-In
                PhraseGroupStyle.purple(R.drawable.ic_ui_calendar_clock), // Make a Reservation
            )
            2 -> listOf( // At the Bar
                PhraseGroupStyle.purple(R.drawable.ic_ui_wine),         // Basics
                PhraseGroupStyle.orange(R.drawable.ic_ui_utensils),     // Snacks
            )
            3 -> listOf( // At the Restaurant
                PhraseGroupStyle.purple(R.drawable.ic_ui_list_checks),  // Allergies
                PhraseGroupStyle.danger(R.drawable.ic_ui_utensils),     // Basics
                PhraseGroupStyle.teal(R.drawable.ic_ui_wine),           // Drinks
            )
            4 -> listOf( // Essentials
                PhraseGroupStyle.teal(R.drawable.ic_ui_list_checks),    // Basics
                PhraseGroupStyle.accent(R.drawable.ic_ui_globe),        // Directions
                PhraseGroupStyle.purple(R.drawable.ic_ui_info),         // General
                PhraseGroupStyle.orange(R.drawable.ic_ui_message),      // Greetings
                PhraseGroupStyle.accent(R.drawable.ic_ui_languages),    // Language
                PhraseGroupStyle.success(R.drawable.ic_ui_cart),        // Money
            )
            5 -> listOf( // Medical Help
                PhraseGroupStyle.danger(R.drawable.ic_ui_alert),        // Allergies and Asthma
                PhraseGroupStyle.success(R.drawable.ic_ui_stethoscope), // General
            )
            6 -> listOf( // Problems
                PhraseGroupStyle.accent(R.drawable.ic_ui_shield),       // Authority
                PhraseGroupStyle.orange(R.drawable.ic_ui_alert),        // Basics
            )
            7 -> listOf( // Time, Date and Numbers
                PhraseGroupStyle.purple(R.drawable.ic_ui_calendar_clock), // Days
                PhraseGroupStyle.accent(R.drawable.ic_ui_calendar_clock), // Months
                PhraseGroupStyle.teal(R.drawable.ic_ui_grid),           // Numbers
                PhraseGroupStyle.orange(R.drawable.ic_ui_history),      // Time
            )
            8 -> listOf( // While Traveling
                PhraseGroupStyle.accent(R.drawable.ic_ui_plane),        // Air Travel
                PhraseGroupStyle.purple(R.drawable.ic_ui_calendar_clock), // Arrivals and Departures
                PhraseGroupStyle.orange(R.drawable.ic_ui_cart),         // Buying Tickets
                PhraseGroupStyle.teal(R.drawable.ic_ui_shield),         // Customs and Immigration
                PhraseGroupStyle.success(R.drawable.ic_ui_globe),       // Locations
                PhraseGroupStyle.accent(R.drawable.ic_ui_send),         // Public Transportation
                PhraseGroupStyle.danger(R.drawable.ic_ui_send),         // Taxis and Cars
            )
            else -> emptyList()
        }
    }
}
