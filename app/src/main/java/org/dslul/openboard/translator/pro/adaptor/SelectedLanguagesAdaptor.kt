package org.dslul.openboard.translator.pro.adaptor

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.SplashScreenActivity
import org.dslul.openboard.translator.pro.interfaces.OnItemClick
import java.util.*

class SelectedLanguagesAdaptor(
    private var activity: Activity,
    private val callback: OnItemClick
) : RecyclerView.Adapter<SelectedLanguagesAdaptor.LanguageHolder>() {
    var isBillingResultOk = false
    private var arrSelectedLanguages: List<String> = emptyList()

    open class LanguageHolder(val itemView: View) :
        RecyclerView.ViewHolder(itemView){
        val tvLanguageName : TextView= itemView.findViewById(R.id.tvLanguageName)
        val ivFlagView: ImageView = itemView.findViewById(R.id.ivFlagView)
        val btnRemove: ImageView = itemView.findViewById(R.id.btnRemove)
        val lngLayoutActivity: LinearLayout = itemView.findViewById(R.id.lngLayoutActivity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.selected_language_item, parent, false)
        return LanguageHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageHolder, position: Int) {
        holder.lngLayoutActivity.tag = arrSelectedLanguages.elementAt(position)

        holder.tvLanguageName.text = Locale(arrSelectedLanguages[position]).displayName
        holder.ivFlagView.setImageResource(
            Misc.getFlag(
                activity,
                arrSelectedLanguages.elementAt(position)
            )
        )

        if (arrSelectedLanguages.size < 2) {
            holder.btnRemove.visibility = ViewGroup.GONE
        }

        holder.btnRemove.setOnClickListener {
            val ar = Misc.getMultipleSelectedLanguages(activity)
            if (arrSelectedLanguages.size < 2) {
                return@setOnClickListener
            }

            ar.remove(arrSelectedLanguages[position])

            Misc.saveMultipleSelectedLanguages(activity, ar)
            setData(Misc.getMultipleSelectedLanguages(activity))

            callback.onClick()
        }

    }

    override fun getItemCount(): Int {
        return arrSelectedLanguages.size
    }

    fun setData(arr: List<String>) {
        arrSelectedLanguages = arr
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}