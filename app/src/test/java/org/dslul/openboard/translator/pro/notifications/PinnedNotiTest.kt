package org.dslul.openboard.translator.pro.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedNotiTest {

    // --- PinnedNoti.isCurrent: a card already in the tray is replaced once, then left alone ---

    @Test
    fun currentCard_isKept() {
        assertTrue(PinnedNoti.isCurrent(version = 1, language = "de", appLanguage = "de"))
    }

    @Test
    fun cardWithoutMarker_isReplaced() {
        // A card with no extras reads as version 0, no language.
        assertFalse(PinnedNoti.isCurrent(version = 0, language = null, appLanguage = "en"))
    }

    @Test
    fun cardInAnotherAppLanguage_isReplaced() {
        assertFalse(PinnedNoti.isCurrent(version = 1, language = "en", appLanguage = "fr"))
    }

    // --- QuickAccessActivity.Tile: one PendingIntent and one task identity per tile ---

    @Test
    fun tiles_haveDistinctActions() {
        // Task reuse compares intents without extras: two tiles sharing an action would bring back
        // the other tile's task instead of opening their own tool.
        val actions = QuickAccessActivity.Tile.entries.map { it.action }
        assertEquals(actions.size, actions.toSet().size)
    }

    @Test
    fun tiles_haveDistinctRequestCodes_clearOfOtherBands() {
        val codes = QuickAccessActivity.Tile.entries.map { it.requestCode }
        assertEquals(codes.size, codes.toSet().size)
        // 1234: the card itself; 3100-3400 the alarms, 3900+ dismiss, 4000+ NotiRouterActivity.
        codes.forEach { code ->
            assertTrue("request code $code collides", code != 1234 && code < 3100)
        }
    }

    @Test
    fun tiles_coverTheDesign() {
        assertEquals(
            listOf("translate", "voice", "camera", "file"),
            QuickAccessActivity.Tile.entries.map { it.analyticsName }
        )
    }
}
