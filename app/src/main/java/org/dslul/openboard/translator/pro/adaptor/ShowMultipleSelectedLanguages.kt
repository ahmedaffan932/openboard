package org.dslul.openboard.translator.pro.adaptor

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import org.dslul.openboard.translator.pro.classes.Misc
import com.guru.translate.translator.pro.translation.keyboard.translator.R
import org.dslul.openboard.translator.pro.interfaces.OnItemClick

class ShowMultipleSelectedLanguages(
    private val activity: Activity,
    private var languages: ArrayList<String>,
    private val callback: OnItemClick
) : RecyclerView.Adapter<ShowMultipleSelectedLanguages.LanguageHolder>() {

    @Suppress("CAST_NEVER_SUCCEEDS")
    open class LanguageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flagView: ImageView = itemView.findViewById(R.id.flagView)
        val tvLngCode: TextView = itemView.findViewById(R.id.tvLngCode)
        val clMain: ConstraintLayout = itemView.findViewById(R.id.clMain)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.language_flag_item, parent, false)
        return LanguageHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageHolder, position: Int) {
        holder.flagView.setImageResource(Misc.getFlag(activity, languages.elementAt(position)))
        holder.tvLngCode.text = languages[position].capitalize()

        holder.clMain.setOnClickListener {
            callback.onClick()
        }
    }

    override fun getItemCount(): Int {
        return languages.size
    }
}