package org.dslul.openboard.translator.pro.classes

/**
 * What the swap button on a translation screen does, as a pure function so it can be unit-tested.
 *
 * Like Google Translate: the languages swap, and when there is a translation the texts swap too.
 * The translation becomes the new input, which the user can edit and translate again, and their
 * original text comes back as the translation without another request. The old behaviour kept the
 * input and re-translated it in the swapped direction, i.e. treated it as being in the other
 * language.
 */
object LanguageSwap {

    data class State(
        val from: String,
        val to: String,
        val input: String,
        val output: String,
    )

    /**
     * @param detectCode the From value meaning "detect the language" ([Misc.defaultLanguage]).
     * @param detectedFrom the language detected for the current translation, if known. Used when
     *        From is "detect", since "detect" cannot become the target language.
     * @return the swapped state, or null when From is "detect" and no language was detected.
     */
    fun swap(current: State, detectCode: String, detectedFrom: String?): State? {
        val oldFrom = if (current.from == detectCode) {
            detectedFrom?.takeIf { it.isNotBlank() && it != detectCode } ?: return null
        } else {
            current.from
        }
        return if (current.output.isNotBlank()) {
            State(from = current.to, to = oldFrom, input = current.output, output = current.input)
        } else {
            // Nothing translated yet: only the languages change. Not re-translating here, since
            // the input is still written in the old From language.
            State(from = current.to, to = oldFrom, input = current.input, output = "")
        }
    }
}
