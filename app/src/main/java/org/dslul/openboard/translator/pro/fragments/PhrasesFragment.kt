package org.dslul.openboard.translator.pro.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.FragmentPhrasesBinding
import org.dslul.openboard.translator.pro.LanguageSelectorActivity
import org.dslul.openboard.translator.pro.SettingsActivity
import org.dslul.openboard.translator.pro.adaptor.PhraseBookMainAdapter
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.PhrasesAssetReader
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import org.json.JSONObject
import java.util.Locale

class PhrasesFragment : Fragment() {
    private lateinit var binding: FragmentPhrasesBinding
    private val arrTo = ArrayList<String>()
    private val arrFrom = ArrayList<String>()
    private val lngSelectorLngTo = 1230
    private val lngSelectorLngFrom = 1090
    private var lastLoadedLanguageFrom: String? = null
    private var lastLoadedLanguageTo: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Ads.loadAndShowInterstitial(
//            requireActivity(),
//            Ads.phraseInt,
//            AdIds.interstitialAdIdAdMobPhrases,
//            object : InterstitialCallBack {
//                override fun onDismiss() {
//                    init()
//                }
//            })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhrasesBinding.inflate(layoutInflater, container, false)
        Misc.isItemClicked = true

        init()

        return binding.root
    }

    private fun init() {

        setSelectedLng()

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        binding.llLanguageFrom.setOnClickListener {
            val intent = Intent(requireActivity(), LanguageSelectorActivity::class.java)
            intent.putExtra("isPhrasebook", true)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorLngFrom)
        }

        binding.llLanguageTo.setOnClickListener {
            startActivityForResult(
                Intent(requireActivity(), LanguageSelectorActivity::class.java),
                lngSelectorLngTo
            )
        }

        binding.ivSwitchLanguages.setOnClickListener {
            if (Misc.getLanguageFrom(requireActivity()) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 200
                rotate.interpolator = LinearInterpolator()

                val image = binding.ivSwitchLanguages
                image.startAnimation(rotate)

                Misc.zoomOutView(binding.llLanguageTo, requireActivity(), 150)
                Misc.zoomOutView(binding.llLanguageFrom, requireActivity(), 150)

                val temp = Misc.getLanguageFrom(requireActivity())
                Misc.setLanguageFrom(requireActivity(), Misc.getLanguageTo(requireActivity()))
                Misc.setLanguageTo(requireActivity(), temp)

                Handler().postDelayed({
                    setSelectedLng()
                    getTextFrom()

                    Misc.zoomInView(binding.llLanguageTo, requireActivity(), 150)
                    Misc.zoomInView(binding.llLanguageFrom, requireActivity(), 150)

                }, 150)


            }
            getTextFrom()
        }

