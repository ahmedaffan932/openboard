package org.dslul.openboard.translator.pro.fragments

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.translatorguru.ads.admob.LoadAdCallBack
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.rw.keyboardlistener.KeyboardUtils
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.FragmentHomeBinding
import org.dslul.openboard.translator.pro.DisplayHistoryActivity
import org.dslul.openboard.translator.pro.EnableKeyboardActivity
import org.dslul.openboard.translator.pro.FragmentsDashboardActivity
import org.dslul.openboard.translator.pro.LanguageSelectorActivity
import org.dslul.openboard.translator.pro.PremiumScreenActivity
import org.dslul.openboard.translator.pro.SettingsActivity
import org.dslul.openboard.translator.pro.TranslateActivity
import org.dslul.openboard.translator.pro.WordGameActivity
import org.dslul.openboard.translator.pro.adaptor.HistoryAdapter
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.InterfaceHistory
import java.util.Locale
import org.dslul.openboard.inputmethod.latin.settings.SettingsActivity as KeyboardSettingsActivity

/**
 * The dashboard's Home tab (Figma "Home — Redesign"): translate card, feature tiles, Recent list.
 * Its native ad goes in the dashboard's bottom dock ([FragmentsDashboardActivity.binding]
 * dockNativeAdSlot), which this fragment fills while it is showing and empties when it goes.
 */
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private var isBtnTranslateVisible = false
    private val speechRequestCode = 0
    lateinit var adapter: HistoryAdapter
    var isShowingNative = false

    private val hintHandler = Handler(Looper.getMainLooper())
    private val keyboardToggleListener = KeyboardUtils.SoftKeyboardToggleListener { isVisible ->
        // The dock's ad would sit on top of the soft keyboard's edge; hide it while typing.
        if (isShowingNative && view != null) {
            dockAdSlot?.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }

    /** The bottom dock's ad slot, owned by the dashboard activity. */
    private val dockAdSlot: FrameLayout?
        get() = (activity as? FragmentsDashboardActivity)?.binding?.dockNativeAdSlot

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)

        Misc.setIsFirstTime(requireActivity(), false)

        Firebase.analytics.logEvent("Dashboard", null)

        showAds()

        binding.btnPro.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    PremiumScreenActivity::class.java
                ).putExtra(AppAnalytics.EXTRA_PREMIUM_SOURCE, AppAnalytics.PREMIUM_SOURCE_PRO_BUTTON)
            )
        }

        if (isShowingNative) {
            KeyboardUtils.addKeyboardToggleListener(requireActivity(), keyboardToggleListener)
        }

        binding.tvCharCount.text = charCountText(0)
        binding.etText.doOnTextChanged { text, _, _, _ ->
            binding.tvCharCount.text = charCountText(text?.length ?: 0)
            showTranslateButton(!text.isNullOrEmpty())
        }

        setUpClickListeners()

        setSelectedLng()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hintHandler.removeCallbacksAndMessages(null)
        KeyboardUtils.removeKeyboardToggleListener(keyboardToggleListener)
        isShowingNative = false
        dockAdSlot?.apply {
            removeAllViews()
            visibility = View.GONE
        }
    }

    private fun charCountText(length: Int): String =
        getString(R.string.home_char_count, length, MAX_TEXT_LENGTH)

    /** The mic button while the field is empty, the Translate button once there is text. */
    private fun showTranslateButton(show: Boolean) {
        if (show == isBtnTranslateVisible) return
        isBtnTranslateVisible = show
        val shown = if (show) binding.btnTranslate else binding.btnSpeakInput
        val hidden = if (show) binding.btnSpeakInput else binding.btnTranslate
        hidden.visibility = View.GONE
        Misc.zoomInView(shown, requireActivity(), 150)
    }

    private fun setUpClickListeners() {
        binding.btnSettings.setOnClickListener {
            Firebase.analytics.logEvent("Settings", null)
            Ads.runWithEverySixthClickInterstitial(requireActivity()) {
                if (!isAdded) return@runWithEverySixthClickInterstitial
                startActivity(Intent(requireActivity(), SettingsActivity::class.java))
            }
        }

        binding.btnTheme.setOnClickListener { toggleTheme() }

        binding.btnTranslate.setOnClickListener {
            Ads.runWithEverySixthClickInterstitial(requireActivity()) {
                if (!isAdded) return@runWithEverySixthClickInterstitial
                startTranslateActivity(binding.etText.text.toString())
            }
        }

        binding.btnSpeakInput.setOnClickListener {
            Firebase.analytics.logEvent("BtnSpeakInput", null)
            displaySpeechRecognizer()
        }

        binding.btnPaste.setOnClickListener { pasteFromClipboard() }

        binding.tvViewAll.setOnClickListener {
            Ads.runWithEverySixthClickInterstitial(requireActivity()) {
                if (!isAdded) return@runWithEverySixthClickInterstitial
                startActivity(Intent(requireContext(), DisplayHistoryActivity::class.java))
            }
        }

        // Turns the keyboard on; once it is the selected keyboard, opens its settings instead.
        binding.clEnableKeyboardBanner.setOnClickListener {
            Ads.runWithEverySixthClickInterstitial(requireActivity()) {
                if (!isAdded) return@runWithEverySixthClickInterstitial
                val target = if (requireContext().isInputMethodSelected()) {
                    KeyboardSettingsActivity::class.java
                } else {
                    EnableKeyboardActivity::class.java
                }
                startActivity(Intent(requireContext(), target))
            }
        }

        binding.clFunWords.setOnClickListener {
            Firebase.analytics.logEvent("WordGameOpen", null)
            Ads.runWithEverySixthClickInterstitial(requireActivity()) {
                if (!isAdded) return@runWithEverySixthClickInterstitial
                startActivity(Intent(requireContext(), WordGameActivity::class.java))
            }
        }
    }

    /** Same switch as Settings > Dark Theme; the dashboard is recreated in the new theme. */
    private fun toggleTheme() {
        val activity = requireActivity()
        val night = !Misc.isNightModeOn(activity)
        Firebase.analytics.logEvent(if (night) "ThemeDark" else "ThemeLight", null)
        Misc.setNightMode(activity, night)
        AppCompatDelegate.setDefaultNightMode(
            if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun pasteFromClipboard() {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(requireContext())
            ?.toString()
            ?.trim()
        if (text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), R.string.paste_nothing, Toast.LENGTH_SHORT).show()
            return
        }
        Firebase.analytics.logEvent("BtnPaste", null)
        val pasted = text.take(MAX_TEXT_LENGTH)
        binding.etText.setText(pasted)
        binding.etText.setSelection(pasted.length)
    }

    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(requireActivity()) == Misc.defaultLanguage) {
            binding.tvLanguageFrom.text = resources.getString(R.string.detect)
            binding.flagFrom.setImageResource(Misc.getFlag(requireActivity(), "100"))
        } else {
            binding.tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(requireActivity())
            ).displayName

            binding.flagFrom.setImageResource(
                Misc.getFlag(
                    requireActivity(),
                    Misc.getLanguageFrom(requireActivity())
                )
            )
        }

        binding.flagTo.setImageResource(
            Misc.getFlag(
                requireActivity(),
                Misc.getLanguageTo(requireActivity())
            )
        )
        binding.tvLanguageTo.text = Locale(
            Misc.getLanguageTo(requireActivity())
        ).displayName


        binding.llLanguageFrom.setOnClickListener {
            val intent = Intent(requireActivity(), LanguageSelectorActivity::class.java)
            intent.putExtra(Misc.lngTo, false)
            Ads.runWithEverySixthClickInterstitial(requireActivity()) {
                if (!isAdded) return@runWithEverySixthClickInterstitial
                startActivity(intent)
            }
        }

        binding.llLanguageTo.setOnClickListener {
            Ads.runWithEverySixthClickInterstitial(requireActivity()) {
                if (!isAdded) return@runWithEverySixthClickInterstitial
                startActivity(Intent(requireActivity(), LanguageSelectorActivity::class.java))
            }
        }

        binding.btnSwitchLngs.setOnClickListener {
            if (Misc.getLanguageFrom(requireActivity()) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 150
                rotate.interpolator = LinearInterpolator()
                val image = binding.btnSwitchLngs
                image.startAnimation(rotate)

                Misc.zoomOutView(binding.llLanguageTo, requireActivity(), 150)
                Misc.zoomOutView(binding.llLanguageFrom, requireActivity(), 150)

                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isAdded) return@postDelayed
                    val temp = Misc.getLanguageFrom(requireActivity())
                    Misc.setLanguageFrom(requireActivity(), Misc.getLanguageTo(requireActivity()))
                    Misc.setLanguageTo(requireActivity(), temp)

                    setSelectedLng()

                    Misc.zoomInView(binding.llLanguageTo, requireActivity(), 150)
                    Misc.zoomInView(binding.llLanguageFrom, requireActivity(), 150)

                }, 150)
            } else {
                Toast.makeText(
                    requireActivity(),
                    resources.getString(R.string.please_select_language_you_want_to_translate),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequestCode && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results?.get(0)
                }

            try {
                if (spokenText != null) {
                    binding.etText.setText(spokenText)
                    startTranslateActivity(binding.etText.text.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startTranslateActivity(text: String) {
        val intent = Intent(requireContext(), TranslateActivity::class.java)
        intent.putExtra(Misc.key, text)
        startActivity(intent)
    }

    private fun displaySpeechRecognizer() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.EXTRA_LANGUAGE)
            }

            if (Misc.getLanguageFrom(requireActivity()) != Misc.defaultLanguage) intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE, Misc.getLanguageFrom(requireActivity())
            )
            startActivityForResult(intent, speechRequestCode)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireActivity(),
                resources.getString(R.string.sorry_some_erroe_occurred_please_try_againg),
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun showAds() {
        var count = 0
        val hint = getString(R.string.enter_some_text_to_translate)
        val runnable: Runnable by lazy {
            return@lazy object : Runnable {
                override fun run() {
                    if (count < hint.length) {
                        binding.etText.hint = hint.substring(0, count)
                        count++
                        hintHandler.postDelayed(this, 75)
                    }
                }
            }
        }
        hintHandler.post(runnable)

        if (Misc.isProScreenEnabled) {
            binding.btnPro.visibility = View.VISIBLE
        } else {
            binding.btnPro.visibility = View.GONE
        }

        val adSlot = dockAdSlot ?: return
        isShowingNative = true

        Ads.loadAndShowNativeAd(
            activity = requireActivity(),
            adIds = AdIds.nativeAdIdDashboard,
            remoteKey = Ads.dashboardNative,
            frameLayout = adSlot,
            shimmerLayout = R.layout.small_native_shimmer,
            adLayout = R.layout.admob_native_dashboard,
            adShimmerLayout = R.layout.shimmer_native_dashboard,
            callBack = object : LoadAdCallBack {
                override fun onLoaded() {
                    // Loaded after the user left the Home tab: the slot belongs to another tab now.
                    if (view == null) {
                        adSlot.removeAllViews()
                        adSlot.visibility = View.GONE
                        return
                    }
                    Log.d(Misc.logKey, "Dashboard native ad loaded.")
                }

                override fun onFailed() {
                    isShowingNative = false
                    adSlot.removeAllViews()
                    adSlot.visibility = View.GONE
                    Log.d(Misc.logKey, "Dashboard native ad failed.")
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
        try {
            val history = Misc.getHistory(requireActivity())
            if (history.isEmpty()) {
                binding.clHistory.visibility = View.GONE
            } else {
                val arr = ArrayList<TranslateHistoryClass>()
                val count = minOf(RECENT_COUNT, history.size)
                for (i in 0 until count) {
                    arr.add(history[history.lastIndex - i])
                }
                adapter = HistoryAdapter(
                    arr,
                    requireActivity(),
                    object : InterfaceHistory {
                        override fun onHistoryCrash() {
                            binding.clHistory.visibility = View.GONE
                        }
                    },
                    itemLayout = R.layout.history_item_home,
                    favoriteIcon = R.drawable.ic_home_heart_filled,
                    notFavoriteIcon = R.drawable.ic_home_heart_outline
                )
                binding.rvHistory.layoutManager = LinearLayoutManager(requireActivity())
                binding.rvHistory.adapter = adapter
                binding.clHistory.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        /** The text field's limit, shown as "n / 5000" under it. */
        const val MAX_TEXT_LENGTH = 5000

        /** How many of the latest translations the Recent list shows. */
        private const val RECENT_COUNT = 5
    }
}
