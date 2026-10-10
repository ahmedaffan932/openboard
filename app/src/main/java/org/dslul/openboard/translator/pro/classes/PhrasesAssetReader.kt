package org.dslul.openboard.translator.pro.classes

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

object PhrasesAssetReader {
    private const val ZIP_ASSET = "translated-phrases.zip"
    private const val ENTRY_PREFIX = "translated-phrases/"
    private val languageCache = HashMap<String, String>()

    suspend fun getLanguageJson(context: Context, language: String): String =
        withContext(Dispatchers.IO) {
            val languageCode = normalizeLanguageCode(language)

            synchronized(languageCache) {
                languageCache[languageCode]?.let { return@withContext it }
            }

            val entryName = "$ENTRY_PREFIX$languageCode.json"
            context.assets.open(ZIP_ASSET).use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name == entryName) {
                            val valueString = zipInputStream.readBytes().toString(Charsets.UTF_8)
                            synchronized(languageCache) {
                                languageCache[languageCode] = valueString
                            }
                            return@withContext valueString
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                }
            }

            throw IllegalArgumentException("Phrasebook language not found: $languageCode")
        }

    private fun normalizeLanguageCode(language: String): String {
        return when (language) {
            Misc.defaultLanguage -> "en"
            "iw" -> "he"
            "zh-CN", "zh-TW" -> "zh"
            else -> language
        }
    }
}
