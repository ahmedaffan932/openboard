package org.dslul.openboard.inputmethod.keyboard.clipboard

import android.content.Context
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.StrictMode
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.nl.languageid.LanguageIdentification
import org.dslul.openboard.inputmethod.latin.ClipboardHistoryEntry
import org.dslul.openboard.inputmethod.latin.ClipboardHistoryManager
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.databinding.ClipboardEntryKeyBinding
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.MiscTranslate
import org.dslul.openboard.translator.pro.interfaces.TranslationInterface
import org.jsoup.Jsoup
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Locale

class ClipboardAdapter(
    val context: Context,
    val keyEventListener: OnKeyEventListener
) : RecyclerView.Adapter<ClipboardAdapter.ClipboardItemViewHolder>() {

    var clipboardHistoryManager: ClipboardHistoryManager? = null

    var pinnedIconResId = 0
    var itemBackgroundId = 0
    var itemTypeFace: Typeface? = null
    var itemTextColor = 0
    var itemTextSize = 0f

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipboardItemViewHolder {
        val binding =
            ClipboardEntryKeyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClipboardItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClipboardItemViewHolder, position: Int) {
        setContent(holder, getItem(position))
        holder.apply {

//            binding.llClipboardContent.setOnTouchListener(object : View.OnTouchListener{
//                override fun onTouch(view: View?, event: MotionEvent?): Boolean {
//                        if (event?.actionMasked != MotionEvent.ACTION_DOWN) {
//                            return false
//                        }
//                        keyEventListener.onKeyDown(view?.tag as Long)
//                        return false
//                    }
//            })

            binding.llClipboardContent.setOnClickListener {
                keyEventListener.onKeyUp(/*binding.root.tag as Long*/getItem(position)?.content.toString())
            }
            binding.llTranslatedContent.setOnClickListener {
                keyEventListener.onKeyUp(/*binding.root.tag as Long*/binding.clipboardTranslatedContent.text.toString())
            }

            binding.clipboardEntryPinnedIcon.setOnClickListener {
                clipboardHistoryManager?.toggleClipPinned(binding.root.tag as Long)
            }

        }
    }


    fun setContent(holder: ClipboardItemViewHolder, historyEntry: ClipboardHistoryEntry?) {
        holder.apply {
            binding.root.tag = historyEntry?.timeStamp
            binding.clipboardEntryContent.text = historyEntry?.content

            var from = Misc.getLanguageFrom(context)
            var to = Misc.getLanguageTo(context)
            val languageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(historyEntry?.content.toString())
                .addOnSuccessListener { languageCode ->
                    Log.d(Misc.logKey, languageCode)
                    if (languageCode != "und") {
                        if (languageCode == to) {
                            to = Misc.getLanguageFrom(context)
                            from = Misc.getLanguageTo(context)
                        }
                    }
                    binding.textLngFrom.text = Locale(from).displayName

                    to = when (to) {
                        "zh" -> "zh-CN"
                        "he" -> "iw"
                        else -> to
                    }
                    from = when (from) {
                        "zh" -> "zh-CN"
                        "he" -> "iw"
                        else -> from
                    }
                    jugarTranslation(holder, from, to, historyEntry?.content.toString())

                }.addOnFailureListener {
                    binding.textLngFrom.text = Locale(from).displayName
                    jugarTranslation(holder, from, to, historyEntry?.content.toString())

                }


            binding.clipboardEntryPinnedIcon.setImageResource(
                if (historyEntry?.isPinned == true)
                    R.drawable.ic_round_push_pin_24
                else
                    R.drawable.ic_outline_push_pin_24
            )
//            binding.clipboardEntryPinnedIcon.visibility =
//                if (historyEntry?.isPinned == true) View.VISIBLE else View.GONE
        }
    }

    private fun jugarTranslation(
        holder: ClipboardItemViewHolder,
        from: String,
        to: String,
        text: String,
    ) {
        holder.apply {
            binding.shimmerView.visibility = View.VISIBLE
            binding.llTranslatedContent.visibility = View.GONE
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            binding.textLngFrom.text = Locale(from).displayName
            binding.textLngTo.text = Locale(to).displayName


            MiscTranslate.offlineTranslation(
                context,
                from,
                to,
                text,
                object : TranslationInterface {
                    override fun onTranslate(translation: String) {
                        binding.clipboardTranslatedContent.text = translation
                        binding.llTranslatedContent.visibility = View.VISIBLE
                        binding.shimmerView.visibility = View.GONE
                    }

                    override fun onFailed() {

                    }
                })

//            TranslateTask(holder, from, to, text).execute()
        }
    }

    private inner class TranslateTask(
        val holder: ClipboardItemViewHolder,
        from: String,
        to: String?,
        text: String?,
    ) :
        AsyncTask<Void?, Void?, String?>() {
        private var fromCode: String? = null
        private var toCode: String? = null
        private var encodedText: String? = null

        init {
            fromCode = when (from) {
                Misc.defaultLanguage -> {
                    ""
                }

                "zh" -> {
                    "zh-CN"
                }

                "he" -> {
                    "iw"
                }

                else -> {
                    from
                }
            }

            toCode = when (to) {
                "zh" -> "zh-CN"
                "he" -> "iw"
                else -> to
            }
            try {
                encodedText = URLEncoder.encode(text, "utf-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }

        override fun onPostExecute(result: String?) {
            if (result != null) {
                holder.apply {
                    Log.d(Misc.logKey, result)
                    binding.clipboardTranslatedContent.text = result
                    binding.llTranslatedContent.visibility = View.VISIBLE
                    binding.shimmerView.visibility = View.GONE
                }
            }

            Misc.isTranslated.postValue(true)
        }

        override fun doInBackground(vararg params: Void?): String? {
            try {
                val doc =
                    Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encodedText")
                        .get()

                val element = doc.getElementsByClass("result-container")

                if (element.text() != "" && !TextUtils.isEmpty(element.text())) {
                    return element.text()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }
    }


    private fun getItem(position: Int) = clipboardHistoryManager?.getHistoryEntry(position)

    override fun getItemCount() = clipboardHistoryManager?.getHistorySize() ?: 0

    inner class ClipboardItemViewHolder(val binding: ClipboardEntryKeyBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.clipboardEntryPinnedIcon.apply {
                setImageResource(pinnedIconResId)
            }
            binding.clipboardEntryContent.apply {
                typeface = itemTypeFace
                setTextColor(itemTextColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize)
            }


            binding.clipboardTranslatedContent.apply {
                typeface = itemTypeFace
                setTextColor(itemTextColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize)
            }

            binding.root.setBackgroundResource(itemBackgroundId)

        }
    }

}