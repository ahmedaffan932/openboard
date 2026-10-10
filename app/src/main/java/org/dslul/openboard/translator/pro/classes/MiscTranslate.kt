package org.dslul.openboard.translator.pro.classes

import android.content.Context
import android.os.StrictMode
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dslul.openboard.translator.pro.interfaces.TranslationInterface
import org.json.JSONArray
import org.jsoup.Jsoup
import java.net.URLEncoder

object MiscTranslate {

    // A real browser User-Agent. The old code used Jsoup's default UA against
    // translate.google.com/m, which Google answered with an HTTP 429 "/sorry/"
    // captcha page, so no translation ever came back.
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 12; Pixel 3a) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    @OptIn(DelicateCoroutinesApi::class)
    fun onlineTranslation(
        context: Context,
        fromCode: String,
        toCode: String,
        text: String,
        callBack: TranslationInterface
    ) {

        if (!Misc.checkInternetConnection(context)) {
            callBack.onFailed()
            return
        }

        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val sl = toWebCode(fromCode).ifEmpty { "auto" }
        val tl = toWebCode(toCode)
        val encoded = URLEncoder.encode(text, "utf-8")

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val translation = withContext(Dispatchers.IO) {
                    // translate_a/single (gtx) returns JSON and is far more tolerant
                    // than scraping the translate.google.com/m HTML page.
                    val body = Jsoup.connect(
                        "https://translate.googleapis.com/translate_a/single" +
                            "?client=gtx&sl=$sl&tl=$tl&dt=t&q=$encoded"
                    )
                        .userAgent(USER_AGENT)
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .ignoreContentType(true)
                        .timeout(8000)
                        .execute()
                        .body()
                    parseGtx(body)
                }

                if (translation.isNotEmpty()) {
                    callBack.onTranslate(translation)
                } else {
                    callBack.onFailed()
                }
            } catch (e: Exception) {
                callBack.onFailed()
                e.printStackTrace()
            }
        }
    }

    // gtx responds with [[["translated","source",...],...],...] — the translated
    // text is the concatenation of segment[0] across the first array.
    private fun parseGtx(body: String): String {
        return try {
            val sentences = JSONArray(body).getJSONArray(0)
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                val segment = sentences.optJSONArray(i) ?: continue
                if (!segment.isNull(0)) sb.append(segment.getString(0))
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // ML Kit / BCP-47 codes -> the codes Google's web endpoint expects.
    private fun toWebCode(code: String): String = when (code) {
        Misc.defaultLanguage, "auto" -> "auto"
        "zh" -> "zh-CN"
        "he" -> "iw"
        else -> code
    }

    fun offlineTranslation(
        context: Context,
        fromCode: String,
        toCode: String,
        text: String,
        callBack: TranslationInterface
    ) {
        // Building the options with an unsupported/empty source code (e.g. "auto")
        // throws synchronously, so guard it and fall back to the online endpoint.
        val client = try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(fromCode)
                .setTargetLanguage(toCode)
                .build()
            Translation.getClient(options)
        } catch (e: Exception) {
            onlineTranslation(context, fromCode, toCode, text, callBack)
            return
        }

        client.translate(text).addOnSuccessListener { translatedText ->
            callBack.onTranslate(translatedText)
            client.close()
        }.addOnFailureListener {
            // No on-device model (or translate failed) -> use the online endpoint.
            client.close()
            onlineTranslation(context, fromCode, toCode, text, callBack)
        }
    }


    fun translate(context: Context, text: String, callBack: TranslationInterface) {
        val savedFrom = Misc.getLanguageFrom(context)
        val toCode = Misc.getLanguageTo(context)

        if (savedFrom == Misc.defaultLanguage) {
            // "Default" source -> detect it with ML Kit language-id first, so both
            // the offline and online paths get a concrete source language.
            LanguageIdentification.getClient().identifyLanguage(text)
                .addOnSuccessListener { langCode ->
                    if (langCode == "und") {
                        // Undetermined -> let the online endpoint auto-detect.
                        onlineTranslation(context, "auto", toCode, text, callBack)
                    } else {
                        offlineTranslation(context, langCode, toCode, text, callBack)
                    }
                }
                .addOnFailureListener {
                    onlineTranslation(context, "auto", toCode, text, callBack)
                }
        } else {
            offlineTranslation(context, savedFrom, toCode, text, callBack)
        }
    }

    /**
     * Translate [text] from an explicit [from] language into [to], rather than the
     * saved from/to pair used by the no-arg overload. Used by the word game, whose
     * puzzle words are always English ("en") and need translating into the player's
     * current target language. Routes through [offlineTranslation], which falls back
     * to the online endpoint when there is no on-device model — the same robust path
     * as the no-arg overload, and deliberately NOT the old translate.google.com/m
     * scraper (which now returns HTTP 429).
     */
    fun translate(
        context: Context,
        text: String,
        from: String,
        to: String,
        callBack: TranslationInterface
    ) {
        offlineTranslation(context, from, to, text, callBack)
    }

}
