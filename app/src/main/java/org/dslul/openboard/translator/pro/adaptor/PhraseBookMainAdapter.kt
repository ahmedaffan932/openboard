package org.dslul.openboard.translator.pro.adaptor

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.PhrasebookDetailedActivity

/**
 * The Phrasebook categories (Figma "Phrasebook", 5:759): one card per category with a tone icon
 * tile, shown as a two-column grid by PhrasesFragment. Categories come in the phrase files' order
 * (At the Store, At the Hotel, ... While Traveling), which [categoryStyle] follows.
 */
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
            categoryStyle(position).applyTo(holder.flPhraseMainItemIcon, holder.imgPhraseMainItemIcon)
            holder.cardMain.setOnClickListener {
                onClick(position)
            }
            holder.clMain.setOnClickListener {
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
        val clMain: View = itemView.findViewById(R.id.clMain)
        val cardMain: View = itemView.findViewById(R.id.cl)
        val tvPhraseBookMainItem: TextView = itemView.findViewById(R.id.tvPhraseBookMainItem)
        val nativeAdFrameLayout: FrameLayout = itemView.findViewById(R.id.nativeAdFrameLayout)
        val flPhraseMainItemIcon: View = itemView.findViewById(R.id.flPhraseMainItemIcon)
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

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    companion object {
        /** Each category's icon and tone, in the phrase files' category order. */
        private val CATEGORY_STYLES = listOf(
            PhraseGroupStyle.orange(R.drawable.ic_ui_cart),            // At the Store
            PhraseGroupStyle.accent(R.drawable.ic_ui_hotel),           // At the Hotel
            PhraseGroupStyle.purple(R.drawable.ic_ui_wine),            // At the Bar
            PhraseGroupStyle.danger(R.drawable.ic_ui_utensils),        // At the Restaurant
            PhraseGroupStyle.teal(R.drawable.ic_ui_list_checks),       // Essentials
            PhraseGroupStyle.success(R.drawable.ic_ui_stethoscope),    // Medical Help
            PhraseGroupStyle.orange(R.drawable.ic_ui_alert),           // Problems
            PhraseGroupStyle.purple(R.drawable.ic_ui_calendar_clock),  // Time, Date and Numbers
            PhraseGroupStyle.accent(R.drawable.ic_ui_plane),           // While Traveling
        )

        /** The icon and tone of the category at [position] (wraps around past the last one). */
        fun categoryStyle(position: Int): PhraseGroupStyle =
            CATEGORY_STYLES[Math.floorMod(position, CATEGORY_STYLES.size)]
    }
}

/** An icon on a soft tone tile (bg_ui_tile_*), tinted with the tone colour (home_*). */
data class PhraseGroupStyle(
    @DrawableRes val icon: Int,
    @DrawableRes val tile: Int,
    @ColorRes val tint: Int
) {
    fun applyTo(tileView: View, iconView: ImageView) {
        tileView.setBackgroundResource(tile)
        iconView.setImageResource(icon)
        ImageViewCompat.setImageTintList(
            iconView,
            ColorStateList.valueOf(ContextCompat.getColor(iconView.context, tint))
        )
    }

    companion object {
        fun accent(@DrawableRes icon: Int) =
            PhraseGroupStyle(icon, R.drawable.bg_ui_tile_accent, R.color.home_accent)

        fun purple(@DrawableRes icon: Int) =
            PhraseGroupStyle(icon, R.drawable.bg_ui_tile_purple, R.color.home_purple)

        fun teal(@DrawableRes icon: Int) =
            PhraseGroupStyle(icon, R.drawable.bg_ui_tile_teal, R.color.home_teal)

        fun orange(@DrawableRes icon: Int) =
            PhraseGroupStyle(icon, R.drawable.bg_ui_tile_orange, R.color.home_orange)

        fun danger(@DrawableRes icon: Int) =
            PhraseGroupStyle(icon, R.drawable.bg_ui_tile_danger, R.color.home_danger)

        fun success(@DrawableRes icon: Int) =
            PhraseGroupStyle(icon, R.drawable.bg_ui_tile_success, R.color.home_success)
    }
}
