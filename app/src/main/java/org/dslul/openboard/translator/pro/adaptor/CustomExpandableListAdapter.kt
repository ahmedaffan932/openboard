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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.ShareUtils
import org.dslul.openboard.translator.pro.classes.TranslateHistoryClass
import com.google.gson.Gson
import org.dslul.openboard.inputmethod.latin.R
import java.util.*
import kotlin.math.roundToInt

/**
 * Phrase details (Figma "Phrasebook detail", 6:1286): each group is a card (list_group) whose
 * phrases (list_item) continue the card while it is open. [groupStyles] gives each group's icon and
 * tone by position; groups past its end use [fallbackStyle] (the category's own).
 */
@Suppress("NAME_SHADOWING")
class CustomExpandableListAdapter(
    private val activity: Activity,
    private val expandableListTitle: List<String>,
    private val expandableListTitleTranslation: List<String>,
    private val expandableListDetailTranslation: HashMap<String, List<String>>,
    private val expandableListDetail: HashMap<String, List<String>>,
    private val groupStyles: List<PhraseGroupStyle>,
    private val fallbackStyle: PhraseGroupStyle
) : BaseExpandableListAdapter() {
    private var textToSpeech: TextToSpeech? = null
    private val sdk = Build.VERSION.SDK_INT

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return expandableListDetail[expandableListTitle[listPosition]]?.get(expandedListPosition)!!
    }

    /** The translation paired with a phrase, or "" if that language's file has none for it. */
    private fun getChildTranslation(listPosition: Int, expandedListPosition: Int): Any {
        return expandableListTitleTranslation.getOrNull(listPosition)
            ?.let { expandableListDetailTranslation[it] }
            ?.getOrNull(expandedListPosition)
            ?: ""
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
        // Inflated before anything that can throw: the catch below returns convertView, and a
        // null one crashed the app (e.g. on the misaligned Bengali phrases).
        if (convertView == null) {
            val layoutInflater =
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.list_item, null)
        }
        try {

            val expandedListText = getChild(listPosition, expandedListPosition) as String

            val expandedListTextTranslation =
                getChildTranslation(listPosition, expandedListPosition) as String

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

            // The last phrase closes the group's card: rounded bottom, no divider under it.
            llMain.setBackgroundResource(
                if (isLastChild) R.drawable.ui_tabs_bg_child_bottom
                else R.drawable.ui_tabs_bg_child_middle
            )
            llMain.setPadding(
                llMain.paddingLeft,
                llMain.paddingTop,
                llMain.paddingRight,
                if (isLastChild) dp(8) else 0
            )
            convertView.findViewById<View>(R.id.vItemDivider).visibility =
                if (isLastChild) View.GONE else View.VISIBLE


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
                Toast.makeText(activity, R.string.copied, Toast.LENGTH_SHORT).show()
            }

            btnShareItem.setOnClickListener {
                ShareUtils.shareText(activity, expandedListTVTranslation.text)
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

        (groupStyles.getOrNull(listPosition) ?: fallbackStyle).applyTo(
            convertView.findViewById(R.id.flItemIcon),
            convertView.findViewById(R.id.imgItemIcon)
        )

        // Open with phrases under it: the header is the top of a card its phrases continue.
        val phraseCount = runCatching { getChildrenCount(listPosition) }.getOrDefault(0)
        val isOpenCard = isExpanded && phraseCount > 0
        convertView.findViewById<TextView>(R.id.tvGroupCount).text = phraseCount.toString()
        convertView.findViewById<View>(R.id.clMain).setBackgroundResource(
            if (isOpenCard) R.drawable.ui_tabs_bg_group_card_top else R.drawable.ui_tabs_bg_group_card
        )
        convertView.findViewById<View>(R.id.vGroupDivider).visibility =
            if (isOpenCard) View.VISIBLE else View.GONE
        convertView.findViewById<ImageView>(R.id.btnItemNext).apply {
            clearAnimation()
            setImageResource(if (isExpanded) R.drawable.ic_ui_chevron_up else R.drawable.ic_ui_chevron_down)
        }

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


    /** Opens or closes the group; getGroupView then swaps the chevron (up while open). */
    @Suppress("UNUSED_PARAMETER")
    private fun expandOrCollapseGroup(
        isExpanded: Boolean,
        parent: ViewGroup,
        listPosition: Int,
        imageView: ImageView
    ) {
        if (isExpanded) {
            (parent as ExpandableListView).collapseGroup(listPosition)
        } else {
            (parent as ExpandableListView).expandGroup(listPosition, true)
        }
    }

    private fun dp(value: Int): Int =
        (value * activity.resources.displayMetrics.density).roundToInt()

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
