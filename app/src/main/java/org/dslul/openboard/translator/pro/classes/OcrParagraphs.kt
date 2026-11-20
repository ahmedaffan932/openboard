package org.dslul.openboard.translator.pro.classes

import kotlin.math.roundToInt

/**
 * Turns camera OCR output into text worth translating. Pure functions, so they are unit-tested.
 *
 * The camera screen used to translate every recognised *line* on its own. A sentence that wraps
 * over three lines became three unrelated fragments, which is why camera translations read as
 * random. Lines are now joined back into the paragraphs (OCR text blocks) they came from.
 */
object OcrParagraphs {

    /**
     * Joins the lines of one text block into one paragraph. A word split across lines with a
     * hyphen ("transla-" / "tion") is joined back together; the rest are joined with [separator]
     * (a space, or nothing for Chinese and Japanese, which do not separate words).
     */
    fun joinLines(lines: List<String>, separator: String = " "): String {
        val sb = StringBuilder()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (sb.isEmpty()) {
                sb.append(line)
                continue
            }
            val last = sb[sb.length - 1]
            val hyphenated = (last == '-' || last == '­') && sb.length >= 2 &&
                sb[sb.length - 2].isLetter() && line[0].isLowerCase()
            if (hyphenated) {
                sb.setLength(sb.length - 1)
                sb.append(line)
            } else {
                sb.append(separator).append(line)
            }
        }
        return sb.toString()
    }

    /** One paragraph per block (each block given as its lines), skipping empty ones. */
    fun build(blocks: List<List<String>>, separator: String = " "): List<String> =
        blocks.map { joinLines(it, separator) }.filter { it.isNotBlank() }

    /**
     * The rotation of most of the text, snapped to 0, 90, 180 or 270 degrees, from the angles of
     * the recognised lines. Each line votes with its character count, so a stray short line
     * cannot decide it. 0 when there are no lines.
     */
    fun dominantRotation(lineAngles: List<Float>, lineLengths: List<Int>): Int {
        if (lineAngles.isEmpty()) return 0
        val votes = IntArray(4)
        for (i in lineAngles.indices) {
            val snapped = Math.floorMod((lineAngles[i] / 90f).roundToInt(), 4)
            votes[snapped] += lineLengths.getOrElse(i) { 1 }.coerceAtLeast(1)
        }
        var best = 0
        for (q in 1..3) if (votes[q] > votes[best]) best = q
        return best * 90
    }
}
