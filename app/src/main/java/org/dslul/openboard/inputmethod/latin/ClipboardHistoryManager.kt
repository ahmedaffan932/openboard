package org.dslul.openboard.inputmethod.latin

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import org.dslul.openboard.inputmethod.compat.ClipboardManagerCompat
import org.dslul.openboard.inputmethod.latin.suggestions.SuggestionStripView
import org.dslul.openboard.inputmethod.latin.utils.JsonUtils
import org.dslul.openboard.translator.pro.classes.Misc
import java.io.File
import java.lang.Exception
import java.util.*

class ClipboardHistoryManager(
        private val latinIME: LatinIME,
//    private val mSuggestionStripView: SuggestionStripView
) : ClipboardManager.OnPrimaryClipChangedListener {

    private lateinit var pinnedHistoryClipsFile: File
    private lateinit var clipboardManager: ClipboardManager
    private val historyEntries: MutableList<ClipboardHistoryEntry> = LinkedList()
    private var onHistoryChangeListener: OnHistoryChangeListener? = null
    private var onHistoryChanged: OnHistoryChanged? = null

    /**
     * Whether the saved history has been read back yet. Until then nothing is saved: the clip
     * recorded at start-up would otherwise be written first and replace the whole saved history.
     */
    private var savedHistoryLoaded = false

    fun onCreate() {
        pinnedHistoryClipsFile = File(latinIME.filesDir, PINNED_CLIPS_DATA_FILE_NAME)
        clipboardManager = latinIME.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        fetchPrimaryClip()
        clipboardManager.addPrimaryClipChangedListener(this)
        startLoadPinnedClipsFromDisk()
    }

    /** The saved history (pinned and recent clips) has been read from disk. */
    fun onPinnedClipsAvailable(pinnedClips: List<ClipboardHistoryEntry>) {
        ClipboardHistoryRules.merge(historyEntries, pinnedClips)
        savedHistoryLoaded = true
        onHistoryChangeListener?.onClipboardHistoryChanged()
        // Saves the merged list, including anything copied while it was loading.
        startSavePinnedClipsToDisk()
    }

    fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(this)
    }

    override fun onPrimaryClipChanged() {
        // Make sure we read clipboard content only if history settings is set
        if (latinIME.mSettings.current?.mClipboardHistoryEnabled == true) {
//            mSuggestionStripView.onTextCopy()

            fetchPrimaryClip()
        }
    }

    private fun fetchPrimaryClip() {
        val clipData = clipboardManager.primaryClip ?: return
        if (clipData.itemCount == 0) return
        // Passwords and other secrets copied from apps that mark them sensitive (Android 13+)
        // are never kept in the history.
        if (isSensitive(clipData.description)) return
        clipData.getItemAt(0)?.let { clipItem ->
            // Starting from API 30, onPrimaryClipChanged() can be called multiple times
            // for the same clip. Clips are identified by their timestamps (ClipboardHistoryRules).
            val timeStamp = ClipboardManagerCompat.getClipTimestamp(clipData)
                ?: System.currentTimeMillis()
            val content = clipItem.coerceToText(latinIME)
            if (TextUtils.isEmpty(content)) return

            if (!ClipboardHistoryRules.add(historyEntries, content, timeStamp)) return
            onHistoryChangeListener?.onClipboardHistoryChanged()
            onHistoryChanged?.onChanged()
            startSavePinnedClipsToDisk()
        }
    }

    fun toggleClipPinned(ts: Long) {
        val from = historyEntries.indexOfFirst { it.timeStamp == ts }
        val historyEntry = historyEntries[from].apply {
            timeStamp = System.currentTimeMillis()
            isPinned = !isPinned
        }
        sortHistoryEntries()
        // Unpinning can push the list over the limit of recent clips.
        ClipboardHistoryRules.trim(historyEntries)
        onHistoryChangeListener?.onClipboardHistoryChanged()
        startSavePinnedClipsToDisk()
    }

    fun clearHistory() {
        ClipboardManagerCompat.clearPrimaryClip(clipboardManager)
        val pos = historyEntries.indexOfFirst { !it.isPinned }
        val count = historyEntries.count { !it.isPinned }
        historyEntries.removeAll { !it.isPinned }
        if (onHistoryChangeListener != null) {
            onHistoryChangeListener?.onClipboardHistoryEntriesRemoved(pos, count)
        }
        startSavePinnedClipsToDisk()
    }

    private fun sortHistoryEntries() {
        historyEntries.sort()
    }

    private fun checkClipRetentionElapsed() {
        val mins = latinIME.mSettings.current.mClipboardHistoryRetentionTime
        if (mins <= 0) return // No retention limit
        val maxClipRetentionTime = mins * 60 * 1000L
        val now = System.currentTimeMillis()
        if (historyEntries.removeAll { !it.isPinned && (now - it.timeStamp) > maxClipRetentionTime }) {
            startSavePinnedClipsToDisk()
        }
    }

    // We do not want to update history while user is visualizing it, so we check retention only
    // when history is about to be shown
    fun prepareClipboardHistory() = checkClipRetentionElapsed()

    fun getHistorySize() = historyEntries.size

    fun getHistoryEntry(position: Int) = historyEntries[position]

    fun getHistoryEntryContent(timeStamp: Long) = historyEntries.first { it.timeStamp == timeStamp }

    fun setHistoryChangeListener(l: OnHistoryChangeListener?) {
        onHistoryChangeListener = l
    }
    fun setHistoryChanged(l: OnHistoryChanged?) {
        onHistoryChanged = l
    }

    fun retrieveClipboardContent(): CharSequence {
        val clipData = clipboardManager.primaryClip ?: return ""
        if (clipData.itemCount == 0) return ""
        return clipData.getItemAt(0)?.coerceToText(latinIME) ?: ""
    }

    private fun startLoadPinnedClipsFromDisk() {
        object : Thread("$TAG-load") {
            override fun run() {
                loadFromDisk()
            }
        }.start()
    }

    private fun loadFromDisk() {
        // Debugging
        if (pinnedHistoryClipsFile.exists() && !pinnedHistoryClipsFile.canRead()) {
            Log.w(TAG, "Attempt to read pinned clips file $pinnedHistoryClipsFile without permission")
        }
        var list = emptyList<ClipboardHistoryEntry>()
        try {
            if (pinnedHistoryClipsFile.exists()) {
                val bytes = Base64.decode(pinnedHistoryClipsFile.readText(), Base64.DEFAULT)
                list = JsonUtils.jsonBytesToHistoryEntryList(bytes)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't retrieve $pinnedHistoryClipsFile content", e)
        }
        latinIME.mHandler.postUpdateClipboardPinnedClips(list)
    }

    /**
     * Saves the history, pinned and recent clips alike. Only pinned clips used to be saved, so the
     * recent ones were lost whenever Android stopped the keyboard. Very long texts stay in memory
     * only. (The file is in the app's private storage.)
     */
    private fun startSavePinnedClipsToDisk() {
        if (!savedHistoryLoaded) return
        val localCopy = historyEntries
            .filter { it.isPinned || it.content.length <= MAX_SAVED_CLIP_LENGTH }
            .map { it.copy() }
        object : Thread("$TAG-save") {
            override fun run() {
                saveToDisk(localCopy)
            }
        }.start()
    }

    private fun saveToDisk(list: List<ClipboardHistoryEntry>) {
        // Debugging
        if (pinnedHistoryClipsFile.exists() && !pinnedHistoryClipsFile.canWrite()) {
            Log.w(TAG, "Attempt to write pinned clips file $pinnedHistoryClipsFile without permission")
        }
        try {
            pinnedHistoryClipsFile.createNewFile()
            val jsonStr = JsonUtils.historyEntryListToJsonStr(list)
            if (!TextUtils.isEmpty(jsonStr)) {
                val rawText = Base64.encodeToString(jsonStr.encodeToByteArray(), Base64.DEFAULT)
                pinnedHistoryClipsFile.writeText(rawText)
            } else {
                pinnedHistoryClipsFile.writeText("")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't write to $pinnedHistoryClipsFile", e)
        }
    }

    interface OnHistoryChangeListener {
        fun onClipboardHistoryEntryAdded(at: Int){}
        fun onClipboardHistoryEntriesRemoved(pos: Int, count: Int)
        fun onClipboardHistoryEntryMoved(from: Int, to: Int)
        /** Several entries may have been added, moved or dropped at once. */
        fun onClipboardHistoryChanged() {}
    }


    interface OnHistoryChanged{
        fun onChanged()
    }
    companion object {
        // The name is historical: the file now holds the recent clips too.
        const val PINNED_CLIPS_DATA_FILE_NAME = "pinned_clips.data"
        const val TAG = "ClipboardHistoryManager"
        private const val MAX_SAVED_CLIP_LENGTH = 5000
        // ClipDescription.EXTRA_IS_SENSITIVE (Android 13), spelled out to compile against older APIs.
        private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

        fun isSensitive(description: ClipDescription?): Boolean =
            description?.extras?.getBoolean(EXTRA_IS_SENSITIVE, false) == true
    }
}