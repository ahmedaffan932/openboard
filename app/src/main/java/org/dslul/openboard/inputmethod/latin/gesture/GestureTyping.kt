package org.dslul.openboard.inputmethod.latin.gesture

import android.content.Context
import android.os.SystemClock
import android.util.Log
import org.dslul.openboard.inputmethod.keyboard.Key
import org.dslul.openboard.inputmethod.keyboard.Keyboard
import org.dslul.openboard.inputmethod.latin.Dictionary
import org.dslul.openboard.inputmethod.latin.NgramContext
import org.dslul.openboard.inputmethod.latin.SuggestedWords
import org.dslul.openboard.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import org.dslul.openboard.inputmethod.latin.common.ComposedData
import org.dslul.openboard.inputmethod.latin.common.Constants
import org.dslul.openboard.inputmethod.latin.utils.SuggestionResults
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

/**
 * Swipe typing ("slide to write") without Google's closed-source gesture library: the keyboard
 * already records the swipe and shows its trail; this turns the swipe into words with
 * [GestureRecognizer], using the words of the current language's main dictionary.
 *
 * The word list is read from the dictionary once per language, in the background.
 */
object GestureTyping {
    private const val TAG = "GestureTyping"

    private val loader = Executors.newSingleThreadExecutor { r ->
        Thread(r, "GestureWordList").apply { isDaemon = true }
    }

    private val lock = Any()
    private var words: GestureWords? = null
    private var wordsFor: WeakReference<Dictionary>? = null
    private var loadingFor: WeakReference<Dictionary>? = null
    private var recognizer: GestureRecognizer? = null
    private var recognizerFor: WeakReference<Keyboard>? = null

    // Swipe points are in view coordinates; keys are in keyboard coordinates. Same offset as the
    // key detector uses for taps.
    @Volatile private var correctionX = 0
    @Volatile private var correctionY = 0

    @JvmStatic
    fun setTouchCorrection(x: Int, y: Int) {
        correctionX = x
        correctionY = y
    }

    /** Starts reading the swipe words of [dictionary] if they aren't loaded or loading yet. */
    @JvmStatic
    fun preload(dictionary: Dictionary?) {
        if (dictionary == null) return
        synchronized(lock) {
            if (wordsFor?.get() === dictionary || loadingFor?.get() === dictionary) return
            loadingFor = WeakReference(dictionary)
        }
        loader.execute { load(dictionary) }
    }

    /** Where the word lists are cached, so a restarted keyboard doesn't read the dictionary again. */
    @JvmStatic
    fun init(context: Context) {
        cacheDir = context.cacheDir
    }

    @Volatile private var cacheDir: File? = null

    private fun load(dictionary: Dictionary) {
        synchronized(lock) { if (loadingFor?.get() !== dictionary) return }
        val start = SystemClock.uptimeMillis()
        val contentId = dictionary.contentId
        val cacheFile = cacheDir?.let { File(it, "gesture_words_${dictionary.mLocale}.txt") }
        var list = if (contentId != null && cacheFile != null) readCache(cacheFile, contentId) else null
        val cached = list != null
        if (list == null) {
            // First time for this dictionary (per language and app version): read it, slow.
            val entries = ArrayList<Pair<String, Int>>()
            try {
                dictionary.collectWords { word, probability -> entries.add(word to probability) }
            } catch (e: RuntimeException) {
                Log.w(TAG, "Could not read the dictionary for swipe typing", e)
                synchronized(lock) { if (loadingFor?.get() === dictionary) loadingFor = null }
                return
            }
            list = GestureWords.build(entries)
            if (contentId != null && cacheFile != null && list.size > 0) writeCache(cacheFile, contentId, list)
        }
        Log.i(TAG, "${list.size} swipe words for ${dictionary.mLocale} in ${SystemClock.uptimeMillis() - start} ms" +
                if (cached) " (cached)" else "")
        synchronized(lock) {
            if (loadingFor?.get() !== dictionary) return
            loadingFor = null
            words = list
            wordsFor = WeakReference(dictionary)
        }
    }

    // First line: the dictionary's content id, then the words (see GestureWords.write).
    private fun readCache(file: File, contentId: String): GestureWords? = try {
        if (!file.isFile) null else file.bufferedReader().use { reader ->
            if (reader.readLine() != contentId) null else GestureWords.read(reader.lineSequence())
        }
    } catch (e: IOException) {
        Log.w(TAG, "Could not read the swipe word cache", e)
        null
    }

    private fun writeCache(file: File, contentId: String, list: GestureWords) {
        try {
            val temp = File(file.path + ".tmp")
            temp.bufferedWriter().use { writer ->
                writer.append(contentId).append('\n')
                list.write(writer)
            }
            if (!temp.renameTo(file)) temp.delete()
        } catch (e: IOException) {
            Log.w(TAG, "Could not write the swipe word cache", e)
        }
    }

    /** The words for the swipe in [composedData], best first. Empty while the word list loads. */
    @JvmStatic
    fun getSuggestionResults(composedData: ComposedData, ngramContext: NgramContext,
            keyboard: Keyboard, dictionary: Dictionary?): SuggestionResults {
        val results = SuggestionResults(SuggestedWords.MAX_SUGGESTIONS,
                ngramContext.isBeginningOfSentenceContext, false)
        if (dictionary == null) return results
        val list: GestureWords?
        val gestureRecognizer: GestureRecognizer
        synchronized(lock) {
            list = if (wordsFor?.get() === dictionary) words else null
            gestureRecognizer = recognizerFor(keyboard)
        }
        if (list == null) {
            preload(dictionary)
            return results
        }

        val pointers = composedData.mInputPointers
        val size = pointers.pointerSize
        if (size < 2) return results
        val ids = pointers.pointerIds
        val allX = pointers.xCoordinates
        val allY = pointers.yCoordinates
        val xs = IntArray(size)
        val ys = IntArray(size)
        var count = 0
        for (i in 0 until size) {
            if (ids[i] != ids[0]) continue // only the finger that started the swipe
            xs[count] = allX[i] + correctionX
            ys[count] = allY[i] + correctionY
            count++
        }
        for (result in gestureRecognizer.recognize(xs, ys, count, list, SuggestedWords.MAX_SUGGESTIONS)) {
            results.add(SuggestedWordInfo(result.word, "", toScore(result.score),
                    SuggestedWordInfo.KIND_CORRECTION, dictionary,
                    SuggestedWordInfo.NOT_AN_INDEX, SuggestedWordInfo.NOT_A_CONFIDENCE))
        }
        return results
    }

    private fun toScore(score: Double) = (1_000_000 + score * 10_000).toInt()

    private fun recognizerFor(keyboard: Keyboard): GestureRecognizer {
        val cached = recognizer
        if (cached != null && recognizerFor?.get() === keyboard) return cached
        val keys = keyboard.sortedKeys.filter { isSwipeKey(it) }
                .map { GestureKey(it.code, it.x + it.width / 2f, it.y + it.height / 2f) }
        return GestureRecognizer(keys, keyboard.mMostCommonKeyWidth.toFloat()).also {
            recognizer = it
            recognizerFor = WeakReference(keyboard)
        }
    }

    private fun isSwipeKey(key: Key): Boolean {
        val code = key.code
        if (code <= 0 || code == Constants.CODE_SPACE || key.isModifier) return false
        if (Character.isLetter(code)) return true
        return when (Character.getType(code)) {
            Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt() -> true
            else -> false
        }
    }
}
