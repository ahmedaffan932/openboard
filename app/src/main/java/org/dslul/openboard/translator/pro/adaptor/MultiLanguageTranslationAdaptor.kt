package org.dslul.openboard.translator.pro.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.TranslatedItem
import com.google.mlkit.nl.languageid.LanguageIdentification
import org.dslul.openboard.inputmethod.latin.R

class MultiLanguageTranslationAdaptor(val activity: Activity) :
    RecyclerView.Adapter<MultiLanguageTranslationAdaptor.TranslationHolder>() {
    var arr = emptyList<TranslatedItem>()

    open class TranslationHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val textLngTo: TextView = itemView.findViewById(R.id.textLngTo)
        val textLngFrom: TextView = itemView.findViewById(R.id.textLngFrom)
        val textViewTextTranslated: TextView = itemView.findViewById(R.id.textViewTextTranslated)
        val llTranslatedText: LinearLayout = itemView.findViewById(R.id.llTranslatedText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranslationHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.multi_laguage_translated_item, parent, false)
        return TranslationHolder(view)
    }

    override fun onBindViewHolder(holder: TranslationHolder, position: Int) {
        val obj = arr[position]


        if (position == 0) {
            holder.llTranslatedText.setBackgroundResource(R.drawable.bg_half_up_rounded)
        }

        if (position == arr.size - 1) {
            holder.llTranslatedText.setBackgroundResource(R.drawable.bg_half_down_rounded)
            val params: ConstraintLayout.LayoutParams =
                holder.llTranslatedText.layoutParams as ConstraintLayout.LayoutParams
            params.setMargins(params.marginStart, params.bottomToTop, params.marginEnd, 20)

            holder.llTranslatedText.layoutParams = params
        }

        holder.textLngTo.text = obj.lngTo
        holder.textLngFrom.text = obj.lngFrom
        holder.textViewTextTranslated.text = obj.translatedText

        if (Misc.getLanguageFrom(activity) == Misc.defaultLanguage) {
            val languageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(obj.originalText)
                .addOnSuccessListener { languageCode ->
                    if (languageCode == "und") {
                        holder.textLngFrom.text = "Detect"
                    } else {
                        holder.textLngFrom.text = "Detected -> $languageCode"
                    }
                }
                .addOnFailureListener {
                    holder.textLngFrom.text = "Detect"
                }
        }


    }

    override fun getItemCount(): Int {
        return arr.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }


    @SuppressLint("SuspiciousIndentation")
    fun setData(arrTranslatedItems: List<TranslatedItem>) {
        arr = arrTranslatedItems
        notifyDataSetChanged()
    }
}