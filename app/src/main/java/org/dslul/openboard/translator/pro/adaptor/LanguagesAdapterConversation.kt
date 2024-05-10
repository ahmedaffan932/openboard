package org.dslul.openboard.translator.pro.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.classes.Misc
import java.util.Locale

class LanguagesAdapterConversation(
    private var languages: ArrayList<String>,
    private val activity: Activity,
    private val bottomSheetId: Int,
    private val view: View

) : RecyclerView.Adapter<LanguagesAdapterConversation.LanguagesHolder>(), Filterable {
    val tempLanguages = languages

    var isBillingResultOk = false
    var speak: TextToSpeech? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguagesHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.languages_list, parent, false)
        return LanguagesHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LanguagesHolder, position: Int) {
        holder.lngLayout.tag = languages.elementAt(position)

        if (!Misc.getPurchasedStatus(activity))
            if (languages[position] == "he" || languages[position] == "zh") {
                holder.textViewPremium.visibility = View.VISIBLE
            }

//            val lngCode = languageCodeForLanguage(languages.elementAt(position))
        holder.languageName.text = Locale(languages[position]).displayName
        holder.temp.setImageResource(Misc.getFlag(activity, languages.elementAt(position)))

        funSpeak()
        holder.btnSpeak.setOnClickListener {
            speak!!.speak(
                Locale(languages[position]).displayName,
                TextToSpeech.QUEUE_FLUSH,
                null
            )
        }

        holder.lngLayout.setOnClickListener {
            if (view.findViewById<View>(bottomSheetId) != null) {

                if (Misc.isLngTo) {
                    Misc.setLanguageTo(activity, languages.elementAt(position))
                } else {
                    Misc.setLanguageFrom(activity, languages.elementAt(position))
                }

                val bottomSheetBehavior =
                    BottomSheetBehavior.from(view.findViewById(bottomSheetId)!!)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    override fun getItemCount(): Int {
        return languages.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun funSpeak() {
        speak = TextToSpeech(
            activity
        ) { i ->
            if (i != TextToSpeech.ERROR) {
                speak?.language = Locale.ENGLISH
            } else {
                Log.d(Misc.logKey, TextToSpeech.ERROR.toString())
            }
        }
    }

    open class LanguagesHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val languageName: TextView = itemView.findViewById(R.id.languageName)
        val lngLayout: LinearLayout = itemView.findViewById(R.id.lngLayoutActivity)
        val temp: ImageView = itemView.findViewById(R.id.flagView)
        val btnSpeak: ImageView = itemView.findViewById(R.id.btnSpeakTranslation)
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


}