        getTextFrom()
    }

    private suspend fun getLanguageJson(lan: String): String {
        return PhrasesAssetReader.getLanguageJson(requireContext(), lan)
    }

    //@DelicateCoroutinesApi
    private fun getLngTo() {

        try {
            binding.llPBPhrasebookFrag.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.Main) {
                Log.d("Button", "English")
                val srcLng = Misc.getLanguageTo(requireContext())

                try {
                    arrTo.clear()
                    val obj = JSONObject(getLanguageJson(srcLng))

                    for (t in obj.keys()) {
                        var isAlreadyAdded = false

                        for (ojdItem in arrFrom) {
                            if (ojdItem == t.toString()) {
                                isAlreadyAdded = true
                            }
                        }

                        if (!isAlreadyAdded)
                            arrTo.add(t.toString())
                    }
                    lastLoadedLanguageTo = srcLng
                    binding.llPBPhrasebookFrag.visibility = View.GONE

                } catch (e: java.lang.Exception) {
                    binding.llPBPhrasebookFrag.visibility = View.GONE
                    Misc.canWeProceed = false
                    Toast.makeText(
                        requireContext(),
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
                    if (Misc.getLanguageFrom(requireContext()) == Misc.defaultLanguage) {
                        "en"
                    } else {
                        Misc.getLanguageFrom(requireContext())
                    }

                try {
                    arrFrom.clear()
                    val obj = JSONObject(getLanguageJson(srcLng))

                    for (t in obj.keys()) {
                        var isAlreadyAdded = false

                        for (ojdItem in arrFrom) {
                            if (ojdItem == t.toString()) {
                                isAlreadyAdded = true
                            }
                        }

                        if (!isAlreadyAdded)
                            arrFrom.add(t.toString())
                    }
                    lastLoadedLanguageFrom = srcLng

                    binding.recyclerViewPhraseBookMain.layoutManager =
                        LinearLayoutManager(requireContext())
                    binding.recyclerViewPhraseBookMain.adapter =
                        PhraseBookMainAdapter(arrFrom, requireActivity())
                    getLngTo()

                } catch (e: java.lang.Exception) {
                    binding.llPBPhrasebookFrag.visibility = View.GONE
                    if (binding.recyclerViewPhraseBookMain.adapter != null)
                        binding.recyclerViewPhraseBookMain.adapter!!.notifyDataSetChanged()
                    Misc.canWeProceed = false
                    try {
                        Toast.makeText(
                            requireContext(),
                            "Sorry! ${Locale(srcLng).displayName} is not available in phrasebook yet.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            }
        } catch (e: Exception) {
            binding.llPBPhrasebookFrag.visibility = View.GONE
            e.printStackTrace()
        }
    }

    //@DelicateCoroutinesApi
    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(requireContext()) == Misc.defaultLanguage) {
            binding.tvLanguageFrom.text = resources.getString(R.string.detect)
            binding.flagFrom.setImageResource(Misc.getFlag(requireContext(), "100"))
        } else {
            binding.tvLanguageFrom.text = Locale(
                Misc.getLanguageFrom(requireContext())
            ).displayName

            binding.flagFrom.setImageResource(
                Misc.getFlag(
                    requireContext(),
                    Misc.getLanguageFrom(requireContext())
                )
            )
        }

        binding.flagTo.setImageResource(
            Misc.getFlag(
                requireContext(),
                Misc.getLanguageTo(requireContext())
            )
        )
        binding.tvLanguageTo.text = Locale(
            Misc.getLanguageTo(requireContext())
        ).displayName


        binding.llLanguageFrom.setOnClickListener {
            val intent = Intent(requireContext(), LanguageSelectorActivity::class.java)
            intent.putExtra("isPhrasebook", true)
            intent.putExtra(Misc.lngTo, false)
            startActivityForResult(intent, lngSelectorLngFrom)
        }

        binding.llLanguageTo.setOnClickListener {
            startActivityForResult(
                Intent(requireContext(), LanguageSelectorActivity::class.java),
                lngSelectorLngTo
            )
        }

        binding.ivSwitchLanguages.setOnClickListener {
            if (Misc.getLanguageFrom(requireContext()) != Misc.defaultLanguage) {
                val rotate = RotateAnimation(
                    0F, 180F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 150
                rotate.interpolator = LinearInterpolator()
                val image = binding.ivSwitchLanguages
                image.startAnimation(rotate)

                Misc.zoomOutView(binding.llLanguageTo, requireActivity(), 150)
                Misc.zoomOutView(binding.llLanguageFrom, requireActivity(), 150)

                Handler().postDelayed({
                    val temp = Misc.getLanguageFrom(requireContext())
                    Misc.setLanguageFrom(requireActivity(), Misc.getLanguageTo(requireContext()))
                    Misc.setLanguageTo(requireActivity(), temp)

                    setSelectedLng()
                    getTextFrom()

                    Misc.zoomInView(binding.llLanguageTo, requireActivity(), 150)
                    Misc.zoomInView(binding.llLanguageFrom, requireActivity(), 150)

                }, 150)
            } else {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.please_select_language_you_want_to_translate),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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
        setSelectedLng()
        if (lastLoadedLanguageFrom != null && hasSelectedLanguageChanged()) {
            getTextFrom()
        }
    }

    private fun hasSelectedLanguageChanged(): Boolean {
        val languageFrom =
            if (Misc.getLanguageFrom(requireContext()) == Misc.defaultLanguage) {
                "en"
            } else {
                Misc.getLanguageFrom(requireContext())
            }
        val languageTo = Misc.getLanguageTo(requireContext())
        return lastLoadedLanguageFrom != languageFrom || lastLoadedLanguageTo != languageTo
    }
}
