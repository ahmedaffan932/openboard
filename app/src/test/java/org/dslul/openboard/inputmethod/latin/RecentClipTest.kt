package org.dslul.openboard.inputmethod.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentClipTest {

    @Test
    fun isFresh_onlyWithinTheWindow() {
        val now = 1_000_000_000L
        assertTrue(RecentClip.isFresh(now - 10_000, now))
        assertTrue(RecentClip.isFresh(now - RecentClip.OFFER_WINDOW_MS, now))
        assertFalse(RecentClip.isFresh(now - RecentClip.OFFER_WINDOW_MS - 1, now))
        // A copy time in the future (clock changed) is not trusted.
        assertFalse(RecentClip.isFresh(now + 60_000, now))
    }

    @Test
    fun isLink_recognisesWebAddresses() {
        assertTrue(RecentClip.isLink("https://example.com/page?x=1"))
        assertTrue(RecentClip.isLink("http://example.org"))
        assertTrue(RecentClip.isLink("www.example.com"))
        assertTrue(RecentClip.isLink("example.co.uk/path"))
        assertTrue(RecentClip.isLink("  https://example.com  "))
    }

    @Test
    fun isLink_plainTextIsNotALink() {
        assertFalse(RecentClip.isLink("Good morning my friend"))
        assertFalse(RecentClip.isLink("see example.com today"))
        assertFalse(RecentClip.isLink("hello."))
    }

    @Test
    fun isOfferable_rejectsBlankAndHugeTexts() {
        assertTrue(RecentClip.isOfferable("hi"))
        assertFalse(RecentClip.isOfferable(null))
        assertFalse(RecentClip.isOfferable("   "))
        assertFalse(RecentClip.isOfferable("x".repeat(RecentClip.MAX_LENGTH + 1)))
    }

    @Test
    fun label_isOneLine() {
        assertEquals("line one line two", RecentClip.label("  line one\n\n  line two "))
    }
}
