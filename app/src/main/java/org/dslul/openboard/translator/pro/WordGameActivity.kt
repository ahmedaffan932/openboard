package org.dslul.openboard.translator.pro

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
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
import org.dslul.openboard.translator.pro.classes.ads.AdIds
import org.dslul.openboard.translator.pro.classes.ads.Ads
import org.dslul.openboard.translator.pro.interfaces.TranslationInterface
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Word-unscramble puzzle ("Fun Words"): the player taps the shuffled letter tiles of an
 * English word to spell it. Solving reveals the word's meaning, plays a confetti
 * animation, and translates the word into the player's target language. Levels advance
 * one word at a time (words are pre-sorted short -> long), gated so the current word must
 * be solved before Next unlocks.
 *
 * Ported from TranslatorGuru's `GameActivity`, re-wired to this app: words come from the
 * bundled asset via [WordGameContent] (no network needed to start), translation goes
 * through [MiscTranslate] (ML Kit + the working gtx endpoint), ads through [Ads] (a no-media
 * native ad at the bottom), analytics through Firebase. Progress is kept in its own [PREFS] file.
 */
class WordGameActivity : AppCompatActivity() {

    /** Other words each puzzle's letters spell; see [WordGameLogic.isAcceptedGuess]. */
    private var alternateAnswers: Map<String, List<String>> = emptyMap()


    private lateinit var binding: ActivityWordGameBinding
    private var wordList: WordList? = null
    private lateinit var currentWord: String
    private val solWordToChar: ArrayList<String> = arrayListOf()
    private val isClickArrayList = ArrayList<LinearLayout>()
    private var currentWordForPuzzle: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Firebase.analytics.logEvent("Game", null)

        Ads.loadAndShowNativeAd(
            activity = this,
            adIds = AdIds.nativeAdIdSharedBottom,
            remoteKey = Ads.gameNative,
            frameLayout = binding.llBannerBottom,
            shimmerLayout = R.layout.small_native_shimmer
        )

        // The clue card shows "?" until the solved word's translation arrives.
        binding.textViewSolTranslation.doAfterTextChanged { text ->
            binding.tvCluePlaceholder.visibility =
                if (text.isNullOrEmpty()) View.VISIBLE else View.INVISIBLE
        }

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
            alternateAnswers = WordGameContent.loadAlternates(this@WordGameActivity)
            binding.llPBFunWord.visibility = View.GONE
            binding.llSolved.visibility = View.GONE

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
        binding.llSolved.visibility = View.GONE

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

    /** Next is the gradient button, dimmed until the current word is solved. */
    private fun refreshNextButtonTint() {
        val unlocked = WordGameLogic.isNextUnlocked(getIndexValueSharedPref(), getLatestSolvedWordSharedPre())
        binding.imgBtnNext.alpha = if (unlocked) 1f else DIMMED_ALPHA
    }

    // --- Puzzle board ---

    /**
     * Lays the letter tiles out in up to three balanced rows (6 letters: 3 + 3, as in the Figma),
     * 58dp each, smaller when a row would not fit the screen.
     */
    private fun updatePuzzleView() {
        val rows = listOf(binding.llPuzzleWord, binding.llPuzzleWord1, binding.llPuzzleWord2)
        rows.forEach { it.removeAllViews() }
        val count = currentWordForPuzzle.size
        if (count == 0) return
        val rowCount = when {
            count <= MAX_TILES_PER_ROW -> 1
            count <= MAX_TILES_PER_ROW * 2 -> 2
            else -> 3
        }
        val perRow = ceil(count / rowCount.toDouble()).toInt()
        val gap = dp(TILE_GAP_DP)
        val tileSize = min(dp(TILE_SIZE_DP), contentWidth() / perRow - gap)
        for (x in 0 until count) {
            val tile = getPuzzleTextView(currentWordForPuzzle[x].uppercase(Locale.getDefault()), tileSize)
            rows[min(x / perRow, rows.lastIndex)].addView(tile)
        }
    }

