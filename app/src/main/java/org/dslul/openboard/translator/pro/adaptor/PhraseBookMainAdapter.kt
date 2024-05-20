package org.dslul.openboard.translator.pro.adaptor

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.PhrasebookDetailedActivity

class PhraseBookMainAdapter(
    private val textList: ArrayList<String>,
    private val activity: Activity
) : RecyclerView.Adapter<PhraseBookMainAdapter.TextHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.phrase_book_main_item, parent, false)
        return TextHolder(view)
    }

    override fun onBindViewHolder(holder: TextHolder, position: Int) {
        try {
            holder.tvPhraseBookMainItem.text = textList[position]
            holder.imgPhraseMainItemIcon.setImageResource(imageResId[position])
            holder.tvPhraseBookMainItem.setOnClickListener {
                onClick(position)
            }
            holder.clMain.setOnClickListener {
                onClick(position)
            }
            holder.imgPhraseMainItemIcon.setOnClickListener {
                onClick(position)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        return textList.size
    }

    open class TextHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clMain: ConstraintLayout = itemView.findViewById(R.id.clMain)
        val tvPhraseBookMainItem: TextView = itemView.findViewById(R.id.tvPhraseBookMainItem)
        val nativeAdFrameLayout: FrameLayout = itemView.findViewById(R.id.nativeAdFrameLayout)
        val imgPhraseMainItemIcon: ImageView = itemView.findViewById(R.id.imgPhraseMainItemIcon)
    }

    private fun onClick(position: Int) {
        if (Misc.canWeProceed) {
            Misc.phrasebookPosition = position
            val intent = Intent(activity, PhrasebookDetailedActivity::class.java)
            intent.putExtra(Misc.data, textList[position])
            intent.putExtra(Misc.key, position)
            activity.startActivity(intent)
        }
    }

    private val imageResId = intArrayOf(
        R.drawable.store,
        R.drawable.review,
        R.drawable.cocktail,
        R.drawable.french_fries,
        R.drawable.delivery_truck,
        R.drawable.hospital,
        R.drawable.error,
        R.drawable.chronometer,
        R.drawable.globe_new,
    )

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}