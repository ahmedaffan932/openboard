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
                    jugarTranslation(holder, from, to, historyEntry?.content.toString())
                }.addOnFailureListener {
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
            if (from == Misc.defaultLanguage) {
                val languageIdentifier = LanguageIdentification.getClient()
                languageIdentifier.identifyLanguage(text).addOnSuccessListener { languageCode ->
                    if (languageCode != "und") {
                        binding.textLngFrom.text = Locale(languageCode).displayName
                    }
                }.addOnFailureListener {}
            } else {
                binding.textLngFrom.text = Locale(from).displayName
            }


            binding.textLngTo.text = Locale(to).displayName

            TranslateTask(holder, from, to, text).execute()
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

//    inner class ViewHolder(
//        view: View
//    ) : RecyclerView.ViewHolder(view)/*, View.OnClickListener, View.OnTouchListener,
//        View.OnLongClickListener */{
//
////        private val pinnedIconView: ImageView
////        private val contentView: TextView
////        private val translatedContentView: TextView
////        private val languageFromContentView: TextView
////        private val languageToContentView: TextView
////        private val llTranslatedContent: LinearLayout
////        private val shimmerView: ShimmerFrameLayout
//
//        init {
//            view.apply {
//
//
//                setOnTouchListener(object : View.OnTouchListener{
//                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//                        if (event?.actionMasked != MotionEvent.ACTION_DOWN) {
//                            return false
//                        }
//                        keyEventListener.onKeyDown(view.tag as Long)
//                        return false
//                    }
//
//                })
//
//                setOnLongClickListener {
//                    clipboardHistoryManager?.toggleClipPinned(view.tag as Long)
//                    true
//                }
//                setBackgroundResource(itemBackgroundId)
//            }
//
//            /*pinnedIconView = view.findViewById<ImageView>(R.id.clipboard_entry_pinned_icon).apply {
//                visibility = View.GONE
//                setImageResource(pinnedIconResId)
//            }
//            contentView = view.findViewById<TextView>(R.id.clipboard_entry_content).apply {
//                typeface = itemTypeFace
//                setTextColor(itemTextColor)
//                setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize)
//            }
//
//            llTranslatedContent =
//                view.findViewById(R.id.llTranslatedContent)
//            shimmerView =
//                view.findViewById(R.id.shimmerView)
//
//            languageFromContentView =
//                view.findViewById(R.id.textLngFrom)
//            languageToContentView =
//                view.findViewById(R.id.textLngTo)
//
//            translatedContentView =
//                view.findViewById<TextView>(R.id.clipboard_translated_content).apply {
//                    typeface = itemTypeFace
//                    setTextColor(itemTextColor)
//                    setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize)
//                }*/
////            clipboardLayoutParams.setItemProperties(view)
//        }
//
////        fun setContent(historyEntry: ClipboardHistoryEntry?) {
////            itemView.tag = historyEntry?.timeStamp
////            contentView.text = historyEntry?.content
////
////            var from = Misc.getLanguageFrom(context)
////            var to = Misc.getLanguageTo(context)
////            val languageIdentifier = LanguageIdentification.getClient()
////            languageIdentifier.identifyLanguage(historyEntry?.content.toString())
////                .addOnSuccessListener { languageCode ->
////                    Log.d(Misc.logKey, languageCode)
////                    if (languageCode != "und") {
////                        if (languageCode == to) {
////                            to = Misc.getLanguageFrom(context)
////                            from = Misc.getLanguageTo(context)
////                        }
////                    }
////                    jugarTranslation(from, to, historyEntry?.content.toString())
////                }.addOnFailureListener {
////                    jugarTranslation(from, to, historyEntry?.content.toString())
////                }
////
////
////            pinnedIconView.visibility =
////                if (historyEntry?.isPinned == true) View.VISIBLE else View.GONE
////        }
//
////        private fun jugarTranslation(
////            from: String,
////            to: String,
////            text: String,
////        ) {
////
////            shimmerView.visibility = View.VISIBLE
////            llTranslatedContent.visibility = View.GONE
////            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
////            StrictMode.setThreadPolicy(policy)
////            if (from == Misc.defaultLanguage) {
////                val languageIdentifier = LanguageIdentification.getClient()
////                languageIdentifier.identifyLanguage(text).addOnSuccessListener { languageCode ->
////                    if (languageCode != "und") {
////                        languageFromContentView.text = Locale(languageCode).displayName
////                    }
////                }.addOnFailureListener {}
////            } else {
////                languageFromContentView.text = Locale(from).displayName
////            }
////
////
////            languageToContentView.text = Locale(to).displayName
////
////            TranslateTask(from, to, text).execute()
////        }
////
////        private inner class TranslateTask(
////            from: String,
////            to: String?,
////            text: String?,
//////            val textView: TextView
////        ) :
////            AsyncTask<Void?, Void?, String?>() {
////            private var fromCode: String? = null
////            private var toCode: String? = null
////            private var encodedText: String? = null
////
////            init {
////                fromCode = when (from) {
////                    Misc.defaultLanguage -> {
////                        ""
////                    }
////
////                    "zh" -> {
////                        "zh-CN"
////                    }
////
////                    "he" -> {
////                        "iw"
////                    }
////
////                    else -> {
////                        from
////                    }
////                }
////
////                toCode = when (to) {
////                    "zh" -> "zh-CN"
////                    "he" -> "iw"
////                    else -> to
////                }
////                try {
////                    encodedText = URLEncoder.encode(text, "utf-8")
////                } catch (e: UnsupportedEncodingException) {
////                    e.printStackTrace()
////                }
////            }
////
////            override fun onPostExecute(result: String?) {
////                if (result != null) {
////                    Log.d(Misc.logKey, result)
////                    translatedContentView.text = result
////                    llTranslatedContent.visibility = View.VISIBLE
////                    shimmerView.visibility = View.GONE
////
////                }
////
////                Misc.isTranslated.postValue(true)
////            }
////
////            override fun doInBackground(vararg params: Void?): String? {
////                try {
////                    val doc =
////                        Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encodedText")
////                            .get()
////
////                    val element = doc.getElementsByClass("result-container")
////
////                    if (element.text() != "" && !TextUtils.isEmpty(element.text())) {
////                        return element.text()
////                    }
////                } catch (e: IOException) {
////                    e.printStackTrace()
////                }
////
////                return null
////            }
////        }
//    }
}