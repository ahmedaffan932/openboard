package org.dslul.openboard.translator.pro.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
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
                    holder.ivDownloadLanguage.setImageResource(R.drawable.ic_baseline_done_24)
                    isLanguageDownloaded = true
                }
            }
        }

        if (lngTo) {
            if (Misc.getLanguageTo(activity) == languages.elementAt(position)) {
                holder.languageName.setTextColor(ContextCompat.getColor(activity, R.color.accent))
                holder.ivSelected.visibility = View.VISIBLE
            }

            setDownloadClickListener(
                holder.ivDownloadLanguage,
                isLanguageDownloaded,
                languages[position],
                Misc.getLanguageTo(activity),
                holder.pbDownloading
            )
        } else {
            if (Misc.getLanguageFrom(activity) == languages.elementAt(position)) {
                holder.languageName.setTextColor(ContextCompat.getColor(activity, R.color.accent))
                holder.ivSelected.visibility = View.VISIBLE
            }
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
            holder.languageName.text = "Detect"
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
            holder.temp.setImageResource(Misc.getFlag(activity, languages.elementAt(position)))

            holder.lngLayout.setOnClickListener {
                if (lngTo) {
                    Misc.setLanguageTo(activity, languages.elementAt(position))
                } else {
                    Misc.setLanguageFrom(activity, languages.elementAt(position))
                }
                activity.onBackPressed()
                callback?.onDismiss()

                Firebase.analytics.logEvent(Locale(languages[position]).displayName, null)

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
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val result = FilterResults()
                val founded: MutableList<String> = ArrayList()
                languages = tempLanguages

                for (item in languages) {
                    if (constraint?.let {
                            Locale(item).displayName.toLowerCase().contains(it)
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
                    ivDownloadLanguage.setImageResource(R.drawable.ic_baseline_done_24)
                    progressBar.visibility = View.GONE
                }

                ivDownloadLanguage.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                ivDownloadLanguage.setOnClickListener { }
            }
        }
    }


}