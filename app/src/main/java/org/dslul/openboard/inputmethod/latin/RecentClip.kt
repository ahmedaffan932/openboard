package org.dslul.openboard.inputmethod.latin

/**
 * Rules for the "recently copied" chip in the keyboard's suggestion row (like Gboard): right after
 * copying a link or text, opening the keyboard offers it for one tap. Pure, so it is unit-tested.
 */
object RecentClip {

    /** How long after copying the chip is offered. */
    const val OFFER_WINDOW_MS = 3 * 60 * 1000L

    /** Clips longer than this are not offered (they are still in the clipboard history). */
    const val MAX_LENGTH = 2000

    private val LINK = Regex(
        "(?i)^(https?://\\S+|www\\.\\S+\\.\\S+|[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}(/\\S*)?)$"
    )

    /** Copied within the offer window. A timestamp in the future (clock change) does not count. */
    fun isFresh(copiedAt: Long, now: Long, window: Long = OFFER_WINDOW_MS): Boolean =
        copiedAt in (now - window)..now

    /** Worth offering: not blank and not too long. */
    fun isOfferable(text: CharSequence?): Boolean =
        text != null && text.isNotBlank() && text.length <= MAX_LENGTH

    /** A web address, shown with a link icon. */
    fun isLink(text: CharSequence): Boolean = LINK.matches(text.trim())

    /** The chip's one-line label: line breaks and runs of spaces become single spaces. */
    fun label(text: CharSequence): String = text.trim().replace(Regex("\\s+"), " ")
}
