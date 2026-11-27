package org.dslul.openboard.translator.pro.learn

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import org.dslul.openboard.translator.pro.classes.Misc
import java.util.Locale

/**
 * The device's text-to-speech voice for one course language (free, on-device). [onReady] reports
 * whether a voice exists; without one, listening questions are left out and speaker buttons hide.
 */
class LearnSpeech(context: Context, private val language: String, private val onReady: (Boolean) -> Unit) :
    TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    var available = false
        private set

    override fun onInit(status: Int) {
        val engine = tts ?: return
        available = status == TextToSpeech.SUCCESS && run {
            val result = try {
                engine.setLanguage(localeFor(language))
            } catch (e: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
            result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
        if (!available) Log.d(Misc.logKey, "Learn: no TTS voice for $language")
        onReady(available)
    }

    fun speak(text: String, slow: Boolean = false) {
        val engine = tts ?: return
        if (!available) return
        engine.setSpeechRate(if (slow) 0.6f else 0.95f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "learn")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        fun localeFor(language: String): Locale = when (language) {
            "en" -> Locale.US
            "es" -> Locale("es", "ES")
            "pt" -> Locale("pt", "BR")
            else -> Locale(language)
        }
    }
}
