package org.dslul.openboard.translator.pro

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.dslul.openboard.translator.pro.classes.ConversationClass
import org.dslul.openboard.translator.pro.classes.Misc
import org.dslul.openboard.translator.pro.classes.admob.BannerAds
import org.dslul.openboard.translator.pro.classes.admob.InterstitialAd
import org.dslul.openboard.translator.pro.classes.admob.NativeAds
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.android.synthetic.main.activity_conversation.*
import kotlinx.android.synthetic.main.bottom_sheet_conversation.*
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.translator.pro.adaptor.ConversationAdapter
import org.dslul.openboard.translator.pro.adaptor.LanguagesAdapterConversation
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList

class ConversationActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    lateinit var adapter: LanguagesAdapterConversation
    lateinit var bottomSheetBehaviorConversation: BottomSheetBehavior<ConstraintLayout>

    var isBGVisible = true
    private val conversation: ArrayList<ConversationClass> = ArrayList()

    private val speechRequestCodeFrom = 1
    private val speechRequestCodeTo = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_conversation)

        showNativeAd()

        InterstitialAd.show(this, Misc.chatIntAm)

        Misc.isActivityCreatingFirstTime = true

        btnBack.setOnClickListener {
            onBackPressed()
        }

        if (!Misc.isNightModeOn(this)) {
            imgConversation.setImageResource(R.drawable.ic_conversation)
        }

        setSelectedLng()

        simpleSearchViewCon.setOnQueryTextListener(this);

        val arr = ArrayList<String>()
        for (lng in TranslateLanguage.getAllLanguages()) {
            arr.add(lng)
        }
        textViewLeftOnClickIfCollapsed(llLngLeftFrag)
        textViewRightOnClickIfCollapsed(llLngRightFrag)

        recyclerViewConversation.layoutManager = LinearLayoutManager(this)
        recyclerViewConversation.adapter = ConversationAdapter(this, conversation)

        recyclerViewLanguageSelectorConversation.layoutManager =
            LinearLayoutManager(this)
        adapter = LanguagesAdapterConversation(
            arr,
            this,
            R.id.bottomSheetConversation,
            clMain
        )
        recyclerViewLanguageSelectorConversation.adapter = adapter
        recyclerViewLanguageSelectorConversation.isNestedScrollingEnabled = true

        btnClearCon.setOnClickListener {
            Misc.zoomOutView(btnClearCon, this, 150)
            Handler().postDelayed({
                btnClearCon.visibility = View.GONE
            }, 150)
            Misc.zoomInView(bgConversation, this, 150)
            bgConversation.visibility = View.VISIBLE
            recyclerViewConversation.visibility = View.GONE
            isBGVisible = true
            conversation.clear()
            recyclerViewConversation.adapter!!.notifyDataSetChanged()
        }

        bottomSheetBehaviorConversation =
            BottomSheetBehavior.from(findViewById(R.id.bottomSheetConversation))
        bottomSheetBehaviorConversation.state = BottomSheetBehavior.STATE_COLLAPSED

        bottomSheetBehaviorConversation.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (bottomSheetBehaviorConversation.state) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        btnOpenCloseBottomSheet.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)

                        llLngLeftFrag.backgroundTintList =
                            ColorStateList.valueOf(resources.getColor(R.color.accent))
                        textViewLngLeftFrag.setTextColor(resources.getColor(R.color.black))
                        Misc.isLngTo = false

                        textViewLeftOnClickIfExpanded(
                            textViewLngLeftFrag,
                            textViewLngRightFrag,
                            llLngLeftFrag,
                            llLngRightFrag
                        )
                        textViewRightOnClickIfExpanded(
                            textViewLngLeftFrag,
                            textViewLngRightFrag,
                            llLngLeftFrag,
                            llLngRightFrag
                        )
                        recyclerViewLanguageSelectorConversation.visibility =
                            View.VISIBLE
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        val typedValue = TypedValue()
                        theme.resolveAttribute(R.attr.colorPrimaryVariant, typedValue, true)
                        val color = typedValue.data
                        Misc.colorPrimary = color

                        btnOpenCloseBottomSheet.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24)

                        llLngRightFrag.setBackgroundResource(R.drawable.bg_main_rounded)
                        llLngRightFrag.backgroundTintList =
                            ColorStateList.valueOf(Misc.colorPrimary)
                        textViewLngRightFrag.setTextColor(
                            Misc.getThemeColor(
                                R.attr.colorOnPrimary,
                                this@ConversationActivity
                            )
                        )

                        llLngLeftFrag.setBackgroundResource(R.drawable.bg_main_rounded)
                        llLngLeftFrag.backgroundTintList =
                            ColorStateList.valueOf(Misc.colorPrimary)
                        textViewLngLeftFrag.setTextColor(
                            Misc.getThemeColor(
                                R.attr.colorOnPrimary,
                                this@ConversationActivity
                            )
                        )

                        textViewLeftOnClickIfCollapsed(llLngLeftFrag)
                        textViewRightOnClickIfCollapsed(llLngRightFrag)
                        setSelectedLng()
                        simpleSearchViewCon.setQuery("", false);
                        simpleSearchViewCon.clearFocus();
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                    }
                }
            }
        })

        val vto: ViewTreeObserver = linearLayout2.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    linearLayout2.viewTreeObserver.removeGlobalOnLayoutListener(
                        this
                    )
                } else {
                    linearLayout2.viewTreeObserver.removeOnGlobalLayoutListener(
                        this
                    )
                }
                val height: Int = linearLayout2.measuredHeight
                bottomSheetBehaviorConversation.peekHeight =
                    btnOpenCloseBottomSheet.layoutParams.height + height
            }
        })


        btnOpenCloseBottomSheet.setOnClickListener {
            if (bottomSheetBehaviorConversation.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehaviorConversation.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehaviorConversation.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

    }


    private fun textViewLeftOnClickIfCollapsed(ll: LinearLayout) {
        ll.setOnClickListener {
            displaySpeechRecognizerFrom()
        }

    }

    private fun textViewRightOnClickIfCollapsed(ll: LinearLayout) {
        ll.setOnClickListener {
            displaySpeechRecognizerTo()
        }
    }

    private fun textViewRightOnClickIfExpanded(
        textViewLeft: TextView,
        textViewRight: TextView,
        llLeft: LinearLayout,
        llRight: LinearLayout
    ) {
        llRight.setOnClickListener {
            if (!Misc.isLngTo) {
                llLeft.backgroundTintList = llRight.backgroundTintList
                textViewLeft.setTextColor(textViewRight.textColors)

                llRight.backgroundTintList =
                    ColorStateList.valueOf(
                        resources.getColor(R.color.accent)
                    )
                textViewRight.setTextColor(resources.getColor(R.color.black))
                Misc.isLngTo = true
            }
        }

    }

    private fun textViewLeftOnClickIfExpanded(
        textViewLeft: TextView,
        textViewRight: TextView,
        llLeft: LinearLayout,
        llRight: LinearLayout
    ) {
        llLeft.setOnClickListener {
            if (Misc.isLngTo) {
                llRight.backgroundTintList = llLeft.backgroundTintList
                textViewRight.setTextColor(textViewLeft.textColors)

                llLeft.backgroundTintList =
                    ColorStateList.valueOf(
                        resources.getColor(R.color.accent)
                    )
                textViewLeft.setTextColor(resources.getColor(R.color.black))
                Misc.isLngTo = false
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun setSelectedLng() {
        if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
            textViewLngLeftFrag.text =
                "English"
            flagFromFragCon.setImageResource(Misc.getFlag(this, "en"))
        } else {
            flagFromFragCon.setImageResource(
                Misc.getFlag(
                    this,
                    Misc.getLanguageFrom(this)
                )
            )
            textViewLngLeftFrag.text = Locale(
                Misc.getLanguageFrom(this)
            ).displayName
        }
        flagToFragCon.setImageResource(
            Misc.getFlag(
                this,
                Misc.getLanguageTo(this)
            )
        )
        textViewLngRightFrag.text = Locale(
            Misc.getLanguageTo(this)
        ).displayName
    }

    override fun onResume() {
        super.onResume()
        setSelectedLng()
        BannerAds.show(bannerFrame)

        if (Misc.isNativeAdClicked) {
            showNativeAd()
        }
    }

    private fun translateFromTo(text: String) {
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                "Please check your Internet connection and try again later.",
                Toast.LENGTH_SHORT
            ).show()
            llPBConversationFrag.visibility = View.GONE
            return
        }

        val encoded = URLEncoder.encode(text, "utf-8")

        val toCode = Misc.getLanguageTo(this)

        val fromCode =
            if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
                "en"
            } else
                Misc.getLanguageFrom(this)

        val stringRequest: StringRequest = @SuppressLint("CutPasteId")
        object : StringRequest(
            Method.POST,
            "${Misc.translationUrl}q=$encoded",
            Response.Listener { response ->
                val tempArrayList =
                    JSONObject(response).getJSONObject(Misc.data).getJSONArray(Misc.translations)

                var str = ""
                for (i in 0 until tempArrayList.length()) {
                    str += tempArrayList.getJSONObject(i).getString(Misc.translatedText)
                }

                val obj = ConversationClass(text, str, fromCode, toCode, true)
                conversation.add(obj)
//                translation.add(str)
                recyclerViewConversation.adapter!!.notifyItemInserted(conversation.lastIndex)
                recyclerViewConversation.scrollToPosition(conversation.lastIndex)
                manageBG()

                llPBConversationFrag.visibility = View.GONE
            },
            Response.ErrorListener { error ->
                llPBConversationFrag.visibility = View.GONE
                Log.d(Misc.logKey, Misc.getGoogleApi(this))
                Log.d(Misc.logKey, "Translation error $error")
            }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params[Misc.key] = Misc.getGoogleApi(this@ConversationActivity)
                params[Misc.target] = toCode
                params[Misc.source] = fromCode
                return params
            }

            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["User-agent"] = Misc.userAgent
                return headers
            }
        }

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)

    }

    private fun jugarTranslationFromTo(text: String) {
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                "Please check your Internet connection and try again later.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val policy: StrictMode.ThreadPolicy =
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val encoded = URLEncoder.encode(text, "utf-8")

        val toCode = when (Misc.getLanguageTo(this)) {
            "zh" -> "zh-CN"
            "he" -> "iw"
            else -> Misc.getLanguageTo(this)
        }

        val fromCode =
            if (Misc.getLanguageFrom(this) == Misc.defaultLanguage)
                ""
            else if (Misc.getLanguageFrom(this) == "zh")
                "zh-CN"
            else if (Misc.getLanguageFrom(this) == "he")
                "iw"
            else
                Misc.getLanguageFrom(this)


        val doc =
            Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                .get()

        val element = doc.getElementsByClass("result-container")

        if (element.text() != "" && !TextUtils.isEmpty(element.text())) {

            this.runOnUiThread {
                val obj = ConversationClass(text, element.text(), fromCode, toCode, false)
                conversation.add(obj)
                recyclerViewConversation.adapter!!.notifyItemInserted(conversation.lastIndex)
                recyclerViewConversation.scrollToPosition(conversation.lastIndex)
                manageBG()
                llPBConversationFrag.visibility =
                    View.GONE
            }
        } else {
            llPBConversationFrag.visibility =
                View.GONE
            Toast.makeText(
                this,
                "Some error occurred in translation please try again later.",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(Misc.logKey, "its empty")
        }
    }

    private fun translateToFrom(text: String) {
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                "Please check your Internet connection and try again later.",
                Toast.LENGTH_SHORT
            ).show()
            llPBConversationFrag.visibility = View.GONE
            return
        }

        val encoded = URLEncoder.encode(text, "utf-8")

        val fromCode = Misc.getLanguageTo(this)

        val toCode =
            if (Misc.getLanguageFrom(this) == Misc.defaultLanguage) {
                "en"
            } else
                Misc.getLanguageFrom(this)

        val stringRequest: StringRequest = @SuppressLint("CutPasteId")
        object : StringRequest(
            Method.POST,
            "${Misc.translationUrl}q=$encoded",
            Response.Listener { response ->
                val tempArrayList =
                    JSONObject(response).getJSONObject(Misc.data).getJSONArray(Misc.translations)

                var str = ""
                for (i in 0 until tempArrayList.length()) {
                    str += tempArrayList.getJSONObject(i).getString(Misc.translatedText)
                }

                val obj = ConversationClass(text, str, fromCode, toCode, false)
                conversation.add(obj)
                recyclerViewConversation.adapter!!.notifyItemInserted(conversation.lastIndex)
                recyclerViewConversation.scrollToPosition(conversation.lastIndex)
                manageBG()

                llPBConversationFrag.visibility =
                    View.GONE
            },
            Response.ErrorListener { error ->
                llPBConversationFrag.visibility = View.GONE
                Log.d(Misc.logKey, Misc.getGoogleApi(this))
                Log.d(Misc.logKey, "Translation error $error")
            }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params[Misc.key] = Misc.getGoogleApi(this@ConversationActivity)
                params[Misc.target] = toCode
                params[Misc.source] = fromCode
                return params
            }

            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["User-agent"] = Misc.userAgent
                return headers
            }
        }

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)

    }

    private fun jugarTranslationToFrom(text: String) {
        if (!Misc.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                "Please check your Internet connection and try again later.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Log.d(Misc.logKey, "Here I am....")

        val policy: StrictMode.ThreadPolicy =
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val encoded = URLEncoder.encode(text, "utf-8")

        val fromCode =
            if (Misc.getLanguageTo(this) == Misc.defaultLanguage)
                ""
            else if (Misc.getLanguageTo(this) == "zh")
                "zh-CN"
            else if (Misc.getLanguageTo(this) == "he")
                "iw"
            else
                Misc.getLanguageFrom(this)

        val toCode = when (Misc.getLanguageFrom(this)) {
            Misc.defaultLanguage -> "en"
            "zh" -> "zh-CN"
            "he" -> "iw"
            else -> Misc.getLanguageFrom(this)
        }

        val doc =
            Jsoup.connect("https://translate.google.com/m?hl=en&sl=$fromCode&tl=$toCode&q=$encoded")
                .get()

        val element = doc.getElementsByClass("result-container")

        if (element.text() != "" && !TextUtils.isEmpty(element.text())) {

            this.runOnUiThread {
                val obj = ConversationClass(text, element.text(), fromCode, toCode, true)
                conversation.add(obj)
                recyclerViewConversation.adapter!!.notifyItemInserted(conversation.lastIndex)
                recyclerViewConversation.scrollToPosition(conversation.lastIndex)
                manageBG()

                llPBConversationFrag.visibility =
                    View.GONE

            }
        } else {
            llPBConversationFrag.visibility =
                View.GONE
            Toast.makeText(
                this,
                "Some error occurred in translation please try again later.",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(Misc.logKey, "its empty")
        }
    }


    private fun displaySpeechRecognizerFrom() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.EXTRA_LANGUAGE
                )
            }

            if (Misc.getLanguageFrom(this) != Misc.defaultLanguage) {
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Misc.getLanguageFrom(this)
                )
            } else {
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "en"
                )
            }
            startActivityForResult(intent, speechRequestCodeFrom)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Sorry, Some Error Occurred. Please try again.", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun displaySpeechRecognizerTo() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.EXTRA_LANGUAGE
                )
            }

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Misc.getLanguageTo(this)
            )
            startActivityForResult(intent, speechRequestCodeTo)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Sorry, Some Error Occurred. Please try again.", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequestCodeTo && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results?.get(0)
                }
            if (spokenText != null) {
                Misc.isRight = true
                llPBConversationFrag.visibility = View.VISIBLE
                Handler().postDelayed({
                    jugarTranslationToFrom(spokenText)
                }, 1)
            }
        } else if (requestCode == speechRequestCodeFrom && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results?.get(0)
                }
            if (spokenText != null) {
                Misc.isRight = false
                llPBConversationFrag.visibility = View.VISIBLE
                Handler().postDelayed({
                    jugarTranslationFromTo(spokenText)
                    Log.d(Misc.logKey, "false")
                }, 1)
            }
        }
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        val inputManager: InputMethodManager =
            this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (this.currentFocus != null) {
            inputManager.hideSoftInputFromWindow(
                this.currentFocus!!.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        adapter.filter.filter(newText)
        recyclerViewLanguageSelectorConversation.adapter = adapter
        return true
    }

    fun manageBG() {
        if (isBGVisible) {
            Misc.zoomInView(btnClearCon, this, 150)
            Misc.zoomOutView(bgConversation, this, 150)
            recyclerViewConversation.visibility = View.VISIBLE
            Handler().postDelayed({
                bgConversation.visibility = View.GONE
            }, 150)
            isBGVisible = false
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehaviorConversation.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehaviorConversation.state = BottomSheetBehavior.STATE_COLLAPSED
        } else
            super.onBackPressed()
    }

    private fun showNativeAd() {
        if(Misc.isChatInBetweenNativeEnabled){
            NativeAds.manageShowNativeAd(
                this,
                Misc.chatNativeAm,
                nativeAdFrameLayoutInBetween
            )
        }else{
            NativeAds.showSmallNativeAd(
                this,
                Misc.chatNativeAm,
                nativeAdFrame
            )
        }

    }

}