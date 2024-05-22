package org.dslul.openboard.translator.pro.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.dslul.openboard.translator.pro.classes.CustomDialog
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import com.google.gson.Gson
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.TranslateActivity
import org.dslul.openboard.translator.pro.interfaces.OnBackPressedHistoryInterface
import java.util.*
import kotlin.collections.ArrayList

class FavoritesAdapter(private var arr: ArrayList<TranslateHistoryClass>, val activity: Activity):
    RecyclerView.Adapter<FavoritesAdapter.FavoriteItemHolder>(), OnBackPressedHistoryInterface {
    private var textToSpeech: TextToSpeech? = null


    override fun myCallBack() {
        textToSpeech?.speak(
            "",
            TextToSpeech.QUEUE_FLUSH,
            null
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteItemHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.history_item, parent, false)
        return FavoriteItemHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: FavoriteItemHolder, position: Int) {
        holder.tvText.text = arr[position].text
        holder.tvTranslation.text = arr[position].translation
        holder.btnFavoriteHistory.setImageResource(R.drawable.ic_baseline_favorite_24)

        if (arr[position].translatedFrom == Misc.defaultLanguage) {
            holder.tvLngFrom.text =
                "Detected"
        } else {
            holder.tvLngFrom.text = Locale(
                arr[position].translatedFrom
            ).displayName
        }
        holder.tvLngTo.text = Locale(
            arr[position].translatedTo
        ).displayName
        holder.btnCopy.setOnClickListener {
            val clipboard: ClipboardManager =
                activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Camera Translator",
                arr[position].translation
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(activity, "Copied", Toast.LENGTH_SHORT).show()
        }

        holder.btnShare.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(
                Intent.EXTRA_TEXT,
                arr[position].translation
            )
            activity.startActivity(
                Intent.createChooser(
                    sharingIntent,
                    "Share via"
                )
            )
        }

        holder.btnSpeak.setOnClickListener {
            speak(arr[position].translation, arr[position].translatedTo)
        }

        holder.llMain.setOnClickListener{
            val intent = Intent(activity, TranslateActivity::class.java)
            intent.putExtra(Misc.key, arr[position].text)
            activity.startActivity(intent)
        }

        holder.btnFavoriteHistory.setOnClickListener {
            val objCustomDialog = CustomDialog(activity)
            objCustomDialog.show()

            val window: Window = objCustomDialog.window!!
            window.setLayout(
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(R.color.color_nothing)

            objCustomDialog.findViewById<TextView>(R.id.tvTitle).text = "Remove from favorites?"
            objCustomDialog.setCancelable(true)

            objCustomDialog.findViewById<TextView>(R.id.btnYes).setOnClickListener {
                val arrayList = Misc.getFavorites(activity)
                arrayList.removeAt(position)
                saveFavorites(arrayList, activity)
                arr.removeAt(position)
                notifyDataSetChanged()
                objCustomDialog.dismiss()
            }

            objCustomDialog.findViewById<TextView>(R.id.btnNo).setOnClickListener {
                objCustomDialog.dismiss()
            }

//            val alert11: android.app.AlertDialog? = objCustomDialog.create()
//            alert11?.show()
            true

        }
    }

    override fun getItemCount(): Int {
        return arr.size
    }

    open class FavoriteItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llMain: LinearLayout = itemView.findViewById(R.id.llMain)
        val tvText: TextView = itemView.findViewById(R.id.tvTextHistory)
        val tvLngTo: TextView = itemView.findViewById(R.id.tvLngToHistory)
        val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopyHistory)
        val tvLngFrom: TextView = itemView.findViewById(R.id.tvLngFromHistory)
        val btnSpeak: ImageButton = itemView.findViewById(R.id.btnSpeakHistory)
        val btnShare: ImageButton = itemView.findViewById(R.id.btnShareHistory)
        val tvTranslation: TextView = itemView.findViewById(R.id.tvTranslationHistory)
        val btnFavoriteHistory: ImageButton = itemView.findViewById(R.id.btnFavoriteHistory)
        val nativeAdFrameLayout: FrameLayout = itemView.findViewById(R.id.nativeAdFrameLayout)
    }
    private fun speak(text: String, lng: String) {
        textToSpeech = TextToSpeech(
            activity.applicationContext
        ) { i ->
            if (i != TextToSpeech.ERROR) {
                textToSpeech?.language = Locale(
                    lng
                )
                textToSpeech!!.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            } else {
                Toast.makeText(
                    activity,
                    "Sorry! Your device does not support this language.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveFavorites(historyList: ArrayList<TranslateHistoryClass>, activity: Activity?) {
        val sharedPreferences =
            activity?.getSharedPreferences(Misc.favorites, AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences!!.edit()
        val gson = Gson()
        val json = gson.toJson(historyList)
        editor.putString(Misc.favorites, json)
        editor.apply()
    }

    private fun saveInFavorites(obj: TranslateHistoryClass): Boolean {
        val arrayList = Misc.getFavorites(activity)

        for (savedObj in arrayList) {
            if (obj.text == savedObj.text && obj.translatedFrom == savedObj.translatedFrom &&
                obj.translatedTo == savedObj.translatedTo && obj.translation == savedObj.translation) {
                arrayList.remove(savedObj)

                val sharedPreferences = activity.getSharedPreferences(
                    Misc.favorites,
                    AppCompatActivity.MODE_PRIVATE
                )
                val editor = sharedPreferences.edit()
                val gson = Gson()
                val json = gson.toJson(arrayList)
                editor.putString(Misc.favorites, json)
                editor.apply()

                return false
            }
        }

        Log.d(Misc.logKey, arrayList.size.toString())
        arrayList.add(obj)

        val sharedPreferences = activity.getSharedPreferences(
            Misc.favorites,
            AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(arrayList)
        editor.putString(Misc.favorites, json)
        editor.apply()
        return true
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}