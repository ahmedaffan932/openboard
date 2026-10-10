package org.dslul.openboard.translator.pro.notifications

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.notifications.NotiPoster.TAG
import org.json.JSONObject
import java.io.BufferedInputStream
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipInputStream

/**
 * Content of the Daily notification (Figma NotiDaily): one phrase from the bundled phrasebook
 * (assets/translated-phrases.zip), "in <the user's target language>". Offline and free: the
 * phrase is only shown here; the Translate screen translates and reads it when the user taps.
 *
 * One phrase per calendar day, so both daily slots carry the same "phrase of the day".
 */
object PhraseOfTheDay {

    class Pick(
        /** The phrase, in [phraseLanguage] - the language the user reads and translates from. */
        val phrase: String,
        val phraseLanguage: String,
        /** The user's "to" language ([Misc.getLanguageTo]), which the Translate screen uses. */
        val targetLanguage: String,
    )

    /** Long enough to be useful, short enough to fit a notification line in any language. */
    private const val MAX_PHRASE_CHARS = 48

    /** Same layout as PhrasesAssetReader's: one `translated-phrases/<lang>.json` per language. */
    private const val ZIP_ASSET = "translated-phrases.zip"
    private const val ENTRY_PREFIX = "translated-phrases/"

    fun pick(context: Context): Pick? {
        var language = phraseLanguage(context)
        var phrases = phrasesIn(context, language)
        if (phrases.isEmpty() && language != "en") {
            // Language not in the phrasebook. English is only a safe stand-in when the Translate
            // screen auto-detects the source; with a fixed "from" it would read English as that
            // language, so there is no honest phrase to offer today.
            if (Misc.getLanguageFrom(context) != Misc.defaultLanguage) return null
            language = "en"
            phrases = phrasesIn(context, language)
        }
        if (phrases.isEmpty()) return null

        val index = Math.floorMod(localEpochDay() * 2_654_435_761L, phrases.size.toLong()).toInt()
        val phrase = phrases[index]

        // The Translate screen always translates into the user's "to" language, so that is the
        // language named - and "Where is the station?" in English is no lesson to an English
        // reader. That setup (translating into the language you read) is rare; skip the day.
        val target = Misc.getLanguageTo(context)
        if (base(target) == base(language)) return null
        return Pick(phrase, language, target)
    }

    /**
     * A context in the app's own language (Misc.setAppLanguage), not the system's: the phrase is
     * in that language, so the sentence around it and the language name must be too.
     */
    fun appLanguageContext(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(appLocale(context))
        return context.createConfigurationContext(config)
    }

    /** "Spanish" / "Spanisch" / "espagnol" - the target's name in the app's language. */
    fun languageName(context: Context, code: String): String {
        val name = Locale.forLanguageTag(code.replace('_', '-')).getDisplayLanguage(appLocale(context))
        return name.ifBlank { code }
    }

    /** The locale `Activity.setAppLanguage()` applies to every screen. */
    private fun appLocale(context: Context): Locale = Locale(Misc.getAppLanguage(context))

    /**
     * The language the Translate screen translates from, so the phrase it receives is the one it
     * expects: the user's "from" language, or the app's language when "from" is auto-detect.
     */
    private fun phraseLanguage(context: Context): String {
        val from = Misc.getLanguageFrom(context)
        val code = if (from == Misc.defaultLanguage) Misc.getAppLanguage(context) else from
        return base(code)
    }

    private fun base(code: String): String = code.substringBefore('-').substringBefore('_').lowercase()

    /** The phrasebook's own file name for a base language code (Hebrew is `he` there). */
    private fun entryLanguage(lang: String): String = if (lang == "iw") "he" else lang

    /**
     * Every phrase of `<lang>.json` that can stand alone, in file order; empty when that language is
     * not bundled. The phrasebook's fill-in templates ("We are ___ people.") and choice lists
     * ("American/British/...") are for the user to complete there - read out on their own they
     * are not a phrase.
     */
    private fun phrasesIn(context: Context, lang: String): List<String> {
        val raw = readEntry(context, entryLanguage(lang)) ?: return emptyList()
        return try {
            val json = JSONObject(raw)
            buildList {
                for (category in json.keys()) {
                    val sections = json.getJSONObject(category)
                    for (section in sections.keys()) {
                        val items = sections.getJSONObject(section)
                        for (key in items.keys()) {
                            val phrase = items.optString(key).trim()
                            if (phrase.isNotEmpty() && phrase.length <= MAX_PHRASE_CHARS &&
                                '_' !in phrase && '/' !in phrase
                            ) add(phrase)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // A bundled entry that does not parse would silently switch the notification off for
            // that language, so it is logged apart from "not bundled".
            Log.e(TAG, "PhraseOfTheDay: $lang.json in $ZIP_ASSET is malformed", e)
            emptyList()
        }
    }

    /** The entry's text, or null when the language is not in the zip. Read in place, never extracted. */
    private fun readEntry(context: Context, lang: String): String? = try {
        val entryName = "$ENTRY_PREFIX$lang.json"
        context.assets.open(ZIP_ASSET).use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                var text: String? = null
                while (entry != null && text == null) {
                    if (!entry.isDirectory && entry.name == entryName) {
                        text = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zip.nextEntry
                }
                text
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun localEpochDay(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }
}
