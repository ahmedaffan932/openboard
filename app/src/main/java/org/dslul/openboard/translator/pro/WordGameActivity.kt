package org.dslul.openboard.translator.pro

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ActivityWordGameBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.MiscTranslate
import org.dslul.openboard.translator.pro.classes.WordGameContent
import org.dslul.openboard.translator.pro.classes.WordGameLogic
import org.dslul.openboard.translator.pro.classes.WordList
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.classes.ads.admob.AdmobBannerAd
import org.dslul.openboard.translator.pro.interfaces.TranslationInterface
import java.util.Locale

/**
 * Word-unscramble puzzle ("Fun Words"): the player taps the shuffled letter tiles of an
 * English word to spell it. Solving reveals the word's meaning, plays a confetti
 * animation, and translates the word into the player's target language. Levels advance
 * one word at a time (words are pre-sorted short -> long), gated so the current word must
 * be solved before Next unlocks.
 *
 * Ported from TranslatorGuru's `GameActivity`, re-wired to this app: words come from the
 * bundled asset via [WordGameContent] (no network needed to start), translation goes
 * through [MiscTranslate] (ML Kit + the working gtx endpoint), ads through [AdmobBannerAd],
 * analytics through Firebase. Progress is kept in its own [PREFS] file.
 */
class WordGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordGameBinding
    private var wordList: WordList? = null
    private var puzzleViewBoxSize: Int = 80
    private lateinit var currentWord: String
    private val solWordToChar: ArrayList<String> = arrayListOf()
    private val isClickArrayList = ArrayList<LinearLayout>()
    private var currentWordForPuzzle: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Firebase.analytics.logEvent("Game", null)

        AdmobBannerAd.loadAdmobAdaptiveBanner(this, binding.llBannerBottom, Ads.dashboardBanner)

        puzzleViewBoxSize = (resources.displayMetrics.xdpi * 0.28).toInt()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnReload.setOnClickListener { getWords() }

        getWords()
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
    }

    // --- Word loading (bundled asset; works offline) ---

    private fun getWords() {
        binding.llPBFunWord.visibility = View.VISIBLE
        lifecycleScope.launch {
            val list = try {
                WordGameContent.load(this@WordGameActivity)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            if (list == null || list.data.isEmpty()) {
                binding.llPBFunWord.visibility = View.GONE
                Toast.makeText(
                    this@WordGameActivity,
                    getString(R.string.sorry_error_in_fetching_words),
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(Misc.logKey, "word list null/empty")
                return@launch
            }

            wordList = list
            binding.llPBFunWord.visibility = View.GONE

            // Clamp a persisted index that is now out of range (e.g. list shrank).
            val clamped = WordGameLogic.resetIndexIfAtEnd(getIndexValueSharedPref(), list.data.size)
            if (clamped != getIndexValueSharedPref()) saveIndexSharedPref("reset")

            getLevelSharedPref()
            currentWord = list.data[getIndexValueSharedPref()].word
            currentWordForPuzzle = charArrayToStringArray(currentWord.lowercase(Locale.ROOT))
            if (getIndexValueSharedPref() != 0) currentWordForPuzzle.shuffle()
            solWordToChar.clear()
            updatePuzzleView()
            updateSolView()
            Handler(Looper.getMainLooper()).postDelayed({ loadMeaning() }, 100)

            setUpControlListeners()
            refreshNextButtonTint()
        }
    }

    private fun setUpControlListeners() {
        binding.btnHint.setOnClickListener { showHint() }

        binding.btnNext.setOnClickListener {
            if (WordGameLogic.isNextUnlocked(getIndexValueSharedPref(), getLatestSolvedWordSharedPre())) {
                loadWordFromSharedPref("increament")
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.please_solve_current_puzzle_to_proceed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnPrevious.setOnClickListener { loadWordFromSharedPref("decreament") }
        binding.btnReplay.setOnClickListener { loadWordFromSharedPref("same") }
        binding.btnRevert.setOnClickListener { removeOneChar() }
        binding.btnClear.setOnClickListener { clearSolutionView() }
        binding.llPuzzleSol.setOnClickListener { removeOneChar() }
        binding.llLngToFunWorld.setOnClickListener {
            startActivity(Intent(this, LanguageSelectorActivity::class.java))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadWordFromSharedPref(actionType: String) {
        val list = wordList ?: return
        binding.btnHint.visibility = View.VISIBLE
        binding.textHint.visibility = View.VISIBLE
        binding.btnClear.isEnabled = true
        binding.btnRevert.isEnabled = true

        saveIndexSharedPref(actionType)
        // Wrap to the first word if we ran off the end.
        if (WordGameLogic.resetIndexIfAtEnd(getIndexValueSharedPref(), list.data.size) != getIndexValueSharedPref()) {
            saveIndexSharedPref("reset")
        }
        getLevelSharedPref()

        currentWord = list.data[getIndexValueSharedPref()].word
        currentWordForPuzzle = charArrayToStringArray(currentWord.lowercase(Locale.getDefault()))
        if (getIndexValueSharedPref() != 0) currentWordForPuzzle.shuffle()
        solWordToChar.clear()
        updatePuzzleView()
        updateSolView()

        binding.textViewSolTranslation.text = ""
        binding.textViewSolTranslation.visibility = View.INVISIBLE
        binding.llHintTranslation.visibility = View.GONE
        binding.llHint.visibility = View.GONE

        loadMeaning()
        refreshNextButtonTint()
    }

    private fun saveIndexSharedPref(action: String) {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            when (action) {
                "reset" -> putInt(KEY_INDEX, 0)
                "increament" -> putInt(KEY_INDEX, getIndexValueSharedPref() + 1)
                "decreament" -> if (getIndexValueSharedPref() > 0) putInt(KEY_INDEX, getIndexValueSharedPref() - 1)
            }
            commit()
        }
        refreshNextButtonTint()
    }

    private fun saveLatestSolvedWordSharedPref(index: Int) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_LATEST, index).commit()
    }

    private fun getLatestSolvedWordSharedPre(): Int =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_LATEST, 0)

    private fun getIndexValueSharedPref(): Int =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_INDEX, 0)

    private fun refreshNextButtonTint() {
        val unlocked = WordGameLogic.isNextUnlocked(getIndexValueSharedPref(), getLatestSolvedWordSharedPre())
        binding.imgBtnNext.backgroundTintList =
            if (unlocked) ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent))
            else ContextCompat.getColorStateList(this, R.color.gray_700)
    }

    // --- Puzzle board ---

    private fun updatePuzzleView() {
        binding.llPuzzleWord.removeAllViews()
        binding.llPuzzleWord1.removeAllViews()
        binding.llPuzzleWord2.removeAllViews()
        for (x in 0 until currentWordForPuzzle.size) {
            val tile = getPuzzleTextView(currentWordForPuzzle[x].uppercase(Locale.getDefault()))
            when {
                x < 6 -> binding.llPuzzleWord.addView(tile)
                x in 6..11 -> binding.llPuzzleWord1.addView(tile)
                else -> binding.llPuzzleWord2.addView(tile)
            }
        }
    }

    inner class PuzzleWordClickListener : View.OnClickListener {
        override fun onClick(p0: View?) {
            if (p0 == null || isClickArrayList.contains(p0)) return
            val mv = p0 as LinearLayout
            val tv = mv.getChildAt(0) as TextView
            if (solWordToChar.size < currentWord.length) solWordToChar.add(tv.text.toString())
            updateSolView()
            p0.setBackgroundResource(R.drawable.bg_puzzle_clicked)
            isClickArrayList.add(p0)
        }
    }

    fun updateSolView() {
        binding.llPuzzleSol.removeAllViews()
        var solWord = ""
        for (x in 0 until currentWordForPuzzle.size) {
            try {
                solWord += solWordToChar[x]
                binding.llPuzzleSol.addView(
                    getSolutionTextView(solWordToChar[x].uppercase(Locale.getDefault()), true)
                )
            } catch (e: Exception) {
                binding.llPuzzleSol.addView(getSolutionTextView("-", false))
            }
        }

        binding.btnRevert.visibility = if (solWord.isNotEmpty()) View.VISIBLE else View.GONE

        if (currentWord.equals(solWord, true)) {
            binding.animationFinish.playAnimation()
            binding.btnClear.visibility = View.INVISIBLE
            binding.btnNext.visibility = View.VISIBLE
            binding.btnPrevious.visibility = View.VISIBLE
            binding.btnReplay.visibility = View.VISIBLE
            binding.textViewSolTranslation.visibility = View.VISIBLE
            binding.btnRevert.visibility = View.GONE

            loadMeaning()
            disableClicks()
            saveLatestSolvedWordSharedPref(getIndexValueSharedPref() + 1)

            translateSolvedWord()
        } else if (currentWord.length == solWord.length) {
            binding.btnClear.visibility = View.VISIBLE
            binding.btnClear.playAnimation()
            Log.v(Misc.logKey, "$currentWord level not clear: $solWord")
        }
        refreshNextButtonTint()
    }

    private fun translateSolvedWord() {
        binding.llPBFunWord.visibility = View.VISIBLE
        MiscTranslate.translate(
            this, currentWord, "en", Misc.getLanguageTo(this),
            object : TranslationInterface {
                override fun onTranslate(translation: String) {
                    if (isFinishing || isDestroyed) return
                    binding.textViewSolTranslation.visibility = View.VISIBLE
                    binding.textViewSolTranslation.text = translation
                    binding.llPBFunWord.visibility = View.GONE
                }

                override fun onFailed() {
                    if (isFinishing || isDestroyed) return
                    binding.llPBFunWord.visibility = View.GONE
                    toastTranslateError()
                }
            }
        )
    }

    private fun loadMeaning() {
        wordList?.let { binding.tvHintMeaning.text = it.data[getIndexValueSharedPref()].meaning }
    }

    private fun disableClicks() {
        binding.btnClear.isEnabled = false
        binding.btnRevert.isEnabled = false
        binding.btnHint.visibility = View.INVISIBLE
        binding.textHint.visibility = View.INVISIBLE
        listOf(binding.llPuzzleSol, binding.llPuzzleWord, binding.llPuzzleWord1, binding.llPuzzleWord2)
            .forEach { row -> row.children.forEach { it.isClickable = false } }
    }

    private fun removeOneChar() {
        if (solWordToChar.isNotEmpty()) {
            solWordToChar.removeAt(solWordToChar.lastIndex)
            isClickArrayList[isClickArrayList.lastIndex].setBackgroundResource(R.drawable.bg_puzzle_char)
            isClickArrayList.removeAt(isClickArrayList.lastIndex)
            updateSolView()
        }
    }

    private fun clearSolutionView() {
        solWordToChar.clear()
        for (item in isClickArrayList) item.setBackgroundResource(R.drawable.bg_puzzle_char)
        isClickArrayList.clear()
        binding.btnClear.visibility = View.INVISIBLE
        updateSolView()
    }

    @SuppressLint("SetTextI18n")
    private fun getLevelSharedPref() {
        binding.progressNumber.text = "${getIndexValueSharedPref() + 1}"
        binding.progressLevel.text = getString(R.string.level)
    }

    // --- Hint: show the English meaning + its translation into the target language ---

    private fun showHint() {
        val list = wordList ?: return
        val index = getIndexValueSharedPref()
        val word = list.data[index].word
        val meaning = list.data[index].meaning
        val to = Misc.getLanguageTo(this)

        binding.tvHintMeaning.text = meaning
        binding.llHint.visibility = View.VISIBLE
        binding.llPBFunWord.visibility = View.VISIBLE

        MiscTranslate.translate(this, word, "en", to, object : TranslationInterface {
            override fun onTranslate(translation: String) {
                if (isFinishing || isDestroyed) return
                binding.tvHintTitleTranslation.text = translation
                MiscTranslate.translate(this@WordGameActivity, meaning, "en", to, object : TranslationInterface {
                    override fun onTranslate(translation: String) {
                        if (isFinishing || isDestroyed) return
                        binding.tvHintMeaningTranslation.text = translation
                        binding.llHintTranslation.visibility = View.VISIBLE
                        binding.llHint.visibility = View.VISIBLE
                        binding.llPBFunWord.visibility = View.GONE
                    }

                    override fun onFailed() {
                        if (isFinishing || isDestroyed) return
                        binding.llPBFunWord.visibility = View.GONE
                        toastTranslateError()
                    }
                })
            }

            override fun onFailed() {
                if (isFinishing || isDestroyed) return
                binding.llPBFunWord.visibility = View.GONE
                toastTranslateError()
            }
        })
    }

    private fun toastTranslateError() {
        Toast.makeText(
            this,
            getString(R.string.some_error_occurred_in_translation),
            Toast.LENGTH_SHORT
        ).show()
    }

    // --- Target-language header ---

    private fun setSelectedLng() {
        val to = Misc.getLanguageTo(this)
        binding.textViewLngToFunWorld.text = Locale(to).displayName
        binding.flagToFunWord.setImageResource(Misc.getFlag(this, to))
    }

    // --- Tile builders ---

    private fun getSolutionTextView(value: String, setColor: Boolean): TextView {
        val textView = TextView(this)
        textView.text = value
        textView.textSize = 30F
        if (setColor) textView.setTextColor(ContextCompat.getColor(this, R.color.accent))
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 8, 0)
        textView.layoutParams = params
        return textView
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getPuzzleTextView(value: String): LinearLayout {
        return try {
            val linearLayout = LinearLayout(this)
            val llParams = LinearLayout.LayoutParams(puzzleViewBoxSize, puzzleViewBoxSize).apply {
                setMargins(4, 4, 24, 6)
                gravity = Gravity.CENTER
            }
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.layoutTransition = LayoutTransition()
            linearLayout.setOnClickListener(PuzzleWordClickListener())
            linearLayout.layoutParams = llParams
            linearLayout.setBackgroundResource(R.drawable.bg_puzzle_char)
            linearLayout.gravity = Gravity.CENTER
            linearLayout.elevation = 8F

            val valueTV = TextView(this)
            valueTV.text = value
            valueTV.textSize = 26F
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.CENTER
            valueTV.layoutParams = params
            valueTV.gravity = Gravity.CENTER
            linearLayout.addView(valueTV)
            linearLayout
        } catch (e: Exception) {
            e.printStackTrace()
            LinearLayout(this)
        }
    }

    private fun charArrayToStringArray(value: String): ArrayList<String> {
        val charArrToStrArr = arrayListOf<String>()
        for (element in value.toCharArray()) charArrToStrArr.add(element.toString())
        return charArrToStrArr
    }

    companion object {
        private const val PREFS = "word_game"
        private const val KEY_INDEX = "currentIndex"
        private const val KEY_LATEST = "latestSolvedWord"
    }
}
