package org.dslul.openboard.translator.pro.classes

import android.content.Context
import android.os.StrictMode
import android.view.View
import android.widget.Toast
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.interfaces.TranslationInterface
import org.jsoup.Jsoup
import java.net.URLEncoder

object MiscTranslate {

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

        val encoded = URLEncoder.encode(text, "utf-8")
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val translation = withContext(Dispatchers.IO) {
                    val doc =
                        Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                            .timeout(4000)
                            .get()
                    val element = doc.getElementsByClass("result-container")
                    element.text()
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

    fun offlineTranslation(
        context: Context,
        fromCode: String,
        toCode: String,
        text: String,
        callBack: TranslationInterface
    ) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(fromCode)
            .setTargetLanguage(toCode)
            .build()
        val obj = Translation.getClient(options)

        obj.translate(text).addOnSuccessListener { translatedText ->
            callBack.onTranslate(translatedText)

        }.addOnFailureListener { exception ->
            onlineTranslation(context, fromCode, toCode, text, callBack)
        }
    }


    fun translate(context: Context, text: String, callBack: TranslationInterface) {
        val fromCode = if (Misc.getLanguageFrom(context) == Misc.defaultLanguage) ""
        else if (Misc.getLanguageFrom(context) == "zh") "zh-CN"
        else if (Misc.getLanguageFrom(context) == "he") "iw"
        else Misc.getLanguageFrom(context)

        val toCode = when (Misc.getLanguageTo(context)) {
            "zh" -> "zh-CN"
            "he" -> "iw"
            else -> Misc.getLanguageTo(context)
        }

        offlineTranslation(context, fromCode, toCode, text, callBack)
    }

}