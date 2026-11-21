package org.dslul.openboard.translator.pro.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import org.dslul.openboard.translator.pro.classes.AppAnalytics
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.ObservableBool
import org.dslul.openboard.translator.pro.interfaces.InterstitialCallBack
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("StaticFieldLeak")
class LanguagesAdapter(
    private var languages: ArrayList<String>,
    private val lngTo: Boolean,
    private val activity: Activity,
    private val arrDownloadedLanguages: List<String>? = null,
    private val callback: InterstitialCallBack? = null
) :
    RecyclerView.Adapter<LanguagesAdapter.LanguageHolder>(), Filterable {
    val tempLanguages = languages
    var speak: TextToSpeech? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.languages_list, parent, false)
        return LanguageHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LanguageHolder, position: Int) {
        var isLanguageDownloaded = false
        if (arrDownloadedLanguages?.isNotEmpty() == true) {
            for (downloadedLanguage in arrDownloadedLanguages) {
                if (downloadedLanguage.contains(languages.elementAt(position))) {
                    isLanguageDownloaded = true
                }
            }
        }
        showDownloadState(holder.ivDownloadLanguage, isLanguageDownloaded)
        holder.ivDownloadLanguage.visibility = View.VISIBLE
        holder.pbDownloading.visibility = View.GONE

        // The chosen language: soft accent row, accent name, check instead of speak / download.
        val selected = isSelected(position)
        holder.lngLayout.setBackgroundResource(
            if (selected) R.drawable.bg_ui_row_selected else R.drawable.bg_ui_row_ripple
        )
        holder.languageName.setTextColor(
            ContextCompat.getColor(
                activity, if (selected) R.color.home_accent else R.color.home_text_primary
            )
        )
        holder.ivSelected.visibility = if (selected) View.VISIBLE else View.GONE
        holder.rowActions.visibility = if (selected) View.GONE else View.VISIBLE
        // No divider under the last row, or next to the selected row's highlight.
        holder.divider.visibility =
            if (position == languages.lastIndex || selected || isSelected(position + 1)) {
                View.INVISIBLE
            } else View.VISIBLE

        if (lngTo) {
            setDownloadClickListener(
                holder.ivDownloadLanguage,
                isLanguageDownloaded,
                languages[position],
                Misc.getLanguageTo(activity),
                holder.pbDownloading
            )
        } else {
            setDownloadClickListener(
                holder.ivDownloadLanguage,
                isLanguageDownloaded,
                Misc.getLanguageFrom(activity),
                languages[position],
                holder.pbDownloading
            )
        }

        holder.lngLayout.tag = languages.elementAt(position)
        if (languages.elementAt(position) == Misc.defaultLanguage) {
            holder.languageName.text = activity.getString(R.string.detect)
            holder.nativeName.visibility = View.GONE
            holder.temp.setImageResource(Misc.getFlag(activity, "100"))
            holder.lngLayout.setOnClickListener {
                if (lngTo) {
                    Misc.setLanguageTo(activity, Misc.defaultLanguage)
                } else {
                    Misc.setLanguageFrom(activity, Misc.defaultLanguage)
                }
                activity.onBackPressed()

            }
            holder.btnSpeak.visibility = View.INVISIBLE
        } else {
            holder.languageName.text = Locale(languages[position]).displayName
            val nativeName = nativeName(languages[position])
            holder.nativeName.text = nativeName
            holder.nativeName.visibility = if (nativeName.isBlank()) View.GONE else View.VISIBLE
            holder.temp.setImageResource(Misc.getFlag(activity, languages.elementAt(position)))
            holder.btnSpeak.visibility = View.VISIBLE

            holder.lngLayout.setOnClickListener {
                if (lngTo) {
                    Misc.setLanguageTo(activity, languages.elementAt(position))
                } else {
                    Misc.setLanguageFrom(activity, languages.elementAt(position))
                }
                activity.onBackPressed()
                callback?.onDismiss()

                AppAnalytics.translateLanguageSelect(languages[position], if (lngTo) "to" else "from")

                if (callback == null)
                    activity.onBackPressed()
            }

            ObservableBool.setMyBoolean(!ObservableBool.getMyBoolean())

            funSpeak()
            holder.btnSpeak.setOnClickListener {
                speak!!.speak(
                    Locale(languages[position]).displayName,
                    TextToSpeech.QUEUE_FLUSH,
                    null
                )
            }
        }
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun funSpeak() {
        speak = TextToSpeech(
            activity.applicationContext
        ) { i ->
            if (i != TextToSpeech.ERROR) {
                speak?.language = Locale.ENGLISH
            } else {
                Log.d(Misc.logKey, TextToSpeech.ERROR.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return languages.size
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    open class LanguageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val languageName: TextView = itemView.findViewById(R.id.languageName)
        val ivSelected: ImageView = itemView.findViewById(R.id.ivSelected)
        val lngLayout: LinearLayout = itemView.findViewById(R.id.lngLayoutActivity)
        val temp: ImageView = itemView.findViewById(R.id.flagView)
        val btnSpeak: ImageView = itemView.findViewById(R.id.btnSpeakTranslation)
        val ivDownloadLanguage: ImageView = itemView.findViewById(R.id.ivDownloadLanguage)
        val pbDownloading: ProgressBar = itemView.findViewById(R.id.pbDownloading)
        val textViewPremium: TextView = itemView.findViewById(R.id.textViewPremium)
        val nativeName: TextView = itemView.findViewById(R.id.tvNativeName)
        val rowActions: View = itemView.findViewById(R.id.llRowActions)
        val divider: View = itemView.findViewById(R.id.vRowDivider)
    }

    /** Whether the row at [position] is the language currently picked for this side. */
    private fun isSelected(position: Int): Boolean {
        val language = languages.getOrNull(position) ?: return false
        val current = if (lngTo) Misc.getLanguageTo(activity) else Misc.getLanguageFrom(activity)
        return current == language
    }

    /** The language's own name for itself ("Español"), shown under its name. */
    private fun nativeName(code: String): String {
        val locale = Locale(code)
        return locale.getDisplayName(locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    /** Download icon (accent) or downloaded check (success). */
    private fun showDownloadState(icon: ImageView, isDownloaded: Boolean) {
        icon.setImageResource(if (isDownloaded) R.drawable.ic_ui_circle_check else R.drawable.ic_ui_download)
        ImageViewCompat.setImageTintList(
            icon,
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    activity, if (isDownloaded) R.color.home_success else R.color.home_accent
                )
            )
        )
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val result = FilterResults()
                val founded: MutableList<String> = ArrayList()
                languages = tempLanguages

                for (item in languages) {
                    if (constraint?.let {
                            Locale(item).displayName.lowercase().contains(it)
                        } == true) {
                        founded.add(item)
                        Log.d(Misc.logKey, item)
                    }
                }
                result.values = founded;
                result.count = founded.size;

                return result
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                languages = tempLanguages
                languages = results!!.values as ArrayList<String>
                notifyDataSetChanged()

            }
        }
    }


    private fun setDownloadClickListener(
        ivDownloadLanguage: ImageView,
        isLanguageDownloaded: Boolean,
        from: String,
        to: String,
        progressBar: View
    ) {
        if (!isLanguageDownloaded) {
            ivDownloadLanguage.setOnClickListener {

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(from)
                    .setTargetLanguage(to)
                    .build()
                val obj = Translation.getClient(options)
                obj.downloadModelIfNeeded().addOnSuccessListener {
                    ivDownloadLanguage.visibility = View.VISIBLE
                    showDownloadState(ivDownloadLanguage, true)
                    progressBar.visibility = View.GONE
                }

                ivDownloadLanguage.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                ivDownloadLanguage.setOnClickListener { }
            }
        }
    }


}
