package org.dslul.openboard.translator.pro

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import org.dslul.openboard.translator.pro.classes.Data
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.WordList
import org.dslul.openboard.translator.pro.classes.admob.BannerAds
import org.dslul.openboard.translator.pro.classes.admob.NativeAds
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.dslul.openboard.inputmethod.latin.R
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList

class GameActivity : AppCompatActivity() {
    var typeface: Typeface? = null
    var puzzleViewBoxSize: Int = 80
    private val isClickArrayList = ArrayList<LinearLayout>()
    private var nextCount = 0
    private var nativeNextCount = 0
    private var isNativeLoaded = false
    lateinit var sb: Snackbar
    private val arrayListForTranslation = ArrayList<String>()
    var solWordToChar: ArrayList<String> = arrayListOf()
    lateinit var currentWordForPuzzle: ArrayList<String>
    private var isSolutionViewUpdated = false
    private lateinit var currentWord: String
    private var wordList: WordList? = null
    private val lngSelectorRequestCode = 1230

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_game)

        InterstitialAd.show(this@GameActivity, Misc.gameIntAm, null)

        val displayMetrics = resources.displayMetrics

        NativeAds.manageShowNativeAd(
            this,
            Misc.gameNativeAm,
            nativeAdFrameLayout
        ) { isLoaded ->
            if (isLoaded) {
                Log.d(Misc.logKey, "Game Native Loaded")
                nativeAdFrameLayout.visibility = View.VISIBLE
                Misc.zoomInView(nativeAdFrameLayout, this@GameActivity, 150)
                isNativeLoaded = true
            } else {
                nativeAdFrameLayout.visibility = View.GONE
            }
        }

        puzzleViewBoxSize = (displayMetrics.xdpi * 0.28).toInt()

        btnBack.setOnClickListener {
            onBackPressed()
        }

        getWords()

        btnReload.setOnClickListener {
            getWords()
        }

        Misc.isActivityCreatingFirstTime = true
    }


    private fun clearSolutionView() {
        solWordToChar.clear()
        for (item in isClickArrayList) {
            item.setBackgroundResource(R.drawable.bg_puzzle_char)
        }
        isClickArrayList.clear()
        btnClear.visibility = View.INVISIBLE
        updateSolView()
    }

    private fun updatePuzzleView() {
        llPuzzleWord.removeAllViews()
        llPuzzleWord1.removeAllViews()
        llPuzzleWord2.removeAllViews()
        for (x in 0 until currentWordForPuzzle.size) {
            when {
                x < 6 -> {
                    llPuzzleWord.addView(getPuzzleTextView(currentWordForPuzzle[x].toUpperCase()))
                }
                x in 6..11 -> llPuzzleWord1.addView(getPuzzleTextView(currentWordForPuzzle[x].toUpperCase()))
                else -> llPuzzleWord2.addView(getPuzzleTextView(currentWordForPuzzle[x].toUpperCase()))
            }
        }
        isSolutionViewUpdated = true
        Log.d(Misc.logKey, llPuzzleWord.childCount.toString())
    }

    @SuppressLint("DefaultLocale")
    private fun loadWordFromSharedPref(actionType: String) {
        btnHint.visibility = View.VISIBLE
        textHint.visibility = View.VISIBLE
        btnClear.isEnabled = true
        btnRevert.isEnabled = true

        saveIndexSharedPref(actionType)
        checkLevel()
        getLevelSharedPref()
        currentWord = wordList!!.data[getIndexValueSharedPref()].word
        currentWordForPuzzle.clear()
        currentWordForPuzzle = charArrayToStringArray(currentWord.toLowerCase())
        print(currentWordForPuzzle)
        if (getIndexValueSharedPref() != 0)
            currentWordForPuzzle.shuffle()
        print(currentWordForPuzzle)
        solWordToChar.clear()
        updatePuzzleView()
        updateSolView()

        textViewSolTranslation.text = ""
        textViewSolTranslation.visibility = View.INVISIBLE
        llHintTranslation.visibility = View.GONE
        llHint.visibility = View.GONE

        loadMeaning()

        if (nativeAdFrameLayout.visibility != View.VISIBLE && !Misc.getPurchasedStatus(
                this
            )
        ) {
            nativeAdFrameLayout.visibility = View.VISIBLE
        }

        if (getIndexValueSharedPref() < getLatestSolvedWordSharedPre())
            imgBtnNext.backgroundTintList = ColorStateList.valueOf(
                resources.getColor(R.color.accent)
            )
        else {
            imgBtnNext.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.gray_700)
        }
    }

    private fun checkLevel() {
        if (wordList!!.data.size == getIndexValueSharedPref()) {
            saveIndexSharedPref("reset")
        }
    }

    private fun saveIndexSharedPref(action: String) {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            if ("reset" == action) {
                putInt("currentIndex", (0))
            } else if ("increament" == action) {
                putInt("currentIndex", (getIndexValueSharedPref() + 1))
            } else if ("decreament" == action) {
                if (getIndexValueSharedPref() > 0)
                    putInt("currentIndex", (getIndexValueSharedPref() - 1))
            }
            commit()
        }
        if (getIndexValueSharedPref() < getLatestSolvedWordSharedPre())
            imgBtnNext.backgroundTintList =
                ColorStateList.valueOf(
                    resources.getColor(R.color.accent)
                )
        else {
            imgBtnNext.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.gray_700)
        }
    }

    private fun saveLatestSolvedWordSharedPref(index: Int) {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt("latestSolvedWord", index)
            commit()
        }
    }

    private fun getLatestSolvedWordSharedPre(): Int {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        return sharedPref?.getInt("latestSolvedWord", 0)!!
    }


    private fun getIndexValueSharedPref(): Int {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        return sharedPref?.getInt("currentIndex", 0) ?: 0
    }

    inner class PuzzleWordClickListener : View.OnClickListener {
        override fun onClick(p0: View?) {
            var isClicked = false
            for (v in isClickArrayList) {
                if (v == p0) {
                    isClicked = true
                    break
                }
            }

            if (!isClicked) {
                val mv = p0 as LinearLayout
                val tv = mv.getChildAt(0) as TextView
                if (solWordToChar.size < currentWord.length)
                    solWordToChar.add(tv.text.toString())
                updateSolView()
                p0.setBackgroundResource(R.drawable.bg_puzzle_clicked)
                isClickArrayList.add(p0)
            }
        }
    }

    fun updateSolView() {
        llPuzzleSol.removeAllViews()
        var solWord = ""
        //Changed
        for (x in 0 until currentWordForPuzzle.size) {
            try {
                solWord += solWordToChar[x]
                llPuzzleSol.addView(
                    getSolutionTextView(
                        solWordToChar[x].toUpperCase(),
                        true
                    )
                )
            } catch (e: Exception) {
                llPuzzleSol.addView(getSolutionTextView("-", false))
                e.printStackTrace()
            }
        }

        //check for solution

        if (solWord.isNotEmpty()) {
            btnRevert.visibility = View.VISIBLE
        } else {
            btnRevert.visibility = View.GONE
        }

        if (currentWord.equals(solWord, true)) {
            animationFinish.playAnimation()

            btnClear.visibility = View.INVISIBLE
            btnNext.visibility = View.VISIBLE
            btnPrevious.visibility = View.VISIBLE
            btnReplay.visibility = View.VISIBLE
            textViewSolTranslation.visibility = View.VISIBLE
            btnRevert.visibility = View.GONE

            loadMeaning()
            disableClicks()
            saveLatestSolvedWordSharedPref(getIndexValueSharedPref() + 1)

            arrayListForTranslation.clear()
            arrayListForTranslation.add("Hint Meaning:")
            arrayListForTranslation.add(wordList!!.data[getIndexValueSharedPref()].word)
            arrayListForTranslation.add(wordList!!.data[getIndexValueSharedPref()].meaning)

            if (!Misc.checkInternetConnection(this)) {
                Toast.makeText(
                    this,
                    "Please check your internet connection and try again.",
                    Toast.LENGTH_SHORT
                ).show()
                llPBFunWord.visibility = View.GONE
            } else {

                jugarTranslationWord(250L)
            }
        } else if (currentWord.length == solWord.length) {
            btnClear.visibility = View.VISIBLE
            btnClear.playAnimation()
            Log.v("Hurray", "$currentWord Level Not Clear $solWord")

        }
        if (getIndexValueSharedPref() < getLatestSolvedWordSharedPre())
            imgBtnNext.backgroundTintList =
                ColorStateList.valueOf(
                    resources.getColor(R.color.accent)
                )
        else {
            imgBtnNext.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.gray_700)
        }
    }

    private fun loadMeaning() {
//        llHintMeaning.visibility = View.INVISIBLE
        tvHintMeaning.text = wordList!!.data[getIndexValueSharedPref()].meaning
    }

    private fun disableClicks() {
        btnClear.isEnabled = false
        btnRevert.isEnabled = false
        btnHint.visibility = View.INVISIBLE
        textHint.visibility = View.INVISIBLE
        llPuzzleSol.children.forEach { child ->
            child.isClickable = false
        }
        llPuzzleWord.children.forEach { child ->
            child.isClickable = false
        }
        llPuzzleWord1.children.forEach { child ->
            child.isClickable = false
        }
        llPuzzleWord2.children.forEach { child ->
            child.isClickable = false
        }
    }

    private fun removeOneChar() {
        if (solWordToChar.size > 0) {
            solWordToChar.removeAt(solWordToChar.lastIndex)
            isClickArrayList[isClickArrayList.lastIndex].setBackgroundResource(R.drawable.bg_puzzle_char)
            isClickArrayList.removeAt(isClickArrayList.lastIndex)
            updateSolView()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getLevelSharedPref() {
        //${wordList!!.data.size}
        progressNumber.text = "${getIndexValueSharedPref() + 1}"
        progressLevel.text = "Level:"
    }

    private suspend fun readWordFromFile(str: String): WordList? {
        return try {
            val sharedPref = getSharedPreferences(Misc.logKey, Context.MODE_PRIVATE)
            var valueString = sharedPref?.getString(str, null)

            if (valueString != null) {
                val wordList: WordList = fromStringToWordList(valueString)
                if (getIndexValueSharedPref() < getLatestSolvedWordSharedPre())
                    imgBtnNext.backgroundTintList =
                        ColorStateList.valueOf(
                            resources.getColor(R.color.accent)
                        )
                else {
                    imgBtnNext.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.gray_700)
                }
                return wordList
            }

            val islandRef = Misc.storage.reference.child("/$str.json")
            val fiftyKBs: Long = 1024 * 5000
            valueString = String(islandRef.getBytes(fiftyKBs).await())
            sharedPref?.edit()?.putString(str, valueString)?.apply()

            val wordList: WordList = fromStringToWordList(valueString)
            wordList
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getSolutionTextView(value: String, setColor: Boolean): TextView {
        val textView = TextView(this)
        textView.text = value
        textView.textSize = 30F
        if (setColor)
            textView.setTextColor(
                resources.getColor(R.color.accent)
            )
        textView.typeface = typeface
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 8, 0)
        textView.layoutParams = params
        return textView
    }

    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    fun getPuzzleTextView(value: String): LinearLayout {
        try {

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
            valueTV.typeface = typeface
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            valueTV.setTextColor(Misc.getThemeColor(R.attr.colorOnPrimary, this))
            params.gravity = Gravity.CENTER
            valueTV.layoutParams = params
            valueTV.gravity = Gravity.CENTER
            linearLayout.addView(valueTV)
            return linearLayout
        } catch (e: Exception) {
            e.printStackTrace()
            return LinearLayout(this)

        }
    }

    private fun charArrayToStringArray(value: String): ArrayList<String> {
        Log.d(Misc.logKey, value)
        val strToArr = value.toCharArray()
        val charArrToStrArr = arrayListOf<String>()
        for (element in strToArr) {
            charArrToStrArr.add(element.toString())
        }
        return charArrToStrArr
    }

    private fun jugarTranslationWord(t: Long) {
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                "Please check your Internet connection and try again later.",
                Toast.LENGTH_SHORT
            ).show()
            llPBFunWord.visibility = View.GONE
            return
        }
        llPBFunWord.visibility = View.VISIBLE
        Handler().postDelayed({
            val policy: StrictMode.ThreadPolicy =
                StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            val encoded = URLEncoder.encode(arrayListForTranslation[1], "utf-8")
            val toCode = Misc.getLanguageTo(this)
            val fromCode = "en"

            val doc =
                Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                    .get()
            val element = doc.getElementsByClass("result-container")
            if (element.text() != "" && !TextUtils.isEmpty(element.text())) {
                textViewSolTranslation.visibility = View.VISIBLE
                textViewSolTranslation.text = element.text()
                llPBFunWord.visibility =
                    View.GONE

            } else {
                llPBFunWord.visibility =
                    View.GONE
                Toast.makeText(
                    this,
                    "Some error occurred in translation please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(Misc.logKey, "its empty")
            }
        }, t)

    }

    private fun jugarTranslateHint(position: Int) {
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                "Please check your Internet connection and try again later.",
                Toast.LENGTH_SHORT
            ).show()
            llPBFunWord.visibility = View.GONE
            return
        }
        llPBFunWord.visibility = View.VISIBLE
        Handler().postDelayed({
            val policy: StrictMode.ThreadPolicy =
                StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            val encoded = URLEncoder.encode(arrayListForTranslation[position], "utf-8")
            val toCode = Misc.getLanguageTo(this)
            val fromCode = "en"

            val doc =
                Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                    .get()
            val element = doc.getElementsByClass("result-container")
            if (element.text() != "" && !TextUtils.isEmpty(element.text())) {
                when (position) {
                    0 -> {
                        tvHintTitleTranslation.text = element.text()
                        jugarTranslateHint(1)
                    }
                    1 -> {
                        jugarTranslateHint(2)
                    }
                    else -> {
                        llPBFunWord.visibility = View.GONE
                        Handler().postDelayed({
                            tvHintMeaningTranslation.text = element.text()
                            Handler().postDelayed({
                                llHintTranslation.visibility = View.VISIBLE
                                llHint.visibility = View.VISIBLE
                            }, 150)
                            nativeAdFrameLayout.visibility = View.GONE
                        }, 100)

                    }
                }
            } else {
                llPBFunWord.visibility =
                    View.GONE
                Toast.makeText(
                    this,
                    "Some error occurred in translation please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(Misc.logKey, "its empty")
            }
        }, 1)

    }

    private fun setSelectedLng() {
        textViewLngToFunWorld.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
        flagToFunWord.setImageResource(
            Misc.getFlag(
                this,
                Misc.getLanguageTo(this)
            )
        )
    }


    override fun onResume() {
        super.onResume()
        setSelectedLng()
        BannerAds.show(bannerFrame)
        if(Misc.isNativeAdClicked){
            NativeAds.manageShowNativeAd(
                this,
                Misc.translateNativeAm,
                nativeAdFrameLayout
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == lngSelectorRequestCode) {
            loadMeaning()
        }
    }

    private fun getWords() {
        if (Misc.checkInternetConnection(this)) {
            llPBFunWord.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.Main) {
                val currentIndex = getIndexValueSharedPref()
                wordList = readWordFromFile(Misc.words)

                Log.d(Misc.logKey, wordList.toString())

                if (wordList == null) {
                    llPBFunWord.visibility = View.GONE
                    Toast.makeText(
                        this@GameActivity,
                        "Sorry! Some error occurred in fetching words.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(Misc.logKey, "word list null")
                    return@launch
                }
                llPBFunWord.visibility = View.GONE
                if (this@GameActivity::sb.isInitialized) {
                    sb.dismiss()
                }
                getLevelSharedPref()

                currentWord = wordList!!.data[currentIndex].word

                currentWordForPuzzle = charArrayToStringArray(
                    wordList!!.data[currentIndex].word.toLowerCase(
                        Locale.ROOT
                    )
                )
                print(currentWordForPuzzle)

                if (getIndexValueSharedPref() != 0)
                    currentWordForPuzzle.shuffle()
                print(currentWordForPuzzle)

                updatePuzzleView()
                updateSolView()
                Handler().postDelayed({
                    loadMeaning()
                }, 100)

                btnHint.setOnClickListener {
                    if (!Misc.checkInternetConnection(this@GameActivity)) {
                        Toast.makeText(
                            this@GameActivity,
                            "Please check your internet connection and try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                        llPBFunWord.visibility = View.GONE
                        return@setOnClickListener
                    }

                    arrayListForTranslation.clear()
                    arrayListForTranslation.add("Hint Meaning:")
                    arrayListForTranslation.add(wordList!!.data[getIndexValueSharedPref()].word)
                    arrayListForTranslation.add(wordList!!.data[getIndexValueSharedPref()].meaning)

                    jugarTranslateHint(0)
                }

                btnNext.setOnClickListener {
                    arrayListForTranslation.clear()
                    arrayListForTranslation.add("Hint Meaning:")
                    arrayListForTranslation.add(wordList!!.data[getIndexValueSharedPref()].word)
                    arrayListForTranslation.add(wordList!!.data[getIndexValueSharedPref()].meaning)

                    if (getIndexValueSharedPref() < getLatestSolvedWordSharedPre())
                        loadWordFromSharedPref("increament")
                    else {
                        Toast.makeText(
                            this@GameActivity,
                            "Solve Current puzzle to proceed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    nextCount++
                    if (nextCount >= Misc.gameNextLimit) {


                        NativeAds.manageShowNativeAd(
                            this@GameActivity,
                            Misc.gameNativeAm,
                            nativeAdFrameLayout
                        ) { isLoaded ->
                            if (isLoaded) {
                                Log.d(Misc.logKey, "Game Native Loaded")
                                nativeAdFrameLayout.visibility = View.VISIBLE
                                Misc.zoomInView(nativeAdFrameLayout, this@GameActivity, 150)
                                isNativeLoaded = true
                            } else {
                                nativeAdFrameLayout.visibility = View.GONE
                            }
                        }
                        nativeNextCount = 0
                        nextCount = 0
                    }


                }

                btnPrevious.setOnClickListener {
                    loadWordFromSharedPref("decreament")
                }

                btnReplay.setOnClickListener {
                    loadWordFromSharedPref("same")
                }

                btnRevert.setOnClickListener {
                    removeOneChar()
                }

                btnClear.setOnClickListener {
                    clearSolutionView()
                }

                llPuzzleSol.setOnClickListener {
                    removeOneChar()
                }

                llLngToFunWorld.setOnClickListener {
                    val intent =
                        Intent(this@GameActivity, LanguageSelectorActivity::class.java)
                    intent.putExtra(Misc.data, Misc.data)
                    startActivityForResult(intent, lngSelectorRequestCode)
                }
            }
        } else {
//            val builder = SpannableStringBuilder()
//            builder.append(" ").append(" ")
//            builder.setSpan(
//                ImageSpan(this, R.drawable.ic_close_btn),
//                builder.length - 1,
//                builder.length,
//                0
//            )
//            builder.append("")

//            if (!this::sb.isInitialized) {
                Toast.makeText(this, "Please Check your Internet connection and try again.", Toast.LENGTH_LONG).show()
//                sb = Snackbar.make(
//                    nativeAdFrameLayout,
//                    "Please Check your Internet connection and try again.",
//                    Snackbar.LENGTH_INDEFINITE
//                )
//                sb.setAction(builder) {
//                    sb.dismiss()
//                }
//                sb.setBackgroundTint(Misc.getThemeColor(R.attr.colorPrimary, this))
//                sb.setTextColor(Misc.getThemeColor(R.attr.colorOnPrimary, this))
//                sb.show()
//            } else {
//                sb.show()
//            }
            llPBFunWord.visibility = View.GONE

        }
    }

    private fun fromStringToWordList(valueString: String): WordList {
        val objJSONArray = JSONObject(valueString).getJSONArray("data")
        val arr = ArrayList<Data>()
        for (i in 0 until objJSONArray.length()) {
            arr.add(
                Data(
                    objJSONArray.getJSONObject(i).getString("word"),
                    objJSONArray.getJSONObject(i).getString("meaning")
                )
            )
        }
        return WordList(arr)
    }
}