    inner class PuzzleWordClickListener : View.OnClickListener {
        override fun onClick(p0: View?) {
            if (p0 == null || isClickArrayList.contains(p0)) return
            val mv = p0 as LinearLayout
            val tv = mv.getChildAt(0) as TextView
            if (solWordToChar.size < currentWord.length) solWordToChar.add(tv.text.toString())
            updateSolView()
            setTileUsed(mv, true)
            isClickArrayList.add(p0)
        }
    }

    fun updateSolView() {
        binding.llPuzzleSol.removeAllViews()
        var solWord = ""
        val slotCount = currentWordForPuzzle.size
        for (x in 0 until slotCount) {
            try {
                solWord += solWordToChar[x]
                binding.llPuzzleSol.addView(
                    getSolutionTextView(solWordToChar[x].uppercase(Locale.getDefault()), true, slotCount)
                )
            } catch (e: Exception) {
                binding.llPuzzleSol.addView(getSolutionTextView("", false, slotCount))
            }
        }

        setRevertActive(solWord.isNotEmpty())

        val isFullLength = solWord.length == currentWord.length
        if (isFullLength && WordGameLogic.isAcceptedGuess(currentWord, solWord, alternateAnswers)) {
            if (!currentWord.equals(solWord, true)) {
                // Another real word from the same letters: it counts, and the puzzle word is shown.
                Toast.makeText(
                    this,
                    getString(
                        R.string.word_game_other_word_accepted,
                        solWord.lowercase(Locale.getDefault()),
                        currentWord
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
            binding.animationFinish.playAnimation()
            binding.btnClear.visibility = View.GONE
            binding.btnNext.visibility = View.VISIBLE
            binding.btnPrevious.visibility = View.VISIBLE
            binding.btnReplay.visibility = View.VISIBLE
            binding.textViewSolTranslation.visibility = View.VISIBLE
            setRevertActive(false)
            showSolved(null)

            loadMeaning()
            disableClicks()
            saveLatestSolvedWordSharedPref(getIndexValueSharedPref() + 1)

            translateSolvedWord()
        } else if (isFullLength) {
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
                    showSolved(translation)
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
        // The solved row takes the hint row's place (Figma 21:5159).
        binding.btnHint.visibility = View.GONE
        binding.textHint.visibility = View.INVISIBLE
        listOf(binding.llPuzzleSol, binding.llPuzzleWord, binding.llPuzzleWord1, binding.llPuzzleWord2)
            .forEach { row -> row.children.forEach { it.isClickable = false } }
    }

    private fun removeOneChar() {
        if (solWordToChar.isNotEmpty()) {
            solWordToChar.removeAt(solWordToChar.lastIndex)
            setTileUsed(isClickArrayList[isClickArrayList.lastIndex], false)
            isClickArrayList.removeAt(isClickArrayList.lastIndex)
            updateSolView()
        }
    }

    private fun clearSolutionView() {
        solWordToChar.clear()
        for (item in isClickArrayList) setTileUsed(item, false)
        isClickArrayList.clear()
        binding.btnClear.visibility = View.GONE
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

    /**
     * One answer slot: 44x52dp as in the Figma, narrower for long words so all [slotCount] fit.
     * Empty slots are plain surface, [filled] ones accent; [markSolutionSolved] turns them green.
     */
    private fun getSolutionTextView(value: String, filled: Boolean, slotCount: Int): TextView {
        val count = slotCount.coerceAtLeast(1)
        val gap = dp(if (count > 7) 5 else 8)
        val width = min(dp(SLOT_WIDTH_DP), (contentWidth() - gap * (count - 1)) / count)
        val height = (width * SLOT_HEIGHT_DP / SLOT_WIDTH_DP.toFloat()).roundToInt()
        val textView = TextView(this)
        textView.text = value
        textView.gravity = Gravity.CENTER
        textView.includeFontPadding = false
        textView.typeface = ResourcesCompat.getFont(this, R.font.sora_bold)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, width * 0.42f)
        textView.setTextColor(ContextCompat.getColor(this, R.color.home_accent))
        textView.setBackgroundResource(
            if (filled) R.drawable.ui_tabs_bg_slot_filled else R.drawable.ui_tabs_bg_slot_empty
        )
        val params = LinearLayout.LayoutParams(width, height)
        params.setMargins(gap / 2, 0, gap - gap / 2, 0)
        textView.layoutParams = params
        return textView
    }

    /** The solved word's slots in success colours (Figma 21:5159). */
    private fun markSolutionSolved() {
        val color = ContextCompat.getColor(this, R.color.home_success)
        binding.llPuzzleSol.children.forEach { slot ->
            slot.setBackgroundResource(R.drawable.ui_tabs_bg_slot_solved)
            (slot as? TextView)?.setTextColor(color)
        }
    }

    /** Shows the "Correct!" row, naming the [translation] once it has arrived. */
    private fun showSolved(translation: String?) {
        markSolutionSolved()
        val word = currentWord.uppercase(Locale.getDefault())
        binding.tvSolved.text = if (translation.isNullOrBlank()) {
            getString(R.string.ui_tabs_word_correct, word)
        } else {
            getString(R.string.ui_tabs_word_correct_meaning, translation, word)
        }
        binding.llSolved.visibility = View.VISIBLE
    }

    /** Backspace stays in place, dimmed while there is no letter to remove. */
    private fun setRevertActive(active: Boolean) {
        binding.btnRevert.visibility = View.VISIBLE
        binding.btnRevert.alpha = if (active) 1f else DIMMED_ALPHA
    }

    /** A letter tile fades once its letter is in the answer and comes back when it is removed. */
    private fun setTileUsed(tile: LinearLayout, used: Boolean) {
        tile.setBackgroundResource(
            if (used) R.drawable.ui_tabs_bg_letter_tile_used else R.drawable.ui_tabs_bg_letter_tile
        )
        tile.elevation = if (used) 0f else dp(2).toFloat()
        (tile.getChildAt(0) as? TextView)?.setTextColor(
            ContextCompat.getColor(this, if (used) R.color.home_text_muted else R.color.home_text_primary)
        )
    }

    /** One letter tile: a [size] square (58dp in the Figma) with a Sora Bold letter. */
    fun getPuzzleTextView(value: String, size: Int): LinearLayout {
        return try {
            val linearLayout = LinearLayout(this)
            val margin = dp(TILE_GAP_DP) / 2
            val llParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
                gravity = Gravity.CENTER
            }
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.layoutTransition = LayoutTransition()
            linearLayout.setOnClickListener(PuzzleWordClickListener())
            linearLayout.layoutParams = llParams
            linearLayout.gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                linearLayout.outlineSpotShadowColor = ContextCompat.getColor(this, R.color.home_shadow)
            }

            val valueTV = TextView(this)
            valueTV.text = value
            valueTV.includeFontPadding = false
            valueTV.typeface = ResourcesCompat.getFont(this, R.font.sora_bold)
            valueTV.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * 0.38f)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.CENTER
            valueTV.layoutParams = params
            valueTV.gravity = Gravity.CENTER
            linearLayout.addView(valueTV)
            setTileUsed(linearLayout, false)
            linearLayout
        } catch (e: Exception) {
            e.printStackTrace()
            LinearLayout(this)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    /** Width the slot and tile rows can use: the screen minus the 16dp side paddings. */
    private fun contentWidth(): Int = resources.displayMetrics.widthPixels - dp(32)

    private fun charArrayToStringArray(value: String): ArrayList<String> {
        val charArrToStrArr = arrayListOf<String>()
        for (element in value.toCharArray()) charArrToStrArr.add(element.toString())
        return charArrToStrArr
    }

    companion object {
        /** Letter tiles: Figma size, gap between tiles, most tiles in one row. */
        private const val TILE_SIZE_DP = 58
        private const val TILE_GAP_DP = 10
        private const val MAX_TILES_PER_ROW = 5

        /** Answer slots: Figma size. */
        private const val SLOT_WIDTH_DP = 44
        private const val SLOT_HEIGHT_DP = 52

        /** Locked Next button / backspace with nothing to remove. */
        private const val DIMMED_ALPHA = 0.45f

        private const val PREFS = "word_game"
        private const val KEY_INDEX = "currentIndex"
        private const val KEY_LATEST = "latestSolvedWord"
    }
}
