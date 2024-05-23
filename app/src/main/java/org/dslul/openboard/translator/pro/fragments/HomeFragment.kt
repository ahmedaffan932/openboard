package org.dslul.openboard.translator.pro.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.rw.keyboardlistener.KeyboardUtils
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.FragmentHomeBinding
import org.dslul.openboard.translator.pro.DisplayHistoryActivity
import org.dslul.openboard.translator.pro.EnableKeyboardActivity
import org.dslul.openboard.translator.pro.LanguageSelectorActivity
import org.dslul.openboard.translator.pro.SettingsActivity
import org.dslul.openboard.translator.pro.TranslateActivity
import org.dslul.openboard.translator.pro.adaptor.HistoryAdapter
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.Misc.isInputMethodSelected
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobMRECAds
import org.dslul.openboard.translator.pro.interfaces.InterfaceHistory
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private var isBtnTranslateVisible = false
    private val speechRequestCode = 0
    lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)

        Misc.setIsFirstTime(requireActivity(), false)

        Firebase.analytics.logEvent("Dashboard", null)

        showAds()

        KeyboardUtils.addKeyboardToggleListener(requireActivity()) { isVisible ->
            if (isVisible) {
                binding.mrecFrameLayout.visibility = View.GONE
            } else {
                binding.mrecFrameLayout.visibility = View.VISIBLE
            }
        }

        if (requireContext().isInputMethodSelected()) {
            binding.btnKeyboard.visibility = View.GONE
        }

        binding.btnKeyboard.setOnClickListener {
            if (requireContext().isInputMethodSelected()) {
                Toast.makeText(requireContext(), "Keyboard is already enabled.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                startActivity(Intent(requireContext(), EnableKeyboardActivity::class.java))
            }
        }

        binding.etText.doOnTextChanged { text, start, before, count ->
            isBtnTranslateVisible = if (binding.etText.text.toString() == "") {
                Misc.zoomOutView(binding.btnTranslate, requireActivity(), 150)
                Misc.zoomInView(binding.btnSpeakInput, requireActivity(), 150)
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.btnTranslate.visibility = View.INVISIBLE
                    binding.btnSpeakInput.visibility = View.VISIBLE
                }, 150)
                false
            } else {
                if (!isBtnTranslateVisible) {
                    Misc.zoomInView(binding.btnTranslate, requireActivity(), 150)
                    Misc.zoomOutView(binding.btnSpeakInput, requireActivity(), 150)
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.btnSpeakInput.visibility = View.INVISIBLE
                    }, 150)
                }
                true
            }
        }


        setUpClickListeners()

        setSelectedLng()

        return binding.root
    }

    private fun setUpClickListeners() {
        binding.btnSettings.setOnClickListener {
            Firebase.analytics.logEvent("Settings", null)
            startActivity(Intent(requireActivity(), SettingsActivity::class.java))
        }

        binding.btnTranslate.setOnClickListener {
            startTranslateActivity(binding.etText.text.toString())
        }

        binding.btnSpeakInput.setOnClickListener {
            Firebase.analytics.logEvent("BtnSpeakInput", null)
            displaySpeechRecognizer()
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(requireContext(), DisplayHistoryActivity::class.java))
        }
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
            startActivity(intent)
        }

        binding.llLanguageTo.setOnClickListener {
            startActivity(Intent(requireActivity(), LanguageSelectorActivity::class.java))
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

                Handler().postDelayed({
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
        val handler = Handler()
        var count = 0
        val hint = getString(R.string.enter_some_text_to_translate)
        val runnable: Runnable by lazy {
            return@lazy object : Runnable {
                override fun run() {
                    if (count < hint.length) {
                        binding.etText.hint = hint.substring(0, count)
                        count++
                        handler.postDelayed(this, 75);
                    }
                }
            }
        }
        handler.post(runnable)

        AdmobMRECAds.showMREC(requireActivity(), binding.mrecFrameLayout, Ads.dashboardNative)
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
        try {
            val arr = ArrayList<TranslateHistoryClass>()
            arr.add(
                Misc.getHistory(requireActivity())[Misc.getHistory(requireActivity()).lastIndex]
            )
            adapter = HistoryAdapter(arr, requireActivity(), object : InterfaceHistory {
                override fun onHistoryCrash() {
                    binding.clHistory.visibility = View.GONE
                }
            })
            binding.rvHistory.layoutManager = LinearLayoutManager(requireActivity())
            binding.rvHistory.adapter = adapter
            binding.clHistory.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}