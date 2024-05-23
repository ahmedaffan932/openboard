package org.dslul.openboard.translator.pro.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import com.google.gson.Gson
import org.dslul.openboard.inputmethod.latin.R
import java.util.*

@Suppress("NAME_SHADOWING")
class CustomExpandableListAdapter(
    private val activity: Activity,
    private val expandableListTitle: List<String>,
    private val expandableListTitleTranslation: List<String>,
    private val expandableListDetailTranslation: HashMap<String, List<String>>,
    private val expandableListDetail: HashMap<String, List<String>>,
    private val images: IntArray
) : BaseExpandableListAdapter() {
    private var textToSpeech: TextToSpeech? = null
    private val sdk = Build.VERSION.SDK_INT

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return expandableListDetail[expandableListTitle[listPosition]]?.get(expandedListPosition)!!
    }

    private fun getChildTranslation(listPosition: Int, expandedListPosition: Int): Any {
        return expandableListDetailTranslation[expandableListTitleTranslation[listPosition]]?.get(
            expandedListPosition
        )!!
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    @SuppressLint("InflateParams")
    override fun getChildView(
        listPosition: Int, expandedListPosition: Int,
        isLastChild: Boolean, convertView: View?, parent: ViewGroup
    ): View {
        var convertView = convertView
        try {

            val expandedListText = getChild(listPosition, expandedListPosition) as String

            val expandedListTextTranslation =
                getChildTranslation(listPosition, expandedListPosition) as String
            if (convertView == null) {
                val layoutInflater =
                    activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                convertView = layoutInflater.inflate(R.layout.list_item, null)
            }

            val expandedListTextView =
                convertView?.findViewById<View>(R.id.expandedListTVText) as TextView
            val expandedListTVTranslation =
                convertView.findViewById<View>(R.id.expandedListTVTranslation) as TextView

            expandedListTextView.text = expandedListText
            expandedListTVTranslation.text = expandedListTextTranslation

            val llItem = convertView.findViewById<LinearLayout>(R.id.llItem)
            val llMain = convertView.findViewById<LinearLayout>(R.id.llMain)
            val btnCopyItem = convertView.findViewById<ImageButton>(R.id.btnCopyItem)
            val btnSpeakItem = convertView.findViewById<ImageButton>(R.id.btnSpeakItem)
            val btnShareItem = convertView.findViewById<ImageButton>(R.id.btnShareItem)
//            val btnFavoriteItem = convertView.findViewById<ImageButton>(R.id.btnFavoriteItem)


            val obj = TranslateHistoryClass(
                expandedListText,
                expandedListTextTranslation,
                Misc.getLanguageTo(activity),
                Misc.getLanguageFrom(activity)
            )

//            btnFavoriteItem.setOnClickListener {
//                if (saveInFavorites(obj)) {
//                    btnFavoriteItem.setImageResource(R.drawable.ic_baseline_favorite_24)
//                } else {
//                    btnFavoriteItem.setImageResource(R.drawable.ic_baseline_favorite_border_24)
//                }
//            }

            btnSpeakItem.setOnClickListener {
                speak(expandedListTVTranslation.text.toString())
            }

            btnCopyItem.setOnClickListener {
                val clipboard: ClipboardManager =
                    activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    "Camera Translator",
                    expandedListTVTranslation.text
                )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(activity, "Copied", Toast.LENGTH_SHORT).show()
            }

            btnShareItem.setOnClickListener {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, expandedListTVTranslation.text)
                activity.startActivity(Intent.createChooser(sharingIntent, "Share via"))
            }

            llMain.setOnClickListener {
//                if (llItem.visibility == View.VISIBLE) {
//                    llItem.visibility = View.GONE
//
//
//                } else {
//                    llItem.visibility = View.VISIBLE
//                }
                getChildView(listPosition, expandedListPosition, isLastChild, convertView, parent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return convertView!!
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return expandableListDetail[expandableListTitle[listPosition]]?.size!!
    }

    override fun getGroup(listPosition: Int): Any {
        return try {
            expandableListTitle[listPosition]
        } catch (e: Exception) {
            expandableListTitle[0]
        }
    }

    override fun getGroupCount(): Int {
        return expandableListTitle.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getGroupView(
        listPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var convertView = convertView
        val listTitle = getGroup(listPosition) as String
        if (convertView == null) {
            val layoutInflater =
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.list_group, null)
        }
        val listTitleTextView = convertView?.findViewById<View>(R.id.listTitle) as TextView
//        listTitleTextView.setTypeface(null, Typeface.BOLD)
        listTitleTextView.text = listTitle

        convertView.findViewById<ImageView>(R.id.imgItemIcon).setImageResource(images[listPosition])

        convertView.findViewById<View>(R.id.imgItemIcon).setOnClickListener {
            expandOrCollapseGroup(
                isExpanded,
                parent,
                listPosition,
                convertView.findViewById(R.id.btnItemNext)
            )
        }

        convertView.findViewById<View>(R.id.clMain).setOnClickListener {
            expandOrCollapseGroup(
                isExpanded,
                parent,
                listPosition,
                convertView.findViewById(R.id.btnItemNext)
            )
        }

        convertView.findViewById<View>(R.id.btnItemNext).setOnClickListener {
            expandOrCollapseGroup(
                isExpanded,
                parent,
                listPosition,
                convertView.findViewById(R.id.btnItemNext)
            )
        }

        listTitleTextView.setOnClickListener {
            expandOrCollapseGroup(
                isExpanded,
                parent,
                listPosition,
                convertView.findViewById(R.id.btnItemNext)
            )
        }

        return convertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }

    private fun speak(text: String) {
        val lng = Misc.getLanguageTo(activity)

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


    private fun expandOrCollapseGroup(
        isExpanded: Boolean,
        parent: ViewGroup,
        listPosition: Int,
        imageView: ImageView
    ) {
        if (isExpanded) {
            (parent as ExpandableListView).collapseGroup(listPosition)
            val a: Animation =
                AnimationUtils.loadAnimation(activity, R.anim.rotate_collapse)
            a.duration = 100
            imageView.startAnimation(a)
        } else {
            (parent as ExpandableListView).expandGroup(listPosition, true)
            val a: Animation =
                AnimationUtils.loadAnimation(activity, R.anim.rotate_expand)
            a.duration = 100
            imageView.startAnimation(a)
        }
    }

    private fun saveInFavorites(obj: TranslateHistoryClass): Boolean {
        val arrayList = Misc.getFavorites(activity)

        for (savedObj in arrayList) {
            if (obj.text == savedObj.text && obj.translatedFrom == savedObj.translatedFrom &&
                obj.translatedTo == savedObj.translatedTo && obj.translation == savedObj.translation
            ) {
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

}
