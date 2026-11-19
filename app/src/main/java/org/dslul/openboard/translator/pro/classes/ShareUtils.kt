package org.dslul.openboard.translator.pro.classes

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.dslul.openboard.inputmethod.latin.R

object ShareUtils {

    /**
     * Opens the share sheet with [text] as plain text.
     *
     * Always puts a `String` in `EXTRA_TEXT`. A TextView that has a text watcher keeps its text as
     * an Editable, and sharing `textView.text` directly sent that object, which arrives as a
     * SpannableString. WhatsApp reads the extra with `getStringExtra()`, gets null for anything
     * that is not a String, and then says it "can't send an empty message".
     *
     * @return false when there was nothing to share (a toast says so).
     */
    fun shareText(context: Context, text: CharSequence?, chooserTitle: String? = null): Boolean {
        val plainText = text?.toString()?.trim().orEmpty()
        if (plainText.isEmpty()) {
            Toast.makeText(context, R.string.nothing_to_share, Toast.LENGTH_SHORT).show()
            return false
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, plainText)
        }
        val chooser = Intent.createChooser(send, chooserTitle ?: context.getString(R.string.share_via))
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.sorry_some_erroe_occurred_please_try_againg, Toast.LENGTH_SHORT)
                .show()
            false
        }
    }
}
