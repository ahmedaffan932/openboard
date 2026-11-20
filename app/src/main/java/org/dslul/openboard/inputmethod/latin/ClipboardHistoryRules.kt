package org.dslul.openboard.inputmethod.latin

/**
 * How the keyboard's clipboard history changes when something is copied, kept free of Android
 * types so it is unit-tested.
 *
 * Like Gboard: the last [MAX_UNPINNED] copied texts are kept (newest first), pinned clips stay
 * until unpinned, and copying a text that is already in the list moves it to the top instead of
 * adding it twice.
 */
object ClipboardHistoryRules {

    /** The client asked for the last 10 copied texts. */
    const val MAX_UNPINNED = 10

    /**
     * Adds a newly copied text to [entries] (kept sorted: pinned first, then newest first).
     *
     * @return false when nothing changed: the same clip was already recorded, or the text is
     *         already pinned.
     */
    fun add(entries: MutableList<ClipboardHistoryEntry>, content: CharSequence, timeStamp: Long,
            maxUnpinned: Int = MAX_UNPINNED): Boolean {
        if (content.isEmpty()) return false
        // Android can report the same clip more than once; its timestamp identifies it.
        if (entries.any { it.timeStamp == timeStamp }) return false
        val text = content.toString()
        val existing = entries.firstOrNull { it.content.toString() == text }
        if (existing != null) {
            if (existing.isPinned) return false
            entries.remove(existing)
        }
        entries.add(ClipboardHistoryEntry(timeStamp, content))
        entries.sort()
        trim(entries, maxUnpinned)
        return true
    }

    /** Drops the oldest unpinned entries beyond [maxUnpinned]. */
    fun trim(entries: MutableList<ClipboardHistoryEntry>, maxUnpinned: Int = MAX_UNPINNED) {
        var unpinnedSeen = 0
        val iterator = entries.iterator()
        // Sorted newest first, so the first [maxUnpinned] unpinned entries are the ones to keep.
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.isPinned) continue
            unpinnedSeen++
            if (unpinnedSeen > maxUnpinned) iterator.remove()
        }
    }

    /**
     * Merges entries read from disk into the in-memory ones (which may already hold the current
     * clip), without duplicating a timestamp or a text.
     */
    fun merge(entries: MutableList<ClipboardHistoryEntry>, saved: List<ClipboardHistoryEntry>,
              maxUnpinned: Int = MAX_UNPINNED) {
        for (entry in saved) {
            val text = entry.content.toString()
            if (entries.any { it.timeStamp == entry.timeStamp || it.content.toString() == text }) {
                continue
            }
            entries.add(entry)
        }
        entries.sort()
        trim(entries, maxUnpinned)
    }
}
