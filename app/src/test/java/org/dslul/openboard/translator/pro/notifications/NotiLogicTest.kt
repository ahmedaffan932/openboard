package org.dslul.openboard.translator.pro.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotiLogicTest {

    // --- NotiConfig.parseSlots ---

    @Test
    fun parseSlots_defaultSpec() {
        assertEquals(
            listOf(11 to 30, 17 to 0, 20 to 0),
            NotiConfig.parseSlots("11:30,17:00,20:00")
        )
    }

    @Test
    fun parseSlots_toleratesWhitespaceAndLeadingZeros() {
        assertEquals(
            listOf(8 to 0, 19 to 0),
            NotiConfig.parseSlots(" 08:00 , 19:00 ")
        )
    }

    @Test
    fun parseSlots_skipsInvalidEntries() {
        assertEquals(
            listOf(9 to 15),
            NotiConfig.parseSlots("25:00,aa:bb,12,9:15,12:60,")
        )
    }

    @Test
    fun parseSlots_emptyString() {
        assertTrue(NotiConfig.parseSlots("").isEmpty())
    }

    // --- NotiContentStore.pickRandomExcluding (spec: never same content twice in a row) ---

    private fun content(id: Int) = LockscreenContent(id, "t$id", "d$id", "b$id", "translate")

    @Test
    fun rotation_neverRepeatsConsecutively() {
        val list = listOf(content(1), content(2), content(3))
        var last = -1
        repeat(500) {
            val picked = NotiContentStore.pickRandomExcluding(list, last)
            assertNotEquals("picked same content twice in a row", last, picked.id)
            last = picked.id
        }
    }

    @Test
    fun rotation_twoItemsAlternate() {
        val list = listOf(content(1), content(2))
        var last = 1
        repeat(50) {
            val picked = NotiContentStore.pickRandomExcluding(list, last)
            assertNotEquals(last, picked.id)
            last = picked.id
        }
    }

    @Test
    fun rotation_singleItemStillReturns() {
        val list = listOf(content(7))
        assertEquals(7, NotiContentStore.pickRandomExcluding(list, 7).id)
    }

    @Test
    fun rotation_noHistoryPicksAny() {
        val list = listOf(content(1), content(2))
        val picked = NotiContentStore.pickRandomExcluding(list, -1)
        assertTrue(picked.id in listOf(1, 2))
    }
}
