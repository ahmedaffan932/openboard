package org.dslul.openboard.translator.pro.learn.engine

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** A phrasebook category turned into practice items ([StudyItem.target] = the phrase to learn). */
data class PhraseTopic(val index: Int, val title: String, val phrases: List<StudyItem>)

/**
 * Phrase Practice content: the bundled phrasebook (translated-phrases.zip, 58 languages) read in
 * two languages and paired by position, exactly as the phrasebook screens pair them. Phrases with
 * a blank to fill ("I would like ___ rooms.") are left out: they cannot be quizzed.
 */
object PhraseDeck {

    fun build(sourceJson: String, targetJson: String): List<PhraseTopic> {
        val source = JsonParser.parseString(sourceJson).asJsonObject
        val target = JsonParser.parseString(targetJson).asJsonObject
        val sourceCats = source.entrySet().toList()
        val targetCats = target.entrySet().toList()
        val topics = ArrayList<PhraseTopic>()
        for (c in sourceCats.indices) {
            val (title, srcCat) = sourceCats[c]
            val tgtCat = targetCats.getOrNull(c)?.value?.asJsonObject ?: continue
            topics += PhraseTopic(c, title, pairs(c, srcCat.asJsonObject, tgtCat))
        }
        return topics
    }

    private fun pairs(cat: Int, src: JsonObject, tgt: JsonObject): List<StudyItem> {
        val out = ArrayList<StudyItem>()
        val srcSubs = src.entrySet().toList()
        val tgtSubs = tgt.entrySet().toList()
        for (s in srcSubs.indices) {
            val srcPhrases = srcSubs[s].value.asJsonObject.entrySet().toList()
            val tgtPhrases = tgtSubs.getOrNull(s)?.value?.asJsonObject?.entrySet()?.toList() ?: continue
            for (p in srcPhrases.indices) {
                val meaning = srcPhrases[p].value.asString.trim()
                val text = tgtPhrases.getOrNull(p)?.value?.asString?.trim().orEmpty()
                if (meaning.isEmpty() || text.isEmpty() || "_" in meaning || "_" in text) continue
                val id = "c${cat}s${s}p${p}"
                out += StudyItem(id, text, emptyList(), meaning, text.contains(' '))
            }
        }
        return out
    }
}
